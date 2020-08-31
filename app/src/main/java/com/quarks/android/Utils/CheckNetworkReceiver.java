package com.quarks.android.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.quarks.android.ChatActivity;
import com.quarks.android.Interfaces.MessagesNotSentInterface;
import com.quarks.android.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;


import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class CheckNetworkReceiver extends BroadcastReceiver {

    private DataBaseHelper dataBaseHelper;
    private Socket socket;
    private Context mContext;
    private JSONObject jsonObject;
    private MessagesNotSentInterface dtInterface;
    private ArrayList<String> alMessagesIds;

    @Override
    public void onReceive(Context context, final Intent intent) {
        dtInterface = (MessagesNotSentInterface) context;
        mContext = context;

        if (Functions.isNetworkAvailable(context)) {
            dataBaseHelper = new DataBaseHelper(context);
            Cursor cursor = dataBaseHelper.getMessagesNotSent();
            if (cursor.getCount() > 0) {
                jsonObject = getMessagesNotSent(context, cursor);
                cursor.moveToFirst();
                alMessagesIds = getMessagesIds(cursor);

                try {
                    socket = IO.socket(context.getResources().getString(R.string.url_chat));
                } catch (URISyntaxException e) {
                    Log.d("Error", "Error socketURL: " + e.toString());
                }
                socket.connect();
                socket.on("connected", connected);
            }
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
                        dataBaseHelper.updateMessagesNotSent(); // Procedemos a actualizar el estado de los registros de la base de datos a enviados. Es decir, con valor de 1.
                        dtInterface.updateMessagesNotSent(alMessagesIds); // Comunicamos al ChatActivity la actulizacion de los mensajes no enviados para que los actualice en enviados.
                        // Disconnecting the socket
                        socket.emit("disconnect", "");
                        socket.disconnect();
                    }
                });
            }
        }
    };

    /* Funcion que retorna una lista de los id de los mensajes no enviados */
    private ArrayList<String> getMessagesIds(Cursor c) {
        ArrayList<String> alMessagesIds = new ArrayList<>();
        for (int i = 0; i < c.getCount(); i++) {
            c.moveToPosition(i);
            alMessagesIds.add(c.getString(c.getColumnIndex("id")));
        }
        return alMessagesIds;
    }

    /* Funcion que crea un JSONObject con los mensajes no enviados */
    private JSONObject getMessagesNotSent(Context context, Cursor c) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("senderId", Preferences.getUserId(context));
            jsonObject.put("senderUsername", Preferences.getUserName(context));

            JSONArray jsonArrayChats = new JSONArray();
            while (c.moveToNext()) {
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
        System.out.println(jsonObject);
        return jsonObject;
    }
}
