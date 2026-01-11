package com.jeremyzay.zaychess.services.application.notation;

import java.util.ArrayList;
import java.util.List;

import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.model.pieces.Piece;
import com.jeremyzay.zaychess.model.pieces.King;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.model.move.MoveGenerator;
import com.jeremyzay.zaychess.model.rules.GameOverType;

/**
 * Utility class for converting moves into SAN (Standard Algebraic Notation).
 *
 * Handles castling, captures, disambiguation, promotion, en passant,
 * and check/checkmate symbols.
 */
public final class NotationSAN {
    private NotationSAN() {}

    /** Convert a move to SAN string (without explicit "e.p." for en passant). */
    public static String toSAN(GameState before, Move m) {
        return toSAN(before, m, /*includeEpSuffix*/ false);
    }

    /**
     * Convert a move to SAN string.
     *
     * @param before game state before the move
     * @param m the move to convert
     * @param includeEpSuffix whether to append " e.p." for en passant
     * @return SAN string for the move
     */
    public static String toSAN(GameState before, Move m, boolean includeEpSuffix) {
        Board board = before.getBoard();
        Position from = m.getFromPos();
        Position to   = m.getToPos();
        Piece mover   = board.getPieceAt(from);
        if (mover == null) throw new IllegalArgumentException("No piece at move.from");

        // 1) Handle castling by king movement geometry
        if (mover instanceof King && Math.abs(to.getFile() - from.getFile()) == 2) {
            String san = (to.getFile() > from.getFile()) ? "O-O" : "O-O-O";
            return appendCheckSymbols(after(before, m), san);
        }

        // 2) Detect capture
        boolean isEnPassant = m.getMoveType() == MoveType.EN_PASSANT;
        boolean isCapture   = isEnPassant || board.getPieceAt(to) != null;

        // 3) Piece letter (pawns omit)
        char pieceChar = Character.toUpperCase(mover.getSymbol());
        boolean isPawn = (pieceChar == 'P' || mover.getName().equalsIgnoreCase("pawn"));
        StringBuilder san = new StringBuilder();

        if (!isPawn) {
            // Piece letter
            san.append(pieceLetterForSAN(pieceChar));
            // Disambiguation if multiple same pieces can move to the target
            san.append(disambiguation(before, mover, from, to));
        } else {
            // Pawn captures include file of origin
            if (isCapture) san.append(fileChar(from.getFile()));
        }

        // 4) Capture marker
        if (isCapture) san.append('x');

        // 5) Destination square
        san.append(square(to));

        // 6) Promotion handling
        if (m.getMoveType() == MoveType.PROMOTION) {
            PromotionPiece pp = m.getPromotion();
            if (pp == null) throw new IllegalArgumentException("Promotion choice missing in move");
            san.append('=').append(promotionLetter(pp));
        }

        // 7) Optional "e.p." suffix
        if (includeEpSuffix && isEnPassant) {
            san.append(" e.p.");
        }

        // 8) Check/mate symbols
        return appendCheckSymbols(after(before, m), san.toString());
    }

    /* ---------- helpers ---------- */

    /** @return a new state after applying move m to before. */
    private static GameState after(GameState before, Move m) {
        GameState probe = before.copy();
        probe.applyMove(m);
        return probe;
    }

    /** Append "+" for check or "#" for checkmate if applicable. */
    private static String appendCheckSymbols(GameState after, String base) {
        if (after.isGameOver()) {
            if (after.getGameOverType() == GameOverType.CHECKMATE) {
                return base + "#";
            }
            return base;
        }
        if (after.isInCheck()) return base + "+";
        return base;
    }

    /** Compute SAN disambiguation if multiple identical pieces could move to same square. */
    private static String disambiguation(GameState before, Piece mover, Position from, Position to) {
        List<Position> conflicts = new ArrayList<>();
        List<Piece> sameColor = before.getBoard().getPiecesOfColor(mover.getColor());
        for (Piece p : sameColor) {
            if (p == mover) continue;
            if (!p.getClass().equals(mover.getClass())) continue;

            List<Move> moves = MoveGenerator.generateLegalMoves(before, p.getPos());
            for (Move cand : moves) {
                if (cand.getToPos().equals(to)) {
                    conflicts.add(p.getPos());
                    break;
                }
            }
        }
        if (conflicts.isEmpty()) return "";

        boolean fileUnique = true, rankUnique = true;
        for (Position other : conflicts) {
            if (other.getFile() == from.getFile()) fileUnique = false;
            if (other.getRank() == from.getRank()) rankUnique = false;
        }

        if (fileUnique) return String.valueOf(fileChar(from.getFile()));
        if (rankUnique) return String.valueOf(rankDigit(from.getRank()));
        return "" + fileChar(from.getFile()) + rankDigit(from.getRank());
    }

    /** Convert piece symbol to SAN letter (omit pawns). */
    private static char pieceLetterForSAN(char symbol) {
        char up = Character.toUpperCase(symbol);
        return (up == 'P') ? '\0' : up;
    }

    /** Convert a position to square notation (e.g. e4). */
    private static String square(Position p) {
        return "" + fileChar(p.getFile()) + rankDigit(p.getRank());
    }

    /** Files: 0..7 -> 'a'..'h'. */
    private static char fileChar(int file) {
        return (char) ('a' + file);
    }

    /** Ranks: 0 (top) -> '8', 7 (bottom) -> '1'. */
    private static char rankDigit(int rank) {
        return (char) ('0' + (8 - rank));
    }

    /** Return promotion letter in SAN. */
    private static char promotionLetter(PromotionPiece pp) {
        return switch (pp) {
            case QUEEN  -> 'Q';
            case ROOK   -> 'R';
            case BISHOP -> 'B';
            case KNIGHT -> 'N';
        };
    }
}
