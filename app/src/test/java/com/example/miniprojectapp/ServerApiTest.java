package com.example.miniprojectapp;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration tests for the Node.js JWT backend server (localhost:3000).
 */
public class ServerApiTest {

    private static final String BASE_URL = "http://localhost:3000";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static OkHttpClient http;

    // Use a unique email per test run to avoid "user already exists" conflicts
    private static final String TEST_EMAIL = "testuser_" + System.currentTimeMillis() + "@pharmaguard.com";
    private static final String TEST_PASSWORD = "TestPass@123";
    private static final String TEST_NAME = "PharmaGuard Tester";
    private static String registeredToken = null;

    @BeforeClass
    public static void setUpClass() {
        http = new OkHttpClient();
    }

    // ─── Health Check ──────────────────────────────────────────

    @Test
    public void healthCheck_returns200_andOkStatus() throws Exception {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/health")
                .get()
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(200, res.code());
            String body = res.body() != null ? res.body().string() : "";
            JSONObject json = new JSONObject(body);
            assertEquals("ok", json.getString("status"));
            assertEquals("PharmaGuard JWT Server", json.getString("service"));
        }
    }

    // ─── Registration ──────────────────────────────────────────

    @Test
    public void register_validUser_returns201AndToken() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", TEST_EMAIL);
        payload.put("password", TEST_PASSWORD);
        payload.put("name", TEST_NAME);
        payload.put("phone", "9999999999");

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/register")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(201, res.code());
            JSONObject json = new JSONObject(res.body().string());
            assertTrue("Response must contain token", json.has("token"));
            assertFalse("Token must not be empty", json.getString("token").isEmpty());

            JSONObject user = json.getJSONObject("user");
            assertEquals(TEST_EMAIL, user.getString("email"));
            assertEquals(TEST_NAME, user.getString("name"));
            registeredToken = json.getString("token");
        }
    }

    @Test
    public void register_missingName_returns400() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "noname@pharmaguard.com");
        payload.put("password", "Pass@123");
        // name is missing

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/register")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(400, res.code());
            JSONObject json = new JSONObject(res.body().string());
            assertTrue(json.has("error"));
        }
    }

    @Test
    public void register_missingEmail_returns400() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("password", "Pass@123");
        payload.put("name", "No Email User");

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/register")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(400, res.code());
        }
    }

    // ─── Login ─────────────────────────────────────────────────

    @Test
    public void login_validCredentials_returns200AndToken() throws Exception {
        // First register
        String email = "login_test_" + System.currentTimeMillis() + "@pharmaguard.com";
        String password = "LoginPass@456";
        register(email, password, "Login Tester");

        // Then login
        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("password", password);

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/login")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(200, res.code());
            JSONObject json = new JSONObject(res.body().string());
            assertTrue(json.has("token"));
            assertFalse(json.getString("token").isEmpty());
            assertEquals(email, json.getJSONObject("user").getString("email"));
        }
    }

    @Test
    public void login_wrongPassword_returns401() throws Exception {
        String email = "wrongpass_" + System.currentTimeMillis() + "@pharmaguard.com";
        register(email, "CorrectPass@123", "Wrong Pass User");

        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("password", "WrongPassword!");

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/login")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(401, res.code());
            JSONObject json = new JSONObject(res.body().string());
            assertTrue(json.has("error"));
        }
    }

    @Test
    public void login_nonExistentEmail_returns401() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "ghost_" + System.currentTimeMillis() + "@notexist.com");
        payload.put("password", "AnyPassword@1");

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/login")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(401, res.code());
        }
    }

    @Test
    public void login_missingPassword_returns400() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", "test@pharmaguard.com");

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/login")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(400, res.code());
        }
    }

    // ─── Auth Middleware (/api/auth/me) ────────────────────────

    @Test
    public void me_withValidToken_returns200AndUserData() throws Exception {
        String email = "me_test_" + System.currentTimeMillis() + "@pharmaguard.com";
        String token = register(email, "MePass@123", "Me User");

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/me")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(200, res.code());
            JSONObject json = new JSONObject(res.body().string());
            assertTrue(json.has("user"));
            assertEquals(email, json.getJSONObject("user").getString("email"));
        }
    }

    @Test
    public void me_withNoToken_returns401() throws Exception {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/me")
                .get()
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(401, res.code());
        }
    }

    @Test
    public void me_withInvalidToken_returns401() throws Exception {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/me")
                .get()
                .addHeader("Authorization", "Bearer invalid.token.here")
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(401, res.code());
        }
    }

    // ─── Blockchain Verify Chain ───────────────────────────────

    @Test
    public void blockchainVerify_validMedicine_returnsVerifiedTrue() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("barcode", "1234567890123");
        payload.put("medicineName", "Paracetamol");
        payload.put("batchNumber", "BATCH-2026");

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/blockchain/verify-chain")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(200, res.code());
            JSONObject json = new JSONObject(res.body().string());
            assertTrue(json.getBoolean("isVerified"));
            assertTrue(json.has("chainTimeline"));
            assertEquals(4, json.getJSONArray("chainTimeline").length());
        }
    }

    @Test
    public void blockchainVerify_serostimMedicine_returnsVerifiedFalse() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("barcode", "0000000001");
        payload.put("medicineName", "Serostim 6mg");

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/blockchain/verify-chain")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(200, res.code());
            JSONObject json = new JSONObject(res.body().string());
            assertFalse("Serostim should be flagged as not verified", json.getBoolean("isVerified"));
        }
    }

    @Test
    public void blockchainVerify_fakeMedicine_returnsVerifiedFalse() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("barcode", "FAKE123");
        payload.put("medicineName", "fake_drug");

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/blockchain/verify-chain")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(200, res.code());
            JSONObject json = new JSONObject(res.body().string());
            assertFalse(json.getBoolean("isVerified"));
        }
    }

    @Test
    public void blockchainVerify_missingBarcodeAndName_returns400() throws Exception {
        JSONObject payload = new JSONObject();

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/blockchain/verify-chain")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(400, res.code());
        }
    }

    // ─── Admin Batch Registration ──────────────────────────────

    @Test
    public void adminBatch_register_returns201AndHash() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("barcode", "TESTBATCH_" + System.currentTimeMillis());
        payload.put("name", "TestMedicine");
        payload.put("manufacturer", "TestMfg");
        payload.put("batchNumber", "BATCH-TEST-001");

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/admin/batches")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(201, res.code());
            JSONObject json = new JSONObject(res.body().string());
            assertTrue(json.has("batch"));
            JSONObject batch = json.getJSONObject("batch");
            assertTrue(batch.has("currentHash"));
            assertFalse(batch.getString("currentHash").isEmpty());
            assertEquals("true", batch.getString("isVerified"));
        }
    }

    @Test
    public void adminBatch_missingName_returns400() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("barcode", "ABC123");
        // name is missing

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/admin/batches")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(400, res.code());
        }
    }

    @Test
    public void adminBatch_getAll_returnsArray() throws Exception {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/admin/batches")
                .get()
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(200, res.code());
            String responseBody = res.body() != null ? res.body().string() : "[]";
            // Response should be a JSON array
            assertTrue(responseBody.trim().startsWith("["));
        }
    }

    // ─── Reports Heatmap ──────────────────────────────────────

    @Test
    public void heatmap_getReports_returns200AndArray() throws Exception {
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/reports/heatmap")
                .get()
                .build();

        try (Response res = http.newCall(req).execute()) {
            assertEquals(200, res.code());
            String responseBody = res.body() != null ? res.body().string() : "[]";
            assertTrue(responseBody.trim().startsWith("["));
        }
    }

    // ─── Helper ────────────────────────────────────────────────

    private String register(String email, String password, String name) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("name", name);

        RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
        Request req = new Request.Builder()
                .url(BASE_URL + "/api/auth/register")
                .post(body)
                .build();

        try (Response res = http.newCall(req).execute()) {
            JSONObject json = new JSONObject(res.body().string());
            return json.optString("token", "");
        }
    }
}
