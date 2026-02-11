package com.jeremyzay.zaychess.services.infrastructure.engine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Minimal UCI client for Java chess GUIs. */
public final class UciClient implements AutoCloseable {
    private static final boolean DEBUG_LOG = Boolean.getBoolean("uci.debug");
    private static final int DEBUG_MAX_LINES = Math.max(20, Integer.getInteger("uci.debug.lines", 100));
    private static final int DEBUG_MAX_COMMANDS = Math.max(10, Integer.getInteger("uci.debug.commands", 40));

    private final AutoCloseable transport;
    private final InProcessRuntime inProcess;
    private final BufferedWriter w;
    private final BufferedReader r;
    private final ExecutorService pump = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "uci-out-pump");
        t.setDaemon(true);
        return t;
    });
    private static final long UCI_HANDSHAKE_TIMEOUT_MS = Long.getLong("uci.handshake.timeout", 10_000L);

    private final Deque<String> recentOutput = new ArrayDeque<>(DEBUG_MAX_LINES);
    private final Object recentLock = new Object();
    private final Deque<String> recentCommands = new ArrayDeque<>(DEBUG_MAX_COMMANDS);
    private final Object commandLock = new Object();
    private volatile String lastCommand;
    private volatile String lastPosition;

    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>(65536);
    private volatile boolean alive = true;

    // We maintain move history in the engine's expected move notation.
    private final List<String> moveHistory = new ArrayList<>();
    private String startFEN = "startpos";

    private UciClient(InputStream stdout, OutputStream stdin, AutoCloseable transport)
            throws IOException, TimeoutException {
        this.transport = transport;
        this.inProcess = (transport instanceof InProcessRuntime)
                ? (InProcessRuntime) transport
                : null;
        this.w = new BufferedWriter(new OutputStreamWriter(stdin, StandardCharsets.UTF_8));
        this.r = new BufferedReader(new InputStreamReader(stdout, StandardCharsets.UTF_8));

        pump.submit(() -> {
            try {
                for (String line; (line = r.readLine()) != null;) {
                    recordOutput(line);
                    lines.offer(line);
                }
            } catch (IOException ignored) {
            } finally {
                alive = false;
            }
        });

        send("uci");
        waitForToken("uciok", UCI_HANDSHAKE_TIMEOUT_MS);
        isReady(10_000);
    }

    /** Launch engine from JAR. javaCmd usually "java". */
    public static UciClient launchJar(String javaCmd, String jarPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                javaCmd,
                "--add-modules=jdk.incubator.vector",
                "-jar",
                jarPath);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        return new UciClient(proc.getInputStream(), proc.getOutputStream(), proc::destroy);
    }

    /** Launch engine in-process and connect via piped streams. */
    public static UciClient launchInProcess(Runnable engineMain) throws Exception {
        InProcessRuntime runtime = InProcessRuntime.start(engineMain);
        return new UciClient(runtime.fromEngine, runtime.toEngine, runtime);
    }

    /** Launch native wrapper (e.g., ./Serendipity-Dev). */
    public static UciClient launchNative(String enginePath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(enginePath);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        return new UciClient(proc.getInputStream(), proc.getOutputStream(), proc::destroy);
    }

    /** Ensure engine is ready. */
    public void isReady(long timeoutMs) throws IOException, TimeoutException {
        drainLines();
        send("isready");
        waitForToken("readyok", timeoutMs);
    }

    /** Discard any pending engine output lines. */
    private void drainLines() {
        lines.clear();
    }

    /** Start a fresh game. Resets internal history too. */
    public void newGame() throws IOException, TimeoutException {
        moveHistory.clear();
        startFEN = "startpos";
        send("ucinewgame");
        isReady(3000);
    }

    /** If you maintain your own board, set a custom FEN instead of startpos. */
    public void setPositionFEN(String fen) throws IOException, TimeoutException {
        this.startFEN = "fen " + fen;
        moveHistory.clear();
        // Removed isReady sync here to reduce overhead; sync happens during 'go'
    }

    /**
     * Record a user move in the engine's expected notation (Serendipity expects
     * SAN).
     */
    public void applyUserMove(String move) {
        moveHistory.add(move);
    }

    /**
     * Option helper (e.g., setOption("Threads", "4"); setOption("Hash", "256")).
     */
    public void setOption(String name, String value) throws IOException, TimeoutException {
        send("setoption name " + name + " value " + value);
        isReady(2000);
    }

    /** Ask engine to think and return bestmove. Choose one time control method. */
    public BestMove goMovetime(int ms, long timeoutBufferMs) throws IOException, TimeoutException {
        positionSync();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        isReady(5000); // Sync before search
        send("go movetime " + ms);
        return waitBestMove(ms + timeoutBufferMs);
    }

    public BestMove goDepth(int depth, long timeoutMs) throws IOException, TimeoutException {
        positionSync();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        isReady(5000); // Sync before search
        send("go depth " + depth);
        return waitBestMove(timeoutMs);
    }

    public BestMove goNodes(long nodes, long timeoutMs) throws IOException, TimeoutException {
        return goNodesWithMoves(null, nodes, timeoutMs);
    }

    public BestMove goNodesWithMoves(List<String> searchMoves, long nodes, long timeoutMs)
            throws IOException, TimeoutException {
        positionSync();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        isReady(5000); // Sync before search
        String cmd = "go nodes " + nodes;
        if (searchMoves != null && !searchMoves.isEmpty()) {
            cmd += " searchmoves " + String.join(" ", searchMoves);
        }
        send(cmd);
        return waitBestMove(timeoutMs);
    }

    private void positionSync() throws IOException {
        String cmd;
        if (moveHistory.isEmpty()) {
            cmd = "position " + startFEN;
        } else {
            cmd = "position " + startFEN + " moves " + String.join(" ", moveHistory);
        }
        lastPosition = cmd;
        send(cmd);
    }

    private volatile String pendingBestMove = null;

    private void waitForToken(String token, long timeoutMs) throws TimeoutException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end && alive) {
            String line;
            try {
                line = lines.poll(20, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                continue;
            }
            if (line == null)
                continue;

            // If we're waiting for something else (like readyok) but see a bestmove,
            // don't discard it! Store it for the next waitBestMove call.
            if (line.startsWith("bestmove ")) {
                pendingBestMove = line;
                if ("bestmove".equals(token))
                    return;
            }

            if (line.contains(token))
                return;
        }
        if (!alive) {
            throw new TimeoutException("Engine stopped while waiting for: " + token
                    + formatRecentOutput()
                    + formatClientState());
        }
        throw new TimeoutException("Timeout waiting for: " + token
                + formatRecentOutput()
                + formatClientState());
    }

    private BestMove waitBestMove(long timeoutMs) throws TimeoutException {
        // Did we already see it during a prior isReady?
        if (pendingBestMove != null) {
            String line = pendingBestMove;
            pendingBestMove = null;
            return parseBestMove(line);
        }

        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end && alive) {
            String line;
            try {
                line = lines.poll(20, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                continue;
            }
            if (line == null)
                continue;

            if (line.startsWith("bestmove ")) {
                return parseBestMove(line);
            }
        }
        if (!alive) {
            throw new TimeoutException("Engine stopped unexpectedly"
                    + formatRecentOutput()
                    + formatClientState());
        }
        throw new TimeoutException("bestmove not received in time"
                + formatRecentOutput()
                + formatClientState());
    }

    private BestMove parseBestMove(String line) {
        String found = null, ponder = null;
        String[] tok = line.split("\\s+");
        if (tok.length >= 2)
            found = tok[1];
        if (tok.length >= 4 && "ponder".equals(tok[2]))
            ponder = tok[3];
        return new BestMove(found, ponder);
    }

    private void send(String cmd) throws IOException {
        lastCommand = cmd;
        recordCommand(cmd);
        if (DEBUG_LOG) {
            System.err.println("[UCI->] " + cmd);
        }
        try {
            w.write(cmd);
            w.write('\n');
            w.flush();
        } catch (IOException e) {
            throw enrichIo("UCI send failed", e);
        }
    }

    @Override
    public void close() {
        // 1. Kill pump first so it stops blocking on reads
        alive = false;
        pump.shutdownNow();
        // 2. Try to send quit (non-blocking best-effort)
        try {
            send("quit");
        } catch (Exception ignored) {
        }
        // 3. Close writer (signals EOF to engine stdin)
        try {
            w.close();
        } catch (Exception ignored) {
        }
        // 4. Do NOT call r.close() — BufferedReader.readLine() and close()
        // share the same synchronized lock. The pump thread may be blocked
        // inside readLine() on queue.take(), so r.close() would deadlock.
        // The pump is already killed by shutdownNow() above.
        // 5. Close transport (engine runtime) last
        try {
            if (transport != null)
                transport.close();
        } catch (Exception ignored) {
        }
    }

    public record BestMove(String move, String ponder) {
    }

    private void recordOutput(String line) {
        if (line == null)
            return;
        synchronized (recentLock) {
            if (recentOutput.size() == DEBUG_MAX_LINES) {
                recentOutput.removeFirst();
            }
            recentOutput.addLast(line);
        }
        if (DEBUG_LOG) {
            System.err.println("[UCI] " + line);
        }
    }

    private void recordCommand(String cmd) {
        if (cmd == null)
            return;
        synchronized (commandLock) {
            if (recentCommands.size() == DEBUG_MAX_COMMANDS) {
                recentCommands.removeFirst();
            }
            recentCommands.addLast(cmd);
        }
    }

    private IOException enrichIo(String prefix, IOException e) {
        String recent = formatRecentOutput();
        String client = formatClientState();
        if (!recent.isEmpty() || !client.isEmpty()) {
            return new IOException(prefix + recent + client, e);
        }
        return e;
    }

    private String formatRecentOutput() {
        StringBuilder sb = new StringBuilder();
        synchronized (recentLock) {
            if (recentOutput.isEmpty())
                return "";
            sb.append("\nLast engine output:\n");
            for (String line : recentOutput) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private String formatClientState() {
        StringBuilder sb = new StringBuilder();
        if (lastCommand != null) {
            sb.append("\nLast UCI command: ").append(lastCommand);
        }
        if (lastPosition != null) {
            sb.append("\nLast position: ").append(lastPosition);
        }
        if (!moveHistory.isEmpty()) {
            sb.append("\nMove history (tail): ");
            int start = Math.max(0, moveHistory.size() - 16);
            for (int i = start; i < moveHistory.size(); i++) {
                if (i > start)
                    sb.append(' ');
                sb.append(moveHistory.get(i));
            }
        }
        String recentCommands = formatRecentCommands();
        if (!recentCommands.isEmpty())
            sb.append(recentCommands);
        if (inProcess != null) {
            String runtime = inProcess.debugState();
            if (!runtime.isEmpty())
                sb.append(runtime);
        }
        return sb.toString();
    }

    private String formatRecentCommands() {
        StringBuilder sb = new StringBuilder();
        synchronized (commandLock) {
            if (recentCommands.isEmpty())
                return "";
            sb.append("\nRecent UCI commands:");
            for (String cmd : recentCommands) {
                sb.append("\n").append(cmd);
            }
        }
        return sb.toString();
    }

    private static final class InProcessRuntime implements AutoCloseable {
        private static final String ENGINE_THREAD_NAME = "serendipity-uci";
        private static final String ENGINE_THREAD_GROUP = "serendipity-uci-group";

        private final InputStream fromEngine;
        private final OutputStream toEngine;
        private final Thread engineThread;
        private final InputStream originalIn;
        private final PrintStream originalOut;
        private final PrintStream originalErr;
        private final AtomicBoolean restored;
        private final AtomicBoolean engineExited;
        private final AtomicReference<Throwable> engineFailure;

        private InProcessRuntime(InputStream fromEngine, OutputStream toEngine,
                Thread engineThread, InputStream originalIn,
                PrintStream originalOut, PrintStream originalErr,
                AtomicBoolean restored, AtomicBoolean engineExited,
                AtomicReference<Throwable> engineFailure) {
            this.fromEngine = fromEngine;
            this.toEngine = toEngine;
            this.engineThread = engineThread;
            this.originalIn = originalIn;
            this.originalOut = originalOut;
            this.originalErr = originalErr;
            this.restored = restored;
            this.engineExited = engineExited;
            this.engineFailure = engineFailure;
        }

        static InProcessRuntime start(Runnable engineMain) throws IOException {
            QueuePipe enginePipe = new QueuePipe();
            QueuePipe appPipe = new QueuePipe();
            InputStream fromEngine = enginePipe.input;
            OutputStream engineOut = enginePipe.output;
            InputStream engineIn = appPipe.input;
            OutputStream toEngine = appPipe.output;

            InputStream originalIn = System.in;
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            ThreadGroup engineGroup = new ThreadGroup(ENGINE_THREAD_GROUP);
            ThreadRoutingInputStream routedIn = new ThreadRoutingInputStream(engineIn, originalIn, engineGroup);
            ThreadRoutingOutputStream routedOut = new ThreadRoutingOutputStream(engineOut, originalOut, engineGroup);
            ThreadRoutingOutputStream routedErr = new ThreadRoutingOutputStream(engineOut, originalErr, engineGroup);

            PrintStream systemOut = new PrintStream(routedOut, true, StandardCharsets.UTF_8);
            PrintStream systemErr = new PrintStream(routedErr, true, StandardCharsets.UTF_8);

            AtomicBoolean restored = new AtomicBoolean(false);
            AtomicBoolean engineExited = new AtomicBoolean(false);
            AtomicReference<Throwable> engineFailure = new AtomicReference<>();
            Thread engineThread = new Thread(engineGroup, () -> {
                System.setIn(routedIn);
                System.setOut(systemOut);
                System.setErr(systemErr);
                try {
                    engineMain.run();
                } catch (Throwable t) {
                    engineFailure.set(t);
                    t.printStackTrace(systemErr);
                } finally {
                    engineExited.set(true);
                    restoreSystemStreams(originalIn, originalOut, originalErr, restored);
                }
            }, ENGINE_THREAD_NAME);

            engineThread.setDaemon(true);
            engineThread.start();

            return new InProcessRuntime(
                    fromEngine, toEngine, engineThread, originalIn, originalOut,
                    originalErr, restored, engineExited, engineFailure);
        }

        @Override
        public void close() {
            // Close engine's stdin pipe first — engine sees EOF and exits
            try {
                toEngine.close();
            } catch (IOException ignored) {
            }
            // Brief wait for graceful exit
            try {
                engineThread.join(300L);
            } catch (InterruptedException ignored) {
            }
            // Force interrupt if still alive
            if (engineThread.isAlive()) {
                engineThread.interrupt();
                try {
                    engineThread.join(200L);
                } catch (InterruptedException ignored) {
                }
            }
            // Restore System streams immediately
            restoreSystemStreams(originalIn, originalOut, originalErr, restored);
        }

        private static void restoreSystemStreams(InputStream originalIn,
                PrintStream originalOut,
                PrintStream originalErr,
                AtomicBoolean restored) {
            if (restored.compareAndSet(false, true)) {
                System.setIn(originalIn);
                System.setOut(originalOut);
                System.setErr(originalErr);
            }
        }

        private String debugState() {
            StringBuilder sb = new StringBuilder();
            sb.append("\nEngine thread: ").append(engineThread.getState());
            if (engineExited.get())
                sb.append(" (exited)");
            Throwable failure = engineFailure.get();
            if (failure != null) {
                sb.append("\nEngine exception: ").append(failure);
            }
            return sb.toString();
        }
    }

    private static final class QueuePipe {
        private static final byte[] EOF = new byte[0];
        private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final InputStream input = new QueueInputStream(queue, closed);
        private final OutputStream output = new QueueOutputStream(queue, closed);
    }

    private static final class QueueInputStream extends InputStream {
        private final BlockingQueue<byte[]> queue;
        private final AtomicBoolean closed;
        private byte[] current;
        private int index;

        private QueueInputStream(BlockingQueue<byte[]> queue, AtomicBoolean closed) {
            this.queue = queue;
            this.closed = closed;
        }

        @Override
        public int read() throws IOException {
            byte[] buf = nextBuffer();
            if (buf == null)
                return -1;
            return buf[index++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null)
                throw new NullPointerException();
            if (off < 0 || len < 0 || off + len > b.length)
                throw new IndexOutOfBoundsException();
            if (len == 0)
                return 0;
            int count = 0;
            while (count < len) {
                int value = read();
                if (value == -1)
                    return (count == 0) ? -1 : count;
                b[off + count] = (byte) value;
                count++;
                if (available() == 0)
                    break;
            }
            return count;
        }

        @Override
        public int available() {
            if (current == null)
                return 0;
            return Math.max(0, current.length - index);
        }

        @Override
        public void close() {
            // Signal EOF to unblock any thread blocked in queue.take()
            if (closed.compareAndSet(false, true)) {
                queue.offer(QueuePipe.EOF);
            }
        }

        private byte[] nextBuffer() throws IOException {
            while (true) {
                if (current != null && index < current.length)
                    return current;
                if (current != null && current.length == 0)
                    return null;
                byte[] next;
                try {
                    next = queue.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for input", e);
                }
                current = next;
                index = 0;
                if (current.length == 0)
                    return null;
            }
        }
    }

    private static final class QueueOutputStream extends OutputStream {
        private final BlockingQueue<byte[]> queue;
        private final AtomicBoolean closed;

        private QueueOutputStream(BlockingQueue<byte[]> queue, AtomicBoolean closed) {
            this.queue = queue;
            this.closed = closed;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (closed.get())
                throw new IOException("Stream closed");
            if (len == 0)
                return;
            byte[] copy = Arrays.copyOfRange(b, off, off + len);
            try {
                queue.put(copy);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while writing output", e);
            }
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                queue.offer(QueuePipe.EOF);
            }
        }
    }

    private static final class ThreadRoutingOutputStream extends OutputStream {
        private final OutputStream engineOut;
        private final OutputStream appOut;
        private final ThreadGroup engineGroup;

        private ThreadRoutingOutputStream(OutputStream engineOut, OutputStream appOut,
                ThreadGroup engineGroup) {
            this.engineOut = engineOut;
            this.appOut = appOut;
            this.engineGroup = engineGroup;
        }

        @Override
        public void write(int b) throws IOException {
            target().write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            target().write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            target().flush();
        }

        private OutputStream target() {
            return isEngineThread() ? engineOut : appOut;
        }

        private boolean isEngineThread() {
            ThreadGroup current = Thread.currentThread().getThreadGroup();
            while (current != null) {
                if (current == engineGroup)
                    return true;
                current = current.getParent();
            }
            return false;
        }
    }

    private static final class ThreadRoutingInputStream extends InputStream {
        private final InputStream engineIn;
        private final InputStream appIn;
        private final ThreadGroup engineGroup;

        private ThreadRoutingInputStream(InputStream engineIn, InputStream appIn,
                ThreadGroup engineGroup) {
            this.engineIn = engineIn;
            this.appIn = appIn;
            this.engineGroup = engineGroup;
        }

        @Override
        public int read() throws IOException {
            return target().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return target().read(b, off, len);
        }

        @Override
        public int available() throws IOException {
            return target().available();
        }

        private InputStream target() {
            return isEngineThread() ? engineIn : appIn;
        }

        private boolean isEngineThread() {
            ThreadGroup current = Thread.currentThread().getThreadGroup();
            while (current != null) {
                if (current == engineGroup)
                    return true;
                current = current.getParent();
            }
            return false;
        }
    }
}
