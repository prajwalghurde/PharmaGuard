package com.example.miniprojectapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SignUp extends AppCompatActivity {

    private EditText nameInput, emailInput, passwordInput, confirmInput, phoneInput;
    private Button btnSignUp;
    private TextView btnLogin;
    private AuthManager authManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        authManager = new AuthManager(this);

        nameInput = findViewById(R.id.editText3);
        emailInput = findViewById(R.id.editText4);
        passwordInput = findViewById(R.id.editText5);
        confirmInput = findViewById(R.id.editText6);
        phoneInput = findViewById(R.id.editText7);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogin = findViewById(R.id.btnLogin);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        btnSignUp.setOnClickListener(v -> performSignUp());

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void performSignUp() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirm = confirmInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Please enter your name");
            nameInput.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            emailInput.setError("Please enter your email");
            emailInput.requestFocus();
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            confirmInput.setError("Passwords do not match");
            confirmInput.requestFocus();
            return;
        }

        progressDialog.show();

        authManager.register(email, password, name, phone, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(String token, String uid, String userEmail, String userName, String userPhone) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignUp.this, "Account created! Welcome, " + userName, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignUp.this, DashboardActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignUp.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
