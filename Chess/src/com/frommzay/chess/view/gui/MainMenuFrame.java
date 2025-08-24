package com.frommzay.chess.view.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.*;

import com.frommzay.chess.controller.game.GameController;
import com.frommzay.chess.controller.game.GameLauncher;
import com.frommzay.chess.controller.saveload.SaveManager;
import com.frommzay.chess.model.game.GameState;

/**
 * The entry point window for the chess application.
 * Provides main menu options:
 *  - Start local game
 *  - Load saved game
 *  - Host or join multiplayer game
 */
public class MainMenuFrame extends JFrame {
	private static final long serialVersionUID = 4115239067261707835L;

	private final GameState gameState = new GameState();
	private final GameController controller;

    {
        try {
            controller = new GameController(gameState, null,true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MainMenuFrame() {
		setTitle("Schach – Launcher");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		// Menu panel with buttons
		MainMenuPanel menuPanel = new MainMenuPanel(this);
		add(menuPanel, BorderLayout.CENTER);

		setSize(768, 432);
		setLocationRelativeTo(null);
		setResizable(false);
	}

	/** Start a fresh offline game. */
	public void startLocalGame() {
		GameLauncher.launch(gameState, controller);
		ChessFrame.getMoveListPanel().clearMoves();
		dispose();
	}

	/** Load a saved offline game from disk. */
	public void loadLocalGame() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Spiel laden");
		chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Schachdatei (*.chesslog)", "chesslog"));

		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			try {
				GameLauncher.launch(gameState, controller);
				ChessFrame.getMoveListPanel().clearMoves();

				new SaveManager(controller).loadGame(chooser.getSelectedFile());

				dispose();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Fehler beim Laden: " + ex.getMessage(),
						"Ladefehler", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/** Start as host in multiplayer. Shows waiting dialog until a client connects. */
	public void startHostGame() {
		JDialog waitingDialog = openWaitingDialog();
		// When waiting window is closed, clean up host
		waitingDialog.addWindowListener(new WindowAdapter() {
		    @Override public void windowClosing(WindowEvent e) {
		        if (controller.getHost() != null) {
		            try { controller.getHost().close(); } catch (Exception ex) { ex.printStackTrace(); }
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
		JDialog loadingDialog = openLoadingDialog();
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

    // in MainMenuFrame
    public void startVsComputer() {
        Object[] options = {"Play White", "Play Black", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                this, "Choose your side", "Play vs Computer",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]
        );
        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) return;

        com.frommzay.chess.model.util.PlayerColor you =
                (choice == 0) ? com.frommzay.chess.model.util.PlayerColor.WHITE
                        : com.frommzay.chess.model.util.PlayerColor.BLACK;

        GameLauncher.launch(gameState, controller);
        controller.startEngineGame(you);          // NEW (see below)
        ChessFrame.getMoveListPanel().clearMoves();
        dispose();
    }


    /** Helper: waiting dialog with spinner while hosting. */
	private JDialog openWaitingDialog() {
		JDialog dialog = new JDialog(this, "Warte auf Gegner...", false);
		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		dialog.add(progressBar);
		dialog.setSize(300, 75);
		dialog.setLocationRelativeTo(this);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		return dialog;
	}

	/** Helper: loading dialog with spinner while connecting as client. */
	private JDialog openLoadingDialog() {
		JDialog dialog = new JDialog(this, "Verbinde...", true);
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
