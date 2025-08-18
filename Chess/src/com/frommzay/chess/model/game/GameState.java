package com.frommzay.chess.model.game;

import java.util.List;

import com.frommzay.chess.model.board.Board;
import com.frommzay.chess.model.board.BoardFactory;
import com.frommzay.chess.model.move.Move;
import com.frommzay.chess.model.move.MoveType;
import com.frommzay.chess.model.move.SpecialMoveHandler;
import com.frommzay.chess.model.pieces.*;
import com.frommzay.chess.model.util.PlayerColor;
import com.frommzay.chess.model.util.Position;
import com.frommzay.chess.model.rules.GameOverType;
import com.frommzay.chess.model.move.MoveGenerator;

/**
 * Represents the full mutable state of a chess game.
 * 
 * Holds the board, turn information, and special-move state
 * such as castling rights and en passant targets.
 * Provides methods to apply moves, detect game-over conditions,
 * query pieces, and create deep copies for undo/redo or simulation.
 */
public class GameState {
    private final Board board;
    private PlayerColor turn;
    private final SpecialMoveHandler specialMoveHandler;

    /** Creates a new game state with the default chess starting position. */
    public GameState () {
        this.board = BoardFactory.createDefaultBoard();
        this.turn = PlayerColor.WHITE;
        this.specialMoveHandler = new SpecialMoveHandler();
    }

    /** Deep-copy constructor. Copies board, turn, and special-move state. */
    public GameState(GameState other) {
        this.turn  = other.turn;
        this.board = new Board(other.board);
        this.specialMoveHandler = new SpecialMoveHandler(other.specialMoveHandler);
    }

    /** @return a fresh deep copy of this game state */
    public GameState copy() {
        return new GameState(this);
    }

    /** @return whose turn it is to move */
    public PlayerColor getTurn() { return turn; }

    /** @return the underlying board */
    public Board getBoard() { return board; }

    /** @return the special-move handler for castling/en passant flags */
    public SpecialMoveHandler getSpecialMoveHandler() { return specialMoveHandler; }

    /** @return true if it is the given color's turn */
    public boolean isTurn(PlayerColor color) { return getTurn() == color; }

    /** Switches the turn from white to black or black to white. */
    public void changeTurn() {
        turn = (turn == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
    }

    /** Retrieves the piece at the given position. */
    public Piece getPieceAt(Position pos) { return board.getPieceAt(pos); }

    /** Retrieves the piece at the given coordinates. */
    public Piece getPieceAt(int rank, int file) { return board.getPieceAt(rank, file); }

    /**
     * Applies a move to the game state.
     * Updates special-move flags, performs captures, handles castling,
     * promotions, en passant, and updates the board and turn.
     *
     * @param m the move to apply
     */
    public void applyMove(Move m) {
        specialMoveHandler.updateHasMovedFlags(this, m);
        specialMoveHandler.updateEnPassantTarget(this, m);
        Position from = m.getFromPos();
        Position to = m.getToPos();
        Piece mover = getPieceAt(from);

        switch (m.getMoveType()) {
            case MoveType.EN_PASSANT:
                int toRank = m.getToPos().getRank();
                int toFile = m.getToPos().getFile();
                switch (getPieceColorAt(m)) {
                    case PlayerColor.BLACK -> setPieceAt(toRank - 1, toFile, null);
                    case PlayerColor.WHITE -> setPieceAt(toRank + 1, toFile, null);
                }
                break;
            case MoveType.CASTLE:
                specialMoveHandler.moveRookDuringCastle(this, m);
                break;
            case MoveType.PROMOTION:
                if (m.getPromotion() == null) break; // skip if only probing
                PlayerColor color = board.getPieceAt(m.getFromPos()).getColor();
                mover = switch (m.getPromotion()) {
                    case QUEEN  -> new Queen(color, to);
                    case ROOK   -> new Rook(color, to);
                    case BISHOP -> new Bishop(color, to);
                    case KNIGHT -> new Knight(color, to);
                };
                break;
            default:
                break;
        }

        setPieceAt(from, null);
        setPieceAt(m.getToPos(), mover);
        changeTurn();
    }

    /** @return the runtime class type of the piece at the origin of a move */
    public Class<? extends Piece> getPieceTypeAt(Move m) {
        return getPieceAt(m.getFromPos()).getClass();
    }

    /** @return the color of the piece at the origin of a move */
    public PlayerColor getPieceColorAt(Move m) {
        return getPieceAt(m.getFromPos()).getColor();
    }

    /**
     * Determines whether the game is over.
     * A game is over if the current player has no legal moves.
     *
     * @return true if no legal moves remain
     */
    public boolean isGameOver() {
        List<Piece> colorPieces = getPiecesOfColor(turn);
        for (Piece piece : colorPieces) {
            if (!(MoveGenerator.generateLegalMoves(this, piece.getPos())).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Assumes the game is over and determines the type.
     *
     * @return CHECKMATE if current player's king is in check,
     *         DRAW otherwise
     */
    public GameOverType getGameOverType() {
        if (getKingOfColor(turn).isInCheck(this)) return GameOverType.CHECKMATE;
        else return GameOverType.DRAW;
    }

    /** @return list of all pieces of the given color */
    public List<Piece> getPiecesOfColor(PlayerColor color) {
        return board.getPiecesOfColor(color);
    }

    /** @return the king piece of the given color (never null in valid states) */
    public King getKingOfColor(PlayerColor color) {
        List<Piece> pieces = board.getPiecesOfColor(color);
        for (Piece piece : pieces) {
            if (piece instanceof King) return (King) piece;
        }
        return null; // should never happen
    }

    /**
     * Assumes the game is over and returns the winning color.
     *
     * @return the opposite of the current turn if checkmate,
     *         null if drawn
     */
    public PlayerColor getWinner() {
        if (getGameOverType() == GameOverType.CHECKMATE) return turn.getOpposite();
        return null;
    }

    /** @return true if the current player is in check */
    public boolean isInCheck() {
        return getKingOfColor(turn).isInCheck(this);
    }

    /**
     * Places a piece on the board at the given position and updates its coordinates.
     */
    public void setPieceAt(Position pos, Piece piece) {
        getBoard().setPieceAt(pos, piece);
        if (piece != null) piece.updateCoords(pos);
    }

    /**
     * Places a piece on the board at the given coordinates and updates its coordinates.
     */
    public void setPieceAt(int rank, int file, Piece piece) {
        getBoard().setPieceAt(rank, file, piece);
        if (piece != null) piece.updateCoords(rank, file);
    }

    /** @return the color of the piece at the given position */
    public PlayerColor getPieceColorAt(Position from) {
        return getPieceAt(from).getColor();
    }

    /** @return a deep snapshot of this game state */
    public GameState snapshot() {
        return new GameState(this);
    }

    /**
     * Restores this state from another snapshot.
     * Copies turn, board contents, and special-move flags.
     *
     * @param snap the snapshot to restore from
     */
    public void restoreFrom(GameState snap) {
        this.turn = snap.turn;
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                Piece p = snap.board.getPieceAt(r, f);
                setPieceAt(r, f, p == null ? null : p.copy());
            }
        }
        this.specialMoveHandler.copyFrom(snap.specialMoveHandler);
    }
}
