package com.frommzay.chess.view.gui;

import java.awt.*;
import java.io.Serial;
import javax.swing.*;

/**
 * Panel for the main menu UI.
 * Displays buttons for starting games, loading saves, multiplayer, and quitting.
 * Draws the main menu background image.
 */
public class MainMenuPanel extends JPanel {
	@Serial
    private static final long serialVersionUID = 3788552984549957863L;

	private final Image backgroundImage = ResourceLoader.MAIN_MENU_BACKGROUND;

    // simple example; adapt to your existing panel layout
    public MainMenuPanel(MainMenuFrame owner) {
        setLayout(new GridLayout(0, 1, 12, 12));

        JButton local = new JButton("Local Game");
        JButton vsAI  = new JButton("Play vs Computer");
        JButton load  = new JButton("Load Saved Game");
        JButton host  = new JButton("Host Multiplayer");
        JButton join  = new JButton("Join Multiplayer");

        add(local); add(vsAI); add(load); add(host); add(join);

        local.addActionListener(e -> owner.startLocalGame());
        vsAI.addActionListener(e -> owner.startVsComputer());
        load.addActionListener(e -> owner.loadLocalGame());
        host.addActionListener(e -> owner.startHostGame());
        join.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog(owner, "Host IP:");
            if (ip != null && !ip.isBlank()) owner.startClientGame(ip.trim());
        });
    }


    @Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (backgroundImage != null) {
			g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
		}
	}
}
