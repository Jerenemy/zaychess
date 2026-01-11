package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.controller.input.MouseInputHandler;
import com.jeremyzay.zaychess.controller.saveload.SaveManager;
import com.jeremyzay.zaychess.model.board.Board;

/**
 * Main game window for the chess application.
 *
 * Contains:
 *  - BoardPanel (the chessboard)
 *  - MoveListPanel (list of moves played)
 *  - StatusPanel (whose turn, checkmate, etc.)
 *  - Top bar with controls (centered) + toggle to show/hide the right panel
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

    // split + toggle state
    private JSplitPane split;
    private boolean movesVisible = true;
    private int lastDivider = -1;           // remember size when hidden
    private JToggleButton movesToggle;      // always visible in top bar

    public ChessFrame(Board board, GameController controller) {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // board + input
        MouseInputHandler mouseInputHandler = new MouseInputHandler(controller);
        boardPanel = new BoardPanel(board, mouseInputHandler);
        controller.setBoardPanel(boardPanel);

        // --------- TOP BAR (CENTERED): toggle + available buttons ---------
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));

        movesToggle = new JToggleButton("≡", true);
        movesToggle.setFocusable(false);
        movesToggle.setToolTipText("Toggle move panel");
        movesToggle.addActionListener(e -> toggleMovesPanel());
        topBar.add(movesToggle);

        // Offline-only: undo/redo (skip when engine drives moves)
        if (!controller.isOnline() && !controller.isUsingEngine()) {
            JButton undoButton = new JButton("←");
            undoButton.setMargin(new Insets(2, 6, 2, 6));
            undoButton.setFocusable(false);
            undoButton.setToolTipText("Undo move");
            undoButton.addActionListener(e -> { controller.undo(); boardPanel.repaint(); });
            topBar.add(undoButton);

            JButton redoButton = new JButton("→");
            redoButton.setMargin(new Insets(2, 6, 2, 6));
            redoButton.setFocusable(false);
            redoButton.setToolTipText("Redo move");
            redoButton.addActionListener(e -> { controller.redo(); boardPanel.repaint(); });
            topBar.add(redoButton);
        }

        // Save is usually offline; add only when it makes sense
        if (!controller.isOnline()) {
            JButton saveButton = new JButton("💾");
            saveButton.setMargin(new Insets(2, 6, 2, 6));
            saveButton.setFocusable(false);
            saveButton.setToolTipText("Save game");
            saveButton.addActionListener(e -> handleSave(controller));
            topBar.add(saveButton);
        }

        // Exit always available
        JButton exitButton = new JButton("❌");
        exitButton.setMargin(new Insets(2, 6, 2, 6));
        exitButton.setFocusable(false);
        exitButton.setToolTipText("Back to main menu");
        exitButton.addActionListener(e -> {
            ChessFrame.this.dispose();
            SwingUtilities.invokeLater(() -> new MainMenuFrame().setVisible(true));
        });
        topBar.add(exitButton);

        add(topBar, BorderLayout.NORTH);

        // --------- CENTER: split with board on left, move list on right ---------
        JComponent boardCenter = new AspectRatioPanel(boardPanel);
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardCenter, moveListPanel);
        split.setBorder(null);
        split.setResizeWeight(1.0);            // prioritize board on resize
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        split.setDividerLocation(0.75);        // ~75% board width

        add(split, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        setTitle("Zaychess");
        setResizable(true);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // F11 = fullscreen toggle
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("F11"), "toggleFS");
        getRootPane().getActionMap().put("toggleFS", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) { toggleFullscreen(); }
        });

        // Optional: Ctrl+M toggles the move list too
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ctrl M"), "toggleMoves");
        getRootPane().getActionMap().put("toggleMoves", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { toggleMovesPanel(); }
        });
    }

    /** Save dialog (offline). */
    private void handleSave(GameController controller) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save game");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Schachdatei (*.chesslog)", "chesslog"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            String path = selected.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".chesslog")) path += ".chesslog";
            File finalFile = new File(path);

            if (finalFile.exists()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "This game (" + finalFile.getName().split("\\.")[0] + ") already exists. Do you want to overwrite?",
                        "Confirmation", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
            }

            try {
                new SaveManager(controller).saveGame(finalFile);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Toggle fullscreen mode on/off. */
    private void toggleFullscreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (!fullscreen) {
            windowedBounds = getBounds();
            windowedDecorations = isUndecorated();
            try {
                dispose();
                setUndecorated(true);
                setVisible(true);
                if (gd.isFullScreenSupported()) gd.setFullScreenWindow(this);
                else setExtendedState(JFrame.MAXIMIZED_BOTH);
                fullscreen = true;
            } catch (Exception ex) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
                fullscreen = true;
            }
        } else {
            try { if (gd.getFullScreenWindow() == this) gd.setFullScreenWindow(null); } catch (Exception ignored) {}
            dispose();
            setUndecorated(windowedDecorations);
            if (windowedBounds != null) setBounds(windowedBounds);
            setVisible(true);
            fullscreen = false;
        }
    }

    // Accessors
    public BoardPanel getBoardPanel() { return boardPanel; }
    public static MoveListPanel getMoveListPanel() { return moveListPanel; }
    public static StatusPanel getStatusPanel() { return statusPanel; }

    /** Update the GUI board from a given model state. */
    public void updateBoard(Board board) { boardPanel.updateBoard(board); }

    /** Show/hide the right move list (toggle stays visible in the top bar). */
    private void toggleMovesPanel() {
        if (movesVisible) {
            lastDivider = split.getDividerLocation();
            split.setRightComponent(null);
            movesVisible = false;
        } else {
            split.setRightComponent(moveListPanel);
            split.resetToPreferredSizes();
            if (lastDivider > 0) split.setDividerLocation(lastDivider);
            movesVisible = true;
        }
        if (movesToggle != null) movesToggle.setSelected(movesVisible);
        revalidate();
        repaint();
    }
}
