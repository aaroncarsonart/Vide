package com.atonementcrystals.dnr.vikari.ide.parsing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VideColorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Test that a JSON object with fields r, g, and b can be parsed into a VideColor object.
     * Validation of the RGB values happens at a later step.
     */
    @Test
    @Order(1)
    public void testJacksonDeserialization_RGB() {
        // 1. Min values.
        String colorDefinitionRGB = """
            { "r": 0, "g": 0, "b": 0 }"
            """;

        try {
            VideColor videColor = objectMapper.readValue(colorDefinitionRGB, VideColor.class);

            assertEquals(videColor.getR(), 0, "Unexpected color value.");
            assertEquals(videColor.getG(), 0, "Unexpected color value.");
            assertEquals(videColor.getB(), 0, "Unexpected color value.");
        } catch (JsonProcessingException e) {
            fail("Test failed. Error: " + e.getMessage());
        }

        // 2. Max values.
        colorDefinitionRGB = """
            { "r": 255, "g": 255, "b": 255 }"
            """;

        try {
            VideColor videColor = objectMapper.readValue(colorDefinitionRGB, VideColor.class);

            assertEquals(videColor.getR(), 255, "Unexpected color value.");
            assertEquals(videColor.getG(), 255, "Unexpected color value.");
            assertEquals(videColor.getB(), 255, "Unexpected color value.");

            assertNull(videColor.getHex(), "Expected hex string to be null.");
            assertNull(videColor.getColorName(), "Unexpected colorName to be null.");
        } catch (JsonProcessingException e) {
            fail("Test failed. Error: " + e.getMessage());
        }
    }

    /**
     * Test that a hex string can be parsed into a VideColor object.
     * Validation of the hex string happens at a later step.
     */
    @Test
    @Order(2)
    public void testJacksonDeserialization_Hex() {
        String colorDefinitionHex = """
            "#ff0000"
            """;

        try {
            VideColor videColor = objectMapper.readValue(colorDefinitionHex, VideColor.class);

            assertEquals("#ff0000", videColor.getHex(), "Unexpected hex string value.");
            assertNull(videColor.getColorName(), "Unexpected colorName to be null.");

            assertEquals(videColor.getR(), VideColor.UNDEFINED_RGB_VALUE, "Expected r to be undefined.");
            assertEquals(videColor.getG(), VideColor.UNDEFINED_RGB_VALUE, "Expected g to be undefined.");
            assertEquals(videColor.getB(), VideColor.UNDEFINED_RGB_VALUE, "Expected b to be undefined.");
        } catch (JsonProcessingException e) {
            fail("Test failed. Error: " + e.getMessage());
        }
    }

    /**
     * Test that a color name (as specified in the colors map of {@link VideColorTheme}) can be parsed into a VideColor.
     * Validation of the color name happens at a later step.
     */
    @Test
    @Order(3)
    public void testJacksonDeserialization_ColorName() {
        String colorDefinitionHex = """
            "red"
            """;

        try {
            VideColor videColor = objectMapper.readValue(colorDefinitionHex, VideColor.class);

            assertEquals("red", videColor.getColorName(), "Unexpected colorName string value.");
            assertNull(videColor.getHex(), "Unexpected hex string to be null.");

            assertEquals(videColor.getR(), VideColor.UNDEFINED_RGB_VALUE, "Expected r to be undefined.");
            assertEquals(videColor.getG(), VideColor.UNDEFINED_RGB_VALUE, "Expected g to be undefined.");
            assertEquals(videColor.getB(), VideColor.UNDEFINED_RGB_VALUE, "Expected b to be undefined.");
        } catch (JsonProcessingException e) {
            fail("Test failed. Error: " + e.getMessage());
        }
    }
}