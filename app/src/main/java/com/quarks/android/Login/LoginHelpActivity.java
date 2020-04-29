package com.quarks.android.Login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
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

public class LoginHelpActivity extends AppCompatActivity {

    private EditText etMailUserName;
    private View vpbNext;
    private ProgressButton progressButton;
    private TextView tvError, tvTitle, tvSubTitle, tvTwo;
    private LinearLayout lyRetry, lySendEmail;

    private Map<String, String> params = new HashMap<String, String>();
    private HashMap<String, String> headers = new HashMap<String, String>();
    private JsonArrayRequest jsonArrayRequest;
    private QueueVolley queueVolley;

    private AlertDialog dialog;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_help);

        /**  STATEMENTS  **/

        etMailUserName = findViewById(R.id.etMailUserName);
        vpbNext = findViewById(R.id.vpbNext);
        tvError = findViewById(R.id.tvError);

        /** DESIGN **/

        /* Status Bar */
        LoginHelpActivity.this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // The color of the icons is adapted, if not white
        LoginHelpActivity.this.getWindow().setStatusBarColor(getResources().getColor(R.color.bg_gris)); // Bar color

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
                Functions.setTvError(tvError, etMailUserName, "", false); // If when making any change in the text there is an error we remove it
                if (etMailUserName.getText().toString().trim().length() > 0) {
                    vpbNext.setEnabled(true);
                }
                if (etMailUserName.getText().toString().trim().length() == 0) {
                    vpbNext.setEnabled(false);
                }
            }
        };
        etMailUserName.addTextChangedListener(tw);


        /** CODE **/

        vpbNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressButton = new ProgressButton(LoginHelpActivity.this, vpbNext);
                progressButton.setText(getResources().getString(R.string.next));
                progressButton.buttonActivated();
                Functions.closeKeyboard(LoginHelpActivity.this); // We hide the keyboard

                /* We launched Volley to send email */
                headers.put("Accept", "application/json");
                queueVolley = QueueVolley.getInstance(LoginHelpActivity.this);
                jsonArrayRequest = jarLoginHelp(etMailUserName.getText().toString().trim());
                jsonArrayRequest.setRetryPolicy((new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)));
                queueVolley.RequestQueueAdd(jsonArrayRequest);

            }
        });
    }

    public void showDialogLoginHelp(final AlertDialog dialog, String tag) {
        dialog.show();
        dialog.setContentView(R.layout.dialog_login);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        lyRetry = dialog.findViewById(R.id.lyTwo);
        tvTitle = dialog.findViewById(R.id.tvTitle);
        tvSubTitle = dialog.findViewById(R.id.tvSubTitle);
        lySendEmail = dialog.findViewById(R.id.lyOne);
        tvTwo = dialog.findViewById(R.id.tvTwo);

        if (tag.equals("network")) {
            tvTitle.setText(getResources().getString(R.string.dialog_error_network_title));
            tvSubTitle.setText(getResources().getString(R.string.dialog_error_network_subtitle));
            tvTwo.setText(getResources().getString(R.string.dialog_error_close));
            tvTwo.setTextColor(getResources().getColor(R.color.bg_blue_2));
            tvTwo.setTypeface(Typeface.DEFAULT_BOLD);
        }

        lyRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }


    /**
     * VOLLEYS
     **/

    private JsonArrayRequest jarLoginHelp(String emailName) {
        params.clear();
        params.put("emailName", emailName);

        JSONArray jsonArrayParams = new JSONArray();
        jsonArrayParams.put(new JSONObject(params));

        String url = getResources().getString(R.string.url_login_help);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArrayParams,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray respuesta) {
                        try {
                            int code = 0;
                            String token = "";
                            for (int i = 0; i < respuesta.length(); i++) {
                                JSONObject jsonObject = respuesta.getJSONObject(i);
                                code = jsonObject.getInt("code");
                                token = jsonObject.getString("token");
                            }
                            if (code == 200) { // OK
                                progressButton.buttonFinished();
                                Intent intent = new Intent(LoginHelpActivity.this, TokenActivity.class);
                                intent.putExtra("token", token);
                                startActivity(intent);
                            } else { // User not found
                                Functions.setTvError(tvError, etMailUserName, getResources().getString(R.string.error_user_not_found), true);
                                progressButton.buttonFinished();
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
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginHelpActivity.this);
                        dialog = builder.create();
                        showDialogLoginHelp(dialog, "network");
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
