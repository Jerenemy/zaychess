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
    private final Runnable onClose;
    private boolean rematchEnabled = true;
    private boolean localRematchRequested = false;
    private boolean peerRematchRequested = false;
    private JButton rematchBtn;

    public GameOverDialog(String message, PlayerColor winner,
            Runnable onRematch, Runnable onMenu) {
        this(message, winner, onRematch, onMenu, null, true);
    }

    public GameOverDialog(String message, PlayerColor winner,
            Runnable onRematch, Runnable onMenu, boolean rematchEnabled) {
        this(message, winner, onRematch, onMenu, null, rematchEnabled);
    }

    public GameOverDialog(String message, PlayerColor winner,
            Runnable onRematch, Runnable onMenu, Runnable onClose, boolean rematchEnabled) {
        this.message = message;
        this.winner = winner;
        this.onRematch = onRematch;
        this.onMenu = onMenu;
        this.onClose = onClose;
        this.rematchEnabled = rematchEnabled;
    }

    public void disableRematch() {
        this.rematchEnabled = false;
        if (rematchBtn != null) {
            rematchBtn.setEnabled(false);
            rematchBtn.setToolTipText("Rematch no longer available");
            rematchBtn.setBackground(null);
        }
    }

    public void setPeerRematchRequested(boolean requested) {
        this.peerRematchRequested = requested;
        updateRematchButtonStyle();
    }

    public void setLocalRematchRequested(boolean requested) {
        this.localRematchRequested = requested;
        updateRematchButtonStyle();
    }

    private void updateRematchButtonStyle() {
        if (rematchBtn == null)
            return;

        if (!rematchEnabled) {
            rematchBtn.setBackground(null);
            rematchBtn.setForeground(null);
            rematchBtn.setBorder(null);
            rematchBtn.setText("Rematch");
            return;
        }

        if (localRematchRequested && !peerRematchRequested) {
            // Waiting for peer: Blueish/Cyan
            rematchBtn.setBackground(new Color(174, 221, 236)); // Light Blue
            rematchBtn.setForeground(Color.DARK_GRAY);
            rematchBtn.setText("Waiting...");
            rematchBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        } else if (!localRematchRequested && peerRematchRequested) {
            // Incoming request: Greenish/Yellowish highlight
            rematchBtn.setBackground(new Color(50, 150, 50)); // Green
            rematchBtn.setForeground(Color.RED);
            rematchBtn.setText("Rematch?");
            rematchBtn.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
        } else if (localRematchRequested && peerRematchRequested) {
            // Both ready: Dark Green (though usually game restarts immediately)
            rematchBtn.setBackground(new Color(30, 100, 30));
            rematchBtn.setForeground(Color.WHITE);
            rematchBtn.setText("Accepting...");
            rematchBtn.setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
        } else {
            // Default
            rematchBtn.setBackground(null);
            rematchBtn.setForeground(null);
            rematchBtn.setText("Rematch");
            rematchBtn.setBorder(null);
        }
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

        rematchBtn = createOverlayButton("Rematch");
        rematchBtn.setEnabled(rematchEnabled);
        rematchBtn.addActionListener(e -> {
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

    @Override
    protected JComponent createFooter() {
        JButton closeBtn = createSecondaryButton("Close");
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> {
            hideOverlay();
            if (onClose != null)
                onClose.run();
        });
        return closeBtn;
    }
}
