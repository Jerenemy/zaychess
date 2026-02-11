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
        eng.newGame();
    }

    @Override
    public void setOption(String name, String value) throws Exception {
        eng.setOption(name, value);
    }

    @Override
    public void setDifficulty(int level) {
        this.difficultyLevel = Math.max(1, Math.min(10, level));
    }

    @Override
    public void setPositionFEN(String fen) throws Exception {
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
        // Map 1-10 to Nodes, Depth or Time
        // Level 0: handled by GameController (Random Move)
        // Level 1: nodes 1 (very bad)
        // Level 2: nodes 5
        // Level 3: nodes 20
        // Level 4: depth 1
        // Level 5: depth 2
        // Level 6: depth 4
        // Level 7: depth 10
        // Level 8: depth 16
        // Level 9: Time 1000ms
        // Level 10: Time 2000ms
        if (difficultyLevel <= 1) {
            return eng.goNodes(1, 5000).move();
        } else if (difficultyLevel == 2) {
            return eng.goNodes(5, 5000).move();
        } else if (difficultyLevel == 3) {
            return eng.goNodes(20, 5000).move();
        } else if (difficultyLevel == 4) {
            return eng.goDepth(1, 5000).move();
        } else if (difficultyLevel == 5) {
            return eng.goDepth(2, 5000).move();
        } else if (difficultyLevel == 6) {
            return eng.goDepth(4, 10000).move();
        } else if (difficultyLevel == 7) {
            return eng.goDepth(10, 20000).move();
        } else if (difficultyLevel == 8) {
            return eng.goDepth(16, 30000).move();
        } else if (difficultyLevel == 9) {
            return bestMoveMs(1000);
        } else {
            return bestMoveMs(2000);
        }
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
