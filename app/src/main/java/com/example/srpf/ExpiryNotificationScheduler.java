package com.example.srpf;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class ExpiryNotificationScheduler {

    private static final String ACTION_EXPIRY_SOON =
            "com.example.srpf.EXPIRY_SOON";

    private static final String ACTION_EXPIRED =
            "com.example.srpf.EXPIRED";

    /**
     * Schedule both expiry notifications for an ingredient.
     *
     * @param context Application/activity context
     * @param ingredientId Firestore ingredient document ID
     * @param ingredientName Name of the ingredient
     * @param expiryTimeMillis Expiry date/time in milliseconds
     */
    public static void scheduleExpiryNotifications(
            Context context,
            String ingredientId,
            String ingredientName,
            long expiryTimeMillis
    ) {

        // -----------------------------------------
        // 3 DAYS BEFORE EXPIRY
        // -----------------------------------------

        long threeDaysBefore =
                expiryTimeMillis - (3L * 24 * 60 * 60 * 1000);

        if (threeDaysBefore > System.currentTimeMillis()) {

            scheduleNotification(
                    context,
                    ingredientId,
                    ingredientName,
                    threeDaysBefore,
                    ACTION_EXPIRY_SOON,
                    false
            );
        }

        // -----------------------------------------
        // EXPIRY DATE
        // -----------------------------------------

        if (expiryTimeMillis > System.currentTimeMillis()) {

            scheduleNotification(
                    context,
                    ingredientId,
                    ingredientName,
                    expiryTimeMillis,
                    ACTION_EXPIRED,
                    true
            );
        }
    }

    /**
     * Schedule one Android alarm.
     */
    private static void scheduleNotification(
            Context context,
            String ingredientId,
            String ingredientName,
            long triggerTime,
            String action,
            boolean expired
    ) {

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(
                context,
                ExpiryNotificationReceiver.class
        );

        intent.setAction(action);

        intent.putExtra(
                "ingredientId",
                ingredientId
        );

        intent.putExtra(
                "ingredientName",
                ingredientName
        );

        intent.putExtra(
                "expired",
                expired
        );

        int requestCode =
                generateRequestCode(
                        ingredientId,
                        action
                );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        /*
         * Android 12+ may require exact-alarm permission.
         *
         * Your OnePlus 6 is Android 11, so this check
         * will not affect your current testing device.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (!alarmManager.canScheduleExactAlarms()) {
                return;
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
        );
    }

    /**
     * Cancel all scheduled expiry notifications
     * for a particular ingredient.
     */
    public static void cancelExpiryNotifications(
            Context context,
            String ingredientId
    ) {

        cancelNotification(
                context,
                ingredientId,
                ACTION_EXPIRY_SOON
        );

        cancelNotification(
                context,
                ingredientId,
                ACTION_EXPIRED
        );
    }

    /**
     * Cancel one scheduled notification.
     */
    private static void cancelNotification(
            Context context,
            String ingredientId,
            String action
    ) {

        Intent intent = new Intent(
                context,
                ExpiryNotificationReceiver.class
        );

        intent.setAction(action);

        int requestCode =
                generateRequestCode(
                        ingredientId,
                        action
                );

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        pendingIntent.cancel();
    }

    /**
     * Generates a unique request code for:
     *
     * ingredient + notification type
     */
    private static int generateRequestCode(
            String ingredientId,
            String action
    ) {

        return Math.abs(
                (ingredientId + action).hashCode()
        );
    }
}