package com.atonementcrystals.dnr.vikari.ide.parsing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single color definition in a {@link VideColorTheme}'s JSON config file.
 * It is either defined in terms of an RGB value, a hex value, or a previously defined color's name.
 */
public class VideColor {
    public static final int UNDEFINED_RGB_VALUE = -1;

    private int r = UNDEFINED_RGB_VALUE;
    private int g = UNDEFINED_RGB_VALUE;
    private int b = UNDEFINED_RGB_VALUE;

    private String hex = null;
    private String colorName = null;

    /**
     * Create a new VideColor with the given RGB valued.
     * @param r The red value.
     * @param g The green value.
     * @param b The blue value.
     */
    @JsonCreator
    public VideColor(@JsonProperty("r") int r, @JsonProperty("g") int g, @JsonProperty("b") int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Create a new VideColor as initialized by the given string value.
     * @param str Either a hex value starting with #, or a previously defined color's name.
     */
    @JsonCreator
    public VideColor(String str) {
        if (str.startsWith("#")) {
            this.hex = str;
        } else {
            this.colorName = str;
        }
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    public String getHex() {
        return hex;
    }

    public String getColorName() {
        return colorName;
    }

    @Override
    public String toString() {
        if (colorName != null) {
            return String.format("VideColor: {colorName: \"%s\"}", colorName);
        }
        if (hex != null) {
            return String.format("VideColor: {hex: \"%s\"}", hex);
        }
        return String.format("VideColor: {r: %d, g: %d, b: %d}", r, g, b);
    }
}
