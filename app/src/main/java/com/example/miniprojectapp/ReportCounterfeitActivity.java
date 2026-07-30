package com.example.miniprojectapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class ReportCounterfeitActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 102;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private EditText etLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_counterfeit);

        EditText etMedicineName = findViewById(R.id.etMedicineName);
        EditText etBarcode = findViewById(R.id.etBarcode);
        EditText etManufacturer = findViewById(R.id.etManufacturer);
        EditText etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        EditText etContact = findViewById(R.id.etContact);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Pre-fill from intent
        String preName = getIntent().getStringExtra("medicineName");
        String preBarcode = getIntent().getStringExtra("barcode");
        if (preName != null) etMedicineName.setText(preName);
        if (preBarcode != null) etBarcode.setText(preBarcode);

        // Check & request location permission
        requestDeviceLocation();

        SessionManager session = new SessionManager(this);

        btnSubmit.setOnClickListener(v -> {
            String medName = etMedicineName.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();

            if (medName.isEmpty()) {
                etMedicineName.setError("Medicine name is required");
                etMedicineName.requestFocus();
                return;
            }
            if (desc.isEmpty()) {
                etDescription.setError("Please describe why you suspect counterfeit");
                etDescription.requestFocus();
                return;
            }

            String reportId = "report_" + System.currentTimeMillis();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            Map<String, Object> report = new HashMap<>();
            report.put("reportId", reportId);
            report.put("medicineName", medName);
            report.put("barcode", etBarcode.getText().toString().trim());
            report.put("manufacturer", etManufacturer.getText().toString().trim());
            report.put("description", desc);
            report.put("location", etLocation.getText().toString().trim());
            report.put("latitude", currentLatitude);
            report.put("longitude", currentLongitude);
            report.put("contact", etContact.getText().toString().trim());
            report.put("reportedBy", session.getUid());
            report.put("reportedByEmail", session.getEmail());
            report.put("timestamp", timestamp);
            report.put("status", "pending");

            DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("reports");
            reportsRef.child(reportId).setValue(report)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Report submitted successfully with geolocation. Thank you!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void requestDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void fetchLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();
                            reverseGeocode(location);
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void reverseGeocode(Location location) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String locationText = address.getLocality() != null ? address.getLocality() + ", " + address.getCountryName()
                            : address.getAddressLine(0);
                    runOnUiThread(() -> {
                        if (etLocation != null && etLocation.getText().toString().trim().isEmpty()) {
                            etLocation.setText(locationText);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        }
    }
}

