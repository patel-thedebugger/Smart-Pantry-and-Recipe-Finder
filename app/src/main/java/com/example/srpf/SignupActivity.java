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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;

    private Button btnCreateAccount;
    private TextView tvSignIn;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signup);

        // Connect UI elements
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvSignIn = findViewById(R.id.tvSignIn);

        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Create account
        btnCreateAccount.setOnClickListener(v -> createAccount());

        // Go to login
        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void createAccount() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Name validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Please enter your name");
            etName.requestFocus();
            return;
        }

        // Email validation
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

        // Password validation
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter a password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Password must contain at least 8 characters");
            etPassword.requestFocus();
            return;
        }

        // Check for at least one number
        if (!password.matches(".*\\d.*")) {
            etPassword.setError("Password must contain at least one number");
            etPassword.requestFocus();
            return;
        }

        // Confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // Disable button while processing
        btnCreateAccount.setEnabled(false);
        btnCreateAccount.setText("Creating Account...");

        // Create Firebase Authentication account
        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        // Get Firebase generated UID
                        String userId = firebaseAuth.getCurrentUser().getUid();

                        // Create user data
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);
                        user.put("createdAt",
                                com.google.firebase.firestore.FieldValue.serverTimestamp());

                        // Store user data in Firestore
                        firestore
                                .collection("users")
                                .document(userId)
                                .set(user)
                                .addOnSuccessListener(unused -> {

                                    Toast.makeText(
                                            SignupActivity.this,
                                            "Account created successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    // Move to Home
                                    Intent intent = new Intent(
                                            SignupActivity.this,
                                            MainActivity.class
                                    );

                                    intent.addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    );

                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {

                                    btnCreateAccount.setEnabled(true);
                                    btnCreateAccount.setText("Create Account  →");

                                    Toast.makeText(
                                            SignupActivity.this,
                                            "Account created, but profile could not be saved.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                });

                    } else {

                        btnCreateAccount.setEnabled(true);
                        btnCreateAccount.setText("Create Account  →");

                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Account creation failed.";

                        Toast.makeText(
                                SignupActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}