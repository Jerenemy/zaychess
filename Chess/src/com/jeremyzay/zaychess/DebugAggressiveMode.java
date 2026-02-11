package com.jeremyzay.zaychess;

import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveGenerator;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.services.application.notation.NotationFEN;
import java.util.List;

public class DebugAggressiveMode {
    public static void main(String[] args) {
        String fen = "r1bqk2r/pp2nppp/2n5/3pN3/3Pp3/P1P2Q2/2P2PPP/R1B1KB1R b KQkq - 0 1";
        GameState snap = NotationFEN.fromFEN(fen);

        List<Move> allMoves = MoveGenerator.generateAllLegalMovesInTurn(snap);
        List<Move> captures = allMoves.stream()
                .filter(m -> m.getMoveType() == MoveType.CAPTURE
                        || m.getMoveType() == MoveType.EN_PASSANT
                        || (m.getMoveType() == MoveType.PROMOTION && snap.getPieceAt(m.getToPos()) != null))
                .toList();

        System.out.println("FEN: " + fen);
        System.out.println("Total moves: " + allMoves.size());
        System.out.println("Captures found: " + captures.size());
        for (Move m : captures) {
            System.out.println("Capture: " + m.getFromPos() + " -> " + m.getToPos() + " (" + m.getMoveType() + ")");
        }

        // Check specifically for e4f3
        boolean foundE4F3 = captures.stream()
                .anyMatch(m -> m.getFromPos().getRank() == 4 && m.getFromPos().getFile() == 4 && // e4
                // is
                // (4,4)?
                // no,
                // rank
                // is
                // 0-indexed.
                // UCI e4 is file 4 (e), rank 3 (4 is '4'-'1'=3).
                // Wait, GameController uses rank 0 = top = rank 8? RANK0_IS_BOTTOM = false.
                // rank 1 = rank 7.
                // file 'e' = 4.
                // rank '4' = 7 - 3 = 4. (if rank0 is bottom, it would be 3).
                // Let's just check the formatted square.
                        false // I'll use a safer check below
                );
    }
}
