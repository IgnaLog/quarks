package com.quarks.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.quarks.android.Adapters.ContactsAdapter;
import com.quarks.android.Items.ContactItem;
import com.quarks.android.Utils.Preferences;
import com.quarks.android.Utils.QueueVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ContactsActivity extends AppCompatActivity {

    private ImageView ivBack;
    private EditText etSearch;
    private RecyclerView rvContacts;

    private Map<String, String> params = new HashMap<String, String>();
    private HashMap<String, String> headers = new HashMap<String, String>();
    private JsonArrayRequest jsonArrayRequest;
    private QueueVolley queueVolley;
    private Context context = ContactsActivity.this;

    private ContactsAdapter adapter;
    private ArrayList<ContactItem> alContacts = new ArrayList<ContactItem>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        /**  STATEMENTS  **/

        ivBack = findViewById(R.id.ivBack);
        etSearch = findViewById(R.id.etSearch);
        rvContacts = findViewById(R.id.rvContacts);

        /** DESIGN **/

        /* Status Bar */
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // The color of the icons is adapted, if not white
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_gris)); // Bar color

        /* Clean the EditText if you press the X inside */
        etSearch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Drawable[] drawables = etSearch.getCompoundDrawables();
                    for (Drawable d : drawables) {
                        if (d != null) {
                            if (event.getRawX() >= (etSearch.getRight() - etSearch.getCompoundDrawables()[2].getBounds().width())) {
                                etSearch.setText("");
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        });

        /* Activate the ic_clear icon if the EditText contain something */
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (etSearch.getText().toString().trim().length() > 0) { // If the edittext contains something, activate the Next button and put the ic_clear to clean text
                    etSearch.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(ContactsActivity.this, R.drawable.ic_clear), null);
                    // we throw the volley
                    headers.put("Accept", "application/json");
                    queueVolley = QueueVolley.getInstance(ContactsActivity.this);
                    jsonArrayRequest = jarSearchContact(etSearch.getText().toString().trim());
                    queueVolley.RequestQueueAdd(jsonArrayRequest);
                }
                if (etSearch.getText().toString().trim().length() == 0) {
                    etSearch.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                    // we clear the adapter
                    adapter.Clear();
                }
            }
        };
        etSearch.addTextChangedListener(tw);

        /** CODE **/

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * FUNCTIONS
     **/

    /**
     * VOLLEYS
     **/

    private JsonArrayRequest jarSearchContact(String wantedLetter) {
        params.clear();
        params.put("wantedLetter", wantedLetter);
        params.put("username", Preferences.getUserName(context));

        JSONArray jsonArrayParams = new JSONArray();
        jsonArrayParams.put(new JSONObject(params));

        String url = getResources().getString(R.string.url_search_contact);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, jsonArrayParams,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray respuesta) {
                        try {
                            if (adapter != null) {
                                adapter.Clear();
                            }
                            for (int i = 0; i < respuesta.length(); i++) {
                                JSONObject jsonObject = respuesta.getJSONObject(i);

                                alContacts.add(new ContactItem(
                                        jsonObject.getString("urlPhoto"),
                                        jsonObject.getString("filename"),
                                        jsonObject.getString("username"),
                                        jsonObject.getString("userId")));
                            }
                            adapter = new ContactsAdapter(getApplicationContext(), alContacts);
                            rvContacts.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                            rvContacts.setAdapter(adapter);
                            rvContacts.setHasFixedSize(true);
                            adapter.notifyDataSetChanged();
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
