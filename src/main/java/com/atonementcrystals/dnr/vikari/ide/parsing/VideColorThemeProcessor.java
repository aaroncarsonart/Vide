package com.atonementcrystals.dnr.vikari.ide.parsing;

import com.atonementcrystals.dnr.vikari.core.crystal.AtonementCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.identifier.TokenType;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For loading a {@link VideColorTheme} into the {@link VideEditorTheme} and {@link VikariSyntaxHighlighter}.
 */
public class VideColorThemeProcessor {
    private static final Color COLOR_DEFINITION_ERROR = Color.BLACK;
    private static final Color WHITE = Color.WHITE;
    private static final Color BLACK = Color.BLACK;
    private static final Color LIGHT_GREY = new Color(245, 245, 245);
    private static final Color GREY = Color.GRAY;
    private static final Color DARK_GREY = Color.DARK_GRAY;

    private final VideColorTheme videColorTheme;
    private final Map<String, Color> namedColors;
    private final List<String> errors;

    /**
     * Load and process the specified VideColorTheme, and install the rules in the EditorTheme and VikariSyntaxHighlighter.
     * @param videColorTheme The file path to load the VideColorTheme at.
     * @param editorTheme The EditorTheme to install the VideColorTheme rules into.
     * @param syntaxHighlighter The VikariSyntaxHighlighter to install the VideColorTheme rules into.
     */
    public VideColorThemeProcessor(VideColorTheme videColorTheme, VideEditorTheme editorTheme,
                                   VikariSyntaxHighlighter syntaxHighlighter) {
        this.videColorTheme = videColorTheme;
        namedColors = new HashMap<>();
        Map<Class<? extends AtonementCrystal>, Color> definedRules = new HashMap<>();
        Map<IdentifierType, Color> overloadRules = new HashMap<>();

        // Load color definitions from VideColorTheme.
        errors = new ArrayList<>();
        Map<String, VideColor> colorDefinitions = videColorTheme.getColors();
        for (Map.Entry<String, VideColor> pair : colorDefinitions.entrySet()) {
            String name = pair.getKey();
            VideColor videColor = pair.getValue();

            String errorMessagePrefix = "\"colors." + name + "\"";
            Color awtColor = getAwtColor(videColor, errorMessagePrefix, errors);
            namedColors.put(name, awtColor);
        }

        // Fetch the defined color theme rules for the syntax highlighter.
        Map<VideColorTheme.Rule, VideColor> colorThemeRules = videColorTheme.getRules();
        for (Map.Entry<VideColorTheme.Rule, VideColor> pair : colorThemeRules.entrySet()) {
            VideColorTheme.Rule rule = pair.getKey();
            TokenType tokenType = rule.getTokenType();
            IdentifierType identifierType = rule.getIdentifierType();
            String errorRuleName = rule.getErrorRuleName();

            VideColor videColor = pair.getValue();
            String errorMessagePrefix = "\"rules." + (tokenType != null ? tokenType : identifierType) + "\"";
            Color awtColor = getAwtColor(videColor, errorMessagePrefix, errors);

            // Define a rule for a single TokenType.
            if (tokenType != null) {
                definedRules.put(tokenType.getJavaType(), awtColor);
            }

            else if (identifierType != null) {
                // Define a rule for an overloaded type's TypeIdentifier.
                switch (identifierType) {
                    case VARIABLE:
                    case TYPE_KEYWORD:
                    case CONSTANT:
                    case QUOTED_IDENTIFIER:
                    case FUNCTION_DECLARATION:
                    case FUNCTION_CALL:
                    case CONSTRUCTOR_DECLARATION:
                    case CONSTRUCTOR_CALL: {
                        overloadRules.put(identifierType, awtColor);
                        break;
                    }

                    // Define an ordinary rule for an IdentifierType's mapped group of types.
                    default: {
                        List<Class<? extends AtonementCrystal>> mappedTypes = VikariSyntaxHighlighter.IDENTIFIER_TYPE_MAPPINGS.get(identifierType);
                        for (Class<? extends AtonementCrystal> type : mappedTypes) {
                            definedRules.put(type, awtColor);
                        }
                        break;
                    }
                }
            }

            else if (errorRuleName != null) {
                errors.add("\"" + errorRuleName + "\" is not a valid rule.");
            }
        }

        // Install the defined color theme rules for the syntax highlighter.
        syntaxHighlighter.setDefinedRules(definedRules);
        syntaxHighlighter.setOverloadRules(overloadRules);

        // Fetch the color theme rules for the editor.
        VideColorTheme.Editor editorConfig = videColorTheme.getEditor();
        VideColorTheme.WindowColorTheme windowColorTheme = VideColorTheme.WindowColorTheme.LIGHT_MODE;
        if (editorConfig.getWindowColorTheme() != null) {
            windowColorTheme = editorConfig.getWindowColorTheme();
        };
        Color caretColor = getAwtColor(editorConfig.getCaretColor(), "editor.caretColor", errors, BLACK);
        Color highlightColor = getAwtColor(editorConfig.getHighlightColor(), "editor.highlightColor", errors, LIGHT_GREY);

        Color textEditorFg = getAwtColor(editorConfig.getTextEditorFg(), "editor.textEditorFg", errors, WHITE);
        Color textEditorBg = getAwtColor(editorConfig.getTextEditorBg(), "editor.textEditorBg", errors, BLACK);
        Color lineNumbersFg = getAwtColor(editorConfig.getLineNumbersFg(), "editor.lineNumbersFg", errors, LIGHT_GREY);
        Color lineNumbersBg = getAwtColor(editorConfig.getLineNumbersBg(), "editor.lineNumbersBg", errors, GREY);
        Color statusLabelFg = getAwtColor(editorConfig.getStatusLabelFg(), "editor.statusLabelFg", errors, DARK_GREY);
        Color statusLabelBg = getAwtColor(editorConfig.getStatusLabelBg(), "editor.statusLabelBg", errors, WHITE);

        // Install the color theme for the editor.
        editorTheme.setWindowColorTheme(windowColorTheme);
        editorTheme.setCaretColor(caretColor);
        editorTheme.setHighlightColor(highlightColor);
        editorTheme.setTextEditorFg(textEditorFg);
        editorTheme.setTextEditorBg(textEditorBg);
        editorTheme.setLineNumbersFg(lineNumbersFg);
        editorTheme.setLineNumbersBg(lineNumbersBg);
        editorTheme.setStatusLabelFg(statusLabelFg);
        editorTheme.setStatusLabelBg(statusLabelBg);
    }

