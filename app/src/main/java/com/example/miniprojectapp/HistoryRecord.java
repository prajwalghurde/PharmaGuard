package com.example.miniprojectapp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class HistoryRecord implements Serializable {
    String scanId, barcode, medicineName, scanType, status, timestamp,
           dosage, sideEffects, composition, manufacturer, expiryDate,
           price, manufacturingDate;

    public HistoryRecord() {}

    public HistoryRecord(String scanId, String barcode, String medicineName, String scanType,
                         String status, String timestamp, String dosage, String sideEffects,
                         String composition, String manufacturer, String expiryDate,
                         String price, String manufacturingDate) {
        this.scanId = scanId;
        this.barcode = barcode;
        this.medicineName = medicineName;
        this.scanType = scanType;
        this.status = status;
        this.timestamp = timestamp;
        this.dosage = dosage;
        this.sideEffects = sideEffects;
        this.composition = composition;
        this.manufacturer = manufacturer;
        this.expiryDate = expiryDate;
        this.price = price;
        this.manufacturingDate = manufacturingDate;
    }

    // Getters and Setters
    public String getScanId() { return scanId; }
    public void setScanId(String scanId) { this.scanId = scanId; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getScanType() { return scanType; }
    public void setScanType(String scanType) { this.scanType = scanType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getSideEffects() { return sideEffects; }
    public void setSideEffects(String sideEffects) { this.sideEffects = sideEffects; }
    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(String manufacturingDate) { this.manufacturingDate = manufacturingDate; }

    // Legacy compatibility
    public String getBarcodeNumber() { return barcode; }
    public String getName() { return medicineName; }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("scanId", scanId);
        result.put("barcode", barcode);
        result.put("medicineName", medicineName);
        result.put("scanType", scanType);
        result.put("status", status);
        result.put("timestamp", timestamp);
        result.put("dosage", dosage);
        result.put("sideEffects", sideEffects);
        result.put("composition", composition);
        result.put("manufacturer", manufacturer);
        result.put("expiryDate", expiryDate);
        result.put("price", price);
        result.put("manufacturingDate", manufacturingDate);
        return result;
    }
}
