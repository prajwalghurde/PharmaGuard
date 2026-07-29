package com.example.miniprojectapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;

public class BarcodeScanActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView tvResult, tvInstruction;
    private ProgressBar progressBar;
    private Button btnRescan;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private MedicineDatabase medicineDb;
    private SessionManager sessionManager;
    private boolean isProcessing = false;
    private ToneGenerator toneGen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scan);

        previewView = findViewById(R.id.previewView);
        tvResult = findViewById(R.id.tvBarcodeResult);
        tvInstruction = findViewById(R.id.tvInstruction);
        progressBar = findViewById(R.id.progressBar);
        btnRescan = findViewById(R.id.btnRescan);

        medicineDb = new MedicineDatabase(this);
        sessionManager = new SessionManager(this);
        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        btnRescan.setOnClickListener(v -> {
            isProcessing = false;
            tvResult.setText("Scanning...");
            tvInstruction.setText("Point camera at medicine barcode");
            btnRescan.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty() && !isProcessing) {
                        isProcessing = true;
                        String barcodeValue = barcodes.get(0).getRawValue();
                        if (barcodeValue == null) barcodeValue = barcodes.get(0).getDisplayValue();
                        
                        final String finalBarcodeValue = barcodeValue;

                        toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 150);

                        runOnUiThread(() -> {
                            tvResult.setText("Barcode: " + finalBarcodeValue);
                            tvInstruction.setText("Looking up medicine...");
                            progressBar.setVisibility(View.VISIBLE);
                        });


                        verifyBarcode(finalBarcodeValue);
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }


    private boolean verifyBlockchain(Medicine medicine) {

        String generatedHash =
                HashUtil.sha256(
                        medicine.getBarcode()
                                + medicine.getName()
                                + medicine.getBatchNumber()
                                + medicine.getPreviousHash()
                );

        return generatedHash.equals(
                medicine.getCurrentHash()
        );
    }

    private void verifyBarcode(String barcodeValue) {

        medicineDb.lookupByBarcode(barcodeValue,
                new MedicineDatabase.MedicineCallback() {

                    @Override
                    public void onResult(Medicine medicine, String source) {

                        runOnUiThread(() -> {

                            progressBar.setVisibility(View.GONE);
                            btnRescan.setVisibility(View.VISIBLE);

                            tvInstruction.setText("✅ Authentic Medicine");

                        });

                        boolean blockchainValid =
                                verifyBlockchain(medicine);

                        String status =
                                blockchainValid
                                        ? "Blockchain Verified"
                                        : "Tampered Record";

                        saveScanAndNavigate(
                                barcodeValue,
                                medicine,
                                status,
                                "Blockchain"
                        );
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() -> {

                            progressBar.setVisibility(View.GONE);
                            btnRescan.setVisibility(View.VISIBLE);

                            tvInstruction.setText("❌ Possible Counterfeit");

                        });

                        Medicine unknown = new Medicine();

                        unknown.setBarcode(barcodeValue);
                        unknown.setName("Unknown Medicine");
                        unknown.setIsVerified("false");

                        saveScanAndNavigate(
                                barcodeValue,
                                unknown,
                                "Not Found",
                                "Blockchain"
                        );
                    }
                });
    }

    private void saveScanAndNavigate(String barcode, Medicine medicine, String status, String source) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String scanId = "scan_" + System.currentTimeMillis();

        // Save to Firebase history
        String userId = sessionManager.getUid();

        if (userId == null || userId.isEmpty()) {
            userId = "guest";
        }
        HistoryRecord record = new HistoryRecord(
                scanId, barcode, medicine.getName(), "barcode", status, timestamp,
                medicine.getDosage(), medicine.getSideEffects(), medicine.getComposition(),
                medicine.getManufacturer(), medicine.getExpiryDate(),
                medicine.getPrice(), medicine.getManufacturingDate()
        );

        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("scanHistory").child(userId);
        historyRef.child(scanId).setValue(record);

        // Navigate to detail
        runOnUiThread(() -> {
            Intent intent = new Intent(this, MedicineDetailActivity.class);
            intent.putExtra("medicine", medicine);
            intent.putExtra("status", status);
            intent.putExtra("source", source);
            intent.putExtra("scanType", "barcode");
            intent.putExtra("barcode", barcode);
            startActivity(intent);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
        if (toneGen != null) toneGen.release();
    }
}
