package com.example.srpf;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditIngredientActivity extends AppCompatActivity {

    private EditText etIngredientName;
    private EditText etQuantity;
    private Spinner spinnerUnit;
    private Spinner spinnerCategory;
    private TextView tvExpiryDate;
    private Button btnSaveIngredient;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private String ingredientId;

    private long selectedExpiryDate = 0;

    private Calendar selectedDate = Calendar.getInstance();

    private static final String[] units = {
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

    private static final String[] categories = {
            "Select Category",
            "Fruits",
            "Vegetables",
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

        etIngredientName = findViewById(R.id.etIngredientName);
        etQuantity = findViewById(R.id.etQuantity);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        tvExpiryDate = findViewById(R.id.tvExpiryDate);
        btnSaveIngredient = findViewById(R.id.btnSaveIngredient);

        ingredientId = getIntent().getStringExtra("ingredientId");

        if (ingredientId == null || ingredientId.isEmpty()) {
            Toast.makeText(
                    this,
                    "Ingredient information missing",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        setupSpinners();
        loadIngredient();

        tvExpiryDate.setOnClickListener(v -> showDatePicker());

        btnSaveIngredient.setText("Save Changes");

        btnSaveIngredient.setOnClickListener(v -> updateIngredient());
    }

    private void setupSpinners() {

        ArrayAdapter<String> unitAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        units
                );

        unitAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerUnit.setAdapter(unitAdapter);


        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void loadIngredient() {

        String userId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : null;

        if (userId == null) {
            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        firestore
                .collection("users")
                .document(userId)
                .collection("ingredients")
                .document(ingredientId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {

                        Toast.makeText(
                                this,
                                "Ingredient not found",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                        return;
                    }

                    populateFields(documentSnapshot);
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Failed to load ingredient: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                });
    }

    private void populateFields(DocumentSnapshot document) {

        String name = document.getString("name");
        Double quantity = document.getDouble("quantity");
        String unit = document.getString("unit");
        String category = document.getString("category");

        Long expiryDate = document.getLong("expiryDate");

        if (name != null) {
            etIngredientName.setText(name);
        }

        if (quantity != null) {
            etQuantity.setText(String.valueOf(quantity));
        }

        setSpinnerSelection(spinnerUnit, unit);
        setSpinnerSelection(spinnerCategory, category);

        if (expiryDate != null) {

            selectedExpiryDate = expiryDate;

            selectedDate.setTimeInMillis(expiryDate);

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    );

            tvExpiryDate.setText(
                    dateFormat.format(selectedDate.getTime())
            );
        }
    }

    private void setSpinnerSelection(
            Spinner spinner,
            String value
    ) {

        if (value == null) {
            return;
        }

        for (int i = 0; i < spinner.getCount(); i++) {

            if (value.equalsIgnoreCase(
                    spinner.getItemAtPosition(i).toString()
            )) {

                spinner.setSelection(i);
                break;
            }
        }
    }

    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        if (selectedExpiryDate > 0) {
            calendar.setTimeInMillis(selectedExpiryDate);
        }

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            selectedDate.set(
                                    year,
                                    month,
                                    dayOfMonth,
                                    0,
                                    0,
                                    0
                            );

                            selectedDate.set(
                                    Calendar.MILLISECOND,
                                    0
                            );

                            selectedExpiryDate =
                                    selectedDate.getTimeInMillis();

                            SimpleDateFormat dateFormat =
                                    new SimpleDateFormat(
                                            "dd/MM/yyyy",
                                            Locale.getDefault()
                                    );

                            tvExpiryDate.setText(
                                    dateFormat.format(
                                            selectedDate.getTime()
                                    )
                            );
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        datePickerDialog.show();
    }

    private void updateIngredient() {

        String name =
                etIngredientName.getText()
                        .toString()
                        .trim();

        String quantityText =
                etQuantity.getText()
                        .toString()
                        .trim();

        if (quantityText.isEmpty()) {
            etQuantity.setError("Please enter quantity");
            etQuantity.requestFocus();
            return;
        }
        double quantity;

        try {
            quantity = Double.parseDouble(quantityText);
        } catch (NumberFormatException e) {

            etQuantity.setError("Please enter a valid quantity");
            etQuantity.requestFocus();
            return;
        }

        String unit =
                spinnerUnit.getSelectedItem()
                        .toString();

        String category =
                spinnerCategory.getSelectedItem()
                        .toString();

        if (name.isEmpty()) {

            etIngredientName.setError(
                    "Please enter ingredient name"
            );

            etIngredientName.requestFocus();
            return;
        }

        if (selectedExpiryDate <= 0) {

            Toast.makeText(
                    this,
                    "Please select an expiry date",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (selectedExpiryDate <= System.currentTimeMillis()) {

            Toast.makeText(
                    this,
                    "Expiry date must be in the future",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String userId =
                firebaseAuth.getCurrentUser() != null
                        ? firebaseAuth.getCurrentUser().getUid()
                        : null;

        if (userId == null) {
            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        btnSaveIngredient.setEnabled(false);
        btnSaveIngredient.setText("Saving...");

        Map<String, Object> updates =
                new HashMap<>();

        updates.put("name", name);
        updates.put("quantity", quantity);
        updates.put("unit", unit);
        updates.put("category", category);
        updates.put("expiryDate", selectedExpiryDate);

        firestore
                .collection("users")
                .document(userId)
                .collection("ingredients")
                .document(ingredientId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    // Cancel notifications associated
                    // with the old expiry date.
                    ExpiryNotificationScheduler
                            .cancelExpiryNotifications(
                                    EditIngredientActivity.this,
                                    ingredientId
                            );

                    // Schedule notifications using
                    // the new expiry date.
                    ExpiryNotificationScheduler
                            .scheduleExpiryNotifications(
                                    EditIngredientActivity.this,
                                    ingredientId,
                                    name,
                                    selectedExpiryDate
                            );

                    Toast.makeText(
                            EditIngredientActivity.this,
                            "Ingredient updated successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent =
                            new Intent(
                                    EditIngredientActivity.this,
                                    PantryActivity.class
                            );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    );

                    startActivity(intent);

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnSaveIngredient.setEnabled(true);
                    btnSaveIngredient.setText("Save Changes");

                    Toast.makeText(
                            EditIngredientActivity.this,
                            "Failed to update ingredient: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}