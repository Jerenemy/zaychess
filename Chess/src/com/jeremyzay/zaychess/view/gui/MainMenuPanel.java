package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import com.jeremyzay.zaychess.model.pieces.*;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.view.gui.theme.BoardTheme;

/**
 * Revamped minimalist main menu with Precise Dynamic Scaling.
 * Guaranteed to fit even in extreme window shapes by using an algebraic
 * fit-to-space calculation for the scale factor.
 */
public class MainMenuPanel extends JPanel {

    // --- Color Palette ---
    private static final Color BG_GRAY = new Color(200, 200, 200); // #c8c8c8
    private static final Color DARK_TEAL = BoardTheme.DARK_TEAL_DEFAULT;
    private static final Color LIGHT_BLUE = BoardTheme.LIGHT_SQUARE_DEFAULT;
    private static final Color BLACK = new Color(15, 15, 15); // #0f0f0f
    private static final Color WHITE = new Color(255, 255, 255); // #ffffff

    // --- Stripe Sequences ---
    private static final Color[] STRIPE_NORTH_SEQ = { WHITE, DARK_TEAL, LIGHT_BLUE, BLACK };
    private static final Color[] STRIPE_ABOVE_SEQ = { BLACK, WHITE };
    private static final Color[] STRIPE_BOTTOM_SEQ = { WHITE, BLACK };

    // --- Scaling Reference Values ---
    // Reference values represent the size at scale 1.0
    private static final int REF_W = 1000;
    private static final int REF_MENU_H = 600; // Total vertical space needed for Menu (Title + Buttons + Insets)
    private static final int STRIPE_UNITS = 8; // Total number of 6px nominal strips (4 top, 2+2 bottom)
    private static final int STRIPE_NOMINAL_H = 6;
    private static final float MIN_SCALE = 0.1f;

    // --- Dynamic State ---
    private float scale = 1.0f;
    private JLabel titleLabel;
    private final List<JButton> buttons = new ArrayList<>();
    private JPanel centerPanel, southPanel;
    private JPanel stripeNorth, stripeAbove, stripeBottom;

    // --- Interactive Toys ---
    private final List<MenuPiece> menuPieces = new ArrayList<>();
    private DragGlassPane dragGlassPane;
    private MenuPiece draggedPiece = null;
    private Point pressPointScreen = null;
    private boolean isDraggingToy = false;
    private static final int TOY_DRAG_THRESHOLD = 5;

    public MainMenuPanel(MainFrame frame, int width) {
        this();
    }

