package com.example.miniprojectapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for GS1Parser - GS1 DataMatrix barcode parsing utility.
 * Tests both parenthesized and unparenthesized GS1 element string formats.
 */
public class GS1ParserTest {

    // ─────────────────────────────────────────────────────────────
    // Parenthesized Format: (01)GTIN(17)EXPIRY(10)BATCH(21)SERIAL
    // ─────────────────────────────────────────────────────────────

    @Test
    public void parse_fullParenthesizedGS1_extractsAllFields() {
        String barcode = "(01)00312345678906(17)251231(10)BATCH123(21)SN987654";
        GS1Parser.GS1Data data = GS1Parser.parse(barcode);

        assertTrue("Should be identified as GS1", data.isGS1);
        assertEquals("00312345678906", data.gtin);
        assertEquals("2025-12-31", data.expiryDate);
        assertEquals("BATCH123", data.batchNumber);
        assertEquals("SN987654", data.serialNumber);
    }

    @Test
    public void parse_parenthesized_onlyGTINAndExpiry() {
        String barcode = "(01)00312345678906(17)260630";
        GS1Parser.GS1Data data = GS1Parser.parse(barcode);

        assertTrue(data.isGS1);
        assertEquals("00312345678906", data.gtin);
        assertEquals("2026-06-30", data.expiryDate);
        assertEquals("", data.batchNumber);
        assertEquals("", data.serialNumber);
    }

    @Test
    public void parse_parenthesized_missingSerial_doesNotCrash() {
        String barcode = "(01)12345678901234(10)LOT456";
        GS1Parser.GS1Data data = GS1Parser.parse(barcode);

        assertTrue(data.isGS1);
        assertEquals("LOT456", data.batchNumber);
        assertEquals("", data.serialNumber);
        assertEquals("", data.expiryDate);
    }

    // ─────────────────────────────────────────────────────────────
    // Unparenthesized Format: 01GTIN17EXPIRY10BATCH
    // ─────────────────────────────────────────────────────────────

    @Test
    public void parse_unparenthesized_extractsGTINAndExpiry() {
        // 01 + 14-digit GTIN + 17 + 6-digit date
        String barcode = "010031234567890617251231";
        GS1Parser.GS1Data data = GS1Parser.parse(barcode);

        assertTrue(data.isGS1);
        assertEquals("00312345678906", data.gtin);
        assertEquals("2025-12-31", data.expiryDate);
    }

    @Test
    public void parse_unparenthesized_tooShort_notGS1() {
        // Less than 16 chars starting with "01"
        String barcode = "01234567";
        GS1Parser.GS1Data data = GS1Parser.parse(barcode);
        assertFalse("Short barcode starting with 01 should not be GS1", data.isGS1);
    }

    // ─────────────────────────────────────────────────────────────
    // Date Formatting
    // ─────────────────────────────────────────────────────────────

    @Test
    public void parse_expiryDate_formattedCorrectly_jan() {
        String barcode = "(01)00312345678906(17)260101";
        GS1Parser.GS1Data data = GS1Parser.parse(barcode);
        assertEquals("2026-01-01", data.expiryDate);
    }

    @Test
    public void parse_expiryDate_formattedCorrectly_dec() {
        String barcode = "(01)00312345678906(17)261231";
        GS1Parser.GS1Data data = GS1Parser.parse(barcode);
        assertEquals("2026-12-31", data.expiryDate);
    }

    // ─────────────────────────────────────────────────────────────
    // Edge / Null Cases
    // ─────────────────────────────────────────────────────────────

    @Test
    public void parse_nullInput_returnsEmptyNonCrashing() {
        GS1Parser.GS1Data data = GS1Parser.parse(null);
        assertNotNull(data);
        assertFalse(data.isGS1);
        assertEquals("", data.gtin);
    }

    @Test
    public void parse_emptyString_returnsEmptyNonCrashing() {
        GS1Parser.GS1Data data = GS1Parser.parse("");
        assertNotNull(data);
        assertFalse(data.isGS1);
        assertEquals("", data.gtin);
    }

    @Test
    public void parse_regularBarcode_notGS1() {
        // Plain EAN-13 — not a GS1 element string
        String barcode = "5901234123457";
        GS1Parser.GS1Data data = GS1Parser.parse(barcode);
        assertFalse("Plain EAN-13 should not be flagged as GS1", data.isGS1);
    }

    @Test
    public void parse_rawBarcodePreserved_inResult() {
        String barcode = "(01)00312345678906(17)251231";
        GS1Parser.GS1Data data = GS1Parser.parse(barcode);
        assertEquals(barcode, data.rawValue);
    }

    @Test
    public void parse_whitespaceInput_returnsEmptyNonCrashing() {
        GS1Parser.GS1Data data = GS1Parser.parse("   ");
        assertNotNull(data);
        assertFalse(data.isGS1);
    }

    // ─────────────────────────────────────────────────────────────
    // GS1Data Model
    // ─────────────────────────────────────────────────────────────

    @Test
    public void gs1Data_constructor_setsRawValue() {
        GS1Parser.GS1Data data = new GS1Parser.GS1Data("test-barcode");
        assertEquals("test-barcode", data.rawValue);
        assertFalse(data.isGS1);
        assertEquals("", data.gtin);
        assertEquals("", data.batchNumber);
        assertEquals("", data.expiryDate);
        assertEquals("", data.serialNumber);
    }

    @Test
    public void gs1Data_nullRawValue_defaultsToEmpty() {
        GS1Parser.GS1Data data = new GS1Parser.GS1Data(null);
        assertEquals("", data.rawValue);
    }
}
