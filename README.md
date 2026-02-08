# Zaychess

A Java-based Chess application with local, AI, and online multiplayer modes.

## 🚀 Features

*   **Local PvP**: Play hot-seat chess on a single computer.
*   **Play vs Computer**: Challenge the integrated **Serendipity** chess engine.
*   **Online Multiplayer**: Host or join games over a network.
    *   **Direct Connect**: Host a TCP server and let a friend connect via IP.
    *   **Online Matchmaking**: Connect to a central Relay Server for automatic matchmaking.
*   **Save/Load**: Save your game state and resume later.
*   **Move History**: view and navigate through past moves.

## 📁 Project Structure

The project is organized into a standard MVC architecture:

*   **`src/com/jeremyzay/zaychess`**
    *   **`App.java`**: Application entry point.
    *   **`model/`**: Core chess logic (Board, Pieces, Rules, Move generation).
    *   **`view/`**: Swing GUI components (`ChessFrame`, `BoardPanel`, `MainMenuFrame`).
    *   **`controller/`**: Game logic coordination (`GameController`, `GameLauncher`).
    *   **`services/`**: Infrastructure services.
        *   **`infrastructure/network/`**: Networking logic (`TcpHost`, `TcpClient`, `RelayClient`).
        *   **`infrastructure/engine/`**: UCI Engine integration (`SerendipityEngineService`).

*   **`relay-server/`**: A Node.js based relay server for matchmaking.

## 🛠️ Build & Run

### Prerequisites
*   **Java JDK 17+**
*   **Node.js** (for Relay Server)

### compiling
```bash
javac -d bin -sourcepath Chess/src -cp Chess/engines/Serendipity.jar Chess/src/com/jeremyzay/zaychess/App.java
```

### Running the App
```bash
java --add-modules=jdk.incubator.vector -cp bin:Chess/engines/Serendipity.jar com.jeremyzay.zaychess.App
```
*Note: If you are missing assets, append `Chess/src` to the classpath:*
`java --add-modules=jdk.incubator.vector -cp bin:Chess/src:Chess/engines/Serendipity.jar com.jeremyzay.zaychess.App`


### Running the Relay Server (for Online Matchmaking)
The authentication server is a simple Node.js script.

```bash
node relay-server/server.js
```
*Runs on port 8080 by default.*

## 🌐 Multiplayer Guide

### Method 1: Relay Server (Recommended)
1.  Start the relay server (`node relay-server/server.js`).
2.  Launch two instances of the app.
3.  Click **"Online Matchmaking"** on both.
4.  The server automatically pairs you up!

### Method 2: Direct TCP
1.  **Host**: Click "Host Multiplayer". Wait for opponent.
2.  **Client**: Click "Join Multiplayer", enter Host's IP address.

## 🚧 Status & Roadmap

### Implemented
*   ✅ Full Chess Rules (Castling, En Passant, Promotion)
*   ✅ GUI with Drag & Drop / Click-Click
*   ✅ Basic AI integration
*   ✅ Local Save/Load system
*   ✅ TCP Networking
*   ✅ Relay Server Matchmaking (Beta)

### Working On
*   🛠 **Linux Server Deployment**: Dockerizing the relay server.
*   🛠 **Robustness**: Handling disconnects and server crashes gracefully.


todo: update save game. also update save game to work with ai
have board switch sides when black is user's turn DONE
improve launcher to not have host multiplayer and join multiplayer DONE
add visuals for what move just occurred
finish multiplayer mode
increase size of popup choosing side against ai
improve visuals of when you or the opponent is in check using colors of the board tiles
improve visuals of when you or the opponent is in checkmate using colors of the board tiles
add undo move against ai
add scaled difficulty ai