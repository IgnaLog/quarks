/**
 * 1,2,3,4,5,6,7,8,9,10,11,12,13,14
 */
package com.quarks.android.Login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.quarks.android.CustomViews.ProgressButton;
import com.quarks.android.R;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.QueueVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NamePassActivity extends AppCompatActivity {

    private EditText etName, etPass;
    private ViewGroup rootView;
    private TextView tvError, tvConnError;
    private View vpbNext;

    private Map<String, String> params = new HashMap<String, String>();
    private HashMap<String, String> headers = new HashMap<String, String>();
    private JsonArrayRequest jsonArrayRequest;
    private QueueVolley queueVolley;

    private ProgressButton progressButton;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_pass);

        /**  STATEMENTS  **/

        etName = findViewById(R.id.etName);
        rootView = findViewById(R.id.constraintLayout);
        etPass = findViewById(R.id.etPass);
        tvError = findViewById(R.id.tvError);
        tvConnError = findViewById(R.id.tvConnError);
        vpbNext = findViewById(R.id.myProgressButton);

        // Initialize the button as not enabled
        vpbNext.setEnabled(false);

        final String email = getIntent().getStringExtra("EMAIL"); // Capturamos el email que viene del activity anterior

        /**  DESIGN  **/

        /* Status Bar */
        Functions.changeStatusBarColor(NamePassActivity.this, R.color.bg_negro);

        /* adjustResize with slow transition - set the android parameter: animateLayoutChanges = "true" in the layout */
        Functions.setLayoutTransition(rootView);

        /* Clean the EditText if you press the X inside */
        etName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (etName.getRight() - etName.getCompoundDrawables()[2].getBounds().width())) {
                        etName.setText("");
                        return true;
                    }
                }
                return false;
            }
        });

        /* Remove TextView Error Connection when click in the Name Edittext*/
        etName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                tvConnError.setVisibility(View.GONE);
            }
        });

        etName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConnError.setVisibility(View.GONE);
            }
        });

        /* Remove TextView Error Connection when click in the Pass Edittext*/
        etPass.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                tvConnError.setVisibility(View.GONE);
            }
        });

        etPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConnError.setVisibility(View.GONE);
            }
        });

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
                Functions.setTvError(tvError, etPass, "", false);  // If when making any change in the text there is an error we remove it
                tvConnError.setVisibility(View.GONE);

                if (etName.getText().toString().trim().length() > 0 && etPass.getText().toString().trim().length() > 5) { // If the EditText contains more than 5 characters activate the following button
                    vpbNext.setEnabled(true);
                }
                if (etName.getText().toString().trim().length() == 0 || etPass.getText().toString().trim().length() < 6) { // If the EditText contains less than 5 characters, deactivate the following button
                    vpbNext.setEnabled(false);
                }
            }
        };
        etName.addTextChangedListener(tw);
        etPass.addTextChangedListener(tw);


        /**  CODE  **/

        vpbNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressButton = new ProgressButton(NamePassActivity.this, vpbNext);
                // We remove the connection error
                tvConnError.setVisibility(View.GONE);

                if (isPassEasy(etPass.getText().toString())) { // If the password is too easy we show an error
                    Functions.setTvError(tvError, etPass, getResources().getString(R.string.usual_password), true);

                } else {
                    // We activate the loading button and then we hide the keyboadrd
                    progressButton.buttonActivated();
                    Functions.closeKeyboard(NamePassActivity.this); // Escondemos el teclado

                    /* Launch Volley to generate a username */
                    headers.put("Accept", "application/json");
                    queueVolley = QueueVolley.getInstance(NamePassActivity.this);
                    jsonArrayRequest = jarGenerateUsername(etName.getText().toString().trim(), email);
                    jsonArrayRequest.setRetryPolicy((new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)));
                    queueVolley.RequestQueueAdd(jsonArrayRequest);
                }
            }
        });
    }

    /**
     * METHODS
     **/

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We leave the interface with the loading button in its initial position and without showing errors
        Functions.setTvError(tvError, etPass, "", false);
        tvConnError.setVisibility(View.GONE);
        if (progressButton != null) { // If the ProgressButton is loading, we cut it
            progressButton.buttonFinished();
        }
    }

    public boolean isPassEasy(String pass) {
        if (pass.equals("012345") || pass.equals("123456") || pass.equals("543210") || pass.equals("654321")
                || pass.equals("987654") || pass.equals("456789") || pass.equals("qwerty") || pass.equals("ytrewq")
                || pass.equals("asdfgh") || pass.equals("hgfdsa") || pass.equals("zxcvbn") || pass.equals("nbvcxz") || allCharactersSame(pass)) {
            return true;
        }
        return false;
    }

    public boolean allCharactersSame(String s) {
        int length = s.length();
        for (int i = 1; i < length; i++)
            if (s.charAt(i) != s.charAt(0))
                return false;
        return true;
    }

    /**
     * VOLLEYS
     **/

    private JsonArrayRequest jarGenerateUsername(String name, final String email) {
        params.clear();
        params.put("email", email);
        params.put("name", name);

        JSONArray jsonArrayParams = new JSONArray();
        jsonArrayParams.put(new JSONObject(params));

        String url = getResources().getString(R.string.url_generate_username);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArrayParams,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray respuesta) {
                        try {
                            String username = "";
                            for (int i = 0; i < respuesta.length(); i++) {
                                JSONObject jsonObject = respuesta.getJSONObject(i);
                                username = jsonObject.getString("username");
                            }
                            if (username.equals("-1")) { // If we don't have a suggested username we go to the activity in which the user chooses it manually
                                Intent intent = new Intent(NamePassActivity.this, NoUsernameActivity.class);
                                intent.putExtra("EMAIL", email);
                                intent.putExtra("USERNAME", "");
                                intent.putExtra("PASS", etPass.getText().toString());
                                NamePassActivity.this.startActivity(intent);
                            } else {
                                // We go to the next activity with the email username and pass:
                                Intent intent = new Intent(NamePassActivity.this, WelcomeActivity.class);
                                intent.putExtra("EMAIL", email);
                                intent.putExtra("USERNAME", username);
                                intent.putExtra("PASS", etPass.getText().toString());
                                NamePassActivity.this.startActivity(intent);
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
                        progressButton.buttonFinished();
                        if (error instanceof TimeoutError || error instanceof NoConnectionError) { // This indicates that the request has either time out or there is no connection
                            tvConnError.setVisibility(View.VISIBLE);
                        } else if (error instanceof AuthFailureError) { // Error indicating that there was an Authentication Failure while performing the request

                        } else if (error instanceof ServerError) {  // Indicates that the server responded with a error response
                            tvConnError.setVisibility(View.VISIBLE);
                        } else if (error instanceof NetworkError) {  // Indicates that there was network error while performing the request
                            tvConnError.setVisibility(View.VISIBLE);
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

