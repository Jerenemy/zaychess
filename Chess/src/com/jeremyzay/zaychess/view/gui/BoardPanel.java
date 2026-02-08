package com.jeremyzay.zaychess.view.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;

import com.jeremyzay.zaychess.controller.input.MouseInputHandler;
import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.view.gui.swing.SquareButton;

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

	// Default to White's perspective (Rank 0 at top-left/start if logic assumes
	// that)
	// Actually, usually Rank 0 is bottom in chess math, but here (0,0) seems mapped
	// to visual top-left?
	// Verified: GameController says (0,0) is a8. So standard view `row=0` is top.
	// White View: Top-Left is (0,0).
	// Black View: Top-Left is (7,7).
	private com.jeremyzay.zaychess.model.util.PlayerColor orientation = com.jeremyzay.zaychess.model.util.PlayerColor.WHITE;

	public BoardPanel(Board board, MouseInputHandler mouseInputHandler) {
		this.mouseInputHandler = mouseInputHandler;
		setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		initializeBoard(board);
	}

	public void setOrientation(com.jeremyzay.zaychess.model.util.PlayerColor color) {
		this.orientation = color;
		// We need the current board state to re-init.
		// We can't easily get it from here unless we store it or ask controller.
		// Actually initializeBoard takes a board.
		// We will need to make sure the caller passes the board or we store it.
		// Since updateBoard passes a board, let's just trigger a re-layout knowing
		// that the next updateBoard will fix pieces if they changed,
		// OR we need the board here.
		// Let's modify setOrientation to take Board, or just store the last seen board?
		// Simpler: The caller usually has the board.
	}

	/**
	 * Updates orientation and re-initializes the board UI components.
	 */
	public void setOrientationAndInit(com.jeremyzay.zaychess.model.util.PlayerColor color, Board board) {
		if (this.orientation != color) {
			this.orientation = color;
			initializeBoard(board);
		}
	}

	public com.jeremyzay.zaychess.model.util.PlayerColor getOrientation() {
		return orientation;
	}

	/**
	 * Initialize the 8x8 grid of square buttons from the given board state.
	 */
	private void initializeBoard(Board board) {
		removeAll();

		boolean isWhite = (orientation == com.jeremyzay.zaychess.model.util.PlayerColor.WHITE);

		// Iterate UI slots: row 0 is top, col 0 is left.
		for (int uiRow = 0; uiRow < BOARD_SIZE; uiRow++) {
			for (int uiCol = 0; uiCol < BOARD_SIZE; uiCol++) {

				// Map UI position to Logical Position
				// If White: (0,0) UI -> (0,0) Logical [a8]
				// If Black: (0,0) UI -> (7,7) Logical [h1]
				int r = isWhite ? uiRow : (BOARD_SIZE - 1 - uiRow);
				int c = isWhite ? uiCol : (BOARD_SIZE - 1 - uiCol);

				Position position = new Position(r, c);

				// Color logic: strictly based on logical position sum
				// square (0,0) is always light (a8) in standard coloring?
				// Wait, a8 is light. (0+0)%2=0 -> light.
				// h1 (7,7) is light. (7+7)%2=0 -> light.
				boolean light = ((r + c) % 2 == 0);
				Color base = light ? lightSqColor : darkSqColor;

				// Create square button with base square color
				SquareButton button = new SquareButton(position, base);
				button.setBorderPainted(false);

				// Place current piece on the square
				button.setPiece(board.getPieceAt(position));
				button.addActionListener(mouseInputHandler);

				squares[r][c] = button; // Store in logical indices

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
