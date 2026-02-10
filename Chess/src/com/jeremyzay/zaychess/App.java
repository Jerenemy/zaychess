package com.jeremyzay.zaychess;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.jeremyzay.zaychess.view.gui.MainFrame;
import com.jeremyzay.zaychess.view.gui.ResourceLoader;

public class App {
    /**
     * Entry point for the Chess application.
     */

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        try {
            SwingUtilities.invokeLater(() -> {
                // Initialize MainFrame immediately to capture instance?
                // The loading dialog logic is fine.

                JDialog loadingDialog = new JDialog((JFrame) null, "Lade Schach...", true);
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                loadingDialog.add(progressBar);
                loadingDialog.setSize(300, 75);
                loadingDialog.setLocationRelativeTo(null);
                loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

                new Thread(() -> {
                    ResourceLoader.preload();
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        new MainFrame().setVisible(true);
                    });
                }).start();

                loadingDialog.setVisible(true);
            });
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.toString(), "Startup error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
