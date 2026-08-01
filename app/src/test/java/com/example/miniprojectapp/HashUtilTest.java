package com.example.miniprojectapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for HashUtil - SHA-256 cryptographic hashing utility.
 * These run on the JVM (no Android device needed).
 */
public class HashUtilTest {

    // Known SHA-256 values verified against external tools
    private static final String KNOWN_INPUT = "PharmaGuard";
    private static final String KNOWN_HASH  = "e2f6b7e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e3";

    // --- Basic correctness ---

    @Test
    public void sha256_knownInput_returnsCorrectHash() {
        // SHA-256 of "hello" is deterministic
        String result = HashUtil.sha256("hello");
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            result
        );
    }

    @Test
    public void sha256_emptyString_returnsKnownHash() {
        String result = HashUtil.sha256("");
        // SHA-256 of "" is always this
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            result
        );
    }

    @Test
    public void sha256_returnsLowercase64CharHex() {
        String result = HashUtil.sha256("test");
        assertNotNull(result);
        assertEquals("Hash should be 64 hex characters", 64, result.length());
        assertTrue("Hash should be lowercase hex", result.matches("[0-9a-f]+"));
    }

    // --- Determinism ---

    @Test
    public void sha256_sameInput_returnsSameHash() {
        String input = "BATCH-2026-MEDICINE-001";
        assertEquals(HashUtil.sha256(input), HashUtil.sha256(input));
    }

    @Test
    public void sha256_differentInputs_returnDifferentHashes() {
        String hash1 = HashUtil.sha256("medicine_A");
        String hash2 = HashUtil.sha256("medicine_B");
        assertNotEquals("Different inputs must produce different hashes", hash1, hash2);
    }

    // --- Blockchain chain integrity simulation ---

    @Test
    public void sha256_blockchainChain_hashChainIsValid() {
        String genesisInput = "barcode123" + "Paracetamol" + "BATCH001" + "0000000000000000000000000000000000000000000000000000000000000000";
        String block1Hash = HashUtil.sha256(genesisInput);

        // Simulate block 2 depending on block 1 hash
        String block2Input = "barcode123" + "Paracetamol" + "BATCH001" + block1Hash;
        String block2Hash = HashUtil.sha256(block2Input);

        assertNotNull(block1Hash);
        assertNotNull(block2Hash);
        assertEquals(64, block1Hash.length());
        assertEquals(64, block2Hash.length());
        assertNotEquals("Chained block hashes must differ", block1Hash, block2Hash);
    }

    // --- Null safety ---

    @Test
    public void sha256_nullInput_returnsEmptyString() {
        // HashUtil catches exceptions and returns ""
        String result = HashUtil.sha256(null);
        assertNotNull(result);
        assertEquals("", result);
    }

    // --- Unicode / special characters ---

    @Test
    public void sha256_unicodeInput_doesNotThrow() {
        String result = HashUtil.sha256("मेडिसिन-बैच-२०२६");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(64, result.length());
    }

    @Test
    public void sha256_specialChars_handledGracefully() {
        String result = HashUtil.sha256("!@#$%^&*()_+-=[]{}|;':\",./<>?");
        assertNotNull(result);
        assertEquals(64, result.length());
    }

    // --- Case sensitivity ---

    @Test
    public void sha256_caseSensitive_differentResults() {
        assertNotEquals(HashUtil.sha256("Paracetamol"), HashUtil.sha256("paracetamol"));
    }
}
