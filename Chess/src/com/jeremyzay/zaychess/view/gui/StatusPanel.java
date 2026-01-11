package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import com.jeremyzay.zaychess.model.game.GameState;
import java.awt.*;

/**
 * Status bar at the bottom of the window.
 * Displays game state info like current turn or check status.
 */
public class StatusPanel extends JPanel {
	private static final long serialVersionUID = -1761050134017936778L;

	private final JLabel statusLabel;
	private final Color textColor;
	private final Color bgColor;
	
	public StatusPanel() {
		this.textColor = Color.WHITE;
		this.bgColor = new Color(45, 45, 45);
		
		setLayout(new FlowLayout(FlowLayout.LEFT));
		statusLabel = new JLabel("Good luck.");
		add(statusLabel);
		setBackground(bgColor); 
		statusLabel.setForeground(textColor);
	}

	/** Update status text (default color). */
	public void setStatus(String status) {
		statusLabel.setText(status);
		statusLabel.setForeground(textColor);
		System.out.println(status);
	}
	
	/** Update status text with custom foreground color. */
	public void setStatus(String status, Color fg) {
		statusLabel.setText(status);
		statusLabel.setForeground(fg);
		System.out.println(status);
	}

	/** Reset status to ready with current turn. */
	public void setReady(GameState gameState) {
        setStatus("Turn: " + gameState.getTurn().toString(), Color.WHITE);
//		statusLabel.setText("Ready - " + gameState.getTurn().toString());
	}


}
