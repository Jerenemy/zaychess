package com.frommzay.chess;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.frommzay.chess.view.gui.MainMenuFrame;
import com.frommzay.chess.view.gui.ResourceLoader;

public class App {
	/**
	 * Entry point for the Chess application.
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JDialog loadingDialog = new JDialog((JFrame) null, "Lade Schach...", true);
			JProgressBar progressBar = new JProgressBar();
			progressBar.setIndeterminate(true);
			loadingDialog.add(progressBar);
			loadingDialog.setSize(300, 75);
			loadingDialog.setLocationRelativeTo(null);
			loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

			new Thread(() -> {
				ResourceLoader.preload();
				loadingDialog.dispose();
				SwingUtilities.invokeLater(() -> new MainMenuFrame().setVisible(true));
			}).start();

			loadingDialog.setVisible(true);
		});
	}
}
