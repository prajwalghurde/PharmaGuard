package com.example.miniprojectapp;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Map;

/**
 * Unit tests for HistoryRecord data model.
 * Verifies construction, getters/setters, toMap() serialization, and legacy compatibility.
 */
public class HistoryRecordTest {

    private HistoryRecord buildSampleRecord() {
        return new HistoryRecord(
            "scan_001",              // scanId
            "1234567890123",         // barcode
            "Paracetamol 500mg",     // medicineName
            "BARCODE",               // scanType
            "VERIFIED",              // status
            "2026-07-30T10:00:00Z",  // timestamp
            "500mg",                 // dosage
            "Mild nausea",           // sideEffects
            "Paracetamol BP",        // composition
            "GSK Ltd",               // manufacturer
            "2027-12-31",            // expiryDate
            "₹25",                   // price
            "2024-01-01"             // manufacturingDate
        );
    }

    // ─── Constructor ───────────────────────────────────────────

    @Test
    public void historyRecord_defaultConstructor_noException() {
        HistoryRecord r = new HistoryRecord();
        assertNotNull(r);
    }

    @Test
    public void historyRecord_fullConstructor_fieldsSetCorrectly() {
        HistoryRecord r = buildSampleRecord();
        assertEquals("scan_001", r.getScanId());
        assertEquals("1234567890123", r.getBarcode());
        assertEquals("Paracetamol 500mg", r.getMedicineName());
        assertEquals("BARCODE", r.getScanType());
        assertEquals("VERIFIED", r.getStatus());
        assertEquals("2026-07-30T10:00:00Z", r.getTimestamp());
        assertEquals("500mg", r.getDosage());
        assertEquals("Mild nausea", r.getSideEffects());
        assertEquals("Paracetamol BP", r.getComposition());
        assertEquals("GSK Ltd", r.getManufacturer());
        assertEquals("2027-12-31", r.getExpiryDate());
        assertEquals("₹25", r.getPrice());
        assertEquals("2024-01-01", r.getManufacturingDate());
    }

    // ─── Scan Types ────────────────────────────────────────────

    @Test
    public void historyRecord_scanType_barcode() {
        HistoryRecord r = new HistoryRecord();
        r.setScanType("BARCODE");
        assertEquals("BARCODE", r.getScanType());
    }

    @Test
    public void historyRecord_scanType_photo() {
        HistoryRecord r = new HistoryRecord();
        r.setScanType("PHOTO");
        assertEquals("PHOTO", r.getScanType());
    }

    // ─── Status Values ─────────────────────────────────────────

    @Test
    public void historyRecord_status_verified() {
        HistoryRecord r = new HistoryRecord();
        r.setStatus("VERIFIED");
        assertEquals("VERIFIED", r.getStatus());
    }

    @Test
    public void historyRecord_status_counterfeit() {
        HistoryRecord r = new HistoryRecord();
        r.setStatus("COUNTERFEIT");
        assertEquals("COUNTERFEIT", r.getStatus());
    }

    @Test
    public void historyRecord_status_unknown() {
        HistoryRecord r = new HistoryRecord();
        r.setStatus("UNKNOWN");
        assertEquals("UNKNOWN", r.getStatus());
    }

    // ─── Legacy Compatibility ──────────────────────────────────

    @Test
    public void historyRecord_getBarcodeNumber_returnsSameAsGetBarcode() {
        HistoryRecord r = buildSampleRecord();
        assertEquals(r.getBarcode(), r.getBarcodeNumber());
    }

    @Test
    public void historyRecord_getName_returnsMedicineName() {
        HistoryRecord r = buildSampleRecord();
        assertEquals(r.getMedicineName(), r.getName());
    }

    // ─── toMap() Serialization ─────────────────────────────────

    @Test
    public void historyRecord_toMap_containsAllKeys() {
        HistoryRecord r = buildSampleRecord();
        Map<String, Object> map = r.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("scanId"));
        assertTrue(map.containsKey("barcode"));
        assertTrue(map.containsKey("medicineName"));
        assertTrue(map.containsKey("scanType"));
        assertTrue(map.containsKey("status"));
        assertTrue(map.containsKey("timestamp"));
        assertTrue(map.containsKey("dosage"));
        assertTrue(map.containsKey("sideEffects"));
        assertTrue(map.containsKey("composition"));
        assertTrue(map.containsKey("manufacturer"));
        assertTrue(map.containsKey("expiryDate"));
        assertTrue(map.containsKey("price"));
        assertTrue(map.containsKey("manufacturingDate"));
    }

    @Test
    public void historyRecord_toMap_valuesMatchGetters() {
        HistoryRecord r = buildSampleRecord();
        Map<String, Object> map = r.toMap();

        assertEquals(r.getScanId(), map.get("scanId"));
        assertEquals(r.getBarcode(), map.get("barcode"));
        assertEquals(r.getMedicineName(), map.get("medicineName"));
        assertEquals(r.getScanType(), map.get("scanType"));
        assertEquals(r.getStatus(), map.get("status"));
        assertEquals(r.getTimestamp(), map.get("timestamp"));
    }

    @Test
    public void historyRecord_toMap_hasCorrectSize() {
        HistoryRecord r = buildSampleRecord();
        Map<String, Object> map = r.toMap();
        assertEquals(13, map.size());
    }

    @Test
    public void historyRecord_toMap_withNullFields_doesNotThrow() {
        HistoryRecord r = new HistoryRecord();
        Map<String, Object> map = r.toMap();
        assertNotNull(map);
        assertEquals(13, map.size());
    }
}
