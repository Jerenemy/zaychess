package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

import com.jeremyzay.zaychess.model.pieces.*;
import com.jeremyzay.zaychess.model.util.PlayerColor;

/**
 * Panel that shows captured pieces grouped by color.
 * White's captured pieces (pieces that White lost) are shown at the top,
 * Black's captured pieces at the bottom.
 */
public class CapturedPiecesPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final List<Piece> whiteCaptured = new ArrayList<>(); // white pieces captured by black
    private final List<Piece> blackCaptured = new ArrayList<>(); // black pieces captured by white

    private final JPanel blackCapturedPanel; // pieces captured BY white (black pieces)
    private final JPanel whiteCapturedPanel; // pieces captured BY black (white pieces)
    private final JLabel blackLabel;
    private final JLabel whiteLabel;

    private static final int ICON_SIZE = 28;
    private static final Color BG_COLOR = new Color(250, 250, 250);

    // Piece value ordering: Q=9, R=5, B=3, N=3, P=1
    private static final Comparator<Piece> PIECE_ORDER = (a, b) -> {
        return Integer.compare(pieceValue(b), pieceValue(a)); // descending
    };

    public CapturedPiecesPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setPreferredSize(new Dimension(180, 400));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- Black captured pieces (pieces captured BY white) ---
        blackLabel = new JLabel("Captured by White");
        blackLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        blackLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(blackLabel);
        content.add(Box.createRigidArea(new Dimension(0, 4)));

        blackCapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        blackCapturedPanel.setBackground(BG_COLOR);
        blackCapturedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(blackCapturedPanel);

        content.add(Box.createRigidArea(new Dimension(0, 12)));

        // --- White captured pieces (pieces captured BY black) ---
        whiteLabel = new JLabel("Captured by Black");
        whiteLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        whiteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(whiteLabel);
        content.add(Box.createRigidArea(new Dimension(0, 4)));

        whiteCapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        whiteCapturedPanel.setBackground(BG_COLOR);
        whiteCapturedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(whiteCapturedPanel);

        content.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    /** Add a captured piece to the display. */
    public void addCapturedPiece(Piece piece) {
        if (piece == null)
            return;
        if (piece instanceof King)
            return; // kings are never captured

        if (piece.getColor() == PlayerColor.WHITE) {
            whiteCaptured.add(piece);
        } else {
            blackCaptured.add(piece);
        }
        refreshDisplay();
    }

    /** Remove the last captured piece of a given color (for undo). */
    public void undoCapture(Piece piece) {
        if (piece == null)
            return;
        List<Piece> list = (piece.getColor() == PlayerColor.WHITE) ? whiteCaptured : blackCaptured;
        // Remove the last occurrence matching this piece type
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).getClass() == piece.getClass()) {
                list.remove(i);
                break;
            }
        }
        refreshDisplay();
    }

    /** Clear all captured pieces (for new game). */
    public void clear() {
        whiteCaptured.clear();
        blackCaptured.clear();
        refreshDisplay();
    }

    private void refreshDisplay() {
        blackCapturedPanel.removeAll();
        whiteCapturedPanel.removeAll();

        // Sort and display black captured pieces
        List<Piece> sortedBlack = new ArrayList<>(blackCaptured);
        sortedBlack.sort(PIECE_ORDER);
        for (Piece p : sortedBlack) {
            blackCapturedPanel.add(createPieceLabel(p));
        }

        // Sort and display white captured pieces
        List<Piece> sortedWhite = new ArrayList<>(whiteCaptured);
        sortedWhite.sort(PIECE_ORDER);
        for (Piece p : sortedWhite) {
            whiteCapturedPanel.add(createPieceLabel(p));
        }

        // Update point differential
        int whiteMaterial = whiteCaptured.stream().mapToInt(CapturedPiecesPanel::pieceValue).sum();
        int blackMaterial = blackCaptured.stream().mapToInt(CapturedPiecesPanel::pieceValue).sum();
        int diff = blackMaterial - whiteMaterial;
        String whiteAdv = diff > 0 ? "  (+" + diff + ")" : "";
        String blackAdv = diff < 0 ? "  (+" + (-diff) + ")" : "";
        blackLabel.setText("Captured by White" + whiteAdv);
        whiteLabel.setText("Captured by Black" + blackAdv);

        revalidate();
        repaint();
    }

    private JLabel createPieceLabel(Piece p) {
        Icon icon = ResourceLoader.getPieceIcon(p, ICON_SIZE);
        JLabel label = new JLabel(icon);
        label.setToolTipText(p.getClass().getSimpleName());
        return label;
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
