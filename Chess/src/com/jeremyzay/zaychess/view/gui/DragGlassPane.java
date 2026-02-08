package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

import com.jeremyzay.zaychess.model.pieces.Piece;
import com.jeremyzay.zaychess.model.util.Position;

/**
 * Transparent overlay for rendering dragged chess piece.
 * Supports smooth dragging and snap-back animation for invalid moves.
 */
public class DragGlassPane extends JPanel {
    private static final long serialVersionUID = 1L;

    private javax.swing.Icon pieceIcon;
    private int pieceSize;
    private Point dragPoint;
    private Point originPoint;
    private Position originPosition;
    private boolean animating = false;

    // Animation
    private Timer snapBackTimer;
    private double animProgress = 0;
    private static final int ANIMATION_DURATION_MS = 150;
    private static final int ANIMATION_FPS = 60;

    public DragGlassPane() {
        setOpaque(false);
        setVisible(false);
    }

    /**
     * Start dragging a piece from the given position.
     */
    public void startDrag(Piece piece, Position pos, Point screenPoint, int size) {
        if (piece == null)
            return;

        this.originPosition = pos;
        this.originPoint = screenPoint;
        this.pieceSize = size;
        this.dragPoint = screenPoint;

        // Get piece icon
        this.pieceIcon = ResourceLoader.getPieceIcon(piece, size);

        setVisible(true);
        repaint();
    }

    /**
     * Update drag position as mouse moves.
     */
    public void updateDrag(Point screenPoint) {
        if (!isVisible() || animating)
            return;
        this.dragPoint = screenPoint;
        repaint();
    }

    /**
     * End drag - either complete move or animate snap-back.
     */
    public void endDrag(boolean moveSucceeded) {
        if (!isVisible())
            return;

        if (moveSucceeded) {
            // Move succeeded, just hide
            reset();
        } else {
            // Animate snap-back
            animateSnapBack();
        }
    }

    /**
     * Animate the piece snapping back to origin.
     */
    private void animateSnapBack() {
        if (originPoint == null || dragPoint == null) {
            reset();
            return;
        }

        animating = true;
        animProgress = 0;

        final Point startPoint = new Point(dragPoint);
        final Point endPoint = new Point(originPoint);

        int delay = 1000 / ANIMATION_FPS;
        double progressStep = (double) delay / ANIMATION_DURATION_MS;

        snapBackTimer = new Timer(delay, e -> {
            animProgress += progressStep;

            if (animProgress >= 1.0) {
                ((Timer) e.getSource()).stop();
                reset();
                return;
            }

            // Ease-out cubic for smooth deceleration
            double t = 1 - Math.pow(1 - animProgress, 3);

            int x = (int) (startPoint.x + (endPoint.x - startPoint.x) * t);
            int y = (int) (startPoint.y + (endPoint.y - startPoint.y) * t);
            dragPoint = new Point(x, y);
            repaint();
        });
        snapBackTimer.start();
    }

    /**
     * Reset drag state.
     */
    public void reset() {
        pieceIcon = null;
        dragPoint = null;
        originPoint = null;
        originPosition = null;
        animating = false;
        if (snapBackTimer != null) {
            snapBackTimer.stop();
            snapBackTimer = null;
        }
        setVisible(false);
        repaint();
    }

    public boolean isDragging() {
        return isVisible() && pieceIcon != null && !animating;
    }

    public Position getOriginPosition() {
        return originPosition;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Important: glass pane is transparent, do not call super.paintComponent if
        // opaque is false
        // But we want to support repainting artifacts? No, it's just an overlay.
        // Actually, for GlassPane, we usually shouldn't paint background.
        // But let's follow standard swing glass pane pattern.
        // If opaque is false, we don't need super.paintComponent() unless we have other
        // children?
        // Let's safe-guard it.

        if (pieceIcon == null || dragPoint == null)
            return;

        Graphics2D g2 = (Graphics2D) g.create();

        // Draw piece centered on cursor
        int x = dragPoint.x - pieceSize / 2;
        int y = dragPoint.y - pieceSize / 2;

        // Add subtle shadow during drag
        if (!animating) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            pieceIcon.paintIcon(this, g2, x + 4, y + 4);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        pieceIcon.paintIcon(this, g2, x, y);
        g2.dispose();
    }
}
