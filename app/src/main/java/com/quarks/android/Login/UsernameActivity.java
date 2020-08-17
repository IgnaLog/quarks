/**
 *
 */
package com.quarks.android.Login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

public class UsernameActivity extends AppCompatActivity {

    private EditText etUsername;
    private TextView tvError, tvConnError;
    private ViewGroup rootView;
    private View vpbNext;

    private Context context = UsernameActivity.this;
    private Map<String, String> params = new HashMap<String, String>();
    private HashMap<String, String> headers = new HashMap<String, String>();
    private JsonArrayRequest jsonArrayRequest;
    private QueueVolley queueVolley;
    private Boolean existsUsername;

    private String email = "";
    private String username = "";
    private String pass = "";

    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username);

        /**  STATEMENTS  **/

        rootView = findViewById(R.id.constraintLayout);
        etUsername = findViewById(R.id.etUsername);
        tvError = findViewById(R.id.tvError);
        tvConnError = findViewById(R.id.tvConnError);
        vpbNext = findViewById(R.id.myProgressButton);

        /**  DESIGN  **/

        /* Status Bar */
        Functions.changeStatusBarColor(UsernameActivity.this, R.color.bg_negro);

        /* adjustResize with slow transition - set the android parameter: animateLayoutChanges = "true" in the layout */
        Functions.setLayoutTransition(rootView);

        /* Remove TextView Error Connection when click in the Username Edittext*/
        etUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConnError.setVisibility(View.GONE);
            }
        });

        /**  CODE  **/

        email = getIntent().getStringExtra("EMAIL"); // We capture the email that comes from the previous activity
        username = getIntent().getStringExtra("USERNAME"); // We capture the username that comes from the previous activity
        pass = getIntent().getStringExtra("PASS"); // We capture the password that comes from the previous activity

        etUsername.setText(username);

        /* Activate the enter button if the editText contain something */
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Functions.setTvError(tvError, etUsername, "", false); // If when making any change in the text there is an error we remove it
                tvConnError.setVisibility(View.GONE);
                etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

                if (etUsername.getText().toString().trim().length() > 0) { // If the EditText username contains something, activate the Next button
                    vpbNext.setEnabled(true);
                }
                if (etUsername.getText().toString().trim().length() == 0) { // If the EditText username contains nothing
                    etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null); // We remove the EditText check
                    vpbNext.setEnabled(false);
                }

                /* We launched Volley to verify the existence of that username */
                headers.put("Accept", "application/json");
                queueVolley = QueueVolley.getInstance(UsernameActivity.this);
                jsonArrayRequest = jarExistsUsername(etUsername.getText().toString().trim());
                queueVolley.RequestQueueAdd(jsonArrayRequest);

            }
        };
        etUsername.addTextChangedListener(tw);


        vpbNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConnError.setVisibility(View.GONE);
                if (tvError.getVisibility() != View.VISIBLE) { // Firstly, we check if there is not a error, in this case do nothing
                    Functions.closeKeyboard(UsernameActivity.this); // Close keyboard and show the registering... dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(UsernameActivity.this);
                    dialog = builder.create();
                    Functions.showDialogRegistering(dialog);

                    /* We launch a Volley that verify the existence of that username(this is because the internet may have disconnected), after registration is done */
                    headers.put("Accept", "application/json");
                    queueVolley = QueueVolley.getInstance(context);
                    jsonArrayRequest = jarSignin(etUsername.getText().toString().trim(), email, pass);
                    queueVolley.RequestQueueAdd(jsonArrayRequest);
                }
            }
        });
    }

    /**
     * METHODS
     **/

    @Override
    protected void onResume() {
        super.onResume();
        // We leave the interface with the loading button in its initial position and without showing errors
        Functions.setTvError(tvError, etUsername, "", false);
        tvConnError.setVisibility(View.GONE);
    }

    /**
     * VOLLEY
     **/

    private JsonArrayRequest jarExistsUsername(String username) {
        params.clear();
        params.put("username", username);

        JSONArray jsonArrayParams = new JSONArray();
        jsonArrayParams.put(new JSONObject(params));

        String url = getResources().getString(R.string.url_exists_username);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArrayParams,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                existsUsername = jsonObject.getBoolean("exists");
                            }
                            if (existsUsername) { // Show error that this email is already used
                                Functions.setTvError(tvError, etUsername, getResources().getString(R.string.exists_username), true);
                                etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                            } else { // If it does not exist, we show the green check
                                etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(UsernameActivity.this, R.drawable.ic_correct_tick), null);
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
                        etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }
        };
        return jsonArrayRequest;
    }

    private JsonArrayRequest jarSignin(final String username, final String email, final String pass) {
        params.clear();
        params.put("username", username);
        params.put("email", email);
        params.put("password", pass);

        JSONArray jsonArrayParams = new JSONArray();
        jsonArrayParams.put(new JSONObject(params));

        String url = getResources().getString(R.string.url_signin);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArrayParams,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            int code = 0;
                            String id = "";
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
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
                                Functions.setTvError(tvError, etUsername, getResources().getString(R.string.exists_username), true);
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
                        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                            tvConnError.setVisibility(View.VISIBLE);
                            dialog.dismiss();
                        } else if (error instanceof AuthFailureError) {
                        } else if (error instanceof ServerError) {
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
                        } else if (error instanceof NetworkError) {
                            tvConnError.setVisibility(View.VISIBLE);
                            dialog.dismiss();
                        } else if (error instanceof ParseError) {
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
