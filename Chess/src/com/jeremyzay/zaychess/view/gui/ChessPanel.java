package com.jeremyzay.zaychess.view.gui;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.controller.input.MouseInputHandler;
import com.jeremyzay.zaychess.controller.saveload.SaveManager;
import com.jeremyzay.zaychess.model.board.Board;

public class ChessPanel extends JPanel {
    private final BoardPanel boardPanel;
    private static final MoveListPanel moveListPanel = new MoveListPanel();
    private static final StatusPanel statusPanel = new StatusPanel();
    private final DragGlassPane dragGlassPane;

    private JSplitPane split;
    private boolean movesVisible = true;
    private int lastDivider = -1;
    private JToggleButton movesToggle;

    public ChessPanel(Board board, GameController controller) {
        setLayout(new BorderLayout());

        // board + input
        MouseInputHandler mouseInputHandler = new MouseInputHandler(controller);
        boardPanel = new BoardPanel(board, mouseInputHandler);
        controller.setBoardPanel(boardPanel);

        // Glass Pane logic needs to happen on the WINDOW, not the panel.
        // We'll handle this by attaching to MainFrame when shown, or MainFrame handles
        // it.
        // Ideally, MainFrame has the GlassPane.
        // For now, let's create it, but we need to attach it to MainFrame instance.
        dragGlassPane = new DragGlassPane();
        MainFrame.getInstance().setGlassPane(dragGlassPane);
        dragGlassPane.setVisible(true); // GlassPane must be visible to intercept events? Usually yes for painting.
        // But DragGlassPane might be custom. existing code: setVisible(true) in
        // StartDrag?
        // Let's check DragGlassPane later. logic: setGlassPane(dragGlassPane).

        boardPanel.setDragGlassPane(dragGlassPane);
        boardPanel.setController(controller);

        // --------- TOP BAR ---------
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));

        movesToggle = new JToggleButton("≡", true);
        movesToggle.setFocusable(false);
        movesToggle.setToolTipText("Toggle move panel");
        movesToggle.addActionListener(e -> toggleMovesPanel());
        topBar.add(movesToggle);

        if (!controller.isOnline()) {
            JButton undoButton = new JButton("←");
            undoButton.setMargin(new Insets(2, 6, 2, 6));
            undoButton.setFocusable(false);
            undoButton.addActionListener(e -> {
                controller.undo();
                boardPanel.repaint();
            });
            topBar.add(undoButton);

            JButton redoButton = new JButton("→");
            redoButton.setMargin(new Insets(2, 6, 2, 6));
            redoButton.setFocusable(false);
            redoButton.addActionListener(e -> {
                controller.redo();
                boardPanel.repaint();
            });
            topBar.add(redoButton);
        }

        if (!controller.isOnline()) {
            JButton saveButton = new JButton("💾");
            saveButton.setMargin(new Insets(2, 6, 2, 6));
            saveButton.setFocusable(false);
            saveButton.addActionListener(e -> handleSave(controller));
            topBar.add(saveButton);
        }

        JButton exitButton = new JButton("Menu");
        exitButton.setMargin(new Insets(2, 6, 2, 6));
        exitButton.setFocusable(false);
        exitButton.addActionListener(e -> {
            MainFrame.getInstance().showMenu();
        });
        topBar.add(exitButton);

        add(topBar, BorderLayout.NORTH);

        // --------- CENTER ---------
        JComponent boardCenter = new AspectRatioPanel(boardPanel);
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardCenter, moveListPanel);
        split.setBorder(null);
        split.setResizeWeight(1.0);
        split.setOneTouchExpandable(true);
        split.setDividerLocation(0.75);

        add(split, BorderLayout.CENTER);
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

    public static StatusPanel getStatusPanel() {
        return statusPanel;
    }

    private void toggleMovesPanel() {
        if (movesVisible) {
            lastDivider = split.getDividerLocation();
            split.setRightComponent(null);
            movesVisible = false;
        } else {
            split.setRightComponent(moveListPanel);
            if (lastDivider > 0)
                split.setDividerLocation(lastDivider);
            movesVisible = true;
        }
        if (movesToggle != null)
            movesToggle.setSelected(movesVisible);
        revalidate();
        repaint();
    }
}
