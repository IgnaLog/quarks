package com.quarks.android.Login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.quarks.android.MainActivity;
import com.quarks.android.R;
import com.quarks.android.Utils.Preferences;
import com.quarks.android.Utils.QueueVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {

    private JsonArrayRequest jsonArrayRequest;
    private QueueVolley queueVolley;
    private Context context = SplashActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /** CODE **/

        /* We launched Volley to login */
        queueVolley = QueueVolley.getInstance(context);
        jsonArrayRequest = jarLogin();
        queueVolley.RequestQueueAdd(jsonArrayRequest);
    }

    /**
     * VOLLEY
     **/

    private JsonArrayRequest jarLogin() {

        String url = getResources().getString(R.string.url_login);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray respuesta) {
                        try {
                            int code = 0;
                            for (int i = 0; i < respuesta.length(); i++) {
                                JSONObject jsonObject = respuesta.getJSONObject(i);
                                code = jsonObject.getInt("code");
                            }
                            if (code == 200) { // OK
                                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                                startActivity(intent);
                                overridePendingTransition(0, 0);
                            } else if (code == 404) { // User not found
                                Intent intent = new Intent(SplashActivity.this, StartActivity.class);
                                startActivity(intent);
                            } else if (code == 401) { // KO no valid password
                                Intent intent = new Intent(SplashActivity.this, StartActivity.class);
                                startActivity(intent);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
                        Log.d("volley", error.toString());
                        Intent intent = new Intent(SplashActivity.this, StartActivity.class);
                        startActivity(intent);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String name = Preferences.getUserName(context); // we send the username through preferences
                String pass = Preferences.getUserPass(context); // we send the password through preferences
                String credentials = name + ":" + pass;
                String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headers.put("Accept", "application/json");
                headers.put("Authorization", auth);
                return headers;
            }
        };
        return jsonArrayRequest;
    }
}
