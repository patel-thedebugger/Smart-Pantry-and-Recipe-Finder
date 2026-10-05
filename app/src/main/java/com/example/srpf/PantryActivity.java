package com.example.srpf;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private EditText etSearch;

    private LinearLayout chipAll;
    private LinearLayout chipVegetables;
    private LinearLayout chipFruits;
    private LinearLayout chipDairy;

    private TextView tvSort;

    private final List<DocumentSnapshot> allIngredients =
            new ArrayList<>();

    private String selectedCategory = "All";
    private boolean sortByExpiry = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pantry);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initializeViews();
        setupNavigation();
        setupSearch();
        setupFilters();

        btnAddIngredient.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PantryActivity.this,
                    AddIngredientActivity.class
            );

            startActivity(intent);
        });

        loadIngredients();
    }

    // ====================================================
    // INITIALIZE VIEWS
    // ====================================================

    private void initializeViews() {

        ingredientsContainer =
                findViewById(R.id.ingredientsContainer);

        btnAddIngredient =
                findViewById(R.id.btnAddIngredient);

        etSearch =
                findViewById(R.id.etSearch);

        chipAll =
                findViewById(R.id.chipAll);

        chipVegetables =
                findViewById(R.id.chipVegetables);

        chipFruits =
                findViewById(R.id.chipFruits);

        chipDairy =
                findViewById(R.id.chipDairy);

        tvSort =
                findViewById(R.id.tvSort);

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

    // ====================================================
    // LOAD INGREDIENTS FROM FIRESTORE
    // ====================================================

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

                    if (error != null || snapshot == null) {
                        return;
                    }

                    allIngredients.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Long expiry = doc.getLong("expiryDate");

                        if (isExpired(expiry)) {
                            doc.getReference().delete();   // removes it from Firestore
                        } else {
                            allIngredients.add(doc);
                        }
                    }


                    displayIngredients();
                });
    }

    // ====================================================
    // DISPLAY INGREDIENTS
    // ====================================================

    private void displayIngredients() {

        ingredientsContainer.removeAllViews();

        List<DocumentSnapshot> filtered =
                new ArrayList<>();

        String searchText =
                etSearch.getText()
                        .toString()
                        .trim()
                        .toLowerCase(Locale.getDefault());

        for (DocumentSnapshot document : allIngredients) {

            String name =
                    document.getString("name");

            if (name == null) {
                name = "";
            }

            boolean matchesSearch =
                    searchText.isEmpty()
                            || name.toLowerCase(
                            Locale.getDefault()
                    ).contains(searchText);

            boolean matchesCategory =
                    matchesCategory(document, name);

            if (matchesSearch && matchesCategory) {
                filtered.add(document);
            }
        }

        // Sort by expiration date
        if (sortByExpiry) {

            Collections.sort(
                    filtered,
                    (o1, o2) -> {

                        Long date1 =
                                o1.getLong("expiryDate");

                        Long date2 =
                                o2.getLong("expiryDate");

                        if (date1 == null) return 1;
                        if (date2 == null) return -1;

                        return date1.compareTo(date2);
                    }
            );
        }

        if (filtered.isEmpty()) {

            TextView emptyText =
                    new TextView(this);

            emptyText.setText(
                    allIngredients.isEmpty()
                            ? "Your pantry is empty.\nAdd your first ingredient!"
                            : "No ingredients found."
            );

            emptyText.setTextSize(15);
            emptyText.setTextColor(
                    Color.rgb(110, 110, 110)
            );

            emptyText.setGravity(Gravity.CENTER);

            emptyText.setPadding(
                    20,
                    60,
                    20,
                    60
            );

            ingredientsContainer.addView(
                    emptyText
            );

            return;
        }

        for (int i = 0; i < filtered.size(); i += 2) {

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            ingredientsContainer.addView(
                    row,
                    new LinearLayout.LayoutParams(
                            MATCH_PARENT,
                            WRAP_CONTENT
                    )
            );

            addIngredientCard(filtered.get(i), row);

            if (i + 1 < filtered.size()) {
                addIngredientCard(filtered.get(i + 1), row);
            } else {
                // keeps a lone last card at half width
                View filler = new View(this);
                filler.setLayoutParams(
                        new LinearLayout.LayoutParams(0, 1, 1f));
                row.addView(filler);
            }
        }
    }

    // ====================================================
    // CATEGORY MATCHING
    // ====================================================

    private boolean matchesCategory(DocumentSnapshot document, String name) {

        if (selectedCategory.equals("All")) {
            return true;
        }
        String saved = document.getString("category");
        if (saved != null && !saved.isEmpty()) {
            return saved.equalsIgnoreCase(selectedCategory);
        }

        // 2) Old items with no category: fall back to name matching
        String value =
                name.toLowerCase(Locale.getDefault());

        if (selectedCategory.equals("Vegetables")) {

            String[] vegetables = {
                    "spinach",
                    "potato",
                    "tomato",
                    "onion",
                    "carrot",
                    "broccoli",
                    "cabbage",
                    "pepper",
                    "bell pepper",
                    "peas",
                    "cucumber",
                    "lettuce",
                    "garlic",
                    "ginger"
            };

            return containsAny(value, vegetables);
        }

        if (selectedCategory.equals("Fruits")) {

            String[] fruits = {
                    "apple",
                    "banana",
                    "orange",
                    "mango",
                    "avocado",
                    "grape",
                    "berry",
                    "strawberry",
                    "watermelon",
                    "pineapple",
                    "papaya"
            };

            return containsAny(value, fruits);
        }

        if (selectedCategory.equals("Dairy")) {

            String[] dairy = {
                    "milk",
                    "cheese",
                    "butter",
                    "yogurt",
                    "curd",
                    "cream"
            };

            return containsAny(value, dairy);
        }

        return true;
    }

    private boolean containsAny(
            String value,
            String[] words
    ) {

        for (String word : words) {

            if (value.contains(word)) {
                return true;
            }
        }

        return false;
    }

    // ====================================================
    // INGREDIENT CARD
    // ====================================================

    private void addIngredientCard(DocumentSnapshot document, LinearLayout parent) {

        String documentId =
                document.getId();

        String name =
                document.getString("name");

        Double quantity =
                document.getDouble("quantity");

        String unit =
                document.getString("unit");

        Long expiryDate =
                document.getLong("expiryDate");

        if (name == null) {
            name = "Unknown ingredient";
        }

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

        card.setBackground(
                createRoundedBackground(
                        Color.WHITE,
                        18
                )
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        0,
                        WRAP_CONTENT,
                        1f
                );
        cardParams.setMargins(6, 0, 6, 12);

        card.setLayoutParams(cardParams);

        // ------------------------------------------------
        // TOP ROW
        // ------------------------------------------------

        LinearLayout topRow =
                new LinearLayout(this);

        topRow.setOrientation(
                LinearLayout.VERTICAL
        );

        topRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        // Name
        TextView nameText =
                new TextView(this);

        nameText.setText(name);

        nameText.setTextSize(18);
        nameText.setTextColor(
                Color.rgb(20, 20, 20)
        );

        nameText.setTypeface(
                null,
                Typeface.BOLD
        );

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);

        nameText.setLayoutParams(nameParams);

        topRow.addView(nameText);

        // Status
        TextView statusText =
                createStatusText(expiryDate);

        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        sp.setMargins(0, 6, 0, 0);
        statusText.setLayoutParams(sp);

        topRow.addView(statusText);

        card.addView(topRow);



        // ------------------------------------------------
        // QUANTITY + EXPIRY
        // ------------------------------------------------

        LinearLayout detailsRow =
                new LinearLayout(this);

        detailsRow.setOrientation(
                LinearLayout.VERTICAL
        );

        detailsRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams detailsParams =
                new LinearLayout.LayoutParams(
                        MATCH_PARENT,
                        WRAP_CONTENT
                );

        detailsParams.setMargins(
                0,
                7,
                0,
                0
        );

        detailsRow.setLayoutParams(
                detailsParams
        );

        String quantityDisplay =
                quantity != null
                        ? formatQuantity(quantity)
                        : "-";

        TextView quantityText =
                new TextView(this);

        quantityText.setText(
                quantityDisplay
                        + " "
                        + (unit != null ? unit : "")
        );

        quantityText.setTextSize(15);
        quantityText.setTextColor(
                Color.rgb(70, 90, 70)
        );

        detailsRow.addView(
                quantityText
        );

        TextView separator =
                new TextView(this);

        separator.setText("  •  ");
        separator.setTextSize(14);
        separator.setTextColor(
                Color.LTGRAY
        );

