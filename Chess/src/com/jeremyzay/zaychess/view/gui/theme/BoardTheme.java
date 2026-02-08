package com.jeremyzay.zaychess.view.gui.theme;

import java.awt.Color;

/**
 * Defines the color scheme for the board and highlights.
 */
public class BoardTheme {
    // Square colors
    public final Color lightSquare;
    public final Color darkSquare;

    // Highlight colors
    public final Color selectionColor;
    public final Color lastMoveColor;
    public final Color checkColor;
    public final Color checkmateColor;

    public BoardTheme(Color lightSquare, Color darkSquare,
            Color selectionColor, Color lastMoveColor,
            Color checkColor, Color checkmateColor) {
        this.lightSquare = lightSquare;
        this.darkSquare = darkSquare;
        this.selectionColor = selectionColor;
        this.lastMoveColor = lastMoveColor;
        this.checkColor = checkColor;
        this.checkmateColor = checkmateColor;
    }

    // Default Theme (Blue/Teal style currently in use)
    public static BoardTheme DEFAULT() {
        return new BoardTheme(
                new Color(160, 221, 235), // Light (Blueish)
                new Color(32, 108, 125), // Dark (Teal)
                new Color(0, 255, 0, 128), // Selection (Green transp)
                new Color(255, 255, 0, 128), // Last Move (Yellow transp)
                new Color(255, 0, 0, 180), // Check (Red transp)
                new Color(200, 0, 0, 200) // Checkmate (Dark Red)
        );
    }
}
