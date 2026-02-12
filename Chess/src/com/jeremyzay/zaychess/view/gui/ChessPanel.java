package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import com.jeremyzay.zaychess.view.gui.swing.ZayButton;
import com.jeremyzay.zaychess.view.gui.swing.ZayToggleButton;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.controller.input.MouseInputHandler;
import com.jeremyzay.zaychess.controller.saveload.SaveManager;
import com.jeremyzay.zaychess.model.board.Board;

public class ChessPanel extends JPanel {
    private final BoardPanel boardPanel;
    private static final MoveListPanel moveListPanel = new MoveListPanel();
    private static final CapturedPiecesPanel capturedPiecesPanel = new CapturedPiecesPanel();
    private static final StatusPanel statusPanel = new StatusPanel();
    private final DragGlassPane dragGlassPane;

    // private JSplitPane rightSplit; // Removed
    // private JSplitPane leftSplit; // Removed
    private boolean movesVisible = true;
    private boolean capturedVisible = true;
    // private int lastRightDivider = -1; // Removed
    // private int lastLeftDivider = -1; // Removed
    private ZayToggleButton movesToggle;
    private ZayToggleButton capturedToggle;

    public ChessPanel(Board board, GameController controller) {
        setLayout(new BorderLayout());

        // board + input
        MouseInputHandler mouseInputHandler = new MouseInputHandler(controller);
        boardPanel = new BoardPanel(board, mouseInputHandler);
        controller.setBoardPanel(boardPanel);

        // Glass Pane logic needs to happen on the WINDOW, not the panel.
        dragGlassPane = new DragGlassPane();
        MainFrame.getInstance().setGlassPane(dragGlassPane);
        dragGlassPane.setVisible(true);

        boardPanel.setDragGlassPane(dragGlassPane);
        boardPanel.setController(controller);

        // --------- TOP BAR ---------
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));

        capturedToggle = new ZayToggleButton("♟", true);
        capturedToggle.setPreferredSize(new Dimension(50, 40));
        capturedToggle.setToolTipText("Toggle captured pieces");
        capturedToggle.addActionListener(e -> toggleCapturedPanel());
        topBar.add(capturedToggle);

        movesToggle = new ZayToggleButton("≡", true);
        movesToggle.setPreferredSize(new Dimension(50, 40));
        movesToggle.setToolTipText("Toggle move panel");
        movesToggle.addActionListener(e -> toggleMovesPanel());
        topBar.add(movesToggle);

        if (!controller.isOnline()) {
            ZayButton undoButton = new ZayButton("←");
            undoButton.setPreferredSize(new Dimension(50, 40));
            undoButton.addActionListener(e -> {
                controller.undo();
                boardPanel.repaint();
            });
            topBar.add(undoButton);

            ZayButton redoButton = new ZayButton("→");
            redoButton.setPreferredSize(new Dimension(50, 40));
            redoButton.addActionListener(e -> {
                controller.redo();
                boardPanel.repaint();
            });
            topBar.add(redoButton);
        }

        if (!controller.isOnline()) {
            ZayButton saveButton = new ZayButton("Save");
            saveButton.setPreferredSize(new Dimension(80, 40));
            saveButton.addActionListener(e -> handleSave(controller));
            topBar.add(saveButton);
        }

        if (!controller.isUsingEngine()) {
            ZayButton drawButton = new ZayButton("Offer Draw");
            drawButton.setPreferredSize(new Dimension(100, 40));
            drawButton.addActionListener(e -> {
                controller.offerDraw();
            });
            topBar.add(drawButton);
        }

        ZayButton resignButton = new ZayButton("Resign");
        resignButton.setPreferredSize(new Dimension(80, 40));
        resignButton.addActionListener(e -> {
            controller.resign();
        });
        topBar.add(resignButton);

        ZayButton exitButton = new ZayButton("Menu");
        exitButton.setPreferredSize(new Dimension(80, 40));
        exitButton.addActionListener(e -> {
            controller.detachNetwork();
            MainFrame.getInstance().showMenu();
        });
        topBar.add(exitButton);

        add(topBar, BorderLayout.NORTH);

        // --------- CENTER: left captured | board | right moves ---------
        JComponent boardCenter = new AspectRatioPanel(boardPanel);

        // Add components to main layout
        add(capturedPiecesPanel, BorderLayout.WEST);
        add(boardCenter, BorderLayout.CENTER);
        add(moveListPanel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void handleSave(GameController controller) {
        java.awt.FileDialog fd = new java.awt.FileDialog(MainFrame.getInstance(), "Save Game",
                java.awt.FileDialog.SAVE);
        fd.setDirectory(System.getProperty("user.home") + "/Downloads");
        fd.setFile("game.chesslog");
        fd.setVisible(true);

        String fileName = fd.getFile();
        if (fileName == null)
            return;

        String path = fd.getDirectory() + fileName;
        if (!path.toLowerCase().endsWith(".chesslog"))
            path += ".chesslog";
        File finalFile = new File(path);

        if (finalFile.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Overwrite " + finalFile.getName() + "?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return;
        }

        try {
            new SaveManager(controller).saveGame(finalFile);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public static MoveListPanel getMoveListPanel() {
        return moveListPanel;
    }

    public static CapturedPiecesPanel getCapturedPiecesPanel() {
        return capturedPiecesPanel;
    }

    public static StatusPanel getStatusPanel() {
        return statusPanel;
    }

    private void toggleMovesPanel() {
        if (movesVisible) {
            moveListPanel.setVisible(false);
            movesVisible = false;
        } else {
            moveListPanel.setVisible(true);
            movesVisible = true;
        }
        if (movesToggle != null)
            movesToggle.setSelected(movesVisible);
        revalidate();
        repaint();
    }

    private void toggleCapturedPanel() {
        if (capturedVisible) {
            capturedPiecesPanel.setVisible(false);
            capturedVisible = false;
        } else {
            capturedPiecesPanel.setVisible(true);
            capturedVisible = true;
        }
        if (capturedToggle != null)
            capturedToggle.setSelected(capturedVisible);
        revalidate();
        repaint();
    }
}
