package com.atonementcrystals.dnr.vikari.ide.parsing;

import com.atonementcrystals.dnr.vikari.core.crystal.AtonementCrystal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * For tracking all cached data relating to lexing and parsing a Vikari code file for the purposes of performant updates
 * to syntax highlighting the contents of said file in a VideEditorWindow.
 */
public class VikariHighlightFileData {
    private List<List<AtonementCrystal>> rows;
    private final TreeMap<Region, Region> multilineTokenRegions;

    public VikariHighlightFileData() {
        this.rows = new ArrayList<>();
        this.multilineTokenRegions = new TreeMap<>();
    }

    public List<List<AtonementCrystal>> getRows() {
        return rows;
    }

    public void setRows(List<List<AtonementCrystal>> rows) {
        this.rows = rows;
    }

    public TreeMap<Region, Region> getRegions() {
        return multilineTokenRegions;
    }

    public void addRegion(Region region) {
        multilineTokenRegions.put(region, region);
    }

    public void updateRegion(Region oldRegion, Region newRegion) {
        multilineTokenRegions.remove(oldRegion);
        multilineTokenRegions.put(newRegion, newRegion);
    }

    public void updateRegions(Region updateRegion) {
        Iterator<Region> it = multilineTokenRegions.keySet().iterator();
    }

    public Region findRegion(int offset) {
        // tokenType is not used for searches. (As we don't know the type we'll encounter beforehand.)
        Region searchRegion = new Region(offset, offset + 1, null);
        return multilineTokenRegions.get(searchRegion);
    }

    public Region findRegion(Region searchRegion) {
        return multilineTokenRegions.get(searchRegion);
    }
}
