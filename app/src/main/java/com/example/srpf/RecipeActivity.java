package com.example.srpf;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecipeActivity extends AppCompatActivity {

    private static final String TAG = "SRPF_RECIPE";

    private static final long USE_SOON_DAYS = 3;
    private static final int MAX_RECIPES = 12;

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
    // Background work
    // =========================================================

    private final ExecutorService apiExecutor =
            Executors.newSingleThreadExecutor();

    // =========================================================
    // Pantry
    // =========================================================

    private final List<PantryIngredient> pantryIngredients =
            new ArrayList<>();

    private static class PantryIngredient {

        String name;
        long expiryDate;

        PantryIngredient(String name, long expiryDate) {
            this.name = name;
            this.expiryDate = expiryDate;
        }
    }

    // =========================================================
    // Recipe model
    // =========================================================

    private static class Recipe {

        String id;
        String name;
        String description;

        int calories;
        int cookingTime;
        int matchPercentage;
        int useSoonCount;

        ArrayList<String> availableIngredients;
        ArrayList<String> missingIngredients;

        Recipe(
                String id,
                String name,
                String description,
                int calories,
                int cookingTime,
                int matchPercentage,
                int useSoonCount,
                ArrayList<String> availableIngredients,
                ArrayList<String> missingIngredients
        ) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.calories = calories;
            this.cookingTime = cookingTime;
            this.matchPercentage = matchPercentage;
            this.useSoonCount = useSoonCount;
            this.availableIngredients = availableIngredients;
            this.missingIngredients = missingIngredients;
        }
    }

    // =========================================================
    // onCreate
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etRecipeSearch = findViewById(R.id.etRecipeSearch);
        btnRecipeSearch = findViewById(R.id.btnRecipeSearch);

        recipeContainer = findViewById(R.id.recipeContainer);
        layoutEmptyRecipes = findViewById(R.id.layoutEmptyRecipes);

        navHomeContainer = findViewById(R.id.navHomeContainer);
        navPantryContainer = findViewById(R.id.navPantryContainer);
        navRecipeContainer = findViewById(R.id.navRecipeContainer);

        btnRecipeSearch.setOnClickListener(v -> {
            String query = etRecipeSearch.getText().toString().trim();

            if (query.isEmpty()) {
                loadRecipesFromPantry();
            } else {
                searchRecipes(query);
            }
        });

        navHomeContainer.setOnClickListener(v -> {
            startActivity(new Intent(RecipeActivity.this, HomeActivity.class));
            finish();
        });

        navPantryContainer.setOnClickListener(v -> {
            startActivity(new Intent(RecipeActivity.this, PantryActivity.class));
            finish();
        });

        navRecipeContainer.setOnClickListener(v -> {
            // Already on Recipe page.
        });

        loadPantryIngredients();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        apiExecutor.shutdownNow();
    }

    // =========================================================
    // LOAD PANTRY INGREDIENTS
    // =========================================================

    private void loadPantryIngredients() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        firestore
                .collection("users")
                .document(user.getUid())
                .collection("ingredients")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    pantryIngredients.clear();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String name = document.getString("name");
                        Long expiryDate = document.getLong("expiryDate");

                        if (name == null || name.trim().isEmpty()) {
                            continue;
                        }

                        pantryIngredients.add(
                                new PantryIngredient(
                                        name.trim(),
                                        expiryDate != null ? expiryDate : 0L
                                )
                        );
                    }

                    if (pantryIngredients.isEmpty()) {
                        showEmptyState("Add ingredients to your pantry first.");
                        return;
                    }

                    // Automatically load recipes using the current pantry.
                    loadRecipesFromPantry();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load pantry", e);
                    showEmptyState("Unable to load your pantry.");
                    Toast.makeText(
                            RecipeActivity.this,
                            "Unable to load pantry ingredients",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================================================
    // AUTOMATIC RECOMMENDATIONS
    // =========================================================

    private void loadRecipesFromPantry() {
        if (pantryIngredients.isEmpty()) {
            showEmptyState("Add ingredients to your pantry first.");
            return;
        }

        if (!hasApiKey()) {
            showApiKeyError();
            return;
        }

        setLoadingState();

        String ingredientList = buildIngredientList();

        apiExecutor.execute(() -> {
            try {
                String endpoint =
                        SpoonacularConfig.BASE_URL
                                + "/recipes/findByIngredients"
                                + "?ingredients="
                                + URLEncoder.encode(
                                ingredientList,
                                "UTF-8"
                        )
                                + "&number="
                                + MAX_RECIPES
                                + "&ranking=2"
                                + "&ignorePantry=true";

                String searchResponse = httpGet(endpoint);
                JSONArray searchResults = new JSONArray(searchResponse);

                if (searchResults.length() == 0) {
                    runOnUiThread(() ->
                            showEmptyState(
                                    "No recipes found for your pantry ingredients."
                            )
                    );
                    return;
                }

                List<Recipe> recipes =
                        buildRecipesFromIngredientResults(searchResults);

                // One bulk request supplies description, calories and time.
                loadRecipeDetailsBulk(recipes);

            } catch (Exception e) {
                Log.e(TAG, "Spoonacular pantry search failed", e);
                runOnUiThread(() -> showApiError(e));
            }
        });
    }

    // =========================================================
    // TEXT SEARCH
    // =========================================================

    private void searchRecipes(String query) {
        if (pantryIngredients.isEmpty()) {
            showEmptyState("Add ingredients to your pantry first.");
            return;
        }

        if (!hasApiKey()) {
            showApiKeyError();
            return;
        }

        setLoadingState();

        String ingredientList = buildIngredientList();

        apiExecutor.execute(() -> {
            try {
                String endpoint =
                        SpoonacularConfig.BASE_URL
                                + "/recipes/complexSearch"
                                + "?query="
                                + URLEncoder.encode(query, "UTF-8")
                                + "&includeIngredients="
                                + URLEncoder.encode(
                                ingredientList,
                                "UTF-8"
                        )
                                + "&fillIngredients=true"
                                + "&addRecipeInformation=true"
                                + "&includeNutrition=true"
                                + "&number="
                                + MAX_RECIPES
                                + "&ranking=2"
                                + "&ignorePantry=true";

                String response = httpGet(endpoint);
                JSONObject root = new JSONObject(response);
                JSONArray results = root.optJSONArray("results");

                if (results == null || results.length() == 0) {
                    runOnUiThread(() ->
                            showEmptyState(
                                    "No recipes matched your search and pantry."
                            )
                    );
                    return;
                }

                List<Recipe> recipes =
                        buildRecipesFromComplexSearch(results);

                sortRecipes(recipes);

                runOnUiThread(() -> displayRecipes(recipes));

            } catch (Exception e) {
                Log.e(TAG, "Spoonacular recipe search failed", e);
                runOnUiThread(() -> showApiError(e));
            }
        });
    }

    // =========================================================
    // BUILD RECIPES FROM findByIngredients
    // =========================================================

    private List<Recipe> buildRecipesFromIngredientResults(
            JSONArray results
    ) {
        List<Recipe> recipes = new ArrayList<>();
        Set<String> useSoon = getUseSoonIngredients();

        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.optJSONObject(i);

            if (item == null) {
                continue;
            }

            String id = String.valueOf(item.optInt("id", 0));
            String title = item.optString("title", "Recipe");

            JSONArray used = item.optJSONArray("usedIngredients");
            JSONArray missed = item.optJSONArray("missedIngredients");

            ArrayList<String> available = new ArrayList<>();
            ArrayList<String> missing = new ArrayList<>();

            int useSoonCount = 0;

            if (used != null) {
                for (int j = 0; j < used.length(); j++) {
                    JSONObject ingredient = used.optJSONObject(j);

                    if (ingredient == null) {
                        continue;
                    }

                    String name = ingredient.optString(
                            "name",
                            ingredient.optString("original", "")
                    );

                    if (!name.isEmpty()) {
                        available.add(name);

                        if (matchesAnyPantryName(name, useSoon)) {
                            useSoonCount++;
                        }
                    }
                }
            }

            if (missed != null) {
                for (int j = 0; j < missed.length(); j++) {
                    JSONObject ingredient = missed.optJSONObject(j);

                    if (ingredient == null) {
                        continue;
                    }

                    String name = ingredient.optString(
                            "name",
                            ingredient.optString("original", "")
                    );

                    if (!name.isEmpty()) {
                        missing.add(name);
                    }
                }
            }

            int total = available.size() + missing.size();
            int matchPercentage = total > 0
                    ? (available.size() * 100) / total
                    : 0;

            recipes.add(
                    new Recipe(
                            id,
                            title,
                            "",
                            0,
                            0,
                            matchPercentage,
                            useSoonCount,
                            available,
                            missing
                    )
            );
        }

        return recipes;
    }

    // =========================================================
    // BUILD RECIPES FROM complexSearch
    // =========================================================

    private List<Recipe> buildRecipesFromComplexSearch(
            JSONArray results
    ) {
        List<Recipe> recipes = new ArrayList<>();
        Set<String> useSoon = getUseSoonIngredients();

        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.optJSONObject(i);

            if (item == null) {
                continue;
            }

            String id = String.valueOf(item.optInt("id", 0));
            String title = item.optString("title", "Recipe");

            int usedCount = item.optInt("usedIngredientCount", 0);
            int missedCount = item.optInt("missedIngredientCount", 0);
            int total = usedCount + missedCount;

            int matchPercentage = total > 0
                    ? (usedCount * 100) / total
                    : 0;

            ArrayList<String> available =
                    parseIngredientNames(item.optJSONArray("usedIngredients"));

            ArrayList<String> missing =
                    parseIngredientNames(item.optJSONArray("missedIngredients"));

            int useSoonCount = 0;
            for (String ingredient : available) {
                if (matchesAnyPantryName(ingredient, useSoon)) {
                    useSoonCount++;
                }
            }

            String description =
                    cleanHtml(item.optString("summary", ""));

            int cookingTime =
                    item.optInt("readyInMinutes", 0);

            int calories =
                    extractCalories(item.optJSONObject("nutrition"));

            recipes.add(
                    new Recipe(
                            id,
                            title,
                            description,
                            calories,
                            cookingTime,
                            matchPercentage,
                            useSoonCount,
                            available,
                            missing
                    )
            );
        }

        return recipes;
    }

    // =========================================================
    // BULK RECIPE DETAILS
    // =========================================================

    private void loadRecipeDetailsBulk(List<Recipe> recipes) {
        if (recipes.isEmpty()) {
            runOnUiThread(() ->
                    showEmptyState("No recipes found for your pantry.")
            );
            return;
        }

        StringBuilder ids = new StringBuilder();

        for (Recipe recipe : recipes) {
            if (ids.length() > 0) {
                ids.append(",");
            }
            ids.append(recipe.id);
        }

        apiExecutor.execute(() -> {
            try {
                String endpoint =
                        SpoonacularConfig.BASE_URL
                                + "/recipes/informationBulk"
                                + "?ids="
                                + URLEncoder.encode(ids.toString(), "UTF-8")
                                + "&includeNutrition=true";

                String response = httpGet(endpoint);
                JSONArray details = new JSONArray(response);

                // Map the details back to the recipes returned by findByIngredients.
                for (int i = 0; i < details.length(); i++) {
                    JSONObject detail = details.optJSONObject(i);

                    if (detail == null) {
                        continue;
                    }

                    String id = String.valueOf(detail.optInt("id", 0));
                    Recipe recipe = findRecipeById(recipes, id);

                    if (recipe == null) {
                        continue;
                    }

                    recipe.name = detail.optString("title", recipe.name);
                    recipe.description =
                            cleanHtml(detail.optString("summary", ""));
                    recipe.cookingTime =
                            detail.optInt("readyInMinutes", 0);
                    recipe.calories =
                            extractCalories(detail.optJSONObject("nutrition"));
                }

                sortRecipes(recipes);

                runOnUiThread(() -> displayRecipes(recipes));

            } catch (Exception e) {
                Log.e(TAG, "Spoonacular bulk details failed", e);

                // We still have useful recipe matches even if details fail.
                sortRecipes(recipes);
                runOnUiThread(() -> displayRecipes(recipes));
            }
        });
    }

    // =========================================================
    // SORTING
    // =========================================================

    private void sortRecipes(List<Recipe> recipes) {
        Collections.sort(
                recipes,
                (r1, r2) -> {
                    // 1. Recipes using more expiring-soon ingredients first.
                    if (r1.useSoonCount != r2.useSoonCount) {
                        return Integer.compare(
                                r2.useSoonCount,
                                r1.useSoonCount
                        );
                    }

                    // 2. Higher pantry match next.
                    if (r1.matchPercentage != r2.matchPercentage) {
                        return Integer.compare(
                                r2.matchPercentage,
                                r1.matchPercentage
                        );
                    }

                    // 3. More available ingredients next.
                    return Integer.compare(
                            r2.availableIngredients.size(),
                            r1.availableIngredients.size()
                    );
                }
        );
    }

    // =========================================================
    // DISPLAY
    // =========================================================

    private void displayRecipes(List<Recipe> recipes) {
        recipeContainer.removeAllViews();

        if (recipes == null || recipes.isEmpty()) {
            showEmptyState("No recipes found for your pantry.");
            return;
        }

        layoutEmptyRecipes.setVisibility(View.GONE);
        recipeContainer.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (Recipe recipe : recipes) {
            View recipeView = inflater.inflate(
                    R.layout.item_recipe,
                    recipeContainer,
                    false
            );

            TextView tvRecipeName =
                    recipeView.findViewById(R.id.tvRecipeName);
            TextView tvMatchPercentage =
                    recipeView.findViewById(R.id.tvMatchPercentage);
            TextView tvUseSoon =
                    recipeView.findViewById(R.id.tvUseSoon);
            TextView tvCalories =
                    recipeView.findViewById(R.id.tvCalories);
            TextView tvCookingTime =
                    recipeView.findViewById(R.id.tvCookingTime);
            View btnViewRecipe =
                    recipeView.findViewById(R.id.btnViewRecipe);

            tvRecipeName.setText(recipe.name);
            tvMatchPercentage.setText(
                    recipe.matchPercentage + "% ingredients available"
            );

            if (recipe.useSoonCount > 0) {
                tvUseSoon.setText(
                        "⭐ Uses "
                                + recipe.useSoonCount
                                + " ingredient"
                                + (recipe.useSoonCount > 1 ? "s" : "")
                                + " expiring soon"
                );
                tvUseSoon.setVisibility(View.VISIBLE);
            } else {
                tvUseSoon.setVisibility(View.GONE);
            }

            tvCalories.setText(
                    recipe.calories > 0
                            ? "🔥 " + recipe.calories + " kcal"
                            : "🔥 Calories unavailable"
            );

            tvCookingTime.setText(
                    recipe.cookingTime > 0
                            ? "⏱ " + recipe.cookingTime + " min"
                            : "⏱ Time unavailable"
            );

            btnViewRecipe.setOnClickListener(v -> openRecipeDetails(recipe));

            recipeContainer.addView(recipeView);
        }
    }

    private void openRecipeDetails(Recipe recipe) {
        Intent intent = new Intent(
                RecipeActivity.this,
                RecipeDetailsActivity.class
        );

        intent.putExtra("recipeId", recipe.id);
        intent.putExtra("recipeName", recipe.name);
        intent.putExtra("description", recipe.description);
        intent.putExtra("calories", recipe.calories);
        intent.putExtra("cookingTime", recipe.cookingTime);
        intent.putExtra("matchPercentage", recipe.matchPercentage);
        intent.putExtra("useSoonCount", recipe.useSoonCount);

        intent.putStringArrayListExtra(
                "availableIngredients",
                recipe.availableIngredients
        );

        intent.putStringArrayListExtra(
                "missingIngredients",
                recipe.missingIngredients
        );

        startActivity(intent);
    }

    // =========================================================
    // API HELPERS
    // =========================================================

    private String buildIngredientList() {
        StringBuilder builder = new StringBuilder();

        for (PantryIngredient ingredient : pantryIngredients) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(ingredient.name);
        }

        return builder.toString();
    }

    private String httpGet(String endpoint) throws Exception {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty(
                    "x-api-key",
                    SpoonacularConfig.API_KEY
            );

            int statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = readStream(stream);

            if (statusCode < 200 || statusCode >= 300) {
                throw new Exception(
                        "Spoonacular HTTP "
                                + statusCode
                                + ": "
                                + response
                );
            }

            return response;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        stream,
                        StandardCharsets.UTF_8
                )
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }

    // =========================================================
    // JSON HELPERS
    // =========================================================

    private ArrayList<String> parseIngredientNames(JSONArray array) {
        ArrayList<String> names = new ArrayList<>();

        if (array == null) {
            return names;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);

            if (item == null) {
                continue;
            }

            String name = item.optString(
                    "name",
                    item.optString("original", "")
            );

            if (!name.trim().isEmpty()) {
                names.add(name.trim());
            }
        }

        return names;
    }

    private int extractCalories(JSONObject nutrition) {
        if (nutrition == null) {
            return 0;
        }

        JSONArray nutrients = nutrition.optJSONArray("nutrients");

        if (nutrients == null) {
            return 0;
        }

        for (int i = 0; i < nutrients.length(); i++) {
            JSONObject nutrient = nutrients.optJSONObject(i);

            if (nutrient == null) {
                continue;
            }

            if ("Calories".equalsIgnoreCase(
                    nutrient.optString("name", "")
            )) {
                return (int) Math.round(
                        nutrient.optDouble("amount", 0.0)
                );
            }
        }

        return 0;
    }

    private Recipe findRecipeById(
            List<Recipe> recipes,
            String id
    ) {
        for (Recipe recipe : recipes) {
            if (recipe.id.equals(id)) {
                return recipe;
            }
        }

        return null;
    }

    private String cleanHtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "Description not available.";
        }

        return Html.fromHtml(
                        html,
                        Html.FROM_HTML_MODE_LEGACY
                )
                .toString()
                .replace("\n\n", "\n")
                .trim();
    }

    // =========================================================
    // EXPIRING-SOON MATCHING
    // =========================================================

    private Set<String> getUseSoonIngredients() {
        Set<String> result = new HashSet<>();

        long now = System.currentTimeMillis();
        long threeDaysFromNow =
                now + (USE_SOON_DAYS * 24L * 60L * 60L * 1000L);

        for (PantryIngredient ingredient : pantryIngredients) {
            if (ingredient.expiryDate <= 0) {
                continue;
            }

            if (
                    ingredient.expiryDate >= now
                            && ingredient.expiryDate <= threeDaysFromNow
            ) {
                result.add(normalize(ingredient.name));
            }
        }

        return result;
    }

    private boolean matchesAnyPantryName(
            String recipeIngredient,
            Set<String> pantryNames
    ) {
        String recipeName = normalize(recipeIngredient);

        for (String pantryName : pantryNames) {
            if (ingredientsMatch(recipeName, pantryName)) {
                return true;
            }
        }

        return false;
    }

    private boolean ingredientsMatch(
            String first,
            String second
    ) {
        if (first.equals(second)) {
            return true;
        }

        return first.contains(second) || second.contains(first);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toLowerCase(Locale.getDefault())
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // =========================================================
    // UI STATES / ERRORS
    // =========================================================

    private void setLoadingState() {
        layoutEmptyRecipes.setVisibility(View.VISIBLE);
        recipeContainer.setVisibility(View.GONE);

        TextView message =
                layoutEmptyRecipes.findViewById(R.id.tvRecipeEmptyMessage);

        if (message != null) {
            message.setText("Finding recipes from your pantry...");
        }
    }

    private void showEmptyState(String message) {
        recipeContainer.removeAllViews();
        recipeContainer.setVisibility(View.GONE);
        layoutEmptyRecipes.setVisibility(View.VISIBLE);

        TextView messageView =
                layoutEmptyRecipes.findViewById(R.id.tvRecipeEmptyMessage);

        if (messageView != null) {
            messageView.setText(message);
        }
    }

    private void showApiKeyError() {
        showEmptyState(
                "Add your Spoonacular API key in RecipeActivity.java to load recipes."
        );
    }

    private void showApiError(Exception e) {
        String message = e.getMessage();

        if (message == null || message.trim().isEmpty()) {
            message = "Unable to load recipes right now.";
        }

        showEmptyState(message);

        Toast.makeText(
                RecipeActivity.this,
                "Spoonacular request failed",
                Toast.LENGTH_LONG
        ).show();
    }

    private boolean hasApiKey() {
        return SpoonacularConfig.API_KEY != null
                && !SpoonacularConfig.API_KEY.trim().isEmpty()
                && !SpoonacularConfig.API_KEY.startsWith("YOUR_");
    }

    private void goToLogin() {
        Intent intent = new Intent(
                RecipeActivity.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
