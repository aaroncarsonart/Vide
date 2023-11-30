package com.atonementcrystals.dnr.vikari.ide.parsing;

import java.awt.Color;

/**
 * Holds color settings for displaying a VideEditorWindow.
 */
public class VideEditorTheme {
    private VideColorTheme.WindowColorTheme windowColorTheme;
    private Color caretColor;
    private Color highlightColor;
    private Color textEditorFg;
    private Color textEditorBg;
    private Color lineNumbersFg;
    private Color lineNumbersBg;
    private Color statusLabelBg;
    private Color statusLabelFg;

    public VideColorTheme.WindowColorTheme getWindowColorTheme() {
        return windowColorTheme;
    }

    public Color getCaretColor() {
        return caretColor;
    }

    public void setCaretColor(Color caretColor) {
        this.caretColor = caretColor;
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
    }

    public void setWindowColorTheme(VideColorTheme.WindowColorTheme windowColorTheme) {
        this.windowColorTheme = windowColorTheme;
    }

    public Color getTextEditorFg() {
        return textEditorFg;
    }

    public void setTextEditorFg(Color textEditorFg) {
        this.textEditorFg = textEditorFg;
    }

    public Color getTextEditorBg() {
        return textEditorBg;
    }

    public void setTextEditorBg(Color textEditorBg) {
        this.textEditorBg = textEditorBg;
    }

    public Color getLineNumbersFg() {
        return lineNumbersFg;
    }

    public void setLineNumbersFg(Color lineNumbersFg) {
        this.lineNumbersFg = lineNumbersFg;
    }

    public Color getLineNumbersBg() {
        return lineNumbersBg;
    }

    public void setLineNumbersBg(Color lineNumbersBg) {
        this.lineNumbersBg = lineNumbersBg;
    }

    public Color getStatusLabelBg() {
        return statusLabelBg;
    }

    public void setStatusLabelBg(Color statusLabelBg) {
        this.statusLabelBg = statusLabelBg;
    }

    public Color getStatusLabelFg() {
        return statusLabelFg;
    }

    public void setStatusLabelFg(Color statusLabelFg) {
        this.statusLabelFg = statusLabelFg;
    }
}
