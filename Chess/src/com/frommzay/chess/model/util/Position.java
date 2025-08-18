package com.frommzay.chess.model.util;

/**
 * Immutable value object representing a position on the chessboard.
 * 
 * Coordinates are zero-based: rank 0 is the 8th rank (top), rank 7 is the 1st rank (bottom),
 * file 0 is column 'a' (left), file 7 is column 'h' (right).
 * 
 * Instances are validated at construction to always lie inside the board.
 */
public final class Position {
    private final int rank, file;

    /**
     * Creates a position with the given rank and file.
     *
     * @param rank rank index (0..7)
     * @param file file index (0..7)
     * @throws IllegalArgumentException if the coordinates are out of range
     */
    public Position(int rank, int file) {
        if (rank < 0 || rank > 7 || file < 0 || file > 7)
            throw new IllegalArgumentException("Coords out of range");

        this.rank = rank;
        this.file = file;
    }

    /** Deep-copy constructor. */
    public Position(Position other) {
        this.rank = other.rank;
        this.file = other.file;
    }

    /** @return the rank index (0..7) */
    public int getRank() { return rank; }

    /** @return the file index (0..7) */
    public int getFile() { return file; }

    /**
     * Encodes the position into a simple integer of the form rank*10 + file.
     * 
     * Example: rank=2, file=3 -> returns 23.
     */
    @Override
    public int hashCode() {
        return rank * 10 + file;
    }

    /** @return human-readable string in the form "(rank,file)" */
    @Override
    public String toString() {
        return "(" + getRank() + "," + getFile() + ")";
    }

    /**
     * Compares this position with another object.
     *
     * @param o object to compare with
     * @return true if the other object is a Position with the same rank and file
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position other = (Position) o;
        return this.rank == other.rank && this.file == other.file;
    }

    /** @return a deep copy of this position */
    public Position copy() {
        return new Position(this);
    }
}
