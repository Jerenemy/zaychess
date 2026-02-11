
import java.io.*;
import java.util.*;

public class ReproduceTimeout {
    public static void main(String[] args) throws Exception {
        String jarPath = "Chess/engines/Serendipity.jar";
        String fen = "r1b2rk1/pp3ppp/8/2bpn3/3N2nq/2P2PP1/PP1Q3P/RNB1KB1R b KQ - 0 1";

        System.out.println("Launching engine...");
        ProcessBuilder pb = new ProcessBuilder("java", "--add-modules=jdk.incubator.vector", "-jar", jarPath);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        sendCommand(writer, "uci");
        waitFor(reader, "uciok");

        sendCommand(writer, "isready");
        waitFor(reader, "readyok");

        sendCommand(writer, "position fen " + fen);
        sendCommand(writer, "go depth 2");

        System.out.println("Waiting for bestmove...");
        long start = System.currentTimeMillis();
        boolean found = false;
        while (System.currentTimeMillis() - start < 15000) {
            if (reader.ready()) {
                String line = reader.readLine();
                System.out.println("[Engine] " + line);
                if (line.startsWith("bestmove")) {
                    found = true;
                    break;
                }
            }
            Thread.sleep(100);
        }

        if (found) {
            System.out.println("Success!");
        } else {
            System.out.println("FAILED: bestmove not received in 15s");
        }

        sendCommand(writer, "quit");
        proc.destroy();
    }

    private static void sendCommand(BufferedWriter w, String cmd) throws IOException {
        System.out.println("[Sent] " + cmd);
        w.write(cmd + "\n");
        w.flush();
    }

    private static void waitFor(BufferedReader r, String token) throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            System.out.println("[Engine] " + line);
            if (line.contains(token))
                return;
        }
    }
}
