package com.jeremyzay.zaychess.view.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.*;
import javax.swing.*;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.controller.input.MouseInputHandler;
import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.pieces.Piece;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.view.gui.swing.SquareButton;

/**
 * GUI component for displaying the chessboard as an 8x8 grid of SquareButtons.
 *
 * Handles rendering of pieces and light/dark square coloring.
 * Supports drag-and-drop piece movement.
 */
public class BoardPanel extends JPanel {
	private static final long serialVersionUID = -5701354526177242530L;

	private static final int BOARD_SIZE = 8;
	private final SquareButton[][] squares = new SquareButton[BOARD_SIZE][BOARD_SIZE];
	private final MouseInputHandler mouseInputHandler;

	// Drag-and-drop state
	private DragGlassPane dragGlassPane;
	private GameController controller;
	private Board currentBoard;
	private Position dragSource;
	private Piece draggedPiece;
	private static final int DRAG_THRESHOLD = 5; // pixels before drag starts
	private Point pressPoint;
	private boolean isDragging = false;

	// Board Theme
	private com.jeremyzay.zaychess.view.gui.theme.BoardTheme currentTheme = com.jeremyzay.zaychess.view.gui.theme.BoardTheme
			.DEFAULT();

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
		this.currentBoard = board;
		setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		initializeBoard(board);
	}

	/** Set the game controller for drag-and-drop move validation */
	public void setController(GameController controller) {
		this.controller = controller;
	}

	/** Set the glass pane for drag rendering */
	public void setDragGlassPane(DragGlassPane pane) {
		this.dragGlassPane = pane;
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
	 * Clear all highlights of a specific type.
	 */
	public void clearHighlights(com.jeremyzay.zaychess.view.gui.theme.HighlightType type) {
		for (int r = 0; r < BOARD_SIZE; r++) {
			for (int c = 0; c < BOARD_SIZE; c++) {
				if (squares[r][c] != null)
					squares[r][c].setHighlight(type, false);
			}
		}
	}

	/**
	 * Set highlight for a specific square.
	 */
	public void setHighlight(Position pos, com.jeremyzay.zaychess.view.gui.theme.HighlightType type, boolean active) {
		if (pos == null)
			return;
		SquareButton btn = getSquareButton(pos);
		if (btn != null)
			btn.setHighlight(type, active);
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
				// Color base = light ? lightSqColor : darkSqColor; -- handled by theme now

				// Create square button with theme
				SquareButton button = new SquareButton(position, light, currentTheme);
				button.setBorderPainted(false);

				// Place current piece on the square
				button.setPiece(board.getPieceAt(position));
				button.addActionListener(mouseInputHandler);

				// Add mouse listeners for drag-and-drop
				final SquareButton btn = button;
				MouseAdapter dragAdapter = new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						handleMousePressed(btn, e);
					}

					@Override
					public void mouseDragged(MouseEvent e) {
						handleMouseDragged(e);
					}

					@Override
					public void mouseReleased(MouseEvent e) {
						handleMouseReleased(e);
					}
				};
				button.addMouseListener(dragAdapter);
				button.addMouseMotionListener(dragAdapter);

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
		this.currentBoard = board;
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

	// ──────────────────────────────────────────────────────────────────────────────
	// Drag-and-Drop Handling
	// ──────────────────────────────────────────────────────────────────────────────

	/**
	 * Handle mouse press - start potential drag
	 */
	public void handleMousePressed(SquareButton source, MouseEvent e) {
		if (dragGlassPane == null || controller == null)
			return;

		// Clear opponent's last move highlight on interaction
		controller.clearLastMoveHighlight();

		Position pos = source.getPosition();

		Piece piece = currentBoard != null ? currentBoard.getPieceAt(pos) : null;

		if (piece != null) {
			dragSource = pos;
			draggedPiece = piece;
			pressPoint = e.getLocationOnScreen();
			isDragging = false;
		}
	}

	/**
	 * Handle mouse drag - move piece with cursor
	 */
	public void handleMouseDragged(MouseEvent e) {
		if (dragGlassPane == null || dragSource == null || draggedPiece == null)
			return;

		Point screenPoint = e.getLocationOnScreen();

		// Start drag after threshold
		if (!isDragging && pressPoint != null) {
			double dist = pressPoint.distance(screenPoint);
			if (dist >= DRAG_THRESHOLD) {
				isDragging = true;

				// UX: If dragging a different piece than currently selected, clear selection
				// highlights
				if (controller != null) {
					Position selected = controller.getSelectedPosition();
					if (selected != null && !selected.equals(dragSource)) {
						controller.clearSelection();
					}
				}

				// Hide piece on source square
				SquareButton sourceBtn = getSquareButton(dragSource);
				sourceBtn.setPiece(null);

				// Get piece size from button
				int size = Math.min(sourceBtn.getWidth(), sourceBtn.getHeight());

				// Start drag on glass pane
				Point originOnGlass = SwingUtilities.convertPoint(
						sourceBtn, sourceBtn.getWidth() / 2, sourceBtn.getHeight() / 2,
						dragGlassPane);
				dragGlassPane.startDrag(draggedPiece, dragSource, originOnGlass, size);
			}
		}

		if (isDragging) {
			// Convert to glass pane coords
			Point glassPt = SwingUtilities.convertPoint(
					null, screenPoint.x, screenPoint.y, dragGlassPane);
			// Adjust for glass pane location on screen
			Point glassPaneLocation = dragGlassPane.getLocationOnScreen();
			glassPt = new Point(screenPoint.x - glassPaneLocation.x,
					screenPoint.y - glassPaneLocation.y);
			dragGlassPane.updateDrag(glassPt);
		}
	}

	/**
	 * Handle mouse release - try to complete move or snap back
	 */
	public void handleMouseReleased(MouseEvent e) {
		if (dragGlassPane == null || dragSource == null)
			return;

		if (isDragging) {
			// Find drop target
			Point screenPt = e.getLocationOnScreen();
			Position dropPos = getPositionAtScreen(screenPt);

			boolean moveSucceeded = false;
			if (dropPos != null && !dropPos.equals(dragSource)) {
				// Try to make the move
				moveSucceeded = controller.tryDragMove(dragSource, dropPos);
			}

			if (!moveSucceeded) {
				// Restore piece to source
				SquareButton sourceBtn = getSquareButton(dragSource);
				sourceBtn.setPiece(draggedPiece);
			}

			// End drag with animation if failed
			dragGlassPane.endDrag(moveSucceeded);
		}

		// Reset drag state
		dragSource = null;
		draggedPiece = null;
		pressPoint = null;
		isDragging = false;
	}

	/**
	 * Get board position at screen coordinates
	 */
	private Position getPositionAtScreen(Point screenPoint) {
		for (int r = 0; r < BOARD_SIZE; r++) {
			for (int c = 0; c < BOARD_SIZE; c++) {
				SquareButton btn = squares[r][c];
				Point btnLoc = btn.getLocationOnScreen();
				java.awt.Rectangle bounds = new java.awt.Rectangle(
						btnLoc.x, btnLoc.y, btn.getWidth(), btn.getHeight());
				if (bounds.contains(screenPoint)) {
					return btn.getPosition();
				}
			}
		}
		return null;
	}
}
