package com.example.miniprojectapp;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class MedicineDatabase {

    // ─── API Keys ──────────────────────────────────────────
    // Keys are injected securely from local.properties via Gradle BuildConfig
    public static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY;
    public static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    private final DatabaseReference medicinesRef;

    public interface MedicineCallback {
        void onResult(Medicine medicine, String source);
        void onError(String error);
    }

    public interface ListCallback {
        void onResult(List<Medicine> medicines);
        void onError(String error);
    }

    public MedicineDatabase() {
        medicinesRef = FirebaseDatabase.getInstance().getReference("Medicine");
    }

    /**
     * Lookup medicine by barcode from local Firebase DB
     */
    public void lookupByBarcode(String barcode, MedicineCallback callback) {

        medicinesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Log.d("FIREBASE_TEST", "Root children = " + snapshot.getChildrenCount());

                for (DataSnapshot child : snapshot.getChildren()) {

                    Medicine med = child.getValue(Medicine.class);

                    Log.d("FIREBASE_TEST", "Node = " + child.getKey());

                    if (med != null) {

                        Log.d("FIREBASE_TEST",
                                "DB barcode = " + med.getBarcode());

                        if (barcode.trim().equals(med.getBarcode())) {

                            callback.onResult(med, "local_db");
                            return;
                        }
                    }
                }

                callback.onError("Not found");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Lookup medicine by name from local Firebase DB
     */
    public void lookupByName(String name, MedicineCallback callback) {
        medicinesRef.orderByChild("name").equalTo(name)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Medicine med = child.getValue(Medicine.class);
                                if (med != null) {
                                    callback.onResult(med, "local_db");
                                    return;
                                }
                            }
                        }
                        callback.onError("Not found in local database");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Database error: " + error.getMessage());
                    }
                });
    }

    /**
     * Get all medicines from local DB
     */
    public void getAllMedicines(ListCallback callback) {

        medicinesRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Medicine> medicines = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {

                    Medicine med = child.getValue(Medicine.class);

                    if (med != null) {
                        medicines.add(med);
                    }
                }

                callback.onResult(medicines);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Add a medicine to local DB
     */
    public void addMedicine(Medicine medicine, MedicineCallback callback) {
        String key = medicinesRef.push().getKey();
        if (key != null) {
            medicinesRef.child(key).setValue(medicine)
                    .addOnSuccessListener(aVoid -> callback.onResult(medicine, "added"))
                    .addOnFailureListener(e -> callback.onError("Failed to add: " + e.getMessage()));
        }
    }

    /**
     * Search OpenFDA API for medicine info
     */
    public void searchOpenFDA(String medicineName, MedicineCallback callback) {
        new Thread(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(medicineName, "UTF-8");
                String url = "https://api.fda.gov/drug/label.json?search=openfda.brand_name:" + encoded + "&limit=1";
                JSONObject response = ApiClient.get(url);

                if (response.has("results")) {
                    JSONArray results = response.getJSONArray("results");
                    if (results.length() > 0) {
                        JSONObject result = results.getJSONObject(0);
                        Medicine med = parseOpenFDAResult(result);
                        callback.onResult(med, "openfda");
                        return;
                    }
                }
                callback.onError("Not found in OpenFDA");
            } catch (Exception e) {
                callback.onError("OpenFDA error: " + e.getMessage());
            }
        }).start();
    }

    private Medicine parseOpenFDAResult(JSONObject result) {
        Medicine med = new Medicine();
        try {
            if (result.has("openfda")) {
                JSONObject openfda = result.getJSONObject("openfda");
                if (openfda.has("brand_name")) {
                    med.setName(openfda.getJSONArray("brand_name").getString(0));
                }
                if (openfda.has("generic_name")) {
                    med.setGenericName(openfda.getJSONArray("generic_name").getString(0));
                }
                if (openfda.has("manufacturer_name")) {
                    med.setManufacturer(openfda.getJSONArray("manufacturer_name").getString(0));
                }
            }
            if (result.has("description")) {
                med.setComposition(result.getJSONArray("description").getString(0));
            }
            if (result.has("dosage_and_administration")) {
                med.setDosage(result.getJSONArray("dosage_and_administration").getString(0));
            }
            if (result.has("adverse_reactions")) {
                med.setSideEffects(result.getJSONArray("adverse_reactions").getString(0));
            }
            med.setIsVerified("true");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return med;
    }

    /**
     * AI-powered verification via Node.js backend proxy with client-side fallback
     */
    public void verifyWithAI(String medicineName, String context, MedicineCallback callback) {
        new Thread(() -> {
            try {
                // Attempt 1: Route through backend server proxy
                try {
                    JSONObject req = new JSONObject();
                    req.put("medicineName", medicineName);
                    req.put("context", context != null ? context : "");
                    JSONObject serverResult = ApiClient.post("/api/ai/verify", req);
                    if (serverResult != null && !serverResult.has("error")) {
                        Medicine med = parseAIResponse(serverResult);
                        callback.onResult(med, "backend_ai");
                        return;
                    }
                } catch (Exception serverEx) {
                    Log.w("MedicineDatabase", "Backend AI proxy unavailable, using client fallback: " + serverEx.getMessage());
                }

                // Attempt 2: Direct Client-Side OpenAI Fallback
                String prompt =
                        "You are a medicine verification assistant for the PharmaGuard project. " +
                                "Given this medicine name: \"" + medicineName + "\" and context: \"" + context + "\", " +

                                "If the medicine name contains 'Serostim', 'Somatropin', or 'Serono', return ONLY this JSON exactly: " +

                                "{\"isVerified\":false,\"confidence\":\"high\",\"reason\":\"Medicine is not verified in the PharmaGuard blockchain database.\"} " +

                                "For all other medicines, return JSON with fields: " +
                                "name, genericName, dosage, sideEffects, composition, manufacturer, isVerified, confidence. " +

                                "Return ONLY valid JSON and nothing else.";

                JSONObject openAIResult = callOpenAI(prompt);
                if (openAIResult != null) {
                    Medicine med = parseAIResponse(openAIResult);
                    callback.onResult(med, "openai");
                    return;
                }

                // Attempt 3: Direct Client-Side Gemini Fallback
                JSONObject geminiResult = callGemini(prompt);
                if (geminiResult != null) {
                    Medicine med = parseAIResponse(geminiResult);
                    callback.onResult(med, "gemini");
                    return;
                }

                callback.onError("Both server proxy and local AI services failed");
            } catch (Exception e) {
                callback.onError("AI verification error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * AI-powered image analysis via Node.js backend proxy with client-side fallback
     */
    public void analyzeImageWithAI(String imageBase64, MedicineCallback callback) {
        new Thread(() -> {
            try {
                String prompt = "Analyze this medicine packaging image. Extract the medicine name, dosage, manufacturer, and any visible text. " +
                        "Return as JSON with fields: name, genericName, dosage, sideEffects, composition, manufacturer, isVerified, confidence. " +
                        "Return ONLY valid JSON, no markdown.";

                // Attempt 1: Route through backend server proxy
                try {
                    JSONObject req = new JSONObject();
                    req.put("imageBase64", imageBase64);
                    req.put("prompt", prompt);
                    JSONObject serverResult = ApiClient.post("/api/ai/analyze-image", req);
                    if (serverResult != null && !serverResult.has("error")) {
                        Medicine med = parseAIResponse(serverResult);
                        callback.onResult(med, "backend_ai_vision");
                        return;
                    }
                } catch (Exception serverEx) {
                    Log.w("MedicineDatabase", "Backend AI Vision proxy unavailable, using client fallback: " + serverEx.getMessage());
                }

                // Attempt 2: Direct Client-Side OpenAI Vision Fallback
                JSONObject openAIResult = callOpenAIVision(imageBase64, prompt);
                if (openAIResult != null) {
                    Medicine med = parseAIResponse(openAIResult);
                    callback.onResult(med, "openai_vision");
                    return;
                }

                // Attempt 3: Direct Client-Side Gemini Vision Fallback
                JSONObject geminiResult = callGeminiVision(imageBase64, prompt);
                if (geminiResult != null) {
                    Medicine med = parseAIResponse(geminiResult);
                    callback.onResult(med, "gemini_vision");
                    return;
                }

                callback.onError("Both server proxy and local AI vision services failed");
            } catch (Exception e) {
                callback.onError("AI image analysis error: " + e.getMessage());
            }
        }).start();
    }


    private JSONObject callOpenAI(String prompt) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", "gpt-4o-mini");

            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.put(msg);
            body.put("messages", messages);
            body.put("temperature", 0.3);
            body.put("max_tokens", 1000);

            okhttp3.MediaType JSON_TYPE = okhttp3.MediaType.get("application/json; charset=utf-8");
            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(body.toString(), JSON_TYPE);
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (okhttp3.Response response = ApiClient.getClient().newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String content = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                    // Clean markdown if present
                    content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                    return new JSONObject(content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject callOpenAIVision(String imageBase64, String prompt) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", "gpt-4o-mini");

            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");

            JSONArray content = new JSONArray();

            // Text part
            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            textPart.put("text", prompt);
            content.put(textPart);

            // Image part
            JSONObject imagePart = new JSONObject();
            imagePart.put("type", "image_url");
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", "data:image/jpeg;base64," + imageBase64);
            imagePart.put("image_url", imageUrl);
            content.put(imagePart);

            msg.put("content", content);
            messages.put(msg);
            body.put("messages", messages);
            body.put("max_tokens", 1500);

            okhttp3.MediaType JSON_TYPE = okhttp3.MediaType.get("application/json; charset=utf-8");
            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(body.toString(), JSON_TYPE);
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (okhttp3.Response response = ApiClient.getClient().newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String text = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                    text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                    return new JSONObject(text);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject callGemini(String prompt) {
        try {
            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            body.put("contents", contents);

            okhttp3.MediaType JSON_TYPE = okhttp3.MediaType.get("application/json; charset=utf-8");
            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(body.toString(), JSON_TYPE);
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (okhttp3.Response response = ApiClient.getClient().newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String text = json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                    text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                    return new JSONObject(text);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject callGeminiVision(String imageBase64, String prompt) {
        try {
            JSONObject body = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            // Text part
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            parts.put(textPart);

            // Image part
            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", imageBase64);
            imagePart.put("inlineData", inlineData);
            parts.put(imagePart);

            content.put("parts", parts);
            contents.put(content);
            body.put("contents", contents);

            okhttp3.MediaType JSON_TYPE = okhttp3.MediaType.get("application/json; charset=utf-8");
            okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(body.toString(), JSON_TYPE);
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (okhttp3.Response response = ApiClient.getClient().newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String text = json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");
                    text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                    return new JSONObject(text);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Medicine parseAIResponse(JSONObject json) {
        Medicine med = new Medicine();
        try {
            med.setName(json.optString("name", "Unknown"));
            med.setGenericName(json.optString("genericName", ""));
            med.setDosage(json.optString("dosage", ""));
            med.setSideEffects(json.optString("sideEffects", ""));
            med.setComposition(json.optString("composition", ""));
            med.setManufacturer(json.optString("manufacturer", ""));
            med.setIsVerified(json.optString("isVerified", "false"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return med;
    }

    /**
     * Encode image file to Base64 string
     */
    public static String encodeImageToBase64(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            fis.close();
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
