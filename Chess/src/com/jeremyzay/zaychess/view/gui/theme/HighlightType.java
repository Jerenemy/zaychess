package com.jeremyzay.zaychess.view.gui.theme;

/**
 * Types of highlights that can be applied to a square.
 * Ordered by priority (lowest to highest preferred for rendering).
 */
public enum HighlightType {
    LAST_MOVE, // Yellow/Gold
    SELECTION, // Green/Blue
    CHECK, // Red
    CHECKMATE // Dark Red / Orange
}
