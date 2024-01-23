package com.atonementcrystals.dnr.vikari.ide;

import com.atonementcrystals.dnr.vikari.ide.gui.VideEditorWindow;
import com.atonementcrystals.dnr.vikari.ide.parsing.VideColorTheme;
import com.atonementcrystals.dnr.vikari.ide.parsing.VideColorThemeProcessor;
import com.atonementcrystals.dnr.vikari.ide.parsing.VideEditorTheme;
import com.atonementcrystals.dnr.vikari.ide.parsing.VikariSyntaxHighlighter;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Image;
import java.awt.Taskbar;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Main class of the Vikari IDE.
 */
public class Vide {
    private static VideEditorTheme videEditorTheme;
    private static VikariSyntaxHighlighter vikariSyntaxHighlighter;
    private static VideColorThemeProcessor videColorThemeProcessor;

    /**
     * Entry point function of program.
     * @param args A list of filenames to initialize Vide with.
     */
    public static void main(String[] args) {
        setAppleLookAndFeel();
        loadDefaultVideColorTheme();
        setAppIconInTaskbar();

        // Open a single Vide editor window.
        if (args.length == 0) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                newVideEditorWindow(null);
                reportColorDefinitionErrors();
            });
        }

        // Open each specified file in a new Vide editor window.
        else {
            javax.swing.SwingUtilities.invokeLater(() -> {
                for (String filename : args) {
                    newVideEditorWindow(filename);
                }
                reportColorDefinitionErrors();
            });
        }

    }

    public static VideEditorTheme getEditorTheme() {
        return videEditorTheme;
    }

    public static VikariSyntaxHighlighter getSyntaxHighlighter() {
        return vikariSyntaxHighlighter;
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
            videEditorWindow.initNewFilePath();
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

                    // Attach the file to the editor window. (As it is empty, it is not loaded.)
                    videEditorWindow.setCurrentFile(currentFile);
                } catch (IOException | SecurityException e) {
                    JOptionPane.showMessageDialog(videEditorWindow.getVideWindow(), "New file error",
                            "Error creating new file.", JOptionPane.ERROR_MESSAGE);

                    // Initialize with the default new file path string, instead.
                    videEditorWindow.initNewFilePath();
                }
            }
        }
    }

    private static void loadDefaultVideColorTheme() {
        loadVideColorTheme("color_theme_light.json");
    }

    public static void loadVideColorTheme(String filePath) {
        VideColorTheme videColorTheme = loadVideColorThemeFromResources(filePath);
        videEditorTheme = new VideEditorTheme();
        vikariSyntaxHighlighter = new VikariSyntaxHighlighter();
        videColorThemeProcessor = new VideColorThemeProcessor(videColorTheme, videEditorTheme, vikariSyntaxHighlighter);
        vikariSyntaxHighlighter.loadColorNames(videColorThemeProcessor.getNamedColors());
    }

    private static VideColorTheme loadVideColorThemeFromResources(String filePath) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            URL colorThemeURL = Vide.class.getClassLoader().getResource(filePath);
            VideColorTheme videColorTheme = objectMapper.readValue(colorThemeURL, VideColorTheme.class);
            videColorTheme.setFilePath(filePath);
            return videColorTheme;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void reportColorDefinitionErrors() {
        // Report any errors, if necessary.
        if (videColorThemeProcessor.hasColorDefinitionErrors()) {
            VideEditorWindow topEditorWindow = VideEditorWindow.ALL_OPEN_WINDOWS.peek();
            videColorThemeProcessor.reportColorDefinitionErrors(topEditorWindow.getVideWindow());
        }
        // Unload the color theme processor after it is no longer needed.
        videColorThemeProcessor = null;
    }

    /**
     * TODO: Support cross-platform system-dependent look and feel for different operating systems.
     */
    private static void setAppleLookAndFeel() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "VIDE");
        System.setProperty("apple.awt.application.name", "VIDE");
        System.setProperty("apple.awt.application.appearance", "system");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load the app icon from the resources folder.
     * @return The app icon as an image. Returns null if there was an error.
     */
    public static Image loadAppIconImage() {
        String iconImagePath = "images/vide_icon-512x512.png";
        ClassLoader classLoader = Vide.class.getClassLoader();
        InputStream iconInputStream = classLoader.getResourceAsStream(iconImagePath);

        if (iconInputStream != null) {
            try {
                return ImageIO.read(iconInputStream);
            } catch (IOException e) {
                System.out.println("IO error while reading file: \"" + iconImagePath + "\"");
            }
        } else {
            System.out.println("File \"" + iconImagePath + "\" could not be loaded from resources.");
        }

        return null;
    }

    /**
     * For the Apple look and feel on macOS, set the icon image in the Dock.
     */
    private static void setAppIconInTaskbar() {
        Image iconImage = loadAppIconImage();
        if (iconImage != null) {
            try {
                Taskbar taskbar = Taskbar.getTaskbar();
                taskbar.setIconImage(iconImage);
            } catch (UnsupportedOperationException | SecurityException e) {
                // Silently fail for non-supported operating systems.
            }
        }
    }
}
