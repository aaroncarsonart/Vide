package com.atonementcrystals.dnr.vikari.ide.gui.document;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Automatically replace typed tab characters with two spaces instead.
 */
public class TabsToSpacesDocumentFilter extends DocumentFilter {
    // TODO: replace this hard-coded value with a configurable setting.
    private static final String TWO_SPACES = "  ";

    @Override
    public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        // TODO: ensure insertion of tabs occurs on tabular boundaries.
        string = string.replace("\t", TWO_SPACES);
        super.insertString(fb, offset, string, attr);
    }

    @Override
    public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
        // TODO: add additional deletes for whitespaces on tabular boundaries.
        super.remove(fb, offset, length);
    }

    @Override
    public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        text = text.replace("\t", TWO_SPACES);
        super.replace(fb, offset, length, text, attrs);
    }
}