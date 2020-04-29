package com.quarks.android.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "dbquarks";
    private static final int DATABASE_VERSION = 1;

    /* TABLE MESSAGES */
    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SENDER_ID = "sender_id";
    private static final String COLUMN_SENDER_USERNAME = "sender_username";
    private static final String COLUMN_CHANNEL = "channel";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_TIME = "time";
    private static final String DATABASE_CREATE_MESSAGES = "CREATE TABLE " + TABLE_MESSAGES + " ( " +
            COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_SENDER_ID + " VARCHAR(30) NOT NULL, " +
            COLUMN_SENDER_USERNAME + " VARCHAR(30) NOT NULL, " +
            COLUMN_CHANNEL + " INTEGER NOT NULL, " +
            COLUMN_MESSAGE + " TEXT NOT NULL, " +
            COLUMN_TIME + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);";

    /* TABLE CONVERSATIONS */
    private static final String TABLE_CONVERSATIONS = "conversations";
    private static final String COLUMN_CONVER_ID = "id";
    private static final String COLUMN_CONVER_SENDER_ID = "sender_id";
    private static final String COLUMN_CONVER_SENDER_USERNAME = "sender_username";
    private static final String DATABASE_CREATE_CONVERSATIONS = "CREATE TABLE " + TABLE_CONVERSATIONS + " ( " +
            COLUMN_CONVER_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_CONVER_SENDER_ID + " VARCHAR(30) NOT NULL, " +
            COLUMN_CONVER_SENDER_USERNAME + " VARCHAR(30) NOT NULL);";


    public DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        db.execSQL("CREATE INDEX idx_messsages_sender_id ON " + TABLE_MESSAGES + " (" + COLUMN_SENDER_ID + ")");
        db.execSQL("CREATE INDEX idx_conversations_sender_id ON " + TABLE_CONVERSATIONS + " (" + COLUMN_CONVER_SENDER_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // private static final String DATABASE_ALTER_TABLE = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN new_column string;";
        // if (newVersion == 2) {}
    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_MESSAGES);
        db.execSQL(DATABASE_CREATE_CONVERSATIONS);
    }

    /* Function that gets all user conversations */
    public Cursor getAllConversations() {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT * FROM " + TABLE_CONVERSATIONS + ";";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        return cursor;
    }

    /* Function that gets all the messages from a particular sender */
    public Cursor getAllMessages(String senderId) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_SENDER_ID + " = '" + senderId + "';";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        return cursor;
    }

    /* Check if there is a conversation for a senderId */
    public boolean thereIsConversation(String senderId){
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;

        String sql = "SELECT count(*) AS result FROM " + TABLE_CONVERSATIONS + " WHERE " + COLUMN_CONVER_SENDER_ID + " = '" + senderId + "';";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        while (cursor.moveToNext()) {
            count = cursor.getInt(cursor.getColumnIndex("result"));
        }
        return count > 0;
    }

    /* Function that saves a message in the database. Insert a new conversation if it doesn't exist. Returns a Map with the new id of the message entered and its date and time */
    public Map<String, String> storeMessage(String senderId, String senderUsername, String message, int channel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cvMessage = new ContentValues();
        ContentValues cvConversation = new ContentValues();
        Map<String, String> values = new HashMap<String, String>();

        /* Insert a new conversation if it doesn't exist */
        if(!thereIsConversation(senderId)){
            cvConversation.put(COLUMN_CONVER_SENDER_ID, senderId);
            cvConversation.put(COLUMN_CONVER_SENDER_USERNAME, senderUsername);
            db.insert(TABLE_CONVERSATIONS, null, cvConversation);
        }

        cvMessage.put(COLUMN_SENDER_ID, senderId);
        cvMessage.put(COLUMN_SENDER_USERNAME, senderUsername);
        cvMessage.put(COLUMN_MESSAGE, message);
        cvMessage.put(COLUMN_CHANNEL, channel);
        long resultID = db.insert(TABLE_MESSAGES, null, cvMessage);

        String time = getTime(resultID);
        values.put("id", String.valueOf(resultID));
        values.put("time", time);
        return values;
    }

    /* Function that given a message id gets its data time */
    private String getTime(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String time = "";

        String sql = "SELECT " + COLUMN_TIME + " FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_ID + " = " + id + ";";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        if (cursor != null) {
            while (cursor.moveToNext()) { // Iterate to get the time for the corresponding ID
                time = cursor.getString(cursor.getColumnIndex(COLUMN_TIME));
            }
            cursor.close();
        }
        Log.d("TIME", time);
        return time;
    }
}
