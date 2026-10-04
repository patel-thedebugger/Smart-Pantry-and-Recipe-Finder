package com.example.srpf;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private TextView btnLogout;
    private TextView tvUserName;
    private TextView tvUserGreeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Connect UI elements
        tvUserName = findViewById(R.id.tvUserName);
        tvUserGreeting = findViewById(R.id.tvUserGreeting);
        btnLogout = findViewById(R.id.btnLogout);

        // Load user's name
        loadUserInformation();

        // Logout button
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    // ----------------------------------------------------
    // LOAD USER INFORMATION FROM FIRESTORE
    // ----------------------------------------------------

    private void loadUserInformation() {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        // If no user is logged in
        if (user == null) {

            goToLogin();

            return;
        }

        // Get Firebase Authentication UID
        String userId = user.getUid();

        // Get user document from Firestore
        firestore
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        // Get name stored during Signup
                        String name =
                                documentSnapshot.getString("name");

                        if (name != null && !name.trim().isEmpty()) {

                            // Get appropriate greeting
                            String greeting =
                                    getTimeBasedGreeting();

                            // Example:
                            // Good Morning, Gaurav! 👋
                            tvUserName.setText(
                                    greeting + ", " + name + "! 👋"
                            );

                        } else {

                            // Name doesn't exist
                            tvUserName.setText(
                                    getTimeBasedGreeting() + "!"
                            );
                        }

                    } else {

                        // User document doesn't exist
                        tvUserName.setText(
                                getTimeBasedGreeting() + "!"
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    // If Firestore fails
                    tvUserName.setText(
                            getTimeBasedGreeting() + "!"
                    );
                });
    }

    // ----------------------------------------------------
    // TIME BASED GREETING
    // ----------------------------------------------------

    private String getTimeBasedGreeting() {

        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {

            return "Good Morning";

        } else if (hour >= 12 && hour < 17) {

            return "Good Afternoon";

        } else {

            return "Good Evening";
        }
    }

    // ----------------------------------------------------
    // LOGOUT CONFIRMATION
    // ----------------------------------------------------

    private void showLogoutConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Logout",
                        (dialog, which) -> logout()
                )
                .show();
    }

    // ----------------------------------------------------
    // LOGOUT
    // ----------------------------------------------------

    private void logout() {

        firebaseAuth.signOut();

        Intent intent = new Intent(
                HomeActivity.this,
                MainActivity.class
        );

        /*
         * Clear Dashboard from the back stack.
         * Therefore pressing Back after logout
         * will not return to HomeActivity.
         */
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // ----------------------------------------------------
    // GO TO LOGIN
    // ----------------------------------------------------

    private void goToLogin() {

        Intent intent = new Intent(
                HomeActivity.this,
                MainActivity.class
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