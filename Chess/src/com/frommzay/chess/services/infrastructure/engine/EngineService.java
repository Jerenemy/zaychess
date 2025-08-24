package com.frommzay.chess.services.infrastructure.engine;

public interface EngineService extends AutoCloseable {
    void start() throws Exception;              // launch engine
    void newGame() throws Exception;            // reset
    void setOption(String name, String value) throws Exception;
    void setPositionFEN(String fen) throws Exception; // or default startpos
    void pushUserMove(String uciMove);          // e.g., "e2e4"
    String bestMoveMs(int movetimeMs) throws Exception; // returns "e7e5" etc.
    @Override void close();                     // shutdown
}
