package com.jeremyzay.zaychess;

import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveGenerator;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.pieces.*;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.services.application.notation.NotationFEN;
import com.jeremyzay.zaychess.services.infrastructure.engine.SerendipityEngineService;
import java.util.List;

public class ReplicateAggressionIssue {
    public static void main(String[] args) throws Exception {
        System.out.println("--- REPRODUCTION TEST START ---");

        // Target: Black piece at e4, White Queen at f3.
        // e4 is file 4, rank 4 (rank index 4 if 0 is top).
        // f3 is file 5, rank 5.

        GameState gs = new GameState();
        Board b = gs.getBoard();
        // Clear board
        for (int r = 0; r < 8; r++)
            for (int f = 0; f < 8; f++)
                b.setPieceAt(r, f, null);

        b.setPieceAt(4, 4, new Pawn(PlayerColor.BLACK, new Position(4, 4))); // e4
        b.setPieceAt(5, 5, new Queen(PlayerColor.WHITE, new Position(5, 5))); // f3

        // Add kings to avoid illegal state if needed (though MoveGenerator might not
        // care for pseudo-legal)
        b.setPieceAt(0, 4, new King(PlayerColor.BLACK, new Position(0, 4)));
        b.setPieceAt(7, 4, new King(PlayerColor.WHITE, new Position(7, 4)));

        if (gs.getTurn() == PlayerColor.WHITE)
            gs.changeTurn();

        System.out.println("FEN: " + NotationFEN.toFEN(gs));

        List<Move> allMoves = MoveGenerator.generateAllLegalMovesInTurn(gs);
        List<Move> captures = allMoves.stream()
                .filter(m -> m.getMoveType() == MoveType.CAPTURE
                        || m.getMoveType() == MoveType.EN_PASSANT
                        || (m.getMoveType() == MoveType.PROMOTION && gs.getPieceAt(m.getToPos()) != null))
                .toList();

        System.out.println("Total moves: " + allMoves.size());
        System.out.println("Captures identified: " + captures.size());
        for (Move m : captures) {
            System.out.println("Found capture: " + formatUci(m) + " (" + m.getMoveType() + ")");
        }

        if (captures.isEmpty()) {
            System.out.println("FAILURE: No captures found in model logic!");
            return;
        }

        System.out.println("Starting engine...");
        SerendipityEngineService engine = new SerendipityEngineService();
        engine.start();
        engine.setDifficulty(1); // Super Aggressive

        engine.setPositionFEN(NotationFEN.toFEN(gs));

        List<String> ucis = captures.stream().map(ReplicateAggressionIssue::formatUci).toList();
        System.out.println("Filters (searchmoves): " + ucis);

        String best = engine.bestMove(ucis);
        System.out.println("RESULT: Engine chose " + best);

        engine.close();
        System.out.println("--- TEST COMPLETE ---");
    }

    private static String formatUci(Move m) {
        char f1 = (char) ('a' + m.getFromPos().getFile());
        int r1 = 8 - m.getFromPos().getRank();
        char f2 = (char) ('a' + m.getToPos().getFile());
        int r2 = 8 - m.getToPos().getRank();
        return "" + f1 + r1 + f2 + r2;
    }
}
