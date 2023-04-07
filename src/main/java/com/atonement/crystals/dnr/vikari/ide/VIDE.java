package com.atonement.crystals.dnr.vikari.ide;

import com.atonement.crystals.dnr.vikari.ide.gui.VideMainWindow;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class VIDE {
    public static void main(String[] args) {
        setAppleLookAndFeel();
        javax.swing.SwingUtilities.invokeLater(() -> {
                VideMainWindow videMainWindow = new VideMainWindow();
                videMainWindow.start();
            });
    }

    private static void setAppleLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "VIDE");
        System.setProperty("apple.awt.application.name", "VIDE");
    }
}
