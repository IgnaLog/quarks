package com.quarks.android.Utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.quarks.android.ChatActivity;
import com.quarks.android.MainActivity;
import com.quarks.android.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FCM extends FirebaseMessagingService {
    private static String CHANNEL_ID_HIGH = "high";
    private static String CHANNEL_ID_LOW = "low";
    private static String NAME_CHANNEL_LOW = "lowPriorityNotification";
    private static String NAME_CHANNEL_HIGH = "highPriorityNotification";
    private static String GROUP_KEY = "com.quarks.android";
    public static int SUMMARY_NOTIFICATION_ID = 0;
    private static int PRIORITY_LOW = -1;
    private static int PRIORITY_HIGH = 1;
    private static int PRIORITY_MIN = -2;
    private static int PRIORITY_DEFAULT = 0;
    private static int PRIORITY_MAX = 2;
    private static int id = 1;
    private static int count = 0;

    public static Map<String, Integer> mapNotificationIds = new HashMap<String, Integer>();
    public static Map<String, List<Message>> mapMessages = new HashMap<String, List<Message>>();
    public static Map<String, String> mapUserIds = new HashMap<String, String>();

    private static NotificationManagerCompat notificationManager;

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        // We store the token
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

            // We launch the timely notification
            sendMessageNotification(this, userId, username, message, mapMessages);

        }
    }

    public void sendMessageNotification(Context context, String userId, String username, String message, Map<String, List<Message>> mapMessages) {
        notificationManager = NotificationManagerCompat.from(context); // Create notification manager
        createChannels();
        NotificationCompat.Action replyAction = createReplyAction(context);
        mapUserIds.put(username, userId); // we save the userId with its respective username
        storeMessage(username, message); // We store the messages of their respective username in our map mapMessages


        /*  For the first user to notify, we show an individual notification.
            We only create the group notification but it isn't shown because there is only one notification id.
            Here the priority will be high. */
        if (mapNotificationIds.size() <= 1) {
            NotificationCompat.MessagingStyle messagingStyle = createMessagingStyle(username);
            launchSimpleNotification(context, CHANNEL_ID_HIGH, messagingStyle, replyAction, PRIORITY_HIGH, userId, username);
            launchSummaryNotification(context, CHANNEL_ID_HIGH, PRIORITY_HIGH, username);
        } else {  // For the following messages from other users we lower the priority of the individual notification and create a new channel for it, with a low priority
            count++;
            // If there are notifications that are not active and a second user has already sent more than one notification:
            if (anyNotificationNotActive(context) && count > 1 && isNotificationActive(getNotificationId(username), context)) { // If the user who notifies a new message already has an active notification, we only notify active messages
                for (Map.Entry<String, List<Message>> entry : mapMessages.entrySet()) {
                    String user = entry.getKey();
                    int id = getNotificationId(user);
                    if (isNotificationActive(id, context)) {
                        NotificationCompat.MessagingStyle messagingStyle = createMessagingStyle(user);
                        String serverUserId = mapUserIds.get(user);
                        launchSimpleNotification(context, CHANNEL_ID_LOW, messagingStyle, replyAction, PRIORITY_LOW, serverUserId, user);
                        launchSummaryNotification(context, CHANNEL_ID_HIGH, PRIORITY_HIGH, user);
                    }
                }
            } else if (anyNotificationNotActive(context) && count > 1 && !isNotificationActive(getNotificationId(username), context)) { // If the user who notifies a new message doesn't previously have an active notification, his messages and all active notifications are notified
                if (numNotificationsActive(context) == 1) { // If there is only one active notification, we cancel the group to create a new notification group
                    int uniqueNotificationActive = getUniqueNotificationActive(context);
                    notificationManager.cancel(SUMMARY_NOTIFICATION_ID);

                    for (Map.Entry<String, List<Message>> entry : mapMessages.entrySet()) {
                        String user = entry.getKey();
                        int id = getNotificationId(user);
                        if (uniqueNotificationActive == id || id == getNotificationId(username)) {
                            NotificationCompat.MessagingStyle messagingStyle = createMessagingStyle(user);
                            String serverUserId = mapUserIds.get(user);
                            launchSimpleNotification(context, CHANNEL_ID_LOW, messagingStyle, replyAction, PRIORITY_LOW, serverUserId, user);
                            launchSummaryNotification(context, CHANNEL_ID_HIGH, PRIORITY_HIGH, user);
                        }
                    }
                } else {
                    for (Map.Entry<String, List<Message>> entry : mapMessages.entrySet()) {
                        String user = entry.getKey();
                        int id = getNotificationId(user);
                        if (isNotificationActive(id, context) || id == getNotificationId(username)) {
                            NotificationCompat.MessagingStyle messagingStyle = createMessagingStyle(user);
                            String serverUserId = mapUserIds.get(user);
                            launchSimpleNotification(context, CHANNEL_ID_LOW, messagingStyle, replyAction, PRIORITY_LOW, serverUserId, user);
                            launchSummaryNotification(context, CHANNEL_ID_HIGH, PRIORITY_HIGH, user);
                        }
                    }
                }
            } else { // If all notifications are active, we load all messages
                if (count == 1) { // If it's the first time that a second user notifies, we cancel the group notification so that a group notification is displayed
                    notificationManager.cancel(SUMMARY_NOTIFICATION_ID);
                }
                for (Map.Entry<String, List<Message>> entry : mapMessages.entrySet()) {
                    String user = entry.getKey();
                    NotificationCompat.MessagingStyle messagingStyle = createMessagingStyle(user);
                    String serverUserId = mapUserIds.get(user);
                    launchSimpleNotification(context, CHANNEL_ID_LOW, messagingStyle, replyAction, PRIORITY_LOW, serverUserId, user);
                    launchSummaryNotification(context, CHANNEL_ID_HIGH, PRIORITY_HIGH, user);
                }
            }
        }
    }

    public static void updateMessageNotification(Context context, Map<String, List<Message>> mapMessages) {
        notificationManager = NotificationManagerCompat.from(context); // Create notification manager
        createChannels();
        NotificationCompat.Action replyAction = createReplyAction(context);

        if (anyNotificationNotActive(context)) {
            for (Map.Entry<String, List<Message>> entry : mapMessages.entrySet()) {
                String user = entry.getKey();
                int id = getNotificationId(user);
                if (isNotificationActive(id, context)) {
                    NotificationCompat.MessagingStyle messagingStyle = createMessagingStyle(user);
                    String serverUserId = mapUserIds.get(user);
                    launchSimpleNotification(context, CHANNEL_ID_LOW, messagingStyle, replyAction, PRIORITY_LOW, serverUserId, user);
                    launchSummaryNotification(context, CHANNEL_ID_LOW, PRIORITY_LOW, user);
                }
            }
        } else { // If all notifications are active, we load all messages
            for (Map.Entry<String, List<Message>> entry : mapMessages.entrySet()) {
                String user = entry.getKey();
                NotificationCompat.MessagingStyle messagingStyle = createMessagingStyle(user);
                String serverUserId = mapUserIds.get(user);
                launchSimpleNotification(context, CHANNEL_ID_LOW, messagingStyle, replyAction, PRIORITY_LOW, serverUserId, user);
                launchSummaryNotification(context, CHANNEL_ID_LOW, PRIORITY_LOW, user);
            }
        }
    }

    /* We launch a simple notification with a messaging style */
    private static void launchSimpleNotification(Context context, String ChannelId, NotificationCompat.MessagingStyle messagingStyle, NotificationCompat.Action replyAction, int priority, String userId, String username) {
        Notification firstNotification = new NotificationCompat.Builder(context, ChannelId)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(messagingStyle)
                .setGroup(GROUP_KEY)
                .addAction(replyAction)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(priority)
                .setContentIntent(pendingIntentChat(context, userId, username)) // Set the intent that will fire when the user taps the notification
                .setAutoCancel(true)
                .setSortKey(username)
                .build();
        notificationManager.notify(getNotificationId(username), firstNotification);
    }

    /* We launched a group notification with a title of the set of messages and chats */
    private static void launchSummaryNotification(Context context, String ChannelId, int priority, String username) {
        Notification summaryNotification = new NotificationCompat.Builder(context, ChannelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(new NotificationCompat.InboxStyle()
                        .setSummaryText(getActiveMessagesChats(context, username))) // We show a small header in the group notification with the list of total chats(username) and messages
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(priority)
                .setContentIntent(pendingIntentConver(context))
                .setAutoCancel(true)
                .setSortKey(username)
                .build();
        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification);
    }

    /* Given a username, we take all your messages and create a messaging style to use for notifications */
    private static NotificationCompat.MessagingStyle createMessagingStyle(String username) {
        // We create a messaging notification style
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("Me");
        // We go through the messages of a user, passing our map with its corresponding user
        List<Message> alMessages = mapMessages.get(username);
        for (Message chatMessage : alMessages) {
            NotificationCompat.MessagingStyle.Message notificationMessage = new NotificationCompat.MessagingStyle.Message(
                    chatMessage.getText(),
                    chatMessage.getTimestamp(),
                    chatMessage.getSender()
            );
            messagingStyle.addMessage(notificationMessage);
        }
        return messagingStyle;
    }

    /*  we add a response mode */
    private static NotificationCompat.Action createReplyAction(Context context) {
        int requestID = (int) System.currentTimeMillis();
        RemoteInput remoteInput = new RemoteInput.Builder("key_text_reply").setLabel(context.getResources().getString(R.string.answer)).build();
        Intent replyIntent;
        PendingIntent replyPendingIntent;

        replyIntent = new Intent(context, DirectReplyReceiver.class);
        replyPendingIntent = PendingIntent.getBroadcast(context, requestID, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action.Builder(R.drawable.ic_reply, context.getResources().getString(R.string.reply), replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    /* For each user we keep a List of all the messages that that are entering inside a key value map. If a new user arrives, we create a new key-value */
    private void storeMessage(String username, String message) {
        if (mapMessages.containsKey(username)) {
            List<Message> myListMessages = mapMessages.get(username);
            myListMessages.add(new Message(message, username));
            mapMessages.put(username, myListMessages);
        } else {
            List<Message> myListMessages = new ArrayList<>();
            myListMessages.add(new Message(message, username));
            mapMessages.put(username, myListMessages);
        }
    }

    /*If the version is superior to Oreo we need to create notification channels. If you want to lower the priority or upload it from a notification, you must also specify it in the channel */
    private static void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channelHigh = new NotificationChannel(CHANNEL_ID_HIGH, NAME_CHANNEL_HIGH, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channelHigh);
            NotificationChannel channelLow = new NotificationChannel(CHANNEL_ID_LOW, NAME_CHANNEL_LOW, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channelLow);
        }
    }

    /* Returns the number of active notifications, not counting the summary notification */
    public static int numNotificationsActive(Context context) {
        int count = 0;
        for (Map.Entry<String, Integer> entry : mapNotificationIds.entrySet()) {
            int id = entry.getValue();
            if (id != 0) {
                boolean isActive = isNotificationActive(id, context);
                if (isActive) {
                    count++;
                }
            }
        }
        return count;
    }

//    /* Returns the number of active notifications, not counting the summary notification */
//    public static int staticNumNotificationsActive(Context context) {
//        int count = 0;
//        for (Map.Entry<String, Integer> entry : mapNotificationIds.entrySet()) {
//            int id = entry.getValue();
//            if (id != 0) {
//                boolean isActive = Functions.isNotificationActive(id, context);
//                if (isActive) {
//                    count++;
//                }
//            }
//        }
//        return count;
//    }

    /* Returns the id of the only active notification */
    private static int getUniqueNotificationActive(Context context) {
        int notification = -1;
        for (Map.Entry<String, Integer> entry : mapNotificationIds.entrySet()) {
            int id = entry.getValue();
            if (id != 0) {
                boolean isActive = isNotificationActive(id, context);
                if (isActive) {
                    notification = id;
                }
            }
        }
        return notification;
    }

    /* If there is any notification that is not active, not counting the group notification */
    private static boolean anyNotificationNotActive(Context context) {
        boolean any = false;
        for (Map.Entry<String, Integer> entry : mapNotificationIds.entrySet()) {
            int id = entry.getValue();
            if (id != 0) {
                boolean isActive = isNotificationActive(id, context);
                if (!isActive) {
                    any = true;
                }
            }
        }
        return any;
    }

    /* We obtain the total messages of actives notifications for each user saved in the map mapMessages */
    private static String getActiveMessagesChats(Context context, String username) {
        int conversations = 0;
        int messages = 0;
        String totalMessagesChats = "";
        for (Map.Entry<String, List<Message>> entry : mapMessages.entrySet()) {
            String user = entry.getKey();
            int id = getNotificationId(user);
            if (isNotificationActive(id, context) || id == getNotificationId(username)) {
                conversations++;
                List<Message> listValues = entry.getValue();
                messages += listValues.size();
                totalMessagesChats = messages + " " + context.getResources().getString(R.string.notifi_messages) + " " + conversations + " " + context.getResources().getString(R.string.notifi_chats);
            }
        }
        return totalMessagesChats;
    }

    /* We obtain the total messages for each user saved in the map mapMessages */
    private static String getTotalMessagesChats(Context context) {
        int conversations = mapMessages.size();
        int messages = 0;
        String totalMessagesChats = "";
        for (Map.Entry<String, List<Message>> entry : mapMessages.entrySet()) {
            List<Message> listValues = entry.getValue();
            messages += listValues.size();
        }
        totalMessagesChats =
                String.valueOf(messages) + " " +
                        context.getResources().getString(R.string.notifi_messages) + " " +
                        String.valueOf(conversations) + " " +
                        context.getResources().getString(R.string.notifi_chats);
        return totalMessagesChats;
    }

    /* Given a notification id, it tells you if it's active */
    public static boolean isNotificationActive(int notificationId, Context context) {
        boolean is = false;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = new StatusBarNotification[0];
        if (mNotificationManager != null) {
            notifications = mNotificationManager.getActiveNotifications();
        }
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == notificationId) {
                is = true;
            }
        }
        return is;
    }

    /* Using a map key value (username: id++), we obtain the corresponding id of each user */
    public static int getNotificationId(String username) {
        if (!mapNotificationIds.containsKey(username)) {
            mapNotificationIds.put(username, id++);
        }
        return mapNotificationIds.get(username);
    }

    /* We create an intent to take you to Chat Activity that goes through MainActivity or CoversationActivity first */
    private static PendingIntent pendingIntentChat(Context context, String userId, String username) {
        int requestID = (int) System.currentTimeMillis();
        Intent intent1 = new Intent(context, MainActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Intent intent2 = new Intent(context, ChatActivity.class);
        intent2.putExtra("receiverId", userId);
        intent2.putExtra("receiverUsername", username);
        Intent[] intents = new Intent[]{intent1, intent2};
        return PendingIntent.getActivities(context, requestID, intents, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /* An intent that takes you to ConversationActivity or MainActivity */
    private static PendingIntent pendingIntentConver(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
