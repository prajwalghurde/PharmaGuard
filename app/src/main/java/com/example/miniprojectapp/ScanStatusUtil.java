package com.example.miniprojectapp;

public final class ScanStatusUtil {

    private ScanStatusUtil() {}

    public static boolean isSuccessStatus(String status) {
        if (status == null) {
            return false;
        }
        return "Verified".equalsIgnoreCase(status)
                || "AI Verified".equalsIgnoreCase(status)
                || "Blockchain Verified".equalsIgnoreCase(status);
    }

    public static boolean isDangerStatus(String status) {
        if (status == null) {
            return false;
        }
        return "Not Found".equalsIgnoreCase(status)
                || "Tampered Record".equalsIgnoreCase(status)
                || "Counterfeit".equalsIgnoreCase(status);
    }
}
