package com.jeremyzay.zaychess.tests;

import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveGenerator;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.services.application.notation.NotationFEN;
import com.jeremyzay.zaychess.services.infrastructure.engine.SerendipityEngineService;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import java.util.List;

public class StressTestAllLevels {
    public static void main(String[] args) {
        System.out.println("Starting Comprehensive AI Stress Test (Levels 1-10)...");

        for (int level = 1; level <= 10; level++) {
            System.out.println("\n------------------------------------------------");
            System.out.println("TESTING LEVEL " + level);
            System.out.println("------------------------------------------------");

            if (!runTestForLevel(level)) {
                System.err.println("FATAL: Level " + level + " FAILED. Aborting suite.");
                System.exit(1);
            }
            System.out.println("SUCCESS: Level " + level + " PASSED.");
        }

        System.out.println("\n================================================");
        System.out.println("ALL LEVELS (1-10) PASSED STABILITY VERIFICATION.");
        System.out.println("================================================");
    }

    private static boolean runTestForLevel(int level) {
        SerendipityEngineService engine = null;
        try {
            engine = new SerendipityEngineService();
            engine.start();
            engine.setDifficulty(level);

            // Standard Start Position
            GameState gameState = NotationFEN.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

            int maxMoves = 20; // 10 full moves
            int moveCount = 0;

            while (moveCount < maxMoves) {
                // Prepare Engine
                String fen = NotationFEN.toFEN(gameState);
                engine.setPositionFEN(fen);

                long start = System.currentTimeMillis();
                try {
                    String uci = engine.bestMove();
                    long dur = System.currentTimeMillis() - start;

                    if (uci == null || uci.isBlank()) {
                        System.err.println("Level " + level + " Error: Null/Empty move returned.");
                        return false;
                    }

                    System.out.println("Level " + level + " Move " + (moveCount + 1) + ": " + uci + " (" + dur + "ms)");

                    Move m = decode(gameState, uci);
                    if (m == null) {
                        System.err.println("Level " + level + " Error: Illegal move " + uci);
                        return false;
                    }
                    gameState.applyMove(m);
                    moveCount++;

                    if (MoveGenerator.generateAllLegalMovesInTurn(gameState).isEmpty()) {
                        System.out.println("Level " + level + ": Game Over early.");
                        break;
                    }

                } catch (Exception e) {
                    System.err.println("Level " + level + " CRASHED: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (engine != null)
                engine.close();
        }
    }

    // --- Helpers ---
    private static Move decode(GameState state, String uci) {
        if (uci == null || uci.length() < 4)
            return null;
        uci = uci.trim();
        List<Move> legal = MoveGenerator.generateAllLegalMovesInTurn(state);
        for (Move m : legal) {
            if (encode(m).equals(uci))
                return m;
        }
        return null;
    }

    private static String encode(Move m) {
        String s = toAlgebraic(m.getFromPos()) + toAlgebraic(m.getToPos());
        if (m.getMoveType() == MoveType.PROMOTION) {
            PromotionPiece pp = m.getPromotion();
            String pChar = "q";
            if (pp != null) {
                switch (pp) {
                    case QUEEN:
                        pChar = "q";
                        break;
                    case ROOK:
                        pChar = "r";
                        break;
                    case BISHOP:
                        pChar = "b";
                        break;
                    case KNIGHT:
                        pChar = "n";
                        break;
                }
            }
            return s + pChar;
        }
        return s;
    }

    private static String toAlgebraic(com.jeremyzay.zaychess.model.util.Position p) {
        char file = (char) ('a' + p.getFile());
        char rank = (char) ('8' - p.getRank());
        return "" + file + rank;
    }
}
