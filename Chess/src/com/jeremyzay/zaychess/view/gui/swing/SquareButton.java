package com.jeremyzay.zaychess.view.gui.swing;

import javax.swing.JButton;
import java.awt.*;
import com.jeremyzay.zaychess.model.pieces.Piece;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.view.gui.ResourceLoader;

/**
 * A single square button on the chessboard.
 * Handles:
 *  - Background color (light/dark)
 *  - Piece icon rendering (scaled to button size)
 *  - Highlight overlay (for legal moves / selection)
 */
public class SquareButton extends JButton {
    private static final long serialVersionUID = 1L;
	private final Position position;
    private final Color baseColor;
    private boolean highlighted;
    private Piece currentPiece; // remembers current piece for scaling icon
    private final Color highlightColor = new Color(255,0,255);

    public SquareButton(Position position, Color baseColor) {
        this.position = position;
        this.baseColor = baseColor;
        setContentAreaFilled(false);
        setOpaque(false);
        setFocusPainted(false);
        setBorderPainted(false);
        
        // Auto-rescale piece icon when resized
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                updateScaledIcon();
            }
        });
    }
// //
    /** Enable or disable square highlight. */
    public void setHighlighted(boolean h) {
        if (highlighted != h) { highlighted = h; repaint(); }
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(baseColor); g2.fillRect(0, 0, getWidth(), getHeight());
        if (highlighted) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.35f));
            g2.setColor(highlightColor);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
        super.paintComponent(g); // paints the piece icon
    }

    public Position getPosition() { return position; }
    
    /** Place a piece on this square and update its icon. */
    public void setPiece(Piece piece) {
        currentPiece = piece;
        updateScaledIcon();
    }

    /** Scale piece icon to current square size. */
    private void updateScaledIcon() {
        if (currentPiece == null) { setIcon(null); return; }
        int sz = Math.min(getWidth(), getHeight());
        if (sz <= 0) sz = 70; // default before layout happens
        setIcon(ResourceLoader.getPieceIcon(currentPiece, sz));
        setDisabledIcon(getIcon());
    }
}
