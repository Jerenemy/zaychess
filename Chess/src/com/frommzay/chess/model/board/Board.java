package com.frommzay.chess.model.board;

import java.util.ArrayList;
import java.util.List;

import com.frommzay.chess.model.pieces.Piece;
import com.frommzay.chess.model.util.PlayerColor;
import com.frommzay.chess.model.util.Position;

/**
 * Represents the 8x8 chess board.
 * 
 * Internally the board is stored as a one–dimensional array of length 64
 * for performance. Index 0 corresponds to rank 0, file 0 (a8),
 * and index 63 corresponds to rank 7, file 7 (h1).
 * 
 * Provides direct access to squares for setting and retrieving pieces,
 * as well as utility methods to query board contents and iterate by color.
 */
public class Board {

    private final Piece[] squares1D = new Piece[64];

    /**
     * Returns the piece at the given coordinates.
     *
     * @param rank 0..7 (0 = 8th rank, 7 = 1st rank)
     * @param file 0..7 (0 = file 'a', 7 = file 'h')
     * @return the piece at that square, or null if empty
     */
    public Piece getPieceAt(int rank, int file) {
        return squares1D[rank * 8 + file];
    }

    /**
     * Returns the piece at the given position.
     *
     * @param pos board coordinates
     * @return the piece at that square, or null if empty
     */
    public Piece getPieceAt(Position pos) {
        return squares1D[pos.getRank() * 8 + pos.getFile()];
    }

    /**
     * Places the piece on the given coordinates.
     * 
     * This is raw assignment. Move legality is not checked here.
     *
     * @param rank  rank index (0..7)
     * @param file  file index (0..7)
     * @param piece the piece to place, or null to clear the square
     */
    public void setPieceAt(int rank, int file, Piece piece) {
        squares1D[rank * 8 + file] = piece;
    }

    /**
     * Places the piece at the given position.
     * 
     * This is raw assignment. Move legality is not checked here.
     *
     * @param pos   board coordinates
     * @param piece the piece to place, or null to clear the square
     */
    public void setPieceAt(Position pos, Piece piece) {
        squares1D[pos.getRank() * 8 + pos.getFile()] = piece;
    }

    /**
     * Checks whether a square is empty.
     *
     * @param pos board coordinates
     * @return true if no piece is present
     */
    public boolean isEmpty(Position pos) {
        return squares1D[pos.getRank() * 8 + pos.getFile()] == null;
    }

    /**
     * Checks whether a square is empty.
     *
     * @param rank rank index (0..7)
     * @param file file index (0..7)
     * @return true if no piece is present
     */
    public boolean isEmpty(int rank, int file) {
        return squares1D[rank * 8 + file] == null;
    }

    /**
     * Checks whether the given coordinates are inside the board.
     *
     * @param rank rank index
     * @param file file index
     * @return true if coordinates represent a valid board square
     */
    public Boolean isInside(int rank, int file) {
        try {
            new Position(rank, file);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /** Creates an empty board. */
    public Board() {}

    /**
     * Deep-copy constructor. Copies all pieces from the given board,
     * duplicating them via {@link Piece#copy()}.
     *
     * @param other the board to copy
     */
    public Board(Board other) {
        for (int i = 0; i < 64; i++) {
            Piece p = other.squares1D[i];
            squares1D[i] = (p == null) ? null : p.copy();
        }
    }

    /**
     * Returns all pieces of a given color currently on the board.
     *
     * @param color player color
     * @return list of pieces matching the color
     */
    public List<Piece> getPiecesOfColor(PlayerColor color) {
        List<Piece> pieces = new ArrayList<Piece>();
        for (int i = 0; i < 64; i++) {
            if (squares1D[i] == null) continue;
            if (squares1D[i].getColor() == color) pieces.add(squares1D[i]);
        }
        return pieces;
    }

    /**
     * Converts a 0..63 array index into a rank.
     *
     * @param index board array index
     * @return rank 0..7
     * @throws IllegalArgumentException if index is out of range
     */
    public int rankOfIndex(int index) {
        if (index < 0 || index >= 64) throw new IllegalArgumentException();
        return index / 8;
    }

    /**
     * Converts a 0..63 array index into a file.
     *
     * @param index board array index
     * @return file 0..7
     * @throws IllegalArgumentException if index is out of range
     */
    public int fileOfIndex(int index) {
        if (index < 0 || index >= 64) throw new IllegalArgumentException();
        return index % 8;
    }
}
