package com.jeremyzay.zaychess.model.game;

import java.util.List;

import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.board.BoardFactory;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.move.SpecialMoveHandler;
import com.jeremyzay.zaychess.model.pieces.*;
import com.jeremyzay.zaychess.model.pieces.*;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.model.rules.GameOverType;
import com.jeremyzay.zaychess.model.move.MoveGenerator;

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
    private int halfmoveClock;
    private int fullmoveNumber;
    private final java.util.List<String> positionHistory = new java.util.ArrayList<>();
    private PlayerColor resignedColor = null;
    private boolean drawAgreed = false;

    /** Creates a new game state with the default chess starting position. */
    public GameState() {
        this.board = BoardFactory.createDefaultBoard();
        this.turn = PlayerColor.WHITE;
        this.specialMoveHandler = new SpecialMoveHandler();
        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;
        this.positionHistory.add(com.jeremyzay.zaychess.services.application.notation.FenGenerator.toPositionFen(this));
    }

    /** Deep-copy constructor. Copies board, turn, and special-move state. */
    public GameState(GameState other) {
        this.turn = other.turn;
        this.board = new Board(other.board);
        this.specialMoveHandler = new SpecialMoveHandler(other.specialMoveHandler);
        this.halfmoveClock = other.halfmoveClock;
        this.fullmoveNumber = other.fullmoveNumber;
        this.positionHistory.addAll(other.positionHistory);
        this.resignedColor = other.resignedColor;
        this.drawAgreed = other.drawAgreed;
    }

    /** @return a fresh deep copy of this game state */
    public GameState copy() {
        return new GameState(this);
    }

    /** @return whose turn it is to move */
    public PlayerColor getTurn() {
        return turn;
    }

    /** @return the underlying board */
    public Board getBoard() {
        return board;
    }

    /** @return the special-move handler for castling/en passant flags */
    public SpecialMoveHandler getSpecialMoveHandler() {
        return specialMoveHandler;
    }

    /** @return the halfmove clock for the 50-move rule */
    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    /** @return the fullmove number */
    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    /** @return true if it is the given color's turn */
    public boolean isTurn(PlayerColor color) {
        return getTurn() == color;
    }

    /** Switches the turn from white to black or black to white. */
    public void changeTurn() {
        turn = (turn == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
    }

    /** Retrieves the piece at the given position. */
    public Piece getPieceAt(Position pos) {
        return board.getPieceAt(pos);
    }

    /** Retrieves the piece at the given coordinates. */
    public Piece getPieceAt(int rank, int file) {
        return board.getPieceAt(rank, file);
    }

    /**
     * Applies a move to the game state.
     * Updates special-move flags, performs captures, handles castling,
     * promotions, en passant, and updates the board and turn.
     *
     * @param m the move to apply
     */
    public void applyMove(Move m) {
        Position from = m.getFromPos();
        Position to = m.getToPos();
        Piece mover = getPieceAt(from);
        Piece captured = getPieceAt(to);

        // Reset halfmove clock if pawn moves or capture occurs
        if (mover instanceof Pawn || captured != null || m.getMoveType() == MoveType.EN_PASSANT) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }

        // Increment fullmove number after Black moves
        if (turn == PlayerColor.BLACK) {
            fullmoveNumber++;
        }

        specialMoveHandler.updateHasMovedFlags(this, m);
        specialMoveHandler.updateEnPassantTarget(this, m);

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
                if (m.getPromotion() == null)
                    break; // skip if only probing
                PlayerColor color = board.getPieceAt(m.getFromPos()).getColor();
                mover = switch (m.getPromotion()) {
                    case QUEEN -> new Queen(color, to);
                    case ROOK -> new Rook(color, to);
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

        // Add new position to history (ignoring move clocks for repetition)
        positionHistory.add(com.jeremyzay.zaychess.services.application.notation.FenGenerator.toPositionFen(this));
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
        // 1. Check if any legal moves remain
        boolean noMoves = true;
        List<Piece> colorPieces = getPiecesOfColor(turn);
        for (Piece piece : colorPieces) {
            if (!(MoveGenerator.generateLegalMoves(this, piece.getPos())).isEmpty()) {
                noMoves = false;
                break;
            }
        }
        if (noMoves)
            return true;

        // 2. Draw conditions
        if (isInsufficientMaterial())
            return true;
        if (isThreefoldRepetition())
            return true;
        if (isFiftyMoveRule())
            return true;

        if (resignedColor != null)
            return true;

        return drawAgreed;
    }

    public void setDrawAgreed(boolean agreed) {
        this.drawAgreed = agreed;
    }

    public boolean isDrawAgreed() {
        return drawAgreed;
    }

    public void resign(PlayerColor color) {
        this.resignedColor = color;
    }

    public PlayerColor getResignedColor() {
        return resignedColor;
    }

    private boolean isInsufficientMaterial() {
        List<Piece> white = board.getPiecesOfColor(PlayerColor.WHITE);
        List<Piece> black = board.getPiecesOfColor(PlayerColor.BLACK);

        if (white.size() == 1 && black.size() == 1)
            return true; // K vs K

        if (white.size() == 2 && black.size() == 1) { // K + (B or N) vs K
            Piece p = white.get(0) instanceof King ? white.get(1) : white.get(0);
            if (p instanceof Bishop || p instanceof Knight)
                return true;
        }
        if (white.size() == 1 && black.size() == 2) { // K vs K + (B or N)
            Piece p = black.get(0) instanceof King ? black.get(1) : black.get(0);
            if (p instanceof Bishop || p instanceof Knight)
                return true;
        }
        if (white.size() == 2 && black.size() == 2) { // KB vs KB (same color bishops)
            Piece wP = white.get(0) instanceof King ? white.get(1) : white.get(0);
            Piece bP = black.get(0) instanceof King ? black.get(1) : black.get(0);
            if (wP instanceof Bishop && bP instanceof Bishop) {
                boolean wLight = (wP.getPos().getRank() + wP.getPos().getFile()) % 2 == 0;
                boolean bLight = (bP.getPos().getRank() + bP.getPos().getFile()) % 2 == 0;
                if (wLight == bLight)
                    return true;
            }
        }
        return false;
    }

    private boolean isThreefoldRepetition() {
        // FenGenerator.toFen simplified clock/turn counters make it useful for position
        // comparison
        if (positionHistory.isEmpty())
            return false;
        String current = positionHistory.get(positionHistory.size() - 1);
        int count = 0;
        for (String fen : positionHistory) {
            if (fen.equals(current)) {
                count++;
            }
        }
        return count >= 3;
    }

    private boolean isFiftyMoveRule() {
        return halfmoveClock >= 100;
    }

    public GameOverType getGameOverType() {
        if (getKingOfColor(turn).isInCheck(this))
            return GameOverType.CHECKMATE;
        if (resignedColor != null)
            return GameOverType.RESIGN;
        if (drawAgreed)
            return GameOverType.DRAW_AGREEMENT;
        if (isInsufficientMaterial())
            return GameOverType.INSUFFICIENT_MATERIAL;
        if (isThreefoldRepetition())
            return GameOverType.THREEFOLD_REPETITION;
        if (isFiftyMoveRule())
            return GameOverType.FIFTY_MOVE_RULE;
        return GameOverType.STALEMATE;
    }

    /** @return list of all pieces of the given color */
    public List<Piece> getPiecesOfColor(PlayerColor color) {
        return board.getPiecesOfColor(color);
    }

    /** @return the king piece of the given color (never null in valid states) */
    public King getKingOfColor(PlayerColor color) {
        List<Piece> pieces = board.getPiecesOfColor(color);
        for (Piece piece : pieces) {
            if (piece instanceof King)
                return (King) piece;
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
        if (getGameOverType() == GameOverType.CHECKMATE)
            return turn.getOpposite();
        return null;
    }

    /** @return true if the current player is in check */
    public boolean isInCheck() {
        return getKingOfColor(turn).isInCheck(this);
    }

    /**
     * Places a piece on the board at the given position and updates its
     * coordinates.
     */
    public void setPieceAt(Position pos, Piece piece) {
        getBoard().setPieceAt(pos, piece);
        if (piece != null)
            piece.updateCoords(pos);
    }

    /**
     * Places a piece on the board at the given coordinates and updates its
     * coordinates.
     */
    public void setPieceAt(int rank, int file, Piece piece) {
        getBoard().setPieceAt(rank, file, piece);
        if (piece != null)
            piece.updateCoords(rank, file);
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
        this.halfmoveClock = snap.halfmoveClock;
        this.fullmoveNumber = snap.fullmoveNumber;
        this.positionHistory.clear();
        this.positionHistory.addAll(snap.positionHistory);
        this.resignedColor = snap.resignedColor;
        this.drawAgreed = snap.drawAgreed;
    }
}
