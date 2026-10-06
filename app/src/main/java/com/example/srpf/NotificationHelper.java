package com.example.srpf;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    public static final String CHANNEL_ID = "expiry_notifications";

    private static final String CHANNEL_NAME = "Expiry Notifications";
    private static final String CHANNEL_DESCRIPTION =
            "Notifications about ingredients that are expiring or expired.";

    /**
     * Creates the notification channel.
     * Required for Android 8.0 and above.
     */
    public static void createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Shows an expiry notification.
     */
    public static void showExpiryNotification(
            Context context,
            String title,
            String message,
            int notificationId
    ) {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(message)
                        );

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(
                        android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            notificationManager.notify(notificationId, builder.build());
        }
    }
}