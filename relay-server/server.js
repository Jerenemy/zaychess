const net = require('net');

const PORT = 8080;

// Enums / Constants
const MSG_JOIN_QUEUE = "JOIN_QUEUE";
const MSG_MATCH_FOUND = "MATCH_FOUND";
const MSG_MOVE = "MOVE";
const MSG_OPPONENT_LEFT = "OPPONENT_LEFT";

// State
let waitingClient = null; // The socket waiting for a match

// Helper to send JSON messages
function send(socket, type, data = {}) {
    if (socket && !socket.destroyed) {
        const payload = JSON.stringify({ type, ...data }) + "\n";
        socket.write(payload);
    }
}

const server = net.createServer((socket) => {
    console.log(`[${socket.remoteAddress}:${socket.remotePort}] Connected`);

    // Attach state to socket object for convenience
    socket.opponent = null;
    socket.isWaiting = false;

    // Buffer for handling split packets
    let buffer = "";

    socket.on('data', (chunk) => {
        buffer += chunk.toString();

        // Process line-by-line
        let lineEndIndex;
        while ((lineEndIndex = buffer.indexOf('\n')) !== -1) {
            const line = buffer.substring(0, lineEndIndex).trim();
            buffer = buffer.substring(lineEndIndex + 1);

            if (line.length > 0) {
                handleMessage(socket, line);
            }
        }
    });

    socket.on('end', () => handleDisconnect(socket));
    socket.on('error', (err) => {
        console.error(`[${socket.remoteAddress}] Error: ${err.message}`);
        handleDisconnect(socket);
    });
});

function handleMessage(socket, line) {
    try {
        console.log(`[${socket.remoteAddress}] Received: ${line}`);
        
        // Basic protocol: Expect JSON or simple commands
        // For robustness, try parsing JSON first.
        // If it's just a raw Move string from the old protocol, implementation might differ,
        // but our plan specifies a NEW JSON protocol.
        
        let msg;
        try {
            msg = JSON.parse(line);
        } catch (e) {
            console.warn("Invalid JSON received");
            return;
        }

        switch (msg.type) {
            case MSG_JOIN_QUEUE:
                handleJoinQueue(socket);
                break;
            case MSG_MOVE:
                if (socket.opponent) {
                    // Forward move to opponent
                    // The client expects "MOVE" data to be the actual game protocol string
                    send(socket.opponent, MSG_MOVE, { data: msg.data });
                }
                break;
            default:
                console.warn(`Unknown message type: ${msg.type}`);
        }

    } catch (err) {
        console.error("Error processing message:", err);
    }
}

function handleJoinQueue(socket) {
    if (socket.opponent) return; // Already in a game
    if (socket.isWaiting) return; // Already waiting

    if (waitingClient && waitingClient !== socket) {
        // MATCH FOUND!
        const opponent = waitingClient;
        waitingClient = null;

        // Link them
        socket.opponent = opponent;
        opponent.opponent = socket;
        
        socket.isWaiting = false;
        opponent.isWaiting = false;

        console.log(`Match created: ${socket.remoteAddress} vs ${opponent.remoteAddress}`);

        // Notify both - determine colors randomly or first-come-first-serve?
        // Let's say the waiting player (opponent) is White (Host logic equivalent)
        // and the joining player (socket) is Black (Client logic equivalent)
        
        // Send "MATCH_FOUND" with "role": "HOST" or "CLIENT" to simplify client logic
        // Zaychess currently has Host (White) and Client (Black) modes.
        
        send(opponent, MSG_MATCH_FOUND, { role: "HOST" });
        send(socket, MSG_MATCH_FOUND, { role: "CLIENT" });

    } else {
        // No one waiting, add to queue
        waitingClient = socket;
        socket.isWaiting = true;
        console.log(`User ${socket.remoteAddress} joined queue.`);
    }
}

function handleDisconnect(socket) {
    console.log(`[${socket.remoteAddress}] Disconnected`);

    // If waiting, remove from queue
    if (waitingClient === socket) {
        waitingClient = null;
    }

    // If in game, notify opponent
    if (socket.opponent) {
        send(socket.opponent, MSG_OPPONENT_LEFT);
        socket.opponent.opponent = null; // Unlink
        socket.opponent = null;
    }
}

server.listen(PORT, () => {
    console.log(`Zaychess Relay Server listening on port ${PORT}`);
});
