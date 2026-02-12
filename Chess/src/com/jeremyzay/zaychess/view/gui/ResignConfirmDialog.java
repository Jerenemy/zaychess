package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Inline overlay shown when the user clicks Resign.
 * Confirms if they really want to forfeit.
 */
public class ResignConfirmDialog extends OverlayPanel {

    private final Runnable onResign;

    public ResignConfirmDialog(Runnable onResign) {
        this.onResign = onResign;
    }

    @Override
    protected JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Title
        JLabel title = createTitle("Resign Game?");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(title);

        // Body message
        JLabel msgLabel = new JLabel(
                "<html><div style='text-align: center;'>Are you sure you want to forfeit the match?<br>Your opponent will be awarded the win.</div></html>",
                SwingConstants.CENTER);
        msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgLabel.setForeground(Color.DARK_GRAY);
        content.add(msgLabel);
        content.add(Box.createRigidArea(new Dimension(0, 30)));

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonPanel.setOpaque(false);

        JButton cancelBtn = createOverlayButton("Cancel");
        cancelBtn.addActionListener(e -> hideOverlay());

        JButton confirmBtn = createOverlayButton("Yes, Resign");
        confirmBtn.setBackground(new Color(220, 50, 50)); // Reddish
        confirmBtn.setForeground(Color.RED);
        confirmBtn.addActionListener(e -> {
            hideOverlay();
            if (onResign != null) {
                onResign.run();
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(confirmBtn);
        content.add(buttonPanel);

        return content;
    }
}
