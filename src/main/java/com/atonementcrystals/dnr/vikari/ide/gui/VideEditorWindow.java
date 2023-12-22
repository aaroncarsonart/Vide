package com.atonementcrystals.dnr.vikari.ide.gui;

import com.atonementcrystals.dnr.vikari.ide.Vide;
import com.atonementcrystals.dnr.vikari.ide.gui.document.SyntaxHighlightDocumentListener;
import com.atonementcrystals.dnr.vikari.ide.gui.document.VideDocumentListener;
import com.atonementcrystals.dnr.vikari.ide.gui.document.TabsToSpacesDocumentFilter;
import com.atonementcrystals.dnr.vikari.ide.parsing.VideColorTheme;
import com.atonementcrystals.dnr.vikari.ide.parsing.VideEditorTheme;
import com.atonementcrystals.dnr.vikari.ide.parsing.VikariSyntaxHighlighter;
import com.atonementcrystals.dnr.vikari.ide.undo.UndoHistory;
import com.atonementcrystals.dnr.vikari.ide.undo.UndoHistoryItem;
import com.atonementcrystals.dnr.vikari.ide.undo.UndoHistoryItemType;
import com.atonementcrystals.dnr.vikari.ide.util.CustomHTMLWriter;
import com.atonementcrystals.dnr.vikari.ide.util.GlobalUserSettings;
import com.atonementcrystals.dnr.vikari.ide.util.HTMLTransferable;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.desktop.QuitHandler;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A single Vide editor window instance.
 */
public class VideEditorWindow {
    private static final String NEW_FILE_TITLE = "VIDE - <New File>";
    private static final int DEFAULT_FONT_SIZE = 14;
    private static final Font DEFAULT_FONT = new Font("Andale Mono", Font.PLAIN, DEFAULT_FONT_SIZE);
    private static final int SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    public static final Stack<VideEditorWindow> ALL_OPEN_WINDOWS = new Stack<>();
    private static final Map<String, VideEditorWindow> ALL_OPEN_FILES = new HashMap<>();
    private static long NEW_FILE_INDEX = 0;
    private static final String NEW_FILE_PATH_PREFIX = "@-New-File-";
    private static QuitHandler quitHandler;

    private final JFrame videWindow;
    private final VideWindowAdapter videWindowAdapter;
    private final JScrollPane editorScrollPane;
    private final VideEditorPane textEditorPane;
    private final SyntaxHighlightDocumentListener syntaxHighlightDocumentListener;
    private final JTextArea lineNumbers;
    private final JTextArea statusLabel;

    private int fontSize;
    private Font font;
    private int fontWidth;
    private int fontHeight;

    private File currentFile;
    private String currentFilePath;
    private String fileContents;
    private boolean edited;

    private VideEditorTheme editorTheme;
    private VikariSyntaxHighlighter vikariSyntaxHighlighter;
    private final UndoHistory undoHistory;
    private int undoHistoryUneditedPosition;

    private int linePosition;
    private int columnPosition;

    /**
     * The default editor window when the program is first launched.
     */
    public VideEditorWindow() {
        this(DEFAULT_FONT);
    }

