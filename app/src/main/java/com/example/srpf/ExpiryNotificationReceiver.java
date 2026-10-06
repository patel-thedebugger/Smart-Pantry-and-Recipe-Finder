package com.example.srpf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExpiryNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        String ingredientName =
                intent.getStringExtra("ingredientName");

        boolean expired =
                intent.getBooleanExtra(
                        "expired",
                        false
                );

        if (ingredientName == null ||
                ingredientName.trim().isEmpty()) {

            ingredientName = "Ingredient";
        }

        if (expired) {

            NotificationHelper.showExpiryNotification(
                    context,
                    "Ingredient Expired",
                    ingredientName + " has expired. "
                            + "Please check your pantry.",
                    generateNotificationId(
                            ingredientName,
                            "expired"
                    )
            );

        } else {

            NotificationHelper.showExpiryNotification(
                    context,
                    "Ingredient Expiring Soon",
                    ingredientName + " is expiring soon. "
                            + "Consider using it in a recipe.",
                    generateNotificationId(
                            ingredientName,
                            "soon"
                    )
            );
        }
    }

    private int generateNotificationId(
            String ingredientName,
            String type
    ) {

        return Math.abs(
                (ingredientName + type).hashCode()
        );
    }
}