package com.frommzay.chess.view.gui;

import java.awt.BorderLayout;
import javax.swing.*;

/**
 * Window for multiplayer menu.
 * Lets the player choose to host or join a game.
 */
public class MultiplayerMenuFrame extends JFrame {
	private static final long serialVersionUID = 6000562322855155778L;

	public MultiplayerMenuFrame(MainMenuFrame parent) {
		setTitle("Schach – Multiplayer");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		MultiplayerMenuPanel panel = new MultiplayerMenuPanel(this, parent);
		add(panel, BorderLayout.CENTER);

		setSize(768, 432);
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
	}
}
