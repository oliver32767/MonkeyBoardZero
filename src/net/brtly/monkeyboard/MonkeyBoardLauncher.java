package net.brtly.monkeyboard;

import java.awt.EventQueue;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class MonkeyBoardLauncher {
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
		    System.setProperty("apple.laf.useScreenMenuBar", "true");
		    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MonkeyBoard");
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(ClassNotFoundException e) {
	        System.out.println("ClassNotFoundException: " + e.getMessage());
		}
		catch(InstantiationException e) {
	        System.out.println("InstantiationException: " + e.getMessage());
		}
		catch(IllegalAccessException e) {
	        System.out.println("IllegalAccessException: " + e.getMessage());
		}
		catch(UnsupportedLookAndFeelException e) {
	        System.out.println("UnsupportedLookAndFeelException: " + e.getMessage());
		}
		
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MonkeyBoard window = new MonkeyBoard();
					window.frmMonkeyboard.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
