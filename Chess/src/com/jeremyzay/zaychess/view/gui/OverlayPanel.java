package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import javax.swing.*;

/**
 * Base class for inline overlay panels that replace popup dialogs.
 * Draws a semi-transparent dark backdrop over the full window,
 * with a centered white rounded card containing the subclass content.
 */
public abstract class OverlayPanel extends JPanel {

    private static final Color BACKDROP = new Color(0, 0, 0, 160);
    private static final Color CARD_BG = new Color(255, 255, 255, 240);
    private static final int CARD_ARC = 20;

    private final JPanel card;

    protected OverlayPanel() {
        setOpaque(false);
        setLayout(new GridBagLayout());

        // Block mouse events on underlying components
        addMouseListener(new java.awt.event.MouseAdapter() {
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
        });

        // Create card container
        card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // Populate card with subclass content
        // (done lazily in show(), but we can also do it here)

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    /**
     * Subclasses override this to provide the card content.
     * Called once when the overlay is first shown.
     */
    protected abstract JPanel createContent();

    /**
     * Show this overlay on the MainFrame glass pane.
     */
    public void showOverlay() {
        // Build content if card is empty
        if (card.getComponentCount() == 0) {
            card.add(createContent(), BorderLayout.CENTER);
        }
        MainFrame frame = MainFrame.getInstance();
        frame.setGlassPane(this);
        setVisible(true);
        revalidate();
        repaint();
    }

    /**
     * Remove this overlay from the MainFrame glass pane.
     */
    public void hideOverlay() {
        setVisible(false);
        Container parent = getParent();
        if (parent != null) {
            // Restore a blank glass pane
            MainFrame frame = MainFrame.getInstance();
            JPanel blank = new JPanel();
            blank.setOpaque(false);
            blank.setVisible(false);
            frame.setGlassPane(blank);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Dark backdrop
        g2.setColor(BACKDROP);
        g2.fillRect(0, 0, w, h);

        // 2. White card background (centered around the card component)
        if (card.getWidth() > 0 && card.getHeight() > 0) {
            int cx = card.getX();
            int cy = card.getY();
            int cw = card.getWidth();
            int ch = card.getHeight();
            // Add some padding around the card
            int pad = 8;
            g2.setColor(CARD_BG);
            g2.fillRoundRect(cx - pad, cy - pad, cw + 2 * pad, ch + 2 * pad, CARD_ARC, CARD_ARC);

            // Subtle border
            g2.setColor(new Color(0, 0, 0, 40));
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(cx - pad, cy - pad, cw + 2 * pad, ch + 2 * pad, CARD_ARC, CARD_ARC);
        }

        g2.dispose();
    }

    // --- Utility: styled button matching menu style ---

    protected static JButton createOverlayButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(160, 50));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    protected static JLabel createTitle(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 20));
        label.setForeground(Color.DARK_GRAY);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        return label;
    }
}
