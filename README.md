# Zaychess

A Java-based Chess application with local, AI, and online multiplayer modes.

## 🚀 Features

*   **Local PvP**: Play hot-seat chess on a single computer. Board flips between turns.
*   **Play vs Computer**: Challenge the integrated **Serendipity** chess engine.
*   **Online Multiplayer**: Connect to the central Relay Server for automatic matchmaking.
*   **Save/Load**: Save your game state and resume later.
*   **Move History**: View and navigate through past moves with undo/redo.
*   **Visual Highlights**:
    *   Last move highlighting (yellow)
    *   Check highlighting (red)
    *   Checkmate highlighting (dark red)
    *   Legal move indicators on piece selection
*   **Custom Game Over Dialog**: Styled dialog with Rematch and Return to Menu options.
*   **Themeable Board**: Customizable colors via `BoardTheme.java`.

## 📁 Project Structure

*   **`Chess/src/com/jeremyzay/zaychess`**: Main application code (MVC architecture).
*   **`relay-server/`**: Node.js relay server for online matchmaking.

## 🛠️ Build & Run

### Prerequisites
*   **Java JDK 17+**
*   **Node.js** (for Relay Server)

### Building
```bash
./build.sh
```

### Running the App
```bash
./run.sh
```

Or manually:
```bash
java --add-modules=jdk.incubator.vector -cp bin:Chess/engines/Serendipity.jar com/jeremyzay/zaychess/App
```

### Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `ZAYCHESS_RELAY_SERVER` | Override the relay server IP | `172.104.31.166` |

## 🖥️ Server Management (pm2)

The relay server runs on a Linux VPS. Use **pm2** to manage it.

### Start the Server
```bash
pm2 start chess/zaychess/relay-server/server.js --name zaychess-relay
```

### Other pm2 Commands
```bash
pm2 status              # Check if running
pm2 logs zaychess-relay # View logs
pm2 restart zaychess-relay
pm2 stop zaychess-relay
```

### Auto-Start on Reboot
```bash
pm2 startup
# Run the command it outputs, then:
pm2 save
```

### Firewall
Ensure port 8080 is open:
```bash
sudo ufw allow 8080/tcp
```

## 🌐 Multiplayer Guide

1.  Ensure the relay server is running (`pm2 status`).
2.  Launch the app and click **"Online Matchmaking"**.
3.  Wait for another player to connect – the server pairs you automatically.

## 🚧 Roadmap

- [ ] Undo move against AI
- [ ] Scaled difficulty AI
- [ ] Captured pieces display
- [ ] Clock functionality
- [ ] Theme selection UI