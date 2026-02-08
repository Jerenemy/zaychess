package com.jeremyzay.zaychess.services.application.history;

import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;

/**
 * Service interface for managing move history (undo/redo).
 *
 * Provides APIs to record moves with snapshots, undo them by restoring
 * previous game states, and redo them by reapplying the same move.
 */
public interface MoveHistoryService {

    /**
     * Records a move and its snapshot before execution.
     *
     * @param gs   current game state
     * @param m    the move to record
     * @param snap snapshot of the state before applying the move
     */
    void record(GameState gs, Move m, GameState snap);

    /** Clears all history (undo/redo stacks). */
    void clear();

    /** @return true if at least one move can be undone. */
    boolean canUndo();

    /**
     * Undo the most recent move by restoring its saved snapshot.
     *
     * @param gs the game state to restore into
     */
    void undo(GameState gs);

    /** @return true if at least one move can be redone. */
    boolean canRedo();

    /**
     * Redo the last undone move by reapplying it.
     *
     * @param gs the game state to apply into
     */
    void redo(GameState gs);

    /**
     * Peek at the next redo move without consuming it.
     *
     * @return the move to be redone, or null if none
     */
    default Move peekRedoMove() {
        return null;
    }

    /**
     * @return the last move played (that can be undone), or null if none.
     */
    default Move peekLastMove() {
        return null;
    }

    /**
     * @return all moves in order (earliest first).
     */
    default java.util.List<Move> getMoves() {
        return java.util.Collections.emptyList();
    }
}
