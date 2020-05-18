package com.quarks.android.Utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.quarks.android.ChatActivity;
import com.quarks.android.MainActivity;
import com.quarks.android.R;

import java.util.Random;

public class FCM extends FirebaseMessagingService {
    private static String CHANNEL_ID = "messageNotification";
    private static String NAME = "new";

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Preferences.setFcmToken(getApplicationContext(), s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            // Get data payload
            String userId = remoteMessage.getData().get("userId");
            String username = remoteMessage.getData().get("username");
            String message = remoteMessage.getData().get("message");
            String urlPhoto = remoteMessage.getData().get("urlPhoto");
            String filename = remoteMessage.getData().get("filename");

            // Create notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Mandatory to create an android channel superior to Oreo
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, NAME, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }
            builder.setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(username)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent(userId, username)) // Set the intent that will fire when the user taps the notification
                    .setAutoCancel(true); // Automatically remove notification when user tap on it.

            // Launch notification
            Random random = new Random();
            int notificationId = random.nextInt(8000); // We create a random number each time so as not to delete the previous notification
//            notificationManager.notify(notificationId, builder.build());
            notificationManager.notify(1, builder.build());
        }

//        // Check if message contains a notification payload.
//        if (remoteMessage.getNotification() != null) {
//            Log.d("FCM", "Message Notification Body: " + remoteMessage.getNotification().getBody());
//        }
    }
    public PendingIntent pendingIntent(String userId, String username){
        // We open two activities. MainActivity first and then ChatActivity
        Intent intent1= new Intent(this, MainActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Intent intent2= new Intent(this, ChatActivity.class);
        intent2.putExtra("receiverId", userId);
        intent2.putExtra("receiverUsername", username);
        Intent[] intents = new Intent[]{intent1, intent2};
        return PendingIntent.getActivities(this, 0, intents, PendingIntent.FLAG_ONE_SHOT);




    }
}
