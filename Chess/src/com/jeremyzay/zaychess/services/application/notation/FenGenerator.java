package com.jeremyzay.zaychess.services.application.notation;

import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.SpecialMoveHandler;
import com.jeremyzay.zaychess.model.pieces.*;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;

/**
 * Generates FEN (Forsyth–Edwards Notation) strings from game state.
 * 
 * FEN format: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
 * - Piece placement (8 ranks, separated by '/')
 * - Active color ('w' or 'b')
 * - Castling availability (KQkq or '-')
 * - En passant target square (e.g. "e3" or '-')
 * - Halfmove clock (for 50-move rule, simplified to 0)
 * - Fullmove number (simplified to 1)
 */
public final class FenGenerator {

    private FenGenerator() {
    } // utility class

    /**
     * Generates a FEN string from the current game state.
     * 
     * @param gs the game state
     * @return FEN string representation
     */
    public static String toFen(GameState gs) {
        StringBuilder sb = new StringBuilder();

        // 1. Piece placement
        sb.append(piecePlacement(gs.getBoard()));
        sb.append(' ');

        // 2. Active color
        sb.append(gs.getTurn() == PlayerColor.WHITE ? 'w' : 'b');
        sb.append(' ');

        // 3. Castling availability
        sb.append(castlingAvailability(gs.getSpecialMoveHandler()));
        sb.append(' ');

        // 4. En passant target
        sb.append(enPassantTarget(gs.getSpecialMoveHandler()));
        sb.append(' ');

        // 5. Halfmove clock (simplified)
        sb.append('0');
        sb.append(' ');

        // 6. Fullmove number (simplified)
        sb.append('1');

        return sb.toString();
    }

    private static String piecePlacement(Board board) {
        StringBuilder sb = new StringBuilder();

        for (int rank = 0; rank < 8; rank++) {
            if (rank > 0)
                sb.append('/');

            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = board.getPieceAt(rank, file);
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount);
                        emptyCount = 0;
                    }
                    sb.append(pieceToChar(piece));
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount);
            }
        }

        return sb.toString();
    }

    private static char pieceToChar(Piece piece) {
        char c = piece.getSymbol();
        // Piece.getSymbol() returns uppercase for the piece type
        // White pieces are uppercase, black pieces are lowercase
        return piece.getColor() == PlayerColor.WHITE
                ? Character.toUpperCase(c)
                : Character.toLowerCase(c);
    }

    private static String castlingAvailability(SpecialMoveHandler handler) {
        StringBuilder sb = new StringBuilder();

        // White kingside: neither white king nor kingside rook has moved
        if (!handler.hasWhiteKingMoved() && !handler.hasWhiteKingsideRookMoved())
            sb.append('K');
        // White queenside: neither white king nor queenside rook has moved
        if (!handler.hasWhiteKingMoved() && !handler.hasWhiteQueensideRookMoved())
            sb.append('Q');
        // Black kingside: neither black king nor kingside rook has moved
        if (!handler.hasBlackKingMoved() && !handler.hasBlackKingsideRookMoved())
            sb.append('k');
        // Black queenside: neither black king nor queenside rook has moved
        if (!handler.hasBlackKingMoved() && !handler.hasBlackQueensideRookMoved())
            sb.append('q');

        return sb.length() == 0 ? "-" : sb.toString();
    }

    private static String enPassantTarget(SpecialMoveHandler handler) {
        Position epTarget = handler.getEnPassantTarget();
        if (epTarget == null)
            return "-";

        // Convert position to algebraic notation
        char file = (char) ('a' + epTarget.getFile());
        int rank = 8 - epTarget.getRank(); // Convert 0-indexed to 1-8
        return "" + file + rank;
    }
}
