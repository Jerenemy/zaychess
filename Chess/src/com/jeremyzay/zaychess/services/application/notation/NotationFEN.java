package com.jeremyzay.zaychess.services.application.notation;

import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.SpecialMoveHandler;
import com.jeremyzay.zaychess.model.pieces.*;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;

/** Encode/decode a {@link GameState} to/from a FEN string. */
public final class NotationFEN {
    private NotationFEN() {
    }

    /**
     * Parses a FEN string and returns the corresponding {@link GameState}.
     *
     * @param fen a standard FEN string (at least piece-placement and active-color
     *            fields)
     * @return a fully initialised GameState
     */
    public static GameState fromFEN(String fen) {
        String[] parts = fen.trim().split("\\s+");
        String placement = parts[0];
        char activeColor = parts.length > 1 ? parts[1].charAt(0) : 'w';
        String castling = parts.length > 2 ? parts[2] : "KQkq";
        String epSquare = parts.length > 3 ? parts[3] : "-";

        // Create a blank game state then clear the default board
        GameState state = new GameState();
        Board board = state.getBoard();
        for (int r = 0; r < 8; r++)
            for (int f = 0; f < 8; f++)
                board.setPieceAt(r, f, null);

        // --- Piece placement ---
        String[] ranks = placement.split("/");
        for (int rank = 0; rank < ranks.length; rank++) {
            int file = 0;
            for (char ch : ranks[rank].toCharArray()) {
                if (Character.isDigit(ch)) {
                    file += Character.getNumericValue(ch);
                } else {
                    PlayerColor color = Character.isUpperCase(ch)
                            ? PlayerColor.WHITE
                            : PlayerColor.BLACK;
                    Position pos = new Position(rank, file);
                    Piece piece = createPiece(Character.toLowerCase(ch), color, pos);
                    state.setPieceAt(pos, piece);
                    file++;
                }
            }
        }

        // --- Active color ---
        if (activeColor == 'b') {
            // GameState starts as WHITE; switch to BLACK
            state.changeTurn();
        }

        // --- Castling rights ---
        SpecialMoveHandler smh = state.getSpecialMoveHandler();
        // Mark rights as LOST (hasMoved = true) when the letter is absent
        if (castling.indexOf('K') < 0)
            smh.setWhiteKingsideRookMoved(true);
        if (castling.indexOf('Q') < 0)
            smh.setWhiteQueensideRookMoved(true);
        if (castling.indexOf('k') < 0)
            smh.setBlackKingsideRookMoved(true);
        if (castling.indexOf('q') < 0)
            smh.setBlackQueensideRookMoved(true);
        // If neither side of a king can castle, mark king as moved too
        if (castling.indexOf('K') < 0 && castling.indexOf('Q') < 0)
            smh.setWhiteKingMoved(true);
        if (castling.indexOf('k') < 0 && castling.indexOf('q') < 0)
            smh.setBlackKingMoved(true);

        // --- En passant target ---
        if (!"-".equals(epSquare) && epSquare.length() == 2) {
            int epFile = epSquare.charAt(0) - 'a';
            int epRank = 8 - Character.getNumericValue(epSquare.charAt(1));
            smh.setEnPassantTarget(new Position(epRank, epFile));
        }

        return state;
    }

    /** Creates a piece from its FEN symbol (always lowercase). */
    private static Piece createPiece(char symbol, PlayerColor color, Position pos) {
        return switch (symbol) {
            case 'k' -> new King(color, pos);
            case 'q' -> new Queen(color, pos);
            case 'r' -> new Rook(color, pos);
            case 'b' -> new Bishop(color, pos);
            case 'n' -> new Knight(color, pos);
            case 'p' -> new Pawn(color, pos);
            default -> throw new IllegalArgumentException("Unknown FEN piece: " + symbol);
        };
    }

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
            if (empty > 0)
                fen.append(empty);
            if (rank < 7)
                fen.append('/');
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

        if (canCastle(board, smh, PlayerColor.WHITE, true))
            rights.append('K');
        if (canCastle(board, smh, PlayerColor.WHITE, false))
            rights.append('Q');
        if (canCastle(board, smh, PlayerColor.BLACK, true))
            rights.append('k');
        if (canCastle(board, smh, PlayerColor.BLACK, false))
            rights.append('q');

        return rights.length() == 0 ? "-" : rights.toString();
    }

    private static boolean canCastle(Board board, SpecialMoveHandler smh,
            PlayerColor color, boolean kingSide) {
        int rank = (color == PlayerColor.WHITE) ? 7 : 0;
        int rookFile = kingSide ? 7 : 0;
        Position kingPos = new Position(rank, 4);
        Position rookPos = new Position(rank, rookFile);

        Piece king = board.getPieceAt(kingPos);
        if (!(king instanceof King) || king.getColor() != color)
            return false;
        Piece rook = board.getPieceAt(rookPos);
        if (!(rook instanceof Rook) || rook.getColor() != color)
            return false;

        if (color == PlayerColor.WHITE) {
            if (smh.hasWhiteKingMoved())
                return false;
            if (kingSide && smh.hasWhiteKingsideRookMoved())
                return false;
            if (!kingSide && smh.hasWhiteQueensideRookMoved())
                return false;
        } else {
            if (smh.hasBlackKingMoved())
                return false;
            if (kingSide && smh.hasBlackKingsideRookMoved())
                return false;
            if (!kingSide && smh.hasBlackQueensideRookMoved())
                return false;
        }
        return true;
    }

    private static String enPassantSquare(GameState state) {
        Position target = state.getSpecialMoveHandler().getEnPassantTarget();
        if (target == null)
            return "-";
        char file = (char) ('a' + target.getFile());
        char rank = (char) ('0' + (8 - target.getRank()));
        return "" + file + rank;
    }
}
