package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import javax.swing.*;

/**
 * Panel that shows the chronological list of moves made in the game.
 */
public class MoveListPanel extends JPanel {
	private static final long serialVersionUID = 4252547406430916548L;

	private final JTextArea moveListArea;
	private final Color bgColor = new Color(250, 250, 250);

	public MoveListPanel() {
		setLayout(new BorderLayout());
		moveListArea = new JTextArea();
		moveListArea.setEditable(false);
        moveListArea.setBackground(bgColor); 
        moveListArea.setMargin(new Insets(6, 8, 6, 8));
        moveListArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
		JScrollPane scrollPane = new JScrollPane(moveListArea);
		add(scrollPane, BorderLayout.CENTER);
		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		setPreferredSize(new Dimension(300, 400));
	}

	/** Append a new move line to the list. */
	public void appendMove(String move) {
		moveListArea.append(move + "\n");
		System.out.println(move);
	}

	/** Remove a specific move string from the history and rebuild list. */
	public void removeMove(java.util.List<String> moves, String move) {
		clearMoves();
		for (String moveString : moves) {
			if (!moveString.equals(move))
				appendMove(moveString);
		}
	}

	/** Clear all moves from the list. */
	public void clearMoves() {
		moveListArea.setText("");
	}
}