    /**
     * The editor window for inheriting settings from the previously focused window.
     * @param videFont The Font to use.
     */
    public VideEditorWindow(Font videFont) {
        ALL_OPEN_WINDOWS.add(this);
        editorTheme = Vide.getEditorTheme();

        videWindow = new JFrame(NEW_FILE_TITLE);
        JMenuBar videMenuBar = initVideMenuBar();
        videWindow.setJMenuBar(videMenuBar);
        videWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        videWindowAdapter = new VideWindowAdapter();
        videWindow.addWindowListener(videWindowAdapter);
        videWindow.addWindowFocusListener(new VideWindowFocusListener());

        this.font = videFont;
        this.fontSize = videFont.getSize();

        textEditorPane = new VideEditorPane();
        textEditorPane.setEditable(true);
        textEditorPane.setFont(videFont);
        calculateFontMetrics();

        lineNumbers = new JTextArea("1");
        lineNumbers.setEditable(false);
        lineNumbers.setFont(font);
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, fontWidth));

        DefaultStyledDocument styledDocument = (DefaultStyledDocument)textEditorPane.getStyledDocument();
        styledDocument.setDocumentFilter(new TabsToSpacesDocumentFilter());

        undoHistory = new UndoHistory(styledDocument);
        vikariSyntaxHighlighter = Vide.getSyntaxHighlighter();
        vikariSyntaxHighlighter.setEnabled(true);

        VideDocumentListener videDocumentListener = new VideDocumentListener(this);
        syntaxHighlightDocumentListener = videDocumentListener.getSyntaxHighlightListener();
        styledDocument.addDocumentListener(videDocumentListener);

        // Update line and column info when the text caret position changes.
        textEditorPane.addCaretListener(event -> {
            updateLineColumnPosition(event.getDot());
            updateStatusLabel();
        });

        editorScrollPane = new JScrollPane();
        editorScrollPane.getViewport().add(textEditorPane);
        editorScrollPane.setRowHeaderView(lineNumbers);
        editorScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        editorScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        editorScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        JScrollBar verticalScrollBar = editorScrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(fontHeight);
        JScrollBar horizontalScrollBar = editorScrollPane.getHorizontalScrollBar();
        horizontalScrollBar.setUnitIncrement(fontWidth);

        Container contentPane = videWindow.getContentPane();
        contentPane.add(editorScrollPane, BorderLayout.CENTER);

        // Set a default size for the first window.
        if (ALL_OPEN_WINDOWS.size() == 1) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            videWindow.setSize(screenSize.width / 3 * 2, screenSize.height / 3 * 2);
        }

        // Reuse the dimensions of the previous window.
        else if (ALL_OPEN_WINDOWS.size() > 1) {
            VideEditorWindow prevWindow = ALL_OPEN_WINDOWS.get(ALL_OPEN_WINDOWS.size() - 2);
            Dimension prevWindowSize = prevWindow.videWindow.getSize();
            videWindow.setSize(prevWindowSize);
        }

        else {
            throw new IllegalStateException("Unreachable code. ALL_OPEN_WINDOWS should never be empty.");
        }

        statusLabel = new JTextArea();
        statusLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
        statusLabel.setFont(font.deriveFont(14.0f));
        statusLabel.setEditable(false);
        contentPane.add(statusLabel, BorderLayout.SOUTH);

        // Initialize caret line and column position info in status label.
        updateLineColumnPosition(0);
        updateStatusLabel();

        fileContents = "";
        edited = false;
        undoHistoryUneditedPosition = undoHistory.getPosition();
        updateComponentColors();
    }

    public JFrame getVideWindow() {
        return videWindow;
    }

    public VideEditorPane getTextEditorPane() {
        return textEditorPane;
    }

    public JTextArea getLineNumbers() {
        return lineNumbers;
    }

    public String getFileContents() {
        return fileContents;
    }

    public void setFileContents(String fileContents) {
        this.fileContents = fileContents;
    }

    public void setEdited(boolean edited) {
        if (this.edited != edited) {
            this.edited = edited;
            if (currentFile != null) {
                updateWindowTitleWithFilename(currentFile);
            } else {
                videWindow.setTitle(NEW_FILE_TITLE + (edited ? " - edited" : ""));
            }
        }
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    public VikariSyntaxHighlighter getVikariSyntaxHighlighter() {
        return vikariSyntaxHighlighter;
    }

    public UndoHistory getUndoHistory() {
        return undoHistory;
    }

    /**
     * Properly dispose of a VideEditorWindow's components when the window is closed.
     */
    private class VideWindowAdapter extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            close();
        }
    }

    /**
     * For pushing focused windows to the top of the {@link #ALL_OPEN_WINDOWS} stack.
     */
    private class VideWindowFocusListener implements WindowFocusListener {
        @Override
        public void windowGainedFocus(WindowEvent e) {
            ALL_OPEN_WINDOWS.remove(VideEditorWindow.this);
            ALL_OPEN_WINDOWS.push(VideEditorWindow.this);
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
            // Handled by WindowFocusListeners of other windows.
        }
    }

    private void calculateFontMetrics() {
        FontMetrics fontMetrics = textEditorPane.getFontMetrics(font);
        fontWidth = fontMetrics.charWidth(' ');
        fontHeight = fontMetrics.getHeight();
    }

    private JMenuItem initNewMenuItem() {
        JMenuItem newMenuItem = new JMenuItem("New");
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, SHORTCUT_KEY_MASK));
        newMenuItem.addActionListener(event -> newFile());
        return newMenuItem;
    }

    private JMenuItem initOpenMenuItem() {
        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, SHORTCUT_KEY_MASK));
        openMenuItem.addActionListener(event -> open());
        return openMenuItem;
    }

    private JMenuItem initSaveMenuItem() {
        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_KEY_MASK));
        saveMenuItem.addActionListener(event -> save());
        return saveMenuItem;
    }

    private JMenuItem initSaveAsMenuItem() {
        JMenuItem saveAsMenuItem = new JMenuItem("Save as");
        saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_KEY_MASK | Event.SHIFT_MASK));
        saveAsMenuItem.addActionListener(event -> saveAs());
        return saveAsMenuItem;
    }

    private JMenuItem initCloseMenuItem() {
        JMenuItem closeMenuItem = new JMenuItem("Close");
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, SHORTCUT_KEY_MASK));
        closeMenuItem.addActionListener(event -> close());
        return closeMenuItem;
    }

    private JMenuItem initQuitMenuItem() {
        JMenuItem quitMenuItem = new JMenuItem("Quit");

        // Handle the Quit command with a QuitHandler if the system supports it.
        if (Desktop.isDesktopSupported()) {
            if (quitHandler == null) {
                quitHandler = createVideQuitHandler();
                Desktop desktop = Desktop.getDesktop();
                desktop.setQuitHandler(quitHandler);
            }
        }

        // Otherwise, set the Ctrl+Q/Cmd+Q keyboard shortcut to this Quit menu item.
        else {
            quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, SHORTCUT_KEY_MASK));
        }

        // Allow handling the Quit command directly via this Quit menu item.
        quitMenuItem.addActionListener(createVideQuitMenuActionListener());

        return quitMenuItem;
    }

    /**
     * For systems that support {@link Desktop}, manage quit requests with a {@link QuitHandler}.
     * @return The QuitHandler to handle quit events for Vide.
     */
    private static QuitHandler createVideQuitHandler() {
        return (quitEvent, quitResponse) -> {
            if (quit()) {
                quitResponse.performQuit();
            } else {
                quitResponse.cancelQuit();
            }
        };
    }

    /**
     * For systems that do not support {@link Desktop}, manage quit events with an {@link MenuItem} {@link ActionListener}.
     * @return The ActionListener to handle quit events for Vide.
     */
    private static ActionListener createVideQuitMenuActionListener() {
        return actionEvent -> {
            if(quit()) {
                System.exit(0);
            }
        };
    }

    private JMenuItem initUndoMenuItem() {
        JMenuItem undoMenuItem = new JMenuItem("Undo");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_KEY_MASK));
        undoMenuItem.addActionListener(event -> undo());
        return undoMenuItem;
    }

    private JMenuItem initRedoMenuItem() {
        JMenuItem redoMenuItem = new JMenuItem("Redo");
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT_KEY_MASK | Event.SHIFT_MASK));
        redoMenuItem.addActionListener(event -> redo());
        return redoMenuItem;
    }

    private JMenuItem initCopyFormattedTextMenuItem() {
        JMenuItem copyFormattedTextMenuItem = new JMenuItem("Copy Formatted Text");
        copyFormattedTextMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, SHORTCUT_KEY_MASK | Event.SHIFT_MASK));
        copyFormattedTextMenuItem.addActionListener(event -> copyHtmlFormattedTextSelectionToClipboard());
        return copyFormattedTextMenuItem;
    }

    private JMenuItem initToggleWordWrapMenuItem() {
        JMenuItem toggleWordWrapMenuItem = new JMenuItem("Toggle Word Wrap");
        toggleWordWrapMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, SHORTCUT_KEY_MASK | Event.ALT_MASK));
        toggleWordWrapMenuItem.addActionListener(event -> toggleWordWrap());
        return toggleWordWrapMenuItem;
    }

    private JMenuItem initToggleSyntaxHighlightingMenuItem() {
        JMenuItem toggleWordWrapMenuItem = new JMenuItem("Toggle Syntax Highlighting");
        toggleWordWrapMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, SHORTCUT_KEY_MASK | Event.SHIFT_MASK));
        toggleWordWrapMenuItem.addActionListener(event -> toggleSyntaxHighlighting());
        return toggleWordWrapMenuItem;
    }

    private JMenuItem initToggleDarkModeMenuItem() {
        JMenuItem toggleDarkModeMenuItem = new JMenuItem("Toggle Dark Mode");
        toggleDarkModeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, SHORTCUT_KEY_MASK));
        toggleDarkModeMenuItem.addActionListener(event -> toggleDarkMode());
        return toggleDarkModeMenuItem;
    }

    private JMenuBar initVideMenuBar() {
        // Create the "File" menu.
        JMenuItem newMenuItem = initNewMenuItem();
        JMenuItem openMenuItem = initOpenMenuItem();
        JMenuItem saveMenuItem = initSaveMenuItem();
        JMenuItem saveAsMenuItem = initSaveAsMenuItem();
        JMenuItem closeMenuItem = initCloseMenuItem();
        JMenuItem quitMenuItem = initQuitMenuItem();


        JMenu fileMenu = new JMenu("File");
        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(closeMenuItem);
        fileMenu.add(quitMenuItem);

        // Create the "Edit" menu.
        JMenuItem undoMenuItem = initUndoMenuItem();
        JMenuItem redoMenuItem = initRedoMenuItem();
        JMenuItem copyFormattedTextMenuItem = initCopyFormattedTextMenuItem();

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(undoMenuItem);
        editMenu.add(redoMenuItem);
        editMenu.add(copyFormattedTextMenuItem);

        // Create the "View" menu.
        JMenuItem toggleWordWrapMenuItem = initToggleWordWrapMenuItem();
        JMenuItem toggleSyntaxHighlightingMenuItem = initToggleSyntaxHighlightingMenuItem();
        JMenuItem toggleDarkModeMenuItem = initToggleDarkModeMenuItem();

        JMenu viewMenu = new JMenu("View");
        viewMenu.add(toggleWordWrapMenuItem);
        viewMenu.add(toggleSyntaxHighlightingMenuItem);
        viewMenu.add(toggleDarkModeMenuItem);

        JMenuBar videMenuBar = new JMenuBar();
        videMenuBar.add(fileMenu);
        videMenuBar.add(editMenu);
        videMenuBar.add(viewMenu);

        return videMenuBar;
    }

    public void updateLineColumnPosition(int caret) {
        linePosition = 1;
        int lastLine = 0;
        for (int i = 0; i < caret; i++) {
            char c = fileContents.charAt(i);
            if (c == '\n') {
                linePosition++;
                lastLine = i;
            }
        }
        int offset = linePosition == 1 ? 1 : 0;
        columnPosition = caret - lastLine + offset;
    }

    public void updateStatusLabel() {
        StringBuilder sb = new StringBuilder();

        sb.append(" Line: ");
        sb.append(linePosition);
        sb.append("  Col: ");
        sb.append(columnPosition);

        String result = sb.toString();
        statusLabel.setText(result);
    }

    /**
     * Make the VideEditorWindow display and to be available for editing a Vikari source file.
     */
    public void start() {
        // The first window is centered.
        if (ALL_OPEN_WINDOWS.size() == 1) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width / 2 - videWindow.getWidth() / 2;
            int y = 0;

            videWindow.setLocation(x, y);
        }

        // Further windows are tiled against the previous window's location.
        else if (ALL_OPEN_WINDOWS.size() > 1) {
            // Must walk one past the current hidden window.
            VideEditorWindow prevWindow = ALL_OPEN_WINDOWS.get(ALL_OPEN_WINDOWS.size() - 2);
            Point prevWindowLocation = prevWindow.videWindow.getLocationOnScreen();

            int prevX = prevWindowLocation.x;
            int prevY = prevWindowLocation.y;

            Point scrollPaneOffset = prevWindow.editorScrollPane.getLocationOnScreen();
            int titleBarOffset = (scrollPaneOffset.y - prevY) / 2;

            int x = prevX + titleBarOffset;
            int y = prevY + titleBarOffset;

            videWindow.setLocation(x, y);
        }

        else {
            throw new IllegalStateException("Unreachable code. ALL_OPEN_WINDOWS should never be empty.");
        }

        // Begin to track edits and display the window.
        undoHistory.setEnabled(true);
        videWindow.setVisible(true);
        textEditorPane.requestFocusInWindow();
    }

    private String getCanonicalFilePath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
        this.currentFilePath = getCanonicalFilePath(currentFile);
        updateWindowTitleWithFilename(currentFile);
        storeInFileCache(currentFile);
    }

    public void loadFile(File file) {
        this.currentFile = file;
        this.currentFilePath = getCanonicalFilePath(currentFile);
        System.out.println(currentFilePath);
        try {
            fileContents = Files.readString(currentFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        undoHistory.setEnabled(false);
        vikariSyntaxHighlighter.setEnabled(false);
        textEditorPane.setText(fileContents);
        textEditorPane.setCaretPosition(0);
        undoHistory.setEnabled(true);
        vikariSyntaxHighlighter.setEnabled(true);

        edited = false;
        updateWindowTitleWithFilename(currentFile);
        storeInFileCache(currentFile);
        vikariSyntaxHighlighter.highlightEntireFile(currentFilePath, fileContents, textEditorPane);
    }

    /**
     * Performs the "New" menu item action.
     */
    public void newFile() {
        VideEditorWindow videEditorWindow = new VideEditorWindow(font);
        videEditorWindow.start();
        videEditorWindow.initNewFilePath();
    }

    private boolean validateFileExtensions(File directory, String filename) {
        String fileExtension = FilenameUtils.getExtension(filename);
        return fileExtension.equals("DNR") || fileExtension.equals("dnr");
    }

    /**
     * Performs the "Open" menu item action.
     */
    public void open() {
        String lastViewedDirectory = GlobalUserSettings.getLastViewedDirectory();

        FileDialog fileDialog = new FileDialog(videWindow, "Open", FileDialog.LOAD);
        fileDialog.setDirectory(lastViewedDirectory);
        fileDialog.setFilenameFilter(this::validateFileExtensions);
        fileDialog.setMultipleMode(true);
        fileDialog.setVisible(true);

        File[] selectedFiles = fileDialog.getFiles();

        if (selectedFiles.length > 0) {
            File firstFile = selectedFiles[0];
            GlobalUserSettings.setLastViewedDirectory(firstFile.getParent());
        }

        for (File selectedFile : selectedFiles) {
            // If the file is already open, simply request focus on that existing window.
            if (isFileIsAlreadyOpen(selectedFile)) {
                VideEditorWindow existingWindow = getOpenEditorWindowFor(selectedFile);
                existingWindow.videWindow.requestFocus();
            }

            // Load the new file in the current active window.
            else if (isReadyForOpenedFile()) {
                loadFile(selectedFile);
            }

            // Otherwise, create a new editor window, if necessary.
            else {
                VideEditorWindow newEditorWindow = new VideEditorWindow(font);
                newEditorWindow.start();
                newEditorWindow.loadFile(selectedFile);
            }
        }
    }


    /**
     * Performs the "Save" menu item action.
     */
    public boolean save() {
        if (currentFile == null) {
            String lastViewedDirectory = GlobalUserSettings.getLastViewedDirectory();

            FileDialog fileDialog = new FileDialog(videWindow, "Save", FileDialog.SAVE);
            fileDialog.setDirectory(lastViewedDirectory);
            fileDialog.setFile("Untitled.DNR");
            fileDialog.setFilenameFilter(this::validateFileExtensions);
            fileDialog.setVisible(true);

            // Length will always be 1 or 0.
            File[] selectedFiles = fileDialog.getFiles();
            if (selectedFiles.length == 1) {
                removeFromFileCache(currentFile);

                currentFile = selectedFiles[0];
                GlobalUserSettings.setLastViewedDirectory(currentFile.getParent());
                saveFile(currentFile);

                storeInFileCache(currentFile);
                return true;
            }
            return false;
        } else {
            saveFile(currentFile);
            return true;
        }
    }
    /**
     * Performs the "Save As" menu item action.
     */
    public void saveAs() {
        String lastViewedDirectory = GlobalUserSettings.getLastViewedDirectory();

        FileDialog fileDialog = new FileDialog(videWindow, "Save As", FileDialog.SAVE);
        fileDialog.setDirectory(lastViewedDirectory);
        fileDialog.setFile("Untitled.DNR");
        fileDialog.setFilenameFilter(this::validateFileExtensions);
        fileDialog.setVisible(true);

        // Length will always be 1 or 0.
        File[] selectedFiles = fileDialog.getFiles();
        if (selectedFiles.length == 1) {
            removeFromFileCache(currentFile);

            currentFile = selectedFiles[0];
            GlobalUserSettings.setLastViewedDirectory(currentFile.getParent());
            saveFile(currentFile);

            storeInFileCache(currentFile);
        }
    }

    /**
     * Save the editor pane's contents to the given file.
     * @param file The File object to save the editor pane's contents to.
     */
    private void saveFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            fileContents = textEditorPane.getText();
            writer.write(fileContents);
            writer.flush();
            edited = false;
            updateWindowTitleWithFilename(currentFile);
            undoHistoryUneditedPosition = undoHistory.getPosition();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Dispose of this editor window's resources, and then finally close the window. Unless it is
     * the last open editor window, in which case it then becomes an editor for a new empty file.
     * @return {@link JOptionPane#YES_OPTION}, {@link JOptionPane#NO_OPTION}, or {@link JOptionPane#CANCEL_OPTION},
     *         based on if the open file was saved, was not saved, or cancel was selected.
     */
    public int close() {
        if (edited) {
            JOptionPane jOptionPane = new JOptionPane("Save before closing?", JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.YES_NO_CANCEL_OPTION);
            JDialog jDialog = jOptionPane.createDialog(videWindow, "");
            jDialog.setLocationRelativeTo(videWindow);
            jDialog.setVisible(true);
            if (jOptionPane.getValue() instanceof Integer selectedValue) {
                if (selectedValue == JOptionPane.YES_OPTION) {
                    boolean saved = save();
                    if (saved) {
                        closeEditorWindow();
                        return JOptionPane.YES_OPTION;
                    }
                    return JOptionPane.CANCEL_OPTION;
                } else if (selectedValue == JOptionPane.NO_OPTION) {
                    closeEditorWindow();
                    return JOptionPane.NO_OPTION;
                }
            }
            return JOptionPane.CANCEL_OPTION;
        } else {
            closeEditorWindow();
            return JOptionPane.NO_OPTION;
        }
    }

    private void closeEditorWindow() {
        // Dispose the current window's assets.
        undoHistory.clear();
        undoHistory.setEnabled(false);
        vikariSyntaxHighlighter.setEnabled(false);
        fileContents = "";
        textEditorPane.setText(fileContents);
        undoHistory.setEnabled(true);

        // Clear the file info, and the entry in the file cache.
        removeFromFileCache(currentFile);
        vikariSyntaxHighlighter.closeFile(currentFilePath);
        vikariSyntaxHighlighter.setEnabled(true);
        currentFile = null;
        currentFilePath = null;

        // Dispose of the window.
        videWindow.setVisible(false);
        videWindow.removeWindowListener(videWindowAdapter);
        videWindow.dispose();
        ALL_OPEN_WINDOWS.remove(this);

        // Close the program if no editor windows are open.
        if (ALL_OPEN_WINDOWS.isEmpty()) {
            System.exit(0);
        }
    }

    /**
     * Convert the filename into a canonical path, and then apply the accessor function to that
     * filename. The accessor function can be anything, but typically it should access the file
     * cache. Use of this method enables accessing the file cache without needing to specify a
     * try/catch block for first generating the necessary canonical path string by which the file
     * info is then cached.
     * @param file The file to access the file cache for.
     * @param accessorFunction The function to apply against the canonical path form of the
     *                         filename. Accepts a string (that is the filename) and optionally
     *                         returns a value.
     * @return The returned value from the accessor function being applied to the file's filename.
     * @param <E> The optional return value of the accessor function.
     */
    private <E> E accessFileCache(File file, Function<String, E> accessorFunction) {
        try {
            String filename = file.getCanonicalPath();
            return accessorFunction.apply(filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check if the file has already been opened in an existing editor window.
     * @param file The file to check.
     * @return True if the file is already open, else false.
     */
    private boolean isFileIsAlreadyOpen(File file) {
        return accessFileCache(file, ALL_OPEN_FILES::containsKey);
    }

    /**
     * Fetches an existing editor window for the given file. Presumes a prior call to {@link
     * #isFileIsAlreadyOpen(File)} has returned true for the file.
     * @param file The file to check.
     * @return A reference to an existing editor window that is mapped to the given file, or else
     *         null if no mapping is present.
     */
    private VideEditorWindow getOpenEditorWindowFor(File file) {
        return accessFileCache(file, ALL_OPEN_FILES::get);
    }

    /**
     * Removes the existing file/editor window mapping from the file cache.
     * @param file The file to remove from the file cache.
     */
    private void removeFromFileCache(File file) {
        if (file != null) {
            accessFileCache(file, ALL_OPEN_FILES::remove);
        }
    }

    /**
     * Stores the existing file in the file cache by mapping it to this current editor window.
     * @param file The file to store in the file cache.
     */
    private void storeInFileCache(File file) {
        accessFileCache(file, filename -> ALL_OPEN_FILES.put(filename, this));
    }

    private void updateWindowTitleWithFilename(File file) {
        String quotedFilename = "\"" + file.getAbsolutePath() + "\"";
        videWindow.setTitle("VIDE - " + quotedFilename + (edited ? " - edited" : ""));
    }

    /**
     * An empty editor window can load an opened file IF it has no current open file, text contents,
     * or undo history associated with it.
     * @return True if this editor window can load a new opened file, else false.
     */
    public boolean isReadyForOpenedFile() {
        return currentFile == null && (fileContents == null || fileContents.isEmpty()) &&
                !undoHistory.canUndo() && !undoHistory.canRedo();
    }

    /**
     * Performs the "Quit" menu action item.
     * @return True if the result of this command means the Vide can be quit, else false.
     */
    public static boolean quit() {
        // Close all windows.
        VideEditorWindow editorWindow;
        while ((editorWindow = ALL_OPEN_WINDOWS.peek()) != null) {
            // Prompt the user to save the file if it has been edited.
            if (editorWindow.edited) {
                int result = editorWindow.close();
                if (result == JOptionPane.CANCEL_OPTION) {
                    // If cancel was selected, stop closing remaining files.
                    break;
                }
            }
            // If the file was not edited, just close the file.
            else {
                editorWindow.close();
            }
        }
        // If there are no editor windows open, report the program should be quit.
        return ALL_OPEN_WINDOWS.isEmpty();
    }

    /**
     * Performs the "Undo" menu item action.
     */
    public void undo() {
        if (undoHistory.canUndo()) {
            undoHistory.undo();
            setEdited(undoHistory.getPosition() != undoHistoryUneditedPosition);
        }
    }

    /**
     * Performs the "Redo" menu item action.
     */
    public void redo() {
        if (undoHistory.canRedo()) {
            undoHistory.redo();
            setEdited(undoHistory.getPosition() != undoHistoryUneditedPosition);
        }
    }

    /**
     * Perform the "Copy Formatted Text" menu item.<br/>
     * <br/>
     * TODO: Rewrite this method to generate exactly the desired HTML output.
     *       (Not the default output from the StyledDocument.)
     */
    public void copyHtmlFormattedTextSelectionToClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        int startIndex = textEditorPane.getSelectionStart();
        int endIndex = textEditorPane.getSelectionEnd();
        int length = endIndex - startIndex;
        StyledDocument styledDocument = textEditorPane.getStyledDocument();

        try (OutputStream os = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(os)) {
            CustomHTMLWriter htmlWriter = new CustomHTMLWriter(osw, styledDocument, startIndex, length);
            htmlWriter.write();
            osw.flush();
            String contents = os.toString();

            // fix preservation of indentation
            contents = contents.replaceAll(Pattern.quote("<body>"), "<body><pre>");
            contents = contents.replaceAll(Pattern.quote("</body>"), "</pre></body>");

            // fix line spacing
            contents = contents.replaceAll(Pattern.quote("<p class=default>"), "");
            contents = contents.replaceAll(Pattern.quote("</p>"), "<br/>");

            HTMLTransferable htmlTransferable = new HTMLTransferable(contents);
            clipboard.setContents(htmlTransferable, null);
        } catch (IOException | BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform the "Toggle Word Wrap" menu item.
     */
    public void toggleWordWrap() {
        textEditorPane.toggleWordWrap();
        editorScrollPane.setViewportView(textEditorPane);
    }

    /**
     * Perform the "Toggle Word Wrap" menu item.
     */
    public void toggleSyntaxHighlighting() {
        boolean newValue = !vikariSyntaxHighlighter.isEnabled();
        vikariSyntaxHighlighter.setEnabled(newValue);
    }

    /**
     * Perform the "Toggle Dark Mode" menu item.
     */
    public void toggleDarkMode() {
        // Load the color theme.
        VideEditorTheme videEditorTheme = Vide.getEditorTheme();
        VideColorTheme.WindowColorTheme colorTheme = videEditorTheme.getWindowColorTheme();

        if (colorTheme == VideColorTheme.WindowColorTheme.LIGHT_MODE) {
            Vide.loadVideColorTheme("color_theme_dark.json");
        } else if (colorTheme == VideColorTheme.WindowColorTheme.DARK_MODE) {
            Vide.loadVideColorTheme("color_theme_light.json");
        } else {
            throw new IllegalStateException("Unexpected color theme: "  + colorTheme);
        }

        Vide.reportColorDefinitionErrors();

        // Update all open editor windows.
        for (VideEditorWindow editorWindow : ALL_OPEN_WINDOWS) {

            // Update the editor's displayed colors.
            editorWindow.editorTheme = Vide.getEditorTheme();
            editorWindow.vikariSyntaxHighlighter = Vide.getSyntaxHighlighter();
            editorWindow.syntaxHighlightDocumentListener.setSyntaxHighlighter(editorWindow.vikariSyntaxHighlighter);

            SwingUtilities.invokeLater(() -> {
                editorWindow.updateComponentColors();
                editorWindow.vikariSyntaxHighlighter.setEnabled(true);
                editorWindow.vikariSyntaxHighlighter.highlightEntireFile(editorWindow.currentFilePath,
                        editorWindow.fileContents, editorWindow.textEditorPane);
            });
        }
    }

    /**
     * Apply a new color theme's styling to the UI components.
     */
    private void updateComponentColors() {
        textEditorPane.setCaretColor(editorTheme.getCaretColor());
        textEditorPane.setSelectionColor(editorTheme.getHighlightColor());
        textEditorPane.setForeground(editorTheme.getTextEditorFg());
        textEditorPane.setBackground(editorTheme.getTextEditorBg());
        lineNumbers.setForeground(editorTheme.getLineNumbersFg());
        lineNumbers.setBackground(editorTheme.getLineNumbersBg());
        statusLabel.setForeground(editorTheme.getStatusLabelFg());
        statusLabel.setBackground(editorTheme.getStatusLabelBg());
        editorScrollPane.setForeground(editorTheme.getLineNumbersFg());
        editorScrollPane.setBackground(editorTheme.getLineNumbersBg());
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(editorTheme.getTextEditorFg()));
        UIManager.put("OptionPane.background",new ColorUIResource(editorTheme.getLineNumbersBg()));
        UIManager.put("OptionPane.foreground",new ColorUIResource(editorTheme.getLineNumbersFg()));
        UIManager.put("Panel.background",new ColorUIResource(editorTheme.getLineNumbersBg()));
        UIManager.put("Panel.foreground",new ColorUIResource(editorTheme.getLineNumbersFg()));
    }

    public void addInsertTextUndoHistoryItem(int startOffset, int length, String addedText) {
        int endOffset = startOffset + length;

        UndoHistoryItem newInsertItem = new UndoHistoryItem(UndoHistoryItemType.INSERT_TEXT,
                startOffset, endOffset, addedText);
        undoHistory.addHistoryItem(newInsertItem);
    }

    public void addRemoveTextUndoHistoryItem(int startOffset, int length, String removedText) {
        int endOffset = startOffset + length;

        UndoHistoryItem newInsertItem = new UndoHistoryItem(UndoHistoryItemType.REMOVE_TEXT,
                startOffset, endOffset, removedText);
        undoHistory.addHistoryItem(newInsertItem);
    }

    private String generateNewFilePath() {
        return NEW_FILE_PATH_PREFIX + NEW_FILE_INDEX++;
    }

    public void initNewFilePath() {
        currentFilePath = generateNewFilePath();
    }
}
