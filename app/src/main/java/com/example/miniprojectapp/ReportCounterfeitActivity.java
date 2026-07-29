package com.example.miniprojectapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReportCounterfeitActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_counterfeit);

        EditText etMedicineName = findViewById(R.id.etMedicineName);
        EditText etBarcode = findViewById(R.id.etBarcode);
        EditText etManufacturer = findViewById(R.id.etManufacturer);
        EditText etDescription = findViewById(R.id.etDescription);
        EditText etLocation = findViewById(R.id.etLocation);
        EditText etContact = findViewById(R.id.etContact);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        // Pre-fill from intent
        String preName = getIntent().getStringExtra("medicineName");
        String preBarcode = getIntent().getStringExtra("barcode");
        if (preName != null) etMedicineName.setText(preName);
        if (preBarcode != null) etBarcode.setText(preBarcode);

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
            report.put("contact", etContact.getText().toString().trim());
            report.put("reportedBy", session.getUid());
            report.put("reportedByEmail", session.getEmail());
            report.put("timestamp", timestamp);
            report.put("status", "pending");

            DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("reports");
            reportsRef.child(reportId).setValue(report)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Report submitted successfully. Thank you!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}
