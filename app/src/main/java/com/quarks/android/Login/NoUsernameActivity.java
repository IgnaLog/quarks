package com.quarks.android.Login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

import static com.quarks.android.Utils.Functions.dpToPx;

public class NoUsernameActivity extends AppCompatActivity {

    private ViewGroup rootView;
    private EditText etUsername;
    private TextView tvError, tvConnError, tvPolitics;

    private Context context = NoUsernameActivity.this;
    private Map<String, String> params = new HashMap<String, String>();
    private HashMap<String, String> headers = new HashMap<String, String>();
    private JsonArrayRequest jsonArrayRequest;
    private QueueVolley queueVolley;
    private Boolean existsUsername;
    private View vpbNext;

    private AlertDialog dialog;

    private String email = "";
    private String pass = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_username);

        /** STATEMENTS **/

        tvPolitics = findViewById(R.id.tvPolitics);
        rootView = findViewById(R.id.constraintLayout);
        etUsername = findViewById(R.id.etUsername);
        tvError = findViewById(R.id.tvError);
        tvConnError = findViewById(R.id.tvConnError);
        vpbNext = findViewById(R.id.myProgressButton);

        /** DESIGN **/

        /* Status Bar */
        Functions.changeStatusBarColor(NoUsernameActivity.this, R.color.bg_negro);

        /* adjustResize with slow transition - set the android parameter: animateLayoutChanges = "true" in the layout */
        Functions.setLayoutTransition(rootView);

        /* When the keyboard appears make certain views disappear */
        final View activityRootView = rootView;
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > dpToPx(NoUsernameActivity.this, 200)) { // if more than 200 dp, it's probably a keyboard...
                    tvPolitics.setVisibility(View.GONE);
                } else {
                    tvPolitics.setVisibility(View.VISIBLE);
                }
            }
        });

        /* Remove TextView Error Connection when click in the Username Edittext*/
        etUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConnError.setVisibility(View.GONE);
            }
        });

        /** CODE **/

        email = getIntent().getStringExtra("EMAIL"); // We capture the email that comes from the previous activity
        pass = getIntent().getStringExtra("PASS"); // We capture the password that comes from the previous activity

        /* When there are changes in the EditText username */
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Functions.setTvError(tvError, etUsername, "", false); // Si al hacer algún cambio en el texto existe algún error lo quitamos
                tvConnError.setVisibility(View.GONE);
                etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

                if (etUsername.getText().toString().trim().length() > 0) { // Si contiene algo el EditText username, activamos el btnSiguiente
                    vpbNext.setEnabled(true);
                }
                if (etUsername.getText().toString().trim().length() == 0) { // Si el EditText username no contiene nada
                    etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null); // Quitamos el check del EditText
                    vpbNext.setEnabled(false);
                }

                /* We launched Volley to verify the existence of that username */
                headers.put("Accept", "application/json");
                queueVolley = QueueVolley.getInstance(NoUsernameActivity.this);
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
                    Functions.closeKeyboard(NoUsernameActivity.this); // Close keyboard and show the registering... dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(NoUsernameActivity.this);
                    dialog = builder.create();
                    Functions.showDialogRegistering(dialog);

                    /* We launch a Volley that verify the existence of that username(this is because the internet may have disconnected), after registration is done */
                    headers.put("Accept", "application/json");
                    queueVolley = QueueVolley.getInstance(NoUsernameActivity.this);
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
     * VOLLEYS
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
                    public void onResponse(JSONArray respuesta) {
                        try {
                            for (int i = 0; i < respuesta.length(); i++) {
                                JSONObject jsonObject = respuesta.getJSONObject(i);
                                existsUsername = jsonObject.getBoolean("exists");
                            }
                            if (existsUsername) { // We show error that this email is already used
                                Functions.setTvError(tvError, etUsername, getResources().getString(R.string.exists_username), true);
                                etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

                            } else {
                                // If it does not exist, we show the green check
                                etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(NoUsernameActivity.this, R.drawable.ic_tick_correcto), null);
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
