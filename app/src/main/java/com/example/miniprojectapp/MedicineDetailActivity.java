package com.example.miniprojectapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MedicineDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_detail);

        Medicine medicine = (Medicine) getIntent().getSerializableExtra("medicine");
        String status = getIntent().getStringExtra("status");
        String source = getIntent().getStringExtra("source");
        String scanType = getIntent().getStringExtra("scanType");
        String barcode = getIntent().getStringExtra("barcode");

        if (medicine == null) {
            finish();
            return;
        }

        // Status banner
        TextView tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvStatusBanner.setText(status);
        if ("Verified".equals(status) || "AI Verified".equals(status)) {
            tvStatusBanner.setBackgroundResource(R.drawable.status_verified);
        } else if ("Not Found".equals(status)) {
            tvStatusBanner.setBackgroundResource(R.drawable.status_danger);
            tvStatusBanner.setText("Not Found - Potential Counterfeit");
        } else {
            tvStatusBanner.setBackgroundResource(R.drawable.status_warning);
        }

        // Medicine info
        TextView tvMedName = findViewById(R.id.tvMedName);
        tvMedName.setText(medicine.getName() != null ? medicine.getName() : "Unknown Medicine");

        TextView tvMedGeneric = findViewById(R.id.tvMedGeneric);
        if (medicine.getGenericName() != null && !medicine.getGenericName().isEmpty()) {
            tvMedGeneric.setText("Generic: " + medicine.getGenericName());
        } else {
            tvMedGeneric.setVisibility(View.GONE);
        }

        TextView tvSource = findViewById(R.id.tvSource);
        tvSource.setText("Source: " + (source != null ? source : "unknown"));

        // Detail fields
        TextView tvBarcode = findViewById(R.id.tvBarcode);
        String barcodeVal = barcode != null ? barcode : medicine.getBarcode();
        if (barcodeVal != null && !barcodeVal.isEmpty()) {
            tvBarcode.setText("Barcode: " + barcodeVal);
        } else {
            tvBarcode.setVisibility(View.GONE);
        }

        TextView tvManufacturer = findViewById(R.id.tvManufacturer);
        if (medicine.getManufacturer() != null && !medicine.getManufacturer().isEmpty()) {
            tvManufacturer.setText("Manufacturer: " + medicine.getManufacturer());
        } else {
            tvManufacturer.setVisibility(View.GONE);
        }

        TextView tvCategory = findViewById(R.id.tvCategory);
        if (medicine.getCategory() != null && !medicine.getCategory().isEmpty()) {
            tvCategory.setText("Category: " + medicine.getCategory());
        } else {
            tvCategory.setVisibility(View.GONE);
        }

        TextView tvPrice = findViewById(R.id.tvPrice);
        if (medicine.getPrice() != null && !medicine.getPrice().isEmpty()) {
            tvPrice.setText("Price: " + medicine.getPrice());
        } else {
            tvPrice.setVisibility(View.GONE);
        }

        TextView tvDates = findViewById(R.id.tvDates);
        StringBuilder dates = new StringBuilder();
        if (medicine.getManufacturingDate() != null && !medicine.getManufacturingDate().isEmpty())
            dates.append("Manufactured: ").append(medicine.getManufacturingDate()).append("\n");
        if (medicine.getExpiryDate() != null && !medicine.getExpiryDate().isEmpty())
            dates.append("Expires: ").append(medicine.getExpiryDate());
        if (dates.length() > 0) {
            tvDates.setText(dates.toString());
        } else {
            tvDates.setVisibility(View.GONE);
        }

        // Dosage
        TextView tvDosage = findViewById(R.id.tvDosage);
        if (medicine.getDosage() != null && !medicine.getDosage().isEmpty()) {
            tvDosage.setText(medicine.getDosage());
        } else {
            tvDosage.setText("No dosage information available.");
        }

        // Side effects
        TextView tvSideEffects = findViewById(R.id.tvSideEffects);
        if (medicine.getSideEffects() != null && !medicine.getSideEffects().isEmpty()) {
            tvSideEffects.setText(medicine.getSideEffects());
        } else {
            tvSideEffects.setText("No side effect information available.");
        }

        // Composition
        TextView tvComposition = findViewById(R.id.tvComposition);
        if (medicine.getComposition() != null && !medicine.getComposition().isEmpty()) {
            tvComposition.setText(medicine.getComposition());
        } else {
            tvComposition.setText("No composition information available.");
        }

        // Expiry date alerts (Task 13)
        TextView tvExpiryAlert = findViewById(R.id.tvExpiryAlert);
        checkExpiry(medicine.getExpiryDate(), tvExpiryAlert);

        // Report button
        Button btnReport = findViewById(R.id.btnReport);
        btnReport.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportCounterfeitActivity.class);
            intent.putExtra("medicineName", medicine.getName());
            intent.putExtra("barcode", barcodeVal);
            startActivity(intent);
        });
    }

    private void checkExpiry(String expiryDateStr, TextView tvExpiryAlert) {
        if (expiryDateStr == null || expiryDateStr.isEmpty()) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date expiryDate = sdf.parse(expiryDateStr);
            if (expiryDate == null) return;

            Date now = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(now);
            cal.add(Calendar.DAY_OF_MONTH, 30);
            Date thirtyDaysFromNow = cal.getTime();

            if (expiryDate.before(now)) {
                tvExpiryAlert.setText("EXPIRED - Do not use this medicine!");
                tvExpiryAlert.setBackgroundResource(R.drawable.status_danger);
                tvExpiryAlert.setVisibility(View.VISIBLE);
            } else if (expiryDate.before(thirtyDaysFromNow)) {
                tvExpiryAlert.setText("Expiring soon - within 30 days");
                tvExpiryAlert.setBackgroundResource(R.drawable.status_warning);
                tvExpiryAlert.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            // Could not parse date, skip alert
        }
    }
}
