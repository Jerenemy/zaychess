package com.frommzay.chess.services.application.history;

import java.util.ArrayDeque;
import java.util.Deque;

import com.frommzay.chess.model.game.GameState;
import com.frommzay.chess.model.move.Move;

/**
 * In-memory implementation of {@link MoveHistoryService}.
 *
 * Maintains two stacks (undo/redo) of moves and snapshots, allowing the
 * user to undo or redo moves while preserving full game state.
 */
public final class MoveHistoryInMemory implements MoveHistoryService {

    /**
     * Represents a single move in history, along with the full
     * {@link GameState} snapshot before the move was applied.
     */
    private static final class Entry {
        final Move move;
        final GameState snapshotBefore; // castling rights, en passant, clocks, etc.
        Entry(Move m, GameState s) { move = m; snapshotBefore = s; }
    }

    /** Stack of moves available to undo. */
    private final Deque<Entry> undo = new ArrayDeque<>();
    /** Stack of moves available to redo. */
    private final Deque<Entry> redo = new ArrayDeque<>();

    /**
     * Record a move and its pre-move snapshot.
     * Clears redo stack since new moves overwrite future history.
     */
    public void record(GameState gs, Move m, GameState snap) {
        undo.push(new Entry(m, snap));
        redo.clear();
    }

    /** @return true if there is at least one move to undo. */
    public boolean canUndo() { return !undo.isEmpty(); }

    /**
     * Undo the most recent move by restoring its snapshot.
     * Pushes the undone move to the redo stack.
     */
    public void undo(GameState gs) {
        Entry e = undo.pop();
        gs.restoreFrom(e.snapshotBefore);
        redo.push(e);
    }

    /** @return true if there is at least one move to redo. */
    public boolean canRedo() { return !redo.isEmpty(); }

    /**
     * Redo the most recently undone move.
     * Re-applies the move and pushes it back onto the undo stack.
     */
    public void redo(GameState gs) {
        Entry e = redo.pop();
        gs.applyMove(e.move);
        undo.push(e);
    }

    /**
     * @return the next move available to redo, or {@code null} if none.
     */
    @Override
    public Move peekRedoMove() {
        return redo.isEmpty() ? null : redo.peek().move;
    }
}
