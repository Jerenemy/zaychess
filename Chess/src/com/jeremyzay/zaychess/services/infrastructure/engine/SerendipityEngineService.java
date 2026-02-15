package com.jeremyzay.zaychess.services.infrastructure.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SerendipityEngineService implements EngineService {
    private static final String UCI_CLASS_NAME = "org.shawn.games.Serendipity.UCI.UCI";
    private static final String SERENDIPITY_JAR_PROP = "serendipity.jar";
    private static volatile ClassLoader uciClassLoader;

    private final String javaCmd;
    private final String jarPath;
    private final boolean inProcess;
    private UciClient eng;
    private int difficultyLevel = 5; // Default Level 5
    private String currentFen; // Added back

    public SerendipityEngineService() {
        this.javaCmd = null;
        this.jarPath = null;
        this.inProcess = true;
    }

    public SerendipityEngineService(String javaCmd, String jarPath) {
        this.javaCmd = javaCmd;
        this.jarPath = jarPath;
        this.inProcess = false;
    }

    @Override
    public void start() throws Exception {
        if (inProcess) {
            requireVectorModule();
            ensureUciAvailable();
            eng = UciClient.launchInProcess(SerendipityEngineService::runUciMain);
        } else {
            eng = UciClient.launchJar(javaCmd, jarPath);
        }
        // sensible defaults; change as you like
        safeSet("Threads", String.valueOf(Runtime.getRuntime().availableProcessors()));
        safeSet("Hash", "256");
        eng.newGame();
    }

    private void safeSet(String name, String value) {
        try {
            eng.setOption(name, value);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void newGame() throws Exception {
        this.currentFen = "startpos";
        eng.newGame();
    }

    @Override
    public void setOption(String name, String value) throws Exception {
        eng.setOption(name, value);
    }

    @Override
    public void setDifficulty(int level) {
        this.difficultyLevel = Math.max(1, Math.min(10, level));
        if (eng != null) {
            // Enforce Threads=1 for ALL levels due to engine instability with
            // multi-threading
            // The engine was observed to hang with Threads=8 even at low depths.
            safeSet("Threads", "1");

            if (this.difficultyLevel <= 5) {
                // Low resource levels (Hash 16MB)
                safeSet("Hash", "16");
            } else {
                // High resource levels (Hash 64MB - safer than 256MB)
                safeSet("Hash", "64");
            }
        }
    }

    // Note: setPositionFEN and pushUserMove are implemented below near currentFen
    // declaration.
    // Wait, let's keep them here to be clean and remove the bottom ones in next
    // step?
    // Or just implement them here and remove bottom ones.

    @Override
    public void setPositionFEN(String fen) throws Exception {
        this.currentFen = fen;
        if (fen == null || fen.isBlank())
            return;
        eng.setPositionFEN(fen);
    }

    @Override
    public void pushUserMove(String uciMove) {
        eng.applyUserMove(uciMove);
    }

    @Override
    public String bestMoveMs(int movetimeMs) throws Exception {
        return eng.goMovetime(movetimeMs, /* buffer */ 5000).move(); // Increased buffer

    }

    @Override
    public String bestMove() throws Exception {
        return bestMove(null);
    }

    @Override
    public String bestMove(java.util.List<String> searchMoves) throws Exception {
        // Serendipity engine does not support 'searchmoves' or 'MultiPV' reliably.
        // We use a manual evaluation strategy:
        // iterate through candidates, evaluate each at depth 1, pick the best score.

        if (searchMoves == null || searchMoves.isEmpty()) {
            return eng.goDepth(1, 5000).move();
        }

        String bestMove = null;
        // Integer.MIN_VALUE is -2^31. Centipawn scores are usually +/- 30000.
        // Mate scores might be +/- 100000 or similar?
        // Need to be careful with comparison.
        // Let's rely on standard integer comparison, where mate > any cp.
        // But we need to normalize mate/cp.
        // Let's store score as integer, handling mate specially?
        // Actually, let's just use int. Mate in 1 = 10000. Mate in 2 = 9999.
        // Check UciClient score parsing.

        int bestScore = Integer.MIN_VALUE;

        // Iterate through candidates
        // Limit to 50 candidates to prevent timeout if list is huge (unlikely for legal
        // moves)
        int count = 0;
        for (String move : searchMoves) {
            if (count++ > 50)
                break;

            // 1. Setup position: Current FEN + this move
            try {
                eng.setPosition(currentFen, java.util.Collections.singletonList(move));
            } catch (Exception e) {
                continue; // Skip invalid moves?
            }

            // 2. Eval at depth 2
            // Depth 1 is too shallow and causes blunders. Depth 2 is much safer.
            UciClient.BestMove result = eng.goDepth(2, 1000);

            // 3. Get score
            // The score returned is for the side to move AFTER 'move'.
            // So if White plays 'move', it's Black's turn. Score is from Black's
            // perspective.
            // We want to maximize White's score.
            // White's score = - (Black's score).
            // So we negate the result.

            int score = -999999; // Default bad score
            if (result.scoreMate() != null) {
                // Mate in N. Positive means Black mates White. Negative means White mates
                // Black.
                // We want to negate it.
                // If result.scoreMate() == 1 (Black mates in 1), then for White it's -Mate.
                // Value: - (100000 - 1) approx?
                // Let's standard value: Mate > 20000.
                int m = result.scoreMate();
                if (m > 0) {
                    // Black wins. Bad for White.
                    // Score = -(30000 - m*10)
                    score = -(30000 - m);
                } else {
                    // Black loses. Good for White.
                    // Score = (30000 + m) (m is negative)
                    score = 30000 + m; // e.g. 30000 + (-1) = 29999
                }
            } else if (result.scoreCp() != null) {
                score = -result.scoreCp();
            } else {
                // No score available? fallback to 0 or skip?
                // Maybe engine returned quickly without score?
                // Treat as drawish?
                score = 0;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            // System.out.println(
            // "DEBUG Passive: Candidate " + move + " Score: " + score + " (Best so far: " +
            // bestScore + ")");

            // Check for immediate mate?
            if (score > 29000)
                break; // Found winning move
        }

        // System.out.println("DEBUG Passive: Final Selection: " + bestMove + " Score: "
        // + bestScore);

        // Reset position to currentFen (without moves) for safety/next call logic
        if (currentFen != null) {
            eng.setPositionFEN(currentFen);
        } else {
            eng.newGame(); // or startpos?
        }

        if (bestMove != null) {
            return bestMove;
        }

        // Fallback
        return eng.goDepth(1, 1000).move();
    }

    @Override
    public synchronized void close() {
        if (eng != null) {
            eng.close();
            eng = null;
        }
    }

    private static void runUciMain() {
        try {
            Class<?> uciClass = loadUciClass();
            Thread current = Thread.currentThread();
            ClassLoader original = current.getContextClassLoader();
            current.setContextClassLoader(uciClass.getClassLoader());
            try {
                Method main = uciClass.getMethod("main", String[].class);
                main.invoke(null, (Object) new String[0]);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                if (cause instanceof Error)
                    throw (Error) cause;
                throw new IllegalStateException("Serendipity UCI main failed.", cause);
            } finally {
                current.setContextClassLoader(original);
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(missingUciMessage(), e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke Serendipity UCI main.", e);
        }
    }

    private static void requireVectorModule() {
        try {
            Class.forName("jdk.incubator.vector.VectorSpecies", false,
                    SerendipityEngineService.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Missing jdk.incubator.vector. Add --add-modules=jdk.incubator.vector to the JVM args.",
                    e);
        }
    }

    private static void ensureUciAvailable() {
        try {
            loadUciClass();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(missingUciMessage(), e);
        }
    }

    private static Class<?> loadUciClass() throws ClassNotFoundException {
        try {
            return Class.forName(UCI_CLASS_NAME, true,
                    SerendipityEngineService.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            ClassLoader loader = getUciClassLoader();
            if (loader != null) {
                return Class.forName(UCI_CLASS_NAME, true, loader);
            }
            throw e;
        }
    }

    private static ClassLoader getUciClassLoader() {
        ClassLoader cached = uciClassLoader;
        if (cached != null)
            return cached;
        synchronized (SerendipityEngineService.class) {
            if (uciClassLoader != null)
                return uciClassLoader;
            Path jarPath = resolveEngineJar();
            if (jarPath == null)
                return null;
            try {
                uciClassLoader = new URLClassLoader(
                        new URL[] { jarPath.toUri().toURL() },
                        SerendipityEngineService.class.getClassLoader());
            } catch (Exception e) {
                return null;
            }
            return uciClassLoader;
        }
    }

    private static Path resolveEngineJar() {
        String override = System.getProperty(SERENDIPITY_JAR_PROP);
        if (override != null && !override.isBlank()) {
            Path path = Paths.get(override);
            if (Files.isRegularFile(path))
                return path.toAbsolutePath();
        }

        String envOverride = System.getenv("SERENDIPITY_JAR");
        if (envOverride != null && !envOverride.isBlank()) {
            Path path = Paths.get(envOverride);
            if (Files.isRegularFile(path))
                return path.toAbsolutePath();
        }

        Path base = resolveCodeBase();
        if (base != null) {
            Path candidate = base.resolve("engines/Serendipity.jar");
            if (Files.isRegularFile(candidate))
                return candidate;
            candidate = base.resolve("Chess/engines/Serendipity.jar");
            if (Files.isRegularFile(candidate))
                return candidate;
            Path contents = base.getParent();
            if (contents != null) {
                candidate = contents.resolve("Resources/engines/Serendipity.jar");
                if (Files.isRegularFile(candidate))
                    return candidate;
            }
        }

        Path cwd = Paths.get("").toAbsolutePath();
        Path candidate = cwd.resolve("Chess/engines/Serendipity.jar");
        if (Files.isRegularFile(candidate))
            return candidate;
        candidate = cwd.resolve("engines/Serendipity.jar");
        if (Files.isRegularFile(candidate))
            return candidate;
        return null;
    }

    private static Path resolveCodeBase() {
        try {
            URL location = SerendipityEngineService.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            if (location == null)
                return null;
            Path path = Paths.get(location.toURI());
            return path.getParent();
        } catch (Exception e) {
            return null;
        }
    }

    private static String missingUciMessage() {
        return "Serendipity UCI class not found. Ensure Serendipity.jar is on the classpath, "
                + "located at Chess/engines/Serendipity.jar, or pass -Dserendipity.jar=/path/to/Serendipity.jar.";
    }
}
