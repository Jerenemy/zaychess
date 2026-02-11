package com.jeremyzay.zaychess.services.infrastructure.engine;

import java.util.List;

public interface EngineService extends AutoCloseable {
    void start() throws Exception; // launch engine

    void newGame() throws Exception; // reset

    void setOption(String name, String value) throws Exception;

    void setDifficulty(int level); // 1-10

    void setPositionFEN(String fen) throws Exception; // or default startpos

    void pushUserMove(String uciMove); // e.g., "e2e4"

    String bestMoveMs(int movetimeMs) throws Exception; // returns "e7e5" etc.

    String bestMove() throws Exception; // returns "e7e5" based on difficulty

    String bestMove(List<String> searchMoves) throws Exception;

    @Override
    void close(); // shutdown
}
