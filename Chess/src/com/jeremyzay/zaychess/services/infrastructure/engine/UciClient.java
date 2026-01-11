package com.jeremyzay.zaychess.services.infrastructure.engine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Minimal UCI client for Java chess GUIs. */
public final class UciClient implements AutoCloseable {
    private final Process proc;
    private final BufferedWriter w;
    private final BufferedReader r;
    private final ExecutorService pump = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "uci-out-pump"); t.setDaemon(true); return t;
    });
    private static final long UCI_HANDSHAKE_TIMEOUT_MS =
            Long.getLong("uci.handshake.timeout", 10_000L);


    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>(4096);
    private volatile boolean alive = true;

    // We maintain move history in UCI long algebraic (e2e4, e7e8q, etc.)
    private final List<String> moveHistory = new ArrayList<>();
    private String startFEN = "startpos";

    private UciClient(ProcessBuilder pb) throws IOException, TimeoutException {
        pb.redirectErrorStream(true);
        this.proc = pb.start();
        this.w = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8));
        this.r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));

        pump.submit(() -> {
            try {
                for (String line; (line = r.readLine()) != null; ) {
                    lines.offer(line);
                    // System.out.println("[ENG] " + line); // enable for debug
                }
            } catch (IOException ignored) { }
            finally { alive = false; }
        });

        send("uci");
        waitForToken("uciok", UCI_HANDSHAKE_TIMEOUT_MS);
        isReady(10_000);
    }

    /** Launch engine from JAR. javaCmd usually "java". */
    public static UciClient launchJar(String javaCmd, String jarPath) throws Exception {
        return new UciClient(new ProcessBuilder(
                javaCmd,
                "--add-modules=jdk.incubator.vector",
                "-jar",
                jarPath
        ));
    }

    /** Launch native wrapper (e.g., ./Serendipity-Dev). */
    public static UciClient launchNative(String enginePath) throws Exception {
        return new UciClient(new ProcessBuilder(enginePath));
    }

    /** Ensure engine is ready. */
    public void isReady(long timeoutMs) throws IOException, TimeoutException {
        send("isready");
        waitForToken("readyok", timeoutMs);
    }

    /** Start a fresh game. Resets internal history too. */
    public void newGame() throws IOException, TimeoutException {
        moveHistory.clear();
        startFEN = "startpos";
        send("ucinewgame");
        isReady(3000);
    }

    /** If you maintain your own board, set a custom FEN instead of startpos. */
    public void setPositionFEN(String fen) throws IOException {
        this.startFEN = "fen " + fen;
        moveHistory.clear();
    }

    /** Record a user move (UCI format, e.g. "e2e4", "e7e8q"). */
    public void applyUserMove(String uciMove) {
        moveHistory.add(uciMove);
    }

    /** Option helper (e.g., setOption("Threads", "4"); setOption("Hash", "256")). */
    public void setOption(String name, String value) throws IOException, TimeoutException {
        send("setoption name " + name + " value " + value);
        isReady(2000);
    }

    /** Ask engine to think and return bestmove. Choose one time control method. */
    public BestMove goMovetime(int ms, long timeoutBufferMs) throws IOException, TimeoutException {
        positionSync();
        send("go movetime " + ms);
        return waitBestMove(ms + timeoutBufferMs);
    }

    public BestMove goDepth(int depth, long timeoutMs) throws IOException, TimeoutException {
        positionSync();
        send("go depth " + depth);
        return waitBestMove(timeoutMs);
    }

    public BestMove goNodes(long nodes, long timeoutMs) throws IOException, TimeoutException {
        positionSync();
        send("go nodes " + nodes);
        return waitBestMove(timeoutMs);
    }

    private void positionSync() throws IOException {
        if (moveHistory.isEmpty()) {
            send("position " + startFEN);
        } else {
            send("position " + startFEN + " moves " + String.join(" ", moveHistory));
        }
    }

    private BestMove waitBestMove(long timeoutMs) throws TimeoutException {
        long end = System.currentTimeMillis() + timeoutMs;
        String found = null, ponder = null;
        while (System.currentTimeMillis() < end && alive) {
            String line;
            try {
                line = lines.poll(20, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) { continue; }
            if (line == null) continue;

            // Parse an optional "info" stream here if you want (scores, pv, depth).
            if (line.startsWith("bestmove ")) {
                String[] tok = line.split("\\s+");
                if (tok.length >= 2) found = tok[1];
                if (tok.length >= 4 && "ponder".equals(tok[2])) ponder = tok[3];
                break;
            }
        }
        if (found == null) throw new TimeoutException("bestmove not received in time");
        return new BestMove(found, ponder);
    }

    private void send(String cmd) throws IOException {
        w.write(cmd);
        w.write('\n');
        w.flush();
    }

    private void waitForToken(String token, long timeoutMs) throws TimeoutException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end && alive) {
            String line;
            try {
                line = lines.poll(20, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) { continue; }
            if (line == null) continue;
            if (line.contains(token)) return;
        }
        throw new TimeoutException("Timeout waiting for: " + token);
    }

    @Override public void close() {
        try { send("quit"); } catch (Exception ignored) {}
        try { w.close(); } catch (Exception ignored) {}
        try { r.close(); } catch (Exception ignored) {}
        proc.destroy();
        pump.shutdownNow();
    }

    public record BestMove(String move, String ponder) { }
}
