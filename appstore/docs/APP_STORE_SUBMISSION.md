# Apple Store Connect Submission Details

## App Information
**App Name:** Zaychess
**Version:** 1.1.3
**Subtitle:** Chess, Refined.

## Promotional Text
A tactile, minimalist AI chess app for macOS.

## Description
**Zaychess: A Love Letter to the Sixty-Four Squares.**

Forge your own path in the game of kings with Zaychess—a macOS-native chess application designed for those who value aesthetics, focus, and the tactile joy of movement. 

Escape the cluttered, data-heavy platforms of the past. Zaychess is built for the "Coffee Shop Player"—the design-conscious enthusiast who wants a premium, zen-like environment to play, learn, and relax.

### Why Zaychess?

**• Tactile & Playful Aesthetics**
Experience chess through a handcrafted visual language of bold geometries and a soothing Teal/Blue palette. Our interactive main menu isn't just a list of buttons—it’s a digital playground with physics-based pieces you can toss, drag, and flick.

**• The Serendipity Engine**
Challenge "Serendipity," a custom-built engine designed to play with human-like character. It doesn't just offer cold calculation; it offers a game that feels alive, winnable, and endlessly engaging.

**• Offline & Privacy-First**
Zaychess runs entirely on your Mac. No sign-ups, no tracking, and no internet required for local or AI play. Your data stays where it belongs: with you.

**• Seamless Social Play**
 prioritization of Local PvP ("Hot-Seat" mode) makes Zaychess the perfect companion for a shared afternoon at a cafe. Need an opponent? Our streamlined online matchmaking pairs you with players globally with zero setup.

### Features At A Glance
• **Pure Gameplay**: Smooth drag-and-drop physics with high-fidelity visual feedback.
• **Smart Assistance**: Legal-move highlights, check detection, and robust undo/redo.
• **Game Archiving**: Save and load games using standardized formats (PGN/FEN).
• **Modern Native UI**: A sleek, squircle-focused design that feels right at home on your Mac.
• **Online Matchmaking**: Jump into the action instantly via our dedicated relay server.

---

## What's New in Version 1.1.2
• **The Design Evolution**: A completely reimagined Main Menu featuring "Toy" physics and a refined Retro-Modern aesthetic.
• **Seamless Matchmaking**: A new, one-click online multiplayer system. No IPs, no hassle—just play.

## What's New in Version 1.1.3
• **Stability Overhaul**: Comprehensive stress testing has been performed on the "Serendipity" AI to ensure rock-solid performance across all difficulty levels (1-10).
• **Matchmaking Finalized**: Version 1.1.3 removes all legacy LAN/Direct-IP hosting code and entitlements, fully migrating to our streamlined online matchmaking system via relay.

• **Resume the Challenge**: Now you can load any saved game directly into a match against the Serendipity AI.

## Keywords
chess, minimalist, zen, design, cozy, macos, native, strategy, local pvp, board game, premium, engine

## Notes for App Review
The legacy Local/LAN hosting mode has been removed in this version. The app no longer requires the `com.apple.security.network.server` entitlement as it now exclusively uses a streamlined Online Matchmaking system via our relay server (acting only as a network client).

### Entitlements & Usage

**com.apple.security.files.user-selected.read-write**
Allows users to open and save chess game files (.chesslog) to a location they choose via the system file picker. Access is strictly limited to files successfully selected by the user for the purpose of archiving and resuming games.

**com.apple.security.network.client**
Enables online matchmaking and multiplayer gameplay. The app connects as a client to our dedicated Zaychess relay server to pair players and transmit chess move data. No direct peer-to-peer hosting or third-party network services are used.

**To test Multiplayer:**
1. Open two instances of the app (e.g., using Terminal: `open -n -a Zaychess`).
2. Click **"Multiplayer"** in both instances.
3. The app will automatically connect to the relay server and pair the two instances into a game. No manual IP entry or hosting configuration is required.
