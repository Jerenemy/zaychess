package com.frommzay.chess.view.gui;

import javax.swing.*;
import java.awt.*;

/**
 * A panel wrapper that keeps its child component square-shaped.
 *
 * Example: ensures the chessboard always renders as a perfect square,
 * even when the window is resized.
 */
public class AspectRatioPanel extends JPanel {
    private static final long serialVersionUID = 1L;
	private final JComponent child;
    private final Color bg; // background fill color behind the child

    public AspectRatioPanel(JComponent child) {
        this(child, new Color(200, 200, 200)); // default light gray background
    }

    public AspectRatioPanel(JComponent child, Color background) {
        super(null); // manual layout control
        this.child = child;
        this.bg = background;
        setOpaque(true);
        add(child);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(bg);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public void doLayout() {
        // Always enforce square aspect ratio
        int w = getWidth(), h = getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2, y = (h - side) / 2;
        child.setBounds(x, y, side, side); // centered square
    }

    @Override public Dimension getMinimumSize() {
        // Ensure child’s minimum size is respected, but square
        Dimension c = child.getMinimumSize();
        int side = Math.min(c.width, c.height);
        return new Dimension(side, side);
    }

    @Override public Dimension getPreferredSize() {
        // Ensure child’s preferred size is respected, but square
        Dimension c = child.getPreferredSize();
        int side = Math.min(c.width, c.height);
        return new Dimension(side, side);
    }
}
