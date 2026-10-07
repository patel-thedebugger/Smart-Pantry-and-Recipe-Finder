package com.example.srpf;

import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CookingActivity extends AppCompatActivity {

    private TextView tvCookingRecipeName;
    private TextView tvCookingCalories;
    private TextView tvCookingTime;
    private LinearLayout instructionsContainer;
    private Button btnFinishCooking;

    private final ExecutorService apiExecutor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking);

        tvCookingRecipeName = findViewById(R.id.tvCookingRecipeName);
        tvCookingCalories = findViewById(R.id.tvCookingCalories);
        tvCookingTime = findViewById(R.id.tvCookingTime);
        instructionsContainer = findViewById(R.id.instructionsContainer);
        btnFinishCooking = findViewById(R.id.btnFinishCooking);

        TextView tvBackCooking = findViewById(R.id.tvBackCooking);
        tvBackCooking.setOnClickListener(v -> finish());

        String recipeId = getIntent().getStringExtra("recipeId");
        String recipeName = getIntent().getStringExtra("recipeName");
        int calories = getIntent().getIntExtra("calories", 0);
        int cookingTime = getIntent().getIntExtra("cookingTime", 0);

        if (recipeName != null) {
            tvCookingRecipeName.setText(recipeName);
        }

        tvCookingCalories.setText(
                calories > 0
                        ? "🔥 " + calories + " kcal"
                        : "🔥 Calories unavailable"
        );

        tvCookingTime.setText(
                cookingTime > 0
                        ? "⏱ " + cookingTime + " min"
                        : "⏱ Time unavailable"
        );

        if (recipeId == null || recipeId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Recipe information unavailable",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        loadInstructions(recipeId);

        btnFinishCooking.setOnClickListener(v -> {
            Toast.makeText(
                    CookingActivity.this,
                    "Recipe completed! 🎉",
                    Toast.LENGTH_SHORT
            ).show();
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        apiExecutor.shutdownNow();
    }

    private void loadInstructions(String recipeId) {
        if (SpoonacularConfig.API_KEY.startsWith("YOUR_")) {
            showInstructionMessage(
                    "Add your Spoonacular API key in CookingActivity.java."
            );
            return;
        }

        apiExecutor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                        SpoonacularConfig.BASE_URL
                                + "/recipes/"
                                + recipeId
                                + "/information?includeNutrition=false"
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty(
                        "x-api-key",
                        SpoonacularConfig.API_KEY
                );

                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String response = readStream(stream);

                if (status < 200 || status >= 300) {
                    throw new Exception(
                            "Spoonacular HTTP " + status
                    );
                }

                JSONObject recipe = new JSONObject(response);
                JSONArray analyzedInstructions =
                        recipe.optJSONArray("analyzedInstructions");

                if (analyzedInstructions == null
                        || analyzedInstructions.length() == 0) {
                    runOnUiThread(() ->
                            showInstructionMessage(
                                    "No cooking instructions available."
                            )
                    );
                    return;
                }

                JSONObject firstInstructionSet =
                        analyzedInstructions.optJSONObject(0);

                JSONArray steps = firstInstructionSet != null
                        ? firstInstructionSet.optJSONArray("steps")
                        : null;

                if (steps == null || steps.length() == 0) {
                    runOnUiThread(() ->
                            showInstructionMessage(
                                    "No cooking instructions available."
                            )
                    );
                    return;
                }

                runOnUiThread(() -> displayInstructions(steps));

            } catch (Exception e) {
                runOnUiThread(() ->
                        showInstructionMessage(
                                "Unable to load cooking instructions."
                        )
                );
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void displayInstructions(JSONArray steps) {
        instructionsContainer.removeAllViews();

        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.optJSONObject(i);

            if (step == null) {
                continue;
            }

            String instruction = step.optString("step", "").trim();

            if (!instruction.isEmpty()) {
                addInstructionStep(i + 1, instruction);
            }
        }
    }

    private void addInstructionStep(
            int stepNumber,
            String instruction
    ) {
        LinearLayout stepLayout = new LinearLayout(this);
        stepLayout.setOrientation(LinearLayout.HORIZONTAL);
        stepLayout.setGravity(Gravity.CENTER_VERTICAL);
        stepLayout.setPadding(0, 8, 0, 8);

        TextView numberText = new TextView(this);
        numberText.setText(String.valueOf(stepNumber));
        numberText.setTextSize(16);
        numberText.setTextColor(0xFFFFFFFF);
        numberText.setGravity(Gravity.CENTER);
        numberText.setBackgroundColor(0xFF4CAF50);
        numberText.setLayoutParams(
                new LinearLayout.LayoutParams(42, 42)
        );

        TextView instructionText = new TextView(this);
        instructionText.setText(
                Html.fromHtml(
                        instruction,
                        Html.FROM_HTML_MODE_LEGACY
                ).toString()
        );
        instructionText.setTextSize(16);
        instructionText.setTextColor(0xFF000000);
        instructionText.setPadding(16, 4, 0, 4);
        instructionText.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        stepLayout.addView(numberText);
        stepLayout.addView(instructionText);
        instructionsContainer.addView(stepLayout);
    }

    private void showInstructionMessage(String message) {
        instructionsContainer.removeAllViews();

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(16);
        text.setTextColor(0xFF777777);
        text.setPadding(0, 20, 0, 20);

        instructionsContainer.addView(text);
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
}
