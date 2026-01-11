package com.jeremyzay.zaychess.controller.game;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.services.infrastructure.network.NetworkTransport;
import com.jeremyzay.zaychess.services.infrastructure.network.TcpClient;
import com.jeremyzay.zaychess.services.infrastructure.network.TcpHost;
import com.jeremyzay.zaychess.view.gui.ChessFrame;
import com.jeremyzay.zaychess.view.gui.MainMenuFrame;

/**
 * Boots the chess UI in offline or online (host/client) modes and wires the controller to a network transport.
 * Responsibilities:
 *   Start an offline (hot-seat) game.
 *   Create and attach a {@link NetworkTransport} for host/client online play.
 *   Ensure UI windows are created on the Swing EDT and any menu/wait dialogs are closed appropriately.
 * Threading:
 *  Uses {@link SwingUtilities#invokeLater(Runnable)} when constructing the main frame from host mode
 *       after the waiting dialog closes to keep all UI work on the EDT.
 * Side effects:
 *   Creates a {@link ChessFrame} window.
 *   In online modes, sets the local side on the {@link GameController} (WHITE for host, BLACK for client)
 *       and calls {@link GameController#attachNetwork(NetworkTransport)}.
 */
public class GameLauncher {
    public static void launch(GameState gameState, GameController controller) {
        new ChessFrame(gameState.getBoard(), controller); // offline/local
    }

    /**
     * Launches the game as an online host (server). Binds a TCP host on the default port,
     * assigns the local side to {@code WHITE}, attaches the transport to the controller,
     * closes the optional waiting dialog, and opens the main {@link ChessFrame} on the EDT.
     *
     * @param gameState     the game state whose board will be shown
     * @param controller    the controller to wire; its local side is set to {@link PlayerColor#WHITE}
     * @param waitingDialog an optional modal dialog shown while waiting for connections; disposed if non-null
     * @param menuFrame     an optional main-menu frame to close after launching; disposed if non-null
     */
    public static void launchAsHost(GameState gameState, GameController controller,
                                    JDialog waitingDialog, MainMenuFrame menuFrame) {
        try {
	    	NetworkTransport host = new TcpHost(5000);
	    	controller.setLocalSide(PlayerColor.WHITE);
	    	controller.attachNetwork(host);
            if (waitingDialog != null) waitingDialog.dispose();
            SwingUtilities.invokeLater(() -> {
                new ChessFrame(gameState.getBoard(), controller);
                if (menuFrame != null) menuFrame.dispose();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Launches the game as an online client. Connects to a host at the given IP on the default port,
     * assigns the local side to {@code BLACK}, attaches the transport to the controller,
     * constructs the {@link ChessFrame}, and returns whether the connection succeeded.
     *
     * @param gameState  the game state whose board will be shown
     * @param controller the controller to wire; its local side is set to {@link PlayerColor#BLACK}
     * @param ip         the host/server IP or hostname to connect to (port 5000 is used)
     * @return {@code true} if the TCP client was created and the frame launched; {@code false} on error
     */
    public static boolean launchAsClient(GameState gameState, GameController controller, String ip) {
        try {
            NetworkTransport client = new TcpClient(ip, 5000);
            controller.setLocalSide(PlayerColor.BLACK);
            controller.attachNetwork(client);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        new ChessFrame(gameState.getBoard(), controller);
        return true;
    }
}
