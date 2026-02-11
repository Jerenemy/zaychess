package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;

/**
 * Panel that shows the chronological list of moves made in the game.
 * Supports dynamic resizing based on panel height to match CapturedPiecesPanel.
 */
public class MoveListPanel extends JPanel {
	private static final long serialVersionUID = 4252547406430916548L;

	private final JTextArea moveListArea;
	private final Color bgColor = new Color(250, 250, 250);
	private static final int MAX_ROWS = 18; // Match CapturedPiecesPanel for symmetry

	public MoveListPanel() {
		setLayout(new BorderLayout());
		setBackground(bgColor);

		moveListArea = new JTextArea();
		moveListArea.setEditable(false);
		moveListArea.setBackground(bgColor);
		moveListArea.setMargin(new Insets(6, 8, 6, 8));
		moveListArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

		JScrollPane scrollPane = new JScrollPane(moveListArea);
		scrollPane.setBorder(null); // Optional: remove border to match look
		add(scrollPane, BorderLayout.CENTER);

		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

		// Add resize listener to trigger re-layout
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				revalidate(); // Recalculate preferred size based on new height
				repaint();
			}
		});
	}

	/** Append a new move line to the list. */
	public void appendMove(String move) {
		moveListArea.append(move + "\n");
		// Auto-scroll to bottom
		moveListArea.setCaretPosition(moveListArea.getDocument().getLength());
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

	@Override
	public Dimension getPreferredSize() {
		// Calculate width based on height to match CapturedPiecesPanel
		int h = getHeight();
		if (h == 0)
			h = 400; // Default fallback
		if (h < 100)
			h = 100;

		int rowHeight = Math.max(20, h / MAX_ROWS);
		int width = (rowHeight * 2) + 16;

		return new Dimension(width, h);
	}
}
