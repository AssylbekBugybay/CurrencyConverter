package com.example.currencyconverter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

// The notification class, provides notifications to the user after the rate update is completed
public class RatesUpdateNotifier {

    private static final int NOTIFICATION_ID = 10;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    // A constructor, context is reference to the activity associated
    public RatesUpdateNotifier(Context context) {
        // Create Manager
        notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel("rates_update_channel");
            if(notificationChannel == null) {
                notificationChannel = new NotificationChannel("rates_update_channel", "Show rates update state",
                        NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        // Create notificationBuilder, not directly show the notification
        notificationBuilder = new NotificationCompat.Builder(context, "currency_converter_channel")
                .setSmallIcon(R.drawable.rates_update)
                .setContentTitle("Updated Currencies!")
                .setContentText("Everything fresh...")
                .setAutoCancel(false);

        Intent resultIntent = new Intent(context, MainActivity.class);
        // if back button is clicked, it returns to home screen, not to the previous views
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // Intent will be provided to notification for usage when needed in the future
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context,
                0, resultIntent, 0);
        notificationBuilder.setContentIntent(resultPendingIntent);
        
        notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void showOrUpdateNotification() {
        notificationManager.notify(10, notificationBuilder.build());
    }

    public void removeNotification() {
        notificationManager.cancel(10);
    }
}

