package com.example.miniprojectapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddMedicineActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        EditText etBarcode = findViewById(R.id.etBarcode);
        EditText etName = findViewById(R.id.etName);
        EditText etGenericName = findViewById(R.id.etGenericName);
        EditText etManufacturer = findViewById(R.id.etManufacturer);
        EditText etDosage = findViewById(R.id.etDosage);
        EditText etComposition = findViewById(R.id.etComposition);
        EditText etSideEffects = findViewById(R.id.etSideEffects);
        EditText etCategory = findViewById(R.id.etCategory);
        EditText etPrice = findViewById(R.id.etPrice);
        EditText etMfgDate = findViewById(R.id.etMfgDate);
        EditText etExpiryDate = findViewById(R.id.etExpiryDate);
        Button btnAdd = findViewById(R.id.btnAddMedicine);

        MedicineDatabase medicineDb = new MedicineDatabase();

        btnAdd.setOnClickListener(v -> {
            String barcode = etBarcode.getText().toString().trim();
            String name = etName.getText().toString().trim();

            if (barcode.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Barcode and Name are required", Toast.LENGTH_SHORT).show();
                return;
            }

            String batchNumber =
                    "BATCH" + System.currentTimeMillis();

            String previousHash = "GENESIS";

            String currentHash =
                    HashUtil.sha256(
                            barcode +
                                    name +
                                    batchNumber +
                                    previousHash
                    );

            Medicine medicine = new Medicine(
                    barcode,
                    name,
                    etGenericName.getText().toString().trim(),
                    etManufacturer.getText().toString().trim(),
                    etDosage.getText().toString().trim(),
                    etSideEffects.getText().toString().trim(),
                    etComposition.getText().toString().trim(),
                    etExpiryDate.getText().toString().trim(),
                    etMfgDate.getText().toString().trim(),
                    etPrice.getText().toString().trim(),
                    etCategory.getText().toString().trim(),
                    "",
                    "true",
                    batchNumber,
                    previousHash,
                    currentHash
            );

            medicineDb.addMedicine(medicine, new MedicineDatabase.MedicineCallback() {
                @Override
                public void onResult(Medicine med, String source) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddMedicineActivity.this, "Medicine added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddMedicineActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
}
