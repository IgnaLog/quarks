package com.quarks.android.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.quarks.android.Interfaces.MessagesNotSentInterface;
import com.quarks.android.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;


import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class checkNetworkReceiver extends BroadcastReceiver {

    private DataBaseHelper dataBaseHelper;
    private Socket socket;
    private Context mContext;
    private JSONObject jsonObject;
    private MessagesNotSentInterface dtInterface;


    @Override
    public void onReceive(Context context, final Intent intent) {
        dtInterface = (MessagesNotSentInterface) context;

        if (Functions.isNetworkAvailable(context)) {
            dataBaseHelper = new DataBaseHelper(context);
            Cursor c = dataBaseHelper.getMessagesNotSent();
            mContext = context;
            jsonObject = getMessagesNotSent(context, c);

            try {
                socket = IO.socket(context.getResources().getString(R.string.url_chat));
            } catch (URISyntaxException e) {
                Log.d("Error", "Error socketURL: " + e.toString());
            }
            socket.connect();
            socket.on("connected", connected);
        }
    }

    /* We have been connected, so we send the messages not sent */
    private Emitter.Listener connected = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            if (Functions.isNetworkAvailable(mContext) && socket.connected()) {
                socket.emit("messages-not-sent", jsonObject, new Ack() {
                    @Override
                    public void call(Object... args) {
                        //JSONObject success = (JSONObject) args[0];
                        //
                        dataBaseHelper.updateMessagesNotSent();
                        //
                        dtInterface.updateMessagesNotSent();
                        /* Disconnecting the socket */
                        socket.emit("disconnect", "");
                        socket.disconnect();
                    }
                });
            }
        }
    };


    private JSONObject getMessagesNotSent(Context context, Cursor c){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("senderId", Preferences.getUserId(context));
            jsonObject.put("senderUsername", Preferences.getUserName(context));

            JSONArray jsonArrayChats = new JSONArray();
            while(c.moveToNext()){
                String userId = c.getString(c.getColumnIndex("sender_id"));

                if (c.isFirst()) {
                    JSONObject jsonObjectChat = new JSONObject();
                    jsonObjectChat.put("userId", c.getString(c.getColumnIndex("sender_id")));  // Who is going to receive the message (receiver)
                    jsonObjectChat.put("username", c.getString(c.getColumnIndex("sender_username")));

                    JSONArray jsonArrayMessages = new JSONArray();
                    JSONObject jsonObjectData = new JSONObject();
                    jsonObjectData.put("content", c.getString(c.getColumnIndex("message"))); // The message
                    jsonObjectData.put("contentId", c.getString(c.getColumnIndex("id")));
                    jsonArrayMessages.put(jsonObjectData);

                    jsonObjectChat.put("messages", jsonArrayMessages);

                    jsonArrayChats.put(jsonObjectChat);
                } else {
                    c.moveToPrevious();
                    String previousUserId = c.getString(c.getColumnIndex("sender_id"));
                    c.moveToNext();

                    if (previousUserId.equals(userId)) {
                        JSONObject jsonObjectChatAux = jsonArrayChats.getJSONObject(jsonArrayChats.length() - 1);
                        JSONArray jsonArrayMessagesAux = jsonObjectChatAux.getJSONArray("messages");
                        JSONObject jsonObjectData = new JSONObject();
                        jsonObjectData.put("content", c.getString(c.getColumnIndex("message"))); // The message
                        jsonObjectData.put("contentId", c.getString(c.getColumnIndex("id")));
                        jsonArrayMessagesAux.put(jsonObjectData);
                    } else {
                        JSONObject jsonObjectChat = new JSONObject();
                        jsonObjectChat.put("userId", c.getString(c.getColumnIndex("sender_id")));  // Who is going to receive the message (receiver)
                        jsonObjectChat.put("username", c.getString(c.getColumnIndex("sender_username")));

                        JSONArray jsonArrayMessages = new JSONArray();
                        JSONObject jsonObjectData = new JSONObject();
                        jsonObjectData.put("content", c.getString(c.getColumnIndex("message"))); // The message
                        jsonObjectData.put("contentId", c.getString(c.getColumnIndex("id")));
                        jsonArrayMessages.put(jsonObjectData);

                        jsonObjectChat.put("messages", jsonArrayMessages);

                        jsonArrayChats.put(jsonObjectChat);
                    }
                }
            }
            jsonObject.put("chats", jsonArrayChats);
        } catch (JSONException e) {
            e.printStackTrace();
        }
       return jsonObject;
    }
}
