package com.quarks.android;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.quarks.android.Adapters.ConversationsAdapter;
import com.quarks.android.CustomViews.LoadingWheel;
import com.quarks.android.Interfaces.ClickConversationInterface;
import com.quarks.android.Interfaces.MessagesNotSentInterface;
import com.quarks.android.Items.ConversationItem;
import com.quarks.android.Utils.CheckNetworkReceiver;
import com.quarks.android.Utils.DataBaseHelper;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.Preferences;
import com.quarks.android.Utils.SocketHandler;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity implements ClickConversationInterface, MessagesNotSentInterface {

    private FloatingActionButton fabContacts;
    private RecyclerView rvConversations;
    private LoadingWheel loadingWheel;
    private View lyCiruclarProgressBar;
    private Context context = MainActivity.this;
    private Socket socket;
    private String userId = "", username = "";
    private Map<String, String> values = new HashMap<String, String>();

    public static ConversationsAdapter adapter;
    private ArrayList<ConversationItem> alConversations = new ArrayList<ConversationItem>();

    private DataBaseHelper dataBaseHelper;
    private Cursor cursor;

    private boolean isScrolling = false;
    private boolean noMoreScrolling = false;
    private LinearLayoutManager linearLayoutManager;
    private int currentItems, totalItems, scrollOutItems, totalCursor;
    private static int limitItemsToScroll = 50;  // Limit number to start loading batch messages
    private static int itemsToShow = 20; // Number of messages to show at the beginning
    private static int nextItemsToShow = 100; // Number of messages to display each time there is a new load
    private static int indexItems = 0; // Indicator to know where we are in the message cursor of the local database

    private static final int PENDING = 1;

    private static final int LAUNCH_SECOND_ACTIVITY = 1;
    private boolean isOnPauseFromConversationClick = false; // Se ejecuta onPause debido a que pulsamos en una conversacion que nos abre un activity nueva

    private static final int STATELESS = -1;
//    private static final int NOT_SENT = 0;
//    private static final int SENT = 1;
//    private static final int RECEIVED = 2;
//    private static final int VIEWED = 3;

    private BroadcastReceiver mNetworkReceiver;
    private static final String MY_CONNECTIVITY_CHANGE = "com.quarks.android.connectivity.change";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**  STATEMENTS  **/

        fabContacts = findViewById(R.id.fabContacts);
        rvConversations = findViewById(R.id.rvConversations);
        lyCiruclarProgressBar = findViewById(R.id.lyCiruclarProgressBar);

        dataBaseHelper = new DataBaseHelper(context);
        linearLayoutManager = new LinearLayoutManager(context);

        loadingWheel = new LoadingWheel(context, lyCiruclarProgressBar); // To show a wheel loading

        userId = Preferences.getUserId(context);
        username = Preferences.getUserName(context);

        mNetworkReceiver = new CheckNetworkReceiver();

        /**  SOCKETS CONNECTIONS  **/

        if (SocketHandler.getSocket() == null) {
            try {
                socket = IO.socket(getResources().getString(R.string.url_chat));
            } catch (URISyntaxException e) {
                Log.d("Error", "Error socketURL: " + e.toString());
            }
            socket.connect();
            socket.on("connected", connected);
            socket.on(Socket.EVENT_RECONNECT, reconnect);
            socket.on("all-pending-messages", getPendingMessages);
            socket.on("send-message", listeningMessages);
            socket.on("typing", onTyping);
            socket.on("stop-typing", onStopTyping);
            SocketHandler.setSocket(socket);
        } else {
            socket = SocketHandler.getSocket();
            socket.off();
            socket.on("connected", connected);
            socket.on(Socket.EVENT_RECONNECT, reconnect);
            socket.on("all-pending-messages", getPendingMessages);
            socket.on("send-message", listeningMessages);
            socket.on("typing", onTyping);
            socket.on("stop-typing", onStopTyping);
        }

        /** DESIGN **/

        /* Status Bar */
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // The color of the icons is adapted, if not white
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_gris)); // Bar color

        /** LISTENERS */

        rvConversations.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                currentItems = linearLayoutManager.getChildCount(); // Total items on screen
                totalItems = linearLayoutManager.getItemCount(); // Total de items
                scrollOutItems = linearLayoutManager.findFirstVisibleItemPosition(); // How many have you displaced

                /* Go loading batches of items so as not to slow down the app */
                if (!noMoreScrolling && isScrolling && (totalCursor > limitItemsToScroll) && (currentItems + scrollOutItems == totalItems)) {
                    //data fetch
                    isScrolling = false;
                    fetchData(false, cursor);
                }
            }
        });

        fabContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * EMITTERS THREADS
     **/

    /* We have been connected, so we send our user data */
    private Emitter.Listener connected = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject jsonObjectData = new JSONObject();
                    try {
                        // My user
                        jsonObjectData.put("userId", userId);
                        jsonObjectData.put("username", username);
                        jsonObjectData.put("activity", "conversationsActivity");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    socket.emit("add-user", jsonObjectData);
                }
            });
        }
    };

    /* We have been reconnected, so we launch the broadcast to send the unsent messages */
    private Emitter.Listener reconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(MY_CONNECTIVITY_CHANGE);
                    sendBroadcast(intent);
                }
            });
        }
    };

    /* We retrieve pending messages*/
    private Emitter.Listener getPendingMessages = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONArray pendingMessages = (JSONArray) args[0];
                    if (pendingMessages != null) {
                        try {
                            JSONObject jsonObject = pendingMessages.getJSONObject(0);
                            JSONArray jsonArrayAllMessages = jsonObject.getJSONArray("allMessages");
                            JSONArray jsonArrayOrderedLastMessages = jsonObject.getJSONArray("orderedLastMessages");
                            if (jsonArrayAllMessages.length() > 0) {
                                JSONObject jsonObjectChats = jsonArrayAllMessages.getJSONObject(0);
                                JSONArray chats = jsonObjectChats.getJSONArray("chats");
                                if (chats.length() > 0) {
                                    for (int i = 0; i < chats.length(); i++) {
                                        JSONObject jsonObjectChat = chats.getJSONObject(i);
                                        String senderId = jsonObjectChat.getString("sender_id");
                                        String senderUsername = jsonObjectChat.getString("sender_username");
                                        JSONArray messages = jsonObjectChat.getJSONArray("messages");
                                        if (messages.length() > 0) {
                                            for (int j = 0; j < messages.length(); j++) {
                                                JSONObject jsonObjectMessages = messages.getJSONObject(j);
                                                String senderMessageId = jsonObjectMessages.getString("message_id");
                                                String message = jsonObjectMessages.getString("message");
                                                String mongoTime = jsonObjectMessages.getString("time");
                                                String time = Functions.formatMongoTime(mongoTime);
                                                int channel = 2;

                                                /* We save the message in the local database and collect the date and the message id to compose a new item */
                                                values = dataBaseHelper.storeMessage(senderId, senderUsername, message, senderMessageId, channel, time, PENDING, STATELESS); // We store the message into the local data base and we obtain the id and time from the record stored
                                            }
                                        }
                                    }
                                }
                            }
                            if (jsonArrayOrderedLastMessages.length() > 0) {
                                JSONObject jsonObjectChat = jsonArrayOrderedLastMessages.getJSONObject(0);
                                JSONArray chats = jsonObjectChat.getJSONArray("chats");
                                if (chats.length() > 0) {
                                    for (int i = 0; i < chats.length(); i++) {
                                        JSONObject jsonObjectChats = chats.getJSONObject(i);
                                        String senderId = jsonObjectChats.getString("sender_id");
                                        String sender_username = jsonObjectChats.getString("sender_username");
                                        int totalMessages = jsonObjectChats.getInt("total_messages");
                                        String lastMessage = jsonObjectChats.getString("last_message");
                                        String time = jsonObjectChats.getString("time");

                                        // We updated the conversations table
                                        dataBaseHelper.updateConversations(senderId, sender_username, lastMessage, Functions.formatMongoTime(time), totalMessages);

                                        reorganizeConversation(senderId, lastMessage, Functions.formatMongoTime(time), totalMessages, i);
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    };

    /* We receive live messages */
    private Emitter.Listener listeningMessages = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String senderId = "";
                    String senderUsername = "";
                    String senderMessageId = "";
                    String message = "";
                    int channel = 2;

                    try {
                        senderId = data.getString("senderId");
                        senderUsername = data.getString("senderUsername");
                        senderMessageId = data.getString("contentId");
                        message = data.getString("content");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    /* We save the message in the local database and collect the date and the message id to compose a new item */
                    values = dataBaseHelper.storeMessage(senderId, senderUsername, message, senderMessageId, channel, "", PENDING, STATELESS); // We store the message into the local data base and we obtain the id and time from the record stored
                    String dateTime = values.get("time"); // Comes from local database when saving the message

                    // We updated the conversations table
                    dataBaseHelper.updateConversations(senderId, senderUsername, message, dateTime, 1);

                    reorganizeConversation(senderId, message, dateTime, 1, 0);
                }
            });
        }
    };

    /* The receiver is typing */
    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String senderId = "";
                    try {
                        senderId = data.getString("senderId");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    conversationTyping(senderId, true);
                }
            });
        }
    };

    /* The receiver has stopped typing */
    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String senderId = "";
                    try {
                        senderId = data.getString("senderId");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    conversationTyping(senderId, false);
                }
            });
        }
    };

    /**
     * OVERRIDE
     **/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LAUNCH_SECOND_ACTIVITY) {
            if (resultCode == Activity.RESULT_CANCELED) {
                socket = SocketHandler.getSocket();
                socket.off();
                socket.on("connected", connected);
                socket.on(Socket.EVENT_RECONNECT, reconnect);
                socket.on("all-pending-messages", getPendingMessages);
                socket.on("send-message", listeningMessages);
                socket.on("typing", onTyping);
                socket.on("stop-typing", onStopTyping);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        myRegisterReceiver(mNetworkReceiver);
        isOnPauseFromConversationClick = false;

        if (adapter != null) {
            adapter.Clear();
        }
        cursor = dataBaseHelper.getAllConversations();
        fetchData(true, cursor); // Load conversations again

        // Quitamos todas las notificaciones
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        if (!socket.connected()) {
            SocketHandler.cleanSocket();
            socket.connect();
            SocketHandler.setSocket(socket);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Suprimimos el broadcastReceiver que controla si hay un cambio en la conexion de internet
        unregisterReceiver(mNetworkReceiver);

        // Si no procede de clickar una conversacion que nos lleva a un activity nueva. Desconectamos el socket
        if (!isOnPauseFromConversationClick) {
            socket.emit("disconnect", "");
            socket.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Si no procede de clickar una conversacion que nos lleva a un activity nueva. Desconectamos el socket
        if (!isOnPauseFromConversationClick) {
            socket.emit("disconnect", "");
            socket.disconnect();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true); // Pressing the back button of Android, we would go to the end of the stack of open activities. In this case it would go to the SplashActivity but it is closed, therefore, the app would be minimized
    }

    /**
     * FUNCTIONS
     **/

    private void conversationTyping(String senderId, boolean typing) {
        int position = indexOf(senderId);
        if (position != -1) {
            String message;
            if (typing) {
                message = getResources().getString(R.string.typing);
            } else {
                Cursor c = dataBaseHelper.getLastMessageConversation(senderId);
                c.moveToFirst();
                message = c.getString(c.getColumnIndex("last_message"));
            }
            adapter.notifyItemChanged(position, message);
        }
    }

    private void reorganizeConversation(String senderId, String lastMessage, String time, int totalMessages, int toPosition) {
        // ver si existe, insertar o Actualizar item y mover
        if (indexOf(senderId) != -1) { // Ya existe, actualizar y mover
            int fromPosition = indexOf(senderId);
            ConversationItem conversationItem = new ConversationItem(
                    alConversations.get(fromPosition).getUrlPhoto(),
                    alConversations.get(fromPosition).getFilename(),
                    alConversations.get(fromPosition).getUsername(),
                    alConversations.get(fromPosition).getUserId(),
                    lastMessage,
                    time,
                    alConversations.get(fromPosition).geNumNewMessages() + totalMessages
            );
            alConversations.set(fromPosition, conversationItem);
            adapter.notifyItemChanged(fromPosition, conversationItem);

            alConversations.remove(fromPosition);
            alConversations.add(toPosition, conversationItem);
            adapter.notifyItemMoved(fromPosition, toPosition);
        } else { // insertar
            Cursor c = dataBaseHelper.getConversation(senderId);
            c.moveToFirst();
            ConversationItem conversationItem = new ConversationItem(
                    "",
                    "",
                    c.getString(c.getColumnIndex("sender_username")),
                    c.getString(c.getColumnIndex("sender_id")),
                    c.getString(c.getColumnIndex("last_message")),
                    c.getString(c.getColumnIndex("time")),
                    c.getInt(c.getColumnIndex("new_messages"))
            );
            alConversations.add(toPosition, conversationItem);
            adapter.notifyItemInserted(toPosition);
        }
    }

    private int indexOf(String senderId) {
        for (int i = 0; i < alConversations.size(); i++) {
            if (alConversations.get(i).getUserId().equals(senderId)) {
                return i;
            }
        }
        return -1;
    }

    /* Function that loads the cursor data into the ArrayList */
    private void loadItems(Cursor c) {
        alConversations.add(new ConversationItem(
                "",
                "",
                c.getString(c.getColumnIndex("sender_username")),
                c.getString(c.getColumnIndex("sender_id")),
                c.getString(c.getColumnIndex("last_message")),
                c.getString(c.getColumnIndex("time")),
                c.getInt(c.getColumnIndex("new_messages"))
        ));
    }

    /* Function that according to if there is a lot of data loads the data in batches */
    private void fetchData(@NonNull Boolean firstLoad, final Cursor c) {
        if (firstLoad) {
            totalCursor = c.getCount();
            if (totalCursor > limitItemsToScroll) { // We exceed the limit set by the developer to start loading items in batches
                c.moveToFirst();
                for (int i = 0; i < itemsToShow; i++) {
                    loadItems(c);
                    c.moveToNext();
                }
                indexItems = itemsToShow;
            } else { // We do not exceed the limit set by the developer. Therefore, we load the data normally
                while (c.moveToNext()) {
                    loadItems(c);
                }
            }
            adapter = new ConversationsAdapter(getApplicationContext(), alConversations, MainActivity.this);
            rvConversations.setLayoutManager(linearLayoutManager);
            rvConversations.setAdapter(adapter);
            rvConversations.setHasFixedSize(true);
        } else {
            loadingWheel.setLoading(true); // We show a wheel loading
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (indexItems + nextItemsToShow >= totalCursor) { // We have reached the end of batch loading
                        c.moveToPosition(indexItems);
                        ArrayList<ConversationItem> alConversationsAux = new ArrayList<ConversationItem>();
                        for (int i = indexItems; i < totalCursor; i++) {
                            alConversationsAux.add(new ConversationItem(
                                    "",
                                    "",
                                    c.getString(c.getColumnIndex("sender_username")),
                                    c.getString(c.getColumnIndex("sender_id")),
                                    c.getString(c.getColumnIndex("last_message")),
                                    c.getString(c.getColumnIndex("time")),
                                    c.getInt(c.getColumnIndex("new_messages"))
                            ));
                            c.moveToNext();
                            if (c.isLast()) {
                                noMoreScrolling = true;
                            }
                        }
                        noMoreScrolling = true;
                        alConversations.addAll(indexItems, alConversationsAux);
                        adapter.notifyItemRangeInserted(indexItems, alConversationsAux.size());
                        c.close();
                        loadingWheel.setLoading(false);
                    } else { // We continue loading batches of items
                        c.moveToPosition(indexItems);
                        ArrayList<ConversationItem> alConversationsAux = new ArrayList<ConversationItem>();
                        for (int i = 0; i < nextItemsToShow; i++) {
                            alConversationsAux.add(new ConversationItem(
                                    "",
                                    "",
                                    c.getString(c.getColumnIndex("sender_username")),
                                    c.getString(c.getColumnIndex("sender_id")),
                                    c.getString(c.getColumnIndex("last_message")),
                                    c.getString(c.getColumnIndex("time")),
                                    c.getInt(c.getColumnIndex("new_messages"))
                            ));
                            c.moveToNext();
                        }
                        alConversations.addAll(indexItems, alConversationsAux);
                        adapter.notifyItemRangeInserted(indexItems, alConversationsAux.size());
                        indexItems = indexItems + nextItemsToShow;
                        loadingWheel.setLoading(false);
                    }
                }
            }, 200); // It is important to put a delay so that when you scroll very fast it does not get blocked
        }
    }

    private void myRegisterReceiver(BroadcastReceiver mNetworkReceiver) {
        IntentFilter filter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final Intent intent = new Intent(MY_CONNECTIVITY_CHANGE);
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(),
                        new ConnectivityManager.NetworkCallback() {
                            @Override
                            public void onAvailable(@NonNull Network network) {
                                sendBroadcast(intent);
                            }

                            @Override
                            public void onLost(@NonNull Network network) {
                                // sendBroadcast(intent);
                            }
                        });
            }
            filter.addAction(MY_CONNECTIVITY_CHANGE);
        } else {
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        }
        registerReceiver(mNetworkReceiver, filter);
    }

    /**
     * INTERFACES
     **/

    @Override
    public void onConversationClick() {
        isOnPauseFromConversationClick = true;
    }

    @Override
    public void updateMessagesNotSent(ArrayList<String> alMessagesIds) {

    }
}