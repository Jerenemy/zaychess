package com.jeremyzay.zaychess.controller.saveload;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.*;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.services.application.notation.NotationSAN;
import com.jeremyzay.zaychess.services.infrastructure.network.MoveCodec;
import com.jeremyzay.zaychess.services.infrastructure.network.MoveMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Save/load manager for chess games.
 *
 * Provides functionality to serialize games to disk and
 * restore them later. Moves are written in a compact, machine-readable
 * format via {@link MoveCodec}, and loading is performed by replaying
 * all moves from the starting position.
 */
public final class SaveManager {
    private final GameController controller;

    /** Constructs a save manager bound to a given controller. */
    public SaveManager(GameController controller) {
        this.controller = controller;
    }

    /**
     * Saves the entire game as a list of encoded moves.
     *
     * @param file destination file
     * @throws IOException if file writing fails
     */
    public void saveGame(File file) throws IOException {
        List<String> lines = controller.getWireLog();
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    /**
     * Loads a game by resetting the {@link GameState} and replaying
     * all previously encoded moves.
     *
     * - Reads lines from the given file.
     * - Decodes each line into a {@link MoveMessage}.
     * - Converts the message into a {@link Move}.
     * - Applies the move to the {@link GameState}.
     * - Updates SAN notation log and refreshes the GUI board panel.
     *
     * @param file source file
     * @throws IOException if file reading fails
     */
    public void loadGame(File file) throws IOException {
        GameState gs = controller.getGameState();

        // Reset game to the initial position
        gs.restoreFrom(new GameState());

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        for (String line : lines) {
            MoveMessage mm = MoveCodec.tryDecode(line);
            if (mm == null) continue;

            // Decode "from" and "to" positions
            Position from = new Position(mm.fromRank(), mm.fromFile());
            Position to   = new Position(mm.toRank(), mm.toFile());

            // Determine move type, handle promotion if present
            MoveType type;
            PromotionPiece promo = null;
            String t = mm.type();
            if (t != null && t.startsWith("PROMOTION")) {
                type = MoveType.PROMOTION;
                int i = t.indexOf(':');
                if (i > 0 && i + 1 < t.length()) {
                    promo = PromotionPiece.valueOf(t.substring(i + 1));
                }
            } else {
                type = MoveType.valueOf(t);
            }

            // Construct move object
            Move m = new Move(from, to, type, promo);

            // Snapshot game state for SAN notation
            GameState before = gs.copy();
            String san = NotationSAN.toSAN(before, m);

            // Apply move and update logs
            gs.applyMove(m);
            controller.getWireLog().add(line);
            controller.dispatchMoveInfo(san);
        }

        // Refresh the GUI if it is present
        if (controller.getBoardPanel() != null) {
            controller.getBoardPanel().updateBoard(gs.getBoard());
        }
    }
}
