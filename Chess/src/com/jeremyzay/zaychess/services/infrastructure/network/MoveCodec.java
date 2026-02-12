package com.jeremyzay.zaychess.services.infrastructure.network;

/**
 * Utility for encoding/decoding moves into a simple string wire format.
 *
 * Format examples:
 * "MOVE|6,4|4,4|NORMAL"
 * "MOVE|6,4|7,4|PROMOTION:QUEEN"
 */
public final class MoveCodec {
    private MoveCodec() {
    }

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

    public static String encodeResign() {
        return "RESIGN";
    }

    public static String encodeOfferDraw() {
        return "OFFER_DRAW";
    }

    public static String encodeAcceptDraw() {
        return "ACCEPT_DRAW";
    }

    public static String encodeDeclineDraw() {
        return "DECLINE_DRAW";
    }

    public static String encodeOfferRematch() {
        return "OFFER_REMATCH";
    }

    public static String encodeAcceptRematch() {
        return "ACCEPT_REMATCH";
    }

    public static String encodeDeclineRematch() {
        return "DECLINE_REMATCH";
    }

    /**
     * Try to decode a line into a MoveMessage.
     *
     * @param line the encoded string
     * @return decoded MoveMessage, or null if invalid
     */
    public static MoveMessage tryDecode(String line) {
        try {
            if (line == null)
                return null;
            if (line.equals("RESIGN")) {
                // Special "move" indicating resignation
                return new MoveMessage(-1, -1, -1, -1, "RESIGN");
            }
            if (line.equals("OFFER_DRAW")) {
                return new MoveMessage(-1, -1, -1, -1, "OFFER_DRAW");
            }
            if (line.equals("ACCEPT_DRAW")) {
                return new MoveMessage(-1, -1, -1, -1, "ACCEPT_DRAW");
            }
            if (line.equals("DECLINE_DRAW")) {
                return new MoveMessage(-1, -1, -1, -1, "DECLINE_DRAW");
            }
            if (line.equals("OFFER_REMATCH")) {
                return new MoveMessage(-1, -1, -1, -1, "OFFER_REMATCH");
            }
            if (line.equals("ACCEPT_REMATCH")) {
                return new MoveMessage(-1, -1, -1, -1, "ACCEPT_REMATCH");
            }
            if (line.equals("DECLINE_REMATCH")) {
                return new MoveMessage(-1, -1, -1, -1, "DECLINE_REMATCH");
            }
            if (!line.startsWith("MOVE|"))
                return null;
            String[] parts = line.split("\\|");
            String[] a = parts[1].split(",");
            String[] b = parts[2].split(",");
            String type = parts.length > 3 ? parts[3] : "NORMAL";
            return new MoveMessage(
                    Integer.parseInt(a[0]), Integer.parseInt(a[1]),
                    Integer.parseInt(b[0]), Integer.parseInt(b[1]),
                    type);
        } catch (Exception e) {
            return null;
        }
    }
}
