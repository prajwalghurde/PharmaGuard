package com.example.miniprojectapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Blockchain verification logic.
 * Simulates the hash-chaining supply chain verification done in BarcodeScanActivity.
 */
public class BlockchainVerificationTest {

    // ─── Hash Chain Integrity ──────────────────────────────────

    @Test
    public void hashChain_genesis_previousHashIsZeros() {
        String previousHash = "0000000000000000000000000000000000000000000000000000000000000000";
        assertEquals(64, previousHash.length());
        assertTrue(previousHash.matches("0+"));
    }

    @Test
    public void hashChain_block1_generatedCorrectly() {
        String barcode = "1234567890123";
        String name = "Paracetamol";
        String batch = "BATCH-001";
        String previousHash = "0000000000000000000000000000000000000000000000000000000000000000";

        String genesisData = barcode + name + batch + previousHash;
        String currentHash = HashUtil.sha256(genesisData);

        assertNotNull(currentHash);
        assertEquals(64, currentHash.length());
        assertNotEquals(previousHash, currentHash);
    }

    @Test
    public void hashChain_multiBlock_eachDependsOnPrevious() {
        String barcode = "9876543210";
        String name = "Ibuprofen";
        String batch = "BATCH-IB-001";
        String hash0 = "0000000000000000000000000000000000000000000000000000000000000000";

        String hash1 = HashUtil.sha256(barcode + name + batch + hash0);
        String hash2 = HashUtil.sha256(barcode + name + batch + hash1);
        String hash3 = HashUtil.sha256(barcode + name + batch + hash2);

        assertNotEquals(hash0, hash1);
        assertNotEquals(hash1, hash2);
        assertNotEquals(hash2, hash3);
        assertEquals(64, hash3.length());
    }

    @Test
    public void hashChain_reproducible_sameInputSameHash() {
        String input = "barcode123MedicineXBATCH001" + "0".repeat(64);
        assertEquals(HashUtil.sha256(input), HashUtil.sha256(input));
    }

    // ─── Tamper Detection ──────────────────────────────────────

    @Test
    public void tamperDetection_modifiedName_producesNewHash() {
        String barcode = "ABC123";
        String batch = "BATCH-001";
        String prevHash = "0".repeat(64);

        String realHash = HashUtil.sha256(barcode + "Paracetamol" + batch + prevHash);
        String tamperedHash = HashUtil.sha256(barcode + "Paracet4mol" + batch + prevHash);

        assertNotEquals("Tampered medicine name must not match original hash", realHash, tamperedHash);
    }

    @Test
    public void tamperDetection_modifiedBatch_producesNewHash() {
        String barcode = "ABC123";
        String name = "Paracetamol";
        String prevHash = "0".repeat(64);

        String realHash = HashUtil.sha256(barcode + name + "BATCH-001" + prevHash);
        String tamperedHash = HashUtil.sha256(barcode + name + "BATCH-999" + prevHash);

        assertNotEquals("Tampered batch number must not match original hash", realHash, tamperedHash);
    }

    @Test
    public void tamperDetection_modifiedBarcode_producesNewHash() {
        String name = "Paracetamol";
        String batch = "BATCH-001";
        String prevHash = "0".repeat(64);

        String realHash = HashUtil.sha256("REAL_BARCODE" + name + batch + prevHash);
        String tamperedHash = HashUtil.sha256("FAKE_BARCODE" + name + batch + prevHash);

        assertNotEquals("Counterfeit barcode must not match original hash", realHash, tamperedHash);
    }

    // ─── isVerified Flag Logic ─────────────────────────────────

    @Test
    public void isVerified_trueString_isAuthentic() {
        Medicine m = new Medicine();
        m.setIsVerified("true");
        assertEquals("true", m.getIsVerified());
        assertTrue(Boolean.parseBoolean(m.getIsVerified()));
    }

    @Test
    public void isVerified_falseString_isCounterfeit() {
        Medicine m = new Medicine();
        m.setIsVerified("false");
        assertFalse(Boolean.parseBoolean(m.getIsVerified()));
    }

    @Test
    public void isVerified_nullString_parsedSafely() {
        Medicine m = new Medicine();
        m.setIsVerified(null);
        assertFalse(Boolean.parseBoolean(m.getIsVerified()));
    }

    // ─── Hash Validation Against Stored Medicine ───────────────

    @Test
    public void medicineHash_storedVsRecomputed_matchesForAuthentic() {
        String barcode = "MED999";
        String name = "Amoxicillin";
        String batch = "BATCH-AMOX-2026";
        String prevHash = "0".repeat(64);
        String storedHash = HashUtil.sha256(barcode + name + batch + prevHash);

        // Simulate what the app does: recompute and compare
        String recomputed = HashUtil.sha256(barcode + name + batch + prevHash);
        assertEquals("Hash must match for authentic medicine", storedHash, recomputed);
    }

    @Test
    public void medicineHash_storedVsRecomputed_doesNotMatchForFake() {
        String barcode = "MED999";
        String name = "Amoxicillin";
        String batch = "BATCH-AMOX-2026";
        String prevHash = "0".repeat(64);
        String storedHash = HashUtil.sha256(barcode + name + batch + prevHash);

        // Attacker changes the batch number
        String fakeRecomputed = HashUtil.sha256(barcode + name + "FAKE-BATCH" + prevHash);
        assertNotEquals("Fake medicine hash must not match stored hash", storedHash, fakeRecomputed);
    }

    // ─── Edge Cases ────────────────────────────────────────────

    @Test
    public void sha256_veryLongMedicineData_doesNotThrow() {
        String longData = "X".repeat(10_000);
        String result = HashUtil.sha256(longData);
        assertNotNull(result);
        assertEquals(64, result.length());
    }

    @Test
    public void sha256_singleChar_producesValidHash() {
        String result = HashUtil.sha256("A");
        assertNotNull(result);
        assertEquals(64, result.length());
        assertTrue(result.matches("[0-9a-f]+"));
    }
}
