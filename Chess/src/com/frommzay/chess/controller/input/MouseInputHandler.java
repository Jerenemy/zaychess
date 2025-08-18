package com.frommzay.chess.controller.input;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.frommzay.chess.controller.game.GameController;
import com.frommzay.chess.model.util.Position;
import com.frommzay.chess.view.gui.swing.SquareButton;

/**
 * Handles mouse input on the chessboard squares.
 *
 * Each {@link SquareButton} on the board has an associated position.
 * When clicked, this handler extracts the position and forwards it
 * to the {@link GameController} for validation and move processing.
 */
public class MouseInputHandler implements ActionListener {
    private final GameController controller;

    /** Constructs a handler bound to a specific game controller. */
    public MouseInputHandler(GameController controller) {
        this.controller = controller;
    }

    /**
     * Called whenever a square is clicked.
     * Identifies the clicked square and notifies the controller.
     *
     * @param e the action event triggered by a square button click
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof SquareButton squareButton) {
            Position position = squareButton.getPosition();
            controller.validateSquareClick(position);
        }
    }
}
