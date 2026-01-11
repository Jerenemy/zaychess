package com.jeremyzay.zaychess.services.infrastructure.network;

/**
 * Immutable record representing a move in wire format.
 *
 * Contains origin/destination squares and a string type (e.g. "NORMAL", "CAPTURE", "PROMOTION:QUEEN").
 */
public record MoveMessage(int fromRank, int fromFile, int toRank, int toFile, String type) {}
