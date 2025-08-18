package com.frommzay.chess.view.gui;

import javax.swing.*;
import java.awt.Component;

import com.frommzay.chess.model.move.PromotionPiece;
import com.frommzay.chess.model.util.PlayerColor;

public final class PromotionDialog {
    private PromotionDialog() {}

    /** Returns the chosen piece or null if the user cancelled. */
    public static PromotionPiece prompt(Component parent, PlayerColor color) {
        Object[] options = {"Queen", "Rook", "Bishop", "Knight"};
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