//        detailsRow.addView(separator);

        TextView expiryText =
                new TextView(this);

        expiryText.setText(
                getExpiryDescription(expiryDate)
        );

        expiryText.setTextSize(14);

        if (isExpired(expiryDate)) {

            expiryText.setTextColor(
                    Color.rgb(190, 35, 35)
            );

        } else if (isExpiringSoon(expiryDate)) {

            expiryText.setTextColor(
                    Color.rgb(190, 75, 30)
            );

        } else {

            expiryText.setTextColor(
                    Color.rgb(75, 100, 75)
            );
        }

        detailsRow.addView(
                expiryText
        );

        card.addView(detailsRow);

        // ------------------------------------------------
        // BOTTOM ACTION ROW
        // ------------------------------------------------

        LinearLayout actionRow =
                new LinearLayout(this);

        actionRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        actionRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        LinearLayout.LayoutParams actionParams =
                new LinearLayout.LayoutParams(
                        MATCH_PARENT,
                        WRAP_CONTENT
                );

        actionParams.setMargins(
                0,
                14,
                0,
                0
        );

        actionRow.setLayoutParams(
                actionParams
        );

        // Minus
        TextView minus =
                createQuantityButton("−");

        // Quantity display
        TextView currentQuantity =
                createQuantityButton(
                        formatQuantity(quantity)
                );

        // Plus
        TextView plus =
                createQuantityButton("+");

        LinearLayout quantityControls =
                new LinearLayout(this);

        quantityControls.setOrientation(
                LinearLayout.HORIZONTAL
        );

        quantityControls.setGravity(
                Gravity.CENTER
        );

        quantityControls.setPadding(
                10,
                4,
                10,
                4
        );

        quantityControls.setBackground(
                createRoundedBackground(
                        Color.rgb(246, 246, 242),
                        30
                )
        );

        quantityControls.addView(minus);
        quantityControls.addView(currentQuantity);
        quantityControls.addView(plus);

        actionRow.addView(
                quantityControls
        );

        // Spacer
        View spacer =
                new View(this);

        LinearLayout.LayoutParams spacerParams =
                new LinearLayout.LayoutParams(
                        0,
                        1,
                        1
                );

        actionRow.addView(
                spacer
        );

        // Edit
        TextView edit =
                new TextView(this);

        edit.setText("✎  Edit");
        edit.setTextSize(14);
        edit.setTextColor(
                Color.rgb(70, 90, 70)
        );

        edit.setPadding(
                8,
                10,
                12,
                10
        );

        edit.setOnClickListener(v ->
                Toast.makeText(
                        PantryActivity.this,
                        "Edit ingredient coming next",
                        Toast.LENGTH_SHORT
                ).show()
        );

