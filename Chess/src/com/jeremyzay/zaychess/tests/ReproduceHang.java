package com.jeremyzay.zaychess.tests;

import com.jeremyzay.zaychess.services.infrastructure.engine.SerendipityEngineService;

public class ReproduceHang {
    public static void main(String[] args) {
        System.out.println("Starting engine hang reproduction test...");

        String[] fens = {
                "rnbqkbnr/pp1ppppp/2p5/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq d3 0 1"
        };

        try {
            SerendipityEngineService service = new SerendipityEngineService();
            service.start();
            service.setDifficulty(1); // Aggressive mode -> go depth 2

            for (String fen : fens) {
                System.out.println("Testing FEN: " + fen);
                service.setPositionFEN(fen);
                long start = System.currentTimeMillis();
                try {
                    String move = service.bestMove();
                    long duration = System.currentTimeMillis() - start;
                    System.out.println("Move found: " + move + " in " + duration + "ms");
                } catch (Exception e) {
                    System.err.println("Failed on FEN: " + fen);
                    e.printStackTrace();
                    // print last output via UciClient? Not exposed here.
                }
            }

            service.close();
            System.out.println("Test complete.");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
