package com.example.srpf;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CookingActivity extends AppCompatActivity {

    private TextView tvCookingRecipeName;
    private TextView tvCookingCalories;
    private TextView tvCookingTime;

    private LinearLayout instructionsContainer;

    private Button btnFinishCooking;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cooking);

        // --------------------------------------------------
        // Firebase
        // --------------------------------------------------

        firebaseAuth =
                FirebaseAuth.getInstance();

        firestore =
                FirebaseFirestore.getInstance();

        // --------------------------------------------------
        // Find Views
        // --------------------------------------------------

        tvCookingRecipeName =
                findViewById(R.id.tvCookingRecipeName);

        tvCookingCalories =
                findViewById(R.id.tvCookingCalories);

        tvCookingTime =
                findViewById(R.id.tvCookingTime);

        instructionsContainer =
                findViewById(R.id.instructionsContainer);

        btnFinishCooking =
                findViewById(R.id.btnFinishCooking);

        TextView tvBackCooking =
                findViewById(R.id.tvBackCooking);

        // --------------------------------------------------
        // Back button
        // --------------------------------------------------

        tvBackCooking.setOnClickListener(v ->
                finish()
        );

        // --------------------------------------------------
        // Get recipe information
        // --------------------------------------------------

        String recipeId =
                getIntent().getStringExtra(
                        "recipeId"
                );

        String recipeName =
                getIntent().getStringExtra(
                        "recipeName"
                );

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

        // --------------------------------------------------
        // Display recipe information
        // --------------------------------------------------

        if (recipeName != null) {

            tvCookingRecipeName.setText(
                    recipeName
            );
        }

        tvCookingCalories.setText(
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

        // --------------------------------------------------
        // Check Recipe ID
        // --------------------------------------------------

        if (
                recipeId == null ||
                        recipeId.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "Recipe information unavailable",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // --------------------------------------------------
        // Load cooking instructions
        // --------------------------------------------------

        loadInstructions(recipeId);

        // --------------------------------------------------
        // Finish Cooking
        // --------------------------------------------------

        btnFinishCooking.setOnClickListener(v -> {

            Toast.makeText(
                    CookingActivity.this,
                    "Recipe completed! 🎉",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        });
    }

    // ======================================================
    // LOAD INSTRUCTIONS
    // ======================================================

    private void loadInstructions(
            String recipeId
    ) {

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String userId =
                firebaseAuth
                        .getCurrentUser()
                        .getUid();

        firestore
                .collection("users")
                .document(userId)
                .collection("recipes")
                .document(recipeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {

                        Toast.makeText(
                                CookingActivity.this,
                                "Recipe not found",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    List<String> instructions =
                            (List<String>)
                                    documentSnapshot.get(
                                            "instructions"
                                    );

                    instructionsContainer
                            .removeAllViews();

                    // --------------------------------------------------
                    // No instructions
                    // --------------------------------------------------

                    if (
                            instructions == null ||
                                    instructions.isEmpty()
                    ) {

                        TextView emptyText =
                                new TextView(
                                        CookingActivity.this
                                );

                        emptyText.setText(
                                "No cooking instructions available."
                        );

                        emptyText.setTextSize(16);

                        emptyText.setTextColor(
                                getResources().getColor(
                                        android.R.color.darker_gray
                                )
                        );

                        instructionsContainer.addView(
                                emptyText
                        );

                        return;
                    }

                    // --------------------------------------------------
                    // Display instructions
                    // --------------------------------------------------

                    for (
                            int i = 0;
                            i < instructions.size();
                            i++
                    ) {

                        addInstructionStep(
                                i + 1,
                                instructions.get(i)
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            CookingActivity.this,
                            "Unable to load instructions",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // ======================================================
    // ADD INSTRUCTION STEP
    // ======================================================

    private void addInstructionStep(
            int stepNumber,
            String instruction
    ) {

        LinearLayout stepLayout =
                new LinearLayout(this);

        stepLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        stepLayout.setGravity(
                Gravity.CENTER_VERTICAL
        );

        stepLayout.setPadding(
                0,
                8,
                0,
                8
        );

        // --------------------------------------------------
        // Step Number
        // --------------------------------------------------

        TextView numberText =
                new TextView(this);

        numberText.setText(
                String.valueOf(stepNumber)
        );

        numberText.setTextSize(16);

        numberText.setTextColor(
                getResources().getColor(
                        android.R.color.white
                )
        );

        numberText.setGravity(
                Gravity.CENTER
        );

        numberText.setBackgroundColor(
                android.graphics.Color.rgb(
                        76,
                        175,
                        80
                )
        );

        LinearLayout.LayoutParams
                numberParams =
                new LinearLayout.LayoutParams(
                        42,
                        42
                );

        numberText.setLayoutParams(
                numberParams
        );

        // --------------------------------------------------
        // Instruction Text
        // --------------------------------------------------

        TextView instructionText =
                new TextView(this);

        instructionText.setText(
                instruction
        );

        instructionText.setTextSize(16);

        instructionText.setTextColor(
                getResources().getColor(
                        android.R.color.black
                )
        );

        instructionText.setPadding(
                16,
                4,
                0,
                4
        );

        LinearLayout.LayoutParams
                instructionParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        instructionText.setLayoutParams(
                instructionParams
        );

        // --------------------------------------------------
        // Add to step layout
        // --------------------------------------------------

        stepLayout.addView(
                numberText
        );

        stepLayout.addView(
                instructionText
        );

        // --------------------------------------------------
        // Add step to container
        // --------------------------------------------------

        instructionsContainer.addView(
                stepLayout
        );
    }
}