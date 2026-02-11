package com.jeremyzay.zaychess.controller.game;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.services.infrastructure.network.NetworkTransport;
import com.jeremyzay.zaychess.services.infrastructure.network.RelayClient;
import com.jeremyzay.zaychess.services.infrastructure.network.TcpClient;
import com.jeremyzay.zaychess.services.infrastructure.network.TcpHost;
import com.jeremyzay.zaychess.view.gui.MainFrame;

/**
 * Boots the chess UI in offline or online (host/client) modes and wires the
 * controller to a network transport.
 */
public class GameLauncher {
    public static void launch(GameState gameState, GameController controller) {
        MainFrame.getInstance().switchToGame(
                new com.jeremyzay.zaychess.view.gui.ChessPanel(gameState.getBoard(), controller));
    }

    public static void launchAsHost(GameState gameState, GameController controller,
            JDialog waitingDialog) {
        try {
            NetworkTransport host = new TcpHost(5000);
            controller.setLocalSide(PlayerColor.WHITE);
            controller.attachNetwork(host);
            if (waitingDialog != null)
                waitingDialog.dispose();
            SwingUtilities.invokeLater(() -> {
                MainFrame.getInstance().switchToGame(
                        new com.jeremyzay.zaychess.view.gui.ChessPanel(gameState.getBoard(), controller));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean launchAsClient(GameState gameState, GameController controller, String ip) {
        try {
            NetworkTransport client = new TcpClient(ip, 5000);
            controller.setLocalSide(PlayerColor.BLACK);
            controller.attachNetwork(client);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        MainFrame.getInstance().switchToGame(
                new com.jeremyzay.zaychess.view.gui.ChessPanel(gameState.getBoard(), controller));
        return true;
    }

    public static void launchOnline(GameState gameState, GameController controller,
            JDialog waitingDialog,
            java.util.concurrent.atomic.AtomicReference<RelayClient> clientRef,
            java.util.concurrent.atomic.AtomicBoolean cancelled) {
        try {
            String relayHost = System.getenv("ZAYCHESS_RELAY_SERVER");
            if (relayHost == null || relayHost.isBlank()) {
                relayHost = "172.104.31.166";
            }
            int relayPort = 8080;

            if (cancelled.get())
                return;

            RelayClient client = new RelayClient(relayHost, relayPort);
            if (clientRef != null) {
                clientRef.set(client);
            }

            // Check cancellation again after potentially slow connection setup
            if (cancelled.get()) {
                client.close();
                return;
            }

            client.setMatchmakingListener(new RelayClient.MatchmakingListener() {
                @Override
                public void onMatchFound(boolean isHost) {
                    if (cancelled.get()) {
                        client.close();
                        return;
                    }
                    System.out.println("Match found! I am " + (isHost ? "HOST (White)" : "CLIENT (Black)"));
                    client.setMatchmakingListener(null);
                    controller.setLocalSide(isHost ? PlayerColor.WHITE : PlayerColor.BLACK);
                    controller.attachNetwork(client);
                    client.send("HELLO");

                    SwingUtilities.invokeLater(() -> {
                        if (waitingDialog != null)
                            waitingDialog.dispose();
                        MainFrame.getInstance().switchToGame(
                                new com.jeremyzay.zaychess.view.gui.ChessPanel(gameState.getBoard(), controller));
                    });
                }

                @Override
                public void onMatchmakingError(String msg) {
                    if (cancelled.get())
                        return;
                    System.err.println("Matchmaking error: " + msg);
                    SwingUtilities.invokeLater(() -> {
                        if (waitingDialog != null)
                            waitingDialog.dispose();
                        javax.swing.JOptionPane.showMessageDialog(MainFrame.getInstance(),
                                "Matchmaking error: " + msg,
                                "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        MainFrame.getInstance().showMenu(); // Ensure we return to menu
                    });
                }
            });

            if (cancelled.get()) {
                client.close();
                return;
            }
            client.start();

        } catch (Exception e) {
            if (cancelled.get())
                return;
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                if (waitingDialog != null)
                    waitingDialog.dispose();
                MainFrame.getInstance().showMenu(); // Ensure we return to menu
            });
        }
    }
}
