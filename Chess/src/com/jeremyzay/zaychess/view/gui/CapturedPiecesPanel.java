package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

import com.jeremyzay.zaychess.model.pieces.*;
import com.jeremyzay.zaychess.model.util.PlayerColor;

/**
 * Panel that shows captured pieces grouped by color.
 * CUSTOM RENDERING IMPLEMENTATION:
 * Dynamically scales piece icons based on panel height to fit all possible
 * captures (15+ per side).
 * Two vertical columns: Left (White Captures), Right (Black Captures).
 * Score headers displayed above columns.
 */
public class CapturedPiecesPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final List<Piece> whiteCaptured = new ArrayList<>(); // white pieces captured by black
    private final List<Piece> blackCaptured = new ArrayList<>(); // black pieces captured by white

    private String whiteScoreText = ""; // Text for right column header (Black Adv)
    private String blackScoreText = ""; // Text for left column header (White Adv)

    private static final Color BG_COLOR = new Color(250, 250, 250);
    private static final int MAX_ROWS = 18; // 15 pieces + header space + margins

    // Piece value ordering: Q=9, R=5, B=3, N=3, P=1
    private static final Comparator<Piece> PIECE_ORDER = (a, b) -> {
        return Integer.compare(pieceValue(b), pieceValue(a)); // descending
    };

    public CapturedPiecesPanel() {
        setBackground(BG_COLOR);
        setToolTipText(""); // Enable tooltips manager

        // Add resize listener to trigger re-layout
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                revalidate(); // Recalculate preferred size based on new height
                repaint();
            }
        });

        // Add mouse motion listener for dynamic tooltips
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateTooltip(e.getPoint());
            }
        };
        addMouseMotionListener(mouseHandler);
    }

    /** Add a captured piece to the display. */
    public void addCapturedPiece(Piece piece) {
        if (piece == null)
            return;
        if (piece instanceof King)
            return;

        if (piece.getColor() == PlayerColor.WHITE) {
            whiteCaptured.add(piece);
        } else {
            blackCaptured.add(piece);
        }
        repaint();
    }

    /** Remove the last captured piece of a given color (for undo). */
    public void undoCapture(Piece piece) {
        if (piece == null)
            return;
        List<Piece> list = (piece.getColor() == PlayerColor.WHITE) ? whiteCaptured : blackCaptured;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getClass() == piece.getClass()) {
                list.remove(i);
                break;
            }
        }
        repaint();
    }

    /** Clear all captured pieces (for new game). */
    public void clear() {
        whiteCaptured.clear();
        blackCaptured.clear();
        repaint();
    }

    public void updateScore(com.jeremyzay.zaychess.model.board.Board board) {
        int whiteMaterial = 0;
        int blackMaterial = 0;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPieceAt(r, c);
                if (p != null) {
                    if (p.getColor() == PlayerColor.WHITE) {
                        whiteMaterial += pieceValue(p);
                    } else {
                        blackMaterial += pieceValue(p);
                    }
                }
            }
        }

        int diff = whiteMaterial - blackMaterial;

        // Left Column: White Captures (Black pieces). Header: White Adv
        if (diff > 0) {
            blackScoreText = "+" + diff;
            whiteScoreText = "";
        } else if (diff < 0) {
            blackScoreText = "";
            whiteScoreText = "+" + (-diff);
        } else {
            blackScoreText = "";
            whiteScoreText = "";
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        // Calculate width based on height to maintain aspect ratio
        int h = getHeight();
        if (h == 0)
            h = 400; // Default fallback

        // Ensure minimum height to avoid division by zero or tiny icons
        if (h < 100)
            h = 100;

        int rowHeight = Math.max(20, h / MAX_ROWS);
        int width = (rowHeight * 2) + 16; // 2 columns + padding

        return new Dimension(width, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int h = getHeight();
        int rowHeight = Math.max(20, h / MAX_ROWS);
        int iconSize = Math.max(16, (int) (rowHeight * 0.85)); // slightly smaller than row height
        int colWidth = rowHeight; // column width roughly equal to row height
        int startY = rowHeight; // Start drawing pieces below header (row 1)
        int leftX = 8 + (colWidth - iconSize) / 2; // Centered in left col
        int rightX = 8 + colWidth + (colWidth - iconSize) / 2; // Centered in right col

        // Draw Headers
        g2.setColor(new Color(50, 50, 50));
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.min(16, iconSize)));
        FontMetrics fm = g2.getFontMetrics();

        if (!blackScoreText.isEmpty()) {
            int textW = fm.stringWidth(blackScoreText);
            g2.drawString(blackScoreText, 8 + colWidth / 2 - textW / 2, startY - 5);
        }
        if (!whiteScoreText.isEmpty()) {
            int textW = fm.stringWidth(whiteScoreText);
            g2.drawString(whiteScoreText, 8 + colWidth + colWidth / 2 - textW / 2, startY - 5);
        }

        // Draw Left Column (Black Pieces captured by White)
        List<Piece> sortedBlack = new ArrayList<>(blackCaptured);
        sortedBlack.sort(PIECE_ORDER);
        drawColumn(g2, sortedBlack, leftX, startY, rowHeight, iconSize);

        // Draw Right Column (White Pieces captured by Black)
        List<Piece> sortedWhite = new ArrayList<>(whiteCaptured);
        sortedWhite.sort(PIECE_ORDER);
        drawColumn(g2, sortedWhite, rightX, startY, rowHeight, iconSize);
    }

    private void drawColumn(Graphics2D g2, List<Piece> pieces, int x, int startY, int rowHeight, int iconSize) {
        int y = startY;
        for (Piece p : pieces) {
            Image img = getPieceImage(p);
            if (img != null) {
                g2.drawImage(img, x, y, iconSize, iconSize, null);
            }
            y += rowHeight;
        }
    }

    private Image getPieceImage(Piece p) {
        // Use ResourceLoader to get the cached base ImageIcon
        // ResourceLoader.getPieceIcon(p) returns the high-res ImageIcon
        ImageIcon icon = ResourceLoader.getPieceIcon(p);
        return icon != null ? icon.getImage() : null;
    }

    private void updateTooltip(Point p) {
        // Calculate which piece is under mouse
        int h = getHeight();
        int rowHeight = Math.max(20, h / MAX_ROWS);
        int colWidth = rowHeight;
        int startY = rowHeight;

        int row = (p.y - startY) / rowHeight;
        int col = (p.x - 8) / colWidth;

        if (row < 0 || col < 0 || col > 1) { // Header or outside
            setToolTipText(null);
            return;
        }

        Piece piece = null;
        if (col == 0) { // Left Col (Black Captures)
            List<Piece> sortedBlack = new ArrayList<>(blackCaptured);
            sortedBlack.sort(PIECE_ORDER);
            if (row < sortedBlack.size())
                piece = sortedBlack.get(row);
        } else { // Right Col
            List<Piece> sortedWhite = new ArrayList<>(whiteCaptured);
            sortedWhite.sort(PIECE_ORDER);
            if (row < sortedWhite.size())
                piece = sortedWhite.get(row);
        }

        if (piece != null) {
            setToolTipText(piece.getClass().getSimpleName());
        } else {
            setToolTipText(null);
        }
    }

    // Usually unused but required for JComponent tooltip support
    @Override
    public String getToolTipText(MouseEvent event) {
        // Logic handled in updateTooltip but Swing calls this too.
        // We set tooltip text dynamically so let's just return current text.
        return super.getToolTipText(event);
    }

    private static int pieceValue(Piece p) {
        if (p instanceof Queen)
            return 9;
        if (p instanceof Rook)
            return 5;
        if (p instanceof Bishop)
            return 3;
        if (p instanceof Knight)
            return 3;
        if (p instanceof Pawn)
            return 1;
        return 0;
    }
}
