package com.frommzay.chess.services.infrastructure.engine;

import com.frommzay.chess.services.infrastructure.engine.UciClient;
import com.frommzay.chess.services.infrastructure.engine.EngineService;

public final class SerendipityEngineService implements EngineService {
    private final String javaCmd;
    private final String jarPath;
    private UciClient eng;

    public SerendipityEngineService(String javaCmd, String jarPath) {
        this.javaCmd = javaCmd;
        this.jarPath = jarPath;
    }

    @Override public void start() throws Exception {
        eng = UciClient.launchJar(javaCmd, jarPath);
        // sensible defaults; change as you like
        safeSet("Threads", String.valueOf(Runtime.getRuntime().availableProcessors()));
        safeSet("Hash", "256");
        eng.newGame();
    }

    private void safeSet(String name, String value) {
        try { eng.setOption(name, value); } catch (Exception ignored) {}
    }

    @Override public void newGame() throws Exception { eng.newGame(); }

    @Override public void setOption(String name, String value) throws Exception {
        eng.setOption(name, value);
    }

    @Override public void setPositionFEN(String fen) throws Exception {
        if (fen == null || fen.isBlank()) return;
        eng.setPositionFEN(fen);
    }

    @Override public void pushUserMove(String uciMove) {
        eng.applyUserMove(uciMove);
    }

    @Override public String bestMoveMs(int movetimeMs) throws Exception {
        return eng.goMovetime(movetimeMs, /*buffer*/ 2000).move();
    }

    @Override public void close() {
        if (eng != null) eng.close();
    }
}
