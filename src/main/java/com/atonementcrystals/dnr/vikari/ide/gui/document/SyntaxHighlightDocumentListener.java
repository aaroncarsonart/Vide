package com.atonementcrystals.dnr.vikari.ide.gui.document;

import com.atonementcrystals.dnr.vikari.ide.Vide;
import com.atonementcrystals.dnr.vikari.ide.gui.VideEditorPane;
import com.atonementcrystals.dnr.vikari.ide.gui.VideEditorWindow;
import com.atonementcrystals.dnr.vikari.ide.parsing.VikariSyntaxHighlighter;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * For managing updates related to the VideSyntaxHighlighter component of a VideEditorWindow.
 */
public class SyntaxHighlightDocumentListener implements DocumentListener {
    private final VideEditorWindow videEditorWindow;
    private final VideEditorPane textEditorPane;
    private VikariSyntaxHighlighter syntaxHighlighter;

    public SyntaxHighlightDocumentListener(VideEditorWindow videEditorWindow) {
        this.videEditorWindow = videEditorWindow;
        this.textEditorPane = videEditorWindow.getTextEditorPane();
        this.syntaxHighlighter = videEditorWindow.getVikariSyntaxHighlighter();
    }

    public void setSyntaxHighlighter(VikariSyntaxHighlighter syntaxHighlighter) {
        this.syntaxHighlighter = syntaxHighlighter;
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
    }

    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        String text = textEditorPane.getText();
        videEditorWindow.setFileContents(text);

        if (syntaxHighlighter.isEnabled()) {
            int offset = documentEvent.getOffset();
            int length = documentEvent.getLength();

            String filePath = videEditorWindow.getCurrentFilePath();

            SwingUtilities.invokeLater(() -> {
                syntaxHighlighter.highlightRegion(filePath, text, offset, length, textEditorPane);
            });
        }
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        if (syntaxHighlighter.isEnabled()) {
            String text = textEditorPane.getText();
            videEditorWindow.setFileContents(text);

            int offset = documentEvent.getOffset();
            int length = documentEvent.getLength();
            String filePath = videEditorWindow.getCurrentFilePath();

            SwingUtilities.invokeLater(() -> {
                // Use a negative length to model removal of text.
                syntaxHighlighter.highlightRegion(filePath, text, offset, -length, textEditorPane);
            });
        } else {
            videEditorWindow.setFileContents(textEditorPane.getText());
        }
    }
}
