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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.quarks.android.Adapters.MessagesAdapter;
import com.quarks.android.CustomViews.LoadingWheel;
import com.quarks.android.Items.MessageItem;
import com.quarks.android.Utils.DataBaseHelper;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.Preferences;

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
    private TextView tvDate, tvBadge, tvUsername;
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
    private int firstVisibleItem, lastVisibleItem, totalItems, scrollOutItems, totalCursor;
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

        /* We retrieve pending messages from this user and enter them */

        /**  SOCKETS CONNECTIONS  **/

        try {
            socket = IO.socket(getResources().getString(R.string.url_chat));
        } catch (URISyntaxException e) {
            Log.d("Error", "Error socketURL: " + e.toString());
        }
        socket.connect();
        socket.on("connected", connected);
        socket.on("pending-messages", pendingMessages);
        socket.on("send-message", listeningMessages);

        /**  LISTENERS  **/

        rvChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
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
                View view = linearLayoutManager.findViewByPosition(firstVisibleItem);
                TextView date = view.findViewById(R.id.tvDate);
                tvDate.setText(date.getText().toString());

                if (oldVisibleItem != firstVisibleItem) {
                    oldVisibleItem = firstVisibleItem;
                    if (!tvDate.getText().toString().equals(getResources().getString(R.string.today).toUpperCase()) && !hasKeyboardStateChanged()) {
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
                }
            }
        });

        flBtnDownRecycler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* We scroll down the conversation and make the scroll button invisible */
                rvChat.scrollToPosition(alMessage.size() - 1);
                badgeMessages = 0;
                tvBadge.setVisibility(View.INVISIBLE);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!etMessage.getText().toString().equals("")) {
                    sendMessage(etMessage.getText().toString().trim(), 1); // We send the message
                }
            }
        });

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
    private Emitter.Listener pendingMessages = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject messages = (JSONObject) args[0];
                    System.out.println(messages);
                }
            });
        }
    };

    /* We receive messages */
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
                    values = dataBaseHelper.storeMessage(receiverId, receiverUsername, message, channel); // We store the message into the local data base and we obtain the id and time from the record stored
                    String id = values.get("id");
                    String dateTime = values.get("time"); // Proviene de la base de datos local al guardar el registro
                    addMessage(id, message, channel, formatTime(dateTime), formatDate(dateTime, context)); // Add message from the receiver to the activity
                }
            });
        }
    };

    /**
     * OVERRIDE
     **/

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

    /* Function that loads the cursor data into the ArrayList */
    public void loadItems(@NonNull Cursor c) {
        alMessage.add(new MessageItem(
                c.getString(c.getColumnIndex("id")),
                c.getString(c.getColumnIndex("message")),
                c.getInt(c.getColumnIndex("channel")),
                Functions.formatTime(c.getString(c.getColumnIndex("time"))),
                Functions.formatDate(c.getString(c.getColumnIndex("time")), context)
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
                                    Functions.formatTime(c.getString(c.getColumnIndex("time"))),
                                    Functions.formatDate(c.getString(c.getColumnIndex("time")), context)
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
                                    Functions.formatTime(c.getString(c.getColumnIndex("time"))),
                                    Functions.formatDate(c.getString(c.getColumnIndex("time")), context)
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
        values = dataBaseHelper.storeMessage(receiverId, receiverUsername, message, channel); // We store the message into the local data base and we obtain the id and time from the record stored
        String id = values.get("id");
        String dateTime = values.get("time");
        addMessage(id, message, channel, formatTime(dateTime), formatDate(dateTime, context)); // Add message from the sender to the activity
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
    public void addMessage(String id, String message, int channel, String time, String date) {
        alMessage.add(new MessageItem(
                id,
                message,
                channel,
                time,
                date
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

