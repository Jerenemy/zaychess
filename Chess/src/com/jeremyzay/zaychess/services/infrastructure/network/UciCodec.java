package com.jeremyzay.zaychess.services.infrastructure.network;

import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.model.util.Position;

/**
 * Utility for converting moves to/from UCI (Universal Chess Interface)
 * notation.
 *
 * UCI format:
 * Normal: "e2e4" (from e2 to e4)
 * Promotion: "e7e8q" (from e7 to e8, promote to queen)
 *
 * File: a=0, b=1, ..., h=7
 * Rank: 1=7 (bottom), 8=0 (top) in internal 0-indexed representation
 */
public final class UciCodec {
    private UciCodec() {
    }

    /**
     * Convert internal 0-indexed file to UCI letter.
     * file 0 -> 'a', file 7 -> 'h'
     */
    public static char fileToChar(int file) {
        return (char) ('a' + file);
    }

    /**
     * Convert internal 0-indexed rank to UCI rank number.
     * rank 0 (top) -> 8, rank 7 (bottom) -> 1
     */
    public static int rankToNumber(int rank) {
        return 8 - rank;
    }

    /**
     * Convert UCI file letter to internal 0-indexed file.
     * 'a' -> 0, 'h' -> 7
     */
    public static int charToFile(char c) {
        return c - 'a';
    }

    /**
     * Convert UCI rank number to internal 0-indexed rank.
     * 8 -> 0, 1 -> 7
     */
    public static int numberToRank(int num) {
        return 8 - num;
    }

    /**
     * Encode a Position to UCI square notation (e.g., "e4").
     */
    public static String positionToUci(Position pos) {
        return "" + fileToChar(pos.getFile()) + rankToNumber(pos.getRank());
    }

    /**
     * Decode UCI square notation to a Position.
     * 
     * @param uci two-character string like "e4"
     * @return Position object
     */
    public static Position uciToPosition(String uci) {
        char file = uci.charAt(0);
        int rankNum = Character.getNumericValue(uci.charAt(1));
        return new Position(numberToRank(rankNum), charToFile(file));
    }

    /**
     * Encode a Move to UCI notation.
     * Normal moves: "e2e4"
     * Promotions: "e7e8q"
     */
    public static String toUci(Move move) {
        StringBuilder sb = new StringBuilder();
        sb.append(positionToUci(move.getFromPos()));
        sb.append(positionToUci(move.getToPos()));

        if (move.getMoveType() == MoveType.PROMOTION && move.getPromotion() != null) {
            sb.append(promotionToChar(move.getPromotion()));
        }

        return sb.toString();
    }

    /**
     * Decode UCI notation to a Move.
     * Determines move type based on context (caller must apply board state if
     * needed).
     * For loading saves, we use MoveType.NORMAL as a generic type - the actual move
     * will be validated/applied by the game logic.
     */
    public static Move fromUci(String uci) {
        if (uci == null || uci.length() < 4)
            return null;

        Position from = uciToPosition(uci.substring(0, 2));
        Position to = uciToPosition(uci.substring(2, 4));

        // Check for promotion suffix
        if (uci.length() == 5) {
            PromotionPiece promo = charToPromotion(uci.charAt(4));
            return new Move(from, to, MoveType.PROMOTION, promo);
        }

        // Generic type - actual type will be determined by game logic
        return new Move(from, to, MoveType.NORMAL);
    }

    /**
     * Convert PromotionPiece to UCI character.
     */
    public static char promotionToChar(PromotionPiece piece) {
        return switch (piece) {
            case QUEEN -> 'q';
            case ROOK -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
        };
    }

    /**
     * Convert UCI promotion character to PromotionPiece.
     */
    public static PromotionPiece charToPromotion(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'q' -> PromotionPiece.QUEEN;
            case 'r' -> PromotionPiece.ROOK;
            case 'b' -> PromotionPiece.BISHOP;
            case 'n' -> PromotionPiece.KNIGHT;
            default -> PromotionPiece.QUEEN; // Default to queen
        };
    }
}
