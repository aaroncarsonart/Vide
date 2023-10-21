package com.atonementcrystals.dnr.vikari.ide;

import com.atonementcrystals.dnr.vikari.ide.gui.VideMainWindow;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Vide {
    public static void main(String[] args) {
        setAppleLookAndFeel();

        // TODO: Rewrite Vide such that multiple windows delegate functionality between them without
        //        unnecessarily duplicating instances of menus and other program-wide settings.

        if (args.length == 0) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                VideMainWindow videMainWindow = new VideMainWindow(null);
                videMainWindow.start();
            });
        } else for (String filename : args) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                VideMainWindow videMainWindow = new VideMainWindow(filename);
                videMainWindow.start();
            });
        }
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
