package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import java.awt.Component;

import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.model.util.PlayerColor;

public final class PromotionDialog {
    private PromotionDialog() {}

    private static String big(String glyph) {
        // Adjust font-size as you like (px works reliably on Swing HTML)
        return "<html><div style='font-size:28px; padding:2px 10px'>" + glyph + "</div></html>";
    }

    /** Returns the chosen piece or null if the user cancelled. */
    public static PromotionPiece prompt(Component parent, PlayerColor color) {
        Object[] options = (color == PlayerColor.WHITE)
                ? new Object[]{ big("♕"), big("♖"), big("♗"), big("♘") }
                : new Object[]{ big("♛"), big("♜"), big("♝"), big("♞") };

        int sel = JOptionPane.showOptionDialog(
                SwingUtilities.getWindowAncestor(parent),
                "Wähle die Figur:",
                (color == PlayerColor.WHITE) ? "Bauernumwandlung (Weiß)" : "Bauernumwandlung (Schwarz)",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        return switch (sel) {
            case 0 -> PromotionPiece.QUEEN;
            case 1 -> PromotionPiece.ROOK;
            case 2 -> PromotionPiece.BISHOP;
            case 3 -> PromotionPiece.KNIGHT;
            default -> null; // cancelled
        };
    }
}
