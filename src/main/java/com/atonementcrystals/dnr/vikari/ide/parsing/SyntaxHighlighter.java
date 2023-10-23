package com.atonementcrystals.dnr.vikari.ide.parsing;

import com.atonementcrystals.dnr.vikari.core.crystal.identifier.TokenType;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * For quickly determined syntax highlighting rules. To be applied before the Vikari interpreter
 * pass can lex and parse the code to make the syntax highlighting exact.
 */
public class SyntaxHighlighter {
    private final StyledDocument textDocument;
    private final List<HighlightingRule> highlightingRules;

    public SyntaxHighlighter(StyledDocument textDocument) {
        this.textDocument = textDocument;
        this.highlightingRules = initializeHighlightingRules();
    }

    private List<HighlightingRule> initializeHighlightingRules() {
        List<HighlightingRule> highlightingRules = new ArrayList<>();

        // TODO: Fix type highlighting for package names between ::
        // TODO: with lookahead/lookbehinds.
        highlightingRules.add(new HighlightingRule("Reset", Pattern.compile(".*"), Color.BLACK));
        highlightingRules.add(new HighlightingRule("Key-value pair", Pattern.compile(Pattern.quote("->")), Colors.BLUE));

        String binaryOperatorsRegex = Arrays.asList(TokenType.MODULUS, TokenType.MULTIPLY, TokenType.LEFT_DIVIDE,
                        TokenType.ADD, TokenType.SUBTRACT, TokenType.LEFT_ASSIGNMENT, TokenType.LEFT_ADD_ASSIGNMENT,
                        TokenType.LEFT_SUBTRACT_ASSIGNMENT, TokenType.LEFT_DIVIDE_ASSIGNMENT, TokenType.RIGHT_DIVIDE,
                        TokenType.LEFT_MULTIPLY_ASSIGNMENT, TokenType.LEFT_LOGICAL_OR_ASSIGNMENT,
                        TokenType.LEFT_LOGICAL_AND_ASSIGNMENT, TokenType.LOGICAL_AND, TokenType.LOGICAL_OR,
                        TokenType.LOGICAL_NOT, TokenType.EQUALS, TokenType.GREATER_THAN, TokenType.LESS_THAN,
                        TokenType.GREATER_THAN_OR_EQUALS, TokenType.LESS_THAN_OR_EQUALS, TokenType.RETURN,
                        TokenType.CONTINUE, TokenType.BREAK)
                .stream()
                .map(TokenType::getIdentifier)
                .map(Pattern::quote)
                .collect(Collectors.joining("|","(?:", ")"));

        highlightingRules.add(new HighlightingRule("Binary Operators", Pattern.compile(binaryOperatorsRegex), Colors.RED));
        highlightingRules.add(new HighlightingRule("Constructors", Pattern.compile("(?:\\*(?=.*\\s*<<\\s*\\(.*\\)\\s*::)|(?<=<<)\\s*\\*)"), Color.BLACK));
        highlightingRules.add(new HighlightingRule("Keywords", Pattern.compile("(?:\\?|--|\\+\\+|<>)"), Colors.ORANGE));
        highlightingRules.add(new HighlightingRule("Numbers", Pattern.compile("\\d+(?:\\.\\d+)?(?i)[LFDB]?"), Colors.PURPLE));
        highlightingRules.add(new HighlightingRule("Identifiers", Pattern.compile("[a-z]\\w+"), Color.BLACK));
        highlightingRules.add(new HighlightingRule("Booleans", Pattern.compile("(?:true|false)"), Colors.PURPLE));
        highlightingRules.add(new HighlightingRule("Index Operators", Pattern.compile("\\$\\w+"), Colors.GREEN));
        highlightingRules.add(new HighlightingRule("Access Operators", Pattern.compile("(?:@\\w*|#\\w*)"), Colors.BLUE));
        highlightingRules.add(new HighlightingRule("Delete Operators", Pattern.compile("~\\w*"), Colors.ORANGE));
        highlightingRules.add(new HighlightingRule("Types", Pattern.compile("(?<!\\w)[A-Z]\\w+"), Colors.GREEN));
        highlightingRules.add(new HighlightingRule("Type Keyword", Pattern.compile("(?<!\\w)(Type|AbstractType" +
                "|Interface|Enum|Record|Library|TestSuite)(?!\\w)"), Colors.BLUE));
        highlightingRules.add(new HighlightingRule("Parameterized Types", Pattern.compile("(?<=\\w)(?:\\[|:\\[).*\\]"), Colors.GREEN));
        highlightingRules.add(new HighlightingRule("Separators", Pattern.compile("[(|)\\[\\]]"), Colors.BLUE));
        highlightingRules.add(new HighlightingRule("Function Declarations", Pattern.compile("\\w+(?=.*\\s*<<\\s*\\(.*\\)\\s*::)"), Colors.GREEN));
        highlightingRules.add(new HighlightingRule("Function Calls", Pattern.compile("\\w+(?=!)"), Colors.GREEN));
        highlightingRules.add(new HighlightingRule("Punctuation", Pattern.compile("[.,:;!&]"), Colors.ORANGE));
        highlightingRules.add(new HighlightingRule("Curly Braces", Pattern.compile("[{}]"), Colors.MAROON));
        highlightingRules.add(new HighlightingRule("Constants", Pattern.compile("(?<=[{])[^{}]+(?=[}])"), Colors.PINK));
        highlightingRules.add(new HighlightingRule("Annotations", Pattern.compile("\\$\\{[A-Z]\\w*[^\\}]*\\}"), Colors.MAROON));
        highlightingRules.add(new HighlightingRule("Characters", Pattern.compile("(?<!`)(?:`\\\\.`|`[^`\\\\]`)(?!`)"), Colors.PURPLE));
        highlightingRules.add(new HighlightingRule("Quoted Identifiers", Pattern.compile("(?<!(`|`.))`[^`][^`]+`"), Colors.BLUE));
        highlightingRules.add(new HighlightingRule("Strings", Pattern.compile("``(?:\\\\.|[^`\\\\]|`(?!`))*``"), Colors.PURPLE));
        highlightingRules.add(new HighlightingRule("Comments", Pattern.compile("~:(?::(?!~)|[^:])*(?::~|$)"), Colors.BLUE));
        highlightingRules.add(new HighlightingRule("Break Operator", Pattern.compile("\\bvv\\b"), Colors.RED));

        return highlightingRules;
    }

    // TODO: Add multi-line pattern detection.

    public void highlight(int startRegion, int endRegion) {
        String text;
        try {
            text = textDocument.getText(0, textDocument.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }

        for (HighlightingRule rule : highlightingRules) {
            Pattern pattern = rule.getRegexPattern();
            Matcher matcher = pattern.matcher(text);
            matcher = matcher.region(startRegion, endRegion);
            AttributeSet attributeSet = rule.getAttributeSet();
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                int length = end - start;
                textDocument.setCharacterAttributes(start, length, attributeSet, false);
            }
        }
    }
}
