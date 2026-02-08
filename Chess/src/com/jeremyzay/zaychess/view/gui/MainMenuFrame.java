package com.jeremyzay.zaychess.view.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.*;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.controller.game.GameLauncher;
import com.jeremyzay.zaychess.controller.saveload.SaveManager;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.util.PlayerColor;

/**
 * The entry point window for the chess application.
 * Provides main menu options:
 * - Start local game
 * - Load saved game
 * - Host or join multiplayer game
 */
public class MainMenuFrame extends JFrame {
	private static final long serialVersionUID = 4115239067261707835L;

	private final GameState gameState = new GameState();
	private final GameController controller;

	{
		try {
			controller = new GameController(gameState, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public MainMenuFrame() {
		setTitle("Zaychess – Launcher");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		// Menu panel with buttons
		MainMenuPanel menuPanel = new MainMenuPanel(this, 500);
		add(menuPanel, BorderLayout.CENTER);
		setContentPane(menuPanel);
		pack();
		// setSize(768, 432);
		setLocationRelativeTo(null);
		setResizable(false);
	}

	/** Start a fresh offline game. */
	public void startLocalGame() {
		GameLauncher.launch(gameState, controller);
		ChessFrame.getMoveListPanel().clearMoves();
		dispose();
	}

	/** Load a saved game from disk. Shows dialog to choose vs Human or vs AI. */
	public void loadLocalGame() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Load Game");
		chooser.setFileFilter(
				new javax.swing.filechooser.FileNameExtensionFilter("Chess save (*.chesslog)", "chesslog"));

		int result = chooser.showOpenDialog(this);
		if (result != JFileChooser.APPROVE_OPTION)
			return;

		java.io.File selectedFile = chooser.getSelectedFile();

		// Show mode selection dialog
		Object[] options = { "vs Human", "vs AI", "Cancel" };
		int choice = JOptionPane.showOptionDialog(this,
				"Continue this game against:",
				"Load Game Mode",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]);

		if (choice == 0) {
			// Load vs Human (local 2-player)
			loadGameHuman(selectedFile);
		} else if (choice == 1) {
			// Load vs AI - show side selection
			loadGameVsAI(selectedFile);
		}
		// else: Cancel - do nothing
	}

	/** Load game for local human vs human play. */
	private void loadGameHuman(java.io.File file) {
		try {
			GameLauncher.launch(gameState, controller);
			ChessFrame.getMoveListPanel().clearMoves();
			new SaveManager(controller).loadGame(file);
			dispose();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Error loading: " + ex.getMessage(),
					"Load Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Load game for play against the AI. Shows side selection dialog. */
	private void loadGameVsAI(java.io.File file) {
		// Start engine first
		controller.setEngine();
		if (!controller.isUsingEngine())
			return;

		// Show side selection dialog
		showAISideSelectionDialog(file);
	}

	/** Shows dialog to select which side to play and then loads game vs AI. */
	private void showAISideSelectionDialog(java.io.File file) {
		JDialog dialog = new JDialog(this, "Choose Your Side", true);
		dialog.setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel(new java.awt.GridLayout(1, 2, 20, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		com.jeremyzay.zaychess.model.pieces.King whiteKing = new com.jeremyzay.zaychess.model.pieces.King(
				PlayerColor.WHITE, null);
		com.jeremyzay.zaychess.model.pieces.King blackKing = new com.jeremyzay.zaychess.model.pieces.King(
				PlayerColor.BLACK, null);

		int iconSize = 80;

		JButton whiteBtn = new JButton("White");
		whiteBtn.setIcon(ResourceLoader.getPieceIcon(whiteKing, iconSize));
		whiteBtn.setHorizontalTextPosition(SwingConstants.CENTER);
		whiteBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
		whiteBtn.setFocusable(false);
		whiteBtn.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
		whiteBtn.addActionListener(e -> {
			dialog.dispose();
			finishLoadVsAI(file, PlayerColor.WHITE);
		});

		JButton blackBtn = new JButton("Black");
		blackBtn.setIcon(ResourceLoader.getPieceIcon(blackKing, iconSize));
		blackBtn.setHorizontalTextPosition(SwingConstants.CENTER);
		blackBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
		blackBtn.setFocusable(false);
		blackBtn.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
		blackBtn.addActionListener(e -> {
			dialog.dispose();
			finishLoadVsAI(file, PlayerColor.BLACK);
		});

		buttonPanel.add(whiteBtn);
		buttonPanel.add(blackBtn);
		dialog.add(buttonPanel, BorderLayout.CENTER);

		JButton cancelBtn = new JButton("❌");
		cancelBtn.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 24));
		cancelBtn.setFocusable(false);
		cancelBtn.addActionListener(e -> dialog.dispose());
		JPanel bottomPanel = new JPanel();
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		bottomPanel.add(cancelBtn);
		dialog.add(bottomPanel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	/** Finish loading game vs AI after side selection. */
	private void finishLoadVsAI(java.io.File file, PlayerColor humanSide) {
		try {
			GameLauncher.launch(gameState, controller);
			ChessFrame.getMoveListPanel().clearMoves();
			new SaveManager(controller).loadGame(file);

			// Sync engine with loaded position
			String fen = com.jeremyzay.zaychess.services.application.notation.FenGenerator.toFen(gameState);
			controller.syncEnginePosition(fen);

			// Start engine game with chosen side
			controller.startEngineGame(humanSide);

			dispose();
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Error loading: " + ex.getMessage(),
					"Load Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Start as host in multiplayer. Shows waiting dialog until a client connects.
	 */
	public void startHostGame() {
		JDialog waitingDialog = openWaitingDialog("Waiting for opponent...");
		// When waiting window is closed, clean up host
		waitingDialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (controller.getHost() != null) {
					try {
						controller.getHost().close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				SwingUtilities.invokeLater(() -> openMultiplayerMenu());
			}
		});

		SwingUtilities.invokeLater(() -> waitingDialog.setVisible(true));

		// Run host launch in a background thread
		new Thread(() -> {
			GameLauncher.launchAsHost(gameState, controller, waitingDialog, this);
			ChessFrame.getMoveListPanel().clearMoves();
		}).start();
	}

	/** Start as client in multiplayer. Connects to given host IP. */
	public void startClientGame(String ip) {
		JDialog loadingDialog = openLoadingDialog("Verbinde...");
		SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));

		new Thread(() -> {
			boolean success = GameLauncher.launchAsClient(gameState, controller, ip);
			loadingDialog.dispose();
			if (success) {
				dispose();
			} else {
				JOptionPane.showMessageDialog(this, "Unbekannter Host oder keine Verbindung möglich.",
						"Verbindungsfehler", JOptionPane.ERROR_MESSAGE);
				openMultiplayerMenu();
			}
		}).start();
	}

	/** Connect to the Relay Server and wait for a match. */
	public void startOnlineMatchmaking() {
		JDialog waitingDialog = openWaitingDialog("Waiting for opponent..."); // Reusing waiting dialog logic

		// Allow cancel
		waitingDialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// TODO: Disconnect relay client if it's connected
				SwingUtilities.invokeLater(() -> openMultiplayerMenu());
			}
		});

		SwingUtilities.invokeLater(() -> waitingDialog.setVisible(true));

		new Thread(() -> {
			GameLauncher.launchOnline(gameState, controller, waitingDialog, this);
		}).start();
	}

	// in MainMenuFrame
	public void startVsComputer() {
		controller.setEngine();
		if (!controller.isUsingEngine())
			return;

		// Custom dialog for side selection
		JDialog dialog = new JDialog(this, "Choose Side", true);
		dialog.setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel(new java.awt.GridLayout(1, 2, 20, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// Create dummy pieces to get icons. King requires position; null is safe for
		// fetching icons.
		com.jeremyzay.zaychess.model.pieces.King whiteKing = new com.jeremyzay.zaychess.model.pieces.King(
				PlayerColor.WHITE, null);
		com.jeremyzay.zaychess.model.pieces.King blackKing = new com.jeremyzay.zaychess.model.pieces.King(
				PlayerColor.BLACK, null);

		int iconSize = 80;

		// White Selection Button
		JButton whiteBtn = new JButton("White");
		whiteBtn.setIcon(ResourceLoader.getPieceIcon(whiteKing, iconSize));
		whiteBtn.setHorizontalTextPosition(SwingConstants.CENTER);
		whiteBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
		whiteBtn.setFocusable(false);
		whiteBtn.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
		whiteBtn.addActionListener(e -> {
			dialog.dispose();
			launchEngineGame(PlayerColor.WHITE);
		});

		// Black Selection Button
		JButton blackBtn = new JButton("Black");
		blackBtn.setIcon(ResourceLoader.getPieceIcon(blackKing, iconSize));
		blackBtn.setHorizontalTextPosition(SwingConstants.CENTER);
		blackBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
		blackBtn.setFocusable(false);
		blackBtn.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
		blackBtn.addActionListener(e -> {
			dialog.dispose();
			launchEngineGame(PlayerColor.BLACK);
		});

		buttonPanel.add(whiteBtn);
		buttonPanel.add(blackBtn);

		// No redundant header label - window title is enough
		dialog.add(buttonPanel, BorderLayout.CENTER);

		JButton cancelBtn = new JButton("❌");
		cancelBtn.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 24));
		cancelBtn.setFocusable(false);
		cancelBtn.addActionListener(e -> dialog.dispose());
		JPanel bottomPanel = new JPanel();
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		bottomPanel.add(cancelBtn);
		dialog.add(bottomPanel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private void launchEngineGame(PlayerColor you) {
		GameLauncher.launch(gameState, controller);
		controller.startEngineGame(you);
		ChessFrame.getMoveListPanel().clearMoves();
		dispose();
	}

	/** Helper: waiting dialog with spinner while hosting. */
	private JDialog openWaitingDialog(String title) {
		JDialog dialog = new JDialog(this, title, false);
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		dialog.add(progressBar);
		dialog.setSize(300, 75);
		dialog.setLocationRelativeTo(this);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		return dialog;
	}

	/** Helper: loading dialog with spinner while connecting as client. */
	private JDialog openLoadingDialog(String title) {
		JDialog dialog = new JDialog(this, title, true);
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		dialog.add(progressBar);
		dialog.setSize(300, 75);
		dialog.setLocationRelativeTo(this);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		return dialog;
	}

	/** Open the multiplayer menu window. */
	public void openMultiplayerMenu() {
		new MultiplayerMenuFrame(this);
		setVisible(false);
	}
}
