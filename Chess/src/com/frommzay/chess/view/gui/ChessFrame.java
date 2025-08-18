package com.frommzay.chess.view.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

import com.frommzay.chess.controller.game.GameController;
import com.frommzay.chess.controller.input.MouseInputHandler;
import com.frommzay.chess.controller.saveload.SaveManager;
import com.frommzay.chess.model.board.Board;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

/**
 * Main game window for the chess application.
 *
 * Contains:
 *  - BoardPanel (the chessboard)
 *  - MoveListPanel (list of moves played)
 *  - StatusPanel (whose turn, checkmate, etc.)
 *  - Undo/redo buttons and save/load controls (offline mode only)
 *  - Fullscreen toggle (F11)
 */
public class ChessFrame extends JFrame {
	private static final long serialVersionUID = 8987259664968168100L;

	private final BoardPanel boardPanel;
	private static final MoveListPanel moveListPanel = new MoveListPanel();
	private static final StatusPanel statusPanel = new StatusPanel();
	
	// fullscreen state
	private boolean fullscreen = false;
    private Rectangle windowedBounds = null;
    private boolean windowedDecorations = true;

	public ChessFrame(Board board, GameController controller) {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		// attach input handler
		MouseInputHandler mouseInputHandler = new MouseInputHandler(controller);
		boardPanel = new BoardPanel(board, mouseInputHandler);

		controller.setBoardPanel(boardPanel);

		// --- offline UI: undo/redo/save/exit ---
		if (!controller.isOnline()) {
			JButton undoButton = new JButton("←");
			undoButton.addActionListener(e -> {
				controller.undo();
				boardPanel.repaint();
			});
			undoButton.setToolTipText("Spielzug zurücknehmen");

			JButton redoButton = new JButton("→");
			redoButton.addActionListener(e -> {
				controller.redo();
				boardPanel.repaint();
			});
			redoButton.setToolTipText("Spielzug wiederherstellen");

			JPanel undoRedoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
			undoRedoPanel.add(undoButton);
			undoRedoPanel.add(redoButton);

			JPanel rightPanel = new JPanel(new BorderLayout());
			rightPanel.add(undoRedoPanel, BorderLayout.NORTH);
			rightPanel.add(moveListPanel, BorderLayout.CENTER);

			// Exit button
			JButton exitButton = new JButton("Beenden");
			exitButton.setMargin(new Insets(2, 6, 2, 6));
			exitButton.setFocusable(false);
			exitButton.setToolTipText("Zurück zum Hauptmenü");
			exitButton.addActionListener(e -> {
				ChessFrame.this.dispose();
				SwingUtilities.invokeLater(() -> new MainMenuFrame().setVisible(true));
			});

			// Save button
			JButton saveButton = new JButton("💾");
			saveButton.setMargin(new Insets(2, 6, 2, 6));
			saveButton.setFocusable(false);
			saveButton.setToolTipText("Spielstand speichern");
			saveButton.addActionListener(e -> handleSave(controller));

			undoRedoPanel.add(saveButton);
			undoRedoPanel.add(exitButton);

			add(rightPanel, BorderLayout.EAST);
		} else {
			// --- online UI: just moves ---
			add(moveListPanel, BorderLayout.EAST);
		}

		add(new AspectRatioPanel(boardPanel), BorderLayout.CENTER);
		add(statusPanel, BorderLayout.SOUTH);

		setTitle("Schach");
		setResizable(true);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
		// F11 shortcut = fullscreen toggle
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("F11"), "toggleFS");
        getRootPane().getActionMap().put("toggleFS", new AbstractAction() {
            private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent e) { toggleFullscreen(); }
        });
	}
	
	/**
	 * Handles save-to-file dialog for offline play.
	 */
	private void handleSave(GameController controller) {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Spiel speichern");
		chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Schachdatei (*.chesslog)", "chesslog"));

		int result = chooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File selected = chooser.getSelectedFile();
			String path = selected.getAbsolutePath();
			if (!path.toLowerCase().endsWith(".chesslog")) {
				path += ".chesslog";
			}
			File finalFile = new File(path);

			if (finalFile.exists()) {
				int confirm = JOptionPane.showConfirmDialog(this,
						"Der Spielstand (" + finalFile.getName().split("\\.")[0] + ") existiert bereits. Überschreiben?",
						"Bestätigung", JOptionPane.YES_NO_OPTION);
				if (confirm != JOptionPane.YES_OPTION) return;
			}

			try {
				new SaveManager(controller).saveGame(finalFile);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Fehler beim Speichern: " + ex.getMessage(),
						"Fehler", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Toggle fullscreen mode on/off.
	 */
	private void toggleFullscreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        if (!fullscreen) {
            // enter fullscreen
            windowedBounds = getBounds();
            windowedDecorations = isUndecorated();

            try {
                dispose();
                setUndecorated(true);
                setVisible(true);
                if (gd.isFullScreenSupported()) {
                    gd.setFullScreenWindow(this);
                } else {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
                fullscreen = true;
            } catch (Exception ex) {
                // fallback: maximize only
                setExtendedState(JFrame.MAXIMIZED_BOTH);
                fullscreen = true;
            }
        } else {
            // exit fullscreen
            try {
                if (gd.getFullScreenWindow() == this) gd.setFullScreenWindow(null);
            } catch (Exception ignored) {}
            dispose();
            setUndecorated(windowedDecorations);
            if (windowedBounds != null) setBounds(windowedBounds);
            setVisible(true);
            fullscreen = false;
        }
    }

	// Accessors for subpanels
	public BoardPanel getBoardPanel() { return boardPanel; }
	public static MoveListPanel getMoveListPanel() { return moveListPanel; }
	public static StatusPanel getStatusPanel() { return statusPanel; }

	/** Update the GUI board from a given model state. */
	public void updateBoard(Board board) {
		boardPanel.updateBoard(board);
	}
}
