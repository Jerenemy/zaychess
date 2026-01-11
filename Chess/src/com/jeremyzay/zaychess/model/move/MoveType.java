package com.jeremyzay.zaychess.model.move;

/**
 * Enumeration of all supported chess move types.
 * 
 * NORMAL: regular quiet move
 * CAPTURE: captures an opponent's piece
 * CASTLE: castling king + rook move
 * EN_PASSANT: special pawn capture
 * PROMOTION: pawn promotion
 */
public enum MoveType {
    NORMAL, CAPTURE, CASTLE, EN_PASSANT, PROMOTION
}
