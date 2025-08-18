package com.frommzay.chess.view.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;

import com.frommzay.chess.controller.input.MouseInputHandler;
import com.frommzay.chess.model.board.Board;
import com.frommzay.chess.model.util.Position;
import com.frommzay.chess.view.gui.swing.SquareButton;

/**
 * GUI component for displaying the chessboard as an 8x8 grid of SquareButtons.
 *
 * Handles rendering of pieces and light/dark square coloring.
 */
public class BoardPanel extends JPanel {
	private static final long serialVersionUID = -5701354526177242530L;

	private static final int BOARD_SIZE = 8;
	private final SquareButton[][] squares = new SquareButton[BOARD_SIZE][BOARD_SIZE];
	private final MouseInputHandler mouseInputHandler;

	// Board square colors
	private final Color darkSqColor = new Color(32, 108, 125);
	private final Color lightSqColor = new Color(160, 221, 235);
	
	public BoardPanel(Board board, MouseInputHandler mouseInputHandler) {
		this.mouseInputHandler = mouseInputHandler;
		setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		initializeBoard(board);
	}

	/**
	 * Initialize the 8x8 grid of square buttons from the given board state.
	 */
	private void initializeBoard(Board board) {
	    removeAll();
	    for (int row = 0; row < BOARD_SIZE; row++) {
	        for (int col = 0; col < BOARD_SIZE; col++) {
	            Position position = new Position(row, col);

	            boolean light = ((row + col) % 2 == 0);
	            Color base = light ? lightSqColor : darkSqColor;

	            // Create square button with base square color
	            SquareButton button = new SquareButton(position, base);
	            button.setBorderPainted(false);

	            // Place current piece on the square
	            button.setPiece(board.getPieceAt(position));
	            button.addActionListener(mouseInputHandler);

	            squares[row][col] = button;

	            button.setPreferredSize(new Dimension(70, 70));
	            add(button);
	        }
	    }
	    revalidate();
	    repaint();
	}

	/**
	 * Refresh all buttons to match the current board state.
	 */
	public void updateBoard(Board board) {
		for (int row = 0; row < BOARD_SIZE; row++) {
			for (int col = 0; col < BOARD_SIZE; col++) {
				squares[row][col].setPiece(board.getPieceAt(new Position(row, col)));
			}
		}
		repaint();
	}

	/**
	 * Accessor for a specific square button by board position.
	 */
	public SquareButton getSquareButton(Position position) {
		return squares[position.getRank()][position.getFile()];
	}
}
