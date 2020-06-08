package com.quarks.android.Login;

import android.content.Context;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.quarks.android.CustomViews.ProgressButton;
import com.quarks.android.MainActivity;
import com.quarks.android.R;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.Preferences;
import com.quarks.android.Utils.QueueVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TokenActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etCode;
    private View vpbEnter;
    private TextView tvTitle, tvSubTitle, tvTwo;
    private LinearLayout lyTwo;

    private Map<String, String> params = new HashMap<String, String>();
    private JsonArrayRequest jsonArrayRequest;
    private QueueVolley queueVolley;

    private Context context = TokenActivity.this;
    private ProgressButton progressButton;
    private AlertDialog dialog;

    private String token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);

        /** STATEMENTS **/

        ivBack = findViewById(R.id.ivBack);
        etCode = findViewById(R.id.etCode);
        vpbEnter = findViewById(R.id.vpbEnter);

        token = getIntent().getStringExtra("token");


        /** DESIGN **/

        progressButton = new ProgressButton(TokenActivity.this, vpbEnter);
        progressButton.setText(getResources().getString(R.string.enter));
        vpbEnter.setEnabled(false);

        /* Status Bar */
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // The color of the icons is adapted, if not white
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_gris)); // Bar color

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
                if (etCode.getText().toString().trim().length() > 0) {
                    vpbEnter.setEnabled(true);
                }
                if (etCode.getText().toString().trim().length() == 0) {
                    vpbEnter.setEnabled(false);
                }
            }
        };
        etCode.addTextChangedListener(tw);

        /** CODE **/

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        vpbEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressButton.buttonActivated();
                Functions.closeKeyboard(TokenActivity.this); // We hide the keyboard

                /* We launched Volley to send email */
                queueVolley = QueueVolley.getInstance(TokenActivity.this);
                jsonArrayRequest = jarLoginCode(etCode.getText().toString().trim(), token);
                queueVolley.RequestQueueAdd(jsonArrayRequest);
            }
        });
    }

    /**
     * METHODS
     **/

    public void showDialogLoginHelp(final AlertDialog dialog, final String tag) {
        dialog.show();
        dialog.setContentView(R.layout.dialog_login);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        tvTitle = dialog.findViewById(R.id.tvTitle);
        tvSubTitle = dialog.findViewById(R.id.tvSubTitle);
        lyTwo = dialog.findViewById(R.id.lyTwo);
        tvTwo = dialog.findViewById(R.id.tvTwo);

        if (tag.equals("expired")) {
            dialog.setCancelable(false);
            tvTitle.setText(getResources().getString(R.string.dialog_error_network_title));
            tvSubTitle.setText(getResources().getString(R.string.dialog_token_subtitle_expired));
            tvTwo.setText(getResources().getString(R.string.dialog_token_retry));
            tvTwo.setTextColor(getResources().getColor(R.color.bg_blue_2));
            tvTwo.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (tag.equals("invalid")) {
            tvTitle.setText(getResources().getString(R.string.dialog_error_network_title));
            tvSubTitle.setText(getResources().getString(R.string.dialog_token_subtitle_invalid));
            tvTwo.setText(getResources().getString(R.string.dialog_token_retry));
            tvTwo.setTextColor(getResources().getColor(R.color.bg_blue_2));
            tvTwo.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            tvTitle.setText(getResources().getString(R.string.dialog_error_network_title));
            tvSubTitle.setText(getResources().getString(R.string.dialog_error_network_subtitle));
            tvTwo.setText(getResources().getString(R.string.dialog_error_close));
            tvTwo.setTextColor(getResources().getColor(R.color.bg_blue_2));
            tvTwo.setTypeface(Typeface.DEFAULT_BOLD);
        }

        lyTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tag.equals("expired")) {
                    dialog.dismiss();
                    finish();
                } else {
                    dialog.dismiss();
                }
            }
        });
    }

    /**
     * VOLLEY
     **/

    private JsonArrayRequest jarLoginCode(String code, final String accessToken) {
        params.clear();
        params.put("code", code);

        JSONArray jsonArrayParams = new JSONArray();
        jsonArrayParams.put(new JSONObject(params));

        String url = getResources().getString(R.string.url_login_code);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArrayParams,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            int code = 0;
                            String id = "", email = "", pass = "", username = "";
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);
                                code = jsonObject.getInt("code");
                                id = jsonObject.getString("id");
                                email = jsonObject.getString("email");
                                pass = jsonObject.getString("pass");
                                username = jsonObject.getString("username");
                            }
                            if (code == 200) { // OK
                                Preferences.setUserId(context, id);
                                Preferences.setUserEmail(context, email);
                                Preferences.setUserPass(context, pass);
                                Preferences.setUserName(context, username);

                                Intent intent = new Intent(TokenActivity.this, MainActivity.class);
                                intent.putExtra("fromLogin", "yes");
                                startActivity(intent);
                            } else if (code == 498) { // Expired code
                                progressButton.buttonFinished();
                                AlertDialog.Builder builder = new AlertDialog.Builder(TokenActivity.this);
                                dialog = builder.create();
                                showDialogLoginHelp(dialog, "expired");
                            } else { // Invalid code
                                progressButton.buttonFinished();
                                AlertDialog.Builder builder = new AlertDialog.Builder(TokenActivity.this);
                                dialog = builder.create();
                                showDialogLoginHelp(dialog, "invalid");
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
                        AlertDialog.Builder builder = new AlertDialog.Builder(TokenActivity.this);
                        dialog = builder.create();
                        showDialogLoginHelp(dialog, "network");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + accessToken);
                return headers;
            }
        };
        return jsonArrayRequest;
    }
}
