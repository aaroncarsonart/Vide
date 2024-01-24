package com.atonementcrystals.dnr.vikari.ide.parsing;

import com.atonementcrystals.dnr.vikari.core.crystal.AtonementCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.CommentCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.MultiLineCommentCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.MultilineToken;
import com.atonementcrystals.dnr.vikari.core.crystal.identifier.ReferenceCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.identifier.TokenType;
import com.atonementcrystals.dnr.vikari.core.crystal.identifier.TypeReferenceCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.literal.BooleanCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.literal.CharacterCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.literal.MultiLineStringLiteralCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.literal.NullKeywordCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.literal.StringLiteralCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.number.BigDecimalCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.number.BigIntegerCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.number.DoubleCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.number.FloatCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.number.IntegerCrystal;
import com.atonementcrystals.dnr.vikari.core.crystal.number.LongCrystal;
import com.atonementcrystals.dnr.vikari.ide.gui.VideEditorPane;
import com.atonementcrystals.dnr.vikari.interpreter.Lexer;
import com.atonementcrystals.dnr.vikari.util.CoordinatePair;
import com.atonementcrystals.dnr.vikari.util.Utils;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Class for processing a {@link VideColorTheme}'s application to the syntax highlighting of Vikari code based on the
 * exact output of the {@link Lexer} and {@link VideParser}'s modifications to that output.
 */
public class VikariSyntaxHighlighter {
    public static final Set<String> TYPE_KEYWORDS = loadTypeKeywords();
    public static final Map<IdentifierType, List<Class<? extends AtonementCrystal>>> IDENTIFIER_TYPE_MAPPINGS = generateIdentifierTypeMappings();
    public static final Map<IdentifierType, Class<? extends AtonementCrystal>> IDENTIFIER_TYPE_OVERLOADS = generateIdentifierTypeOverloads();
    private static final int LINE_SEPARATOR_LENGTH = System.lineSeparator().length();

    private Map<Color, String> colorNames;
    private Map<Class<? extends AtonementCrystal>, Color> definedRules;
    private Map<IdentifierType, Color> overloadRules;
    private final Map<Color, AttributeSet> attributeSets;
    private final Lexer lexer;
    private final VideParser parser;
    private final Map<String, VikariHighlightFileData> fileCache;
    private int startOffset;
    private int startRowNumber;
    private int endRowNumber;
    private boolean enabled;

