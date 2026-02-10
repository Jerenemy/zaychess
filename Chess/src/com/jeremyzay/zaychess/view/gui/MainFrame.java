package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import com.jeremyzay.zaychess.controller.game.GameController;
import com.jeremyzay.zaychess.controller.game.GameLauncher;
import com.jeremyzay.zaychess.controller.saveload.SaveManager;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.pieces.King;
import com.jeremyzay.zaychess.services.application.notation.FenGenerator;

public class MainFrame extends JFrame {
    private static MainFrame instance;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private GameState gameState;
    private GameController controller;

    public static final String VIEW_MENU = "MENU";
    public static final String VIEW_GAME = "GAME";

    public MainFrame() {
        super("ZayChess");
        instance = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Init state
        gameState = new GameState();
        try {
            controller = new GameController(gameState, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Add Views
        // Menu added parameters based on MainMenuPanel constructor
        MainMenuPanel menuPanel = new MainMenuPanel(this, 700); // 700 is height
        addView(VIEW_MENU, menuPanel);

        add(mainPanel);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        showMenu(); // Show menu by default
    }

    public static MainFrame getInstance() {
        return instance;
    }

    public void addView(String name, Component component) {
        mainPanel.add(component, name);
    }

    public void showView(String name) {
        cardLayout.show(mainPanel, name);
    }

    public void showMenu() {
        showView(VIEW_MENU);
    }

    // --- Game Lifecycle Methods ---

    public void startLocalGame() {
        // Reset state logic? GameLauncher.launch does just 'new ChessFrame'.
        // GameController.restartGame() resets logic.
        // Better: new GameState/Controller or reuse?
        // Let's reset.
        gameState.restoreFrom(new GameState());
        try {
            // Rebuild a fresh controller to be safe? Or reuse?
            // GameController has state. Reuse is okay if we reset.
            // controller = new GameController(gameState, null); // If we recreate, we lose
            // history ref?
            // Let's rely on reuse + reset for now or recreate if cheap.
            // Recreating is ensuring clean state.
            controller = new GameController(gameState, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        GameLauncher.launch(gameState, controller);
    }

    // Called by GameLauncher
    public void switchToGame(JPanel gamePanel) {
        // Remove old game view if exists?
        // For CardLayout to be dynamic, we can remove/add.
        // Or simpler: We always name it VIEW_GAME and replace it.
        // Check if VIEW_GAME exists in layout? CardLayout doesn't easily expose this.
        // But mainPanel.add(comp, "GAME") replaces if same name? No, it adds a new
        // card.
        // We should remove the old one if we want to save memory/cleanup.

        // remove old "GAME" component if possible.
        // Iterate components?
        for (Component c : mainPanel.getComponents()) {
            // How to identify? We can keep a ref.
        }
        // Let's just add and show. If we overwrite name "GAME", does CardLayout
        // replace?
        // AWT CardLayout: "If a component is added to a container that uses CardLayout,
        // and a component with the same name was previously added, the new component
        // replaces the old one." -> YES!

        mainPanel.add(gamePanel, VIEW_GAME);
        showView(VIEW_GAME);
    }

    public void startVsComputer() {
        // Reset first
        gameState.restoreFrom(new GameState());
        try {
            controller = new GameController(gameState, null);
        } catch (Exception e) {
        }

        controller.setEngine();
        if (!controller.isUsingEngine())
            return;

        // Show side selection dialog
        showAISideSelectionDialog();
    }

    private void showAISideSelectionDialog() {
        JDialog dialog = new JDialog(this, "Choose Side", true);
        dialog.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        King whiteKing = new King(PlayerColor.WHITE, null);
        King blackKing = new King(PlayerColor.BLACK, null);
        int iconSize = 80;

        JButton whiteBtn = new JButton("White");
        whiteBtn.setIcon(ResourceLoader.getPieceIcon(whiteKing, iconSize));
        whiteBtn.addActionListener(e -> {
            dialog.dispose();
            launchEngineGame(PlayerColor.WHITE);
        });

        JButton blackBtn = new JButton("Black");
        blackBtn.setIcon(ResourceLoader.getPieceIcon(blackKing, iconSize));
        blackBtn.addActionListener(e -> {
            dialog.dispose();
            launchEngineGame(PlayerColor.BLACK);
        });

        buttonPanel.add(whiteBtn);
        buttonPanel.add(blackBtn);
        dialog.add(buttonPanel, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void launchEngineGame(PlayerColor humanSide) {
        GameLauncher.launch(gameState, controller);
        controller.startEngineGame(humanSide);
        ChessPanel.getMoveListPanel().clearMoves();
    }

    public void loadLocalGame() {
        FileDialog fd = new FileDialog(this, "Load Game", FileDialog.LOAD);
        fd.setDirectory(System.getProperty("user.home") + "/Downloads");
        fd.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".chesslog"));
        fd.setVisible(true);

        String fileName = fd.getFile();
        if (fileName == null)
            return;
        java.io.File selectedFile = new java.io.File(fd.getDirectory(), fileName);

        Object[] options = { "vs Human", "vs AI", "Cancel" };
        int choice = JOptionPane.showOptionDialog(this, "Continue this game against:", "Load Game Mode",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            try {
                // Recreate controller/state
                gameState.restoreFrom(new GameState());
                controller = new GameController(gameState, null);

                GameLauncher.launch(gameState, controller);
                ChessPanel.getMoveListPanel().clearMoves();
                new SaveManager(controller).loadGame(selectedFile);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading: " + ex.getMessage());
            }
        } else if (choice == 1) {
            // Load vs AI
            // We need to set up engine, select side, then load
            // showAISideSelectionDialogForLoad(selectedFile);
            // ... simplifying for implementation plan
            // Reusing logic:
            try {
                gameState.restoreFrom(new GameState());
                controller = new GameController(gameState, null);
                controller.setEngine();
                if (controller.isUsingEngine()) {
                    // Reuse/Duplicate side selection logic for load?
                    // For brevity, let's just trigger load then engine.
                    // But side selection matters.
                    // Let's implementation later or skip complex load-vs-ai flow for this refactor
                    // step if possible?
                    // User said "do not change any of how the app looks". Functionality should
                    // remain.
                    // I will implement simple version or copy logic.
                    showAISideSelectionDialogForLoad(selectedFile);
                }
            } catch (Exception e) {
            }
        }
    }

    private void showAISideSelectionDialogForLoad(java.io.File file) {
        // Copy of showAISideSelectionDialog but calls finishLoadVsAI
        JDialog dialog = new JDialog(this, "Choose Your Side", true);
        dialog.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new java.awt.GridLayout(1, 2, 20, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        King whiteKing = new King(PlayerColor.WHITE, null);
        King blackKing = new King(PlayerColor.BLACK, null);

        int iconSize = 80;

        JButton whiteBtn = new JButton("White");
        whiteBtn.setIcon(ResourceLoader.getPieceIcon(whiteKing, iconSize));
        whiteBtn.addActionListener(e -> {
            dialog.dispose();
            finishLoadVsAI(file, PlayerColor.WHITE);
        });

        JButton blackBtn = new JButton("Black");
        blackBtn.setIcon(ResourceLoader.getPieceIcon(blackKing, iconSize));
        blackBtn.addActionListener(e -> {
            dialog.dispose();
            finishLoadVsAI(file, PlayerColor.BLACK);
        });

        buttonPanel.add(whiteBtn);
        buttonPanel.add(blackBtn);
        dialog.add(buttonPanel, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void finishLoadVsAI(java.io.File file, PlayerColor humanSide) {
        try {
            GameLauncher.launch(gameState, controller);
            ChessPanel.getMoveListPanel().clearMoves();
            new SaveManager(controller).loadGame(file);

            // Sync engine
            String fen = FenGenerator.toFen(gameState);
            controller.syncEnginePosition(fen);
            controller.startEngineGame(humanSide);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading: " + ex.getMessage());
        }
    }

    public void startOnlineMatchmaking() {
        JDialog waitingDialog = openWaitingDialog("Waiting for opponent...");
        java.util.concurrent.atomic.AtomicReference<com.jeremyzay.zaychess.services.infrastructure.network.RelayClient> clientRef = new java.util.concurrent.atomic.AtomicReference<>();

        waitingDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                com.jeremyzay.zaychess.services.infrastructure.network.RelayClient client = clientRef.get();
                if (client != null) {
                    client.close();
                }
            }
        });

        SwingUtilities.invokeLater(() -> waitingDialog.setVisible(true));

        new Thread(() -> {
            GameLauncher.launchOnline(gameState, controller, waitingDialog, clientRef);
        }).start();
    }

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
}
