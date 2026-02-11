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

public class StressTestAIvsAI {
    public static void main(String[] args) {
        System.out.println("Starting AI vs AI Stress Test (Level 5, 60 moves/side, Single Engine)...");
        SerendipityEngineService engine = null;

        try {
            // 1. Initialize Engine
            System.out.println("Initializing engine...");
            engine = new SerendipityEngineService();
            engine.start();

            // Set Difficulty 5
            engine.setDifficulty(5);
            System.out.println("Engine started at Difficulty 5.");

            // 2. Initialize Game
            // Standard start position
            GameState gameState = NotationFEN.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

            int halfMoves = 0;
            int maxHalfMoves = 120; // 60 moves per player

            while (halfMoves < maxHalfMoves) {
                boolean isWhiteTurn = gameState.getTurn() == PlayerColor.WHITE;
                SerendipityEngineService activeEngine = engine;

                String fen = NotationFEN.toFEN(gameState);
                // System.out.println("FEN: " + fen);

                activeEngine.setPositionFEN(fen);

                System.out.print("Move " + (halfMoves / 2 + 1) + " (" + (isWhiteTurn ? "WHITE" : "BLACK") + "): ");
                long start = System.currentTimeMillis();

                // Get Best Move
                String uci = activeEngine.bestMove();

                long dur = System.currentTimeMillis() - start;
                System.out.println("Engine picked " + uci + " in " + dur + "ms");

                if (uci == null || uci.isBlank()) {
                    System.err.println("CRASH/FAIL: Engine returned null or empty move!");
                    break;
                }

                // Apply Move
                Move m = decode(gameState, uci);
                if (m == null) {
                    System.err.println("CRASH/FAIL: Engine made illegal move: " + uci + " (FEN: " + fen + ")");
                    break;
                }
                gameState.applyMove(m);
                halfMoves++;

                // Check Game Over
                if (MoveGenerator.generateAllLegalMovesInTurn(gameState).isEmpty()) {
                    System.out.println("Game Over (Mate or Stalemate).");
                    break;
                }
            }

            if (halfMoves >= maxHalfMoves) {
                System.out.println("Test Completed Successfully (Reached move limit).");
            }

        } catch (Exception e) {
            System.err.println("TEST CRASHED WITH EXCEPTION:");
            e.printStackTrace();
        } finally {
            if (engine != null)
                engine.close();
        }
    }

    // --- Helper Methods ---

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
