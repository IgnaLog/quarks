package com.quarks.android;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import com.quarks.android.Items.ConversationItem;
import com.quarks.android.Utils.DataBaseHelper;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static com.quarks.android.Utils.Functions.formatDate;
import static com.quarks.android.Utils.Functions.formatTime;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton fabContacts;
    private RecyclerView rvConversations;
    private LoadingWheel loadingWheel;
    private View lyCiruclarProgressBar;
    private Context context = MainActivity.this;
    private Socket socket;
    private String userId = "", username = "";
    private Map<String, String> values = new HashMap<String, String>();

    private ConversationsAdapter adapter;
    private ArrayList<ConversationItem> alConversations = new ArrayList<ConversationItem>();

    private SQLiteDatabase db;
    private DataBaseHelper dataBaseHelper;
    private Cursor cursor;

    private Boolean isScrolling = false;
    private Boolean noMoreScrolling = false;
    private LinearLayoutManager linearLayoutManager;
    private int currentItems, totalItems, scrollOutItems, totalCursor;
    private static int limitItemsToScroll = 50;  // Limit number to start loading batch messages
    private static int itemsToShow = 20; // Number of messages to show at the beginning
    private static int nextItemsToShow = 100; // Number of messages to display each time there is a new load
    private static int indexItems = 0; // Indicator to know where we are in the message cursor of the local database

    private boolean typing = false;
    private Handler typingHandler = new Handler();
    private static final int TYPING_TIMER_LENGTH = 1500;

    private static final int PENDING = 1;

    private Map<String, String> orderedDates = new HashMap<String, String>();

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
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    socket.emit("add-user", jsonObjectData);
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
                    JSONArray chats = (JSONArray) args[0];
                    try {
                        if (chats != null) {
                            for (int i = 0; i < chats.length(); i++) {
                                JSONObject jsonObjectChats = chats.getJSONObject(i);
                                String senderId = jsonObjectChats.getString("sender_id");
                                String senderUsername = jsonObjectChats.getString("sender_username");
                                JSONArray messages = jsonObjectChats.getJSONArray("messages");
                                if (messages.length() > 0) {
                                    String lastMessage = "";
                                    String lastDateTime = "";
                                    for (int j = 0; j < messages.length(); j++) {
                                        JSONObject jsonObjectMessages = messages.getJSONObject(j);
                                        String message = jsonObjectMessages.getString("message");
                                        String mongoTime = jsonObjectMessages.getString("time");
                                        String time = Functions.formatMongoTime(mongoTime);
                                        int channel = 2;

                                        /* We save the message in the local database and collect the date and the message id to compose a new item */
                                        values = dataBaseHelper.storeMessage(senderId, senderUsername, message, channel, time, PENDING); // We store the message into the local data base and we obtain the id and time from the record stored
                                        String dateTime = values.get("time"); // Comes from local database when saving the message

                                        // To save data in the conversations table and then present the messages sorted by date
                                        if(j == messages.length()){
                                            lastMessage = message;
                                            lastDateTime = dateTime;
                                            orderMessages(senderId, dateTime);
                                        }
                                    }
                                    // We updated the conversations table
                                    dataBaseHelper.updateConversations(senderId, senderUsername, lastMessage, lastDateTime, messages.length());
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
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
                    String message = "";
                    int channel = 2;

                    try {
                        senderId = data.getString("senderId");
                        senderUsername = data.getString("senderUsername");
                        message = data.getString("content");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    /* We save the message in the local database and collect the date and the message id to compose a new item */
                    values = dataBaseHelper.storeMessage(senderId, senderUsername, message, channel, "", PENDING); // We store the message into the local data base and we obtain the id and time from the record stored
                    String dateTime = values.get("time"); // Comes from local database when saving the message
                    // We updated the conversations table
                    dataBaseHelper.updateConversations(senderId, senderUsername, message, dateTime, 1);

                    // ver si existe, insertar o Actualizar item y mover
                    if (adapter.indexOf(senderId) != -1) { // Ya existe, actualizar y mover
                        int fromPosition = adapter.indexOf(senderId);
                        int toPosition = 1;
                        ConversationItem conversationItem = new ConversationItem(
                                alConversations.get(fromPosition).getUrlPhoto(),
                                alConversations.get(fromPosition).getFilename(),
                                alConversations.get(fromPosition).getUsername(),
                                alConversations.get(fromPosition).getUserId(),
                                message,
                                dateTime,
                                alConversations.get(fromPosition).geNumNewMessages() + 1
                        );
                        alConversations.remove(fromPosition);
                        alConversations.add(toPosition, conversationItem);
                        adapter.notifyItemMoved(fromPosition, toPosition);
                    } else { // insertar
                        int insertIndex = 1;
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
                        alConversations.add(insertIndex, conversationItem);
                        adapter.notifyItemInserted(insertIndex);
                    }
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
                    int position = adapter.indexOf(senderId);
                    ConversationItem conversationItem = new ConversationItem(
                            alConversations.get(position).getUrlPhoto(),
                            alConversations.get(position).getFilename(),
                            alConversations.get(position).getUsername(),
                            alConversations.get(position).getUserId(),
                            getResources().getString(R.string.typing),
                            alConversations.get(position).geTime(),
                            alConversations.get(position).geNumNewMessages()
                    );

                    if (position != -1) {
                        alConversations.set(position, conversationItem);
                        adapter.notifyItemChanged(position);
                    }
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
                    int position = adapter.indexOf(senderId);
                    ConversationItem conversationItem = new ConversationItem(
                            alConversations.get(position).getUrlPhoto(),
                            alConversations.get(position).getFilename(),
                            alConversations.get(position).getUsername(),
                            alConversations.get(position).getUserId(),
                            alConversations.get(position).getLastMessage(),
                            alConversations.get(position).geTime(),
                            alConversations.get(position).geNumNewMessages()
                    );

                    if (position != -1) {
                        alConversations.set(position, conversationItem);
                        adapter.notifyItemChanged(position);
                    }
                }
            });
        }
    };

    /**
     * OVERRIDE
     **/

    @Override
    protected void onResume() {
        super.onResume();
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

        // Volvemos a conectar el socket
        if (!socket.connected()) {
            try {
                socket = IO.socket(getResources().getString(R.string.url_chat));
            } catch (URISyntaxException e) {
                Log.d("Error", "Error socketURL: " + e.toString());
            }
            socket.connect();
            socket.on("connected", connected);

            socket.on("all-pending-messages", getPendingMessages);
            socket.on("send-message", listeningMessages);

            socket.on("typing", onTyping);
            socket.on("stop-typing", onStopTyping);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true); // Pressing the back button of Android, we would go to the end of the stack of open activities. In this case it would go to the SplashActivity but it is closed, therefore, the app would be minimized
    }

    /**
     * FUNCTIONS
     **/

    private void orderMessages(String senderId, String dateTime){
        if(orderedDates.isEmpty()){
            orderedDates.put(senderId, dateTime);
        }
        LinkedHashMap<String,String> ha = new LinkedHashMap<accessOrder>();

        ConcurrentSkipListMap<String,String> has = new ConcurrentSkipListMap<>();
        has.
        if(){

        }
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
            adapter = new ConversationsAdapter(getApplicationContext(), alConversations);
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
}