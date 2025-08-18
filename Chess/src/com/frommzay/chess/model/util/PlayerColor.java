package com.frommzay.chess.model.util;

import java.awt.Color;

/**
 * Enumeration of the two chess player colors.
 * 
 * Associates each color with a display color for UI.
 * Provides helpers to check and retrieve the opposite color.
 */
public enum PlayerColor {
    BLACK(Color.black),
    WHITE(Color.white);

    PlayerColor(Color color) {
        // Currently unused, but may be used for UI or styling
    }

    /**
     * Checks if this color is the opposite of another.
     *
     * @param color the color to compare against
     * @return true if the colors differ
     */
    public boolean isOpposite(PlayerColor color) {
        return this != color;
    }

    /**
     * Returns the opposite player color.
     *
     * @return WHITE if this is BLACK, BLACK if this is WHITE
     */
    public PlayerColor getOpposite() {
        return (this == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
    }
}
