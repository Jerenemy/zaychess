package com.jeremyzay.zaychess.tests;

import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveGenerator;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.services.application.notation.NotationFEN;
import com.jeremyzay.zaychess.services.infrastructure.engine.SerendipityEngineService;
import java.util.List;
import java.util.Random;

public class StressTestEngine {
    public static void main(String[] args) {
        System.out.println("Starting Stress Test (100 moves)...");
        SerendipityEngineService service = null;
        try {
            // Initialize GameState with standard start position.
            // Using FEN string directly as NotationFEN.STARTING_FEN is not exposed (checked
            // file content).
            GameState gameState = NotationFEN.fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

            service = new SerendipityEngineService();
            service.start();
            service.setDifficulty(1); // Aggressive (depth 2)

            // Random rand = new Random(); // Unused if we stick to engine vs itself?
            // Actually, let's make the engine play BOTH sides?
            // No, GameController logic applies only to one side usually?
            // User requested: "write a test with multiple consecutive moves".
            // If Engine plays white, and random plays black.
            // Or Engine plays both.
            // Engine vs Self is easiest to implement loop.

            int moves = 0;

            while (moves < 100) {
                // Sync engine with current state
                // Try to clear engine state to prevent hang
                service.newGame();

                String fen = NotationFEN.toFEN(gameState);
                service.setPositionFEN(fen);

                System.out.print("Move " + (moves + 1) + " (" + gameState.getTurn() + "): ");
                long start = System.currentTimeMillis();

                // This call might hang if the bug is present
                String uci = service.bestMove();

                long dur = System.currentTimeMillis() - start;
                System.out.println("Engine picked " + uci + " in " + dur + "ms");

                // Apply move to local state to continue game
                Move m = decode(gameState, uci);
                if (m == null) {
                    System.err.println("Engine made illegal move or null: " + uci);
                    break;
                }
                gameState.applyMove(m);
                moves++;

                // Check if game over
                if (MoveGenerator.generateAllLegalMovesInTurn(gameState).isEmpty()) {
                    System.out.println("Game Over (Mate or Stalemate).");
                    break;
                }
            }

            System.out.println("Stress Test Completed Successfully.");

        } catch (Exception e) {
            System.err.println("STRESS TEST FAILED with exception:");
            e.printStackTrace();
        } finally {
            if (service != null)
                service.close();
        }
    }

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
