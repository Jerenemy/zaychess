package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import javax.swing.*;

/**
 * Panel for multiplayer options: host, join, or go back.
 * Draws background image like main menu.
 */
public class MultiplayerMenuPanel extends JPanel {
	private static final long serialVersionUID = -7348860546079045900L;

	private final Image backgroundImage = ResourceLoader.MAIN_MENU_BACKGROUND;

	public MultiplayerMenuPanel(JFrame frame, MainMenuFrame parent) {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.insets = new Insets(5, 0, 5, 0);

		Dimension buttonSize = new Dimension(180, 30);

		JButton hostButton = new JButton("Spiel hosten");
		JButton joinButton = new JButton("Spiel beitreten");
		JButton backButton = new JButton("Zurück");

		// size
		hostButton.setPreferredSize(buttonSize);
		joinButton.setPreferredSize(buttonSize);
		backButton.setPreferredSize(buttonSize);

		// actions
		hostButton.addActionListener(e -> {
			parent.startHostGame();
			frame.dispose();
		});

		joinButton.addActionListener(e -> {
			String ip = JOptionPane.showInputDialog(frame, "IP-Adresse des Hosts:", "Verbinden",
					JOptionPane.QUESTION_MESSAGE);
			if (ip != null && !ip.trim().isEmpty()) {
				parent.startClientGame(ip.trim());
				frame.dispose();
			}
		});

		JButton onlineButton = new JButton("Online-Warteschlange");
		onlineButton.setPreferredSize(buttonSize);
		onlineButton.addActionListener(e -> {
			parent.startOnlineMatchmaking();
			frame.dispose();
		});

		add(onlineButton, gbc);

		backButton.addActionListener(e -> {
			parent.setVisible(true);
			frame.dispose();
		});

		add(hostButton, gbc);
		add(joinButton, gbc);
		add(backButton, gbc);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (backgroundImage != null) {
			g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
		}
	}
}
