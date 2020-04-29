package com.quarks.android;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.quarks.android.Adapters.ConversationsAdapter;
import com.quarks.android.CustomViews.LoadingWheel;
import com.quarks.android.Items.ConversationItem;
import com.quarks.android.Items.MessageItem;
import com.quarks.android.Utils.DataBaseHelper;

import java.util.ArrayList;

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton fabContacts;
    private RecyclerView rvConversations;
    private LoadingWheel loadingWheel;
    private View lyCiruclarProgressBar;
    private Context context = MainActivity.this;

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

        /** DESIGN **/

        /* Status Bar */
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // The color of the icons is adapted, if not white
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_gris)); // Bar color

        /** CODE **/

        cursor = dataBaseHelper.getAllConversations();
        fetchData(true, cursor); // First data load


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
     * METHODS
     **/

    @Override
    public void onBackPressed() {
        moveTaskToBack(true); // Pressing the back button of Android, we would go to the end of the stack of open activities. In this case it would go to the SplashActivity but it is closed, therefore, the app would be minimized
    }

    /* Function that loads the cursor data into the ArrayList */
    private void loadItems(Cursor c) {
        alConversations.add(new ConversationItem(
                "",
                "",
                c.getString(c.getColumnIndex("sender_username")),
                c.getString(c.getColumnIndex("sender_id"))
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
                                    c.getString(c.getColumnIndex("sender_id"))
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
                                    c.getString(c.getColumnIndex("sender_id"))
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