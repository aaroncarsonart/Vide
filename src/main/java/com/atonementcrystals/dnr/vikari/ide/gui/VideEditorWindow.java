package com.atonementcrystals.dnr.vikari.ide.gui;

import com.atonementcrystals.dnr.vikari.ide.gui.document.LineNumbersDocumentListener;
import com.atonementcrystals.dnr.vikari.ide.gui.document.SyntaxHighlightUndoHistoryDocumentListener;
import com.atonementcrystals.dnr.vikari.ide.gui.document.TabsToSpacesDocumentFilter;
import com.atonementcrystals.dnr.vikari.ide.parsing.SyntaxHighlighter;
import com.atonementcrystals.dnr.vikari.ide.undo.UndoHistory;
import com.atonementcrystals.dnr.vikari.ide.undo.UndoHistoryItem;
import com.atonementcrystals.dnr.vikari.ide.undo.UndoHistoryItemType;
import com.atonementcrystals.dnr.vikari.ide.util.CustomHTMLWriter;
import com.atonementcrystals.dnr.vikari.ide.util.GlobalUserSettings;
import com.atonementcrystals.dnr.vikari.ide.util.HTMLTransferable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
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
    private static final Color LIGHT_GREY = new Color(245, 245, 245);
    private static final Stack<VideEditorWindow> ALL_OPEN_WINDOWS = new Stack<>();
    private static final Map<String, VideEditorWindow> ALL_OPEN_FILES = new HashMap<>();

    private final JFrame videWindow;
    private final JScrollPane editorScrollPane;
    private final VideEditorPane textEditorPane;
    private final JTextArea lineNumbers;
    private final JLabel statusLabel;

    private int fontSize;
    private Font font;
    private int fontWidth;
    private int fontHeight;

    private File currentFile;
    private String fileContents;

    private final SyntaxHighlighter syntaxHighlighter;
    private final UndoHistory undoHistory;

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

        videWindow = new JFrame(NEW_FILE_TITLE);
        JMenuBar videMenuBar = initVideMenuBar();
        videWindow.setJMenuBar(videMenuBar);
        videWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        videWindow.addWindowListener(new VideWindowAdapter());
        videWindow.addWindowFocusListener(new VideWindowFocusListener());

        this.font = videFont;
        this.fontSize = videFont.getSize();

        textEditorPane = new VideEditorPane();
        textEditorPane.setEditable(true);
        textEditorPane.setFont(videFont);
        calculateFontMetrics();

        DefaultStyledDocument styledDocument = (DefaultStyledDocument)textEditorPane.getStyledDocument();
        styledDocument.setDocumentFilter(new TabsToSpacesDocumentFilter());

        undoHistory = new UndoHistory(styledDocument);
        syntaxHighlighter = new SyntaxHighlighter(styledDocument);

        lineNumbers = new JTextArea("1");
        lineNumbers.setBackground(LIGHT_GREY);
        lineNumbers.setForeground(Color.GRAY);
        lineNumbers.setEditable(false);
        lineNumbers.setEnabled(false);
        lineNumbers.setFont(font);
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, fontWidth));

        styledDocument.addDocumentListener(new LineNumbersDocumentListener(textEditorPane, lineNumbers));
        styledDocument.addDocumentListener(new SyntaxHighlightUndoHistoryDocumentListener(this));

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

        statusLabel = new JLabel();
        statusLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
        statusLabel.setForeground(LIGHT_GREY);
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setFont(font.deriveFont(14.0f));
        contentPane.add(statusLabel, BorderLayout.SOUTH);

        // Initialize caret line and column position info in status label.
        updateLineColumnPosition(0);
        updateStatusLabel();
    }

    public JFrame getVideWindow() {
        return videWindow;
    }

    public VideEditorPane getTextEditorPane() {
        return textEditorPane;
    }

    public String getFileContents() {
        return fileContents;
    }

    public void setFileContents(String fileContents) {
        this.fileContents = fileContents;
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
        quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, SHORTCUT_KEY_MASK));
        quitMenuItem.addActionListener(event -> System.exit(0));
        return quitMenuItem;
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
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(toggleWordWrapMenuItem);

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

    public boolean isStarted() {
        return videWindow.isVisible();
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
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
        updateWindowTitleWithFilename(currentFile);
        storeInFileCache(currentFile);
    }

    public void loadFile(File file) {
        this.currentFile = file;
        try {
            fileContents = Files.readString(currentFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        undoHistory.setEnabled(false);
        textEditorPane.setText(fileContents);
        textEditorPane.setCaretPosition(0);
        undoHistory.setEnabled(true);

        updateWindowTitleWithFilename(currentFile);
        storeInFileCache(currentFile);
    }

    /**
     * Performs the "New" menu item action.
     */
    public void newFile() {
        VideEditorWindow videEditorWindow = new VideEditorWindow(font);
        videEditorWindow.start();
    }

    /**
     * Performs the "Open" menu item action.
     */
    public void open() {
        // TODO: Try using FileDialog instead of JFileChooser to force a native window look.
        String lastViewedDirectory = GlobalUserSettings.getLastViewedDirectory();
        JFileChooser fileChooser = new JFileChooser(lastViewedDirectory);

        int result = fileChooser.showOpenDialog(videWindow);
        if (result == JFileChooser.APPROVE_OPTION) {
            File currentFile = fileChooser.getSelectedFile();
            GlobalUserSettings.setLastViewedDirectory(currentFile.getParent());

            // If the file is already open, simply request focus on that existing window.
            if (isFileIsAlreadyOpen(currentFile)) {
                VideEditorWindow existingWindow = getOpenEditorWindowFor(currentFile);
                existingWindow.videWindow.requestFocus();
            }

            // Load the new file in the current active window.
            else if (isReadyForOpenedFile()) {
                loadFile(currentFile);
            }

            // Otherwise, create a new editor window, if necessary.
            else {
                VideEditorWindow newEditorWindow = new VideEditorWindow(font);
                newEditorWindow.start();
                newEditorWindow.loadFile(currentFile);
            }
        }
    }

    /**
     * Performs the "Save" menu item action.
     */
    public void save() {
        if (currentFile == null) {
            String lastViewedDirectory = GlobalUserSettings.getLastViewedDirectory();
            JFileChooser fileChooser = new JFileChooser(lastViewedDirectory);
            int result = fileChooser.showSaveDialog(videWindow);
            if (result == JFileChooser.APPROVE_OPTION) {
                removeFromFileCache(currentFile);

                currentFile = fileChooser.getSelectedFile();
                GlobalUserSettings.setLastViewedDirectory(currentFile.getParent());
                saveFile(currentFile);

                storeInFileCache(currentFile);
            }
        } else {
            saveFile(currentFile);
        }
    }
    /**
     * Performs the "Save As" menu item action.
     */
    public void saveAs() {
        String lastViewedDirectory = GlobalUserSettings.getLastViewedDirectory();
        JFileChooser fileChooser = new JFileChooser(lastViewedDirectory);
        int result = fileChooser.showSaveDialog(videWindow);
        if (result == JFileChooser.APPROVE_OPTION) {
            removeFromFileCache(currentFile);

            currentFile = fileChooser.getSelectedFile();
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
            updateWindowTitleWithFilename(currentFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Dispose of this editor window's resources, and then finally close the window. Unless it is
     * the last open editor window, in which case it then becomes an editor for a new empty file.
     */
    public void close() {
        // Dispose the current window's assets.
        undoHistory.clear();
        undoHistory.setEnabled(false);
        fileContents = "";
        textEditorPane.setText(fileContents);
        undoHistory.setEnabled(true);

        // Clear the file info, and the entry in the file cache.
        removeFromFileCache(currentFile);
        currentFile = null;

        // Dispose of the window if it is not the last one.
        if (ALL_OPEN_WINDOWS.size() > 1) {
            videWindow.setVisible(false);
            videWindow.dispose();
            ALL_OPEN_WINDOWS.remove(this);
        }

        // Otherwise, reuse the final window as a new file editor, instead.
        else {
            videWindow.setTitle(NEW_FILE_TITLE);
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
        videWindow.setTitle("VIDE - " + quotedFilename);
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
     * Performs the "Undo" menu item action.
     */
    public void undo() {
        if (undoHistory.canUndo()) {
            undoHistory.undo();
        }
    }

    /**
     * Performs the "Redo" menu item action.
     */
    public void redo() {
        if (undoHistory.canRedo()) {
            undoHistory.redo();
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

    public int getIndexOfNewlineBefore(int position) {
        if (!fileContents.isEmpty()) {
            for (int i = position - 1; i >= 0; i--) {
                char c = fileContents.charAt(i);
                if (c == '\n') {
                    return i + 1;
                }
            }
        }
        return 0;
    }

    public int getIndexOfNewlineAfter(int position) {
        int index = fileContents.indexOf('\n', position);
        if (index == -1) {
            index = fileContents.length();
        } else if (fileContents.charAt(position) == '\n') {
            return position;
        }
        return index;
    }

    public void syntaxHighlightCurrentLine(int offset) {
        int startIndex = getIndexOfNewlineBefore(offset);
        int endIndex = getIndexOfNewlineAfter(offset);

        SwingUtilities.invokeLater(() -> {
            try {
                syntaxHighlighter.highlight(startIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                // Silently fail because too many removal updates were applied all at once to
                // the editorTextPane. (Can't syntax-highlight regions that no longer exist.)
            }
        });
    }

    public void syntaxHighlightRegion(int startRegion, int endRegion) {
        int startIndex = getIndexOfNewlineBefore(startRegion);
        int endIndex = getIndexOfNewlineAfter(endRegion);

        SwingUtilities.invokeLater(() -> {
            try {
                syntaxHighlighter.highlight(startIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                // Silently fail because too many removal updates were applied all at once to
                // the editorTextPane. (Can't syntax-highlight regions that no longer exist.)
            }
        });
    }

    public void addInsertTextUndoHistoryItem(int offset, int length, String addedText) {
        int startCursor = offset;
        int endCursor = offset + length;

        UndoHistoryItem newInsertItem = new UndoHistoryItem(UndoHistoryItemType.INSERT_TEXT,
                startCursor, endCursor, addedText);
        undoHistory.addHistoryItem(newInsertItem);
    }

    public void addRemoveTextUndoHistoryItem(int offset, int length, String removedText) {
        int startCursor = offset;
        int endCursor = offset + length;

        UndoHistoryItem newInsertItem = new UndoHistoryItem(UndoHistoryItemType.REMOVE_TEXT,
                startCursor, endCursor, removedText);
        undoHistory.addHistoryItem(newInsertItem);
    }
}
