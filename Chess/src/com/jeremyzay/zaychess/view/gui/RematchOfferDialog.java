package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Inline overlay shown when a rematch is requested.
 */
public class RematchOfferDialog extends OverlayPanel {

    private final String message;
    private final Runnable onAccept;
    private final Runnable onDecline;

    public RematchOfferDialog(String message, Runnable onAccept, Runnable onDecline) {
        this.message = message;
        this.onAccept = onAccept;
        this.onDecline = onDecline;
    }

    @Override
    protected JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Title
        JLabel title = createTitle("Rematch Requested");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        // Body message
        JLabel msgLabel = new JLabel(
                "<html><div style='text-align: center;'>" + message + "</div></html>",
                SwingConstants.CENTER);
        msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgLabel.setForeground(Color.DARK_GRAY);
        content.add(msgLabel);
        content.add(Box.createRigidArea(new Dimension(0, 30)));

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonPanel.setOpaque(false);

        JButton declineBtn = createOverlayButton("Decline");
        declineBtn.addActionListener(e -> {
            hideOverlay();
            if (onDecline != null)
                onDecline.run();
        });

        JButton acceptBtn = createOverlayButton("Accept");
        acceptBtn.setBackground(new Color(50, 150, 50)); // Greenish
        acceptBtn.setForeground(Color.RED);
        acceptBtn.addActionListener(e -> {
            hideOverlay();
            if (onAccept != null)
                onAccept.run();
        });

        buttonPanel.add(declineBtn);
        buttonPanel.add(acceptBtn);
        content.add(buttonPanel);

        return content;
    }
}
