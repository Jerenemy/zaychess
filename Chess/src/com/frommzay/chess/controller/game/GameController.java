package com.frommzay.chess.controller.game;

import com.frommzay.chess.model.game.*;
import com.frommzay.chess.model.move.*;
import com.frommzay.chess.model.pieces.Piece;
import com.frommzay.chess.model.rules.GameOverType;
import com.frommzay.chess.model.util.PlayerColor;
import com.frommzay.chess.model.util.Position;
import com.frommzay.chess.services.infrastructure.engine.EngineService;
import com.frommzay.chess.services.infrastructure.engine.SerendipityEngineService;
import com.frommzay.chess.view.gui.*;
import com.frommzay.chess.services.infrastructure.network.MoveCodec;
import com.frommzay.chess.services.infrastructure.network.MoveMessage;
import com.frommzay.chess.services.infrastructure.network.NetworkTransport;
import com.frommzay.chess.services.application.history.MoveHistoryInMemory;
import com.frommzay.chess.services.application.history.MoveHistoryService;
import com.frommzay.chess.services.application.notation.NotationSAN;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * Coordinates UI input, game rules, history/undo, and (optionally) network play.
 * 
 * Responsibilities:
 *   Translates board clicks into validated moves.
 *   Applies moves to {@link GameState}, updates the Swing UI, and records history.
 *   Encodes/decodes moves to/from a {@link NetworkTransport} when online.
 *   Manages post-move UI states (check, game over) and move list display.
 * Threading:
 *   All UI updates occur on the Swing EDT. Incoming network messages are
 *   marshaled onto the EDT via {@link SwingUtilities#invokeLater(Runnable)}.
 * Invariants:
 *   When online, local moves are only accepted on the local player's turn.
 *   {@code selectedPosition} is cleared after any attempted move.
 */
public class GameController implements NetworkTransport.Listener {

	private final GameState gameState;
	private BoardPanel boardPanel;
	public List<String> moveLog = new ArrayList<>();
	private final List<String> wireLog = new ArrayList<>();
	private final MoveHistoryService history;
	private NetworkTransport transport; // optional network dependency
	private volatile boolean networkReady = true; // offline defaults to true
	private PlayerColor localSide = null; 
	private Position highlightedSquare = null;
	private Position selectedPosition = null;

	// ──────────────────────────────────────────────────────────────────────────────
    // Constructors
    // ──────────────────────────────────────────────────────────────────────────────
	
    private final EngineService engine;
	/**
     * Create a controller with in-memory move history and no network.
     *
     * @param gameState the mutable game state this controller operates on
     */
	public GameController(GameState gameState) throws Exception {
		this(gameState, new MoveHistoryInMemory());
	}
	/**
     * Create a controller with a supplied history service (for custom undo/redo storage).
     *
     * @param gameState the mutable game state this controller operates on
     * @param history   the history service used for recording, undoing, and redoing moves
     */
    public GameController(GameState gameState, MoveHistoryService history) throws Exception {
        this(gameState, history, false);
    }

    public GameController(GameState gameState,
                          MoveHistoryService history,
                          boolean useEngine) throws Exception {
        this.history = Objects.requireNonNullElseGet(history, MoveHistoryInMemory::new);
        this.gameState = gameState;
        if (useEngine) {
            this.engine = new SerendipityEngineService("java",
//                    "/Users/jeremyzay/Desktop/intellij/chess/ptp25-mi05-chess-jeremy/Chess/src/com/frommzay/chess/services/infrastructure/engine/Serendipity.jar");
                    "Chess/src/com/frommzay/chess/services/infrastructure/engine/Serendipity.jar");
            this.engine.start();
        }
        else this.engine = null;
    }



    // ──────────────────────────────────────────────────────────────────────────────
    // Trivial setters/getters 
    // ──────────────────────────────────────────────────────────────────────────────
	public void setLocalSide(PlayerColor side) { this.localSide = side; }
  
	public List<String> getWireLog() { return wireLog; }
  
	public void setBoardPanel(BoardPanel boardPanel) { this.boardPanel = boardPanel; }
  
	public GameState getGameState() { return gameState; }
	
	public BoardPanel getBoardPanel() { return boardPanel; }
	
	public NetworkTransport getHost() { return transport; }
  
	
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
  
	public boolean isOnline() { return transport != null; }
  
	public boolean isNetworkReady() { return !isOnline() || networkReady; }

	/**
     * Detaches and closes the current network transport (if any) and
     * returns to offline mode.
     */
	public void detachNetwork() {
	    try { if (transport != null) transport.close(); }
	    catch (Exception ignored) {}
	    finally {
	        transport = null;
	        networkReady = true;                  
	        localSide = null;
	    }
	}
  
  
	// ──────────────────────────────────────────────────────────────────────────────
    // UI entry point
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that a click can be processed (UI ready, turn/order, game not over, etc.)
     * and then routes it into the selection/move state machine.
     *
     * @param pos the clicked board position (rank/file); ignored if null or UI not ready
     */
	public void validateSquareClick(Position pos) {  
	   if (pos == null) return;
	
	   // ignore clicks if UI not ready
	   if (boardPanel == null) return;
	
	   // block until connected in online mode
	   if (isOnline() && !networkReady) {
          ChessFrame.getStatusPanel().setStatus("Waiting for opponent to connect…", Color.GRAY);
          return;
	   }
      
	   // ignore after game over
	   if (gameState.isGameOver()) {
	       ChessFrame.getStatusPanel().setStatus("Game is over", Color.GRAY);
	       return;
	   }
	
	   // online: only allow clicks on your turn
	   if (isOnline() && localSide != null && gameState.getTurn() != localSide) {
	       ChessFrame.getStatusPanel().setStatus("Not your turn", Color.GRAY);
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
	    if (isReselectingOwnPiece(clicked)) {
	        select(clicked);
	        return;
	    }
	    if (isSelectingOwnPieceAfterSelectingEnemy(clicked)) {
	    	select(clicked);
	        return;
	    }
	    attemptMove(selectedPosition, clicked);        // <- main branch
	    selectedPosition = null;                       // always reset after a try
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
  		if (msg == null) return;
  		SwingUtilities.invokeLater(() -> applyNetworkMove(msg));
  	}
  	
  	/**
     * Called when the transport reports an error.
     * Intended for logging and optional UI surfacing.
     *
     * @param e the error raised by the transport
     */
  	@Override 
  	public void onError(Exception e) { e.printStackTrace(); }	
  
  
  	// ──────────────────────────────────────────────────────────────────────────────
    // Network application
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Applies a decoded network move after verifying it is legal in the current position.
     * Does not broadcast back to avoid echo loops.
     *
     * @param mm a decoded network move message
     */  	
  	private void applyNetworkMove(MoveMessage mm) {
	    Position from = new Position(mm.fromRank(), mm.fromFile());
	    Position to   = new Position(mm.toRank(), mm.toFile());

	    // geometry legality check in current position
	    Move legal = MoveGenerator.getValidMoveInTurn(gameState, from, to);
	    if (legal == null) return;

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
	    applyMoveAndNotify(toApply, /*broadcast*/ false); // don't echo back
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
 	    if (transport == null) return;

 	    String typeStr = m.getMoveType().name(); 
 	    if (m.getMoveType() == MoveType.PROMOTION) {
 	        // include the chosen piece so the other side can apply correctly
 	        PromotionPiece pp = m.getPromotion();
 	        typeStr = (pp == null) ? "PROMOTION" : "PROMOTION:" + pp.name();
 	    }

 	    MoveMessage msg = new MoveMessage(
 	        m.getFromPos().getRank(), m.getFromPos().getFile(),
 	        m.getToPos().getRank(),   m.getToPos().getFile(),
 	        typeStr
 	    );
 	    transport.send(MoveCodec.encode(msg));
 	}

 	
 	
 	// ──────────────────────────────────────────────────────────────────────────────
    // Selection helpers
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * If a piece exists on {@code p}, selects it; otherwise no-ops.
     *
     * @param p the square to possibly select
     */ 	
 	private void selectOrIgnore(Position p) {
	    Piece piece = gameState.getPieceAt(p);
	    if (piece == null) return;
	    select(p);
	}

 	/**
     * @return {@code true} if the user clicked another of their own pieces while one is already selected
     */
	private boolean isReselectingOwnPiece(Position clicked) {
	    Piece from = gameState.getPieceAt(selectedPosition);
	    Piece to   = gameState.getPieceAt(clicked);
	    return from != null && to != null && from.getColor() == to.getColor();
	}

	/**
     * @return {@code true} if the current selection is an enemy piece and the click is on a friendly piece;
     * in this case, we switch the selection to the friendly piece.
     */
	private boolean isSelectingOwnPieceAfterSelectingEnemy(Position clicked) {
	    Piece from = gameState.getPieceAt(selectedPosition);
	    Piece to   = gameState.getPieceAt(clicked);
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
	    selectedPosition = p;

	    // UI highlight
	    if (boardPanel != null) {
	        if (highlightedSquare != null) {
	            boardPanel.getSquareButton(highlightedSquare).setHighlighted(false);
	        }
	        boardPanel.getSquareButton(p).setHighlighted(true);
	        highlightedSquare = p;
	    }

	    Piece piece = gameState.getPieceAt(p);
	    ChessFrame.getStatusPanel().setStatus(
	        "Selected: " + piece.getColor() + " " + piece.getClass().getSimpleName()
	        + "[" + p.getRank() + ":" + p.getFile() + "]"
	    );
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
	        PromotionPiece choice = PromotionDialog.prompt(boardPanel, gameState.getTurn()); // color of the mover
	        if (choice == null) return; // user cancelled
	        m = m.withPromotion(choice);
	    }
//        ai
        if (engine != null && !isOnline()) {
            try { engine.pushUserMove(toUci(m)); } catch (Exception ignored) {}
        }
        applyMoveAndNotify(m, /*broadcast*/ true);
        maybeEngineRespond();

//	    applyMoveAndNotify(m, /*broadcast*/ true);
	}
 	
 	/**
     * Notifies the user of an illegal move attempt, clears any highlight/selection.
     */
 	private void showIllegalMove() {
	    ChessFrame.getStatusPanel().setStatus("Illegal move", Color.RED);
	    // clear UI highlight + selection
	    if (boardPanel != null && highlightedSquare != null) {
	        boardPanel.getSquareButton(highlightedSquare).setHighlighted(false);
	        highlightedSquare = null;
	    }
	    selectedPosition = null;  // drop selection after an illegal try
	}
 	
 	/**
     * Records history and logs, applies the move to {@link GameState},
     * updates the board UI and status, and optionally broadcasts it.
     *
     * @param m          the move to apply
     * @param broadcast  whether to send the move to the peer (if online)
     */
 	private void applyMoveAndNotify(Move m, boolean broadcast) {
	    GameState snap = gameState.snapshot();
	    String san = NotationSAN.toSAN(gameState, m);

	    // record history & logs
	    history.record(gameState, m, snap);
	    wireLog.add(encodeWire(m));                   
	    String line = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + san;
	    dispatchMoveInfo(line);

	    // apply & UI
	    gameState.applyMove(m);
	    if (boardPanel != null) {
	    	boardPanel.updateBoard(gameState.getBoard());
	    	// clear selection highlight after a successful move
	        if (highlightedSquare != null) {
	            boardPanel.getSquareButton(highlightedSquare).setHighlighted(false);
	            highlightedSquare = null;
	        }
	    }
	    postMoveUiChecks();

	    if (broadcast) sendIfOnline(m);
	}
 	

 	// ──────────────────────────────────────────────────────────────────────────────
    // Post-move UI
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Updates the status panel based on check state; otherwise shows ready status..
     */
	private void updatePostMoveUi() {
	    if (gameState.isInCheck()) {
	        ChessFrame.getStatusPanel().setStatus(gameState.getTurn() + " is in check!", new Color(255,0,255));
	    } else {
	        ChessFrame.getStatusPanel().setReady(gameState);
	    }
	}

	/**
     * If the game is over, shows a modal dialog with the final result..
     */
	private void maybeShowGameOverDialog() {
	    if (!gameState.isGameOver()) return;

	    String title = "Spiel beendet";
	    GameOverType type = gameState.getGameOverType();
	    PlayerColor winner = gameState.getWinner();
	    String message = switch (type) {
	        case CHECKMATE -> "Schachmatt! " + (winner == PlayerColor.WHITE ? "Weiß" : "Schwarz") + " gewinnt.";
	        case DRAW      -> "Unentschieden – das Spiel endet im Remis.";
	        case RESIGN    -> (winner == PlayerColor.WHITE ? "Schwarz" : "Weiß") + " hat aufgegeben. " +
	                          (winner == PlayerColor.WHITE ? "Weiß" : "Schwarz") + " gewinnt.";
	        default        -> "Das Spiel ist beendet.";
	    };
	    JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
	}
	
	
	// ──────────────────────────────────────────────────────────────────────────────
    // Move list / history
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Appends a formatted move line to the move list UI and stores it in {@code moveLog}.
     *
     * @param info a human-readable move string (e.g., "12:01:03 e4")
     */
	public void dispatchMoveInfo(String info) {
		ChessFrame.getMoveListPanel().appendMove(info);
		moveLog.add(info);
	}

	/**
     * Undoes the last move if available, restores the board/UI accordingly,
     * and removes the last line from the move list.
     */
	public void undo() {
	    if (!history.canUndo()) return;

	    history.undo(gameState);               // restores snapshot
	    if (boardPanel != null) boardPanel.updateBoard(gameState.getBoard());
	    updatePostMoveUi();

	    // remove last line from the move list
	    if (!moveLog.isEmpty()) {
	        String last = moveLog.remove(moveLog.size() - 1);
	        ChessFrame.getMoveListPanel().removeMove(moveLog, last);
	    }
	}

	/**
     * Redoes the next move if available, computes SAN based on the current state,
     * reapplies the move, updates UI, and appends to the move list.
     */
	public void redo() {
	    if (!history.canRedo()) return;

	    Move redoMove = history.peekRedoMove();
	    if (redoMove == null) return;
	    String san = NotationSAN.toSAN(gameState, redoMove);

	    history.redo(gameState);               // reapplies the move
	    if (boardPanel != null) boardPanel.updateBoard(gameState.getBoard());
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
     * {@code "MOVE|6,4|7,4|PROMOTION:QUEEN"}
     */
	private String encodeWire(Move m) {
	    String type = m.getMoveType().name();
	    if (m.getMoveType() == MoveType.PROMOTION) {
	        type = "PROMOTION" + (m.getPromotion() != null ? ":" + m.getPromotion().name() : "");
	    }
	    MoveMessage msg = new MoveMessage(
	        m.getFromPos().getRank(), m.getFromPos().getFile(),
	        m.getToPos().getRank(),   m.getToPos().getFile(),
	        type
	    );
	    return MoveCodec.encode(msg); 
	}

    // ──────────────────────────────────────────────────────────────────────────────
    // AI
    // ──────────────────────────────────────────────────────────────────────────────

    // --- fields
    private volatile boolean engineThinking = false;

    // Call from MainMenuFrame after launching board
    public void startEngineGame(PlayerColor you) {
        setLocalSide(you);
        try { if (engine != null) engine.newGame(); } catch (Exception ignored) {}
        if (engine != null && gameState.getTurn() != you) engineMoveAsync();
    }

    // If it’s the engine’s turn, let it move (off the EDT)
    private void maybeEngineRespond() {
        if (engine == null) return;
        if (localSide == null) return;       // not an AI game
        if (isOnline()) return;              // AI not for network games
        if (gameState.getTurn() != localSide) engineMoveAsync();
    }

    private void engineMoveAsync() {
        if (engine == null || engineThinking) return;
        engineThinking = true;
        new Thread(() -> {
            try {
                String uci = engine.bestMoveMs(1500);        // ~1.5s thinking
                Move em = decodeUci(uci);
                if (em != null) {
                    // keep engine’s internal move history in sync
                    engine.pushUserMove(uci);
                    SwingUtilities.invokeLater(() -> applyMoveAndNotify(em, false));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                engineThinking = false;
                SwingUtilities.invokeLater(this::maybeEngineRespond);
            }
        }, "engine-move").start();
    }

    // --- convert between your Position/Move and UCI
    private static final boolean RANK0_IS_BOTTOM = false; // set false if (0,0)==a8

    private String toUci(Move m) {
        String s = sq(m.getFromPos()) + sq(m.getToPos());
        if (m.getMoveType() == MoveType.PROMOTION && m.getPromotion() != null) {
            s += switch (m.getPromotion()) {
                case QUEEN -> "q"; case ROOK -> "r"; case BISHOP -> "b"; case KNIGHT -> "n";
            };
        }
        return s;
    }
    private String sq(Position p) {
        int f = p.getFile(), r = p.getRank();
        char file = (char) ('a' + f);
        int rank = RANK0_IS_BOTTOM ? (r + 1) : (8 - r);
        return "" + file + rank;
    }
    private Position parseSquare(String s) {
        int file = s.charAt(0) - 'a';
        int rank = s.charAt(1) - '1';
        if (!RANK0_IS_BOTTOM) rank = 7 - rank;
        return new Position(rank, file);
    }
    private Move decodeUci(String uci) {
        if (uci == null || uci.length() < 4) return null;
        Position from = parseSquare(uci.substring(0,2));
        Position to   = parseSquare(uci.substring(2,4));
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
        if (legal == null) return null;
        if (promo != null && legal.getMoveType() == MoveType.PROMOTION) {
            legal = legal.withPromotion(promo);
        }
        return legal;
    }



}