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
    public static final String VIEW_LOADING = "LOADING";

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

    private void resetGameSessionAsync(Runnable onComplete) {
        System.out.println("[DEBUG] resetGameSessionAsync called.");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // 1. Cleanup Old (Background)
        new Thread(() -> {
            if (controller != null) {
                System.out.println("[DEBUG] Stopping engine/network in background...");
                controller.detachNetwork();
                controller.stopEngine();
                System.out.println("[DEBUG] Engine/network stopped.");
            }

            // 2. Setup New (EDT)
            SwingUtilities.invokeLater(() -> {
                System.out.println("[DEBUG] Finishing reset on EDT...");
                gameState.restoreFrom(new GameState());
                try {
                    controller = new GameController(gameState, null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                ChessPanel.getStatusPanel().setStatus("Ready");
                ChessPanel.getMoveListPanel().clearMoves();

                setCursor(Cursor.getDefaultCursor());

                if (onComplete != null) {
                    onComplete.run();
                }
            });
        }).start();
    }

    public void startLocalGame() {
        resetGameSessionAsync(() -> {
            GameLauncher.launch(gameState, controller);
        });
    }

    // Called by GameLauncher
    public void switchToGame(JPanel gamePanel) {
        // ... (existing logic)
        mainPanel.add(gamePanel, VIEW_GAME);
        showView(VIEW_GAME);
    }

    public void startVsComputer() {
        resetGameSessionAsync(() -> {
            // setEngine involves I/O and might block, so run it in background too
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            new Thread(() -> {
                controller.setEngine();
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    if (controller.isUsingEngine()) {
                        showAISideSelectionDialog();
                    }
                });
            }).start();
        });
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
        // GameLauncher.launch is already called in startVsComputer? No, that was
        // creating controller.
        // Wait, startVsComputer calls reset -> setEngine -> showDialog.
        // Dialog calls launchEngineGame.
        // We need to launch here.
        GameLauncher.launch(gameState, controller);
        controller.startEngineGame(humanSide);
        // ChessPanel.getMoveListPanel().clearMoves(); // Already cleared in reset
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
            resetGameSessionAsync(() -> {
                try {
                    GameLauncher.launch(gameState, controller);
                    new SaveManager(controller).loadGame(selectedFile);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error loading: " + ex.getMessage());
                }
            });
        } else if (choice == 1) {
            resetGameSessionAsync(() -> {
                try {
                    // setEngine might be slow
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    new Thread(() -> {
                        controller.setEngine();
                        SwingUtilities.invokeLater(() -> {
                            setCursor(Cursor.getDefaultCursor());
                            if (controller.isUsingEngine()) {
                                showAISideSelectionDialogForLoad(selectedFile);
                            }
                        });
                    }).start();
                } catch (Exception e) {
                }
            });
        }
    }

    private void showAISideSelectionDialogForLoad(java.io.File file) {
        // ... (existing implementation)
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
            // moves cleared in reset
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
        resetGameSessionAsync(() -> {
            // Create atomic ref for the client so we can cancel it
            java.util.concurrent.atomic.AtomicReference<com.jeremyzay.zaychess.services.infrastructure.network.RelayClient> clientRef = new java.util.concurrent.atomic.AtomicReference<>();

            LoadingOverlay loadingOverlay = new LoadingOverlay("Waiting for opponent...", e -> {
                com.jeremyzay.zaychess.services.infrastructure.network.RelayClient client = clientRef.get();
                if (client != null) {
                    client.close();
                }
                showMenu(); // Return to menu on cancel
            });

            addView(VIEW_LOADING, loadingOverlay);
            showView(VIEW_LOADING);
            loadingOverlay.start();

            new Thread(() -> {
                GameLauncher.launchOnline(gameState, controller, null, clientRef);
            }).start();
        });
    }

    // kept for reference or other uses
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
