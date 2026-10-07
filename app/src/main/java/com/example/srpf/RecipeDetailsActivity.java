package com.example.srpf;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class RecipeDetailsActivity extends AppCompatActivity {

    private TextView tvRecipeName;
    private TextView tvDescription;
    private TextView tvCalories;
    private TextView tvCookingTime;
    private TextView tvMatchPercentage;
    private TextView tvMissingCount;

    private LinearLayout ingredientsContainer;
    private Button btnStartCooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);

        tvRecipeName = findViewById(R.id.tvRecipeName);
        tvDescription = findViewById(R.id.tvDescription);
        tvCalories = findViewById(R.id.tvCalories);
        tvCookingTime = findViewById(R.id.tvCookingTime);
        tvMatchPercentage = findViewById(R.id.tvMatchPercentage);
        tvMissingCount = findViewById(R.id.tvMissingCount);
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        btnStartCooking = findViewById(R.id.btnStartCooking);

        TextView tvBack = findViewById(R.id.tvBack);
        tvBack.setOnClickListener(v -> finish());

        String recipeId = getIntent().getStringExtra("recipeId");
        String recipeName = getIntent().getStringExtra("recipeName");
        String description = getIntent().getStringExtra("description");

        int calories = getIntent().getIntExtra("calories", 0);
        int cookingTime = getIntent().getIntExtra("cookingTime", 0);
        int matchPercentage = getIntent().getIntExtra("matchPercentage", 0);
        int useSoonCount = getIntent().getIntExtra("useSoonCount", 0);

        ArrayList<String> availableIngredients =
                getIntent().getStringArrayListExtra("availableIngredients");

        ArrayList<String> missingIngredients =
                getIntent().getStringArrayListExtra("missingIngredients");

        if (recipeName != null && !recipeName.trim().isEmpty()) {
            tvRecipeName.setText(recipeName);
        }

        if (description != null && !description.trim().isEmpty()) {
            tvDescription.setText(description);
        } else {
            tvDescription.setText("Description not available.");
        }

        tvCalories.setText(
                calories > 0
                        ? "🔥 " + calories + " kcal"
                        : "🔥 Calories unavailable"
        );

        tvCookingTime.setText(
                cookingTime > 0
                        ? "⏱ " + cookingTime + " min"
                        : "⏱ Time unavailable"
        );

        String matchText =
                matchPercentage + "% ingredients available";

        if (useSoonCount > 0) {
            matchText +=
                    "\n⭐ Uses "
                            + useSoonCount
                            + " ingredient"
                            + (useSoonCount > 1 ? "s" : "")
                            + " expiring soon";
        }

        tvMatchPercentage.setText(matchText);

        if (availableIngredients != null) {
            for (String ingredient : availableIngredients) {
                addIngredientRow(ingredient, true);
            }
        }

        int missingCount = 0;

        if (missingIngredients != null) {
            missingCount = missingIngredients.size();

            for (String ingredient : missingIngredients) {
                addIngredientRow(ingredient, false);
            }
        }

        tvMissingCount.setText(
                "Missing Ingredients: " + missingCount
        );

        btnStartCooking.setOnClickListener(v -> {
            if (recipeId == null || recipeId.trim().isEmpty()) {
                return;
            }

            Intent intent = new Intent(
                    RecipeDetailsActivity.this,
                    CookingActivity.class
            );

            intent.putExtra("recipeId", recipeId);
            intent.putExtra("recipeName", recipeName);
            intent.putExtra("calories", calories);
            intent.putExtra("cookingTime", cookingTime);

            startActivity(intent);
        });
    }

    private void addIngredientRow(
            String ingredient,
            boolean available
    ) {
        if (ingredient == null || ingredient.trim().isEmpty()) {
            return;
        }

        TextView row = new TextView(this);

        row.setText(
                (available ? "✓  " : "✕  ")
                        + ingredient
        );

        row.setTextSize(15);
        row.setTextColor(
                available
                        ? 0xFF2E7D32
                        : 0xFFD32F2F
        );

        row.setPadding(12, 10, 12, 10);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ingredientsContainer.addView(row);
    }
}
