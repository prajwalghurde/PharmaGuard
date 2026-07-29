package com.example.miniprojectapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MedicineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMedicine(MedicineEntity medicine);

    @Query("SELECT * FROM cached_medicines WHERE barcode = :barcode LIMIT 1")
    MedicineEntity getMedicineByBarcode(String barcode);

    @Query("SELECT * FROM cached_medicines WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    MedicineEntity getMedicineByName(String name);

    @Query("SELECT * FROM cached_medicines ORDER BY cachedAt DESC")
    List<MedicineEntity> getAllCachedMedicines();

    @Query("DELETE FROM cached_medicines")
    void clearCache();
}
