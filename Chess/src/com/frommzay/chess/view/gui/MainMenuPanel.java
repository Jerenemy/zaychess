package com.frommzay.chess.view.gui;

import java.awt.*;
import javax.swing.*;

/**
 * Panel for the main menu UI.
 * Displays buttons for starting games, loading saves, multiplayer, and quitting.
 * Draws the main menu background image.
 */
public class MainMenuPanel extends JPanel {
	private static final long serialVersionUID = 3788552984549957863L;

	private final Image backgroundImage = ResourceLoader.MAIN_MENU_BACKGROUND;

	public MainMenuPanel(MainMenuFrame frame) {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.insets = new Insets(5, 0, 5, 0);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setOpaque(false);
		buttonPanel.setLayout(new GridLayout(4, 1, 10, 10));

		Dimension buttonSize = new Dimension(180, 30);

		// Create menu buttons
		JButton localGameButton = new JButton("Lokales Spiel starten");
		JButton loadGameButton = new JButton("Spiel laden");
		JButton multiplayerButton = new JButton("Multiplayer");
		JButton quitButton = new JButton("Beenden");

		for (JButton button : new JButton[] { localGameButton, loadGameButton, multiplayerButton, quitButton }) {
			button.setPreferredSize(buttonSize);
			buttonPanel.add(button);
		}

		// Actions
		localGameButton.addActionListener(e -> frame.startLocalGame());
		loadGameButton.addActionListener(e -> frame.loadLocalGame());
		multiplayerButton.addActionListener(e -> frame.openMultiplayerMenu());
		quitButton.addActionListener(e -> System.exit(0));

		add(buttonPanel, gbc);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (backgroundImage != null) {
			g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
		}
	}
}
