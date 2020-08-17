package com.quarks.android.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
    private static final String COLUMN_SENDER_MESSAGE_ID = "sender_message_id";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_PENDING = "pending";
    private static final String COLUMN_STATUS = "status";
    private static final String DATABASE_CREATE_MESSAGES = "CREATE TABLE " + TABLE_MESSAGES + " ( " +
            COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_SENDER_ID + " VARCHAR(30) NOT NULL, " +
            COLUMN_SENDER_USERNAME + " VARCHAR(30) NOT NULL, " +
            COLUMN_CHANNEL + " INTEGER NOT NULL, " +
            COLUMN_MESSAGE + " TEXT NOT NULL, " +
            COLUMN_SENDER_MESSAGE_ID + " TEXT, " +
            COLUMN_TIME + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            COLUMN_PENDING + " INTEGER NOT NULL, " +
            COLUMN_STATUS + " INTEGER NOT NULL);";


    /* TABLE CONVERSATIONS */
    private static final String TABLE_CONVERSATIONS = "conversations";
    private static final String COLUMN_CONVER_ID = "id";
    private static final String COLUMN_CONVER_SENDER_ID = "sender_id";
    private static final String COLUMN_CONVER_SENDER_USERNAME = "sender_username";
    private static final String COLUMN_CONVER_LAST_MESSAGE = "last_message";
    private static final String COLUMN_CONVER_TIME = "time";
    private static final String COLUMN_CONVER_NEW_MESSAGES = "new_messages";
    private static final String DATABASE_CREATE_CONVERSATIONS = "CREATE TABLE " + TABLE_CONVERSATIONS + " ( " +
            COLUMN_CONVER_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_CONVER_SENDER_ID + " VARCHAR(30) NOT NULL, " +
            COLUMN_CONVER_SENDER_USERNAME + " VARCHAR(30) NOT NULL, " +
            COLUMN_CONVER_LAST_MESSAGE + " TEXT NOT NULL, " +
            COLUMN_CONVER_TIME + " TIMESTAMP NOT NULL, " +
            COLUMN_CONVER_NEW_MESSAGES + " INTEGER NOT NULL);";

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

        String sql = "SELECT * FROM " + TABLE_CONVERSATIONS + " ORDER BY datetime(" + COLUMN_CONVER_TIME + ") DESC;";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        return cursor;
    }

    /* Function that gets a user conversation */
    public Cursor getConversation(String senderId) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT * FROM " + TABLE_CONVERSATIONS + "  WHERE " + COLUMN_CONVER_SENDER_ID + "= '" + senderId + "';";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        return cursor;
    }

    /* Function that gets the last message of a user conversation */
    public Cursor getLastMessageConversation(String senderId) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT " + COLUMN_CONVER_LAST_MESSAGE + " FROM " + TABLE_CONVERSATIONS + "  WHERE " + COLUMN_CONVER_SENDER_ID + "= '" + senderId + "';";
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

    public int getPreviousMessage(String senderId, String id){
        SQLiteDatabase db = this.getReadableDatabase();
        int channel = -1;

        String sql = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_SENDER_ID + "= '" + senderId + "' " +
                "AND " + COLUMN_ID + " < (SELECT " + COLUMN_ID + " FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_ID + " = " + id + ") " +
                "ORDER BY " + COLUMN_ID + " DESC LIMIT 1;";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        if (cursor != null) {
            while (!cursor.isAfterLast()) {
                cursor.moveToFirst();
                channel = cursor.getInt(cursor.getColumnIndex("channel"));
                cursor.close();
            }
        }

        return channel;
    }

    /* Function that leaves pending messages with the value of zero to mark them as read */
    public void updatePendingMessages(String senderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cvMessage = new ContentValues();
        cvMessage.put(COLUMN_PENDING, 0);
        db.update(TABLE_MESSAGES, cvMessage, COLUMN_SENDER_ID + "='" + senderId + "' AND " + COLUMN_PENDING + "=" + 1, null);
    }

    /* Function that gets all the pending messages from a particular sender */
    public Cursor getAllPendingMessages(String senderId) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_SENDER_ID + " = '" + senderId + "' AND " + COLUMN_PENDING + " = 1;";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        return cursor;
    }

    /* Check if there is a conversation for a senderId */
    public boolean thereIsConversation(String senderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;

        String sql = "SELECT count(*) AS result FROM " + TABLE_CONVERSATIONS + " WHERE " + COLUMN_CONVER_SENDER_ID + " = '" + senderId + "';";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        if (cursor != null) {
            while (cursor.moveToNext()) {
                count = cursor.getInt(cursor.getColumnIndex("result"));
            }
            cursor.close();
        }
        return count > 0;
    }

    /* Function that saves a message in the database. Insert a new conversation if it doesn't exist. Returns a Map with the new id of the message entered and its date and time */
    public Map<String, String> storeMessage(String senderId, String senderUsername, String message, String senderMessageId, int channel, String dateTime, int pending, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cvMessage = new ContentValues();
        Map<String, String> values = new HashMap<String, String>();
        long resultID;

        if (dateTime.equals("")) { // Without dateTime
            cvMessage.put(COLUMN_SENDER_ID, senderId);
            cvMessage.put(COLUMN_SENDER_USERNAME, senderUsername);
            cvMessage.put(COLUMN_MESSAGE, message);
            cvMessage.put(COLUMN_SENDER_MESSAGE_ID, senderMessageId);
            cvMessage.put(COLUMN_CHANNEL, channel);
            cvMessage.put(COLUMN_PENDING, pending);
            cvMessage.put(COLUMN_STATUS, status);
            resultID = db.insert(TABLE_MESSAGES, null, cvMessage);
        } else { // With dateTime established
            cvMessage.put(COLUMN_SENDER_ID, senderId);
            cvMessage.put(COLUMN_SENDER_USERNAME, senderUsername);
            cvMessage.put(COLUMN_MESSAGE, message);
            cvMessage.put(COLUMN_SENDER_MESSAGE_ID, senderMessageId);
            cvMessage.put(COLUMN_CHANNEL, channel);
            cvMessage.put(COLUMN_TIME, dateTime);
            cvMessage.put(COLUMN_PENDING, pending);
            cvMessage.put(COLUMN_STATUS, status);
            resultID = db.insert(TABLE_MESSAGES, null, cvMessage);
        }
        String time = getTime(resultID);
        values.put("id", String.valueOf(resultID));
        values.put("time", time);

        return values;
    }

    public void updateConversations(String senderId, String senderUsername, String message, String dateTime, int numNewMessages){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cvConversation = new ContentValues();

        /* Insert a new conversation if it doesn't exist */
        if (!thereIsConversation(senderId)) {
            cvConversation.put(COLUMN_CONVER_SENDER_ID, senderId);
            cvConversation.put(COLUMN_CONVER_SENDER_USERNAME, senderUsername);
            cvConversation.put(COLUMN_CONVER_LAST_MESSAGE, message);
            cvConversation.put(COLUMN_CONVER_TIME, dateTime);
            cvConversation.put(COLUMN_CONVER_NEW_MESSAGES, numNewMessages);
            db.insert(TABLE_CONVERSATIONS, null, cvConversation);
        } else {
            cvConversation.put(COLUMN_CONVER_LAST_MESSAGE, message);
            cvConversation.put(COLUMN_CONVER_TIME, dateTime);
            if (numNewMessages > 0) {
                cvConversation.put(COLUMN_CONVER_NEW_MESSAGES, getNumNewPreviousMessagesConver(senderId) + numNewMessages);
            }
            db.update(TABLE_CONVERSATIONS, cvConversation, COLUMN_CONVER_SENDER_ID + "='" + senderId + "'", null);
        }
    }

    private int getNumNewPreviousMessagesConver(String senderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int totalNewMessages = 0;

        String sql = "SELECT " + COLUMN_CONVER_NEW_MESSAGES + " FROM " + TABLE_CONVERSATIONS + " WHERE " + COLUMN_CONVER_SENDER_ID + " = '" + senderId + "';";
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(sql, null);
        }
        if (cursor != null) {
            cursor.moveToFirst();
            totalNewMessages = cursor.getInt(cursor.getColumnIndex(COLUMN_CONVER_NEW_MESSAGES));
            cursor.close();
        }
        return totalNewMessages;
    }

    public void cleanNewMessagesConversation(String senderId){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cvConversation = new ContentValues();

        if(thereIsConversation(senderId)){
            cvConversation.put(COLUMN_CONVER_NEW_MESSAGES, 0);
            db.update(TABLE_CONVERSATIONS, cvConversation, COLUMN_CONVER_SENDER_ID + "='" + senderId + "'", null);
        }
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
        return time;
    }
}
