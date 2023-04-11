package com.atonement.crystals.dnr.vikari.ide.gui;

import com.atonement.crystals.dnr.vikari.ide.parsing.SyntaxHighlighter;
import com.atonement.crystals.dnr.vikari.ide.undo.UndoHistory;
import com.atonement.crystals.dnr.vikari.ide.undo.UndoHistoryItem;
import com.atonement.crystals.dnr.vikari.ide.undo.UndoHistoryItemType;
import com.atonement.crystals.dnr.vikari.ide.util.CustomHTMLWriter;
import com.atonement.crystals.dnr.vikari.ide.util.HTMLTransferable;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VideMainWindow {

    private JFrame videWindow;
    private JScrollPane editorScrollPane;
    private VideTextPane editorTextPane;
    private JTextArea lineNumbers;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu viewMenu;
    private Font font;
    int fontWidth;
    int fontHeight;

    private File currentFile;
    private String fileContents;

    private SyntaxHighlighter syntaxHighlighter;
    private UndoHistory undoHistory;

    public VideMainWindow() {
        videWindow = new JFrame("VIDE");
//        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        videWindow.setSize(screenSize.width, screenSize.height);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        videWindow.setSize(screenSize.width / 3 * 2, screenSize.height / 3 * 2);

        font = new Font("Andale Mono", Font.PLAIN, 18);

        editorTextPane = new VideTextPane();
        editorTextPane.setEditable(true);
        editorTextPane.setFont(font);

        undoHistory = new UndoHistory(editorTextPane.getStyledDocument());
        syntaxHighlighter = new SyntaxHighlighter(editorTextPane.getStyledDocument());

        FontMetrics fontMetrics = editorTextPane.getFontMetrics(font);
        fontWidth = fontMetrics.charWidth(' ');
        fontHeight = fontMetrics.getHeight();
        editorTextPane.setBorder(BorderFactory.createEmptyBorder(0, fontWidth, 0, 0));

        // replace tabs with spaces
        ((DefaultStyledDocument) editorTextPane.getDocument()).setDocumentFilter(new DocumentFilter() {
            private String tabString = "  ";

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
                // TODO: ensure insertion of tabs occurs on tabular boundaries.
                string = string.replace("\t", tabString);
                super.insertString(fb, offset, string, attr);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                // TODO: add additional deletes for whitespaces on tabular boundaries.
                super.remove(fb, offset, length);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
                text = text.replace("\t", tabString);
                super.replace(fb, offset, length, text, attrs);
            }
        });

        lineNumbers = new JTextArea("1");
        lineNumbers.setBackground(new Color(245, 245, 245));
        lineNumbers.setForeground(Color.GRAY);
        lineNumbers.setEditable(false);
        lineNumbers.setEnabled(false);
        lineNumbers.setFont(font);
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, fontWidth));

        // manage updating of line numbers
        editorTextPane.getDocument().addDocumentListener(new DocumentListener() {
            public String getLineNumbersText() {

                // count number of lines
                int lineCount = 1;
                String editorText = editorTextPane.getText();
                Pattern newlinePattern = Pattern.compile("\n");
                Matcher matcher = newlinePattern.matcher(editorText);
                while (matcher.find()) {
                    lineCount++;
                }

                String text = "1" + System.getProperty("line.separator");
                for(int i = 2; i <= lineCount; i++) {
                    text += i + System.getProperty("line.separator");
                }
                return text;
            }
            int counter = 0;
            @Override
            public void changedUpdate(DocumentEvent de) {
//                System.out.println(counter++ + "CHANGED Line numbers");
                lineNumbers.setText(getLineNumbersText());

                int caretPosition = editorTextPane.getCaretPosition();
                Element root = editorTextPane.getDocument().getDefaultRootElement();
                int lineNumberCaretPosition = root.getElementIndex(caretPosition);
                lineNumbers.setCaretPosition(lineNumberCaretPosition);
            }
            @Override
            public void insertUpdate(DocumentEvent de) {
//                System.out.println(counter++ + "INSERT Line numbers");
                lineNumbers.setText(getLineNumbersText());

                int caretPosition = editorTextPane.getCaretPosition();
                Element root = editorTextPane.getDocument().getDefaultRootElement();
                int lineNumberCaretPosition = root.getElementIndex(caretPosition);
                lineNumbers.setCaretPosition(lineNumberCaretPosition);
            }
            @Override
            public void removeUpdate(DocumentEvent de) {
//                System.out.println(counter++ + "REMOVE Line numbers");
                lineNumbers.setText(getLineNumbersText());

                int caretPosition = editorTextPane.getCaretPosition();
                Element root = editorTextPane.getDocument().getDefaultRootElement();
                int lineNumberCaretPosition = root.getElementIndex(caretPosition);
                lineNumbers.setCaretPosition(lineNumberCaretPosition);
            }
        });

        // manage syntax color highlighting and undo history
        editorTextPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
            }
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                System.out.println("DocumentListener::insertUpdate");

                StyledDocument document = editorTextPane.getStyledDocument();
                int offset = documentEvent.getOffset();
                int length = documentEvent.getLength();
                String addedText;

                try {
                    addedText = document.getText(offset, length);
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }

                // TODO: Add CaretListener to finalize edits when no text is added or removed.
                // TODO: Detect edits greater than length 1 and finalize previous edit before adding them.
                addInsertTextUndoHistoryItem(offset, length, addedText);

                // syntax color highlighting
                fileContents = editorTextPane.getText();
                syntaxHighlightRegion(offset, offset + length);
            }
            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                System.out.println("DocumentListener::removeUpdate");

                if (undoHistory.isEnabled()) {
                    int startIndex = documentEvent.getOffset();
                    int length = documentEvent.getLength();
                    int endIndex = startIndex + length;
                    String removedText;

                    try {
                        removedText = fileContents.substring(startIndex, endIndex);
                    } catch (IndexOutOfBoundsException e) {
                        throw new RuntimeException(e);
                    }

                    // TODO: Add CaretListener to finalize edits when no text is added or removed.
                    // TODO: Detect edits greater than length 1 and finalize previous edit before adding them.
                    addRemoveTextUndoHistoryItem(startIndex, length, removedText);
                }
                // syntax color highlighting
                fileContents = editorTextPane.getText();
                syntaxHighlightCurrentLine(documentEvent.getOffset());
            }
        });

        editorScrollPane = new JScrollPane();
        editorScrollPane.getViewport().add(editorTextPane);
        editorScrollPane.setRowHeaderView(lineNumbers);
        editorScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        editorScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JScrollBar verticalScrollBar = editorScrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(fontHeight);
        JScrollBar horizontalScrollBar = editorScrollPane.getHorizontalScrollBar();
        horizontalScrollBar.setUnitIncrement(fontWidth);

        Container contentPane = videWindow.getContentPane();
        contentPane.add(editorScrollPane, BorderLayout.CENTER);

        JMenuItem openItem = new JMenuItem("Open");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        openItem.addActionListener(event -> {
            JFileChooser fileChooser = new JFileChooser("/Users/aaron/DNR/test");
            int result = fileChooser.showOpenDialog(videWindow);
            if (result == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                loadFile(currentFile);
            }
        });
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.addActionListener(event -> {
            System.out.println("Save");
            if (currentFile == null) {
                JFileChooser fileChooser = new JFileChooser("/Users/aaron/DNR");
                int result = fileChooser.showSaveDialog(videWindow);
                if (result == JFileChooser.APPROVE_OPTION) {
                    currentFile = fileChooser.getSelectedFile();
                    saveFile(currentFile);
                }
            } else {
                saveFile(currentFile);
            }
        });
        JMenuItem saveAsItem = new JMenuItem("Save as");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | Event.SHIFT_MASK));
        saveAsItem.addActionListener(event -> {
            JFileChooser fileChooser = new JFileChooser("/Users/aaron/DNR");
            int result = fileChooser.showSaveDialog(videWindow);
            if (result == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                saveFile(currentFile);
            }
        });
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        closeItem.addActionListener(event -> {
            undoHistory.clear();
            undoHistory.setEnabled(false);
            fileContents = "";
            editorTextPane.setText(fileContents);
            undoHistory.setEnabled(true);
            lineNumbers.setText("1 ");
            currentFile = null;
            updateWindowNameWithFilename("<New File>");
        });

        fileMenu = new JMenu("File");
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(closeItem);

        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undoItem.addActionListener(event -> {
            if (undoHistory.canUndo()) {
                undoHistory.undo();
            }
        });

        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | Event.SHIFT_MASK));
        redoItem.addActionListener(event -> {
            if (undoHistory.canRedo()) {
                undoHistory.redo();
            }
        });

        JMenuItem copyFormattedTextItem = new JMenuItem("Copy Formatted Text");
        copyFormattedTextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | Event.SHIFT_MASK));
        copyFormattedTextItem.addActionListener(event -> copyHtmlFormattedTextSelectionToClipboard());

        editMenu = new JMenu("Edit");
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.add(copyFormattedTextItem);

        JMenuItem toggleWordWrapMenuItem = new JMenuItem("Toggle Word Wrap");
        toggleWordWrapMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | Event.ALT_MASK));
        toggleWordWrapMenuItem.addActionListener(event -> {
            editorTextPane.toggleWordWrap();
            editorScrollPane.setViewportView(editorTextPane);
        });

        viewMenu = new JMenu("View");
        viewMenu.add(toggleWordWrapMenuItem);

        menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);

        videWindow.setJMenuBar(menuBar);

        currentFile = new File("/Users/aaron/DNR/Test/ExampleCrystal.DNR");
        loadFile(currentFile);
    }

    public void start() {
        videWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width / 2 - videWindow.getWidth() / 2;
        int y = 0;
        videWindow.setLocation(x, y);
        //videWindow.setLocationRelativeTo(null);
        videWindow.setVisible(true);
    }

    private void loadFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            fileContents = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        undoHistory.setEnabled(false);
        editorTextPane.setText(fileContents);
        editorTextPane.setCaretPosition(0);
        undoHistory.setEnabled(true);
        updateWindowNameWithFilename(currentFile);
        syntaxHighlightEntireFile();
    }

    private void saveFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            fileContents = editorTextPane.getText();
            writer.write(fileContents);
            writer.flush();
            updateWindowNameWithFilename(currentFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateWindowNameWithFilename(File file) {
        updateWindowNameWithFilename("\"" + file.getAbsolutePath() + "\"");
    }

    private void updateWindowNameWithFilename(String filename) {
        videWindow.setTitle("VIDE - " + filename);
    }

    private void syntaxHighlightEntireFile() {
        syntaxHighlightRegion(0, fileContents.length());
    }

    private int getIndexOfNewlineBefore(int position) {
        if (!fileContents.isEmpty()) {
            for (int i = position; i >= 0; i--) {
                char c = fileContents.charAt(i);
                if (c == '\n') {
                    return i + 1;
                }
            }
        }
        return 0;
    }

    private int getIndexOfNewlineAfter(int position) {
        int index = fileContents.indexOf('\n', position);
        if (index == -1) {
            index = fileContents.length();
        } else if (fileContents.charAt(position) == '\n') {
            return position;
        }
        return index;
    }

    private void syntaxHighlightCurrentLine(int offset) {
        System.out.printf("syntaxHighlightCurrentLine(%d)\n", offset);
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

    private void syntaxHighlightRegion(int startRegion, int endRegion) {
        System.out.printf("syntaxHighlightRegion(%d, %d)\n", startRegion, endRegion);

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

    private void addInsertTextUndoHistoryItem(int offset, int length, String addedText) {
        System.out.printf("addInsertTextUndoHistoryItem(%d, %d, \"%s\")\n", offset, length, addedText);
        int startCursor = offset;
        int endCursor = offset + length;

        UndoHistoryItem newInsertItem = new UndoHistoryItem(UndoHistoryItemType.INSERT_TEXT,
                startCursor, endCursor, addedText);
        undoHistory.addHistoryItem(newInsertItem);
    }

    private void addRemoveTextUndoHistoryItem(int offset, int length, String removedText) {
        System.out.printf("addRemoveTextUndoHistoryItem(%d, %d, \"%s\")\n", offset, length, removedText);
        int startCursor = offset;
        int endCursor = offset + length;

        UndoHistoryItem newInsertItem = new UndoHistoryItem(UndoHistoryItemType.REMOVE_TEXT,
                startCursor, endCursor, removedText);
        undoHistory.addHistoryItem(newInsertItem);
    }

    public void copyHtmlFormattedTextSelectionToClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        int startIndex = editorTextPane.getSelectionStart();
        int endIndex = editorTextPane.getSelectionEnd();
        int length = endIndex - startIndex;
        StyledDocument styledDocument = editorTextPane.getStyledDocument();

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
}
