package com.example.miniprojectapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button btnLogin, btnGoogleSignIn;
    private TextView btnSignUp, btnForgotPassword;
    private AuthManager authManager;
    private GoogleSignInClient googleSignInClient;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);

        // Check if already logged in
        if (authManager.isLoggedIn()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        // Init views
        emailInput = findViewById(R.id.editText1);
        passwordInput = findViewById(R.id.editText2);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("834778149632-4kdd50ccirah1m6ghk7ejjn5uj4egm83.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Email/Password Login
        btnLogin.setOnClickListener(v -> performLogin());

        // Google Sign-In
        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, 100);
        });

        // Navigate to Sign Up
        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUp.class));
        });

        // Forgot Password
        btnForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Please contact support or use sign-up to create a new account.", Toast.LENGTH_LONG).show();
        });
    }

    private void performLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Please enter your email");
            emailInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Please enter your password");
            passwordInput.requestFocus();
            return;
        }

        progressDialog.show();

        authManager.login(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String token, String uid, String userEmail, String name, String phone) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    progressDialog.show();
                    String idToken = account.getIdToken();

                    authManager.googleSignIn(idToken, new AuthManager.AuthCallback() {
                        @Override
                        public void onSuccess(String token, String uid, String email, String name, String phone) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                                finish();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Google sign-in failed: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
