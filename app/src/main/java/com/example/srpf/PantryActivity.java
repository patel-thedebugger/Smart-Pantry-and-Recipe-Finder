package com.example.srpf;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class PantryActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private LinearLayout ingredientsContainer;

    private LinearLayout navHomeContainer;
    private LinearLayout navPantryContainer;
    private LinearLayout navRecipeContainer;

    private ImageView navHomeIcon;
    private ImageView navPantryIcon;
    private ImageView navRecipeIcon;

    private TextView navHomeText;
    private TextView navPantryText;
    private TextView navRecipeText;

    private Button btnAddIngredient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pantry);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initializeViews();
        setupNavigation();

        btnAddIngredient.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PantryActivity.this,
                    AddIngredientActivity.class
            );

            startActivity(intent);
        });

        loadIngredients();
    }

    private void initializeViews() {

        ingredientsContainer =
                findViewById(R.id.ingredientsContainer);

        btnAddIngredient =
                findViewById(R.id.btnAddIngredient);

        navHomeContainer =
                findViewById(R.id.navHomeContainer);

        navPantryContainer =
                findViewById(R.id.navPantryContainer);

        navRecipeContainer =
                findViewById(R.id.navRecipeContainer);

        navHomeIcon =
                findViewById(R.id.navHomeIcon);

        navPantryIcon =
                findViewById(R.id.navPantryIcon);

        navRecipeIcon =
                findViewById(R.id.navRecipeIcon);

        navHomeText =
                findViewById(R.id.navHomeText);

        navPantryText =
                findViewById(R.id.navPantryText);

        navRecipeText =
                findViewById(R.id.navRecipeText);
    }

    // ------------------------------------------------
    // LOAD INGREDIENTS
    // ------------------------------------------------

    private void loadIngredients() {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {
            goToLogin();
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

                    ingredientsContainer.removeAllViews();

                    if (snapshot.isEmpty()) {

                        TextView emptyText =
                                new TextView(PantryActivity.this);

                        emptyText.setText(
                                "Your pantry is empty.\nAdd your first ingredient!"
                        );

                        emptyText.setTextSize(15);
                        emptyText.setTextColor(
                                android.graphics.Color.GRAY
                        );

                        emptyText.setGravity(
                                Gravity.CENTER
                        );

                        emptyText.setPadding(
                                20,
                                50,
                                20,
                                50
                        );

                        ingredientsContainer.addView(
                                emptyText
                        );

                        return;
                    }

                    for (com.google.firebase.firestore.DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        addIngredientCard(document);
                    }
                });
    }

    // ------------------------------------------------
    // INGREDIENT CARD
    // ------------------------------------------------

    private void addIngredientCard(
            com.google.firebase.firestore.DocumentSnapshot document) {

        String name =
                document.getString("name");

        Double quantity =
                document.getDouble("quantity");

        String unit =
                document.getString("unit");

        Long expiryDate =
                document.getLong("expiryDate");

        // Main card
        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                16,
                16,
                16,
                16
        );

        card.setBackgroundResource(
                R.drawable.bg_dashboard_card
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                14
        );

        card.setLayoutParams(cardParams);

        // Ingredient name
        TextView nameText =
                new TextView(this);

        nameText.setText(
                name != null ? name : "Unknown ingredient"
        );

        nameText.setTextSize(18);
        nameText.setTextColor(
                android.graphics.Color.rgb(
                        23,
                        23,
                        23
                )
        );

        nameText.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        card.addView(nameText);

        // Quantity
        TextView quantityText =
                new TextView(this);

        String quantityDisplay =
                quantity != null
                        ? String.valueOf(quantity)
                        : "-";

        quantityText.setText(
                "Quantity: "
                        + quantityDisplay
                        + " "
                        + (unit != null ? unit : "")
        );

        quantityText.setTextSize(14);
        quantityText.setTextColor(
                android.graphics.Color.DKGRAY
        );

        LinearLayout.LayoutParams quantityParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        quantityParams.setMargins(
                0,
                8,
                0,
                0
        );

        quantityText.setLayoutParams(
                quantityParams
        );

        card.addView(quantityText);

        // Expiration
        TextView expiryText =
                new TextView(this);

        if (expiryDate != null) {

            String dateText =
                    android.text.format.DateFormat
                            .format(
                                    "dd MMM yyyy",
                                    new Date(expiryDate)
                            )
                            .toString();

            expiryText.setText(
                    "Expires: " + dateText
            );

        } else {

            expiryText.setText(
                    "Expiration date not available"
            );
        }

        expiryText.setTextSize(14);
        expiryText.setTextColor(
                android.graphics.Color.DKGRAY
        );

        LinearLayout.LayoutParams expiryParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        expiryParams.setMargins(
                0,
                5,
                0,
                0
        );

        expiryText.setLayoutParams(
                expiryParams
        );

        card.addView(expiryText);

        ingredientsContainer.addView(card);
    }

    // ------------------------------------------------
    // BOTTOM NAVIGATION
    // ------------------------------------------------

    private void setupNavigation() {

        // HOME

        navHomeContainer.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            PantryActivity.this,
                            HomeActivity.class
                    );

            startActivity(intent);

            finish();
        });

        // PANTRY

        navPantryContainer.setOnClickListener(v -> {
            // Already on Pantry
        });

        // RECIPE

        navRecipeContainer.setOnClickListener(v -> {

            android.widget.Toast.makeText(
                    PantryActivity.this,
                    "Recipe Finder coming next",
                    android.widget.Toast.LENGTH_SHORT
            ).show();
        });
    }

    // ------------------------------------------------
    // GO TO LOGIN
    // ------------------------------------------------

    private void goToLogin() {

        Intent intent =
                new Intent(
                        PantryActivity.this,
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