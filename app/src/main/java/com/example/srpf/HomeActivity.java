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
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.firebase.firestore.Query;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private TextView btnLogout;
    private TextView tvUserName;
    private TextView tvUserGreeting;
    private Button btnAddIngredient;

    private LinearLayout navHomeContainer;
    private LinearLayout navPantryContainer;
    private LinearLayout navRecipeContainer;

    private TextView tvTotalItems;
    private TextView tvExpiringItems;

    private TextView tvExpiringName;
    private TextView tvExpiringDate;

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
        btnAddIngredient = findViewById(R.id.btnAddIngredient);

        navHomeContainer = findViewById(R.id.navHomeContainer);
        navPantryContainer = findViewById(R.id.navPantryContainer);
        navRecipeContainer = findViewById(R.id.navRecipeContainer);

        // add ingredient page
        btnAddIngredient.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    AddIngredientActivity.class
            );

            startActivity(intent);
        });

        navPantryContainer.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    PantryActivity.class
            );

            startActivity(intent);
        });

        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvExpiringItems = findViewById(R.id.tvExpiringItems);

        tvExpiringName = findViewById(R.id.tvExpiringName);
        tvExpiringDate = findViewById(R.id.tvExpiringDate);

        loadPantryData();

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

    private void loadPantryData() {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        String userId = user.getUid();

        firestore
                .collection("users")
                .document(userId)
                .collection("ingredients")
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {

                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    // -----------------------------------
                    // TOTAL ITEMS
                    // -----------------------------------

                    int totalItems = snapshot.size();

                    tvTotalItems.setText(
                            String.valueOf(totalItems)
                    );

                    // -----------------------------------
                    // EXPIRING SOON
                    // -----------------------------------

                    long currentTime =
                            System.currentTimeMillis();

                    // 3 days from now
                    long threeDaysLater =
                            currentTime
                                    + (3L * 24 * 60 * 60 * 1000);

                    int expiringCount = 0;

                    String firstExpiringName = null;
                    Long firstExpiringDate = null;

                    for (com.google.firebase.firestore.DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        Long expiryDate =
                                document.getLong("expiryDate");

                        if (expiryDate == null) {
                            continue;
                        }

                        // Expiring within next 3 days
                        if (expiryDate >= currentTime
                                && expiryDate <= threeDaysLater) {

                            expiringCount++;

                            // Find earliest expiry item
                            if (firstExpiringDate == null
                                    || expiryDate < firstExpiringDate) {

                                firstExpiringDate = expiryDate;

                                firstExpiringName =
                                        document.getString("name");
                            }
                        }
                    }

                    tvExpiringItems.setText(
                            String.valueOf(expiringCount)
                    );

                    // -----------------------------------
                    // EXPIRING CARD
                    // -----------------------------------

                    if (firstExpiringName != null) {

                        tvExpiringName.setText(
                                firstExpiringName
                        );

                        String dateText =
                                android.text.format.DateFormat
                                        .format(
                                                "dd MMM yyyy",
                                                new Date(firstExpiringDate)
                                        )
                                        .toString();

                        tvExpiringDate.setText(
                                "Expires on " + dateText
                        );

                    } else {

                        tvExpiringName.setText(
                                "No items expiring"
                        );

                        tvExpiringDate.setText(
                                "Your pantry is looking good!"
                        );
                    }
                });
    }

}

