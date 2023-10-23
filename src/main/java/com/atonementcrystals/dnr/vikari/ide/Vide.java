package com.atonementcrystals.dnr.vikari.ide;

import com.atonementcrystals.dnr.vikari.ide.gui.VideEditorWindow;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

/**
 * Main class of the Vikari IDE.
 */
public class Vide {

    /**
     * Entry point function of program.
     * @param args A list of filenames to initialize Vide with.
     */
    public static void main(String[] args) {
        setAppleLookAndFeel();

        if (args.length == 0) {
            javax.swing.SwingUtilities.invokeLater(() -> newVideEditorWindow(null));
        } else {
            javax.swing.SwingUtilities.invokeLater(() -> {
                for (String filename : args) {
                    newVideEditorWindow(filename);
                }
            });
        }
    }

    /**
     * Load the input filename in a new editor window. Or, prompt the user to create a new file if
     * it does not yet exist.
     * @param filename The filename to load a new editor window for.
     */
    private static void newVideEditorWindow(String filename) {
        // 1. Null case: Just open a new empty editor window.
        if (filename == null) {
            VideEditorWindow videEditorWindow = new VideEditorWindow();
            videEditorWindow.start();
            return;
        }
        File currentFile = new File(filename);

        // 2. The file already exists. So load it for editing.
        if (currentFile.exists()) {
            VideEditorWindow videEditorWindow = new VideEditorWindow();
            videEditorWindow.start();
            videEditorWindow.loadFile(currentFile);
        }

        // 3. The file does not exist. So ask the user to ask if it should be created.
        else {
            String title = "";
            String message = "File \"" + filename + "\" does not exist. Would you like to create it?";
            Object[] options = new Object[] { "Yes", "No" };

            // Display a dialog window to ask the user about creating the new file.
            int result = JOptionPane.showOptionDialog(null, message, title, JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

            if (result == JOptionPane.YES_OPTION) {
                // Create the editor window.
                VideEditorWindow videEditorWindow = new VideEditorWindow();
                videEditorWindow.start();

                // Attempt to create the new file.
                try {
                    currentFile.createNewFile();
                } catch (IOException | SecurityException e) {
                    JOptionPane.showMessageDialog(videEditorWindow.getVideWindow(), "New file error",
                            "Error creating new file.", JOptionPane.ERROR_MESSAGE);
                }

                // Attach the file to the editor window. (As it is empty, it is not loaded.)
                videEditorWindow.setCurrentFile(currentFile);
            }
        }
    }

    /**
     * TODO: Support cross-platform system-dependent look and feel for different operating systems.
     */
    private static void setAppleLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "VIDE");
        System.setProperty("apple.awt.application.name", "VIDE");
    }
}
