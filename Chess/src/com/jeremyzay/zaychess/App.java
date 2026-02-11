package com.jeremyzay.zaychess;

import javax.swing.SwingUtilities;

import com.jeremyzay.zaychess.view.gui.MainFrame;

public class App {
    /**
     * Entry point for the Chess application.
     */

    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SwingUtilities.invokeLater(() -> {
                new MainFrame().setVisible(true);
            });
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, e.toString(), "Startup error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
