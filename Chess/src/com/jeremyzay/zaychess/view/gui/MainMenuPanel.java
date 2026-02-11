package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Revamped minimalist main menu with Robust Dynamic Scaling.
 * Guaranteed to fit even in wide/short windows by calculating scale based on
 * actual available height after decorations.
 */
public class MainMenuPanel extends JPanel {

    // --- Color Palette ---
    private static final Color BG_GRAY = new Color(200, 200, 200); // #c8c8c8
    private static final Color DARK_TEAL = new Color(59, 107, 124); // #3b6b7c
    private static final Color LIGHT_BLUE = new Color(174, 221, 236); // #aeddec
    private static final Color BLACK = new Color(15, 15, 15); // #0f0f0f
    private static final Color WHITE = new Color(255, 255, 255); // #ffffff

    // --- Stripe Sequences ---
    private static final Color[] STRIPE_NORTH_SEQ = { WHITE, DARK_TEAL, LIGHT_BLUE, BLACK };
    private static final Color[] STRIPE_ABOVE_SEQ = { BLACK, WHITE };
    private static final Color[] STRIPE_BOTTOM_SEQ = { WHITE, BLACK };

    // --- Scaling Reference Values ---
    private static final int REF_W = 1200;
    private static final int REF_MENU_H = 480; // Estimated height of (Title + 4 Buttons + Insets) at scale 1.0
    private static final float MIN_SCALE = 0.1f; // User adjustable floor

    // --- Dynamic State ---
    private float scale = 1.0f;
    private JLabel titleLabel;
    private List<JButton> buttons = new ArrayList<>();
    private JPanel stripeNorth, stripeAbove, stripeBottom;
    private JPanel centerPanel;

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
        add(createBottomDecoration(), BorderLayout.SOUTH);

        // --- Handle Resizing ---
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateScaling();
            }
        });
    }

    private void updateScaling() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0)
            return;

        // 1. Determine Fixed Decoration Heights
        // North stripes height: 4 strips * 6px (nominal) * scale
        // We'll use a rough initial scale of w/REF_W for the decoration "greed"
        float tempScale = (float) w / REF_W;
        if (tempScale < MIN_SCALE)
            tempScale = MIN_SCALE;

        int nH = (int) (6 * tempScale) * STRIPE_NORTH_SEQ.length;
        int checkH = w / 8; // Checkerboard greedy squares
        int sH = checkH + (int) (6 * tempScale) * (STRIPE_ABOVE_SEQ.length + STRIPE_BOTTOM_SEQ.length);

        // 2. Calculate Available Height for Menu
        int availableMenuH = h - nH - sH;

        // 3. Compute Scaling Factor to fit available height
        float scaleH = (float) availableMenuH / REF_MENU_H;
        float scaleW = (float) w / REF_W;

        // Final scale is limited by width but MUST respect available height
        scale = Math.min(scaleW, scaleH);
        if (scale < MIN_SCALE)
            scale = MIN_SCALE;

        // --- Apply Scaling ---

        // 1. Stripes (Update to actual scale)
        int finalStripeH = (int) (6 * scale);
        if (finalStripeH < 1)
            finalStripeH = 1;
        stripeNorth.setPreferredSize(new Dimension(0, finalStripeH * STRIPE_NORTH_SEQ.length));
        stripeAbove.setPreferredSize(new Dimension(0, finalStripeH * STRIPE_ABOVE_SEQ.length));
        stripeBottom.setPreferredSize(new Dimension(0, finalStripeH * STRIPE_BOTTOM_SEQ.length));

        // 2. Title
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, (int) (64 * scale)));

        // 3. Buttons
        // Slightly smaller base size for better balance: 260x64
        Dimension btnAlloc = new Dimension((int) (260 * scale), (int) (66 * scale));
        for (JButton btn : buttons) {
            btn.setFont(new Font("SansSerif", Font.BOLD, (int) (20 * scale)));
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
            gbcBtn.insets = new Insets((int) (8 * scale), 0, (int) (8 * scale), 0);
            gbl.setConstraints(btn, gbcBtn);
        }

        revalidate();
        repaint();
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
                // Keep squares: height = width / 8
                return new Dimension(w, Math.max(1, w / 8));
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
