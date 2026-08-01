package com.example.miniprojectapp;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented tests for Room local cache (AppDatabase + MedicineDao + MedicineEntity).
 * Uses in-memory database — wiped after every test run.
 * Must run on a real device or emulator.
 */
@RunWith(AndroidJUnit4.class)
public class RoomDatabaseTest {

    private AppDatabase db;
    private MedicineDao dao;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase.class
        ).allowMainThreadQueries().build();
        dao = db.medicineDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ─── Helper ────────────────────────────────────────────────

    private MedicineEntity buildEntity(String barcode, String name) {
        MedicineEntity e = new MedicineEntity();
        e.id = "barcode_" + barcode;
        e.barcode = barcode;
        e.name = name;
        e.genericName = "GenericName";
        e.manufacturer = "TestMfg";
        e.dosage = "500mg";
        e.sideEffects = "None";
        e.composition = "Active ingredient";
        e.expiryDate = "2027-12-31";
        e.manufacturingDate = "2024-01-01";
        e.price = "₹50";
        e.category = "Analgesic";
        e.imageUrl = "";
        e.isVerified = "true";
        e.batchNumber = "BATCH-001";
        e.previousHash = "0000";
        e.currentHash = HashUtil.sha256(barcode + name);
        e.cachedAt = System.currentTimeMillis();
        return e;
    }

    // ─── Insert & Query by Barcode ─────────────────────────────

    @Test
    public void insertMedicine_andGetByBarcode_returnsCorrectEntity() {
        MedicineEntity entity = buildEntity("1234567890", "Paracetamol");
        dao.insertMedicine(entity);

        MedicineEntity result = dao.getMedicineByBarcode("1234567890");
        assertNotNull("Entity should be found by barcode", result);
        assertEquals("1234567890", result.barcode);
        assertEquals("Paracetamol", result.name);
    }

    @Test
    public void getMedicineByBarcode_nonExistent_returnsNull() {
        MedicineEntity result = dao.getMedicineByBarcode("DOES_NOT_EXIST");
        assertNull("Non-existent barcode should return null", result);
    }

    // ─── Insert & Query by Name ────────────────────────────────

    @Test
    public void insertMedicine_andGetByName_returnsCorrectEntity() {
        dao.insertMedicine(buildEntity("AAA001", "Ibuprofen"));

        MedicineEntity result = dao.getMedicineByName("Ibuprofen");
        assertNotNull(result);
        assertEquals("Ibuprofen", result.name);
    }

    @Test
    public void getMedicineByName_caseInsensitive_returnsCorrectEntity() {
        dao.insertMedicine(buildEntity("BBB002", "Amoxicillin"));

        // Query with different case
        MedicineEntity result = dao.getMedicineByName("AMOXICILLIN");
        assertNotNull("Name lookup should be case-insensitive", result);
        assertEquals("Amoxicillin", result.name);
    }

    @Test
    public void getMedicineByName_nonExistent_returnsNull() {
        MedicineEntity result = dao.getMedicineByName("NonExistentMedicine");
        assertNull(result);
    }

    // ─── Get All Cached Medicines ──────────────────────────────

    @Test
    public void getAllCachedMedicines_afterInserts_returnsAll() {
        dao.insertMedicine(buildEntity("X001", "MedA"));
        dao.insertMedicine(buildEntity("X002", "MedB"));
        dao.insertMedicine(buildEntity("X003", "MedC"));

        List<MedicineEntity> all = dao.getAllCachedMedicines();
        assertEquals(3, all.size());
    }

    @Test
    public void getAllCachedMedicines_empty_returnsEmptyList() {
        List<MedicineEntity> all = dao.getAllCachedMedicines();
        assertNotNull(all);
        assertEquals(0, all.size());
    }

    @Test
    public void getAllCachedMedicines_orderedByNewestFirst() throws InterruptedException {
        MedicineEntity older = buildEntity("T001", "OlderMed");
        older.cachedAt = 1000L;
        dao.insertMedicine(older);

        Thread.sleep(10);

        MedicineEntity newer = buildEntity("T002", "NewerMed");
        newer.cachedAt = 2000L;
        dao.insertMedicine(newer);

        List<MedicineEntity> all = dao.getAllCachedMedicines();
        assertEquals(2, all.size());
        assertEquals("NewerMed", all.get(0).name); // newest first (DESC)
    }

    // ─── Upsert (REPLACE on conflict) ─────────────────────────

    @Test
    public void insertMedicine_samePrimaryKey_replacesOldRecord() {
        MedicineEntity first = buildEntity("DUP001", "OldName");
        dao.insertMedicine(first);

        MedicineEntity updated = new MedicineEntity();
        updated.id = "barcode_DUP001"; // same primary key
        updated.barcode = "DUP001";
        updated.name = "UpdatedName";
        updated.cachedAt = System.currentTimeMillis();
        dao.insertMedicine(updated);

        MedicineEntity result = dao.getMedicineByBarcode("DUP001");
        assertNotNull(result);
        assertEquals("UpdatedName", result.name);
    }

    // ─── Clear Cache ───────────────────────────────────────────

    @Test
    public void clearCache_deletesAllRecords() {
        dao.insertMedicine(buildEntity("C001", "MedOne"));
        dao.insertMedicine(buildEntity("C002", "MedTwo"));
        dao.clearCache();

        List<MedicineEntity> all = dao.getAllCachedMedicines();
        assertEquals(0, all.size());
    }

    @Test
    public void clearCache_onEmptyTable_doesNotThrow() {
        dao.clearCache(); // should not throw
        assertEquals(0, dao.getAllCachedMedicines().size());
    }

    // ─── Data Integrity ────────────────────────────────────────

    @Test
    public void insertMedicine_hashFields_persistCorrectly() {
        MedicineEntity e = buildEntity("HASH01", "HashMed");
        String expectedHash = HashUtil.sha256("HASH01HashMed");
        e.currentHash = expectedHash;
        dao.insertMedicine(e);

        MedicineEntity result = dao.getMedicineByBarcode("HASH01");
        assertNotNull(result);
        assertEquals(expectedHash, result.currentHash);
    }

    @Test
    public void insertMedicine_isVerifiedTrue_persistsCorrectly() {
        MedicineEntity e = buildEntity("V001", "VerifiedMed");
        e.isVerified = "true";
        dao.insertMedicine(e);

        MedicineEntity result = dao.getMedicineByBarcode("V001");
        assertNotNull(result);
        assertEquals("true", result.isVerified);
    }

    @Test
    public void insertMedicine_isVerifiedFalse_persistsCorrectly() {
        MedicineEntity e = buildEntity("V002", "UnverifiedMed");
        e.isVerified = "false";
        dao.insertMedicine(e);

        MedicineEntity result = dao.getMedicineByBarcode("V002");
        assertNotNull(result);
        assertEquals("false", result.isVerified);
    }

    @Test
    public void insertMedicine_batchNumber_persistsCorrectly() {
        MedicineEntity e = buildEntity("B001", "BatchMed");
        e.batchNumber = "PHARMA-BATCH-2026";
        dao.insertMedicine(e);

        MedicineEntity result = dao.getMedicineByBarcode("B001");
        assertNotNull(result);
        assertEquals("PHARMA-BATCH-2026", result.batchNumber);
    }

    @Test
    public void insertMedicine_expiryDate_persistsCorrectly() {
        MedicineEntity e = buildEntity("E001", "ExpiryMed");
        e.expiryDate = "2028-06-30";
        dao.insertMedicine(e);

        MedicineEntity result = dao.getMedicineByBarcode("E001");
        assertNotNull(result);
        assertEquals("2028-06-30", result.expiryDate);
    }

    // ─── fromMedicine() & toMedicine() round-trip ──────────────

    @Test
    public void fromMedicine_toMedicine_roundTripPreservesData() {
        Medicine original = new Medicine(
            "ROUND001", "RoundTrip", "Generic", "Mfg",
            "250mg", "None", "Active",
            "2027-01-01", "2024-06-01", "₹10",
            "Tablet", "", "true", "BATCH-RT",
            "0000", HashUtil.sha256("ROUND001RoundTrip")
        );

        MedicineEntity entity = MedicineEntity.fromMedicine(original);
        dao.insertMedicine(entity);

        MedicineEntity fetched = dao.getMedicineByBarcode("ROUND001");
        assertNotNull(fetched);

        Medicine restored = fetched.toMedicine();
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getBarcode(), restored.getBarcode());
        assertEquals(original.getManufacturer(), restored.getManufacturer());
        assertEquals(original.getDosage(), restored.getDosage());
        assertEquals(original.getIsVerified(), restored.getIsVerified());
        assertEquals(original.getBatchNumber(), restored.getBatchNumber());
        assertEquals(original.getCurrentHash(), restored.getCurrentHash());
    }

    // ─── Large Dataset ─────────────────────────────────────────

    @Test
    public void insert100medicines_allRetrievableByBarcode() {
        for (int i = 0; i < 100; i++) {
            dao.insertMedicine(buildEntity("CODE_" + i, "Medicine_" + i));
        }
        assertEquals(100, dao.getAllCachedMedicines().size());

        MedicineEntity result = dao.getMedicineByBarcode("CODE_50");
        assertNotNull(result);
        assertEquals("Medicine_50", result.name);
    }
}
