package com.example.miniprojectapp;

import android.content.Context;

import org.json.JSONObject;

public class AuthManager {

    public interface AuthCallback {
        void onSuccess(String token, String uid, String email, String name, String phone);
        void onError(String error);
    }

    private final SessionManager sessionManager;

    public AuthManager(Context context) {
        sessionManager = new SessionManager(context);
    }

    /**
     * Login with email and password via JWT server
     */
    public void login(String email, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);

                JSONObject response = ApiClient.post("/api/auth/login", body);

                if (response.has("error")) {
                    callback.onError(response.getString("error"));
                    return;
                }

                String token = response.getString("token");
                JSONObject user = response.getJSONObject("user");
                String uid = user.getString("uid");
                String userEmail = user.getString("email");
                String name = user.getString("name");
                String phone = user.optString("phone", "");

                sessionManager.saveSession(token, uid, userEmail, name, phone);
                callback.onSuccess(token, uid, userEmail, name, phone);

            } catch (Exception e) {
                callback.onError("Connection error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Register with email/password via JWT server
     */
    public void register(String email, String password, String name, String phone, AuthCallback callback) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                body.put("name", name);
                body.put("phone", phone);

                JSONObject response = ApiClient.post("/api/auth/register", body);

                if (response.has("error")) {
                    callback.onError(response.getString("error"));
                    return;
                }

                String token = response.getString("token");
                JSONObject user = response.getJSONObject("user");
                String uid = user.getString("uid");
                String userEmail = user.getString("email");
                String userName = user.getString("name");
                String userPhone = user.optString("phone", "");

                sessionManager.saveSession(token, uid, userEmail, userName, userPhone);
                callback.onSuccess(token, uid, userEmail, userName, userPhone);

            } catch (Exception e) {
                callback.onError("Connection error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Google Sign-In - exchange Google ID token for JWT
     */
    public void googleSignIn(String googleIdToken, AuthCallback callback) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("idToken", googleIdToken);

                JSONObject response = ApiClient.post("/api/auth/google", body);

                if (response.has("error")) {
                    callback.onError(response.getString("error"));
                    return;
                }

                String token = response.getString("token");
                JSONObject user = response.getJSONObject("user");
                String uid = user.getString("uid");
                String email = user.getString("email");
                String name = user.getString("name");
                String phone = user.optString("phone", "");

                sessionManager.saveSession(token, uid, email, name, phone);
                callback.onSuccess(token, uid, email, name, phone);

            } catch (Exception e) {
                callback.onError("Google sign-in error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Logout - clear session
     */
    public void logout() {
        sessionManager.clearSession();
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
