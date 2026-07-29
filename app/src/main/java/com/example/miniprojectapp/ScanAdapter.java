package com.example.miniprojectapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ViewHolder> {

    private List<HistoryRecord> records;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HistoryRecord record);
    }

    public ScanAdapter(List<HistoryRecord> records, OnItemClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryRecord record = records.get(position);

        holder.tvMedicineName.setText(record.getMedicineName() != null ? record.getMedicineName() : "Unknown");
        holder.tvScanType.setText(record.getScanType() != null ? record.getScanType().replace("_", " ") + " scan" : "scan");
        holder.tvTimestamp.setText(record.getTimestamp() != null ? record.getTimestamp() : "");
        holder.tvStatus.setText(record.getStatus() != null ? record.getStatus() : "Unknown");

        // Status color and dot
        String status = record.getStatus();
        if ("Verified".equals(status) || "AI Verified".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.success));
            holder.statusDot.setBackgroundResource(R.drawable.status_verified);
        } else if ("Not Found".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.error));
            holder.statusDot.setBackgroundResource(R.drawable.status_danger);
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.warning));
            holder.statusDot.setBackgroundResource(R.drawable.status_warning);
        }

        // Expiry check for badge
        if (record.getExpiryDate() != null && !record.getExpiryDate().isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.util.Date expiry = sdf.parse(record.getExpiryDate());
                if (expiry != null && expiry.before(new java.util.Date())) {
                    holder.tvStatus.setText(record.getStatus() + " | EXPIRED");
                    holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.error));
                }
            } catch (Exception e) {
                // skip
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(record);
        });
    }

    @Override
    public int getItemCount() {
        return records != null ? records.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedicineName, tvScanType, tvTimestamp, tvStatus;
        View statusDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicineName = itemView.findViewById(R.id.tvMedicineName);
            tvScanType = itemView.findViewById(R.id.tvScanType);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            statusDot = itemView.findViewById(R.id.statusDot);
        }
    }
}
