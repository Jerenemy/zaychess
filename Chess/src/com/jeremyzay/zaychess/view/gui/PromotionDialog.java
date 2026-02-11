package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import java.awt.*;

import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.model.util.PlayerColor;

/**
 * Inline overlay for pawn promotion piece selection.
 * Uses a callback pattern — the caller provides a Consumer that receives
 * the chosen PromotionPiece (or null if cancelled).
 */
public final class PromotionDialog extends OverlayPanel {

    private final PlayerColor color;
    private final java.util.function.Consumer<PromotionPiece> onSelect;

    private PromotionDialog(PlayerColor color, java.util.function.Consumer<PromotionPiece> onSelect) {
        this.color = color;
        this.onSelect = onSelect;
    }

    /**
     * Shows the promotion overlay.
     * 
     * @param parent   ignored (kept for API compatibility)
     * @param color    the color of the promoting pawn
     * @param onSelect callback receiving the chosen piece
     */
    public static void prompt(Component parent, PlayerColor color,
            java.util.function.Consumer<PromotionPiece> onSelect) {
        new PromotionDialog(color, onSelect).showOverlay();
    }

    @Override
    protected JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = createTitle(
                (color == PlayerColor.WHITE) ? "Promote Pawn (White)" : "Promote Pawn (Black)");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 12, 0));
        buttonPanel.setOpaque(false);

        String[] glyphs = (color == PlayerColor.WHITE)
                ? new String[] { "♕", "♖", "♗", "♘" }
                : new String[] { "♛", "♜", "♝", "♞" };
        PromotionPiece[] pieces = {
                PromotionPiece.QUEEN, PromotionPiece.ROOK,
                PromotionPiece.BISHOP, PromotionPiece.KNIGHT
        };

        for (int i = 0; i < 4; i++) {
            final PromotionPiece piece = pieces[i];
            JButton btn = new JButton(
                    "<html><div style='font-size:28px; padding:2px 10px'>" + glyphs[i] + "</div></html>");
            btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(80, 80));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                hideOverlay();
                if (onSelect != null)
                    onSelect.accept(piece);
            });
            buttonPanel.add(btn);
        }

        content.add(buttonPanel);
        return content;
    }
}