    public MainMenuPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_GRAY);

        // 1. Top Decoration
        stripeNorth = createStripePanel(STRIPE_NORTH_SEQ);
        add(stripeNorth, BorderLayout.NORTH);

        // 2. Center Menu
        centerPanel = createCenterMenu();
        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Decoration
        southPanel = createBottomDecoration();
        add(southPanel, BorderLayout.SOUTH);

        // 4. Interactive Toys
        initMenuPieces();
        initToyInteractivity();

        // --- Handle Resizing ---
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateScaling();
            }
        });
    }

    /**
     * Enables or disables all menu buttons.
     * Used to prevent concurrent session starts during reset.
     */
    public void setMenuEnabled(boolean enabled) {
        for (JButton btn : buttons) {
            btn.setEnabled(enabled);
        }
    }

    private void updateScaling() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0)
            return;

        // --- Algebraic Scaling Calculation ---
        // Rule: H = (REF_MENU_H * scale) + (w / 8 [Checkerboard]) + (STRIPE_UNITS *
        // STRIPE_NOMINAL_H * scale)
        // scale * (REF_MENU_H + STRIPE_UNITS * STRIPE_NOMINAL_H) = H - (w / 8)

        int checkH = w / 8;
        int availableSpaceForScaleParts = h - checkH;
        int totalUnitsForScale = REF_MENU_H + (STRIPE_UNITS * STRIPE_NOMINAL_H);

        float scaleH = (float) availableSpaceForScaleParts / totalUnitsForScale;
        float scaleW = (float) w / REF_W;

        // Final scale is the minimum of height fit and width fit
        scale = Math.min(scaleW, scaleH);

        // Safety floor
        if (scale < MIN_SCALE)
            scale = MIN_SCALE;

        // --- Apply Scaling ---

        // 1. Stripes
        int finalStripeH = (int) (STRIPE_NOMINAL_H * scale);
        if (finalStripeH < 1)
            finalStripeH = 1;
        stripeNorth.setPreferredSize(new Dimension(0, finalStripeH * STRIPE_NORTH_SEQ.length));
        stripeAbove.setPreferredSize(new Dimension(0, finalStripeH * STRIPE_ABOVE_SEQ.length));
        stripeBottom.setPreferredSize(new Dimension(0, finalStripeH * STRIPE_BOTTOM_SEQ.length));

        // 2. Title
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (64 * scale)));

        // 3. Buttons
        Dimension btnAlloc = new Dimension((int) (300 * scale), (int) (72 * scale));
        for (JButton btn : buttons) {
            btn.setFont(new Font("SansSerif", Font.BOLD, (int) (22 * scale)));
            btn.setPreferredSize(btnAlloc);
            btn.setMinimumSize(btnAlloc);
            btn.setMaximumSize(btnAlloc);
        }

        // 4. Update Spacing (Insets)
        GridBagLayout gbl = (GridBagLayout) centerPanel.getLayout();
        GridBagConstraints gbcTitle = gbl.getConstraints(titleLabel);
        gbcTitle.insets = new Insets((int) (50 * scale), 0, (int) (40 * scale), 0);
        gbl.setConstraints(titleLabel, gbcTitle);

        for (JButton btn : buttons) {
            GridBagConstraints gbcBtn = gbl.getConstraints(btn);
            gbcBtn.insets = new Insets((int) (10 * scale), 0, (int) (10 * scale), 0);
            gbl.setConstraints(btn, gbcBtn);
        }

        revalidate();
        repaint();
    }

    private void initMenuPieces() {
        PlayerColor color = PlayerColor.BLACK;
        menuPieces.add(new MenuPiece(new Rook(color, new Position(0, 0)), 0));
        menuPieces.add(new MenuPiece(new Knight(color, new Position(0, 1)), 1));
        menuPieces.add(new MenuPiece(new Bishop(color, new Position(0, 2)), 2));
        menuPieces.add(new MenuPiece(new Queen(color, new Position(0, 3)), 3));
        menuPieces.add(new MenuPiece(new King(color, new Position(0, 4)), 4));
        menuPieces.add(new MenuPiece(new Bishop(color, new Position(0, 5)), 5));
        menuPieces.add(new MenuPiece(new Knight(color, new Position(0, 6)), 6));
        menuPieces.add(new MenuPiece(new Rook(color, new Position(0, 7)), 7));
    }

    private void initToyInteractivity() {
        MouseAdapter ma = new MouseAdapter() {
            private Point getPanelPoint(MouseEvent e) {
                return SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), MainMenuPanel.this);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                Point p = getPanelPoint(e);
                for (MenuPiece mp : menuPieces) {
                    if (mp.getBounds().contains(p)) {
                        draggedPiece = mp;
                        pressPointScreen = e.getLocationOnScreen();
                        isDraggingToy = false;
                        return;
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedPiece == null)
                    return;

                if (!isDraggingToy) {
                    double dist = e.getLocationOnScreen().distance(pressPointScreen);
                    if (dist > TOY_DRAG_THRESHOLD) {
                        isDraggingToy = true;

                        // Robust Attachment: Re-attach to the frame every time we start a drag.
                        // This prevents dragging from breaking if showMenu() replaced the glass pane.
                        if (dragGlassPane == null) {
                            dragGlassPane = new DragGlassPane();
                        }
                        MainFrame.getInstance().setGlassPane(dragGlassPane);
                        dragGlassPane.setVisible(true);

                        int size = draggedPiece.getSize();
                        Point centerLocal = new Point((int) draggedPiece.getBounds().getCenterX(),
                                (int) draggedPiece.getBounds().getCenterY());
                        Point originOnGlass = SwingUtilities.convertPoint(MainMenuPanel.this, centerLocal,
                                dragGlassPane);

                        draggedPiece.visible = false;
                        dragGlassPane.startDrag(draggedPiece.piece, null, originOnGlass, size);
                        repaint();
                    }
                }

                if (isDraggingToy && dragGlassPane != null) {
                    Point glassPt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), dragGlassPane);
                    dragGlassPane.updateDrag(glassPt);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDraggingToy && dragGlassPane != null) {
                    dragGlassPane.endDrag(false);

                    final MenuPiece pieceToRestore = draggedPiece;
                    Timer restoreTimer = new Timer(200, ex -> {
                        if (pieceToRestore != null) {
                            pieceToRestore.visible = true;
                            repaint();
                        }
                    });
                    restoreTimer.setRepeats(false);
                    restoreTimer.start();
                } else if (!isDraggingToy && draggedPiece != null) {
                    // It was a click!
                    draggedPiece.cycleColor();
                    repaint();
                }
                draggedPiece = null;
                isDraggingToy = false;
            }
        };

        // Add listeners to panel and decoration areas to ensure pieces in squares are
        // hittable
        addMouseListener(ma);
        addMouseMotionListener(ma);
        centerPanel.addMouseListener(ma);
        centerPanel.addMouseMotionListener(ma);
        southPanel.addMouseListener(ma);
        southPanel.addMouseMotionListener(ma);
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        // Paint menu pieces on top of children (stripes/checkerboard)
        for (MenuPiece mp : menuPieces) {
            mp.paint(g);
        }
    }

    private class MenuPiece {
        Piece piece;
        final int col;
        boolean visible = true;

        MenuPiece(Piece piece, int col) {
            this.piece = piece;
            this.col = col;
        }

        void cycleColor() {
            PlayerColor next = (piece.getColor() == PlayerColor.BLACK) ? PlayerColor.WHITE : PlayerColor.BLACK;
            Position pos = piece.getPos();
            if (piece instanceof Rook)
                piece = new Rook(next, pos);
            else if (piece instanceof Knight)
                piece = new Knight(next, pos);
            else if (piece instanceof Bishop)
                piece = new Bishop(next, pos);
            else if (piece instanceof Queen)
                piece = new Queen(next, pos);
            else if (piece instanceof King)
                piece = new King(next, pos);
            else if (piece instanceof Pawn)
                piece = new Pawn(next, pos);
        }

        Rectangle getBounds() {
            int w = getWidth();
            int h = getHeight();
            int checkH = w / 8;

            // Size relative to squares (80% of square width)
            int size = (int) (checkH * 0.80);

            // Center in the checkerboard column
            double colWidth = (double) w / 8.0;
            int x = (int) (colWidth * col + (colWidth - size) / 2);

            // Center vertically within the checkerboard row
            // South decoration = stripeAbove + checkerboard + stripeBottom
            int finalStripeH = (int) (STRIPE_NOMINAL_H * scale);
            if (finalStripeH < 1)
                finalStripeH = 1;
            int stripeAboveH = finalStripeH * STRIPE_ABOVE_SEQ.length;
            int stripeBottomH = finalStripeH * STRIPE_BOTTOM_SEQ.length;
            int southH = checkH + stripeAboveH + stripeBottomH;

            // y = top of south panel + stripeAbove height + centering in checker row
            int y = (h - southH) + stripeAboveH + (checkH - size) / 2;

            return new Rectangle(x, y, size, size);
        }

        int getSize() {
            int w = getWidth();
            int checkH = w / 8;
            return (int) (checkH * 0.80);
        }

        void paint(Graphics g) {
            if (!visible)
                return;
            Rectangle b = getBounds();
            Icon icon = ResourceLoader.getPieceIcon(piece, b.width);
            icon.paintIcon(MainMenuPanel.this, g, b.x, b.y);
        }
    }

    private JPanel createStripePanel(final Color[] seq) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                int w = getWidth();
                int totalH = getHeight();
                int h = totalH / seq.length;
                if (h < 1)
                    h = 1;
                for (int i = 0; i < seq.length; i++) {
                    g2.setColor(seq[i]);
                    g2.fillRect(0, i * h, w, (i == seq.length - 1) ? totalH - (i * h) : h);
                }
            }
        };
    }

    private JPanel createCenterMenu() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.CENTER;

        // Title
        titleLabel = new JLabel("ZAYCHESS");
        titleLabel.setForeground(BLACK);
        panel.add(titleLabel, gbc);

        // Buttons
        buttons.add(createMenuButton("Local Game", e -> MainFrame.getInstance().startLocalGame()));
        buttons.add(createMenuButton("ZayBot", e -> MainFrame.getInstance().startVsComputer()));
        buttons.add(createMenuButton("Load Game", e -> MainFrame.getInstance().loadLocalGame()));
        buttons.add(createMenuButton("Multiplayer", e -> MainFrame.getInstance().startOnlineMatchmaking()));

        for (JButton btn : buttons) {
            panel.add(btn, gbc);
        }

        return panel;
    }

    private JPanel createBottomDecoration() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        stripeAbove = createStripePanel(STRIPE_ABOVE_SEQ);
        panel.add(stripeAbove, BorderLayout.NORTH);

        final JPanel checkerboard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = getWidth();
                int h = getHeight();
                for (int i = 0; i < 8; i++) {
                    g.setColor((i % 2 == 0) ? DARK_TEAL : LIGHT_BLUE);
                    int x = (int) ((double) w / 8.0 * i);
                    int nextX = (int) ((double) w / 8.0 * (i + 1));
                    g.fillRect(x, 0, nextX - x, h);
                }
            }

            @Override
            public Dimension getPreferredSize() {
                Container parent = getTopLevelAncestor();
                int w = (parent != null) ? parent.getWidth() : 800;
                // Square cells: Height = Width / 8
                return new Dimension(w, Math.round(w / 8.0f));
            }
        };

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                checkerboard.invalidate();
                panel.revalidate();
            }
        });

        panel.add(checkerboard, BorderLayout.CENTER);

        stripeBottom = createStripePanel(STRIPE_BOTTOM_SEQ);
        panel.add(stripeBottom, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createMenuButton(String text, ActionListener action) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int arc = OverlayPanel.BUTTON_ARC;

                ButtonModel m = getModel();
                boolean hover = m.isRollover();
                boolean press = m.isPressed();

                // Expansion: 6% on hover (scaled)
                float expansion = hover ? 1.06f : 1.0f;
                int curW = (int) (w * 0.94f * expansion);
                int curH = (int) (h * 0.85f * expansion);

                int x = (w - curW) / 2;
                int y = (h - curH) / 2;

                g2.setColor(press ? new Color(220, 220, 220) : (hover ? new Color(245, 245, 245) : Color.WHITE));
                g2.fillRoundRect(x, y, curW, curH, arc, arc);

                g2.setColor(new Color(180, 180, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, y, curW, curH, arc, arc);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setForeground(BLACK);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.repaint();
            }
        });

        return btn;
    }
}
