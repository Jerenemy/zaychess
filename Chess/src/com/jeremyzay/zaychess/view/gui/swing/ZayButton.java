package com.jeremyzay.zaychess.view.gui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Unified button with soft shadow, squircle corners, and subtle hover
 * expansion.
 * No hard border – depth comes from a soft shadow underneath.
 */
public class ZayButton extends JButton {

    private static final int ARC = 30;

    /* ---- constructors ---- */

    public ZayButton(String text) {
        super(text);
        init();
    }

    public ZayButton(Icon icon) {
        super(icon);
        init();
    }

    /* ---- setup ---- */

    private void init() {
        setForeground(new Color(20, 20, 20));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Remove all internal padding so text/icons have full room
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                repaint();
            }
        });
    }

    /* ---- painting ---- */

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        ButtonModel m = getModel();
        boolean hover = m.isRollover();
        boolean press = m.isPressed();

        // Leave room for the shadow on all sides
        int inset = 4;
        int growOnHover = hover ? 2 : 0;
        int x = inset - growOnHover;
        int y = inset - growOnHover;
        int curW = w - inset * 2 + growOnHover * 2;
        int curH = h - inset * 2 + growOnHover * 2;

        // Faint white glow (visible on dark backgrounds)
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRoundRect(x - 2, y - 1, curW + 4, curH + 4, ARC + 4, ARC + 4);

        // Soft shadow (multiple translucent layers offset downward)
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(x, y + 2, curW, curH, ARC, ARC);
        g2.setColor(new Color(0, 0, 0, 16));
        g2.fillRoundRect(x - 1, y + 3, curW + 2, curH, ARC, ARC);

        // Button body
        g2.setColor(press ? new Color(235, 235, 235)
                : hover ? new Color(252, 252, 252)
                        : new Color(250, 250, 250));
        g2.fillRoundRect(x, y, curW, curH, ARC, ARC);

        g2.dispose();
        super.paintComponent(g);
    }
}
