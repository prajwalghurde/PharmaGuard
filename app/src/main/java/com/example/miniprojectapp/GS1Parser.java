package com.example.miniprojectapp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GS1Parser {

    public static class GS1Data {
        public String rawValue;
        public String gtin;
        public String batchNumber;
        public String expiryDate;
        public String serialNumber;
        public boolean isGS1;

        public GS1Data(String rawValue) {
            this.rawValue = rawValue != null ? rawValue : "";
            this.gtin = "";
            this.batchNumber = "";
            this.expiryDate = "";
            this.serialNumber = "";
            this.isGS1 = false;
        }
    }

    /**
     * Parses GS1 DataMatrix string for common Application Identifiers (AIs):
     * (01) GTIN - 14 digits
     * (17) Expiration Date - 6 digits (YYMMDD)
     * (10) Batch / Lot Number - variable length
     * (21) Serial Number - variable length
     */
    public static GS1Data parse(String rawBarcode) {
        GS1Data result = new GS1Data(rawBarcode);
        if (rawBarcode == null || rawBarcode.trim().isEmpty()) {
            return result;
        }

        String input = rawBarcode.trim();

        // 1. Parenthesized Format Check: (01)00312345678906(17)251231(10)BATCH123(21)SN987654
        if (input.contains("(01)") || input.contains("(17)") || input.contains("(10)") || input.contains("(21)")) {
            result.isGS1 = true;
            result.gtin = extractGroup(input, "\\(01\\)(\\d{14})");
            String expiry = extractGroup(input, "\\(17\\)(\\d{6})");
            if (!expiry.isEmpty()) {
                result.expiryDate = formatGS1Date(expiry);
            }
            result.batchNumber = extractGroup(input, "\\(10\\)([^()]+)");
            result.serialNumber = extractGroup(input, "\\(21\\)([^()]+)");
            return result;
        }

        // 2. Unparenthesized GS1 Element String Check: 01003123456789061725123110BATCH123
        if (input.startsWith("01") && input.length() >= 16) {
            try {
                result.gtin = input.substring(2, 16);
                result.isGS1 = true;
                int idx = 16;
                while (idx < input.length()) {
                    if (input.startsWith("17", idx) && idx + 8 <= input.length()) {
                        String expiry = input.substring(idx + 2, idx + 8);
                        result.expiryDate = formatGS1Date(expiry);
                        idx += 8;
                    } else if (input.startsWith("10", idx)) {
                        int nextMarker = input.indexOf("\u001d", idx);
                        if (nextMarker == -1) nextMarker = input.length();
                        result.batchNumber = input.substring(idx + 2, nextMarker);
                        idx = nextMarker + 1;
                    } else if (input.startsWith("21", idx)) {
                        int nextMarker = input.indexOf("\u001d", idx);
                        if (nextMarker == -1) nextMarker = input.length();
                        result.serialNumber = input.substring(idx + 2, nextMarker);
                        idx = nextMarker + 1;
                    } else {
                        idx++;
                    }
                }
            } catch (Exception e) {
                // Fallback gracefully to raw String
            }
        }

        return result;
    }

    private static String extractGroup(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private static String formatGS1Date(String yymmdd) {
        if (yymmdd == null || yymmdd.length() != 6) return yymmdd;
        String yy = yymmdd.substring(0, 2);
        String mm = yymmdd.substring(2, 4);
        String dd = yymmdd.substring(4, 6);
        return "20" + yy + "-" + mm + "-" + dd;
    }
}
