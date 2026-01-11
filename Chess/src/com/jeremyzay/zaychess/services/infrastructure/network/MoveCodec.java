package com.jeremyzay.zaychess.services.infrastructure.network;

/**
 * Utility for encoding/decoding moves into a simple string wire format.
 *
 * Format examples:
 *   "MOVE|6,4|4,4|NORMAL"
 *   "MOVE|6,4|7,4|PROMOTION:QUEEN"
 */
public final class MoveCodec {
    private MoveCodec() {}

    /**
     * Encode a MoveMessage into a single line string.
     *
     * @param m the MoveMessage to encode
     * @return encoded string
     */
    public static String encode(MoveMessage m) {
        return "MOVE|" + m.fromRank() + "," + m.fromFile() +
               "|" + m.toRank() + "," + m.toFile() +
               "|" + (m.type() == null ? "NORMAL" : m.type());
    }

    /**
     * Try to decode a line into a MoveMessage.
     *
     * @param line the encoded string
     * @return decoded MoveMessage, or null if invalid
     */
    public static MoveMessage tryDecode(String line) {
        try {
            if (line == null || !line.startsWith("MOVE|")) return null;
            String[] parts = line.split("\\|");
            String[] a = parts[1].split(",");
            String[] b = parts[2].split(",");
            String type = parts.length > 3 ? parts[3] : "NORMAL";
            return new MoveMessage(
                Integer.parseInt(a[0]), Integer.parseInt(a[1]),
                Integer.parseInt(b[0]), Integer.parseInt(b[1]),
                type
            );
        } catch (Exception e) {
            return null;
        }
    }
}
