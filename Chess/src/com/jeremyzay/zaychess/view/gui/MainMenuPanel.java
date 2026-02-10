package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.io.Serial;
import javax.swing.*;

/**
 * Panel for the main menu UI.
 * Displays buttons for starting games, loading saves, multiplayer, and
 * quitting.
 * Draws the main menu background image.
 */
public class MainMenuPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 3788552984549957863L;

    private final Image backgroundImage = ResourceLoader.MAIN_MENU_BACKGROUND;
    private final int uiHeight;
    private final int uiWidth;

    public MainMenuPanel(MainFrame owner, int targetHeight) {
        // --- compute width from image aspect ratio ---
        int imgW = 0, imgH = 0;
        if (backgroundImage != null) {
            ImageIcon ii = new ImageIcon(backgroundImage); // forces load; reliable dims
            imgW = ii.getIconWidth();
            imgH = ii.getIconHeight();
        }
        if (imgW <= 0 || imgH <= 0) { // fallback to a safe ratio if something’s wrong
            imgW = 1920;
            imgH = 1080;
        }

        this.uiHeight = targetHeight;
        this.uiWidth = (int) Math.round(targetHeight * (imgW / (double) imgH));

        // fix the panel size to this image‑scaled size
        Dimension d = new Dimension(uiWidth, uiHeight);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);

        // your existing layout and buttons...
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalGlue());
        JButton local = createMenuButton("Local Game");
        JButton vsAI = createMenuButton("Play vs Computer");
        JButton load = createMenuButton("Load Saved Game");

        int vertSeparation = 0;
        add(center(local));
        add(Box.createRigidArea(new Dimension(0, vertSeparation)));
        add(center(vsAI));
        add(Box.createRigidArea(new Dimension(0, vertSeparation)));
        add(center(load));
        add(Box.createRigidArea(new Dimension(0, vertSeparation)));

        JButton online = createMenuButton("Online Matchmaking");
        online.addActionListener(e -> owner.startOnlineMatchmaking());
        add(center(online));
        add(Box.createRigidArea(new Dimension(0, vertSeparation)));

        add(Box.createVerticalGlue());

        local.addActionListener(e -> owner.startLocalGame());
        vsAI.addActionListener(e -> owner.startVsComputer());
        load.addActionListener(e -> owner.loadLocalGame());

    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // base + hover/press alpha
                float alpha = 0.35f;
                ButtonModel m = getModel();
                if (m.isRollover())
                    alpha = 0.50f;
                if (m.isPressed())
                    alpha = 0.65f;

                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g2.setColor(new Color(255, 255, 255)); // or new Color(255,0,0) if you want red tint
                int arc = 14;
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();

                super.paintComponent(g); // paints the text/icon
            }
        };

        // fixed size, centered, and NO default square
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(220, 45));
        button.setMaximumSize(new Dimension(220, 45));
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setForeground(Color.BLACK);

        // IMPORTANT: disable default background painting
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        // button.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        // button.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));

        return button;
    }

    private Component center(JButton button) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        // reduce vertical gap (was 15 → try 5 for tighter stacking)
        wrapper.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 5));
        wrapper.add(button);
        return wrapper;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, uiWidth, uiHeight, this);
        }
    }

}
