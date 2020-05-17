/**
 * 1,2,3,4,5,6,7,8,9,10,11,12,13,14
 */
package com.quarks.android.Login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.quarks.android.R;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.Preferences;
import com.quarks.android.Utils.QueueVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WelcomeActivity extends AppCompatActivity {

    private TextView tvUsername, tvChangeUsername, tvConnError;
    private Button btnNext;

    private Context context = WelcomeActivity.this;
    private Map<String, String> params = new HashMap<String, String>();
    private HashMap<String, String> headers = new HashMap<String, String>();
    private JsonArrayRequest jsonArrayRequest;
    private QueueVolley queueVolley;

    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        /**  STATEMENTS **/

        tvUsername = findViewById(R.id.tvUsername);
        tvChangeUsername = findViewById(R.id.tvChangeUsername);
        btnNext = findViewById(R.id.btnNext);
        tvConnError = findViewById(R.id.tvConnError);

        final String email = getIntent().getStringExtra("EMAIL"); // We capture the email that comes from the previous activity
        final String username = getIntent().getStringExtra("USERNAME"); // We capture the username that comes from the previous activity
        final String pass = getIntent().getStringExtra("PASS"); // We capture the password that comes from the previous activity

        /**  DESIGN  **/

        /* Status Bar */
        Functions.changeStatusBarColor(WelcomeActivity.this, R.color.bg_negro);

        /** CODE  **/

        tvUsername.setText(username);

        tvChangeUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, UsernameActivity.class);
                intent.putExtra("EMAIL", email);
                intent.putExtra("USERNAME", username);
                intent.putExtra("PASS", pass);
                WelcomeActivity.this.startActivity(intent);
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Register user and launch PhotoActivity
                AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                dialog = builder.create();
                Functions.showDialogRegistering(dialog);

                headers.put("Accept", "application/json");
                queueVolley = QueueVolley.getInstance(context);
                jsonArrayRequest = jarSignin(tvUsername.getText().toString().trim(), email, pass);
                queueVolley.RequestQueueAdd(jsonArrayRequest);
            }
        });
    }

    /**
     * METHODS
     **/

    @Override
    protected void onResume() {
        super.onResume();
        // We show the interface without showing errors
        tvConnError.setVisibility(View.GONE);
    }

    /**
     * VOLLEY
     **/

    private JsonArrayRequest jarSignin(final String username, final String email, final String pass) {
        params.clear();
        params.put("username", username);
        params.put("email", email);
        params.put("password", pass);
        params.put("fcmToken", Preferences.getFCMToken(context));

        JSONArray jsonArrayParams = new JSONArray();
        jsonArrayParams.put(new JSONObject(params));

        String url = getResources().getString(R.string.url_signin);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArrayParams,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray respuesta) {
                        try {
                            int code = 0;
                            String id = "";
                            for (int i = 0; i < respuesta.length(); i++) {
                                JSONObject jsonObject = respuesta.getJSONObject(i);
                                code = jsonObject.getInt("code");
                                id = jsonObject.getString("id");
                            }
                            if (code == 200) {
                                // We keep essential data user
                                Preferences.setUserId(context, id);
                                Preferences.setUserEmail(context, email);
                                Preferences.setUserPass(context, pass);
                                Preferences.setUserName(context, username);

                                // We go to the next Activity
                                Intent intent = new Intent(context, PhotoActivity.class);
                                context.startActivity(intent);
                            } else if (code == 409) { // Username already exists
                                String error = getResources().getString(R.string.error_other_account_using_email) + " " + email; // Show error banner. Email is already in use
                                tvConnError.setText(error);
                                tvConnError.setVisibility(View.VISIBLE);
                                dialog.dismiss();
                            } else if (code == 410) { // Email already exists
                                String error = getResources().getString(R.string.error_other_account_using_email) + " " + email; // Show error banner. Email is already in use
                                tvConnError.setText(error);
                                tvConnError.setVisibility(View.VISIBLE);
                                dialog.dismiss();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Toast.makeText(LoginActivity.this,error.toString(),Toast.LENGTH_LONG).show();
                        Log.d("volley", error.toString());
                        //dialog.dismiss();
                        if (error instanceof TimeoutError || error instanceof NoConnectionError) { // This indicates that the request has either time out or there is no connection
                            tvConnError.setVisibility(View.VISIBLE);
                            dialog.dismiss();
                        } else if (error instanceof AuthFailureError) { // Error indicating that there was an Authentication Failure while performing the request
                        } else if (error instanceof ServerError) {  // Indicates that the server responded with a error response
                            tvConnError.setVisibility(View.VISIBLE);
                            dialog.dismiss();
                            if (error.networkResponse.statusCode == 409) {
                                String err = getResources().getString(R.string.error_other_account_using_username) + " " + username;
                                tvConnError.setText(err);
                                tvConnError.setVisibility(View.VISIBLE);
                            }
                            if (error.networkResponse.statusCode == 400) {
                                String err = getResources().getString(R.string.error_invalid_application);
                                tvConnError.setText(err);
                                tvConnError.setVisibility(View.VISIBLE);
                            }
                        } else if (error instanceof NetworkError) {  // Indicates that there was network error while performing the request
                            tvConnError.setVisibility(View.VISIBLE);
                            dialog.dismiss();
                        } else if (error instanceof ParseError) {   // Indicates that the server response could not be parsed
                        }
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }
        };
        return jsonArrayRequest;
    }
}
