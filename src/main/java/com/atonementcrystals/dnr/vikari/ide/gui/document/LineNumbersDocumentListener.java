package com.atonementcrystals.dnr.vikari.ide.gui.document;

import com.atonementcrystals.dnr.vikari.ide.gui.VideEditorPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For managing the updating line numbers in a VideEditorWindow.
 */
public class LineNumbersDocumentListener implements DocumentListener {
    private final VideEditorPane textEditorPane;
    private final JTextArea lineNumbers;

    public LineNumbersDocumentListener(VideEditorPane textEditorPane, JTextArea lineNumbers) {
        this.textEditorPane = textEditorPane;
        this.lineNumbers = lineNumbers;
    }

    public String getLineNumbersText() {

        // count number of lines
        int lineCount = 1;
        String editorText = textEditorPane.getText();
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

    private void updateLineNumbers() {
        lineNumbers.setText(getLineNumbersText());

        int caretPosition = textEditorPane.getCaretPosition();
        Element root = textEditorPane.getDocument().getDefaultRootElement();
        int lineNumberCaretPosition = root.getElementIndex(caretPosition);
        lineNumbers.setCaretPosition(lineNumberCaretPosition);
    }

    @Override
    public void changedUpdate(DocumentEvent de) {
        updateLineNumbers();
    }

    @Override
    public void insertUpdate(DocumentEvent de) {
        updateLineNumbers();
    }

    @Override
    public void removeUpdate(DocumentEvent de) {
        updateLineNumbers();
    }
}