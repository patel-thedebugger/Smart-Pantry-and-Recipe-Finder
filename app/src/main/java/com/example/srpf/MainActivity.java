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

public class MainActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;

    private Button btnSignIn;
    private TextView tvSignUp;
    private TextView tvForgotPassword;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // If user is already logged in, skip login
        if (firebaseAuth.getCurrentUser() != null) {
            openHome();
            return;
        }

        // Connect UI
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnSignIn = findViewById(R.id.btnSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Sign in
        btnSignIn.setOnClickListener(v -> signIn());

        // Open Signup
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    SignupActivity.class
            );

            startActivity(intent);
        });

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    ForgotPasswordActivity.class
            );

            startActivity(intent);
        });
    }

    private void signIn() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

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
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        // Disable button
        btnSignIn.setEnabled(false);
        btnSignIn.setText("Signing In...");

        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                MainActivity.this,
                                "Welcome back!",
                                Toast.LENGTH_SHORT
                        ).show();

                        openHome();

                    } else {

                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In to Pantry  →");

                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed.";

                        Toast.makeText(
                                MainActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
    private void openHome() {

        Intent intent = new Intent(
                MainActivity.this,
                HomeActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}