package com.jeremyzay.zaychess.model.move;

import java.util.Objects;
import com.jeremyzay.zaychess.model.util.Position;

/**
 * Immutable value object representing a chess move.
 * 
 * A move contains the origin and destination positions,
 * the move type (normal, capture, promotion, etc.), 
 * and an optional promotion payload.
 */
public final class Move {
    private final Position fromPos;
    private final Position toPos;
    private final MoveType moveType;
    private final PromotionPiece promotion; // nullable

    /** Creates a move without promotion. */
    public Move(Position from, Position to, MoveType type) {
        this(from, to, type, null);
    }

    /** Canonical constructor. */
    public Move(Position from, Position to, MoveType type, PromotionPiece promotion) {
        this.fromPos = Objects.requireNonNull(from);
        this.toPos   = Objects.requireNonNull(to);
        this.moveType = Objects.requireNonNull(type);
        this.promotion = promotion; // may be null for pending promotion
    }

    /**
     * Creates a promotion move with a chosen promotion piece.
     */
    public static Move promotion(Position from, Position to, PromotionPiece piece) {
        return new Move(from, to, MoveType.PROMOTION, Objects.requireNonNull(piece));
    }

    /**
     * Creates a promotion move with no piece chosen yet (pending).
     */
    public static Move pendingPromotion(Position from, Position to) {
        return new Move(from, to, MoveType.PROMOTION, null);
    }

    /** @return the promotion piece, or null if not yet chosen */
    public PromotionPiece getPromotion() { return promotion; }

    /**
     * Creates a new move identical to this one, but with a promotion payload.
     *
     * @param p the promotion piece
     * @return new move with promotion filled in
     */
    public Move withPromotion(PromotionPiece p) {
        return new Move(fromPos, toPos, moveType, Objects.requireNonNull(p));
    }

    /** @return origin square */
    public Position getFromPos() { return this.fromPos; }

    /** @return destination square */
    public Position getToPos() { return this.toPos; }

    /** @return the type of move (normal, capture, promotion, etc.) */
    public MoveType getMoveType() { return this.moveType; }

    /** @return simple string representation for debugging */
    @Override
    public String toString() {
        return getToPos() + "," + getMoveType() + ".";
    }
}
