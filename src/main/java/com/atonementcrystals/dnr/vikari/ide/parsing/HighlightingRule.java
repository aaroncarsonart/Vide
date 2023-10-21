package com.atonementcrystals.dnr.vikari.ide.parsing;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.Color;
import java.util.regex.Pattern;

public class HighlightingRule {
    private String name;
    private AttributeSet attributeSet;
    private Pattern regexPattern;

    public HighlightingRule(String name, Pattern regexPattern, Color color) {
        this.name = name;
        this.regexPattern = regexPattern;

        StyleContext styleContext = StyleContext.getDefaultStyleContext();
        this.attributeSet = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
    }

    public AttributeSet getAttributeSet() {
        return attributeSet;
    }

    public Pattern getRegexPattern() {
        return regexPattern;
    }
}
