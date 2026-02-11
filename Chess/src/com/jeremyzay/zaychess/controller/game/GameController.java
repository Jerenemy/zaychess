package com.jeremyzay.zaychess.controller.game;

import com.jeremyzay.zaychess.model.game.*;
import com.jeremyzay.zaychess.model.move.*;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveGenerator;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.move.PromotionPiece;
import com.jeremyzay.zaychess.model.pieces.Piece;
import com.jeremyzay.zaychess.model.rules.GameOverType;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.services.infrastructure.engine.EngineService;
import com.jeremyzay.zaychess.services.infrastructure.engine.SerendipityEngineService;
import com.jeremyzay.zaychess.view.gui.*;
import com.jeremyzay.zaychess.services.infrastructure.network.MoveCodec;
import com.jeremyzay.zaychess.services.infrastructure.network.MoveMessage;
import com.jeremyzay.zaychess.services.infrastructure.network.NetworkTransport;
import com.jeremyzay.zaychess.services.application.history.MoveHistoryInMemory;
import com.jeremyzay.zaychess.services.application.history.MoveHistoryService;
import com.jeremyzay.zaychess.services.application.notation.NotationFEN;
import com.jeremyzay.zaychess.services.application.notation.NotationSAN;
import com.jeremyzay.zaychess.view.gui.BoardPanel;
import com.jeremyzay.zaychess.view.gui.PromotionDialog;
import com.jeremyzay.zaychess.view.gui.MainFrame;
import com.jeremyzay.zaychess.view.gui.ChessPanel;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Coordinates UI input, game rules, history/undo, and (optionally) network
 * play.
 * 
 * Responsibilities:
 * Translates board clicks into validated moves.
 * Applies moves to {@link GameState}, updates the Swing UI, and records
 * history.
 * Encodes/decodes moves to/from a {@link NetworkTransport} when online.
 * Manages post-move UI states (check, game over) and move list display.
 * Threading:
 * All UI updates occur on the Swing EDT. Incoming network messages are
 * marshaled onto the EDT via {@link SwingUtilities#invokeLater(Runnable)}.
 * Invariants:
 * When online, local moves are only accepted on the local player's turn.
 * {@code selectedPosition} is cleared after any attempted move.
 */
public class GameController implements NetworkTransport.Listener {

	private final GameState gameState;
	private BoardPanel boardPanel;
	public List<String> moveLog = new ArrayList<>();
	private final List<Piece> captureLog = new ArrayList<>(); // parallel capture history for undo
	private final List<String> wireLog = new ArrayList<>();
	private final MoveHistoryService history;
	private NetworkTransport transport; // optional network dependency
	private volatile boolean networkReady = true; // offline defaults to true
	private PlayerColor localSide = null;
	private Position selectedPosition = null;
	private boolean suppressDialogs = false;

	public void setSuppressDialogs(boolean suppress) {
		this.suppressDialogs = suppress;
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// Constructors
	// ──────────────────────────────────────────────────────────────────────────────

	private EngineService engine = null;

	/**
	 * Create a controller with in-memory move history and no network.
	 *
	 * @param gameState the mutable game state this controller operates on
	 */
	public GameController(GameState gameState) throws Exception {
		this(gameState, new MoveHistoryInMemory());
	}

	/**
	 * Create a controller with a supplied history service (for custom undo/redo
	 * storage).
	 *
	 * @param gameState the mutable game state this controller operates on
	 * @param history   the history service used for recording, undoing, and redoing
	 *                  moves
	 */
	public GameController(GameState gameState, MoveHistoryService history) throws Exception {
		this.gameState = gameState;
		this.history = Objects.requireNonNullElseGet(history, MoveHistoryInMemory::new);
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// Trivial setters/getters
	// ──────────────────────────────────────────────────────────────────────────────
	public void setLocalSide(PlayerColor side) {
		this.localSide = side;
		if (boardPanel != null && side != null) {
			boardPanel.setOrientationAndInit(side, gameState.getBoard());
		}
	}

	public List<String> getWireLog() {
		return wireLog;
	}

	public void setBoardPanel(BoardPanel boardPanel) {
		this.boardPanel = boardPanel;
		// If we already know our side (e.g. online client JOINED as BLACK), apply
		// orientation now
		if (localSide != null) {
			boardPanel.setOrientationAndInit(localSide, gameState.getBoard());
		}
	}

	public GameState getGameState() {
		return gameState;
	}

	public BoardPanel getBoardPanel() {
		return boardPanel;
	}

	public NetworkTransport getHost() {
		return transport;
	}

	public boolean isUsingEngine() {
		return this.engine != null;
	}

	public MoveHistoryService getHistory() {
		return history;
	}

	public void stopEngine() {
		if (this.engine != null) {
			try {
				this.engine.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				this.engine = null;
			}
		}
	}

	public void setEngine() {
		this.engine = new SerendipityEngineService();
		try {
			this.engine.start();
		} catch (Throwable e) {
			if (e instanceof VirtualMachineError)
				throw (VirtualMachineError) e;
			this.engine = null;
			if (!suppressDialogs)
				JOptionPane.showMessageDialog(null,
						"Failed to start Serendipity engine.\n"
								+ rootCauseMessage(e)
								+ "\n\nMake sure Serendipity.jar is on the classpath, located at "
								+ "Chess/engines/Serendipity.jar, or pass -Dserendipity.jar=/path/to/Serendipity.jar, "
								+ "and run the app with --add-modules=jdk.incubator.vector.",
						"Engine Error",
						JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Syncs the engine's internal position with the current game state.
	 * Call this after loading a saved game to ensure the engine knows the current
	 * position.
	 * 
	 * @param fen the FEN string representing the current board position
	 */
	public void syncEnginePosition(String fen) {
		if (engine == null)
			return;
		try {
			engine.newGame();
			engine.setPositionFEN(fen);
		} catch (Exception e) {
			System.err.println("Failed to sync engine position: " + e.getMessage());
		}
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// NetworkTransport.Listener
	// ──────────────────────────────────────────────────────────────────────────────

	/**
	 * Handles an incoming network line, attempts to decode a move message,
	 * and applies it on the Swing EDT.
	 *
	 * @param line a single encoded line from the peer
	 * @implNote UI mutations are wrapped in {@code SwingUtilities.invokeLater}.
	 */
	@Override
	public void onMessage(String line) {
		networkReady = true;
		var msg = MoveCodec.tryDecode(line);
		if (msg == null)
			return;
		SwingUtilities.invokeLater(() -> applyNetworkMove(msg));
	}

	/**
	 * Called when the transport reports an error.
	 * Intended for logging and optional UI surfacing.
	 *
	 * @param e the error raised by the transport
	 */
	@Override
	public void onError(Exception e) {
		e.printStackTrace();
	}

	// Network application

	/**
	 * Applies a decoded network move after verifying it is legal in the current
	 * position.
	 * Does not broadcast back to avoid echo loops.
	 *
	 * @param mm a decoded network move message
	 */
	private void applyNetworkMove(MoveMessage mm) {
		Position from = new Position(mm.fromRank(), mm.fromFile());
		Position to = new Position(mm.toRank(), mm.toFile());

		// geometry legality check in current position
		Move legal = MoveGenerator.getValidMoveInTurn(gameState, from, to);
		if (legal == null)
			return;

		// decode type + promotion
		MoveType mt;
		PromotionPiece promo = null;
		String t = mm.type();
		if (t != null && t.startsWith("PROMOTION")) {
			mt = MoveType.PROMOTION;
			int idx = t.indexOf(':');
			if (idx > 0 && idx + 1 < t.length()) {
				promo = PromotionPiece.valueOf(t.substring(idx + 1));
			}
		} else {
			mt = MoveType.valueOf(t); // NORMAL, EN_PASSANT, CAPTURE, etc.
		}

		Move toApply = new Move(from, to, mt, promo);
		applyMoveAndNotify(toApply, /* broadcast */ false); // don't echo back
	}

	/**
	 * Runs post-move UI updates in a single call (status + game-over dialog).
	 * Intended to be called after a successful local or remote move.
	 */
	private void postMoveUiChecks() {
		updatePostMoveUi();
		maybeShowGameOverDialog();
	}

	/**
	 * Encodes and sends a move over the active transport (if any).
	 * For promotions, includes the chosen promotion piece.
	 *
	 * @param m the move to broadcast
	 */
	private void sendIfOnline(Move m) {
		if (transport == null)
			return;

		String typeStr = m.getMoveType().name();
		if (m.getMoveType() == MoveType.PROMOTION) {
			// include the chosen piece so the other side can apply correctly
			PromotionPiece pp = m.getPromotion();
			typeStr = (pp == null) ? "PROMOTION" : "PROMOTION:" + pp.name();
		}

		MoveMessage msg = new MoveMessage(
				m.getFromPos().getRank(), m.getFromPos().getFile(),
				m.getToPos().getRank(), m.getToPos().getFile(),
				typeStr);
		transport.send(MoveCodec.encode(msg));
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// Network wiring
	// ──────────────────────────────────────────────────────────────────────────────

	/**
	 * Attaches a transport for online play and begins listening.
	 * Blocks local input until the connection is established.
	 *
	 * @param t the network transport to use for sending/receiving moves
	 */
	public void attachNetwork(NetworkTransport t) {
		this.transport = t;
		this.networkReady = false;

		t.setListener(this);
		t.start();
	}

	public boolean isOnline() {
		return transport != null;
	}

	public boolean isNetworkReady() {
		return !isOnline() || networkReady;
	}

	/**
	 * Detaches and closes the current network transport (if any) and
	 * returns to offline mode.
	 */
	public void detachNetwork() {
		try {
			if (transport != null)
				transport.close();
		} catch (Exception ignored) {
		} finally {
			transport = null;
			networkReady = true;
			localSide = null;
		}
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// UI entry point
	// ──────────────────────────────────────────────────────────────────────────────

	/**
	 * Validates that a click can be processed (UI ready, turn/order, game not over,
	 * etc.)
	 * and then routes it into the selection/move state machine.
	 *
	 * @param pos the clicked board position (rank/file); ignored if null or UI not
	 *            ready
	 */
	public void validateSquareClick(Position pos) {
		if (pos == null)
			return;

		// ignore clicks if UI not ready
		if (boardPanel == null)
			return;

		// Clear opponent's last move highlight on interaction
		clearLastMoveHighlight();

		// block until connected in online mode
		if (isOnline() && !networkReady) {
			ChessPanel.getStatusPanel().setStatus("Waiting for opponent to connect…", Color.GRAY);
			return;
		}

		// ignore after game over
		if (gameState.isGameOver()) {
			ChessPanel.getStatusPanel().setStatus("Game is over", Color.GRAY);
			return;
		}

		// online: only allow clicks on your turn
		if (isOnline() && localSide != null && gameState.getTurn() != localSide) {
			ChessPanel.getStatusPanel().setStatus("Not your turn", Color.GRAY);
			return;
		}

		// AI: block input while the engine is to move
		if (!isOnline() && engine != null && localSide != null && gameState.getTurn() != localSide) {
			ChessPanel.getStatusPanel().setStatus("Engine thinking...", Color.GRAY);
			return;
		}
		selectOrMovePiece(pos);
	}

	/**
	 * State machine: select a piece, reselect, or attempt to move from the
	 * current selection to the clicked square.
	 *
	 * @param clicked the clicked board position
	 */
	public void selectOrMovePiece(Position clicked) {
		if (selectedPosition == null) {
			selectOrIgnore(clicked);
			return;
		}
		// Clicked the same piece? Deselect.
		if (selectedPosition.equals(clicked)) {
			clearHighlights();
			selectedPosition = null;
			return;
		}
		if (isReselectingOwnPiece(clicked)) {
			select(clicked);
			return;
		}
		// If clicking enemy piece, we might be attacking it (attemptMove),
		// but we shouldn't just "select" it to see its moves.
		// However, attemptMove checks legality.
		// If attemptMove fails (illegal move), we show illegal move.
		// But wait, isSelectingOwnPieceAfterSelectingEnemy handles the specific
		// case where we selected an enemy piece (bug) and then click our own.
		// Since we are preventing enemy selection now, that method might be redundant
		// or at least less likely to trigger.

		attemptMove(selectedPosition, clicked); // <- main branch
		// selectedPosition = null; // attemptMove or showIllegalMove handles clearing
		// if needed?
		// actually attemptMove calls applyMoveAndNotify which calls clearHighlights.
		// showIllegalMove calls clearHighlights.
		// So we can just null it here to be safe or rely on those.
		// But let's look at attemptMove.

		// If move was successful, selectedPosition should be cleared.
		// If illegal, showIllegalMove clears it.
		// So we don't strictly need to null it here, but let's leave existing logic
		// which was: attemptMove(...) then selectedPosition = null.
		// However, if we want to KEEP selection after a failed move (optional UX),
		// we shouldn't clear it. But standard behavior is usually deselect or keep.
		// The user didn't ask to change that, so I'll stick to:

		selectedPosition = null;
	}

	// ... (unchanged methods)

	private void selectOrIgnore(Position p) {
		Piece piece = gameState.getPieceAt(p);
		if (piece == null)
			return;

		// Prevent selecting opponent's pieces
		if (piece.getColor() != gameState.getTurn())
			return;

		select(p);
	}

	/**
	 * @return {@code true} if the user clicked another of their own pieces while
	 *         one is already selected
	 */
	private boolean isReselectingOwnPiece(Position clicked) {
		Piece from = gameState.getPieceAt(selectedPosition);
		Piece to = gameState.getPieceAt(clicked);
		return from != null && to != null && from.getColor() == to.getColor();
	}

	/**
	 * @return {@code true} if the current selection is an enemy piece and the click
	 *         is on a friendly piece;
	 *         in this case, we switch the selection to the friendly piece.
	 */
	private boolean isSelectingOwnPieceAfterSelectingEnemy(Position clicked) {
		Piece from = gameState.getPieceAt(selectedPosition);
		Piece to = gameState.getPieceAt(clicked);
		PlayerColor us = gameState.getTurn();
		PlayerColor them = us.getOpposite();
		return from != null && to != null && from.getColor() == them && to.getColor() == us;
	}

	/**
	 * Selects {@code p}, updates UI highlight and status.
	 *
	 * @param p the square to select
	 */
	private void select(Position p) {
		// Clear previous highlights first
		clearHighlights();

		// UI highlight
		highlightLegalMoves(p);

		Piece piece = gameState.getPieceAt(p);
		if (gameState.isInCheck())
			ChessPanel.getStatusPanel().setStatus("Turn: " + gameState.getTurn(), Color.MAGENTA);
		else
			ChessPanel.getStatusPanel().setStatus("Turn: " + gameState.getTurn());
		// ChessPanel.getStatusPanel().setStatus(
		// "Selected: " + piece.getColor() + " " + piece.getClass().getSimpleName()
		// + "[" + p.getRank() + ":" + p.getFile() + "]"
		// );
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// Move attempts & application
	// ──────────────────────────────────────────────────────────────────────────────

	/**
	 * Validates a potential move from {@code from} to {@code to}. If legal,
	 * handles promotion choice (when needed) and then applies/broadcasts the move.
	 *
	 * @param from origin square
	 * @param to   destination square
	 */
	private void attemptMove(Position from, Position to) {
		Move m = MoveGenerator.getValidMoveInTurn(gameState, from, to);
		if (m == null) {
			showIllegalMove();
			return;
		}
		if (m.getMoveType() == MoveType.PROMOTION) {
			final Move promotionMove = m;
			PromotionDialog.prompt(boardPanel, gameState.getTurn(), choice -> {
				if (choice == null)
					return; // user cancelled
				Move promoted = promotionMove.withPromotion(choice);
				applyMoveAndNotify(promoted, /* broadcast */ true);
				maybeEngineRespond();
			});
			return;
		}
		applyMoveAndNotify(m, /* broadcast */ true);
		maybeEngineRespond();

		// applyMoveAndNotify(m, /*broadcast*/ true);
	}

	/**
	 * Attempts a drag-and-drop move from {@code from} to {@code to}.
	 * Returns true if the move was valid and applied, false otherwise.
	 * Does not show illegal move message - BoardPanel handles the snap-back
	 * animation.
	 *
	 * @param from origin square
	 * @param to   destination square
	 * @return true if move was valid and applied
	 */
	public boolean tryDragMove(Position from, Position to) {
		// Same validation as validateSquareClick
		if (from == null || to == null)
			return false;
		if (boardPanel == null)
			return false;
		if (isOnline() && !networkReady)
			return false;
		if (gameState.isGameOver())
			return false;
		if (isOnline() && localSide != null && gameState.getTurn() != localSide)
			return false;
		if (!isOnline() && engine != null && localSide != null && gameState.getTurn() != localSide)
			return false;

		Move m = MoveGenerator.getValidMoveInTurn(gameState, from, to);
		if (m == null) {
			return false;
		}

		if (m.getMoveType() == MoveType.PROMOTION) {
			final Move promotionMove = m;
			PromotionDialog.prompt(boardPanel, gameState.getTurn(), choice -> {
				if (choice == null)
					return;
				Move promoted = promotionMove.withPromotion(choice);
				applyMoveAndNotify(promoted, /* broadcast */ true);
				maybeEngineRespond();
			});
			return true; // move is pending promotion selection
		}

		applyMoveAndNotify(m, /* broadcast */ true);
		maybeEngineRespond();
		return true;
	}

	/**
	 * Notifies the user of an illegal move attempt, clears any highlight/selection.
	 */
	private void showIllegalMove() {
		ChessPanel.getStatusPanel().setStatus("Illegal move", Color.RED);
		// clear UI highlight + selection
		clearHighlights();
		selectedPosition = null; // drop selection after an illegal try
	}

	/**
	 * Records history and logs, applies the move to {@link GameState},
	 * updates the board UI and status, and optionally broadcasts it.
	 *
	 * @param m         the move to apply
	 * @param broadcast whether to send the move to the peer (if online)
	 */
	private void applyMoveAndNotify(Move m, boolean broadcast) {
		GameState snap = gameState.snapshot();
		String san = NotationSAN.toSAN(gameState, m);

		// record history & logs
		history.record(gameState, m, snap);
		wireLog.add(encodeWire(m));
		String line = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + san;
		dispatchMoveInfo(line);

		// detect capture before applying
		Piece capturedPiece = null;
		if (m.getMoveType() == MoveType.EN_PASSANT) {
			// en passant: captured pawn is adjacent, not on destination
			int epRank = m.getToPos().getRank();
			int epFile = m.getToPos().getFile();
			PlayerColor moverColor = gameState.getPieceColorAt(m.getFromPos());
			int capturedRank = (moverColor == PlayerColor.WHITE) ? epRank + 1 : epRank - 1;
			capturedPiece = gameState.getPieceAt(capturedRank, epFile);
		} else {
			capturedPiece = gameState.getPieceAt(m.getToPos());
		}

		// apply & UI
		gameState.applyMove(m);

		// update captured pieces panel
		captureLog.add(capturedPiece); // null if no capture — keeps in sync with move history
		if (capturedPiece != null) {
			ChessPanel.getCapturedPiecesPanel().addCapturedPiece(capturedPiece);
		}
		if (boardPanel != null) {
			boolean flipped = false;
			// Local hotseat: flip if the turn perspective changed
			if (!isOnline() && !isUsingEngine()) {
				if (boardPanel.getOrientation() != gameState.getTurn()) {
					boardPanel.setOrientationAndInit(gameState.getTurn(), gameState.getBoard());
					flipped = true;
				}
			}
			// If we didn't do a full re-init (flip), we must update the pieces on existing
			// buttons
			if (!flipped) {
				boardPanel.updateBoard(gameState.getBoard());
			}

			// clear selection highlight after a successful move
			clearHighlights();
		}
		postMoveUiChecks();

		if (broadcast)
			sendIfOnline(m);
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// Post-move UI
	// ──────────────────────────────────────────────────────────────────────────────

	// --- Highlight Helpers ---

	private void highlightLegalMoves(Position from) {
		selectedPosition = from;
		if (boardPanel != null) {
			boardPanel.setHighlight(from, com.jeremyzay.zaychess.view.gui.theme.HighlightType.SELECTION, true);

			// also highlight legal destinations
			java.util.List<Move> legalMoves = MoveGenerator.generateLegalMoves(gameState, from);
			for (Move m : legalMoves) {
				boardPanel.setHighlight(m.getToPos(), com.jeremyzay.zaychess.view.gui.theme.HighlightType.SELECTION,
						true);
			}
		}
	}

	public Position getSelectedPosition() {
		return selectedPosition;
	}

	public void clearSelection() {
		clearHighlights();
	}

	private void clearHighlights() {
		if (boardPanel != null) {
			boardPanel.clearHighlights(com.jeremyzay.zaychess.view.gui.theme.HighlightType.SELECTION);
		}
		selectedPosition = null; // Ensure logic state matches UI state
	}

	// --- Game End Handling ---

	// --- Game End Handling ---

	private void handleGameEnd() {
		if (!gameState.isGameOver())
			return;

		com.jeremyzay.zaychess.model.rules.GameOverType type = gameState.getGameOverType();
		String msg = "";
		PlayerColor winner = null;

		// Highlight Checkmate
		if (type == com.jeremyzay.zaychess.model.rules.GameOverType.CHECKMATE) {
			// Winner is the one who made the last move (current turn is the loser)
			PlayerColor loser = gameState.getTurn();
			Position kingPos = gameState.getBoard().findKing(loser);
			if (kingPos != null && boardPanel != null) {
				boardPanel.setHighlight(kingPos, com.jeremyzay.zaychess.view.gui.theme.HighlightType.CHECKMATE, true);
			}
			winner = loser.getOpposite();
			msg = "Checkmate! " + (winner == PlayerColor.WHITE ? "White" : "Black") + " wins.";
		} else if (type == com.jeremyzay.zaychess.model.rules.GameOverType.DRAW) {
			msg = "Draw.";
		} else if (type == com.jeremyzay.zaychess.model.rules.GameOverType.RESIGN) {
			msg = "Resignation.";
			// In resignation, the current turn player didn't necessarily lose,
			// but we'll assume the resigner lost.
			// This logic depends on who triggered resignation.
			// For now, no winner icon for resignation unless we track who resigned.
		} else {
			msg = "Game Over.";
		}

		ChessPanel.getStatusPanel().setStatus(msg, java.awt.Color.GREEN);

		if (!suppressDialogs) {
			new com.jeremyzay.zaychess.view.gui.GameOverDialog(
					msg,
					winner,
					this::restartGame,
					this::returnToMenu).showOverlay();
		}
	}

	private void restartGame() {
		// Reset state
		gameState.restoreFrom(new GameState());
		selectedPosition = null;

		// Clear history/logs
		history.clear();
		wireLog.clear();
		com.jeremyzay.zaychess.view.gui.ChessPanel.getMoveListPanel().clearMoves();
		com.jeremyzay.zaychess.view.gui.ChessPanel.getCapturedPiecesPanel().clear();
		captureLog.clear();

		// Update Board UI
		if (boardPanel != null) {
			boardPanel.clearHighlights(com.jeremyzay.zaychess.view.gui.theme.HighlightType.SELECTION);
			boardPanel.clearHighlights(com.jeremyzay.zaychess.view.gui.theme.HighlightType.CHECKMATE);
			boardPanel.clearHighlights(com.jeremyzay.zaychess.view.gui.theme.HighlightType.LAST_MOVE);

			// If playing vs Engine or Hotseat, we might need to reset orientation?
			// Usually hotseat keeps last orientation, vs Engine keeps player side.
			// Getting boardPanel orientation matches current view.

			boardPanel.updateBoard(gameState.getBoard());
		}

		ChessPanel.getStatusPanel().setStatus("Turn: " + gameState.getTurn());

		// Restart Engine if active
		if (isUsingEngine()) {
			try {
				engine.newGame();
			} catch (Exception e) {
				e.printStackTrace();
			}
			maybeEngineRespond();
		}
	}

	private void returnToMenu() {
		System.out.println("[DEBUG] returnToMenu() called on thread: " + Thread.currentThread().getName());
		java.awt.Window win = SwingUtilities.getWindowAncestor(boardPanel);
		if (win != null) {
			if (win instanceof MainFrame) {
				System.out.println("[DEBUG] Ancestor is MainFrame, NOT disposing. Just switching view.");
			} else {
				System.out.println("[DEBUG] Ancestor is " + win.getClass().getName() + ", disposing.");
				win.dispose();
			}
		} else {
			System.out.println("[DEBUG] No ancestor window found for boardPanel.");
		}

		// Ensure engine is stopped when leaving game view?
		// Run in background to avoid freezing EDT
		new Thread(() -> {
			System.out.println("[DEBUG] Stopping engine in background...");
			stopEngine();
			System.out.println("[DEBUG] Engine stopped.");
		}).start();

		SwingUtilities.invokeLater(() -> {
			System.out.println("[DEBUG] Switching to Main Menu view...");
			MainFrame.getInstance().showMenu();
		});
	}

	/**
	 * Updates the status panel based on check state; otherwise shows ready status..
	 * Also highlights the King if in check.
	 */
	private void updatePostMoveUi() {
		// Update captured pieces score
		ChessPanel.getCapturedPiecesPanel().updateScore(gameState.getBoard());

		if (gameState.isInCheck()) {
			ChessPanel.getStatusPanel().setStatus(gameState.getTurn() + " is in check!", java.awt.Color.MAGENTA);
			// Highlight King in check
			com.jeremyzay.zaychess.model.util.Position kingPos = gameState.getBoard().findKing(gameState.getTurn());
			if (kingPos != null && boardPanel != null) {
				boardPanel.setHighlight(kingPos, com.jeremyzay.zaychess.view.gui.theme.HighlightType.CHECK, true);
			}
		} else {
			ChessPanel.getStatusPanel().setReady(gameState);
			// Clear check highlights
			if (boardPanel != null) {
				boardPanel.clearHighlights(com.jeremyzay.zaychess.view.gui.theme.HighlightType.CHECK);
			}
		}
		updateLastMoveHighlight();
	}

	private void updateLastMoveHighlight() {
		if (boardPanel == null)
			return;
		boardPanel.clearHighlights(com.jeremyzay.zaychess.view.gui.theme.HighlightType.LAST_MOVE);
		Move last = history.peekLastMove();
		if (last != null) {
			boardPanel.setHighlight(last.getFromPos(), com.jeremyzay.zaychess.view.gui.theme.HighlightType.LAST_MOVE,
					true);
			boardPanel.setHighlight(last.getToPos(), com.jeremyzay.zaychess.view.gui.theme.HighlightType.LAST_MOVE,
					true);
		}
	}

	/**
	 * Public method to clear last move highlights on user interaction
	 */
	public void clearLastMoveHighlight() {
		if (boardPanel != null) {
			boardPanel.clearHighlights(com.jeremyzay.zaychess.view.gui.theme.HighlightType.LAST_MOVE);
		}
	}

	/**
	 * If the game is over, shows a modal dialog with the final result.
	 */
	private void maybeShowGameOverDialog() {
		if (!gameState.isGameOver())
			return;
		handleGameEnd();
	}
	// ------------------------------------------------------------------------------
	// Move List UI
	// ------------------------------------------------------------------------------

	/**
	 * Appends a
	 * formatted move
	 * line to
	 * the move
	 * list UI
	 * and stores
	 * it in*
	 * {@code moveLog}.**
	 * 
	 * @param info a human-readable move
	 * 
	 *             string (e.g., "12:01:03 e4")
	 */

	public void dispatchMoveInfo(String info) {
		ChessPanel.getMoveListPanel().appendMove(info);
		moveLog.add(info);
	}

	/**
	 * Undoes the last move if available, restores the board/UI accordingly,
	 * and removes the last line from the move list.
	 * 
	 * In AI mode, undoes both the AI's move and the human's move so the player
	 * can redo their turn. Then syncs the engine with the new position.
	 */
	public void undo() {
		if (!history.canUndo())
			return;

		// Invalidate any pending AI move computation
		engineMoveVersion++;

		// In AI games, undo twice (AI's response + human's move)
		if (isUsingEngine() && history.canUndo()) {
			// Check if we need to undo AI's move first (it's currently human's turn)
			if (gameState.getTurn() == localSide) {
				// AI just moved, need to undo AI's move first
				undoSingleMove();
			}
			// Now undo human's move
			if (history.canUndo()) {
				undoSingleMove();
			}
			// Sync engine with new position
			syncEngineAfterUndo();
		} else {
			// Standard undo for non-AI games
			undoSingleMove();
		}
	}

	/** Helper: undoes a single move and updates UI */
	private void undoSingleMove() {
		history.undo(gameState); // restores snapshot
		if (boardPanel != null)
			boardPanel.updateBoard(gameState.getBoard());
		updatePostMoveUi();

		// remove last line from the move list
		if (!moveLog.isEmpty()) {
			String last = moveLog.remove(moveLog.size() - 1);
			ChessPanel.getMoveListPanel().removeMove(moveLog, last);
		}
		// remove last capture
		if (!captureLog.isEmpty()) {
			Piece undone = captureLog.remove(captureLog.size() - 1);
			if (undone != null) {
				ChessPanel.getCapturedPiecesPanel().undoCapture(undone);
			}
		}
	}

	/** Syncs the engine position after undo */
	private void syncEngineAfterUndo() {
		if (engine == null)
			return;
		String fen = com.jeremyzay.zaychess.services.application.notation.FenGenerator.toFen(gameState);
		syncEnginePosition(fen);
	}

	/**
	 * Redoes the next move if available, computes SAN based on the current state,
	 * reapplies the move, updates UI, and appends to the move list.
	 * 
	 * In AI mode, redoes both the human's move and the AI's response,
	 * then syncs the engine with the new position.
	 */
	public void redo() {
		if (!history.canRedo())
			return;

		// In AI games, redo twice (human's move + AI's response)
		if (isUsingEngine()) {
			// First redo: human's move
			redoSingleMove();
			// Second redo: AI's move (if available)
			if (history.canRedo()) {
				redoSingleMove();
			}
			// Sync engine with new position
			syncEngineAfterUndo();
			// If it's now AI's turn and no AI move was redone, trigger AI
			if (gameState.getTurn() != localSide) {
				maybeEngineRespond();
			}
		} else {
			// Standard redo for non-AI games
			redoSingleMove();
		}
	}

	/** Helper: redoes a single move and updates UI */
	private void redoSingleMove() {
		Move redoMove = history.peekRedoMove();
		if (redoMove == null)
			return;

		// Detect capture before redoing (same logic as applyMoveAndNotify)
		Piece capturedPiece = null;
		if (redoMove.getMoveType() == com.jeremyzay.zaychess.model.move.MoveType.EN_PASSANT) {
			int epRank = redoMove.getToPos().getRank();
			int epFile = redoMove.getToPos().getFile();
			com.jeremyzay.zaychess.model.util.PlayerColor moverColor = gameState.getPieceColorAt(redoMove.getFromPos());
			int capturedRank = (moverColor == com.jeremyzay.zaychess.model.util.PlayerColor.WHITE) ? epRank + 1
					: epRank - 1;
			capturedPiece = gameState.getPieceAt(capturedRank, epFile);
		} else {
			capturedPiece = gameState.getPieceAt(redoMove.getToPos());
		}

		String san = NotationSAN.toSAN(gameState, redoMove);

		history.redo(gameState); // reapplies the move

		// Update capture log and UI
		captureLog.add(capturedPiece);
		if (capturedPiece != null) {
			ChessPanel.getCapturedPiecesPanel().addCapturedPiece(capturedPiece);
		}

		if (boardPanel != null)
			boardPanel.updateBoard(gameState.getBoard());
		updatePostMoveUi();

		String line = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + san;
		dispatchMoveInfo(line);
	}

	/**
	 * Encodes a move into a single-line wire format for transport.
	 * For promotions, includes the promotion piece name after a colon.
	 *
	 * @param m the move to encode
	 * @return an encoded line such as {@code "MOVE|6,4|4,4|NORMAL"} or
	 *         {@code "MOVE|6,4|7,4|PROMOTION:QUEEN"}
	 */
	private String encodeWire(Move m) {
		String type = m.getMoveType().name();
		if (m.getMoveType() == MoveType.PROMOTION) {
			type = "PROMOTION" + (m.getPromotion() != null ? ":" + m.getPromotion().name() : "");
		}
		MoveMessage msg = new MoveMessage(
				m.getFromPos().getRank(), m.getFromPos().getFile(),
				m.getToPos().getRank(), m.getToPos().getFile(),
				type);
		return MoveCodec.encode(msg);
	}

	// ──────────────────────────────────────────────────────────────────────────────
	// AI
	// ──────────────────────────────────────────────────────────────────────────────

	// --- fields
	private volatile boolean engineThinking = false;
	private volatile int engineMoveVersion = 0; // incremented on undo to invalidate pending AI moves

	// Call from MainMenuFrame after launching board
	public void startEngineGame(PlayerColor you) {
		setLocalSide(you);
		try {
			if (engine != null)
				engine.newGame();
		} catch (Exception ignored) {
		}
		if (engine != null && gameState.getTurn() != you)
			engineMoveAsync();
	}

	// If it’s the engine’s turn, let it move (off the EDT)
	private void maybeEngineRespond() {
		if (engine == null)
			return;
		if (localSide == null)
			return; // not an AI game
		if (isOnline())
			return; // AI not for network games
		if (gameState.isGameOver())
			return; // game already ended
		if (gameState.getTurn() != localSide)
			engineMoveAsync();
	}

	private void engineMoveAsync() {
		if (engine == null || engineThinking)
			return;
		if (gameState.isGameOver())
			return; // don't ask engine for moves after game over
		engineThinking = true;
		final int versionAtStart = engineMoveVersion;
		new Thread(() -> {
			try {
				GameState snap = gameState.snapshot();
				engine.setPositionFEN(NotationFEN.toFEN(snap));
				String uci = engine.bestMoveMs(1500); // ~1.5s thinking
				Move em = decodeUci(uci);
				if (em != null) {
					SwingUtilities.invokeLater(() -> {
						// Only apply if no undo occurred during thinking
						if (versionAtStart == engineMoveVersion) {
							applyMoveAndNotify(em, false);
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				engineThinking = false;
				SwingUtilities.invokeLater(() -> {
					// Only continue if no undo occurred
					if (versionAtStart == engineMoveVersion) {
						maybeEngineRespond();
					}
				});
			}
		}, "engine-move").start();
	}

	// --- convert between your Position and UCI
	private static final boolean RANK0_IS_BOTTOM = false; // set false if (0,0)==a8

	private String sq(Position p) {
		int f = p.getFile(), r = p.getRank();
		char file = (char) ('a' + f);
		int rank = RANK0_IS_BOTTOM ? (r + 1) : (8 - r);
		return "" + file + rank;
	}

	private Position parseSquare(String s) {
		if (s == null || s.length() < 2)
			return null;
		int file = s.charAt(0) - 'a';
		int rank = s.charAt(1) - '1';
		if (!RANK0_IS_BOTTOM)
			rank = 7 - rank;
		if (file < 0 || file > 7 || rank < 0 || rank > 7)
			return null;
		return new Position(rank, file);
	}

	private Move decodeUci(String uci) {
		if (uci == null || uci.length() < 4)
			return null;
		Position from = parseSquare(uci.substring(0, 2));
		Position to = parseSquare(uci.substring(2, 4));
		if (from == null || to == null)
			return null; // invalid UCI string (e.g. "(none)", "0000")
		PromotionPiece promo = null;
		if (uci.length() >= 5) {
			promo = switch (Character.toLowerCase(uci.charAt(4))) {
				case 'q' -> PromotionPiece.QUEEN;
				case 'r' -> PromotionPiece.ROOK;
				case 'b' -> PromotionPiece.BISHOP;
				case 'n' -> PromotionPiece.KNIGHT;
				default -> null;
			};
		}
		Move legal = MoveGenerator.getValidMoveInTurn(gameState, from, to);
		if (legal == null)
			return null;
		if (promo != null && legal.getMoveType() == MoveType.PROMOTION) {
			legal = legal.withPromotion(promo);
		}
		return legal;
	}

	// Engine loader helpers
	// Pick the Java you’re currently running, cross-platform
	// static String javaCmd() {
	// boolean win = System.getProperty("os.name").toLowerCase().contains("win");
	// return java.nio.file.Paths.get(System.getProperty("java.home"), "bin", win ?
	// "java.exe" : "java").toString();
	// }
	private static String rootCauseMessage(Throwable t) {
		Throwable cur = t;
		while (cur.getCause() != null)
			cur = cur.getCause();
		String msg = cur.getMessage();
		return (msg == null || msg.isBlank()) ? cur.toString() : msg;
	}

}
