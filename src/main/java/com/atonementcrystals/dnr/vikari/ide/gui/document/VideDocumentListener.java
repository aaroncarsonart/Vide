package com.atonementcrystals.dnr.vikari.ide.gui.document;

import com.atonementcrystals.dnr.vikari.ide.gui.VideEditorWindow;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * For structuring a specified order of all DocumentListeners that depend on a given order of execution.
 * (Because of modifying fields in VideEditorWindow.
 */
public class VideDocumentListener implements DocumentListener {
    private final UndoHistoryDocumentListener undoHistoryListener;
    private final SyntaxHighlightDocumentListener syntaxHighlightListener;
    private final LineNumbersDocumentListener lineNumbersListener;
    private final VideEditorWindow videEditorWindow;

    public VideDocumentListener(VideEditorWindow videEditorWindow) {
        undoHistoryListener = new UndoHistoryDocumentListener(videEditorWindow);
        syntaxHighlightListener = new SyntaxHighlightDocumentListener(videEditorWindow);
        lineNumbersListener = new LineNumbersDocumentListener(videEditorWindow);
        this.videEditorWindow = videEditorWindow;
    }

    public SyntaxHighlightDocumentListener getSyntaxHighlightListener() {
        return syntaxHighlightListener;
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        lineNumbersListener.changedUpdate(e);
    }


    @Override
    public void insertUpdate(DocumentEvent e) {
        videEditorWindow.setEdited(true);
        undoHistoryListener.insertUpdate(e);
        syntaxHighlightListener.insertUpdate(e);
        lineNumbersListener.insertUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        videEditorWindow.setEdited(true);
        undoHistoryListener.removeUpdate(e);
        syntaxHighlightListener.removeUpdate(e);
        lineNumbersListener.removeUpdate(e);
    }
}
