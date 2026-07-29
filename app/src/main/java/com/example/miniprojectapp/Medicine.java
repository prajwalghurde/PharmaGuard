package com.example.miniprojectapp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Medicine implements Serializable {
    String barcode, name, genericName, manufacturer, dosage, sideEffects,
           composition, expiryDate, manufacturingDate, price, category,
           imageUrl, isVerified , batchNumber, previousHash, currentHash;

    public Medicine() {}

    public Medicine(String barcode, String name, String genericName, String manufacturer,
                    String dosage, String sideEffects, String composition,
                    String expiryDate, String manufacturingDate, String price,
                    String category, String imageUrl, String isVerified,String batchNumber,
                    String previousHash, String currentHash) {
        this.barcode = barcode;
        this.name = name;
        this.genericName = genericName;
        this.manufacturer = manufacturer;
        this.dosage = dosage;
        this.sideEffects = sideEffects;
        this.composition = composition;
        this.expiryDate = expiryDate;
        this.manufacturingDate = manufacturingDate;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.isVerified = isVerified;
        this.batchNumber = batchNumber;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
    }

    // Getters and Setters
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getSideEffects() { return sideEffects; }
    public void setSideEffects(String sideEffects) { this.sideEffects = sideEffects; }
    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public String getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(String manufacturingDate) { this.manufacturingDate = manufacturingDate; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getIsVerified() { return isVerified; }
    public void setIsVerified(String isVerified) { this.isVerified = isVerified; }

    // Legacy getters for backward compatibility
    public String getBarcodeNumber() { return barcode; }
    public void setBarcodeNumber(String barcodeNumber) { this.barcode = barcodeNumber; }

    public String getBatchNumber() { return batchNumber; }

    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getPreviousHash() { return previousHash; }

    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getCurrentHash() { return currentHash; }

    public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("barcode", barcode);
        result.put("name", name);
        result.put("genericName", genericName);
        result.put("manufacturer", manufacturer);
        result.put("dosage", dosage);
        result.put("sideEffects", sideEffects);
        result.put("composition", composition);
        result.put("expiryDate", expiryDate);
        result.put("manufacturingDate", manufacturingDate);
        result.put("price", price);
        result.put("category", category);
        result.put("imageUrl", imageUrl);
        result.put("isVerified", isVerified);
        result.put("batchNumber", batchNumber);
        result.put("previousHash", previousHash);
        result.put("currentHash", currentHash);
        return result;
    }
}
