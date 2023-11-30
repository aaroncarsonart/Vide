package com.atonementcrystals.dnr.vikari.ide.parsing;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegionTest {

    @Test
    @Order(1)
    public void testCompareTo_Basic() {
        // Equal regions
        Region region1 = new Region(0, 1, null);
        Region region2 = new Region(0, 1, null);
        int expected = 0;
        assertEquals(expected, region1.compareTo(region2), "Expected equivalent regions to be equal.");

        // Less than
        region1 = new Region(0, 1, null);
        region2 = new Region(1, 2, null);
        expected = -1;
        assertEquals(expected, region1.compareTo(region2), "Expected a less than result.");

        // Greater than
        region1 = new Region(1, 2, null);
        region2 = new Region(0, 1, null);
        expected = 1;
        assertEquals(expected, region1.compareTo(region2), "Expected a greater than result.");
    }

    @Test
    @Order(2)
    public void testCompareTo_Intersect() {
        // One region encloses the other.
        Region region1 = new Region(0, 1, null);
        Region region2 = new Region(0, 2, null);
        assertEquals(0, region1.compareTo(region2), "Expected intersecting regions compare as equivalent.");
        assertEquals(0, region2.compareTo(region1), "Expected intersecting regions compare as equivalent.");

        // Regions intersect by one character.
        region1 = new Region(0, 2, null);
        region2 = new Region(1, 3, null);
        assertEquals(0, region1.compareTo(region2), "Expected intersecting regions compare as equivalent.");
        assertEquals(0, region2.compareTo(region1), "Expected intersecting regions compare as equivalent.");

        // Regions don't intersect (by zero characters).
        region1 = new Region(0, 1, null);
        region2 = new Region(1, 2, null);
        assertEquals(-1, region1.compareTo(region2), "Expected intersecting regions compare as equivalent.");
        assertEquals(1, region2.compareTo(region1), "Expected intersecting regions compare as equivalent.");

        // Regions don't intersect (by two characters).
        region1 = new Region(0, 2, null);
        region2 = new Region(3, 5, null);
        assertEquals(-3, region1.compareTo(region2), "Expected intersecting regions compare as equivalent.");
        assertEquals(3, region2.compareTo(region1), "Expected intersecting regions compare as equivalent.");
    }
}
