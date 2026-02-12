package com.jeremyzay.zaychess.services.infrastructure.audio;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles playing sound effects and management of ambient background loops.
 */
public class SoundService {
    private static final String SFX_PATH = "/com/jeremyzay/zaychess/view/assets/sounds/sfx/";
    private static final String AMBIENCE_PATH = "/com/jeremyzay/zaychess/view/assets/sounds/ambience/";

    private static final ExecutorService soundPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "SoundPool-Thread");
        t.setDaemon(true);
        return t;
    });

    private static final Map<SFX, byte[]> sfxCache = new ConcurrentHashMap<>();
    private static final Map<String, Clip> ambienceClips = new ConcurrentHashMap<>();
    private static String currentAmbience = null;

    public enum SFX {
        MOVE("move.wav"),
        CAPTURE("capture.wav"),
        CHECK("check.wav"),
        CASTLE("castle.wav"),
        PROMOTE("promote.wav"),
        GAME_OVER("game_over.wav"),
        UI_CLICK("ui_click.wav");

        private final String filename;

        SFX(String filename) {
            this.filename = filename;
        }

        public String getPath() {
            return SFX_PATH + filename;
        }
    }

    public enum Ambience {
        RAINY_CAFE("rainy_cafe.wav"),
        LATE_NIGHT_VINYL("vinyl.wav"),
        LIBRARY("library.wav");

        private final String filename;

        Ambience(String filename) {
            this.filename = filename;
        }

        public String getPath() {
            return AMBIENCE_PATH + filename;
        }
    }

    static {
        // Pre-cache SFX to avoid I/O blocking
        for (SFX sfx : SFX.values()) {
            try (InputStream is = SoundService.class.getResourceAsStream(sfx.getPath())) {
                if (is != null) {
                    sfxCache.put(sfx, is.readAllBytes());
                }
            } catch (Exception e) {
                System.err.println("[SoundService] Failed to cache: " + sfx.name());
            }
        }
        System.out.println("[SoundService] Cached " + sfxCache.size() + " sound effects.");
    }

    /**
     * Plays a short sound effect from memory.
     */
    public static void play(SFX sfx) {
        soundPool.execute(() -> {
            byte[] data = sfxCache.get(sfx);
            if (data == null)
                return;

            // Use the Java AudioSystem to pick the best mixer/device
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
                AudioFormat format = ais.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);

                // Bluetooth on Mac/Java often fails if we don't handle the Mixer specifically,
                // but getClip() is the standard way to get the default system output.
                Clip clip = AudioSystem.getClip();
                clip.open(ais);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(0.0f);
                }

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        try {
                            clip.close();
                        } catch (Exception ignored) {
                        }
                    }
                });

                clip.start();
            } catch (Exception e) {
                System.err.println("[SoundService] Playback error: " + e.getMessage());
            }
        });
    }

    public static void startAmbience(Ambience type) {
        if (type.name().equals(currentAmbience))
            return;
        stopAmbience();

        soundPool.execute(() -> {
            try (InputStream is = SoundService.class.getResourceAsStream(type.getPath())) {
                if (is == null)
                    return;
                AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.loop(Clip.LOOP_CONTINUOUSLY);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    ((FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(-15.0f);
                }

                clip.start();
                ambienceClips.put(type.name(), clip);
                currentAmbience = type.name();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void stopAmbience() {
        if (currentAmbience != null) {
            Clip clip = ambienceClips.remove(currentAmbience);
            if (clip != null) {
                try {
                    clip.stop();
                    clip.close();
                } catch (Exception ignored) {
                }
            }
            currentAmbience = null;
        }
    }
}
