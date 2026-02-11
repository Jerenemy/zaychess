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
            angle -= 5f;
            if (angle <= 0f)
                angle += 360f;
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
                int arc = OverlayPanel.BUTTON_ARC; // rounded corners
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
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        int h = getHeight();

        // 1. Dark Overlay
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, w, h);

        // 2. Swiftify-style Gradient Spinner
        int cx = w / 2;
        int cy = h / 2 - 30; // Shift up slightly
        int r = 40; // radius
        int strokeWidth = 5;

        g2.translate(cx, cy);

        // Draw segments with fading alpha
        int segments = 240; // High density for smooth look
        float angleStep = 360f / segments;
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

        for (int i = 0; i < segments; i++) {
            // i=0 is the head (brightest)
            float alpha = (float) Math.pow(1.0f - ((float) i / segments), 2.5); // Smooth accelerated fade
            if (alpha < 0.01f)
                continue;

            g2.setColor(new Color(1f, 1f, 1f, alpha));

            // Swing arcs: 0 is right, positive is counter-clockwise.
            // Since we rotate clockwise (angle decreases), the trail must be
            // counter-clockwise from the head.
            // So we ADD to the angle as 'i' increases to move "backwards" in time.
            float startAngle = (float) (angle - 90 + (i * angleStep));

            // Slightly overlap segments to prevent gaps
            g2.draw(new java.awt.geom.Arc2D.Float(-r, -r, 2 * r, 2 * r, startAngle, angleStep * 1.5f,
                    java.awt.geom.Arc2D.OPEN));
        }

        // Add a rounded cap at the very head for polish
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // The head is at 'angle - 90'
        g2.draw(new java.awt.geom.Arc2D.Float(-r, -r, 2 * r, 2 * r, (float) (angle - 90), 0.1f,
                java.awt.geom.Arc2D.OPEN));

        g2.translate(-cx, -cy);

        // 3. Text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(message);
        g2.drawString(message, cx - tw / 2, cy + r + 45);

        g2.dispose();
    }
}
