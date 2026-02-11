package com.jeremyzay.zaychess.tests;

import com.jeremyzay.zaychess.services.infrastructure.engine.SerendipityEngineService;

public class ReproduceEngineFreeze {
    public static void main(String[] args) {
        System.out.println("Starting engine freeze reproduction test...");

        for (int i = 0; i < 5; i++) {
            System.out.println("Iteration " + (i + 1));
            SerendipityEngineService service = new SerendipityEngineService();
            try {
                System.out.println("  Starting service...");
                service.start();
                System.out.println("  Service started. Thinking for 1s...");
                Thread.sleep(1000);

                System.out.println("  Stopping service...");
                long startStop = System.currentTimeMillis();
                service.close();
                long endStop = System.currentTimeMillis();
                System.out.println("  Service stopped in " + (endStop - startStop) + "ms.");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Test complete.");
        System.exit(0);
    }
}
