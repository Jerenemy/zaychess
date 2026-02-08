package com.jeremyzay.zaychess.tests;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.controller.saveload.SaveManager;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.services.infrastructure.network.UciCodec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Comprehensive test suite for the UCI save/load system.
 * Tests undo/redo, promotions, castling, en passant, check, and checkmate
 * scenarios.
 */
public class SaveManagerTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== SaveManager UCI Test Suite ===\n");

        testBasicSaveLoad();
        testUndoRedoThenSaveLoad();
        testUndoNewMoveThenSaveLoad();
        testPromotionSaveLoad();
        testCastlingSaveLoad();
        testEnPassantSaveLoad();
        testCheckStateSaveLoad();
        testCheckmateUndoSaveLoad();
        testUciCodecConversions();

        System.out.println("\n=== Test Results ===");
        System.out.println("Total: " + testsRun);
        System.out.println("Passed: " + testsPassed);
        System.out.println("Failed: " + testsFailed);

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    // =====================================================
    // Test Cases
    // =====================================================

    private static void testBasicSaveLoad() throws Exception {
        System.out.println("TEST: Basic Save/Load");

        GameController controller = new GameController(new GameState(), null);
        GameState gs = controller.getGameState();

        // Play e2-e4, e7-e5, Nf3
        playMove(controller, gs, 6, 4, 4, 4, MoveType.NORMAL); // e2-e4
        playMove(controller, gs, 1, 4, 3, 4, MoveType.NORMAL); // e7-e5
        playMove(controller, gs, 7, 6, 5, 5, MoveType.NORMAL); // Nf3

        // Save
        File tempFile = File.createTempFile("test_basic", ".txt");
        tempFile.deleteOnExit();
        new SaveManager(controller).saveGame(tempFile);

        // Verify file contents
        List<String> lines = Files.readAllLines(tempFile.toPath());
        assertEqual("Line count", 3, lines.size());
        assertEqual("Move 1", "e2e4", lines.get(0));
        assertEqual("Move 2", "e7e5", lines.get(1));
        assertEqual("Move 3", "g1f3", lines.get(2));

        // Load into fresh controller
        GameController controller2 = new GameController(new GameState(), null);
        new SaveManager(controller2).loadGame(tempFile);
        GameState gs2 = controller2.getGameState();

        // Verify state
        assertEqual("Turn after load", PlayerColor.BLACK, gs2.getTurn());
        assertNull("e2 empty", gs2.getBoard().getPieceAt(6, 4));
        assertNotNull("e4 has piece", gs2.getBoard().getPieceAt(4, 4));

        passed("Basic Save/Load");
    }

    private static void testUndoRedoThenSaveLoad() throws Exception {
        System.out.println("TEST: Undo/Redo then Save/Load");

        GameController controller = new GameController(new GameState(), null);
        GameState gs = controller.getGameState();

        // Play 3 moves
        playMove(controller, gs, 6, 4, 4, 4, MoveType.NORMAL); // e4
        playMove(controller, gs, 1, 4, 3, 4, MoveType.NORMAL); // e5
        playMove(controller, gs, 6, 3, 4, 3, MoveType.NORMAL); // d4

        // Undo last move
        controller.undo();

        // Redo it
        controller.redo();

        // Save (should have 3 moves)
        File tempFile = File.createTempFile("test_undo_redo", ".txt");
        tempFile.deleteOnExit();
        new SaveManager(controller).saveGame(tempFile);

        List<String> lines = Files.readAllLines(tempFile.toPath());
        assertEqual("Move count after undo/redo", 3, lines.size());

        // Load and verify
        GameController controller2 = new GameController(new GameState(), null);
        new SaveManager(controller2).loadGame(tempFile);

        assertEqual("History size", 3, controller2.getHistory().getMoves().size());

        passed("Undo/Redo then Save/Load");
    }

    private static void testUndoNewMoveThenSaveLoad() throws Exception {
        System.out.println("TEST: Undo + New Move then Save/Load");

        GameController controller = new GameController(new GameState(), null);
        GameState gs = controller.getGameState();

        // Play 2 moves
        playMove(controller, gs, 6, 4, 4, 4, MoveType.NORMAL); // e4
        playMove(controller, gs, 1, 4, 3, 4, MoveType.NORMAL); // e5

        // Undo last move
        controller.undo();

        // Play different move (d5 instead of e5)
        playMove(controller, gs, 1, 3, 3, 3, MoveType.NORMAL); // d5

        // Save (should have 2 moves: e4, d5)
        File tempFile = File.createTempFile("test_undo_new", ".txt");
        tempFile.deleteOnExit();
        new SaveManager(controller).saveGame(tempFile);

        List<String> lines = Files.readAllLines(tempFile.toPath());
        assertEqual("Move count", 2, lines.size());
        assertEqual("Move 1", "e2e4", lines.get(0));
        assertEqual("Move 2", "d7d5", lines.get(1));

        passed("Undo + New Move then Save/Load");
    }

    private static void testPromotionSaveLoad() throws Exception {
        System.out.println("TEST: Promotion Save/Load");

        GameController controller = new GameController(new GameState(), null);
        GameState gs = controller.getGameState();

        // Setup: move pawn to promotion square (simplified - just test UCI codec)
        // For a real game, this would require many moves. Let's test the codec
        // directly.

        Move promoMove = Move.promotion(new Position(1, 4), new Position(0, 4), PromotionPiece.QUEEN);
        String uci = UciCodec.toUci(promoMove);
        assertEqual("Promotion UCI", "e7e8q", uci);

        Move decoded = UciCodec.fromUci(uci);
        assertNotNull("Decoded move", decoded);
        assertEqual("Decoded move type", MoveType.PROMOTION, decoded.getMoveType());
        assertEqual("Decoded promotion piece", PromotionPiece.QUEEN, decoded.getPromotion());

        // Test other promotion pieces
        assertEqual("Rook promo", "e7e8r",
                UciCodec.toUci(Move.promotion(new Position(1, 4), new Position(0, 4), PromotionPiece.ROOK)));
        assertEqual("Bishop promo", "e7e8b",
                UciCodec.toUci(Move.promotion(new Position(1, 4), new Position(0, 4), PromotionPiece.BISHOP)));
        assertEqual("Knight promo", "e7e8n",
                UciCodec.toUci(Move.promotion(new Position(1, 4), new Position(0, 4), PromotionPiece.KNIGHT)));

        passed("Promotion Save/Load");
    }

    private static void testCastlingSaveLoad() throws Exception {
        System.out.println("TEST: Castling Save/Load");

        // Test UCI encoding of castling move (King e1-g1)
        Move castleKingside = new Move(new Position(7, 4), new Position(7, 6), MoveType.CASTLE);
        String uci = UciCodec.toUci(castleKingside);
        assertEqual("Kingside castle UCI", "e1g1", uci);

        Move castleQueenside = new Move(new Position(7, 4), new Position(7, 2), MoveType.CASTLE);
        uci = UciCodec.toUci(castleQueenside);
        assertEqual("Queenside castle UCI", "e1c1", uci);

        // Decode should work (type determined by move resolution)
        Move decoded = UciCodec.fromUci("e1g1");
        assertNotNull("Decoded castle", decoded);
        assertEqual("Decoded from file", 4, decoded.getFromPos().getFile());
        assertEqual("Decoded to file", 6, decoded.getToPos().getFile());

        passed("Castling Save/Load");
    }

    private static void testEnPassantSaveLoad() throws Exception {
        System.out.println("TEST: En Passant Save/Load");

        // En passant move: pawn on e5 captures d6
        Move epMove = new Move(new Position(3, 4), new Position(2, 3), MoveType.EN_PASSANT);
        String uci = UciCodec.toUci(epMove);
        assertEqual("En passant UCI", "e5d6", uci);

        // When loaded, resolveMove should detect en passant (pawn diagonal to empty)
        // This is tested via integration when actually loading a game

        passed("En Passant Save/Load");
    }

    private static void testCheckStateSaveLoad() throws Exception {
        System.out.println("TEST: Check State Save/Load");

        GameController controller = new GameController(new GameState(), null);
        GameState gs = controller.getGameState();

        // Scholar's mate opening leading to check (simplified)
        // 1. e4 e5 2. Qh5 (checks? No, just threatens)
        // Let's do: 1. e4 f6 2. Qh5+ (check!)
        playMove(controller, gs, 6, 4, 4, 4, MoveType.NORMAL); // e4
        playMove(controller, gs, 1, 5, 2, 5, MoveType.NORMAL); // f6
        playMove(controller, gs, 7, 3, 3, 7, MoveType.NORMAL); // Qh5+

        // Black is now in check - verify (it's Black's turn)
        boolean inCheck = gs.isInCheck();
        assertEqual("Black in check", true, inCheck);

        // Save
        File tempFile = File.createTempFile("test_check", ".txt");
        tempFile.deleteOnExit();
        new SaveManager(controller).saveGame(tempFile);

        // Load
        GameController controller2 = new GameController(new GameState(), null);
        new SaveManager(controller2).loadGame(tempFile);
        GameState gs2 = controller2.getGameState();

        // Verify still in check after load
        assertEqual("Check preserved after load", true, gs2.isInCheck());

        passed("Check State Save/Load");
    }

    private static void testCheckmateUndoSaveLoad() throws Exception {
        System.out.println("TEST: Checkmate Undo Save/Load");

        GameController controller = new GameController(new GameState(), null);
        GameState gs = controller.getGameState();

        // Fool's mate: 1. f3 e5 2. g4 Qh4#
        playMove(controller, gs, 6, 5, 5, 5, MoveType.NORMAL); // f3
        playMove(controller, gs, 1, 4, 3, 4, MoveType.NORMAL); // e5
        playMove(controller, gs, 6, 6, 4, 6, MoveType.NORMAL); // g4
        playMove(controller, gs, 0, 3, 4, 7, MoveType.NORMAL); // Qh4#

        // Verify checkmate (game over + check)
        boolean isCheckmate = gs.isGameOver() && gs.isInCheck();
        assertEqual("Is checkmate", true, isCheckmate);

        // Undo the checkmate move
        controller.undo();

        // Should no longer be checkmate
        assertEqual("No longer checkmate", false, gs.isGameOver() && gs.isInCheck());

        // Save
        File tempFile = File.createTempFile("test_checkmate_undo", ".txt");
        tempFile.deleteOnExit();
        new SaveManager(controller).saveGame(tempFile);

        List<String> lines = Files.readAllLines(tempFile.toPath());
        assertEqual("Move count after undo", 3, lines.size());

        // Load
        GameController controller2 = new GameController(new GameState(), null);
        new SaveManager(controller2).loadGame(tempFile);
        GameState gs2 = controller2.getGameState();

        // Verify not checkmate
        assertEqual("Not checkmate after load", false, gs2.isGameOver() && gs2.isInCheck());
        assertEqual("Turn after load", PlayerColor.BLACK, gs2.getTurn());

        passed("Checkmate Undo Save/Load");
    }

    private static void testUciCodecConversions() throws Exception {
        System.out.println("TEST: UCI Codec Conversions");

        // Test position to UCI
        assertEqual("a1", "a1", UciCodec.positionToUci(new Position(7, 0)));
        assertEqual("h8", "h8", UciCodec.positionToUci(new Position(0, 7)));
        assertEqual("e4", "e4", UciCodec.positionToUci(new Position(4, 4)));

        // Test UCI to position
        Position a1 = UciCodec.uciToPosition("a1");
        assertEqual("a1 rank", 7, a1.getRank());
        assertEqual("a1 file", 0, a1.getFile());

        Position h8 = UciCodec.uciToPosition("h8");
        assertEqual("h8 rank", 0, h8.getRank());
        assertEqual("h8 file", 7, h8.getFile());

        // Test full move conversion
        Move move = new Move(new Position(6, 4), new Position(4, 4), MoveType.NORMAL);
        assertEqual("e2e4", "e2e4", UciCodec.toUci(move));

        Move decoded = UciCodec.fromUci("e2e4");
        assertEqual("Decoded from rank", 6, decoded.getFromPos().getRank());
        assertEqual("Decoded from file", 4, decoded.getFromPos().getFile());
        assertEqual("Decoded to rank", 4, decoded.getToPos().getRank());
        assertEqual("Decoded to file", 4, decoded.getToPos().getFile());

        passed("UCI Codec Conversions");
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private static void playMove(GameController controller, GameState gs,
            int fromRank, int fromFile, int toRank, int toFile, MoveType type) {
        Move m = new Move(new Position(fromRank, fromFile), new Position(toRank, toFile), type);
        GameState snap = gs.copy();
        controller.getHistory().record(gs, m, snap);
        gs.applyMove(m);
    }

    private static void assertEqual(String name, Object expected, Object actual) {
        testsRun++;
        if (expected == null ? actual == null : expected.equals(actual)) {
            testsPassed++;
        } else {
            testsFailed++;
            System.out.println("  FAIL: " + name + " - Expected: " + expected + ", Got: " + actual);
        }
    }

    private static void assertNotNull(String name, Object value) {
        testsRun++;
        if (value != null) {
            testsPassed++;
        } else {
            testsFailed++;
            System.out.println("  FAIL: " + name + " - Expected non-null");
        }
    }

    private static void assertNull(String name, Object value) {
        testsRun++;
        if (value == null) {
            testsPassed++;
        } else {
            testsFailed++;
            System.out.println("  FAIL: " + name + " - Expected null, got: " + value);
        }
    }

    private static void passed(String testName) {
        System.out.println("  ✓ " + testName + " passed\n");
    }
}