//        actionRow.addView(edit);

        // Delete
        TextView delete =
                new TextView(this);

        delete.setText("▣  Delete");
        delete.setTextSize(14);
        delete.setTextColor(
                Color.rgb(210, 40, 40)
        );

        delete.setPadding(
                8,
                10,
                4,
                10
        );

        delete.setOnClickListener(v ->
                deleteIngredient(documentId)
        );

//        actionRow.addView(delete);

        LinearLayout editDeleteRow = new LinearLayout(this);
        editDeleteRow.setOrientation(LinearLayout.HORIZONTAL);
        editDeleteRow.addView(edit);
        editDeleteRow.addView(delete);

        card.addView(actionRow);
        card.addView(editDeleteRow);

        // Quantity buttons
        minus.setOnClickListener(v -> {

            if (quantity == null || quantity <= 0) {
                return;
            }

            double newQuantity =
                    Math.max(0, quantity - 1);

            updateQuantity(
                    documentId,
                    newQuantity
            );
        });

        plus.setOnClickListener(v -> {

            double current =
                    quantity != null
                            ? quantity
                            : 0;

            updateQuantity(
                    documentId,
                    current + 1
            );
        });

        parent.addView(card);
    }

    // ====================================================
    // STATUS
    // ====================================================

    private TextView createStatusText(
            Long expiryDate
    ) {

        TextView status =
                new TextView(this);

        status.setTextSize(12);
        status.setTypeface(
                null,
                Typeface.BOLD
        );

        status.setPadding(
                12,
                6,
                12,
                6
        );

        if (isExpired(expiryDate)) {

            status.setText("● Expired");

            status.setTextColor(
                    Color.rgb(170, 35, 35)
            );

            status.setBackground(
                    createRoundedBackground(
                            Color.rgb(255, 220, 220),
                            30
                    )
            );

        } else if (isExpiringSoon(expiryDate)) {

            status.setText("◷ Expiring Soon");

            status.setTextColor(
                    Color.rgb(175, 65, 25)
            );

            status.setBackground(
                    createRoundedBackground(
                            Color.rgb(255, 220, 210),
                            30
                    )
            );

        } else {

            status.setText("✓ In Stock");

            status.setTextColor(
                    Color.rgb(45, 110, 45)
            );

            status.setBackground(
                    createRoundedBackground(
                            Color.rgb(220, 238, 210),
                            30
                    )
            );
        }

        return status;
    }

    private String getExpiryDescription(
            Long expiryDate
    ) {

        if (expiryDate == null) {
            return "No expiry date";
        }

        long now =
                System.currentTimeMillis();

        long difference =
                expiryDate - now;

        long day =
                24L * 60L * 60L * 1000L;

        if (difference < 0) {

            long daysAgo =
                    Math.max(
                            1,
                            Math.abs(difference) / day
                    );

            return "Expired "
                    + daysAgo
                    + (daysAgo == 1
                    ? " day ago"
                    : " days ago");
        }

        long days =
                difference / day;

        if (days == 0) {
            return "Expires today";
        }

        if (days == 1) {
            return "Expires tomorrow";
        }

        if (days <= 3) {
            return "Expires in "
                    + days
                    + " days";
        }

        return "Fresh ("
                + days
                + " days)";
    }

    private boolean isExpired(
            Long expiryDate
    ) {

        return expiryDate != null
                && expiryDate <
                System.currentTimeMillis();
    }

    private boolean isExpiringSoon(
            Long expiryDate
    ) {

        if (expiryDate == null) {
            return false;
        }

        long now =
                System.currentTimeMillis();

        long threeDays =
                3L
                        * 24L
                        * 60L
                        * 60L
                        * 1000L;

        return expiryDate >= now
                && expiryDate <= now + threeDays;
    }

    // ====================================================
    // QUANTITY
    // ====================================================

    private TextView createQuantityButton(
            String text
    ) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(
                Color.rgb(65, 75, 65)
        );

        view.setGravity(
                Gravity.CENTER
        );

        view.setPadding(
                9,
                5,
                9,
                5
        );

        return view;
    }

    private String formatQuantity(
            Double quantity
    ) {

        if (quantity == null) {
            return "-";
        }

        if (quantity % 1 == 0) {
            return String.valueOf(
                    quantity.intValue()
            );
        }

        return String.valueOf(quantity);
    }

    private void updateQuantity(
            String documentId,
            double quantity
    ) {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        firestore
                .collection("users")
                .document(user.getUid())
                .collection("ingredients")
                .document(documentId)
                .update(
                        "quantity",
                        quantity
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                PantryActivity.this,
                                "Could not update quantity",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    // ====================================================
    // DELETE
    // ====================================================

    private void deleteIngredient(
            String documentId
    ) {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Ingredient")
                .setMessage(
                        "Are you sure you want to delete this ingredient?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            firestore
                                    .collection("users")
                                    .document(user.getUid())
                                    .collection("ingredients")
                                    .document(documentId)
                                    .delete()
                                    .addOnSuccessListener(unused ->
                                            Toast.makeText(
                                                    PantryActivity.this,
                                                    "Ingredient deleted",
                                                    Toast.LENGTH_SHORT
                                            ).show()
                                    );
                        }
                )
                .show();
    }

    // ====================================================
    // SEARCH
    // ====================================================

    private void setupSearch() {

        etSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        displayIngredients();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    // ====================================================
    // FILTERS
    // ====================================================

    private void setupFilters() {

        chipAll.setOnClickListener(v -> {

            selectedCategory = "All";
            updateChipStyles();
            displayIngredients();
        });

        chipVegetables.setOnClickListener(v -> {

            selectedCategory = "Vegetables";
            updateChipStyles();
            displayIngredients();
        });

        chipFruits.setOnClickListener(v -> {

            selectedCategory = "Fruits";
            updateChipStyles();
            displayIngredients();
        });

        chipDairy.setOnClickListener(v -> {

            selectedCategory = "Dairy";
            updateChipStyles();
            displayIngredients();
        });

        tvSort.setOnClickListener(v -> {

            sortByExpiry = !sortByExpiry;

            tvSort.setText(
                    sortByExpiry
                            ? "↕  Sort by: Expiring soon"
                            : "↕  Sort by: Recently added"
            );

            if (!sortByExpiry) {

                Collections.reverse(
                        allIngredients
                );
            }

            displayIngredients();
        });

        updateChipStyles();
    }

    private void updateChipStyles() {

        styleChip(
                chipAll,
                selectedCategory.equals("All")
        );

        styleChip(
                chipVegetables,
                selectedCategory.equals("Vegetables")
        );

        styleChip(
                chipFruits,
                selectedCategory.equals("Fruits")
        );

        styleChip(
                chipDairy,
                selectedCategory.equals("Dairy")
        );
    }

    private void styleChip(
            LinearLayout chip,
            boolean selected
    ) {

        chip.setBackground(
                createRoundedBackground(
                        selected
                                ? Color.rgb(38, 125, 47)
                                : Color.WHITE,
                        40
                )
        );

        TextView text =
                (TextView) chip.getChildAt(0);

        text.setTextColor(
                selected
                        ? Color.WHITE
                        : Color.rgb(50, 65, 50)
        );
    }

    // ====================================================
    // NAVIGATION
    // ====================================================

    private void setupNavigation() {

        navHomeContainer.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            PantryActivity.this,
                            HomeActivity.class
                    );

            startActivity(intent);
            finish();
        });

        navPantryContainer.setOnClickListener(v -> {
            // Already on Pantry
        });

        navRecipeContainer.setOnClickListener(v -> {

            Toast.makeText(
                    PantryActivity.this,
                    "Recipe Finder coming next",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    // ====================================================
    // DRAWABLE
    // ====================================================

    private GradientDrawable createRoundedBackground(
            int color,
            float radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        return drawable;
    }

    // ====================================================
    // LOGIN
    // ====================================================

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