package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import java.awt.*;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.pieces.King;

public class GameOverDialog extends JDialog {

    public GameOverDialog(Frame owner, String title, String message, PlayerColor winner, Runnable onRematch,
            Runnable onMenu) {
        super(owner, title, true);
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // Icon (if winner exists)
        if (winner != null) {
            // Create a dummy King to get the icon
            King dummyKing = new King(winner, null);
            Icon icon = ResourceLoader.getPieceIcon(dummyKing, 80);
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            centerPanel.add(iconLabel, BorderLayout.CENTER);
        }

        // Message
        JLabel messageLabel = new JLabel("<html><div style='text-align: center;'>" + message + "</div></html>",
                SwingConstants.CENTER);
        messageLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        centerPanel.add(messageLabel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // Rematch Button
        JButton rematchBtn = new JButton("Rematch");
        rematchBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        rematchBtn.setFocusable(false);
        rematchBtn.addActionListener(e -> {
            dispose();
            if (onRematch != null)
                onRematch.run();
        });

        // Menu Button
        JButton menuBtn = new JButton("Menu");
        menuBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        menuBtn.setFocusable(false);
        menuBtn.addActionListener(e -> {
            dispose();
            if (onMenu != null)
                onMenu.run();
        });

        buttonPanel.add(rematchBtn);
        buttonPanel.add(menuBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
        setVisible(true);
    }

}