    /**
     * Instantiate a new VikariSyntaxHighlighter.
     */
    public VikariSyntaxHighlighter() {
        lexer = new Lexer();
        parser = new VideParser();

        lexer.setErrorReportingEnabled(false);
        parser.setErrorReportingEnabled(false);

        lexer.setLexUnparsableTokens(true);

        fileCache = new HashMap<>();
        attributeSets = new HashMap<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private VikariHighlightFileData getCachedFileData(String filePath) {
        return fileCache.computeIfAbsent(filePath, path -> new VikariHighlightFileData());
    }

    /**
     * Load the color names from the processed {@link VideColorTheme} by reversing the mapping. (For debugging purposes.)
     * @param colorNames The map of names to Color objects from the {@link VideColorThemeProcessor} to load.
     */
    public void loadColorNames(Map<String, Color> colorNames) {
        this.colorNames = new HashMap<>();
        for (Map.Entry<String, Color> pair : colorNames.entrySet()) {
            String name = pair.getKey();
            Color color = pair.getValue();
            this.colorNames.put(color, name);
        }
    }

    /**
     * Count the newlines present in the text between the start and end offsets.
     * @param text The text to search.
     * @param startOffset The start offset of the search region.
     * @param endOffset The end offset of the search region.
     * @return The number of newlines encountered in the search region of the text.
     */
    private int countNewlines(String text, int startOffset, int endOffset) {
        int newlineCount = 0;
        int nextNewlineIndex = startOffset;
        while (nextNewlineIndex < endOffset) {
            nextNewlineIndex = text.indexOf('\n', nextNewlineIndex);
            if (nextNewlineIndex == -1 || nextNewlineIndex >= endOffset) {
                break;
            }
            // Walk past the last encountered newline, and increment the counter.
            nextNewlineIndex += 1;
            newlineCount++;
        }
        return newlineCount;
    }

    /**
     * Highlight a region of a file after an edit has been made to its contents. This method presumes the file has
     * already been highlighted once by the {@link #highlightEntireFile(String, String, VideEditorPane)} method, and
     * that therefore the file contents have an existing entry in the {@link #fileCache}.
     * @param filePath The path to the file to highlight.
     * @param text The complete text for the file to highlight.
     * @param offset The offset into the text for the start of the edit.
     * @param length The length of the edit. If negative, it represents a deletion, rather than an addition.
     * @param editorPane The UI component that displays the syntax highlighted text to the user.
     */
    public void highlightRegion(String filePath, String text, int offset, int length, VideEditorPane editorPane) {
        if (enabled) {
            lexAndParse(filePath, text, offset, length);
            highlight(filePath, text, editorPane);
        }
    }

    /**
     * Debugging method for viewing the cached rows.
     * @param filePath The path for the file the rows are cached at.
     */
    private void printRows(String filePath) {
        VikariHighlightFileData cachedFileData = getCachedFileData(filePath);
        List<List<AtonementCrystal>> rows = cachedFileData.getRows();

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        int maxRowNumberCharWidth = String.valueOf(rows.size()).length();
        String rowNumberFormat = "[%0"+ maxRowNumberCharWidth + "d]: ";

        for (int rowNumber = 0; rowNumber < rows.size(); rowNumber++) {
            formatter.format(rowNumberFormat, rowNumber + 1);

            List<AtonementCrystal> row = rows.get(rowNumber);
            if (row == null) {
                System.out.println("null row: " + rowNumber);
                continue;
            }
            for (int i = 0; i < row.size(); i++) {
                AtonementCrystal crystal = row.get(i);

                String identifier = crystal.getIdentifier();
                String typeName = Utils.getSimpleClassName(crystal);
                formatter.format("%s(\"%s\")", typeName, identifier);

                if ( i < row.size() - 1) {
                    sb.append(", ");
                }
            }
            if (rowNumber < rows.size() - 1) {
                sb.append('\n');
            }
        }

        String result = sb.toString();
        System.out.println(result);
    }

    /**
     * Debugging method for printing the cached regions.
     * @param filePath The path for the cached file.
     * @param text The text of the file.
     */
    private void printRegions(String filePath, String text) {
        VikariHighlightFileData cachedFileData = getCachedFileData(filePath);
        TreeMap<Region, Region> regions = cachedFileData.getRegions();

        int newlineCount = countNewlines(text, 0, text.length()) + 1;
        int[] rowOffsets = getRowOffsets(text, newlineCount, 0, text.length());

        for (Region region : regions.navigableKeySet()) {
            CoordinatePair start = getLocationFor(rowOffsets, region.getStart());
            CoordinatePair end = getLocationFor(rowOffsets, region.getEnd());
            System.out.printf("Region(start: %s, end: %s) \n", start, end);
        }
    }

    /**
     * Find the location for an offset based on the row data of the rowOffsets array.
     * @param rowOffsets The array of offsets for each row.
     * @param offset The offset to find the location for.
     * @return The CoordinatePair location for the input offset in relation to the array of row offsets.
     */
    private CoordinatePair getLocationFor(int[] rowOffsets, int offset) {
        for (int row = rowOffsets.length - 1; row >= 0; row--) {
            int rowOffset = rowOffsets[row];
            if (rowOffset <= offset) {
                int column = offset - rowOffset;
                return new CoordinatePair(row, column);
            }
        }
        return null;
    }

    /**
     * Lex and parse the smallest possible region of text that both encloses the edit but also ensures that any edited
     * tokens are updated properly by the Lexer and Parser in the cached file row data such that it can be passed onto
     * the {@link #highlight(String, String, VideEditorPane)} method.
     * @param filePath The canonical file path for the file being lexed and parsed.
     * @param text The full text contents of the file being lexed and parsed.
     * @param editOffset The offset into the text for the start of the edit.
     * @param editLength The length of the edit. If negative, it represents a deletion, rather than an addition.
     */
    private void lexAndParse(String filePath, String text, int editOffset, int editLength) {
        VikariHighlightFileData cachedFileData = getCachedFileData(filePath);
        List<List<AtonementCrystal>> cachedRows = cachedFileData.getRows();

        int startOfRowOffsetSearchIndex = editOffset;
        int endOfRowOffsetSearchIndex = editOffset + Math.max(editLength, 0);
        if (text.indexOf('\n', editOffset) == editOffset) {
            // walk to the start of the previous line if an edit starts with a newline.
            startOfRowOffsetSearchIndex -= 1;
        }

        int startOffset = getStartOfRowOffset(text, startOfRowOffsetSearchIndex);
        int endOffset = getEndOfRowOffset(text, endOfRowOffsetSearchIndex);
        int startRow = countNewlines(text, 0, startOffset);
        int addedNewlineCount = countNewlines(text, startOffset, endOffset);
        int endRow = startRow + addedNewlineCount;

        // Detect what Regions are intersected. If so, update startOffset and startRow to the start of the first Region.
        TreeMap<Region, Region> cachedRegions = cachedFileData.getRegions();
        TreeMap<Region, Region> intersectedRegions = new TreeMap<>();

        // Collect all regions intersected by the edit.
        int searchEndOffset = (startOffset == endOffset) ? startOffset + 1 : endOffset;
        Region editSearchRegion = new Region(startOffset, searchEndOffset, null);
        Region editSearchResult;

        while ((editSearchResult = cachedRegions.remove(editSearchRegion)) != null) {
            intersectedRegions.put(editSearchResult, editSearchResult);
        }

        int initialStartOffset = startOffset;
        int initialEndOffset = endOffset;

        // Expand the update region for the initial pass of lexing and parsing the file contents, if necessary.
        if (!intersectedRegions.isEmpty()) {
            Region firstRegion = intersectedRegions.navigableKeySet().iterator().next();
            startOffset = Math.min(startOffset, firstRegion.getStart());

            if (startOffset != initialStartOffset) {
                startOffset = getStartOfRowOffset(text, startOffset);
                startRow = countNewlines(text, 0, startOffset);
            }

            Region lastRegion = intersectedRegions.navigableKeySet().descendingIterator().next();
            endOffset = Math.max(endOffset, lastRegion.getEnd());

            if (endOffset != initialEndOffset) {
                endOffset = getEndOfRowOffset(text, endOffset);
                endRow = startRow + countNewlines(text, startOffset, endOffset);
            }
        }

        // Lex and parse the necessary region of text.
        String textRegion = text.substring(startOffset, endOffset);

        lexer.reset();
        lexer.setLineNumberOffset(startRow);
        List<List<AtonementCrystal>> lexedStatements = lexer.lex(textRegion);

        parser.reset();
        parser.parse(null, lexedStatements);

        List<List<AtonementCrystal>> rows = convertToRows(lexedStatements, lexer.getUnparsableTokens(), startRow, endRow);

        // Update the cached rows.
        cachedRows.addAll(rows);
        startRowNumber = startRow;
        endRowNumber = startRow + rows.size() - 1;
        this.startOffset = startOffset;

        // Update all intersected Regions.
        int[] rowOffsets = getRowOffsets(text, rows.size(), startOffset, endOffset);
        updateRegions(text, rowOffsets, rows, cachedRegions);

        // Check if we need to lex and parse the remainder of the file contents.
        AtonementCrystal lastToken = getLastToken(rows);
        if (endOffset < text.length() && lastToken instanceof MultilineToken multilineToken && !multilineToken.isClosingToken()) {
            CoordinatePair unclosedTokenStartLocation = getUnclosedTokenStartLocation(rows, lastToken);
            int rowNumber = unclosedTokenStartLocation.getRow();
            int rowOffset = rowOffsets[rowNumber - startRow];
            // Lex and parse again from the start of the unclosed token until the end of the file.
            lexAndParseOffsetToEnd(filePath, text, rowOffset, unclosedTokenStartLocation);
            endRowNumber = startRow + cachedRows.size() - 1;
        }

        // Otherwise, shift all further regions by a constant amount equal to the edit length.
        else if (!cachedRegions.isEmpty()){

            // Walk forward to the first region after the endOffset.
            for (Region next : cachedRegions.navigableKeySet()) {
                if (endOffset <= next.getStart()) {
                    // Update all further regions by the length of the edit.
                    next.update(editLength);
                }
            }
        }
    }

    /**
     * Walk backwards to the first encountered opening multiline token, and return its coordinates.
     * @param rows The row data being searched through.
     * @param lastUnclosedMultilineToken The final token of the input row data.
     * @return The coordinate pair of the opening token paired with the final token.
     */
    private static CoordinatePair getUnclosedTokenStartLocation(List<List<AtonementCrystal>> rows,
                                                                AtonementCrystal lastUnclosedMultilineToken) {
        AtonementCrystal openingToken = null;
        for (int i = rows.size() - 1; i >= 0 && openingToken == null; i--) {
            List<AtonementCrystal> row = rows.get(i);
            if (row.isEmpty()) {
                continue;
            }
            AtonementCrystal lastCrystal = row.get(row.size() - 1);
            if (lastCrystal instanceof MultilineToken multilineToken && multilineToken.isOpeningToken()) {
                openingToken = lastCrystal;
            }
        }
        if (openingToken == null) {
            throw new IllegalStateException("Invalid row data state. Missing opening token for multiline token at" +
                    lastUnclosedMultilineToken.getCoordinates());
        }
        return openingToken.getCoordinates();
    }

    /**
     * Walk backwards to the first newline that is encountered, Then return the index of that newline plus one.
     * @param text The text to search.
     * @param offset The starting index to search from.
     * @return The offset of the start of the row of the given offset.
     */
    private int getStartOfRowOffset(String text, int offset) {
        int start = Math.min(offset, text.length() - 1);
        for (int i = start; i > 0; i--) {
            char c = text.charAt(i);
            if (c == '\n') {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * Walk forwards to the first newline that is encountered, then return the index of that newline.
     * @param text The text to search.
     * @param offset The starting index to search from.
     * @return The offset of the end of the row of the given offset.
     */
    private int getEndOfRowOffset(String text, int offset) {
        for (int i = offset; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                return i;
            }
        }
        return text.length();
    }

    private AtonementCrystal getLastToken(List<List<AtonementCrystal>> rows) {
        if (!rows.isEmpty()) {
            for (int i = rows.size() -1; i >= 0; i--) {
                List<AtonementCrystal> row = rows.get(i);
                if (!row.isEmpty()) {
                    return row.get(row.size() - 1);
                }
            }
        }
        return null;
    }

    /**
     * Convert the list of crystals from being organized on statement boundaries to being organized on row boundaries.
     * @param lexedStatements The list of lexed and parsed crystals to reorganize.
     * @param unparsableTokens The list of unparsable tokens as collected by the Lexer.
     * @param startRow The starting row number for the row data.
     * @param endRow The ending row number for the row data.
     * @return A list of crystals structured such that each row is organized in its own list. Empty rows necessitate an
     * empty list in the result set. (To preserve row-number-based traversals of the output of this method.)
     */
    private List<List<AtonementCrystal>> convertToRows(List<List<AtonementCrystal>> lexedStatements,
                                                       List<AtonementCrystal> unparsableTokens,
                                                       int startRow, int endRow) {
        List<AtonementCrystal> crystals = new ArrayList<>();
        for (List<AtonementCrystal> lexedStatement : lexedStatements) {
            crystals.addAll(lexedStatement);
        }

        // Only sort if any unparsable tokens are present. (The statements of crystals are all already in order.)
        if (!unparsableTokens.isEmpty()) {
            crystals.addAll(unparsableTokens);
            crystals.sort(Comparator.comparing(AtonementCrystal::getCoordinates));
        }
        List<List<AtonementCrystal>> rows = new ArrayList<>();

        // Simplify handling for the single row case.
        if (startRow == endRow) {
            rows.add(crystals);
            return rows;
        }

        // Initialize the rows list by the number of rows.
        for (int i = startRow; i <= endRow; i++) {
            rows.add(new ArrayList<>());
        }

        // Group crystals by row number into the final list of rows.
        for (AtonementCrystal crystal : crystals) {
            CoordinatePair location = crystal.getCoordinates();
            int crystalRowNumber = location.getRow();
            int listIndex = crystalRowNumber - startRow;
            List<AtonementCrystal> row = rows.get(listIndex);
            row.add(crystal);
        }
        return rows;
    }

    /**
     * Syntax highlight the previously lexed and parsed region. (As just processed by {@link #lexAndParse(String,
     * String, int, int)}.
     * @param filePath The canonical file path for the file being highlighted.
     * @param text The full text contents of the file being highlighted.
     * @param editorPane The UI component that displays the syntax highlighted text to the user.
     */
    private void highlight(String filePath, String text, VideEditorPane editorPane) {
        VikariHighlightFileData cachedFileData = getCachedFileData(filePath);
        List<List<AtonementCrystal>> rows = cachedFileData.getRows();

        if (rows.isEmpty()) return;
        StyledDocument styledDocument = editorPane.getStyledDocument();

        int rowOffset = startOffset;

        // For each statement
        for (int rowNumber = startRowNumber; rowNumber <= endRowNumber; rowNumber++) {
            int rowIndex = rowNumber - startRowNumber;
            List<AtonementCrystal> statement = rows.get(rowIndex);

            // For each crystal
            for (AtonementCrystal crystal : statement) {

                // Get the color rule.
                Color highlightColor = definedRules.get(crystal.getClass());
                if (highlightColor == null) {
                    highlightColor = Color.GRAY;
                }

                // Apply any overloaded rules for the crystal.
                if (crystal instanceof ReferenceCrystal reference) {
                    if (reference.isQuotedIdentifier()) {
                        highlightColor = overloadRules.get(IdentifierType.QUOTED_IDENTIFIER);
                    } else {
                        highlightColor = overloadRules.get(IdentifierType.VARIABLE);
                    }
                    // TODO: Implement highlighting rules for function calls and declarations.
                    //       (FUNCTION_DECLARATION, FUNCTION_CALL, CONSTRUCTOR_DECLARATION, CONSTRUCTOR_CALL.)
                } else if (crystal instanceof TypeReferenceCrystal && TYPE_KEYWORDS.contains(crystal.getIdentifier())) {
                    highlightColor = overloadRules.get(IdentifierType.TYPE_KEYWORD);
                }

                // Calculate the indexes for the token in the text.
                CoordinatePair location = crystal.getCoordinates();
                int column = location.getColumn();
                int offset = rowOffset + column;
                int length = crystal.getIdentifier().length();

                // Fetch the attribute set for the update.
                AttributeSet attributeSet = attributeSets.computeIfAbsent(highlightColor, color -> {
                    StyleContext styleContext = new StyleContext();
                    return styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
                });

                // Update the styled document.
                styledDocument.setCharacterAttributes(offset, length, attributeSet, true);
            }

            // Update the rowOffset for processing the next row.
            if (rowNumber < endRowNumber) {
                rowOffset = text.indexOf('\n', rowOffset) + 1;
            }
        }

        // Clear the cached rows after they are no longer needed.
        rows.clear();
    }

    /**
     * Highlight the entire file contents.
     * @param filePath The canonical file path to the cached file data to update.
     * @param text The text to lex and parse.
     * @param editorPane The UI component that displays the syntax highlighted text to the user.
     */
    public void highlightEntireFile(String filePath, String text, VideEditorPane editorPane) {
        if (enabled) {
            lexAndParseEntireFile(filePath, text);
            highlight(filePath, text, editorPane);
        }
    }

    /**
     * Lex and parse the entire file contents. The algorithm for doing so can be greatly simplified in this case.
     * (In comparison to the additional complexity required for {@link #lexAndParse(String, String, int, int)}.
     * @param filePath The canonical file path to the cached file data to update.
     * @param text The text to lex and parse.
     */
    private void lexAndParseEntireFile(String filePath, String text) {
        lexer.reset();
        parser.reset();

        List<List<AtonementCrystal>> lexedStatements = lexer.lex(text);
        parser.parse(null, lexedStatements);

        int startRow = 0;
        int endRow = countNewlines(text, 0, text.length());
        List<List<AtonementCrystal>> rows = convertToRows(lexedStatements, lexer.getUnparsableTokens(), startRow, endRow);

        VikariHighlightFileData cachedFileData = getCachedFileData(filePath);
        cachedFileData.setRows(rows);
        TreeMap<Region, Region> regions = cachedFileData.getRegions();
        calculateAllRegions(text, rows, regions);

        startRowNumber = 0;
        endRowNumber = rows.size() - 1;
        this.startOffset = 0;
    }

    /**
     * Updates the regions argument by replacing its contents with the Region data for the given rows.
     * @param text What the rows argument is modeling. Used to calculate row offsets.
     * @param rows The lexed and parsed row data.
     * @param regions The map of multiline token Region data to update.
     */
    private void calculateAllRegions(String text, List<List<AtonementCrystal>> rows, TreeMap<Region, Region> regions) {
        updateRegions(text, rows, regions, 0, text.length());
    }

    private void clearRegions(TreeMap<Region, Region> regions, int startOffset) {
        if (startOffset == 0) {
            regions.clear();
        } else {
            Iterator<Region> it = regions.navigableKeySet().iterator();
            while (it.hasNext()) {
                Region next = it.next();
                if (startOffset <= next.getStart()) {
                    it.remove();
                    // The TreeMap's collection ordering guarantees all future Regions match the above condition.
                    while (it.hasNext()) {
                        it.next();
                        it.remove();
                    }
                }
            }
        }
    }

    private void updateRegions(String text, List<List<AtonementCrystal>> rows, TreeMap<Region, Region> regions,
                               int startOffset, int endOffset) {
        int[] rowOffsets = getRowOffsets(text, rows.size(), startOffset, endOffset);
        updateRegions(text, rowOffsets, rows, regions);
    }

    private void updateRegions(String text, int[] rowOffsets, List<List<AtonementCrystal>> rows,
                               TreeMap<Region, Region> regions) {
        // Calculate all new Regions to add past the given startOffset.
        Class<? extends MultilineToken> tokenType = null;
        int regionStart = -1;
        int regionEnd;

        // rowIndex is not the absolute row number, but rather the relative index into the list of updated rows.
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<AtonementCrystal> row = rows.get(rowIndex);
            for (AtonementCrystal crystal : row) {
                if (crystal instanceof MultilineToken multilineToken) {
                    if (multilineToken.isOpeningToken()) {
                        if (tokenType != null) {
                            throw new IllegalStateException("Malformed row data. Expected null tokenType, but it was " +
                                    tokenType.getName());
                        } else {
                            tokenType = multilineToken.getClass();
                        }
                        CoordinatePair location = crystal.getCoordinates();
                        int column = location.getColumn();
                        int rowOffset = rowOffsets[rowIndex];
                        regionStart = rowOffset + column;
                    } else {
                        // The following checks are for both middle and closing multiline tokens.
                        if (tokenType == null) {
                            throw new IllegalStateException("Malformed row data. Expected non-null tokenType, but it " +
                                    "was null.");
                        } else if (tokenType != crystal.getClass()) {
                            String prevTypeName = tokenType.getName();
                            String currentTypeName = crystal.getClass().getName();
                            throw new IllegalStateException("Malformed row data. Expected tokenTypes to be equivalent," +
                                    ", but previous is " + prevTypeName + " and current is " + currentTypeName);
                        }
                        if (multilineToken.isClosingToken()) {
                            // Note that a closing token will always have a column value of zero.
                            int rowOffset = rowOffsets[rowIndex];
                            int tokenLength = crystal.getIdentifier().length();
                            regionEnd = rowOffset + tokenLength;
                            Region region = new Region(regionStart, regionEnd, tokenType);
                            regions.put(region, region);
                            // Clear regionStart and tokenType so that later unclosed tokens are detectable.
                            regionStart = -1;
                            tokenType = null;
                        }
                    }
                }
            }
        }

        // Handle an unclosed multiline token terminating the file.
        if (regionStart != -1) {
            // Extend the region so that an edit to the end of the file will intersect this final Region.
            regionEnd = text.length() + LINE_SEPARATOR_LENGTH + 1;
            Region region = new Region(regionStart, regionEnd, tokenType);
            regions.put(region, region);
        }
    }

    /**
     * Calculate all row offsets for the text between the start and end offsets. A row offset is the index of a newline
     * character plus one.
     * @param text The entire contents of the file to scan.
     * @param rowCount The number of rows in the region to scan.
     * @param startOffset The start index for the region to scan.
     * @param endOffset The end index for the region to scan.
     * @return An array of row offsets for the scanned region of text.
     */
    private int[] getRowOffsets(String text, int rowCount, int startOffset, int endOffset) {
        int[] rowOffsets = new int[rowCount];
        rowOffsets[0] = getStartOfRowOffset(text, startOffset);
        int nextNewlineIndex = startOffset;
        for (int offsetsIndex = 1; offsetsIndex < rowCount; offsetsIndex++) {
            nextNewlineIndex = text.indexOf('\n', nextNewlineIndex);
            if (nextNewlineIndex == -1) {
                String scannedText = text.substring(startOffset, endOffset);
                throw new IllegalStateException("rowCount of " + rowCount + " does not match count of newlines found " +
                        "(" + offsetsIndex + ") in the following region of text:\n" + scannedText);
            } else {
                // Walk forward past the newline character.
                nextNewlineIndex += 1;
                rowOffsets[offsetsIndex] = nextNewlineIndex;
            }
        }
        return rowOffsets;
    }

    /**
     * The row at startRow is terminated by an unclosed multiline token. And so then the region defined by the start of
     * that token to the end of the text must be lexed and parsed again, and updated in the file cache.
     * @param filePath The canonical file path to the cached file data to update.
     * @param text The text to lex and parse.
     * @param rowOffset The offset into the text of the start of the row to begin lexing and parsing at.
     * @param unclosedTokenLocation The row and column info for the aforementioned unclosed multiline token.
     */
    private void lexAndParseOffsetToEnd(String filePath, String text, int rowOffset, CoordinatePair unclosedTokenLocation) {
        int startRow = unclosedTokenLocation.getRow();
        int startColumn = unclosedTokenLocation.getColumn();
        int startOffset = rowOffset + startColumn;

        String textToLexAndParse = text.substring(startOffset);

        lexer.reset();
        lexer.setLineNumberOffset(startRow);
        List<List<AtonementCrystal>> lexedStatements = lexer.lex(textToLexAndParse);

        parser.reset();
        parser.parse(null, lexedStatements);

        int endRow = countNewlines(text, 0, text.length());
        List<List<AtonementCrystal>> newRows = convertToRows(lexedStatements, lexer.getUnparsableTokens(), startRow, endRow);

        VikariHighlightFileData cachedFileData = getCachedFileData(filePath);
        List<List<AtonementCrystal>> cachedRows = cachedFileData.getRows();

        // Prepare the existing row data by modifying the opening token, and removing any further tokens.
        List<AtonementCrystal> firstNewRow = newRows.get(0);
        AtonementCrystal lastToken = firstNewRow.get(firstNewRow.size() - 1);
        lastToken.setCoordinates(unclosedTokenLocation);

        List<AtonementCrystal> finalCachedRowToKeep = null;
        int cachedRowRemovalStartIndex = -1;
        for (int i = cachedRows.size() - 1; i >= 0; i--) {
            List<AtonementCrystal> cachedRow = cachedRows.get(i);
            if (!cachedRow.isEmpty()) {
                AtonementCrystal lastCrystal = cachedRow.get(cachedRow.size() - 1);
                CoordinatePair location = lastCrystal.getCoordinates();
                if (location == unclosedTokenLocation) {
                    finalCachedRowToKeep = cachedRow;
                    cachedRowRemovalStartIndex = i + 1;
                    break;
                }
            }
        }
        if (finalCachedRowToKeep == null) {
            throw new IllegalStateException("Malformed row data: cached rows do not contain a token with the " +
                    "location " + unclosedTokenLocation);
        }

        // Update the final cached row with the new opening multiline token.
        finalCachedRowToKeep.set(finalCachedRowToKeep.size() - 1, lastToken);

        // Clear the incomplete row data so it can be replaced by the new rows.
        if (startRow < endRow) {
            cachedRows.removeAll(cachedRows.subList(cachedRowRemovalStartIndex, cachedRows.size()));
        }

        // Update the rest of the rows.
        for (int i = 1; i < newRows.size(); i++) {
            List<AtonementCrystal> newRow = newRows.get(i);
            cachedRows.add(newRow);
        }

        TreeMap<Region, Region> cachedRegions = cachedFileData.getRegions();
        clearRegions(cachedRegions, startOffset);
        updateRegions(text, newRows, cachedRegions, startOffset, text.length());

        // NOTE: startRowNumber, endRowNumber, and offset are set outside of this method.
    }

    /**
     * Remove the cached file data for the given file.
     * @param filePath The canonical file path to remove the cached file data for.
     */
    public void closeFile(String filePath) {
        fileCache.remove(filePath);
    }

    public void setDefinedRules(Map<Class<? extends AtonementCrystal>, Color> definedRules) {
        this.definedRules = definedRules;
    }

    public void setOverloadRules(Map<IdentifierType, Color> overloadRules) {
        this.overloadRules = overloadRules;
    }

    /**
     * @return The set of all type keywords that are available as base types for type declarations.
     */
    private static Set<String> loadTypeKeywords() {
        Set<String> typeKeywords = new HashSet<>();

        typeKeywords.add("Type");
        typeKeywords.add("Interface");
        typeKeywords.add("AbstractType");
        typeKeywords.add("Enum");
        typeKeywords.add("Record");
        typeKeywords.add("Library");
        typeKeywords.add("TestSuite");

        return typeKeywords;
    }

    /**
     * Convert the variable arguments list of TokenTypes into a List of their associated crystal types.
     * @param TokenTypes The list of TokenTypes to convert.
     * @return A list of crystal types mapped from the input TokenTypes.
     */
    private static List<Class<? extends AtonementCrystal>> listOfTypes(TokenType... TokenTypes) {
        return Arrays.stream(TokenTypes)
                .map(TokenType::getJavaType)
                .collect(Collectors.toList());
    }

    /**
     * The primary mappings for IdentifierTypes to AtonementCrystal classes.
     * @return The Map of class lists for each IdentifierType.
     */
    private static Map<IdentifierType, List<Class<? extends AtonementCrystal>>> generateIdentifierTypeMappings() {
        Map<IdentifierType, List<Class<? extends AtonementCrystal>>> identifierTypeMappings = new HashMap<>();

        identifierTypeMappings.put(IdentifierType.VARIABLE, List.of(
                ReferenceCrystal.class
        ));
        identifierTypeMappings.put(IdentifierType.TYPE, List.of(
                TypeReferenceCrystal.class
        ));
        identifierTypeMappings.put(IdentifierType.TYPE_KEYWORD, List.of());
        identifierTypeMappings.put(IdentifierType.PUNCTUATION, listOfTypes(
                TokenType.DOT,                      // .
                TokenType.VARIABLE_ARGUMENTS_LIST,  // ...
                TokenType.STATEMENT_SEPARATOR,  // ,
                TokenType.REGION_SEPARATOR,     // ;
                TokenType.TYPE_LABEL,           // :
                TokenType.EXISTS,               // ?
                TokenType.INSTANCE_OF,          // ?
                TokenType.FUNCTION_CALL,        // !
                TokenType.CAST,                 // !
                TokenType.COPY_CONSTRUCTOR,     // &
                TokenType.DELETE,               // ~
                TokenType.LINE_CONTINUATION,    // ~
                TokenType.MINIMIZED_LINE_CONTINUATION,  // /~/
                TokenType.REGION_OPERATOR      // ::
        ));
        identifierTypeMappings.put(IdentifierType.CONTROL_FLOW, listOfTypes(
                TokenType.CONDITIONAL_BRANCH,   // ??
                TokenType.LOOP,                 // <>
                TokenType.THROW,                // --
                TokenType.CATCH                 // ++
        ));
        identifierTypeMappings.put(IdentifierType.OPERATORS, listOfTypes(
                TokenType.RETURN,       // ^^
                TokenType.CONTINUE,     // >>
                TokenType.BREAK,        // vv
                TokenType.RANGE,        // ..
                TokenType.MODULUS,      // %
                TokenType.MULTIPLY,     // *
                TokenType.SUBTRACT,     // -
                TokenType.NEGATE,       // -
                TokenType.ADD,          // +
                TokenType.CONCATENATE,  // +
                TokenType.LEFT_DIVIDE,  // /
                TokenType.RIGHT_DIVIDE, // \
                TokenType.LEFT_ASSIGNMENT,              // <<
                TokenType.LEFT_ADD_ASSIGNMENT,          // +<<
                TokenType.LEFT_SUBTRACT_ASSIGNMENT,     // -<<
                TokenType.LEFT_DIVIDE_ASSIGNMENT,       // /<<
                TokenType.LEFT_MULTIPLY_ASSIGNMENT,     // *<<
                TokenType.RIGHT_ASSIGNMENT,             // >>
                TokenType.RIGHT_ADD_ASSIGNMENT,         // +>>
                TokenType.RIGHT_SUBTRACT_ASSIGNMENT,    // ->>
                TokenType.RIGHT_DIVIDE_ASSIGNMENT,      // \>>
                TokenType.RIGHT_MULTIPLY_ASSIGNMENT,    // *>>
                TokenType.LEFT_LOGICAL_AND_ASSIGNMENT,  // ^<<
                TokenType.LEFT_LOGICAL_OR_ASSIGNMENT,   // "<<
                TokenType.RIGHT_LOGICAL_AND_ASSIGNMENT, // ^>>
                TokenType.RIGHT_LOGICAL_OR_ASSIGNMENT,  // ">>
                TokenType.LOGICAL_AND,              // ^
                TokenType.LOGICAL_OR,               // "
                TokenType.LOGICAL_NOT,              // '
                TokenType.EQUALS,                   // =
                TokenType.NOT_EQUALS,               // '=
                TokenType.REFERENCE_EQUALS,         // <=>
                TokenType.GREATER_THAN,             // >
                TokenType.LESS_THAN,                // <
                TokenType.GREATER_THAN_OR_EQUALS,   // >=
                TokenType.LESS_THAN_OR_EQUALS,      // <=
                TokenType.ITERATION_ELEMENT         // <-
        ));
        identifierTypeMappings.put(IdentifierType.SEPARATORS, listOfTypes(
                TokenType.KEY_VALUE_PAIR,           // =>
                TokenType.LIST_ELEMENT_SEPARATOR,   // |
                TokenType.LEFT_PARENTHESIS,         // (
                TokenType.RIGHT_PARENTHESIS,        // )
                TokenType.LEFT_SQUARE_BRACKET,      // [
                TokenType.RIGHT_SQUARE_BRACKET,     // ]
                TokenType.LEFT_CURLY_BRACKET,       // {
                TokenType.RIGHT_CURLY_BRACKET,      // }
                TokenType.ANNOTATION,                   // $:
                TokenType.ANNOTATION_OPENING_BRACKET,   // {
                TokenType.ANNOTATION_CLOSING_BRACKET,   // |
                TokenType.ANNOTATION_ELEMENT_SEPARATOR, // }
                TokenType.COLLECTION_LITERAL,                   // $:
                TokenType.LIST_LITERAL_CLOSING_BRACKET,         // (
                TokenType.LIST_LITERAL_CLOSING_BRACKET,         // )
                TokenType.SET_LITERAL_OPENING_BRACKET,          // {
                TokenType.SET_LITERAL_CLOSING_BRACKET,          // }
                TokenType.ARRAY_LITERAL_OPENING_BRACKET,        // [
                TokenType.ARRAY_LITERAL_CLOSING_BRACKET,        // ]
                TokenType.COLLECTION_LITERAL_ELEMENT_SEPARATOR, // |
                TokenType.FUNCTION_PARAMETER_LIST_OPENING_BRACKET,      // (
                TokenType.FUNCTION_PARAMETER_LIST_CLOSING_BRACKET,      // )
                TokenType.FUNCTION_PARAMETER_LIST_ELEMENT_SEPARATOR,    // |
                TokenType.FUNCTION_ARGUMENT_LIST_OPENING_BRACKET,       // (
                TokenType.FUNCTION_ARGUMENT_LIST_CLOSING_BRACKET,       // )
                TokenType.FUNCTION_ARGUMENT_LIST_ELEMENT_SEPARATOR      // |
        ));
        identifierTypeMappings.put(IdentifierType.GROUPING, listOfTypes(
                TokenType.LEFT_SQUARE_BRACKET,  // [
                TokenType.RIGHT_SQUARE_BRACKET  // ]
        ));
        identifierTypeMappings.put(IdentifierType.CONSTANT_BRACKETS, listOfTypes(
                TokenType.LEFT_CURLY_BRACKET,   // {
                TokenType.RIGHT_CURLY_BRACKET   // }
        ));
        identifierTypeMappings.put(IdentifierType.ANNOTATION, listOfTypes(
                TokenType.ANNOTATION,                   // $:
                TokenType.ANNOTATION_OPENING_BRACKET,   // {
                TokenType.ANNOTATION_CLOSING_BRACKET,   // |
                TokenType.ANNOTATION_ELEMENT_SEPARATOR  // }
        ));
        identifierTypeMappings.put(IdentifierType.COLLECTION_LITERAL, listOfTypes(
                TokenType.COLLECTION_LITERAL,                   // $:
                TokenType.LIST_LITERAL_CLOSING_BRACKET,         // (
                TokenType.LIST_LITERAL_CLOSING_BRACKET,         // )
                TokenType.SET_LITERAL_OPENING_BRACKET,          // {
                TokenType.SET_LITERAL_CLOSING_BRACKET,          // }
                TokenType.ARRAY_LITERAL_OPENING_BRACKET,        // [
                TokenType.ARRAY_LITERAL_CLOSING_BRACKET,        // ]
                TokenType.COLLECTION_LITERAL_ELEMENT_SEPARATOR  // |
        ));
        identifierTypeMappings.put(IdentifierType.LITERALS, List.of(
                StringLiteralCrystal.class,
                MultiLineStringLiteralCrystal.class,
                CharacterCrystal.class,
                IntegerCrystal.class,
                LongCrystal.class,
                BigIntegerCrystal.class,
                FloatCrystal.class,
                DoubleCrystal.class,
                BigDecimalCrystal.class,
                BooleanCrystal.class,
                NullKeywordCrystal.class
        ));
        identifierTypeMappings.put(IdentifierType.SWORDS, listOfTypes(
                TokenType.SWORD,                // _
                TokenType.CATCH_ALL,            // ||
                TokenType.LEFT_FEATHER_FALL,    // \\
                TokenType.RIGHT_FEATHER_FALL    // //
        ));
        identifierTypeMappings.put(IdentifierType.NUMBERS, List.of(
                IntegerCrystal.class,
                LongCrystal.class,
                BigIntegerCrystal.class,
                FloatCrystal.class,
                DoubleCrystal.class,
                BigDecimalCrystal.class
        ));
        identifierTypeMappings.put(IdentifierType.BOOLEANS, List.of(
                BooleanCrystal.class
        ));
        identifierTypeMappings.put(IdentifierType.STRINGS, List.of(
                StringLiteralCrystal.class,
                MultiLineStringLiteralCrystal.class
        ));
        identifierTypeMappings.put(IdentifierType.CHARACTERS, List.of(
                CharacterCrystal.class
        ));
        identifierTypeMappings.put(IdentifierType.COMMENT, List.of(
                CommentCrystal.class,
                MultiLineCommentCrystal.class
        ));
        identifierTypeMappings.put(IdentifierType.FIELD_ACCESS, listOfTypes(
                TokenType.INSTANCE_FIELD_ACCESS,    // @
                TokenType.STATIC_FIELD_ACCESS,      // #
                TokenType.INSTANCE_SUPER,           // @
                TokenType.STATIC_SUPER              // #
        ));
        identifierTypeMappings.put(IdentifierType.FUNCTION_PARAMETER_LIST, listOfTypes(
                TokenType.FUNCTION_PARAMETER_LIST_OPENING_BRACKET,  // (
                TokenType.FUNCTION_PARAMETER_LIST_CLOSING_BRACKET,  // )
                TokenType.FUNCTION_PARAMETER_LIST_ELEMENT_SEPARATOR // |
        ));
        identifierTypeMappings.put(IdentifierType.FUNCTION_ARGUMENT_LIST, listOfTypes(
                TokenType.FUNCTION_ARGUMENT_LIST_OPENING_BRACKET,   // (
                TokenType.FUNCTION_ARGUMENT_LIST_CLOSING_BRACKET,   // )
                TokenType.FUNCTION_ARGUMENT_LIST_ELEMENT_SEPARATOR  // |
        ));

        return identifierTypeMappings;
    }

    /**
     * For mappings that would override the base rules, but still need a definition somewhere for their application.
     * @return The Map of class lists for each IdentifierType.
     */
    private static Map<IdentifierType, Class<? extends AtonementCrystal>> generateIdentifierTypeOverloads() {
        Map<IdentifierType, Class<? extends AtonementCrystal>> identifierTypeOverloads = new HashMap<>();

        identifierTypeOverloads.put(IdentifierType.TYPE_KEYWORD, TypeReferenceCrystal.class);
        identifierTypeOverloads.put(IdentifierType.CONSTANT, ReferenceCrystal.class);
        identifierTypeOverloads.put(IdentifierType.QUOTED_IDENTIFIER, ReferenceCrystal.class);
        identifierTypeOverloads.put(IdentifierType.FUNCTION_DECLARATION, ReferenceCrystal.class);
        identifierTypeOverloads.put(IdentifierType.FUNCTION_CALL, ReferenceCrystal.class);
        identifierTypeOverloads.put(IdentifierType.CONSTRUCTOR_DECLARATION, ReferenceCrystal.class);
        identifierTypeOverloads.put(IdentifierType.CONSTRUCTOR_CALL, ReferenceCrystal.class);

        return identifierTypeOverloads;
    }
}
