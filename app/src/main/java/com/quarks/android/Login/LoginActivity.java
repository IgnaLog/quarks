package com.quarks.android.Login;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

import static com.quarks.android.Utils.Functions.dpToPx;

public class LoginActivity extends AppCompatActivity {

    private TextView tvGetHelp, tvNoAccount, tvTitle, tvSubTitle, tvTwo;
    private EditText etName, etPass;
    private View footerLine;
    private ViewGroup rootView;
    private ImageView ivLogo;
    private View vpbLogin;
    private LinearLayout lyRetry, lySendEmail;

    private JsonArrayRequest jsonArrayRequest;
    private Map<String, String> params = new HashMap<String, String>();
    private QueueVolley queueVolley;

    private Context context = LoginActivity.this;
    private ProgressButton progressButton;
    private AlertDialog dialog;
    private int attempts = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /**  STATEMENTS  **/

        footerLine = findViewById(R.id.vFooterLine);
        tvGetHelp = findViewById(R.id.tvForgotPassword);
        tvNoAccount = findViewById(R.id.tvNoAccount);
        etName = findViewById(R.id.etName);
        etPass = findViewById(R.id.etPass);
        rootView = findViewById(R.id.rootView);
        ivLogo = findViewById(R.id.ivLogo);
        vpbLogin = findViewById(R.id.btnLogin);

        /**  DESIGN  **/

        // We start with the enter button disabled
        vpbLogin.setEnabled(false);

        /* Status Bar */
        LoginActivity.this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // The color of the icons is adapted, if not white
        LoginActivity.this.getWindow().setStatusBarColor(getResources().getColor(R.color.bg_white));

        //* adjustResize with slow transition - set the android parameter: animateLayoutChanges = "true" in the layout */
        Functions.setLayoutTransition(rootView);

