package com.jeremyzay.zaychess.view.gui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Toggle button with the same unified style as ZayButton.
 */
public class ZayToggleButton extends JToggleButton {

    private static final int ARC = 30;

    public ZayToggleButton(String text, boolean selected) {
        super(text, selected);
        setForeground(new Color(20, 20, 20));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    com.jeremyzay.zaychess.services.infrastructure.audio.SoundService.play(
                            com.jeremyzay.zaychess.services.infrastructure.audio.SoundService.SFX.UI_CLICK);
                }
            }
        });
    }

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
        boolean selected = m.isSelected();

        int inset = 4;
        int growOnHover = hover ? 2 : 0;
        int x = inset - growOnHover;
        int y = inset - growOnHover;
        int curW = w - inset * 2 + growOnHover * 2;
        int curH = h - inset * 2 + growOnHover * 2;

        // Faint white glow (visible on dark backgrounds)
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRoundRect(x - 2, y - 1, curW + 4, curH + 4, ARC + 4, ARC + 4);

        // Soft shadow
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(x, y + 2, curW, curH, ARC, ARC);
        g2.setColor(new Color(0, 0, 0, 16));
        g2.fillRoundRect(x - 1, y + 3, curW + 2, curH, ARC, ARC);

        // Body – slightly darker when selected
        if (selected) {
            g2.setColor(new Color(228, 228, 228));
        } else {
            g2.setColor(press ? new Color(225, 225, 225)
                    : hover ? new Color(245, 245, 245)
                            : new Color(240, 240, 240));
        }
        g2.fillRoundRect(x, y, curW, curH, ARC, ARC);

        g2.dispose();
        super.paintComponent(g);
    }
}
