package com.example.miniprojectapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.json.JSONArray;
import org.json.JSONObject;

public class PhotoScanActivity extends AppCompatActivity {


    private ImageView imageView;
    private Button btnCapture, btnViewDetails;
    private ProgressBar progressBar;
    private TextView tvStatus, tvMedicineName, tvMedicineInfo;
    private LinearLayout resultCard;
    private MedicineDatabase medicineDb;
    private SessionManager sessionManager;
    private Uri imageUri;
    private Medicine detectedMedicine;
    private String detectedStatus, detectedSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_scan);

        imageView = findViewById(R.id.imageView);
        btnCapture = findViewById(R.id.btnCapture);
        btnViewDetails = findViewById(R.id.btnViewDetails);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvMedicineName = findViewById(R.id.tvMedicineName);
        tvMedicineInfo = findViewById(R.id.tvMedicineInfo);
        resultCard = findViewById(R.id.resultCard);

        medicineDb = new MedicineDatabase(this);
        sessionManager = new SessionManager(this);

        btnCapture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 201);
            } else {
                openCamera();
            }
        });

        btnViewDetails.setOnClickListener(v -> {
            if (detectedMedicine != null) {
                Intent intent = new Intent(this, MedicineDetailActivity.class);
                intent.putExtra("medicine", detectedMedicine);
                intent.putExtra("status", detectedStatus);
                intent.putExtra("source", detectedSource);
                intent.putExtra("scanType", "photo");
                startActivity(intent);
            }
        });
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Medicine Photo");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From PharmaGuard");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && imageUri != null) {
            imageView.setImageURI(imageUri);
            tvStatus.setText("Status: Analyzing image with AI...");
            progressBar.setVisibility(View.VISIBLE);
            resultCard.setVisibility(View.GONE);
            analyzeImage();
        } else {
            Toast.makeText(this, "No photo taken", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractMedicineNameWithGemini(String ocrText) {

        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                MedicineDatabase.GEMINI_API_KEY   // Set your API key in MedicineDatabase.java
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder()
                .addText(
                        "You are an expert pharmacist and medical assistant.\n\n" +

                                "The following text was extracted from a medicine package using OCR and may contain spelling mistakes.\n" +

                                "Your task is to:\n" +
                                "1. Correct OCR mistakes.\n" +
                                "2. Identify the medicine brand name.\n" +
                                "3. Using your medical knowledge, provide the most likely information about that medicine.\n\n" +

                                "Return ONLY valid JSON in exactly this format:\n\n" +

                                "{\n" +
                                "\"name\":\"\",\n" +
                                "\"generic_name\":\"\",\n" +
                                "\"uses\":\"\",\n" +
                                "\"dosage\":\"\",\n" +
                                "\"side_effects\":\"\",\n" +
                                "\"composition\":\"\",\n" +
                                "\"warnings\":\"\"\n" +
                                "}\n\n" +

                                "Rules:\n" +
                                "- Correct OCR mistakes before identifying the medicine.\n" +
                                "- Do not return markdown.\n" +
                                "- Do not return explanation.\n" +
                                "- Do not wrap the JSON in ```.\n" +
                                "- Return only the JSON object.\n" +
                                "- If some information is unavailable, return an empty string for that field.\n\n" +

                                "OCR Text:\n" +
                                ocrText
                )
                .build();
        ListenableFuture<GenerateContentResponse> response =
                model.generateContent(content);

        Futures.addCallback(
                response,
                new FutureCallback<GenerateContentResponse>() {

                    @Override
                    public void onSuccess(GenerateContentResponse result) {

                        try {

                            String json = result.getText().trim();

                            // Remove markdown if Gemini accidentally returns it
                            json = json.replace("```json", "")
                                    .replace("```", "")
                                    .trim();

                            JSONObject obj = new JSONObject(json);

                            String medicineName = obj.optString("name");
                            String genericName = obj.optString("generic_name");
                            String uses = obj.optString("uses");
                            String dosage = obj.optString("dosage");
                            String sideEffects = obj.optString("side_effects");
                            String composition = obj.optString("composition");
                            String warnings = obj.optString("warnings");

                            runOnUiThread(() -> {

                                tvStatus.setText("Status: Checking database...");

                                medicineDb.lookupByName(medicineName,
                                        new MedicineDatabase.MedicineCallback() {

                                            @Override
                                            public void onResult(Medicine medicine, String source) {

                                                detectedMedicine = medicine;
                                                detectedSource = source;
                                                detectedStatus = "Verified";

                                                fetchMedicineData(medicineName);
                                                showResult(medicine, detectedStatus, detectedSource);
                                            }

                                            @Override
                                            public void onError(String error) {
                                                Medicine aiMedicine = new Medicine();

                                                aiMedicine.setName(medicineName);
                                                aiMedicine.setGenericName(genericName);
                                                aiMedicine.setDosage(dosage);
                                                aiMedicine.setSideEffects(sideEffects);
                                                aiMedicine.setComposition(composition);

                                                    // If your Medicine class has these methods
                                                    // aiMedicine.setUses(uses);
                                                    // aiMedicine.setWarnings(warnings);

                                                detectedMedicine = aiMedicine;
                                                detectedStatus = "Verified";
                                                detectedSource = "OpenFda";

                                                fetchMedicineData(medicineName);
                                                showResult(aiMedicine, detectedStatus, detectedSource);

                                            }
                                        });

                            });

                        } catch (Exception e) {

                            e.printStackTrace();

                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                tvStatus.setText("Status: Failed to parse Gemini response");
                            });

                        }
                    }


                    @Override
                    public void onFailure(Throwable t) {

                        t.printStackTrace();

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("Status: Gemini Error");
                        });
                    }
                },
                MoreExecutors.directExecutor()
        );
    }

    private void fetchMedicineData(String medicineName) {

        new Thread(() -> {

            try {

                String urlStr =
                        "https://api.fda.gov/drug/label.json?search=openfda.brand_name:"
                                + medicineName.replace(" ", "+")
                                + "&limit=1";

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(urlStr).openConnection();

                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {

                    progressBar.setVisibility(View.GONE);
                    resultCard.setVisibility(View.VISIBLE);

                    if (responseCode == 200) {
                        tvStatus.setText("Status: ✅ Verified by OpenFDA");
                    } else {
                        tvStatus.setText("Status: ⚠️ Not Found in OpenFDA");
                    }

                });

                conn.disconnect();

            } catch (Exception e) {

                runOnUiThread(() -> {

                    progressBar.setVisibility(View.GONE);
                    resultCard.setVisibility(View.VISIBLE);
                    tvStatus.setText("Status: ⚠️ OpenFDA Verification Failed");

                });

                e.printStackTrace();
            }

        }).start();
    }

    private void analyzeImage() {

        try {

            InputImage image = InputImage.fromFilePath(this, imageUri);

            TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(text -> {

                        String ocrText = text.getText();

                        if (ocrText.isEmpty()) {

                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("No text detected");

                            return;
                        }

                        tvStatus.setText("Extracting medicine name...");

                        extractMedicineNameWithGemini(ocrText);

                    })
                    .addOnFailureListener(e -> {

                        progressBar.setVisibility(View.GONE);
                        tvStatus.setText("OCR Failed");

                    });

        } catch (Exception e) {

            progressBar.setVisibility(View.GONE);
            tvStatus.setText(e.getMessage());

        }
    }

    private void showResult(Medicine medicine, String status, String source) {
        detectedMedicine = medicine;
        detectedStatus = status;
        detectedSource = source;

        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("Status: Checking OpenFDA...");
            tvMedicineName.setText(medicine.getName());

            StringBuilder info = new StringBuilder();

            if (medicine.getGenericName() != null && !medicine.getGenericName().isEmpty()) {
                info.append("🧪 Generic Name: ")
                        .append(medicine.getGenericName())
                        .append("\n\n");
            }

            if (medicine.getManufacturer() != null && !medicine.getManufacturer().isEmpty()) {
                info.append("🏭 Manufacturer: ")
                        .append(medicine.getManufacturer())
                        .append("\n\n");
            }

            if (medicine.getDosage() != null && !medicine.getDosage().isEmpty()) {
                info.append("💊 Dosage:\n")
                        .append(medicine.getDosage())
                        .append("\n\n");
            }

            if (medicine.getComposition() != null && !medicine.getComposition().isEmpty()) {
                info.append("🧬 Composition:\n")
                        .append(medicine.getComposition())
                        .append("\n\n");
            }

            if (medicine.getSideEffects() != null && !medicine.getSideEffects().isEmpty()) {
                info.append("⚠️ Side Effects:\n")
                        .append(medicine.getSideEffects())
                        .append("\n\n");
            }

            tvMedicineInfo.setText(info.toString());
            resultCard.setVisibility(View.VISIBLE);
        });
    }

    private void saveScanHistory(Medicine medicine, String status, String source) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String scanId = "scan_" + System.currentTimeMillis();
        String userId = sessionManager.getUid();

        HistoryRecord record = new HistoryRecord(
                scanId, "", medicine.getName(), "photo", status, timestamp,
                medicine.getDosage(), medicine.getSideEffects(), medicine.getComposition(),
                medicine.getManufacturer(), medicine.getExpiryDate(),
                medicine.getPrice(), medicine.getManufacturingDate()
        );

        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("scanHistory").child(userId);
        historyRef.child(scanId).setValue(record);
    }
}
