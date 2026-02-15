package com.jeremyzay.zaychess.tests;

import com.jeremyzay.zaychess.services.infrastructure.engine.SerendipityEngineService;
import java.util.Arrays;
import java.util.List;

public class ReproducePassiveBug {
    public static void main(String[] args) {
        System.out.println("Debugging Passive AI move selection...");
        try (SerendipityEngineService engine = new SerendipityEngineService()) {
            engine.start();
            engine.setDifficulty(1); // Or 0? GameController handles logic.
            // Here we want to test bestMove(List) behavior.

            // Recreate the position:
            // 1. e4 Nc6
            // 2. d3 Rb8
            // 3. d4 Ra8
            // 4. d5 (Black to move)

            // FEN after 4. d5:
            // r1bqkbnr/pppppppp/2n5/3P4/4P3/8/PPP2PPP/RNBQKBNR b KQkq - 0 4
            String fen = "r1bqkbnr/pppppppp/2n5/3P4/4P3/8/PPP2PPP/RNBQKBNR b KQkq - 0 4";
            engine.setPositionFEN(fen);

            // Candidates (Quiet moves)
            // Knight moves: b8, a5, e5
            // Rook moves: b8
            List<String> candidates = Arrays.asList("c6b8", "c6a5", "c6e5", "a8b8"); // c6b8 (Nb8)

            System.out.println("Testing candidates: " + candidates);

            // We need to see the scores.
            // Since bestMove(List) returns just the string, we can't see scores without
            // modifying code.
            // But we can observe which one it picks.

            String best = engine.bestMove(candidates);
            System.out.println("Engine selected: " + best);

            if ("a8b8".equals(best)) {
                System.out.println("FAILURE: Engine picked the blunder Rb8!");
            } else {
                System.out.println("SUCCESS: Engine picked " + best);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
