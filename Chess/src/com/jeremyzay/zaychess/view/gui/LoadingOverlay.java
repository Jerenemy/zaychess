package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.Arc2D;
import javax.swing.*;

/**
 * A glass-pane overlay for loading/waiting screens.
 * Features:
 * - Semi-transparent dark background (blur simulation)
 * - Animated spiraling spinner
 * - Scaled, centered text
 * - Optional Cancel button
 */
public class LoadingOverlay extends JPanel {
    private static final long serialVersionUID = 1L;

    private final String message;
    private Timer animationTimer;
    private float angle = 0f;

    public LoadingOverlay(String message, ActionListener cancelAction) {
        this.message = message;

        setOpaque(false);
        setLayout(new GridBagLayout());

        // Capture mouse events to block interaction with underlying components
        addMouseListener(new java.awt.event.MouseAdapter() {
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
        });

        // Cancel Button (if action provided)
        if (cancelAction != null) {
            JButton cancelButton = createCancelButton();
            cancelButton.addActionListener(cancelAction);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1; // Below spinner/text (we'll paint spinner manually)
            gbc.insets = new Insets(180, 0, 0, 0); // Push down from center
            gbc.anchor = GridBagConstraints.CENTER;
            add(cancelButton, gbc);
        }

        // Animation loop
        animationTimer = new Timer(16, e -> {
            angle += 5f;
            if (angle >= 360f)
                angle -= 360f;
            repaint();
        });
    }

    private JButton createCancelButton() {
        JButton btn = new JButton("Cancel") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // base + hover/press alpha
                float alpha = 0.20f;
                ButtonModel m = getModel();
                if (m.isRollover())
                    alpha = 0.40f;
                if (m.isPressed())
                    alpha = 0.60f;

                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g2.setColor(Color.WHITE);
                int arc = 20; // rounded corners
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Sizing
        btn.setPreferredSize(new Dimension(140, 40));

        return btn;
    }

    public void start() {
        if (!animationTimer.isRunning())
            animationTimer.start();
        setVisible(true);
    }

    public void stop() {
        if (animationTimer.isRunning())
            animationTimer.stop();
        setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Dark Overlay
        g2.setColor(new Color(0, 0, 0, 180)); // 70% opacity black
        g2.fillRect(0, 0, w, h);

        // 2. Spinner
        int cx = w / 2;
        int cy = h / 2 - 30; // Shift up slightly
        int r = 40; // radius
        int stroke = 6;

        g2.translate(cx, cy);
        g2.rotate(Math.toRadians(angle));

        // Draw multiple arcs for "spiraling" effect
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Trail effect
        for (int i = 0; i < 6; i++) {
            float alpha = 1.0f - (i * 0.15f);
            if (alpha < 0)
                alpha = 0;
            g2.setColor(new Color(1f, 1f, 1f, alpha));
            // Draw arc segment
            g2.draw(new Arc2D.Float(-r, -r, 2 * r, 2 * r, 90 + (i * 20), -40, Arc2D.OPEN));
        }

        g2.rotate(-Math.toRadians(angle)); // Un-rotate for text
        g2.translate(-cx, -cy);

        // 3. Text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(message);
        g2.drawString(message, cx - tw / 2, cy + r + 40);

        g2.dispose();

        // Let children (buttons) paint on top
        // paintChildren is called automatically by Swing after paintComponent
    }
}
