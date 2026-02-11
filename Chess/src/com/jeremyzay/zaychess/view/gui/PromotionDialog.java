package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import java.awt.*;

import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.model.pieces.Piece;
import com.jeremyzay.zaychess.model.pieces.Queen;
import com.jeremyzay.zaychess.model.pieces.Rook;
import com.jeremyzay.zaychess.model.pieces.Bishop;
import com.jeremyzay.zaychess.model.pieces.Knight;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.view.gui.swing.ZayButton;

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

        PromotionPiece[] promoTypes = {
                PromotionPiece.QUEEN, PromotionPiece.ROOK,
                PromotionPiece.BISHOP, PromotionPiece.KNIGHT
        };

        for (PromotionPiece pp : promoTypes) {
            // Create a dummy piece to get the correct icon
            final Piece dummy = switch (pp) {
                case QUEEN -> new Queen(color, null);
                case ROOK -> new Rook(color, null);
                case BISHOP -> new Bishop(color, null);
                case KNIGHT -> new Knight(color, null);
            };

            // Use the same icon loader as the board (60px for 80px button)
            Icon icon = ResourceLoader.getPieceIcon(dummy, 60);

            JButton btn = new ZayButton(icon);
            btn.setPreferredSize(new Dimension(80, 80));

            btn.addActionListener(e -> {
                hideOverlay();
                if (onSelect != null)
                    onSelect.accept(pp);
            });
            buttonPanel.add(btn);
        }

        content.add(buttonPanel);
        return content;
    }
}
