package com.jeremyzay.zaychess.view.gui.swing;

import javax.swing.JButton;
import java.awt.*;
import java.util.Set;
import java.util.EnumSet; // Added import
import com.jeremyzay.zaychess.model.pieces.Piece;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.view.gui.ResourceLoader;
import com.jeremyzay.zaychess.view.gui.theme.BoardTheme;
import com.jeremyzay.zaychess.view.gui.theme.HighlightType;

/**
 * A single square button on the chessboard.
 * Handles:
 * - Background color (light/dark)
 * - Piece icon rendering (scaled to button size)
 * - Highlight overlay (for legal moves / selection / last move / check)
 */
public class SquareButton extends JButton {
    private static final long serialVersionUID = 1L;
    private final Position position;
    private final Color baseColor;
    private Piece currentPiece; // remembers current piece for scaling icon
    private final Set<HighlightType> activeHighlights = EnumSet.noneOf(HighlightType.class);
    private BoardTheme theme;
    private final boolean isLightSquare;

    public SquareButton(Position position, boolean isLightSquare, BoardTheme theme) {
        this.position = position;
        this.isLightSquare = isLightSquare;
        this.theme = theme;
        this.baseColor = isLightSquare ? theme.lightSquare : theme.darkSquare;

        setContentAreaFilled(false);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);

        // Auto-rescale piece icon when resized
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateScaledIcon();
            }
        });
    }

    public void setTheme(BoardTheme theme) {
        this.theme = theme;
        // Update base color? Ideally baseColor is derived from theme + isLightSquare
        // But for now, let's assume theme is set at init or we need to repaint.
        repaint();
    }

    /** Enable or disable a specific highlight type. */
    public void setHighlight(HighlightType type, boolean active) {
        if (active) {
            if (activeHighlights.add(type))
                repaint();
        } else {
            if (activeHighlights.remove(type))
                repaint();
        }
    }

    /** Clear all highlights of a specific type (or all). */
    public void clearHighlights() {
        if (!activeHighlights.isEmpty()) {
            activeHighlights.clear();
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        // 1. Draw Base Square
        Color color = isLightSquare ? theme.lightSquare : theme.darkSquare;
        g2.setColor(color);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 2. Draw Highlights in Order of Priority
        // Check/Checkmate -> Selection -> LastMove

        if (activeHighlights.contains(HighlightType.CHECKMATE)) {
            drawHighlight(g2, theme.checkmateColor);
        } else if (activeHighlights.contains(HighlightType.CHECK)) {
            drawHighlight(g2, theme.checkColor);
        }

        if (activeHighlights.contains(HighlightType.SELECTION)) {
            drawHighlight(g2, theme.selectionColor);
        }

        if (activeHighlights.contains(HighlightType.LAST_MOVE)) {
            drawHighlight(g2, theme.lastMoveColor);
        }

        g2.dispose();

        // 3. Draw Piece Icon Manually (Avoids Aqua LAF NPE)
        javax.swing.Icon icon = getIcon();
        if (icon != null) {
            int x = (getWidth() - icon.getIconWidth()) / 2;
            int y = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g, x, y);
        }
    }

    private void drawHighlight(Graphics2D g2, Color c) {
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(c);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    public Position getPosition() {
        return position;
    }

    /** Place a piece on this square and update its icon. */
    public void setPiece(Piece piece) {
        currentPiece = piece;
        updateScaledIcon();
    }

    /** Scale piece icon to current square size. */
    private void updateScaledIcon() {
        if (currentPiece == null) {
            setIcon(null);
            return;
        }
        int sz = Math.min(getWidth(), getHeight());
        if (sz <= 0)
            sz = 70; // default before layout happens
        setIcon(ResourceLoader.getPieceIcon(currentPiece, sz));
        setDisabledIcon(getIcon());
    }
}
