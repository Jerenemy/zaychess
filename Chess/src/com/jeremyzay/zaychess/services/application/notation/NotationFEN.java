package com.jeremyzay.zaychess.services.application.notation;

import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.SpecialMoveHandler;
import com.jeremyzay.zaychess.model.pieces.King;
import com.jeremyzay.zaychess.model.pieces.Piece;
import com.jeremyzay.zaychess.model.pieces.Rook;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;

/** Encode the current {@link GameState} into a FEN string. */
public final class NotationFEN {
    private NotationFEN() {}

    public static String toFEN(GameState state) {
        StringBuilder fen = new StringBuilder();
        Board board = state.getBoard();
        for (int rank = 0; rank < 8; rank++) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = board.getPieceAt(rank, file);
                if (piece == null) {
                    empty++;
                    continue;
                }
                if (empty > 0) {
                    fen.append(empty);
                    empty = 0;
                }
                char symbol = piece.getSymbol();
                symbol = piece.getColor() == PlayerColor.WHITE
                        ? Character.toUpperCase(symbol)
                        : Character.toLowerCase(symbol);
                fen.append(symbol);
            }
            if (empty > 0) fen.append(empty);
            if (rank < 7) fen.append('/');
        }

        fen.append(' ')
                .append(state.getTurn() == PlayerColor.WHITE ? 'w' : 'b')
                .append(' ')
                .append(castlingRights(state))
                .append(' ')
                .append(enPassantSquare(state))
                .append(" 0 1");
        return fen.toString();
    }

    private static String castlingRights(GameState state) {
        SpecialMoveHandler smh = state.getSpecialMoveHandler();
        Board board = state.getBoard();
        StringBuilder rights = new StringBuilder();

        if (canCastle(board, smh, PlayerColor.WHITE, true)) rights.append('K');
        if (canCastle(board, smh, PlayerColor.WHITE, false)) rights.append('Q');
        if (canCastle(board, smh, PlayerColor.BLACK, true)) rights.append('k');
        if (canCastle(board, smh, PlayerColor.BLACK, false)) rights.append('q');

        return rights.length() == 0 ? "-" : rights.toString();
    }

    private static boolean canCastle(Board board, SpecialMoveHandler smh,
                                     PlayerColor color, boolean kingSide) {
        int rank = (color == PlayerColor.WHITE) ? 7 : 0;
        int rookFile = kingSide ? 7 : 0;
        Position kingPos = new Position(rank, 4);
        Position rookPos = new Position(rank, rookFile);

        Piece king = board.getPieceAt(kingPos);
        if (!(king instanceof King) || king.getColor() != color) return false;
        Piece rook = board.getPieceAt(rookPos);
        if (!(rook instanceof Rook) || rook.getColor() != color) return false;

        if (color == PlayerColor.WHITE) {
            if (smh.hasWhiteKingMoved()) return false;
            if (kingSide && smh.hasWhiteKingsideRookMoved()) return false;
            if (!kingSide && smh.hasWhiteQueensideRookMoved()) return false;
        } else {
            if (smh.hasBlackKingMoved()) return false;
            if (kingSide && smh.hasBlackKingsideRookMoved()) return false;
            if (!kingSide && smh.hasBlackQueensideRookMoved()) return false;
        }
        return true;
    }

    private static String enPassantSquare(GameState state) {
        Position target = state.getSpecialMoveHandler().getEnPassantTarget();
        if (target == null) return "-";
        char file = (char) ('a' + target.getFile());
        char rank = (char) ('0' + (8 - target.getRank()));
        return "" + file + rank;
    }
}
