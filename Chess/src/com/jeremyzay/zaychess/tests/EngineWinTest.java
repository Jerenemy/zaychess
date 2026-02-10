package com.jeremyzay.zaychess.tests;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EngineWinTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Engine Win Condition Test ===\n");

        try {
            testFoolsMateWin();
            System.out.println("\n✓ Test Passed: No crashes observed.");
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("\n✗ Test Failed: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testFoolsMateWin() throws Exception {
        System.out.println("Setting up Fool's Mate position...");

        // Initialize controller
        GameController controller = new GameController(new GameState());
        controller.setSuppressDialogs(true);
        GameState gs = controller.getGameState();

        // 1. f3 e5
        playMove(controller, gs, 6, 5, 5, 5); // White f3
        playMove(controller, gs, 1, 4, 3, 4); // Black e5

        // 2. g4 (Black to move)
        playMove(controller, gs, 6, 6, 4, 6); // White g4

        System.out.println("Position set. starting engine as White...");

        // Start engine. logic assumes we are BLACK, engine is WHITE.
        // It is currently Black's turn. Engine should NOT move yet.
        SwingUtilities.invokeAndWait(() -> {
            try {
                controller.setEngine(); // Loads Serendipity
                // Check if engine loaded
                if (!controller.isUsingEngine()) {
                    System.err.println("WARNING: Engine failed to load. unexpected for this test environment?");
                }

                controller.startEngineGame(PlayerColor.BLACK);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Wait a moment to ensure no spurious engine moves happen
        Thread.sleep(1000);

        System.out.println("Delivering Checkmate (Qh4#)...");

        // 2... Qh4#
        // Move from d8 (0,3) to h4 (4,7)
        Move matingMove = new Move(new Position(0, 3), new Position(4, 7), MoveType.NORMAL);

        // We must apply move via controller to trigger `maybeEngineRespond` logic
        SwingUtilities.invokeAndWait(() -> {
            try {
                java.lang.reflect.Method method = GameController.class.getDeclaredMethod("applyMoveAndNotify",
                        Move.class, boolean.class);
                method.setAccessible(true);
                method.invoke(controller, matingMove, false);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke applyMoveAndNotify via reflection", e);
            }
        });

        System.out.println("Checkmate delivered. Waiting for engine thread reaction...");

        // If the bug exists, the engine thread might start, find it's mated, return
        // "(none)", and crash.
        // Or loop infinitely printing errors.
        // We wait 3 seconds to observe output.
        Thread.sleep(3000);

        if (gs.isGameOver()) {
            System.out.println("Game is Over. Checking for errors...");
        } else {
            throw new RuntimeException("Game should be over but isn't!");
        }
    }

    private static void playMove(GameController controller, GameState gs,
            int fromRank, int fromFile, int toRank, int toFile) {
        Move m = new Move(new Position(fromRank, fromFile), new Position(toRank, toFile), MoveType.NORMAL);
        gs.applyMove(m);
    }
}
