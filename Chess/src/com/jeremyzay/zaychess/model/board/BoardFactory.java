package com.jeremyzay.zaychess.model.board;

import com.jeremyzay.zaychess.model.pieces.Bishop;
import com.jeremyzay.zaychess.model.pieces.King;
import com.jeremyzay.zaychess.model.pieces.Knight;
import com.jeremyzay.zaychess.model.pieces.Pawn;
import com.jeremyzay.zaychess.model.pieces.Queen;
import com.jeremyzay.zaychess.model.pieces.Rook;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;

/**
 * Utility class for creating Board instances in standard configuration.
 */
public final class BoardFactory {
    private BoardFactory() {
        // Prevent instantiation
    }

    /**
     * Creates a new Board set up in the standard chess starting position.
     *
     * @return a Board with all pieces placed in their initial squares
     */
    public static Board createDefaultBoard() {
        Board board = new Board();

        // Black back rank (rank 0)
        board.setPieceAt(0, 0, new Rook(PlayerColor.BLACK, new Position(0,0)));
        board.setPieceAt(0, 1, new Knight(PlayerColor.BLACK, new Position(0,1)));
        board.setPieceAt(0, 2, new Bishop(PlayerColor.BLACK, new Position(0,2)));
        board.setPieceAt(0, 3, new Queen(PlayerColor.BLACK, new Position(0,3)));
        board.setPieceAt(0, 4, new King(PlayerColor.BLACK, new Position(0,4)));
        board.setPieceAt(0, 5, new Bishop(PlayerColor.BLACK, new Position(0,5)));
        board.setPieceAt(0, 6, new Knight(PlayerColor.BLACK, new Position(0,6)));
        board.setPieceAt(0, 7, new Rook(PlayerColor.BLACK, new Position(0,7)));
        // Black pawns (rank 1)
        for (int file = 0; file < 8; file++) {
            board.setPieceAt(1, file, new Pawn(PlayerColor.BLACK, new Position(1,file)));
        }

        // White pawns (rank 6)
        for (int file = 0; file < 8; file++) {
            board.setPieceAt(6, file, new Pawn(PlayerColor.WHITE, new Position(6, file)));
        }
        // White back rank (rank 7)
        board.setPieceAt(7, 0, new Rook(PlayerColor.WHITE, new Position(7,0)));
        board.setPieceAt(7, 1, new Knight(PlayerColor.WHITE, new Position(7,1)));
        board.setPieceAt(7, 2, new Bishop(PlayerColor.WHITE, new Position(7,2)));
        board.setPieceAt(7, 3, new Queen(PlayerColor.WHITE, new Position(7,3)));
        board.setPieceAt(7, 4, new King(PlayerColor.WHITE, new Position(7,4)));
        board.setPieceAt(7, 5, new Bishop(PlayerColor.WHITE, new Position(7,5)));
        board.setPieceAt(7, 6, new Knight(PlayerColor.WHITE, new Position(7,6)));
        board.setPieceAt(7, 7, new Rook(PlayerColor.WHITE, new Position(7,7)));

        return board;
    }
}