        /* When the keyboad appears, make some views */
        final View activityRootView = rootView;
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > dpToPx(LoginActivity.this, 200)) { // If more than 200 dp, it's probably a keyboard...
                    tvNoAccount.setVisibility(View.GONE);
                    footerLine.setVisibility(View.GONE);
                    if (Functions.getScreenInches(LoginActivity.this) <= 5.2) { // Check if the mobile screen is lower than 5.2 inches. In that case, we hide the logo image
                        ivLogo.setVisibility(View.GONE);
                    }
                } else {
                    tvNoAccount.setVisibility(View.VISIBLE);
                    footerLine.setVisibility(View.VISIBLE);
                    ivLogo.setVisibility(View.VISIBLE);
                }
            }
        });

        /* Changing the color of part of the text */
        SpannableStringBuilder str = new SpannableStringBuilder(getResources().getString(R.string.forgotten_data_session));
        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 44, 56, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.bg_blue_4)), 44, 56, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableStringBuilder str2 = new SpannableStringBuilder(getResources().getString(R.string.sign_up_here));
        str2.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 23, 33, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        str2.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.bg_blue_4)), 23, 33, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvGetHelp.setText(str);
        tvNoAccount.setText(str2);


        /* Activate the enter button if the EditText contain */
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (etName.getText().toString().trim().length() > 0 && etPass.getText().toString().trim().length() > 0) {
                    vpbLogin.setEnabled(true);
                }
                if (etName.getText().toString().trim().length() == 0 || etPass.getText().toString().trim().length() == 0) {
                    vpbLogin.setEnabled(false);
                }
            }
        };
        etName.addTextChangedListener(tw);
        etPass.addTextChangedListener(tw);


        /**  CODE  **/

        tvGetHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(LoginActivity.this, LoginHelpActivity.class);
                LoginActivity.this.startActivity(myIntent);
            }
        });

        tvNoAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(LoginActivity.this, SignInActivity.class);
                LoginActivity.this.startActivity(myIntent);
                finish();
            }
        });

        vpbLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressButton = new ProgressButton(LoginActivity.this, vpbLogin);
                progressButton.setText(getResources().getString(R.string.enter));
                progressButton.buttonActivated();
                Functions.closeKeyboard(LoginActivity.this); // We hide the keyboard

                /* We launched Volley to login */
                queueVolley = QueueVolley.getInstance(LoginActivity.this);
                jsonArrayRequest = jarLogin();
                queueVolley.RequestQueueAdd(jsonArrayRequest);
            }
        });
    }

    /**
     * METHODS
     **/

    /* Show a dialog about account not found  */
    public void showDialogAccountNotFound(final AlertDialog dialog, String tag) {
        dialog.show();
        dialog.setContentView(R.layout.dialog_login);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        lyRetry = dialog.findViewById(R.id.lyTwo);
        tvTitle = dialog.findViewById(R.id.tvTitle);
        tvSubTitle = dialog.findViewById(R.id.tvSubTitle);
        lySendEmail = dialog.findViewById(R.id.lyOne);
        tvTwo = dialog.findViewById(R.id.tvTwo);

        if (tag.equals("pass")) {
            if (attempts > 1) {
                lySendEmail.setVisibility(View.VISIBLE);
                String textTitle = getResources().getString(R.string.dialog_password_not_found_title_retries) + " " + etName.getText().toString().trim() + "?";
                tvTitle.setText(textTitle);
                tvSubTitle.setText(getResources().getString(R.string.dialog_password_not_found_subtitle_retries));
            } else {
                String textTitle = getResources().getString(R.string.dialog_password_not_found_title) + " " + etName.getText().toString().trim();
                tvTitle.setText(textTitle);
                tvSubTitle.setText(getResources().getString(R.string.dialog_password_not_found_subtitle));
            }
        }else if(tag.equals("user")){
            tvTitle.setText(getResources().getString(R.string.dialog_username_not_found_title));
            tvSubTitle.setText(getResources().getString(R.string.dialog_username_not_found_subtitle));
        } else {
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

        lySendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent myIntent = new Intent(LoginActivity.this, LoginHelpActivity.class);
                LoginActivity.this.startActivity(myIntent);
            }
        });

    }

    /**
     * VOLLEYS
     **/

    private JsonArrayRequest jarLogin() {
        params.clear();
        params.put("fcmToken", Preferences.getFCMToken(context));

        JSONArray jsonArrayParams = new JSONArray();
        jsonArrayParams.put(new JSONObject(params));

        String url = getResources().getString(R.string.url_login);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArrayParams,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray respuesta) {
                        try {
                            int code = 0;
                            String id = "", pass = "", email = "", username = "";
                            for (int i = 0; i < respuesta.length(); i++) {
                                JSONObject jsonObject = respuesta.getJSONObject(i);
                                code = jsonObject.getInt("code");

                                id = jsonObject.getString("id");
                                pass = jsonObject.getString("pass");
                                email = jsonObject.getString("email");
                                username = jsonObject.getString("username");
                            }
                            if (code == 200) { // OK
                                Preferences.setUserId(context, id);
                                Preferences.setUserEmail(context, email);
                                Preferences.setUserPass(context, pass);
                                Preferences.setUserName(context, username);

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else if (code == 404) { // User not found
                                progressButton.buttonFinished();
                                vpbLogin.setEnabled(false);
                                // Show dialog account not found
                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                dialog = builder.create();
                                showDialogAccountNotFound(dialog, "user");

                            } else if (code == 401) { // KO no valid password
                                progressButton.buttonFinished();
                                vpbLogin.setEnabled(false);
                                attempts++; // to know how many password attempts the user has made before send another type of dialog
                                // Show dialog password not found
                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                dialog = builder.create();
                                showDialogAccountNotFound(dialog, "pass");
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
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                        dialog = builder.create();
                        showDialogAccountNotFound(dialog, "");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String name = etName.getText().toString().trim();
                String pass = etPass.getText().toString().trim();
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
