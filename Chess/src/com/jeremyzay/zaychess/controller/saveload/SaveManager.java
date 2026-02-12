package com.jeremyzay.zaychess.controller.saveload;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.services.application.notation.NotationSAN;
import com.jeremyzay.zaychess.services.infrastructure.network.UciCodec;

import com.jeremyzay.zaychess.model.pieces.Piece;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Save/load manager for chess games.
 *
 * Provides functionality to serialize games to disk and
 * restore them later. Moves are written in UCI notation,
 * and loading is performed by replaying all moves from the
 * starting position.
 */
public final class SaveManager {
    private final GameController controller;

    /** Constructs a save manager bound to a given controller. */
    public SaveManager(GameController controller) {
        this.controller = controller;
    }

    /**
     * Saves the entire game as a list of UCI move strings.
     *
     * @param file destination file
     * @throws IOException if file writing fails
     */
    public void saveGame(File file) throws IOException {
        List<Move> moves = controller.getHistory().getMoves();
        List<String> lines = new ArrayList<>();
        for (Move m : moves) {
            lines.add(UciCodec.toUci(m));
        }
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
    }

    /**
     * Loads a game by resetting the {@link GameState} and replaying
     * all previously saved UCI moves.
     *
     * - Reads lines from the given file.
     * - Decodes each line as UCI notation.
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
        controller.getHistory().clear();

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            Move m = UciCodec.fromUci(line);
            if (m == null)
                continue;

            // For loaded moves, we need to determine the actual move type from board state
            Move actualMove = resolveMove(gs, m);

            // Snapshot game state for SAN notation
            GameState before = gs.copy();
            String san = NotationSAN.toSAN(before, actualMove);

            // Record in history
            controller.getHistory().record(gs, actualMove, before);

            // Detect capture for UI
            Piece capturedPiece = null;
            if (actualMove.getMoveType() == MoveType.EN_PASSANT) {
                int epRank = actualMove.getToPos().getRank();
                int epFile = actualMove.getToPos().getFile();
                var moverColor = before.getPieceColorAt(actualMove.getFromPos());
                int capturedRank = (moverColor == com.jeremyzay.zaychess.model.util.PlayerColor.WHITE) ? epRank + 1
                        : epRank - 1;
                capturedPiece = before.getPieceAt(capturedRank, epFile);
            } else {
                capturedPiece = before.getPieceAt(actualMove.getToPos());
            }

            // Apply move
            gs.applyMove(actualMove);

            // Update logs
            controller.getWireLog().add(line);
            controller.dispatchMoveInfo(san);

            // Sync captureLog and UI via recordCapture
            controller.recordCapture(capturedPiece);
        }

        // Refresh the GUI if it is present
        if (controller.getBoardPanel() != null) {
            controller.getBoardPanel().updateBoard(gs.getBoard());
        }
    }

    /**
     * Resolves a basic UCI move to a fully-typed move by examining board state.
     * UCI doesn't encode move type, so we infer it from the current position.
     */
    private Move resolveMove(GameState gs, Move uciMove) {
        var board = gs.getBoard();
        var fromPiece = board.getPieceAt(uciMove.getFromPos());
        var toPiece = board.getPieceAt(uciMove.getToPos());

        // If already promotion, keep it
        if (uciMove.getMoveType() == MoveType.PROMOTION) {
            return uciMove;
        }

        // Check for castling (king moving 2 squares)
        if (fromPiece instanceof com.jeremyzay.zaychess.model.pieces.King) {
            int fileDiff = Math.abs(uciMove.getToPos().getFile() - uciMove.getFromPos().getFile());
            if (fileDiff == 2) {
                return new Move(uciMove.getFromPos(), uciMove.getToPos(), MoveType.CASTLE);
            }
        }

        // Check for en passant (pawn diagonal capture to empty square)
        if (fromPiece instanceof com.jeremyzay.zaychess.model.pieces.Pawn) {
            int fileDiff = Math.abs(uciMove.getToPos().getFile() - uciMove.getFromPos().getFile());
            if (fileDiff == 1 && toPiece == null) {
                return new Move(uciMove.getFromPos(), uciMove.getToPos(), MoveType.EN_PASSANT);
            }
        }

        // Check for pawn promotion (pawn reaching back rank)
        if (fromPiece instanceof com.jeremyzay.zaychess.model.pieces.Pawn) {
            int toRank = uciMove.getToPos().getRank();
            if (toRank == 0 || toRank == 7) {
                // Default to queen if no promotion specified
                return Move.promotion(uciMove.getFromPos(), uciMove.getToPos(),
                        com.jeremyzay.zaychess.model.move.PromotionPiece.QUEEN);
            }
        }

        // Capture or normal
        if (toPiece != null) {
            return new Move(uciMove.getFromPos(), uciMove.getToPos(), MoveType.CAPTURE);
        }

        return new Move(uciMove.getFromPos(), uciMove.getToPos(), MoveType.NORMAL);
    }
}
