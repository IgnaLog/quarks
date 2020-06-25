package com.quarks.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quarks.android.Adapters.MessagesAdapter;
import com.quarks.android.CustomViews.LoadingWheel;
import com.quarks.android.Items.MessageItem;
import com.quarks.android.Utils.DataBaseHelper;
import com.quarks.android.Utils.FCM;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.Preferences;

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

import static com.quarks.android.Utils.Functions.formatDate;
import static com.quarks.android.Utils.Functions.formatTime;

public class ChatActivity extends AppCompatActivity {

    private RelativeLayout rootView;
    private LoadingWheel loadingWheel;
    private LinearLayout lyProfileBack;
    private FrameLayout flBtnDownRecycler;
    private View lyCiruclarProgressBar;
    private RecyclerView rvChat;
    private MessagesAdapter adapter;
    private ArrayList<MessageItem> alMessage = new ArrayList<MessageItem>();
    private EditText etMessage;
    private TextView tvDate, tvBadge, tvUsername, tvTyping;
    private Socket socket;
    private Button btnSend;
    private Context context = ChatActivity.this;
    private String userId = "", username = "", receiverId = "", receiverUsername = "";

    private SQLiteDatabase db;
    private DataBaseHelper dataBaseHelper = new DataBaseHelper(context);
    private Cursor cursor;

    private Map<String, String> values = new HashMap<String, String>();
    private LinearLayoutManager linearLayoutManager;
    private boolean isScrolling = false;
    private boolean endOfScroll = false;
    private int firstVisibleItem, firstCompletelyVisibleItem, lastVisibleItem, totalItems, scrollOutItems, totalCursor;
    private static int limitItemsToScroll = 20; // Limit number to start loading batch messages
    private static int itemsToShow = 20; // Number of messages to show at the beginning
    private static int nextItemsToShow = 50; // Number of messages to display each time there is a new load
    private static int indexItems = 0; // Indicator to know where we are in the message cursor of the local database

    private ObjectAnimator animDateAppear, animDateDisappear;
    private int oldVisibleItem = 0;
    private boolean appearFinished = false;
    private boolean disappearFinished = false;
    private int previousHeightDiff = 0;
    private Animation animBtnDownAppear, animBtnDownDisappear;
    private int oldLastVisibleItem = -1;
    private int badgeMessages = 0;
    private boolean newMessage = false; // Boolean to prevent tvDate animation appear

    private int positionPendingMessages = -1;
    private MessageItem itemWithPendingMessages;

    private boolean typing = false;
    private Handler typingHandler = new Handler();
    private static final int TYPING_TIMER_LENGTH = 1500;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        /**  STATEMENTS  **/

        setStatements();

        /**  CODE  **/

        /*  Retrieve previous messages from the local database */
        cursor = dataBaseHelper.getAllMessages(receiverId);
        fetchData(true, cursor);

        /**  SOCKETS CONNECTIONS  **/

        try {
            socket = IO.socket(getResources().getString(R.string.url_chat));
        } catch (URISyntaxException e) {
            Log.d("Error", "Error socketURL: " + e.toString());
        }
        socket.connect();
        socket.on("connected", connected);
        socket.on("pending-messages", getPendingMessages);
        socket.on("send-message", listeningMessages);
        socket.on("typing", onTyping);
        socket.on("stop-typing", onStopTyping);

        /**  LISTENERS  **/

        /* To cast when typing */
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                /* Emit typing message */
                if (null == receiverUsername) return;
                if (!socket.connected()) return;

