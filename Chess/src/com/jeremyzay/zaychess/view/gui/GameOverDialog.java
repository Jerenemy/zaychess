package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import java.awt.*;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.pieces.King;

/**
 * Inline overlay shown when the game ends.
 * Displays the result message, winner icon, and Rematch/Menu buttons.
 */
public class GameOverDialog extends OverlayPanel {

    private final String message;
    private final PlayerColor winner;
    private final Runnable onRematch;
    private final Runnable onMenu;

    public GameOverDialog(String message, PlayerColor winner,
            Runnable onRematch, Runnable onMenu) {
        this.message = message;
        this.winner = winner;
        this.onRematch = onRematch;
        this.onMenu = onMenu;
    }

    @Override
    protected JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Title
        JLabel title = createTitle("Game Over");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        // Winner icon
        if (winner != null) {
            King dummyKing = new King(winner, null);
            Icon icon = ResourceLoader.getPieceIcon(dummyKing, 80);
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(iconLabel);
            content.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        // Message
        JLabel msgLabel = new JLabel(
                "<html><div style='text-align: center;'>" + message + "</div></html>",
                SwingConstants.CENTER);
        msgLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgLabel.setForeground(Color.DARK_GRAY);
        content.add(msgLabel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonPanel.setOpaque(false);

        JButton rematchBtn = createOverlayButton("Rematch");
        rematchBtn.addActionListener(e -> {
            hideOverlay();
            if (onRematch != null)
                onRematch.run();
        });

        JButton menuBtn = createOverlayButton("Menu");
        menuBtn.addActionListener(e -> {
            hideOverlay();
            if (onMenu != null)
                onMenu.run();
        });

        buttonPanel.add(rematchBtn);
        buttonPanel.add(menuBtn);
        content.add(buttonPanel);

        return content;
    }
}
