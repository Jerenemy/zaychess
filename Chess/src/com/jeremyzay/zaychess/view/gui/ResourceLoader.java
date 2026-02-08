package com.jeremyzay.zaychess.view.gui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.jeremyzay.zaychess.model.pieces.Piece;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.awt.Image;

public final class ResourceLoader {
    private static final String MAIN_MENU_BG_PATH = "/com/jeremyzay/zaychess/view/assets/main_menu_background.png";

    public static BufferedImage MAIN_MENU_BACKGROUND;

    public static synchronized void preload() {
        if (MAIN_MENU_BACKGROUND != null)
            return;
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
        return "/com/jeremyzay/zaychess/view/assets/pieces/"
                + p.getColor().toString().toLowerCase() + "_" + p.getSymbol() + ".png";
    }

    private static ImageIcon loadIcon(String absoluteClasspathPath) {
        URL url = ResourceLoader.class.getResource(absoluteClasspathPath);
        if (url == null)
            throw new IllegalStateException("Missing resource: " + absoluteClasspathPath);
        return new ImageIcon(url);
    }

    public static ImageIcon getPieceIcon(Piece p) {
        String key = piecePath(p);
        return ICON_CACHE.computeIfAbsent(key, ResourceLoader::loadIcon);
    }

    private static final Map<String, javax.swing.Icon> SCALED_ICON_CACHE = new ConcurrentHashMap<>();

    public static javax.swing.Icon getPieceIcon(Piece p, int size) {
        String baseKey = piecePath(p);
        String key = baseKey + "#" + size;
        return SCALED_ICON_CACHE.computeIfAbsent(key, k -> {
            ImageIcon base = ICON_CACHE.computeIfAbsent(baseKey, ResourceLoader::loadIcon);
            return new HiDPIIcon(base.getImage(), size, size);
        });
    }

    /**
     * Custom Icon that paints the original high-res image scaled down,
     * ensuring high quality on HiDPI displays.
     */
    private static class HiDPIIcon implements javax.swing.Icon {
        private final Image original;
        private final int width;
        private final int height;

        public HiDPIIcon(Image original, int width, int height) {
            this.original = original;
            this.width = width;
            this.height = height;
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }

        @Override
        public synchronized void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                    java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            g2.drawImage(original, x, y, width, height, c);
            g2.dispose();
        }
    }

    private ResourceLoader() {
    }
}
