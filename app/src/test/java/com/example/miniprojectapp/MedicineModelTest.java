package com.example.miniprojectapp;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Map;

/**
 * Unit tests for Medicine data model.
 * Verifies construction, getters/setters, and toMap() serialization.
 */
public class MedicineModelTest {

    private Medicine buildSampleMedicine() {
        return new Medicine(
            "1234567890123",         // barcode
            "Paracetamol 500mg",     // name
            "Acetaminophen",         // genericName
            "GSK Ltd",               // manufacturer
            "500mg",                 // dosage
            "Nausea, rash",          // sideEffects
            "Paracetamol BP 500mg",  // composition
            "2027-12-31",            // expiryDate
            "2024-01-01",            // manufacturingDate
            "₹25",                   // price
            "Analgesic",             // category
            "",                      // imageUrl
            "true",                  // isVerified
            "BATCH-2026-001",        // batchNumber
            "0000000000000000000000000000000000000000000000000000000000000000", // previousHash
            "abc123def456"           // currentHash
        );
    }

    // ─── Constructor ───────────────────────────────────────────

    @Test
    public void medicine_defaultConstructor_noException() {
        Medicine m = new Medicine();
        assertNotNull(m);
    }

    @Test
    public void medicine_fullConstructor_fieldsSetCorrectly() {
        Medicine m = buildSampleMedicine();
        assertEquals("1234567890123", m.getBarcode());
        assertEquals("Paracetamol 500mg", m.getName());
        assertEquals("Acetaminophen", m.getGenericName());
        assertEquals("GSK Ltd", m.getManufacturer());
        assertEquals("500mg", m.getDosage());
        assertEquals("Nausea, rash", m.getSideEffects());
        assertEquals("Paracetamol BP 500mg", m.getComposition());
        assertEquals("2027-12-31", m.getExpiryDate());
        assertEquals("2024-01-01", m.getManufacturingDate());
        assertEquals("₹25", m.getPrice());
        assertEquals("Analgesic", m.getCategory());
        assertEquals("true", m.getIsVerified());
        assertEquals("BATCH-2026-001", m.getBatchNumber());
    }

    // ─── Getters/Setters ───────────────────────────────────────

    @Test
    public void medicine_setBarcode_getBarcode() {
        Medicine m = new Medicine();
        m.setBarcode("9876543210");
        assertEquals("9876543210", m.getBarcode());
    }

    @Test
    public void medicine_setName_getName() {
        Medicine m = new Medicine();
        m.setName("Ibuprofen");
        assertEquals("Ibuprofen", m.getName());
    }

    @Test
    public void medicine_setIsVerified_true() {
        Medicine m = new Medicine();
        m.setIsVerified("true");
        assertEquals("true", m.getIsVerified());
    }

    @Test
    public void medicine_setIsVerified_false() {
        Medicine m = new Medicine();
        m.setIsVerified("false");
        assertEquals("false", m.getIsVerified());
    }

    @Test
    public void medicine_setPreviousHash_getHash() {
        Medicine m = new Medicine();
        String hash = "abc123";
        m.setPreviousHash(hash);
        assertEquals(hash, m.getPreviousHash());
    }

    @Test
    public void medicine_setCurrentHash_getHash() {
        Medicine m = new Medicine();
        String hash = HashUtil.sha256("test-medicine-batch");
        m.setCurrentHash(hash);
        assertEquals(hash, m.getCurrentHash());
    }

    // ─── Legacy Getters ────────────────────────────────────────

    @Test
    public void medicine_getBarcodeNumber_returnsSameAsGetBarcode() {
        Medicine m = buildSampleMedicine();
        assertEquals(m.getBarcode(), m.getBarcodeNumber());
    }

    @Test
    public void medicine_setBarcodeNumber_updatesBarcode() {
        Medicine m = new Medicine();
        m.setBarcodeNumber("LEGACY123");
        assertEquals("LEGACY123", m.getBarcode());
        assertEquals("LEGACY123", m.getBarcodeNumber());
    }

    // ─── toMap() Serialization ─────────────────────────────────

    @Test
    public void medicine_toMap_containsAllKeys() {
        Medicine m = buildSampleMedicine();
        Map<String, Object> map = m.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("barcode"));
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("genericName"));
        assertTrue(map.containsKey("manufacturer"));
        assertTrue(map.containsKey("dosage"));
        assertTrue(map.containsKey("sideEffects"));
        assertTrue(map.containsKey("composition"));
        assertTrue(map.containsKey("expiryDate"));
        assertTrue(map.containsKey("manufacturingDate"));
        assertTrue(map.containsKey("price"));
        assertTrue(map.containsKey("category"));
        assertTrue(map.containsKey("imageUrl"));
        assertTrue(map.containsKey("isVerified"));
        assertTrue(map.containsKey("batchNumber"));
        assertTrue(map.containsKey("previousHash"));
        assertTrue(map.containsKey("currentHash"));
    }

    @Test
    public void medicine_toMap_valuesMatchGetters() {
        Medicine m = buildSampleMedicine();
        Map<String, Object> map = m.toMap();

        assertEquals(m.getBarcode(), map.get("barcode"));
        assertEquals(m.getName(), map.get("name"));
        assertEquals(m.getManufacturer(), map.get("manufacturer"));
        assertEquals(m.getDosage(), map.get("dosage"));
        assertEquals(m.getIsVerified(), map.get("isVerified"));
        assertEquals(m.getBatchNumber(), map.get("batchNumber"));
    }

    @Test
    public void medicine_toMap_withNullFields_doesNotThrow() {
        Medicine m = new Medicine();  // all fields null
        Map<String, Object> map = m.toMap();
        assertNotNull(map);
        assertEquals(16, map.size());
    }
}
