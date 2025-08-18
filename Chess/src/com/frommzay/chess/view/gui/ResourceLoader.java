package com.frommzay.chess.view.gui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.frommzay.chess.model.pieces.Piece;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.awt.Image;

public final class ResourceLoader {
    private static final String MAIN_MENU_BG_PATH =
        "/com/frommzay/chess/view/assets/main_menu_background.jpg";

    public static BufferedImage MAIN_MENU_BACKGROUND;

    public static synchronized void preload() {
        if (MAIN_MENU_BACKGROUND != null) return;
        try (InputStream in = ResourceLoader.class.getResourceAsStream(MAIN_MENU_BG_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found: " + MAIN_MENU_BG_PATH);
            }
            MAIN_MENU_BACKGROUND = ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + MAIN_MENU_BG_PATH, e);
        }
    }
    
    private static final Map<String, ImageIcon> ICON_CACHE = new ConcurrentHashMap<>();

    private static String piecePath(Piece p) {
        return "/com/frommzay/chess/view/assets/pieces/"
                + p.getColor().toString().toLowerCase() + "_" + p.getSymbol() + ".png";
    }

    private static ImageIcon loadIcon(String absoluteClasspathPath) {
        URL url = ResourceLoader.class.getResource(absoluteClasspathPath);
        if (url == null) throw new IllegalStateException("Missing resource: " + absoluteClasspathPath);
        return new ImageIcon(url);
    }

    public static ImageIcon getPieceIcon(Piece p) {
        String key = piecePath(p);
        return ICON_CACHE.computeIfAbsent(key, ResourceLoader::loadIcon);
    }

    private static final Map<String, ImageIcon> SCALED_ICON_CACHE = new ConcurrentHashMap<>();

    public static ImageIcon getPieceIcon(Piece p, int size) {
        String baseKey = piecePath(p);
        String key = baseKey + "#" + size;
        return SCALED_ICON_CACHE.computeIfAbsent(key, k -> {
            ImageIcon base = ICON_CACHE.computeIfAbsent(baseKey, ResourceLoader::loadIcon);
            Image scaled = base.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        });
    }

    private ResourceLoader() {}
}