                if (!typing) {
                    typing = true;
                    JSONObject jsonObjectData = new JSONObject();
                    try {
                        jsonObjectData.put("receiverId", receiverId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    socket.emit("typing", jsonObjectData);
                }
                // We allow time before considering that writing has stopped
                typingHandler.removeCallbacks(onTypingTimeout);
                typingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        /* Here we manage the control of new items to load, the animation with the date of the messages and the badge with which you scroll at the end of the conversation */
        rvChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                /* We have reached the end of the recyclerview and we hide the tvDate */
                if (!recyclerView.canScrollVertically(-1) && endOfScroll) {
                    if (animDateAppear.isStarted()) {
                        animDateAppear.cancel();
                    }
                    animDateDisappear.end();
                    animDateDisappear.cancel();
                    appearFinished = false;
                    disappearFinished = true;
                }

                /* Scroll state changes to motion, including inertial motion */
                if (newState == RecyclerView.SCROLL_STATE_SETTLING || newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isScrolling = true;

                    // We check if the second animation of tvDate has started and is not running to disappear to stop it in case of scroll
                    if (animDateDisappear.isStarted() && !animDateDisappear.isRunning() && !disappearFinished) {
                        animDateDisappear.pause();
                    }

                    /* Scroll status is stopped */
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isScrolling = false;
                    // If the animation is paused and the srcoll has stopped, we start the animation to disappear the tvDate
                    if (animDateDisappear.isPaused()) {
                        animDateDisappear.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                appearFinished = false;
                                disappearFinished = true;
                            }
                        });
                        animDateDisappear.start();
                    }
                    // If the animation of tvDate has not started to disappear, we run it
                    if (appearFinished && !animDateDisappear.isRunning()) {
                        animDateDisappear.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                appearFinished = false;
                                disappearFinished = true;
                            }
                        });
                        animDateDisappear.start();
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItems = linearLayoutManager.getItemCount(); // Total items
                scrollOutItems = totalItems - linearLayoutManager.findFirstCompletelyVisibleItemPosition(); // How many items have you displaced
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition(); // Position of the last item visible

                /* Go loading batches of items so as not to slow down the app */
                if (!endOfScroll && isScrolling && (totalCursor > limitItemsToScroll) && (scrollOutItems == totalItems)) {
                    // Data fetch
                    isScrolling = false;
                    fetchData(false, cursor);
                }

                /* This section shows a button to navigate down the recyclerview. It is calculated for each new item that appears on the screen */
                if (oldLastVisibleItem != lastVisibleItem) {
                    oldLastVisibleItem = lastVisibleItem;
                    if ((lastVisibleItem == totalItems - 1) && (flBtnDownRecycler.getVisibility() == View.VISIBLE)) { // Make the conversation scroll button invisible and reset the badge
                        flBtnDownRecycler.setVisibility(View.INVISIBLE);
                        flBtnDownRecycler.startAnimation(animBtnDownDisappear);
                        badgeMessages = 0;
                        tvBadge.setVisibility(View.INVISIBLE);
                    } else if ((lastVisibleItem <= totalItems - 2) && (flBtnDownRecycler.getVisibility() != View.VISIBLE)) { // Make the conversation scroll button visible
                        flBtnDownRecycler.setVisibility(View.VISIBLE);
                        flBtnDownRecycler.startAnimation(animBtnDownAppear);
                    }
                }

                /* We load a drop-down animation of the date of the messages. It is calculated for each new item that appears on the screen */
                firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                firstCompletelyVisibleItem = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
                View view = linearLayoutManager.findViewByPosition(firstVisibleItem);
                TextView date = view.findViewById(R.id.tvDate);
                tvDate.setText(date.getText().toString());

                if (oldVisibleItem != firstVisibleItem && !newMessage) {
                    oldVisibleItem = firstVisibleItem;
                    if (!hasKeyboardStateChanged()) {
                        if (!tvDate.getText().toString().equals(getResources().getString(R.string.today).toUpperCase())) {
                            if (!animDateAppear.isRunning() && (!appearFinished || disappearFinished)) {
                                disappearFinished = false;
                                animDateAppear.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        appearFinished = true;
                                        if (!isScrolling) {
                                            animDateDisappear.addListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animation) {
                                                    super.onAnimationEnd(animation);
                                                    appearFinished = false;
                                                    disappearFinished = true;
                                                }
                                            });
                                            animDateDisappear.start();
                                        }
                                    }
                                });
                                animDateAppear.start();
                            }
                        } else {
                            if (animDateAppear.isStarted()) {
                                animDateAppear.cancel();
                            }
                            animDateDisappear.end();
                            animDateDisappear.cancel();
                            appearFinished = false;
                            disappearFinished = true;
                        }
                    } else {
                        if (tvDate.getText().toString().equals(getResources().getString(R.string.today).toUpperCase())) {
                            if (animDateAppear.isStarted()) {
                                animDateAppear.cancel();
                            }
                            animDateDisappear.end();
                            animDateDisappear.cancel();
                            appearFinished = false;
                            disappearFinished = true;
                        }
                    }
                } else {
                    newMessage = false;
                }
            }
        });

        /* This listener controls the button that takes you to the end of the conversation */
        flBtnDownRecycler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* We scroll down the conversation and make the scroll button invisible */
                rvChat.scrollToPosition(alMessage.size() - 1);
                badgeMessages = 0;
                tvBadge.setVisibility(View.INVISIBLE);
            }
        });

        /* Here a new message is sent and the textView with the number of unread messages is deleted */
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* We send the message */
                if (!etMessage.getText().toString().equals("")) {
                    sendMessage(etMessage.getText().toString().trim(), 1);

                    /* This block of code is responsible for removing the textView with the number of unread messages */
                    if (positionPendingMessages != -1) {
                        itemWithPendingMessages.setPendingMessages(0);
                        alMessage.set(positionPendingMessages, itemWithPendingMessages);
                        adapter.notifyItemChanged(positionPendingMessages);
                        positionPendingMessages = -1;
                    }
                }
            }
        });

        /* We close the activity */
        lyProfileBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
                        // The person with whom I communicate, this is util for receive pending messages in the server.
                        jsonObjectData.put("receiverId", receiverId);
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
                    JSONArray messages = (JSONArray) args[0];
                    try {
                        if (messages != null) {
                            int pendingMessages = 0;
                            for (int i = 0; i < messages.length(); i++) {
                                JSONObject jsonObject = messages.getJSONObject(i);
                                String message = jsonObject.getString("message");
                                String mongoTime = jsonObject.getString("time");
                                String time = formatMongoTime(mongoTime);
                                int channel = 2;

                                /* We collect the number of messages to pass it to the item. For the first message, we mark the new messages with the number of messages to read */
                                if (i == 0) {
                                    pendingMessages = messages.length();
                                } else {
                                    pendingMessages = 0;
                                }

                                /* If it is the beginning of the conversation, we mark that it's the last item, so that the adapter shows the date of the first message */
                                if (!dataBaseHelper.thereIsConversation(receiverId)) {
                                    adapter.isLastItem();
                                }

                                /* We save the message in the local database and collect the date and the message id to compose a new item */
                                values = dataBaseHelper.storeMessage(receiverId, receiverUsername, message, channel, time); // We store the message into the local data base and we obtain the id and time from the record stored
                                String id = values.get("id");
                                String dateTime = values.get("time"); // Comes from local database when saving the message
                                addMessage(id, message, channel, formatTime(dateTime, context), formatDate(dateTime, context), pendingMessages); // We add a new item to the adapter

                                /* This is to show the textView with the number of unread messages.
                                If it is the first message, we capture the current position of alMessage and create a new item with the appropriate data */
                                if (i == 0) {
                                    positionPendingMessages = alMessage.size() - 1;
                                    itemWithPendingMessages = new MessageItem(id, message, channel, formatTime(dateTime, context), formatDate(dateTime, context), pendingMessages);
                                }
                            }
                            // We move the RecyclerView to the message of pending messages
                            linearLayoutManager.scrollToPositionWithOffset(adapter.getItemCount() - messages.length(), 280);
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
                    String username = "";
                    String message = "";
                    int channel = 2;

                    try {
                        username = data.getString("username");
                        message = data.getString("content");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    /* If it is the beginning of the conversation, we mark that it's the last item, so that the adapter shows the date of the first message */
                    if (!dataBaseHelper.thereIsConversation(receiverId)) {
                        adapter.isLastItem();
                    }

                    /* We save the message in the local database and collect the date and the message id to compose a new item */
                    values = dataBaseHelper.storeMessage(receiverId, receiverUsername, message, channel, ""); // We store the message into the local data base and we obtain the id and time from the record stored
                    String id = values.get("id");
                    String dateTime = values.get("time"); // Comes from local database when saving the message
                    addMessage(id, message, channel, formatTime(dateTime, context), formatDate(dateTime, context), 0); // We add a new item to the adapter

                    /* This code block is responsible for adding one more message to the textView with the number of unread messages.
                    To do this, if the variable positionPendingMessages contains the position in which the textView of unread messages is displayed, we increase the number the value of unread messages */
                    if (positionPendingMessages != -1) {
                        int count = itemWithPendingMessages.getPendingMessages();
                        itemWithPendingMessages.setPendingMessages(count + 1);
                        alMessage.set(positionPendingMessages, itemWithPendingMessages);
                        adapter.notifyItemChanged(positionPendingMessages);
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
                    tvTyping.setVisibility(View.VISIBLE);
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
                    tvTyping.setVisibility(View.GONE);
                }
            });
        }
    };

    /* After a while without writing we launch stop-typing event */
    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!typing) return;

            JSONObject jsonObjectData = new JSONObject();
            try {
                jsonObjectData.put("receiverId", receiverId);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            typing = false;
            socket.emit("stop-typing", jsonObjectData);
        }
    };


    /**
     * OVERRIDE
     **/

    @Override
    protected void onResume() {
        super.onResume();
        tvTyping.setVisibility(View.GONE);

        if (!socket.connected()) {
            try {
                socket = IO.socket(getResources().getString(R.string.url_chat));
            } catch (URISyntaxException e) {
                Log.d("Error", "Error socketURL: " + e.toString());
            }
            socket.connect();
            socket.on("connected", connected);
            socket.on("pending-messages", getPendingMessages);
            socket.on("send-message", listeningMessages);
            socket.on("typing", onTyping);
            socket.on("stop-typing", onStopTyping);
        }

        if(FCM.numNotificationsActive(context) > 0){
            int id = FCM.getNotificationId(receiverUsername);
            if (FCM.isNotificationActive(id, context)) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context); // Create notification manager
                if (FCM.numNotificationsActive(context) == 1) { // If there is only one user notification, we also cancel the summary notification
                    notificationManager.cancel(FCM.SUMMARY_NOTIFICATION_ID);
                    notificationManager.cancel(id);
                    FCM.mapNotificationIds.remove(receiverUsername);
                    FCM.mapMessages.remove(receiverUsername);
                } else {
                    notificationManager.cancel(id);
                    FCM.mapNotificationIds.remove(receiverUsername);
                    FCM.mapMessages.remove(receiverUsername);
                    FCM.updateMessageNotification(context, FCM.mapMessages);
                }
            } else {
                if (FCM.numNotificationsActive(context) > 1) {
                    FCM.mapNotificationIds.remove(receiverUsername);
                    FCM.mapMessages.remove(receiverUsername);
                    FCM.updateMessageNotification(context, FCM.mapMessages);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disconnect the sockets so that the server send a notification for new messages
        socket.emit("disconnect", "");
        socket.disconnect();
        // We remove the textView in the item that contains the number of unread messages
        if (positionPendingMessages != -1) {
            itemWithPendingMessages.setPendingMessages(0);
            alMessage.set(positionPendingMessages, itemWithPendingMessages);
            adapter.notifyItemChanged(positionPendingMessages);
            positionPendingMessages = -1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.emit("disconnect", "");
        socket.disconnect();
    }

    /**
     * FUNCTIONS
     **/

    /* All the view declarations, class assignments and SharedPreferences data retrieval are here */
    public void setStatements() {
        /* Set views ids */
        rootView = findViewById(R.id.rootView);
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        lyCiruclarProgressBar = findViewById(R.id.lyCiruclarProgressBar);
        flBtnDownRecycler = findViewById(R.id.flBtnDownRecycler);
        tvDate = findViewById(R.id.tvDate);
        tvBadge = findViewById(R.id.tvBadge);
        lyProfileBack = findViewById(R.id.lyProfileBack);
        tvUsername = findViewById(R.id.tvUsername);
        tvTyping = findViewById(R.id.tvTyping);

        /* Animations */
        animDateAppear = ObjectAnimator.ofFloat(tvDate, "translationY", 0f, 110f);
        animDateAppear.setInterpolator(new DecelerateInterpolator());
        animDateAppear.setDuration(200);
        animDateDisappear = ObjectAnimator.ofFloat(tvDate, "translationY", 110f, 0f);
        animDateDisappear.setInterpolator(new DecelerateInterpolator());
        animDateDisappear.setStartDelay(1200);
        animDateDisappear.setDuration(300);

        animBtnDownAppear = AnimationUtils.loadAnimation(this, R.anim.loading_wheel_appear);
        animBtnDownDisappear = AnimationUtils.loadAnimation(this, R.anim.loading_wheel_disappear);

        loadingWheel = new LoadingWheel(context, lyCiruclarProgressBar); // To show a wheel loading

        /* We initialize the recyclerview with its adapter */
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(linearLayoutManager);
        adapter = new MessagesAdapter(getApplicationContext(), alMessage);
        rvChat.setAdapter(adapter);

        /* We get our username, userId and the receiverId receiverUsername to whom we are going to send the messages */
        userId = Preferences.getUserId(context);
        username = Preferences.getUserName(context);
        receiverId = getIntent().getStringExtra("receiverId");
        receiverUsername = getIntent().getStringExtra("receiverUsername"); // We capture the username and id of the previous activity

        /* Put the receiver's username in the title */
        tvUsername.setText(receiverUsername);
    }

    /* Returns if the keyboard has appeared or been hidden since the last time */
    public boolean hasKeyboardStateChanged() {
        boolean hasChanged = false;
        int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
        if (heightDiff != previousHeightDiff) {
            hasChanged = true;
            previousHeightDiff = heightDiff;
        }
        return hasChanged;
    }

    public String formatMongoTime(String mongoTime) {
        String[] parts = mongoTime.split("T");
        String date = parts[0];
        String time = parts[1];

        String[] partsOfTime = time.split("\\.");
        String realTime = partsOfTime[0];

        return date + " " + realTime;
    }

    /* Function that loads the cursor data into the ArrayList */
    public void loadItems(@NonNull Cursor c) {
        alMessage.add(new MessageItem(
                c.getString(c.getColumnIndex("id")),
                c.getString(c.getColumnIndex("message")),
                c.getInt(c.getColumnIndex("channel")),
                Functions.formatTime(c.getString(c.getColumnIndex("time")), context),
                Functions.formatDate(c.getString(c.getColumnIndex("time")), context),
                0
        ));
    }

    /* Function that depending on whether there is a lot of data, loads the data in batches */
    public void fetchData(@NonNull Boolean firstLoad, final Cursor c) {
        if (firstLoad) {
            totalCursor = c.getCount();
            if (totalCursor > limitItemsToScroll) { // We exceed the limit set by the developer to start loading items in batches
                c.moveToPosition(totalCursor - itemsToShow);
                for (int i = 0; i < itemsToShow; i++) {
                    loadItems(c);
                    c.moveToNext();
                }
                indexItems = totalCursor - itemsToShow;
            } else { // We do not exceed the limit set by the developer. Therefore, we load the data normally
                while (c.moveToNext()) {
                    loadItems(c);
                    if (c.isLast()) {
                        adapter.isLastItem();
                    }
                }
            }
            adapter.notifyDataSetChanged();
            rvChat.scrollToPosition(adapter.getItemCount() - 1);
        } else {
            loadingWheel.setLoading(true); // We show a wheel loading
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (indexItems - nextItemsToShow <= 0) { // We have reached the end of batch loading
                        c.moveToFirst();
                        ArrayList<MessageItem> alMessageAux = new ArrayList<MessageItem>();
                        for (int i = 0; i < indexItems; i++) {
                            if (i == indexItems - 1) {
                                adapter.isLastItem();
                            }
                            alMessageAux.add(new MessageItem(
                                    c.getString(c.getColumnIndex("id")),
                                    c.getString(c.getColumnIndex("message")),
                                    c.getInt(c.getColumnIndex("channel")),
                                    Functions.formatTime(c.getString(c.getColumnIndex("time")), context),
                                    Functions.formatDate(c.getString(c.getColumnIndex("time")), context),
                                    0
                            ));
                            c.moveToNext();
                        }
                        endOfScroll = true;
                        alMessage.addAll(0, alMessageAux);
                        c.close();
                        adapter.notifyItemRangeInserted(0, alMessageAux.size());
                        loadingWheel.setLoading(false);
                    } else {  // We continue loading batches of items
                        c.moveToPosition(indexItems - nextItemsToShow);
                        ArrayList<MessageItem> alMessageAux = new ArrayList<MessageItem>();
                        for (int i = 0; i < nextItemsToShow; i++) {
                            alMessageAux.add(new MessageItem(
                                    c.getString(c.getColumnIndex("id")),
                                    c.getString(c.getColumnIndex("message")),
                                    c.getInt(c.getColumnIndex("channel")),
                                    Functions.formatTime(c.getString(c.getColumnIndex("time")), context),
                                    Functions.formatDate(c.getString(c.getColumnIndex("time")), context),
                                    0
                            ));
                            c.moveToNext();
                        }
                        alMessage.addAll(0, alMessageAux);
                        indexItems = indexItems - nextItemsToShow;
                        adapter.notifyItemRangeInserted(0, alMessageAux.size());
                        loadingWheel.setLoading(false);
                    }
                }
            }, 200); // It is important to put a delay so that when you scroll very fast it does not get blocked
        }
    }

    /* Save a message in the local database, update the adapter and send the message to the addressee */
    public void sendMessage(String message, int channel) {
        etMessage.setText("");
        if (!dataBaseHelper.thereIsConversation(receiverId)) {
            adapter.isLastItem();
        }
        values = dataBaseHelper.storeMessage(receiverId, receiverUsername, message, channel, ""); // We store the message into the local data base and we obtain the id and time from the record stored
        String id = values.get("id");
        String dateTime = values.get("time");
        addMessage(id, message, channel, formatTime(dateTime, context), formatDate(dateTime, context), 0); // Add message from the sender to the activity
        JSONObject jsonObjectData = new JSONObject(); // We prepare a jsonObject to send the message to the server
        try {
            jsonObjectData.put("userId", receiverId); // Who is going to receive the message (receiver)
            jsonObjectData.put("username", receiverUsername);
            jsonObjectData.put("senderId", userId); // Who send the message (sender)
            jsonObjectData.put("senderUsername", username);
            jsonObjectData.put("content", message); // The message
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("private-message", jsonObjectData); // Here we must check if the receiving user is active or not to leave a pending message
    }

    /* Returns if the keyboard has appeared or been hidden since the last time */
    public void addMessage(String id, String message, int channel, String time, String date, int pendingMessages) {
        newMessage = true; // Boolean to prevent tvDate animation appear
        alMessage.add(adapter.getItemCount(), new MessageItem(
                id,
                message,
                channel,
                time,
                date,
                pendingMessages
        ));
        adapter.notifyDataSetChanged();

        /* We update the number of messages on the button to navigate down the conversation */
        if (flBtnDownRecycler.getVisibility() == View.VISIBLE) {
            if (channel == 1) {
                rvChat.scrollToPosition(adapter.getItemCount() - 1); // We move to the end of the conversation
            } else {
                // We update the number of messages on the badge
                badgeMessages++;
                if (badgeMessages > 999) {
                    badgeMessages = 999;
                }
                tvBadge.setText(String.valueOf(badgeMessages));
                tvBadge.setVisibility(View.VISIBLE);
            }
        } else {
            rvChat.scrollToPosition(adapter.getItemCount() - 1);
        }
    }
}

