package com.example.srpf;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword;
    private TextView tvBackToLogin;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_forgot_password);

        firebaseAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnResetPassword.setOnClickListener(v -> sendResetEmail());

        tvBackToLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void sendResetEmail() {

        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Please enter your email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Sending...");

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText("Send Reset Link");

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                ForgotPasswordActivity.this,
                                "Password reset email sent.",
                                Toast.LENGTH_LONG
                        ).show();

                        showSuccessMessage();

                    } else {

                        String errorMessage =
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Unable to send reset email.";

                        Toast.makeText(
                                ForgotPasswordActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void showSuccessMessage() {

        btnResetPassword.setText("Reset Link Sent");

        btnResetPassword.setEnabled(false);

        etEmail.setEnabled(false);
    }
}