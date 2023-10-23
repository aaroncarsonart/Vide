package com.atonementcrystals.dnr.vikari.ide.gui.document;

import com.atonementcrystals.dnr.vikari.ide.gui.VideEditorPane;
import com.atonementcrystals.dnr.vikari.ide.gui.VideEditorWindow;
import com.atonementcrystals.dnr.vikari.ide.undo.UndoHistory;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

/**
 * For managing updates related to the SyntaxHighlighter and UndoHistory components of a
 * VideEditorWindow.
 */
public class SyntaxHighlightUndoHistoryDocumentListener implements DocumentListener {
    private final VideEditorWindow videEditorWindow;
    private final VideEditorPane textEditorPane;
    private final UndoHistory undoHistory;

    public SyntaxHighlightUndoHistoryDocumentListener(VideEditorWindow videEditorWindow) {
        this.videEditorWindow = videEditorWindow;
        this.textEditorPane = videEditorWindow.getTextEditorPane();
        this.undoHistory = videEditorWindow.getUndoHistory();
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
    }

    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        StyledDocument document = textEditorPane.getStyledDocument();
        int offset = documentEvent.getOffset();
        int length = documentEvent.getLength();

        if (undoHistory.isEnabled()) {
            String addedText;

            try {
                addedText = document.getText(offset, length);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }

            // TODO: Add CaretListener to finalize edits when no text is added or removed.
            // TODO: Detect edits greater than length 1 and finalize previous edit before adding them.
            videEditorWindow.addInsertTextUndoHistoryItem(offset, length, addedText);
        }
        // syntax color highlighting
        videEditorWindow.setFileContents(textEditorPane.getText());
        videEditorWindow.syntaxHighlightRegion(offset, offset + length);
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        if (undoHistory.isEnabled()) {
            int startIndex = documentEvent.getOffset();
            int length = documentEvent.getLength();
            int endIndex = startIndex + length;
            String removedText;

            try {
                String fileContents = videEditorWindow.getFileContents();
                removedText = fileContents.substring(startIndex, endIndex);
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException(e);
            }

            // TODO: Add CaretListener to finalize edits when no text is added or removed.
            // TODO: Detect edits greater than length 1 and finalize previous edit before adding them.
            videEditorWindow.addRemoveTextUndoHistoryItem(startIndex, length, removedText);
        }
        // syntax color highlighting
        videEditorWindow.setFileContents(textEditorPane.getText());
        videEditorWindow.syntaxHighlightCurrentLine(documentEvent.getOffset());
    }
}
