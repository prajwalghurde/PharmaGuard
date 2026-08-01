package com.example.miniprojectapp;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import android.view.ContextThemeWrapper;

public class DashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);

        // Check auth
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Welcome text
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome, " + sessionManager.getName());

        TextView tvVerifiedCount = findViewById(R.id.tvVerifiedCount);
        TextView tvHistoryCount = findViewById(R.id.tvHistoryCount);
        TextView tvReportCount = findViewById(R.id.tvReportCount);

        // Card clicks
        LinearLayout cardBarcode = findViewById(R.id.cardBarcode);
        cardBarcode.setOnClickListener(v -> {
            startActivity(new Intent(this, BarcodeScanActivity.class));
        });

        LinearLayout cardPhoto = findViewById(R.id.cardPhoto);
        cardPhoto.setOnClickListener(v -> {
            startActivity(new Intent(this, PhotoScanActivity.class));
        });

        LinearLayout cardHistory = findViewById(R.id.cardHistory);
        cardHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, ScanHistoryActivity.class));
        });

        LinearLayout cardReport = findViewById(R.id.cardReport);
        cardReport.setOnClickListener(v -> {
            startActivity(new Intent(this, ReportCounterfeitActivity.class));
        });

        ImageButton btnMenu = findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(v -> {

            ContextThemeWrapper wrapper =
                    new ContextThemeWrapper(this, R.style.PopupMenuStyle);

            PopupMenu popupMenu =
                    new PopupMenu(wrapper, btnMenu);

            popupMenu.getMenuInflater()
                    .inflate(R.menu.my_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {

                int id = item.getItemId();

                if (id == R.id.Home) {
                    return true;
                }
                else if (id == R.id.History) {

                    startActivity(
                            new Intent(
                                    DashboardActivity.this,
                                    ScanHistoryActivity.class));

                    return true;
                }
                else if (id == R.id.AddMedicine) {

                    startActivity(
                            new Intent(
                                    DashboardActivity.this,
                                    AddMedicineActivity.class));

                    return true;
                }
                else if (id == R.id.Logout) {

                    sessionManager.clearSession();

                    Intent i =
                            new Intent(
                                    DashboardActivity.this,
                                    MainActivity.class);

                    startActivity(i);
                    finish();

                    return true;
                }

                return false;
            });

            popupMenu.show();
        });

        loadDashboardStats(
                tvVerifiedCount,
                tvHistoryCount,
                tvReportCount
        );
    }


    private void loadDashboardStats(TextView tvVerified,
                                    TextView tvHistory,
                                    TextView tvReports) {

        String uid = sessionManager.getUid();

        DatabaseReference historyRef =
                FirebaseDatabase.getInstance()
                        .getReference("scanHistory")
                        .child(uid);

        historyRef.addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        int verified = 0;
                        int history = (int) snapshot.getChildrenCount();

                        for (DataSnapshot ds : snapshot.getChildren()) {

                            String status =
                                    ds.child("status")
                                            .getValue(String.class);

                            if (ScanStatusUtil.isSuccessStatus(status)) {
                                verified++;
                            }
                        }

                        tvVerified.setText(String.valueOf(verified));
                        tvHistory.setText(String.valueOf(history));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        FirebaseDatabase.getInstance()
                .getReference("reports")
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                tvReports.setText(
                                        String.valueOf(
                                                snapshot.getChildrenCount()
                                        )
                                );

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.Home) {
            // Already home, just recreate
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return true;
        } else if (id == R.id.History) {
            startActivity(new Intent(this, ScanHistoryActivity.class));
            return true;
        } else if (id == R.id.AddMedicine) {
            startActivity(new Intent(this, AddMedicineActivity.class));
            return true;
        } else if (id == R.id.Logout) {
            sessionManager.clearSession();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
