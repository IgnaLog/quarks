package com.quarks.android.Utils;

import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.quarks.android.R;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class DirectReplyReceiver extends BroadcastReceiver {

    private Socket socket;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

        if (remoteInput != null) {
            CharSequence replyText = remoteInput.getCharSequence("key_text_reply");


            try {
                socket = IO.socket(context.getResources().getString(R.string.url_chat));
            } catch (URISyntaxException e) {
                Log.d("Error", "Error socketURL: " + e.toString());
            }
            socket.connect();
            socket.on("connected", connected);

//            socket.emit("disconnect", "");
//            socket.disconnect();
        }
    }

    /* We have been connected, so we send our user data */
    private Emitter.Listener connected = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            //  JSONObject data = (JSONObject) args[0];
            System.out.println("holaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
           // JSONObject jsonObjectData = new JSONObject();
//                    try {
//                        // My user
////                        jsonObjectData.put("userId", userId);
////                        jsonObjectData.put("username", username);
////                        // The person with whom I communicate, this is util for receive pending messages in the server.
////                        jsonObjectData.put("receiverId", receiverId);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
         //   socket.emit("add-user", jsonObjectData);
        }
    };
}
