package com.atonementcrystals.dnr.vikari.ide.parsing;

import com.atonementcrystals.dnr.vikari.core.crystal.MultilineToken;

import java.util.Objects;

/**
 * Represents a distinct region of a Vikari code file. Modeled as a pair of line numbers.
 */
public class Region implements Comparable<Region> {
    private int start;
    private int end;
    private final Class<? extends MultilineToken> tokenType;

    public Region(int start, int end, Class<? extends MultilineToken> tokenType) {
        this.start = start;
        this.end = end;
        this.tokenType = tokenType;
        if (start >= end) {
            throw new IllegalStateException("Region's start value must be less than its end value.");
        }
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public Class<? extends MultilineToken> getTokenType() {
        return tokenType;
    }

    public void update(int offset) {
        start += offset;
        end += offset;
    }

    public boolean intersects(Region other) {
        if (this.start <= other.start && other.start < this.end) {
            return true;
        } else return other.start <= this.start && this.start < other.end;
    }

    /**
     * Regions ordinarily will never intersect. Because the TreeMap they are stored in always stores non-overlapping
     * Regions, each representing a different sequential multiline token. So if a smaller Region is intersecting another
     * Region's bounds, it will return zero from this method. This way {@link java.util.Map#remove(Object)} can be used
     * to search the TreeMap for a match of all Regions intersecting an edit. This is performant for the algorithm for
     * processing single-line edits.
     * @param other The other Region to be compared.
     * @return Zero if the region intersect. Otherwise, it returns the difference found between either the startRow
     * values, or the endRow values (if the startRow values are found to be equal).
     */
    @Override
    public int compareTo(Region other) {
        if (this.intersects(other)) {
            return 0;
        }
        int startDifference = this.start - other.start;
        if (startDifference != 0) {
            return startDifference;
        }
        return this.end - other.end;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Region region) {
            return start == region.start && end == region.end && tokenType == region.tokenType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, tokenType);
    }

    /**
     * For debuging purposes.
     * @return A string representation of this class's fields.
     */
    @Override
    public String toString() {
        return "{ start = " + start + ", end = " + end + ", tokenType = " + tokenType.getSimpleName() + " }";
    }
}