    public Map<String, Color> getNamedColors() {
        return namedColors;
    }

    public boolean hasColorDefinitionErrors() {
        return !errors.isEmpty();
    }

    public void reportColorDefinitionErrors(JFrame parentWindow) {
        String messageText = String.join("\n", errors);
        JTextArea textArea = new JTextArea(messageText);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(textArea);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        String title = String.format("\"%s\" - Color Definition Errors", videColorTheme.getFilePath());
        JFrame window = new JFrame(title);
        window.add(scrollPane, BorderLayout.CENTER);

        JButton okButton = new JButton("Ok");
        okButton.addActionListener(event -> window.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(new JPanel());
        buttonPanel.add(okButton);
        buttonPanel.add(new JPanel());

        window.add(buttonPanel, BorderLayout.SOUTH);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        window.setSize(screenSize.width / 3, screenSize.height / 3);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setLocationRelativeTo(parentWindow);
        window.setAlwaysOnTop(true);
        window.setVisible(true);
    }

    /**
     * Get the awt Color object associated with the given VideColor.
     * @param videColor The VideColor to generate the awt Color object for.
     * @param errorReportPrefix A string used to identify the location of the color definition in the JSON file.
     *                          (For error reporting purposes.)
     * @param errors The list of String error messages. (For error reporting purposes.)
     * @return The awt Color associated with the given VideColor.
     */
    private Color getAwtColor(VideColor videColor, String errorReportPrefix, List<String> errors) {
        return getAwtColor(videColor, errorReportPrefix, errors, COLOR_DEFINITION_ERROR);
    }

    /**
     * Get the awt Color object associated with the given VideColor.
     * @param videColor The VideColor to generate the awt Color object for.
     * @param errorReportPrefix A string used to identify the location of the color definition in the JSON file.
     *                          (For error reporting purposes.)
     * @param errors The list of String error messages. (For error reporting purposes.)
     * @param defaultColor The awt Color to use if there is a color definition error.
     * @return The awt Color associated with the given VideColor.
     */
    private Color getAwtColor(VideColor videColor, String errorReportPrefix, List<String> errors, Color defaultColor) {
        // The color is defined using a name referring to an existing color definition.
        if (videColor.getColorName() != null) {
            String colorName = videColor.getColorName();
            if (namedColors.containsKey(colorName)) {
                return namedColors.get(colorName);
            } else {
                errors.add(errorReportPrefix + " is defined with a color name \"" + colorName + "\" that does not exist.");
                return defaultColor;
            }
        }

        // The color is defined using a 7 digit hex literal. 9A.k.a.: "#ff0000")
        else if (videColor.getHex() != null) {
            String hex = videColor.getHex();
            if (hex.length() == 7 && hex.charAt(0) == '#') {
                try {
                    int rgb = Integer.decode(hex);
                    return new Color(rgb);
                } catch (NumberFormatException e) {
                    errors.add(errorReportPrefix + " contains invalid characters for its hex code.");
                    return defaultColor;
                }
            } else {
                errors.add(errorReportPrefix + "'s hex code is invalid. Must start with # and then have 6 digits.");
                return defaultColor;
            }
        }

        // The color is defined with RGB values.
        else {
            int r = videColor.getR();
            int g = videColor.getG();
            int b = videColor.getB();

            boolean error = false;
            if (r < 0 || r > 255) {
                error = errors.add(errorReportPrefix + " r value must be between 0 and 255.");
            }
            if (g < 0 || g > 255) {
                error = errors.add(errorReportPrefix + " g value must be between 0 and 255.");
            }
            if (b < 0 || b > 255) {
                error = errors.add(errorReportPrefix + " b value must be between 0 and 255.");
            }

            if (error) {
                return defaultColor;
            }

            return new Color(r, g, b);
        }
    }
}
