package com.atonement.crystals.dnr.vikari.ide.util;

import javax.swing.text.StyledDocument;
import javax.swing.text.html.MinimalHTMLWriter;
import java.io.Writer;

/**
 * Minimizes output html by removing line spacing and indentation.
 * This corrects formatting of sequential span elements to prevent
 * additional spaces from being rendered as they are formatted by
 * the default output of HTMLEditorKit.
 */
public class CustomHTMLWriter extends MinimalHTMLWriter {

    public CustomHTMLWriter(Writer writer, StyledDocument document, int position, int length) {
        super(writer, document, position, length);
    }

    @Override
    public String getLineSeparator() {
        return "";
    }

    @Override
    protected int getIndentSpace() {
        return 0;
    }
}
