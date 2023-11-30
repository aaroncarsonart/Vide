package com.atonementcrystals.dnr.vikari.ide.parsing;

import com.atonementcrystals.dnr.vikari.core.crystal.identifier.TokenType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;

/**
 * Holds color info for the syntax highlighter and UI as specified in a JSON config file.
 */
public class VideColorTheme {

    /**
     * The path for the JSON file from which this VideColorTheme was loaded.
     */
    @JsonIgnore
    private String filePath;

    /**
     * The name of the color theme.
     */
    private String name;

    /**
     * Custom color name mappings. (If hardcoded values are not preferred.)
     */
    private LinkedHashMap<String, VideColor> colors;

    /**
     * The set of all syntax highlighting rule mappings between TokenTypes and colors.
     */
    private LinkedHashMap<Rule, VideColor> rules;

    /**
     * General color settings for the editor window.
     */
    private Editor editor;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    public LinkedHashMap<String, VideColor> getColors() {
        return colors;
    }

    @SuppressWarnings("unused")
    public void setColors(LinkedHashMap<String, VideColor> colors) {
        this.colors = colors;
    }

    public LinkedHashMap<Rule, VideColor> getRules() {
        return rules;
    }

    @SuppressWarnings("unused")
    public void setRules(LinkedHashMap<Rule, VideColor> rules) {
        this.rules = rules;
    }

    public Editor getEditor() {
        return editor;
    }

    @SuppressWarnings("unused")
    public void setEditor(Editor editor) {
        this.editor = editor;
    }

    /**
     * Specifies a syntax highlighting rule for a VideColorTheme. It can be either a {@link TokenType} for overriding a
     * specific TokenType's color values, or an {@link IdentifierType} for specifying a general class of identifiers
     * against which to provide a syntax highlighting rule for.
     */
    public static class Rule {
        private TokenType tokenType = null;
        private IdentifierType identifierType = null;
        private String errorRuleName = null;

        @JsonCreator
        public Rule(String value) {
            // First, attempt to parse as an IdentifierType.
            try {
                identifierType = IdentifierType.valueOf(value);
            } catch (IllegalArgumentException e1) {

                // If that fails, then attempt to parse as a TokenType.
                try {
                    tokenType = TokenType.valueOf(value);
                } catch (IllegalArgumentException e2) {
                    // Ignore. The error will be caught at a later time when the Rules are validated.
                    errorRuleName = value;
                }
            }
        }

        public TokenType getTokenType() {
            return tokenType;
        }

        public IdentifierType getIdentifierType() {
            return identifierType;
        }

        public String getErrorRuleName() {
            return errorRuleName;
        }

        @Override
        public String toString() {
            if (tokenType != null) {
                return String.format("Rule: {TokenType: \"%s\"}", tokenType.name());
            }
            if (identifierType != null) {
                return String.format("Rule: {identifierType: \"%s\"}", identifierType.name());
            }
            return "Rule: {}";
        }
    }

    /**
     * Class to hold editor color settings.
     */
    public static class Editor {
        private WindowColorTheme windowColorTheme;
        private VideColor caretColor;
        private VideColor highlightColor;
        private VideColor textEditorFg;
        private VideColor textEditorBg;
        private VideColor lineNumbersFg;
        private VideColor lineNumbersBg;
        private VideColor statusLabelBg;
        private VideColor statusLabelFg;

        public VideColor getCaretColor() {
            return caretColor;
        }

        @SuppressWarnings("unused")
        public void setCaretColor(VideColor caretColor) {
            this.caretColor = caretColor;
        }

        public VideColor getHighlightColor() {
            return highlightColor;
        }

        @SuppressWarnings("unused")
        public void setHighlightColor(VideColor highlightColor) {
            this.highlightColor = highlightColor;
        }

        public WindowColorTheme getWindowColorTheme() {
            return windowColorTheme;
        }

        @SuppressWarnings("unused")
        public void setWindowColorTheme(WindowColorTheme windowColorTheme) {
            this.windowColorTheme = windowColorTheme;
        }

        public VideColor getTextEditorFg() {
            return textEditorFg;
        }

        @SuppressWarnings("unused")
        public void setTextEditorFg(VideColor textEditorFg) {
            this.textEditorFg = textEditorFg;
        }

        public VideColor getTextEditorBg() {
            return textEditorBg;
        }

        @SuppressWarnings("unused")
        public void setTextEditorBg(VideColor textEditorBg) {
            this.textEditorBg = textEditorBg;
        }

        public VideColor getLineNumbersFg() {
            return lineNumbersFg;
        }

        @SuppressWarnings("unused")
        public void setLineNumbersFg(VideColor lineNumbersFg) {
            this.lineNumbersFg = lineNumbersFg;
        }

        public VideColor getLineNumbersBg() {
            return lineNumbersBg;
        }

        @SuppressWarnings("unused")
        public void setLineNumbersBg(VideColor lineNumbersBg) {
            this.lineNumbersBg = lineNumbersBg;
        }

        public VideColor getStatusLabelBg() {
            return statusLabelBg;
        }

        @SuppressWarnings("unused")
        public void setStatusLabelBg(VideColor statusLabelBg) {
            this.statusLabelBg = statusLabelBg;
        }

        public VideColor getStatusLabelFg() {
            return statusLabelFg;
        }

        @SuppressWarnings("unused")
        public void setStatusLabelFg(VideColor statusLabelFg) {
            this.statusLabelFg = statusLabelFg;
        }
    }

    /**
     * Enum for specifying the OS's color theme.
     */
    public enum WindowColorTheme {
        LIGHT_MODE, DARK_MODE;
    }
}
