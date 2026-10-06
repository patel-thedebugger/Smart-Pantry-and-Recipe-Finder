package com.example.srpf;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class RecipeDetailsActivity extends AppCompatActivity {

    private TextView tvRecipeName;
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

        // --------------------------------------------------
        // Find views
        // --------------------------------------------------

        tvRecipeName =
                findViewById(R.id.tvRecipeName);

        tvCalories =
                findViewById(R.id.tvCalories);

        tvCookingTime =
                findViewById(R.id.tvCookingTime);

        tvMatchPercentage =
                findViewById(R.id.tvMatchPercentage);

        tvMissingCount =
                findViewById(R.id.tvMissingCount);

        ingredientsContainer =
                findViewById(R.id.ingredientsContainer);

        btnStartCooking =
                findViewById(R.id.btnStartCooking);

        TextView tvBack = findViewById(R.id.tvBack);

        tvBack.setOnClickListener(v -> finish());
        // --------------------------------------------------
        // Get recipe information
        // --------------------------------------------------

        String recipeName =
                getIntent().getStringExtra("recipeName");

        int calories =
                getIntent().getIntExtra(
                        "calories",
                        0
                );

        int cookingTime =
                getIntent().getIntExtra(
                        "cookingTime",
                        0
                );

        int matchPercentage =
                getIntent().getIntExtra(
                        "matchPercentage",
                        0
                );

        ArrayList<String> availableIngredients =
                getIntent().getStringArrayListExtra(
                        "availableIngredients"
                );

        ArrayList<String> missingIngredients =
                getIntent().getStringArrayListExtra(
                        "missingIngredients"
                );

        // --------------------------------------------------
        // Set recipe information
        // --------------------------------------------------

        if (recipeName != null) {

            tvRecipeName.setText(recipeName);
        }

        tvCalories.setText(
                "🔥 " + calories + " kcal"
        );

        if (cookingTime > 0) {

            tvCookingTime.setText(
                    "⏱ " + cookingTime + " min"
            );

        } else {

            tvCookingTime.setText(
                    "⏱ Time unavailable"
            );
        }

        tvMatchPercentage.setText(
                matchPercentage
                        + "% ingredients available"
        );

        // --------------------------------------------------
        // Display available ingredients
        // --------------------------------------------------

        if (availableIngredients != null) {

            for (String ingredient :
                    availableIngredients) {

                addIngredientRow(
                        ingredient,
                        true
                );
            }
        }

        // --------------------------------------------------
        // Display missing ingredients
        // --------------------------------------------------

        int missingCount = 0;

        if (missingIngredients != null) {

            missingCount =
                    missingIngredients.size();

            for (String ingredient :
                    missingIngredients) {

                addIngredientRow(
                        ingredient,
                        false
                );
            }
        }

        tvMissingCount.setText(
                "Missing Ingredients: "
                        + missingCount
        );

        // --------------------------------------------------
        // Start Cooking
        // --------------------------------------------------

        btnStartCooking.setOnClickListener(v -> {

            String recipeId =
                    getIntent().getStringExtra(
                            "recipeId"
                    );

            Intent intent =
                    new Intent(
                            RecipeDetailsActivity.this,
                            CookingActivity.class
                    );

            intent.putExtra(
                    "recipeId",
                    recipeId
            );

            intent.putExtra(
                    "recipeName",
                    recipeName
            );

            intent.putExtra(
                    "calories",
                    calories
            );

            intent.putExtra(
                    "cookingTime",
                    cookingTime
            );

            startActivity(intent);
        });
    }

    // ------------------------------------------------------
    // Add ingredient row
    // ------------------------------------------------------

    private void addIngredientRow(
            String ingredient,
            boolean available
    ) {

        TextView textView =
                new TextView(this);

        String prefix;

        if (available) {

            prefix = "✓ ";

        } else {

            prefix = "✗ ";
        }

        textView.setText(
                prefix + ingredient
        );

        textView.setTextSize(16);

        textView.setPadding(
                0,
                8,
                0,
                8
        );

        if (available) {

            textView.setTextColor(
                    getResources().getColor(
                            android.R.color.holo_green_dark
                    )
            );

        } else {

            textView.setTextColor(
                    getResources().getColor(
                            android.R.color.holo_red_dark
                    )
            );
        }

        ingredientsContainer.addView(
                textView
        );
    }
}