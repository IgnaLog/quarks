/**
 * 1,2,3,4,5,6,7,8,9,10,11,12,13,14
 **/
package com.quarks.android.Login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

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
import com.quarks.android.CustomViews.ProgressButton;
import com.quarks.android.R;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.QueueVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {

    private EditText etEmail;
    private TextView tvFooter, tvError, tvConnError;
    private ViewGroup rootView;
    private View vpbNext;

    private Map<String, String> params = new HashMap<String, String>();
    private HashMap<String, String> headers = new HashMap<String, String>();
    private JsonArrayRequest jsonArrayRequest;
    private QueueVolley queueVolley;
    private Boolean existsEmail;

    private ProgressButton progressButton;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        /**  STATEMENTS  **/

        tvFooter = findViewById(R.id.tvFooter);
        rootView = findViewById(R.id.constraintLayout);
        etEmail = findViewById(R.id.etEmail);
        tvError = findViewById(R.id.tvError);
        tvConnError = findViewById(R.id.tvConnError);
        vpbNext = findViewById(R.id.myProgressButton);

        // Initialize the button as not enabled
        vpbNext.setEnabled(false);

        /**  DESIGN  **/

        /* Status Bar */
        Functions.changeStatusBarColor(SignInActivity.this, R.color.bg_negro);

        /* adjustResize with slow transition - set the android parameter: animateLayoutChanges = "true" in the layout */
        Functions.setLayoutTransition(rootView);

        /* Changing color of part of the texts */
        SpannableStringBuilder str = new SpannableStringBuilder(getResources().getString(R.string.already_have_an_account));
        String strAColor = getResources().getString(R.string.login);
        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), str.toString().indexOf(strAColor), str.toString().indexOf(strAColor) + strAColor.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.bg_blue_4)), str.toString().indexOf(strAColor), str.toString().indexOf(strAColor) + strAColor.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvFooter.setText(str);

        /* Remove TextView Error Connection when click in the Email Edittext*/
        etEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConnError.setVisibility(View.GONE);
            }
        });

        /* Clean the EditText if you press the X inside */
        etEmail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Drawable[] drawables = etEmail.getCompoundDrawables();
                    for (Drawable d : drawables) {
                        if (d != null) {
                            if (event.getRawX() >= (etEmail.getRight() - etEmail.getCompoundDrawables()[2].getBounds().width())) {
                                etEmail.setText("");
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });

        /* Activate the enter button if the EditText contain something */
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Functions.setTvError(tvError, etEmail, "", false); // If when making any change in the text there is an error we remove it
                tvConnError.setVisibility(View.GONE);

                if (etEmail.getText().toString().trim().length() > 0) { // If the edittext contains something, activate the Next button and put the ic_clear to clean text
                    vpbNext.setEnabled(true);
                    etEmail.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(SignInActivity.this, R.drawable.ic_clear), null);
                }
                if (etEmail.getText().toString().trim().length() == 0) {
                    vpbNext.setEnabled(false);
                    etEmail.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
            }
        };
        etEmail.addTextChangedListener(tw);

        /**  CODE  **/

        tvFooter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
                SignInActivity.this.startActivity(intent);
                finish();
            }
        });

        vpbNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressButton = new ProgressButton(SignInActivity.this, vpbNext);
                progressButton.buttonActivated();
                Functions.closeKeyboard(SignInActivity.this); // We hide the keyboard
                // We remove the connection error
                tvConnError.setVisibility(View.GONE);

                if (!Functions.isValidEmail(etEmail.getText().toString().trim())) {  // If the email is not an email we show an error and stop the loading button
                    Functions.setTvError(tvError, etEmail, getResources().getString(R.string.email_error), true);
                    progressButton.buttonFinished();

                } else {
                    /* We launched Volley to check email existence */
                    headers.put("Accept", "application/json");
                    queueVolley = QueueVolley.getInstance(SignInActivity.this);
                    jsonArrayRequest = jarExistsEmail(etEmail.getText().toString().trim());
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
        super.onResume(); // We leave the interface with the loading button in its initial position and without showing errors
        Functions.setTvError(tvError, etEmail, "", false);
        tvConnError.setVisibility(View.GONE);
        if (progressButton != null) {
            progressButton.buttonFinished();
        }
    }

    /**
     * VOLLEYS
     **/

    private JsonArrayRequest jarExistsEmail(String email) {
        params.clear();
        params.put("email", email);

        JSONArray jsonArrayParams = new JSONArray();
        jsonArrayParams.put(new JSONObject(params));

        String url = getResources().getString(R.string.url_exists_email);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArrayParams,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray respuesta) {
                        try {
                            for (int i = 0; i < respuesta.length(); i++) {
                                JSONObject jsonObject = respuesta.getJSONObject(i);
                                existsEmail = jsonObject.getBoolean("exists");
                            }

                            if (existsEmail) { // Show error that this email is already used
                                Functions.setTvError(tvError, etEmail, getResources().getString(R.string.exists_email), true);
                                progressButton.buttonFinished();

                            } else {
                                // If it does not exist we move on to the next activity:
                                Intent intent = new Intent(SignInActivity.this, NamePassActivity.class);
                                intent.putExtra("EMAIL", etEmail.getText().toString());
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


/* When the keyboard appears make certain views disappear */
//        final View activityRootView = rootView;
//        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
//                if (heightDiff > dpToPx(RegistroActivity.this, 200)) { // if more than 200 dp, it's probably a keyboard...
//                    tvFooter.setVisibility(View.GONE);
//                    lineFooter.setVisibility(View.GONE);
//                } else {
//                    tvFooter.setVisibility(View.VISIBLE);
//                    lineFooter.setVisibility(View.VISIBLE);
//                }
//            }
//        });
