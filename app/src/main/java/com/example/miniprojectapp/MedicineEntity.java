package com.example.miniprojectapp;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_medicines")
public class MedicineEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String barcode;
    public String name;
    public String genericName;
    public String manufacturer;
    public String dosage;
    public String sideEffects;
    public String composition;
    public String expiryDate;
    public String manufacturingDate;
    public String price;
    public String category;
    public String imageUrl;
    public String isVerified;
    public String batchNumber;
    public String previousHash;
    public String currentHash;
    public long cachedAt;

    public MedicineEntity() {
        this.id = "";
    }

    public static MedicineEntity fromMedicine(Medicine med) {
        MedicineEntity entity = new MedicineEntity();
        if (med.getBarcode() != null && !med.getBarcode().isEmpty()) {
            entity.id = "barcode_" + med.getBarcode();
        } else if (med.getName() != null && !med.getName().isEmpty()) {
            entity.id = "name_" + med.getName().toLowerCase().trim();
        } else {
            entity.id = "med_" + System.currentTimeMillis();
        }

        entity.barcode = med.getBarcode();
        entity.name = med.getName();
        entity.genericName = med.getGenericName();
        entity.manufacturer = med.getManufacturer();
        entity.dosage = med.getDosage();
        entity.sideEffects = med.getSideEffects();
        entity.composition = med.getComposition();
        entity.expiryDate = med.getExpiryDate();
        entity.manufacturingDate = med.getManufacturingDate();
        entity.price = med.getPrice();
        entity.category = med.getCategory();
        entity.imageUrl = med.getImageUrl();
        entity.isVerified = med.getIsVerified();
        entity.batchNumber = med.getBatchNumber();
        entity.previousHash = med.getPreviousHash();
        entity.currentHash = med.getCurrentHash();
        entity.cachedAt = System.currentTimeMillis();
        return entity;
    }

    public Medicine toMedicine() {
        Medicine med = new Medicine();
        med.setBarcode(this.barcode);
        med.setName(this.name);
        med.setGenericName(this.genericName);
        med.setManufacturer(this.manufacturer);
        med.setDosage(this.dosage);
        med.setSideEffects(this.sideEffects);
        med.setComposition(this.composition);
        med.setExpiryDate(this.expiryDate);
        med.setManufacturingDate(this.manufacturingDate);
        med.setPrice(this.price);
        med.setCategory(this.category);
        med.setImageUrl(this.imageUrl);
        med.setIsVerified(this.isVerified);
        med.setBatchNumber(this.batchNumber);
        med.setPreviousHash(this.previousHash);
        med.setCurrentHash(this.currentHash);
        return med;
    }
}
