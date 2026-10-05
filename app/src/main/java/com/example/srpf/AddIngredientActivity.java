package com.example.srpf;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddIngredientActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private EditText etIngredientName;
    private EditText etQuantity;

    private Spinner spinnerUnit;
    private Spinner spinnerCategory;

    private TextView tvExpiryDate;
    private Button btnSaveIngredient;

    private LinearLayout navHomeContainer;
    private ImageView navHomeIcon;
    private TextView navHomeText;

    private long selectedExpiryDate = 0;

    private static final String[] UNITS = {
            "Select Unit",
            "Pieces",
            "Kg",
            "Gram",
            "Litres",
            "Millilitres",
            "Pack",
            "Bottle",
            "Can",
            "Cup",
            "Tablespoon",
            "Teaspoon"
    };

    private static final String[] CATEGORIES = {
            "Select Category",
            "Produce (Fruits & Vegetables)",
            "Dairy & Alternatives",
            "Meat, Poultry & Seafood",
            "Bakery & Grains",
            "Spices & Baking",
            "Pantry Staples & Canned Goods"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_ingredient);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initializeViews();

        setupUnitSpinner();
        setupCategorySpinner();
        setupDatePicker();
        setUpNavigation();

        btnSaveIngredient.setOnClickListener(v -> saveIngredient());
    }

    private void initializeViews() {

        etIngredientName = findViewById(R.id.etIngredientName);
        etQuantity = findViewById(R.id.etQuantity);

        spinnerUnit = findViewById(R.id.spinnerUnit);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        tvExpiryDate = findViewById(R.id.tvExpiryDate);

        btnSaveIngredient = findViewById(R.id.btnSaveIngredient);

        navHomeContainer = findViewById(R.id.navHomeContainer);
        navHomeIcon = findViewById(R.id.navHomeIcon);
        navHomeText = findViewById(R.id.navHomeText);
    }

    // ----------------------------------------------------
    // UNIT SPINNER
    // ----------------------------------------------------

    private void setupUnitSpinner() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                UNITS
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerUnit.setAdapter(adapter);
    }

    // ----------------------------------------------------
    // CATEGORY SPINNER
    // ----------------------------------------------------

    private void setupCategorySpinner() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                CATEGORIES
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCategory.setAdapter(adapter);
    }

    // ----------------------------------------------------
    // DATE PICKER
    // ----------------------------------------------------

    private void setupDatePicker() {

        tvExpiryDate.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog =
                    new DatePickerDialog(
                            AddIngredientActivity.this,

                            (view, selectedYear, selectedMonth, selectedDay) -> {

                                Calendar selectedDate =
                                        Calendar.getInstance();

                                selectedDate.set(
                                        selectedYear,
                                        selectedMonth,
                                        selectedDay,
                                        23,
                                        59,
                                        59
                                );

                                selectedExpiryDate =
                                        selectedDate.getTimeInMillis();

                                String formattedDate =
                                        String.format(
                                                "%02d/%02d/%04d",
                                                selectedDay,
                                                selectedMonth + 1,
                                                selectedYear
                                        );

                                tvExpiryDate.setText(formattedDate);

                                tvExpiryDate.setTextColor(
                                        0xFF333333
                                );
                            },

                            year,
                            month,
                            day
                    );

            datePickerDialog
                    .getDatePicker()
                    .setMinDate(
                            System.currentTimeMillis()
                    );

            datePickerDialog.show();
        });
    }

    // ----------------------------------------------------
    // SAVE INGREDIENT
    // ----------------------------------------------------

    private void saveIngredient() {

        String name =
                etIngredientName
                        .getText()
                        .toString()
                        .trim();

        String quantityText =
                etQuantity
                        .getText()
                        .toString()
                        .trim();

        String unit =
                spinnerUnit
                        .getSelectedItem()
                        .toString();

        String category =
                spinnerCategory
                        .getSelectedItem()
                        .toString();

        // Name
        if (name.isEmpty()) {

            etIngredientName.setError(
                    "Please enter ingredient name"
            );

            etIngredientName.requestFocus();
            return;
        }

        // Quantity
        if (quantityText.isEmpty()) {

            etQuantity.setError(
                    "Please enter quantity"
            );

            etQuantity.requestFocus();
            return;
        }

        // Unit
        if (unit.equals("Select Unit")) {

            Toast.makeText(
                    this,
                    "Please select a unit",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Category
        if (category.equals("Select Category")) {

            Toast.makeText(
                    this,
                    "Please select a category",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Expiry
        if (selectedExpiryDate == 0) {

            Toast.makeText(
                    this,
                    "Please select expiration date",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        double quantity;

        try {

            quantity =
                    Double.parseDouble(quantityText);

        } catch (NumberFormatException e) {

            etQuantity.setError(
                    "Please enter a valid quantity"
            );

            etQuantity.requestFocus();
            return;
        }

        if (quantity <= 0) {

            etQuantity.setError(
                    "Quantity must be greater than 0"
            );

            etQuantity.requestFocus();
            return;
        }

        // ------------------------------------------------
        // CREATE FIRESTORE DOCUMENT
        // ------------------------------------------------

        Map<String, Object> ingredient =
                new HashMap<>();

        ingredient.put(
                "name",
                name
        );

        ingredient.put(
                "quantity",
                quantity
        );

        ingredient.put(
                "unit",
                unit
        );

        ingredient.put(
                "category",
                category
        );

        ingredient.put(
                "expiryDate",
                selectedExpiryDate
        );

        ingredient.put(
                "createdAt",
                System.currentTimeMillis()
        );

        btnSaveIngredient.setEnabled(false);
        btnSaveIngredient.setText("Saving...");

        // ------------------------------------------------
        // SAVE UNDER CURRENT USER
        // ------------------------------------------------

        firestore
                .collection("users")
                .document(currentUser.getUid())
                .collection("ingredients")
                .add(ingredient)

                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(
                            AddIngredientActivity.this,
                            "Ingredient added successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    /*
                     * Returning to PantryActivity.
                     *
                     * PantryActivity has a Firestore
                     * snapshot listener, so the new
                     * ingredient will appear automatically.
                     */

                    finish();
                })

                .addOnFailureListener(e -> {

                    btnSaveIngredient.setEnabled(true);

                    btnSaveIngredient.setText(
                            "Save Ingredient"
                    );

                    Toast.makeText(
                            AddIngredientActivity.this,
                            "Failed to save ingredient: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // ----------------------------------------------------
    // NAVIGATION
    // ----------------------------------------------------

    private void setUpNavigation() {

        navHomeContainer.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            AddIngredientActivity.this,
                            HomeActivity.class
                    );

            startActivity(intent);

            finish();
        });
    }
}