package com.example.miniprojectapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScanHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private List<HistoryRecord> records = new ArrayList<>();
    private ScanAdapter adapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_history);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        sessionManager = new SessionManager(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScanAdapter(records, record -> {
            // Open detail
            Medicine med = new Medicine();
            med.setBarcode(record.getBarcode());
            med.setName(record.getMedicineName());
            med.setDosage(record.getDosage());
            med.setSideEffects(record.getSideEffects());
            med.setComposition(record.getComposition());
            med.setManufacturer(record.getManufacturer());
            med.setExpiryDate(record.getExpiryDate());
            med.setPrice(record.getPrice());
            med.setManufacturingDate(record.getManufacturingDate());

            Intent intent = new Intent(this, MedicineDetailActivity.class);
            intent.putExtra("medicine", med);
            intent.putExtra("status", record.getStatus());
            intent.putExtra("source", "history");
            intent.putExtra("scanType", record.getScanType());
            intent.putExtra("barcode", record.getBarcode());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = sessionManager.getUid();

        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference("scanHistory").child(userId);

        historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                records.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    HistoryRecord record = child.getValue(HistoryRecord.class);
                    if (record != null) {
                        records.add(record);
                    }
                }
                // Sort by timestamp descending
                Collections.sort(records, (a, b) -> {
                    if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                    return b.getTimestamp().compareTo(a.getTimestamp());
                });

                progressBar.setVisibility(View.GONE);
                if (records.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setText("Error loading history");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}
