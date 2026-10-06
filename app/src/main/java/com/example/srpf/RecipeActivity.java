package com.example.srpf;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeActivity extends AppCompatActivity {

    private static final String TAG = "SRPF_RECIPE";

    /*
     * An ingredient is considered "Use Soon"
     * if it expires within the next 3 days.
     */
    private static final long USE_SOON_DAYS = 3;

    // =========================================================
    // UI
    // =========================================================

    private EditText etRecipeSearch;
    private ImageButton btnRecipeSearch;

    private LinearLayout recipeContainer;
    private LinearLayout layoutEmptyRecipes;

    private LinearLayout navHomeContainer;
    private LinearLayout navPantryContainer;
    private LinearLayout navRecipeContainer;

    // =========================================================
    // Firebase
    // =========================================================

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // =========================================================
    // Pantry
    // =========================================================

    private final List<PantryIngredient> pantryIngredients =
            new ArrayList<>();

    // =========================================================
    // Pantry Ingredient Model
    // =========================================================

    private static class PantryIngredient {

        String name;
        long expiryDate;

        PantryIngredient(
                String name,
                long expiryDate
        ) {
            this.name = name;
            this.expiryDate = expiryDate;
        }
    }

    // =========================================================
    // Recipe Model
    // =========================================================

    private static class Recipe {

        String id;
        String name;

        int calories;
        int cookingTime;
        int matchPercentage;

        /*
         * Number of ingredients in this recipe
         * that are expiring soon.
         */
        int useSoonCount;

        ArrayList<String> availableIngredients;
        ArrayList<String> missingIngredients;

        Recipe(
                String id,
                String name,
                int calories,
                int cookingTime,
                int matchPercentage,
                int useSoonCount,
                ArrayList<String> availableIngredients,
                ArrayList<String> missingIngredients
        ) {

            this.id = id;
            this.name = name;

            this.calories = calories;
            this.cookingTime = cookingTime;
            this.matchPercentage = matchPercentage;

            this.useSoonCount = useSoonCount;

            this.availableIngredients =
                    availableIngredients;

            this.missingIngredients =
                    missingIngredients;
        }
    }

    // =========================================================
    // onCreate
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recipe);

        // -----------------------------------------------------
        // Firebase
        // -----------------------------------------------------

        firebaseAuth =
                FirebaseAuth.getInstance();

        firestore =
                FirebaseFirestore.getInstance();

        // -----------------------------------------------------
        // Search UI
        // -----------------------------------------------------

        etRecipeSearch =
                findViewById(R.id.etRecipeSearch);

        btnRecipeSearch =
                findViewById(R.id.btnRecipeSearch);

        // -----------------------------------------------------
        // Recipe UI
        // -----------------------------------------------------

        recipeContainer =
                findViewById(R.id.recipeContainer);

        layoutEmptyRecipes =
                findViewById(R.id.layoutEmptyRecipes);

        // -----------------------------------------------------
        // Bottom Navigation
        // -----------------------------------------------------

        navHomeContainer =
                findViewById(R.id.navHomeContainer);

        navPantryContainer =
                findViewById(R.id.navPantryContainer);

        navRecipeContainer =
                findViewById(R.id.navRecipeContainer);

        // -----------------------------------------------------
        // Load pantry
        // -----------------------------------------------------

        loadPantryIngredients();

        // -----------------------------------------------------
        // Search button
        // -----------------------------------------------------

        btnRecipeSearch.setOnClickListener(v -> {

            String query =
                    etRecipeSearch
                            .getText()
                            .toString()
                            .trim();

            if (query.isEmpty()) {

                Toast.makeText(
                        RecipeActivity.this,
                        "Please enter a recipe or ingredient",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            searchRecipes(query);
        });

        // -----------------------------------------------------
        // Home navigation
        // -----------------------------------------------------

        navHomeContainer.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            RecipeActivity.this,
                            HomeActivity.class
                    );

            startActivity(intent);

            finish();
        });

        // -----------------------------------------------------
        // Pantry navigation
        // -----------------------------------------------------

        navPantryContainer.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            RecipeActivity.this,
                            PantryActivity.class
                    );

            startActivity(intent);

            finish();
        });

        // -----------------------------------------------------
        // Current Recipe page
        // -----------------------------------------------------

        navRecipeContainer.setOnClickListener(v -> {
            // Already on Recipe page
        });
    }

    // =========================================================
    // LOAD PANTRY INGREDIENTS
    // =========================================================

    private void loadPantryIngredients() {

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String userId =
                user.getUid();

        Log.d(
                TAG,
                "Loading pantry for user: "
                        + userId
        );

        firestore
                .collection("users")
                .document(userId)
                .collection("ingredients")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    pantryIngredients.clear();

                    for (
                            QueryDocumentSnapshot document :
                            querySnapshot
                    ) {

                        // -------------------------------------------------
                        // Ingredient name
                        // -------------------------------------------------

                        String name =
                                document.getString("name");

                        // -------------------------------------------------
                        // Expiry date
                        // -------------------------------------------------

                        Long expiryDate =
                                document.getLong("expiryDate");

                        if (
                                name != null &&
                                        !name.trim().isEmpty()
                        ) {

                            long expiryMillis = 0;

                            if (expiryDate != null) {

                                expiryMillis =
                                        expiryDate;
                            }

                            pantryIngredients.add(
                                    new PantryIngredient(
                                            name.trim(),
                                            expiryMillis
                                    )
                            );

                            Log.d(
                                    TAG,
                                    "Pantry item: "
                                            + name
                                            + " | Expiry: "
                                            + expiryMillis
                            );
                        }
                    }

                    Log.d(
                            TAG,
                            "Pantry ingredients loaded: "
                                    + getPantryNames()
                    );

                    Log.d(
                            TAG,
                            "Use Soon ingredients: "
                                    + getUseSoonIngredients()
                    );

                    /*
                     * Automatically display recommended
                     * recipes when Recipe page opens.
                     */
                    searchRecipes("");

                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "Failed to load pantry ingredients",
                            e
                    );

                    Toast.makeText(
                            RecipeActivity.this,
                            "Unable to load pantry ingredients",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================================================
    // GET PANTRY NAMES
    // =========================================================

    private ArrayList<String> getPantryNames() {

        ArrayList<String> names =
                new ArrayList<>();

        for (
                PantryIngredient ingredient :
                pantryIngredients
        ) {

            names.add(
                    ingredient.name
            );
        }

        return names;
    }

    // =========================================================
    // GET USE SOON INGREDIENTS
    // =========================================================

    private Set<String> getUseSoonIngredients() {

        Set<String> useSoonIngredients =
                new HashSet<>();

        long currentTime =
                System.currentTimeMillis();

        long threeDaysFromNow =
                currentTime
                        + (
                        USE_SOON_DAYS
                                * 24L
                                * 60L
                                * 60L
                                * 1000L
                );

        for (
                PantryIngredient ingredient :
                pantryIngredients
        ) {

            /*
             * expiryDate == 0 means
             * no valid expiry date.
             */
            if (ingredient.expiryDate <= 0) {
                continue;
            }

            /*
             * Future expiry within 3 days.
             */
            if (
                    ingredient.expiryDate >= currentTime
                            &&
                            ingredient.expiryDate
                                    <= threeDaysFromNow
            ) {

                useSoonIngredients.add(
                        normalize(ingredient.name)
                );
            }
        }

        return useSoonIngredients;
    }

    // =========================================================
    // SEARCH RECIPES
    // =========================================================

    private void searchRecipes(String query) {

        Log.d(
                TAG,
                "Recipe search: " + query
        );

        Log.d(
                TAG,
                "Available pantry ingredients: "
                        + getPantryNames()
        );

        if (pantryIngredients.isEmpty()) {

            Toast.makeText(
                    this,
                    "No pantry ingredients found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // -----------------------------------------------------
        // Normalize pantry ingredients
        // -----------------------------------------------------

        Set<String> pantrySet =
                new HashSet<>();

        for (
                PantryIngredient ingredient :
                pantryIngredients
        ) {

            if (
                    ingredient.name != null &&
                            !ingredient.name.trim().isEmpty()
            ) {

                pantrySet.add(
                        normalize(
                                ingredient.name
                        )
                );
            }
        }

        Log.d(
                TAG,
                "Normalized pantry ingredients: "
                        + pantrySet
        );

        // -----------------------------------------------------
        // Get Use Soon ingredients
        // -----------------------------------------------------

        Set<String> useSoonIngredients =
                getUseSoonIngredients();

        Log.d(
                TAG,
                "Use Soon ingredients: "
                        + useSoonIngredients
        );

        // -----------------------------------------------------
        // Current Firebase user
        // -----------------------------------------------------

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String userId =
                user.getUid();

        Log.d(
                TAG,
                "Loading recipes for user: "
                        + userId
        );

        // -----------------------------------------------------
        // Firestore recipe query
        //
        // users
        //   └── userId
        //       └── recipes
        // -----------------------------------------------------

        firestore
                .collection("users")
                .document(userId)
                .collection("recipes")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    Log.d(
                            TAG,
                            "Firestore recipe query SUCCESS"
                    );

                    Log.d(
                            TAG,
                            "Total recipes found: "
                                    + querySnapshot.size()
                    );

                    if (querySnapshot.isEmpty()) {

                        layoutEmptyRecipes
                                .setVisibility(
                                        View.VISIBLE
                                );

                        recipeContainer
                                .setVisibility(
                                        View.GONE
                                );

                        Toast.makeText(
                                RecipeActivity.this,
                                "No recipes found",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    // -------------------------------------------------
                    // Recipe list
                    // -------------------------------------------------

                    List<Recipe> recipes =
                            new ArrayList<>();

                    // -------------------------------------------------
                    // Process each recipe
                    // -------------------------------------------------

                    for (
                            QueryDocumentSnapshot document :
                            querySnapshot
                    ) {

                        Log.d(
                                TAG,
                                "Processing recipe document: "
                                        + document.getId()
                        );

                        String recipeName =
                                document.getString("name");

                        Long calories =
                                document.getLong("calories");

                        Long cookingTime =
                                document.getLong(
                                        "cookingTime"
                                );

                        List<String> recipeIngredients =
                                (List<String>)
                                        document.get(
                                                "ingredients"
                                        );

                        // -------------------------------------------------
                        // Validate recipe
                        // -------------------------------------------------

                        if (
                                recipeName == null ||
                                        recipeName.trim().isEmpty()
                        ) {

                            Log.e(
                                    TAG,
                                    "Recipe name missing: "
                                            + document.getId()
                            );

                            continue;
                        }

                        if (
                                recipeIngredients == null ||
                                        recipeIngredients.isEmpty()
                        ) {

                            Log.e(
                                    TAG,
                                    "Recipe ingredients missing: "
                                            + document.getId()
                            );

                            continue;
                        }

                        // -------------------------------------------------
                        // Search filter
                        // -------------------------------------------------

                        if (
                                query != null &&
                                        !query.trim().isEmpty()
                        ) {

                            String normalizedQuery =
                                    normalize(query);

                            boolean recipeNameMatches =
                                    normalize(recipeName)
                                            .contains(
                                                    normalizedQuery
                                            );

                            boolean ingredientMatches =
                                    false;

                            for (
                                    String ingredient :
                                    recipeIngredients
                            ) {

                                if (
                                        ingredient != null &&
                                                normalize(ingredient)
                                                        .contains(
                                                                normalizedQuery
                                                        )
                                ) {

                                    ingredientMatches =
                                            true;

                                    break;
                                }
                            }

                            if (
                                    !recipeNameMatches &&
                                            !ingredientMatches
                            ) {

                                continue;
                            }
                        }

                        // -------------------------------------------------
                        // Available / Missing
                        // -------------------------------------------------

                        int matchedIngredients = 0;

                        int useSoonCount = 0;

                        ArrayList<String>
                                availableIngredients =
                                new ArrayList<>();

                        ArrayList<String>
                                missingIngredients =
                                new ArrayList<>();

                        for (
                                String ingredient :
                                recipeIngredients
                        ) {

                            if (ingredient == null) {
                                continue;
                            }

                            String originalIngredient =
                                    ingredient.trim();

                            String normalizedIngredient =
                                    normalize(
                                            originalIngredient
                                    );

                            if (
                                    pantrySet.contains(
                                            normalizedIngredient
                                    )
                            ) {

                                matchedIngredients++;

                                availableIngredients.add(
                                        originalIngredient
                                );

                                /*
                                 * Ingredient is available AND
                                 * expires within 3 days.
                                 */
                                if (
                                        useSoonIngredients.contains(
                                                normalizedIngredient
                                        )
                                ) {

                                    useSoonCount++;
                                }

                            } else {

                                missingIngredients.add(
                                        originalIngredient
                                );
                            }
                        }

                        // -------------------------------------------------
                        // Match percentage
                        // -------------------------------------------------

                        int totalIngredients =
                                recipeIngredients.size();

                        int matchPercentage = 0;

                        if (totalIngredients > 0) {

                            matchPercentage =
                                    (
                                            matchedIngredients * 100
                                    )
                                            / totalIngredients;
                        }

                        // -------------------------------------------------
                        // Calories
                        // -------------------------------------------------

                        int caloriesValue =
                                calories != null
                                        ? calories.intValue()
                                        : 0;

                        // -------------------------------------------------
                        // Cooking time
                        // -------------------------------------------------

                        int cookingTimeValue =
                                cookingTime != null
                                        ? cookingTime.intValue()
                                        : 0;

                        // -------------------------------------------------
                        // Create Recipe
                        // -------------------------------------------------

                        Recipe recipe =
                                new Recipe(
                                        document.getId(),
                                        recipeName.trim(),
                                        caloriesValue,
                                        cookingTimeValue,
                                        matchPercentage,
                                        useSoonCount,
                                        availableIngredients,
                                        missingIngredients
                                );

                        recipes.add(recipe);

                        Log.d(
                                TAG,
                                "Recipe: "
                                        + recipeName
                                        + " | Match: "
                                        + matchPercentage
                                        + "%"
                                        + " | Use Soon: "
                                        + useSoonCount
                                        + " | Available: "
                                        + availableIngredients
                                        + " | Missing: "
                                        + missingIngredients
                                        + " | Calories: "
                                        + caloriesValue
                                        + " | Cooking time: "
                                        + cookingTimeValue
                        );
                    }

                    // -------------------------------------------------
                    // Sort
                    //
                    // Priority 1:
                    // More Use Soon ingredients
                    //
                    // Priority 2:
                    // Higher pantry match
                    // -------------------------------------------------

                    Collections.sort(
                            recipes,
                            (r1, r2) -> {

                                if (
                                        r1.useSoonCount
                                                != r2.useSoonCount
                                ) {

                                    return Integer.compare(
                                            r2.useSoonCount,
                                            r1.useSoonCount
                                    );
                                }

                                return Integer.compare(
                                        r2.matchPercentage,
                                        r1.matchPercentage
                                );
                            }
                    );

                    Log.d(
                            TAG,
                            "Recipes sorted by Use Soon "
                                    + "and match percentage"
                    );

                    // -------------------------------------------------
                    // Display
                    // -------------------------------------------------

                    displayRecipes(recipes);

                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "Firestore recipe query FAILED",
                            e
                    );

                    Toast.makeText(
                            RecipeActivity.this,
                            "Unable to load recipes: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // DISPLAY RECIPE CARDS
    // =========================================================

    private void displayRecipes(
            List<Recipe> recipes
    ) {

        recipeContainer.removeAllViews();

        // -----------------------------------------------------
        // No recipes
        // -----------------------------------------------------

        if (recipes.isEmpty()) {

            layoutEmptyRecipes
                    .setVisibility(
                            View.VISIBLE
                    );

            recipeContainer
                    .setVisibility(
                            View.GONE
                    );

            return;
        }

        // -----------------------------------------------------
        // Recipes available
        // -----------------------------------------------------

        layoutEmptyRecipes
                .setVisibility(
                        View.GONE
                );

        recipeContainer
                .setVisibility(
                        View.VISIBLE
                );

        LayoutInflater inflater =
                LayoutInflater.from(this);

        // -----------------------------------------------------
        // Create cards
        // -----------------------------------------------------

        for (
                Recipe recipe :
                recipes
        ) {

            View recipeView =
                    inflater.inflate(
                            R.layout.item_recipe,
                            recipeContainer,
                            false
                    );

            TextView tvRecipeName =
                    recipeView.findViewById(
                            R.id.tvRecipeName
                    );

            TextView tvMatchPercentage =
                    recipeView.findViewById(
                            R.id.tvMatchPercentage
                    );

            TextView tvUseSoon =
                    recipeView.findViewById(
                            R.id.tvUseSoon
                    );

            TextView tvCalories =
                    recipeView.findViewById(
                            R.id.tvCalories
                    );

            TextView tvCookingTime =
                    recipeView.findViewById(
                            R.id.tvCookingTime
                    );

            View btnViewRecipe =
                    recipeView.findViewById(
                            R.id.btnViewRecipe
                    );

            // -------------------------------------------------
            // Recipe name
            // -------------------------------------------------

            tvRecipeName.setText(
                    recipe.name
            );

            // -------------------------------------------------
            // Match
            // -------------------------------------------------

            tvMatchPercentage.setText(
                    recipe.matchPercentage
                            + "% ingredients available"
            );

            // -------------------------------------------------
            // Use Soon
            // -------------------------------------------------

            if (recipe.useSoonCount > 0) {

                tvUseSoon.setText(
                        "⭐ Uses "
                                + recipe.useSoonCount
                                + " ingredient"
                                + (
                                recipe.useSoonCount > 1
                                        ? "s"
                                        : ""
                        )
                                + " expiring soon"
                );

                tvUseSoon.setVisibility(
                        View.VISIBLE
                );

            } else {

                tvUseSoon.setVisibility(
                        View.GONE
                );
            }

            // -------------------------------------------------
            // Calories
            // -------------------------------------------------

            tvCalories.setText(
                    "🔥 "
                            + recipe.calories
                            + " kcal"
            );

            // -------------------------------------------------
            // Cooking time
            // -------------------------------------------------

            if (recipe.cookingTime > 0) {

                tvCookingTime.setText(
                        "⏱ "
                                + recipe.cookingTime
                                + " min"
                );

            } else {

                tvCookingTime.setText(
                        "⏱ Time unavailable"
                );
            }

            // -------------------------------------------------
            // View Recipe
            // -------------------------------------------------

            btnViewRecipe.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                RecipeActivity.this,
                                RecipeDetailsActivity.class
                        );

                // Recipe ID
                intent.putExtra(
                        "recipeId",
                        recipe.id
                );

                // Recipe name
                intent.putExtra(
                        "recipeName",
                        recipe.name
                );

                // Calories
                intent.putExtra(
                        "calories",
                        recipe.calories
                );

                // Cooking time
                intent.putExtra(
                        "cookingTime",
                        recipe.cookingTime
                );

                // Match percentage
                intent.putExtra(
                        "matchPercentage",
                        recipe.matchPercentage
                );

                // Use Soon count
                intent.putExtra(
                        "useSoonCount",
                        recipe.useSoonCount
                );

                // Available ingredients
                intent.putStringArrayListExtra(
                        "availableIngredients",
                        recipe.availableIngredients
                );

                // Missing ingredients
                intent.putStringArrayListExtra(
                        "missingIngredients",
                        recipe.missingIngredients
                );

                startActivity(intent);
            });

            // -------------------------------------------------
            // Add card
            // -------------------------------------------------

            recipeContainer.addView(
                    recipeView
            );
        }
    }

    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase();
    }
}