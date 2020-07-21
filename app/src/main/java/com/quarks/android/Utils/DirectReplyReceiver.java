package com.quarks.android.Utils;

import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.quarks.android.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class DirectReplyReceiver extends BroadcastReceiver {

    private Socket socket;
    private CharSequence replyText;
    private String receiverId, receiverUsername, userId, username;
    private Map<String, String> values = new HashMap<String, String>();
    private DataBaseHelper dataBaseHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        dataBaseHelper = new DataBaseHelper(context);

        if (remoteInput != null) {
            replyText = remoteInput.getCharSequence("key_text_reply");
            receiverId = remoteInput.getString("receiverId");
            receiverUsername = remoteInput.getString("receiverUsername");
            userId = remoteInput.getString("userId");
            username = remoteInput.getString("username");

            try {
                socket = IO.socket(context.getResources().getString(R.string.url_chat));
            } catch (URISyntaxException e) {
                Log.d("Error", "Error socketURL: " + e.toString());
            }
            socket.connect();
            socket.on("connected", connected);
        }
    }

    /* We have been connected, so we send the reply text */
    private Emitter.Listener connected = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject jsonObjectData = new JSONObject();
            try {
                jsonObjectData.put("userId", receiverId); // Who is going to receive the message (receiver)
                jsonObjectData.put("username", receiverUsername);
                jsonObjectData.put("senderId", userId); // Who send the message (sender)
                jsonObjectData.put("senderUsername", username);
                jsonObjectData.put("content", replyText); // The message
            } catch (JSONException e) {
                e.printStackTrace();
            }

            socket.emit("direct-reply-message", jsonObjectData);

            /* We save the message in the local database and collect the date to compose the conversations */
            values = dataBaseHelper.storeMessage(receiverId, receiverUsername, String.valueOf(replyText), 1, "", 0); // We store the message into the local data base and we obtain the id and time from the record stored
            String dateTime = values.get("time");

            // We updated the conversations table to use it in the conversations activity
            dataBaseHelper.updateConversations(receiverId, receiverUsername, String.valueOf(replyText), dateTime, 0);


            socket.emit("disconnect", "");
            socket.disconnect();
        }
    };
}
