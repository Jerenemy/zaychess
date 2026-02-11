package com.jeremyzay.zaychess.view.gui;

import javax.swing.*;
import java.awt.*;

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
        MainMenuPanel menuPanel = new MainMenuPanel(this, 700);
        addView(VIEW_MENU, menuPanel);

        add(mainPanel);
        setSize(1000, 700);
        setMinimumSize(new Dimension(500, 400));
        setLocationRelativeTo(null);
        showMenu();
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

        new Thread(() -> {
            try {
                if (controller != null) {
                    System.out.println("[DEBUG] Stopping engine/network in background...");
                    controller.detachNetwork();
                    controller.stopEngine();
                    System.out.println("[DEBUG] Engine/network stopped.");
                }
            } catch (Throwable t) {
                System.err.println("Error during game session reset cleanup:");
                t.printStackTrace();
            }

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

    public void switchToGame(JPanel gamePanel) {
        mainPanel.add(gamePanel, VIEW_GAME);
        showView(VIEW_GAME);
    }

    // --- Play vs Computer (inline side selection) ---

    public void startVsComputer() {
        resetGameSessionAsync(() -> {
            showLoadingOverlay("Initializing AI...", null, () -> {
                controller.setEngine();
            }, () -> {
                if (controller.isUsingEngine()) {
                    showSideSelectionOverlay(side -> launchEngineGame(side));
                }
            });
        });
    }

    /**
     * Shows a LoadingOverlay on the glass pane while a background task runs.
     * 
     * @param message  text to display
     * @param onCancel optional cancel action (null = no cancel button)
     * @param bgTask   work to run on a background thread
     * @param onDone   callback on EDT after bgTask completes
     */
    private void showLoadingOverlay(String message, Runnable onCancel,
            Runnable bgTask, Runnable onDone) {
        LoadingOverlay overlay = new LoadingOverlay(message, onCancel != null ? e -> {
            onCancel.run();
        } : null);
        setGlassPane(overlay);
        overlay.setVisible(true);
        overlay.start();

        new Thread(() -> {
            try {
                bgTask.run();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    overlay.stop();
                    overlay.setVisible(false);
                    // Restore blank glass pane
                    JPanel blank = new JPanel();
                    blank.setOpaque(false);
                    blank.setVisible(false);
                    setGlassPane(blank);
                    if (onDone != null)
                        onDone.run();
                });
            }
        }).start();
    }

    /**
     * Shows an inline overlay for choosing White or Black side.
     * 
     * @param onSelect callback receiving the chosen PlayerColor
     */
    private void showSideSelectionOverlay(java.util.function.Consumer<PlayerColor> onSelect) {
        new OverlayPanel() {
            @Override
            protected JPanel createContent() {
                JPanel content = new JPanel();
                content.setOpaque(false);
                content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

                JLabel title = createTitle("Choose Side");
                title.setAlignmentX(Component.CENTER_ALIGNMENT);
                content.add(title);

                JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
                buttonPanel.setOpaque(false);

                King whiteKing = new King(PlayerColor.WHITE, null);
                King blackKing = new King(PlayerColor.BLACK, null);
                int iconSize = 80;

                JButton whiteBtn = createOverlayButton("White");
                whiteBtn.setIcon(ResourceLoader.getPieceIcon(whiteKing, iconSize));
                whiteBtn.addActionListener(e -> {
                    hideOverlay();
                    onSelect.accept(PlayerColor.WHITE);
                });

                JButton blackBtn = createOverlayButton("Black");
                blackBtn.setIcon(ResourceLoader.getPieceIcon(blackKing, iconSize));
                blackBtn.addActionListener(e -> {
                    hideOverlay();
                    onSelect.accept(PlayerColor.BLACK);
                });

                buttonPanel.add(whiteBtn);
                buttonPanel.add(blackBtn);
                content.add(buttonPanel);

                // Cancel button moved to footer
                return content;
            }

            @Override
            protected JComponent createFooter() {
                JButton cancelBtn = createSecondaryButton("Cancel");
                cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                cancelBtn.addActionListener(e -> {
                    hideOverlay();
                    showMenu();
                });
                return cancelBtn;
            }
        }.showOverlay();
    }

    private void launchEngineGame(PlayerColor humanSide) {
        GameLauncher.launch(gameState, controller);
        controller.startEngineGame(humanSide);
    }

    // --- Load Saved Game (inline mode selection) ---

    public void loadLocalGame() {
        FileDialog fd = new FileDialog(this, "Load Game", FileDialog.LOAD);
        fd.setDirectory(System.getProperty("user.home") + "/Downloads");
        fd.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".chesslog"));
        fd.setVisible(true);

        String fileName = fd.getFile();
        if (fileName == null)
            return;
        java.io.File selectedFile = new java.io.File(fd.getDirectory(), fileName);

        // Inline overlay for choosing load mode
        showLoadModeOverlay(selectedFile);
    }

    private void showLoadModeOverlay(java.io.File selectedFile) {
        new OverlayPanel() {
            @Override
            protected JPanel createContent() {
                JPanel content = new JPanel();
                content.setOpaque(false);
                content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

                JLabel title = createTitle("Continue this game against:");
                title.setAlignmentX(Component.CENTER_ALIGNMENT);
                content.add(title);

                JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 16, 0));
                buttonPanel.setOpaque(false);

                JButton humanBtn = createOverlayButton("vs Human");
                humanBtn.addActionListener(e -> {
                    hideOverlay();
                    resetGameSessionAsync(() -> {
                        try {
                            GameLauncher.launch(gameState, controller);
                            new SaveManager(controller).loadGame(selectedFile);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(MainFrame.this,
                                    "Error loading: " + ex.getMessage());
                        }
                    });
                });

                JButton aiBtn = createOverlayButton("vs AI");
                aiBtn.addActionListener(e -> {
                    hideOverlay();
                    resetGameSessionAsync(() -> {
                        showLoadingOverlay("Initializing AI...", null, () -> {
                            controller.setEngine();
                        }, () -> {
                            if (controller.isUsingEngine()) {
                                showSideSelectionOverlay(side -> finishLoadVsAI(selectedFile, side));
                            }
                        });
                    });
                });

                buttonPanel.add(humanBtn);
                buttonPanel.add(aiBtn);
                content.add(buttonPanel);
                return content;
            }

            @Override
            protected JComponent createFooter() {
                JButton cancelBtn = createSecondaryButton("Cancel");
                cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
                cancelBtn.addActionListener(e -> hideOverlay());
                return cancelBtn;
            }
        }.showOverlay();
    }

    private void finishLoadVsAI(java.io.File file, PlayerColor humanSide) {
        try {
            GameLauncher.launch(gameState, controller);
            new SaveManager(controller).loadGame(file);

            String fen = FenGenerator.toFen(gameState);
            controller.syncEnginePosition(fen);
            controller.startEngineGame(humanSide);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading: " + ex.getMessage());
        }
    }

    // --- Online Matchmaking ---

    public void startOnlineMatchmaking() {
        resetGameSessionAsync(() -> {
            java.util.concurrent.atomic.AtomicReference<com.jeremyzay.zaychess.services.infrastructure.network.RelayClient> clientRef = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);

            final LoadingOverlay[] holder = { null };
            holder[0] = new LoadingOverlay("Waiting for opponent...", e -> {
                cancelled.set(true);

                com.jeremyzay.zaychess.services.infrastructure.network.RelayClient client = clientRef.get();
                if (client != null) {
                    client.close();
                }
                // Remove overlay and return to menu
                holder[0].stop();
                holder[0].setVisible(false);
                JPanel blank = new JPanel();
                blank.setOpaque(false);
                blank.setVisible(false);
                setGlassPane(blank);
                showMenu();
            });
            LoadingOverlay loadingOverlay = holder[0];

            // Show as glass pane overlay on top of darkened menu
            setGlassPane(loadingOverlay);
            loadingOverlay.setVisible(true);
            loadingOverlay.start();

            new Thread(() -> {
                GameLauncher.launchOnline(gameState, controller, null, clientRef, cancelled);
            }).start();
        });
    }
}
