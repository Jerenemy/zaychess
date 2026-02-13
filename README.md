# Zaychess

A Java-based Chess application with local, AI, and online multiplayer modes.

## 🚀 Features

*   **Local PvP**: Play hot-seat chess on a single computer. Board flips between turns.
*   **Play vs Computer**: Challenge the integrated **Serendipity** chess engine.
*   **Online Multiplayer**: Connect to the central Relay Server for automatic matchmaking.
*   **Save/Load**: Save your game state and resume later.
*   **Move History**: View and navigate through past moves with undo/redo.
*   **Drag-and-Drop Gameplay**: Smooth piece movement with visual feedback and snap-back animations.
*   **Smart Selection**: Click-to-move or drag-to-move, with intelligent highlight clearing.
*   **Load Game vs AI**: Resume saved games against the engine.
*   **Robust Undo/Redo**: Fully supported in Local and AI modes (undoes both moves in AI games).
*   **UCI Save Format**: Standardized game saving compatible with chess tools.
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

- [x] Undo move against AI
- [x] Drag-and-Drop Support
- [ ] Scaled difficulty AI
- [x] Captured pieces display
- [x] Improve UI of captured pieces panel
- [x] Modern UI with unified "squircle" buttons
- [x] Fix local game to AI loading screen bug
- [x] Close button on Game Over dialog
- [x] Sound effects
- [x] Fix sound effects on bluetooth
- [x] Make 2 new ai difficulties: 
    - [x] half passive, half aggressive
    - [x] 1/3 passive, 1/3 normal, 1/3 aggressive
- [ ] add settings where you can toggle sound on and off (or just put as button on home screen)
- [ ] add button to flip board in local mode
    - [ ] make sure it works with undo
    - [ ] animate the flip
- [ ] add sound elements to playing with home screen toy pieces
- [ ] add ai arena mode to watch ai's of different difficulties play each other
- [ ] Clock functionality
- [ ] Theme selection UI