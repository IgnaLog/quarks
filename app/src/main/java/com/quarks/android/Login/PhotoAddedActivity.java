package com.quarks.android.Login;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.quarks.android.MainActivity;
import com.quarks.android.R;
import com.quarks.android.Utils.Functions;
import com.quarks.android.Utils.Preferences;
import com.quarks.android.Utils.QueueVolley;
import com.quarks.android.Utils.VolleyMultipartRequest;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PhotoAddedActivity extends AppCompatActivity {

    private Button btnNext;
    private TextView tvChangePhoto, tvConnError;
    private CircleImageView civProfile;

    private Uri imageUri;
    private Context context = PhotoAddedActivity.this;
    private Bitmap bitmap;

    private VolleyMultipartRequest volleyMultipartRequest;
    private QueueVolley queueVolley;

    private static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_added);

        /** STATEMENTS */

        btnNext = findViewById(R.id.btnNext);
        tvChangePhoto = findViewById(R.id.tvChangePhoto);
        civProfile = findViewById(R.id.civProfile);
        tvConnError = findViewById(R.id.tvConnError);

        final String filename = getIntent().getStringExtra("filename"); // we capture the local path of the image server
        final String cloudUrl = getIntent().getStringExtra("cloudUrl"); // we capture the cloud path of the image server

        /** DESIGN **/

        /* Status Bar */
        Functions.changeStatusBarColor(PhotoAddedActivity.this, R.color.bg_negro);

        /** CODE */

        // Place the cloud path into Picasso library to show the image into the circleImageView
        //Picasso.get().load(getResources().getString(R.string.url_get_image) + filename).fit().centerCrop().into(civProfile); // From my server
        Picasso.get().load(cloudUrl).fit().centerCrop().into(civProfile); // From cloudinary server. Need to change server response!

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoAddedActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        tvChangePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConnError.setVisibility(View.GONE);
                Intent gallery = new Intent();
                gallery.setType("image/*");
                gallery.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(gallery, "Select Picture"), PICK_IMAGE);
            }
        });
    }

    /**
     * METHODS
     */

    @Override
    protected void onResume() {
        super.onResume();
        // Dejamos la interfaz sin mostrar errores
        tvConnError.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            imageUri = data.getData();
            // We rotate the image
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);
                // We rotate the image
                bitmap = Functions.modifyOrientation(context, bitmap, imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // We transform the bitmap into byte array
            byte[] image = Functions.imageToByteArray(bitmap);
            double size = image.length;
            if (size > 10000000) { // > 10mb
                // Refuse
                String err = getResources().getString(R.string.error_size_image);
                tvConnError.setText(err);
                tvConnError.setVisibility(View.VISIBLE);
            } else if (size > 5000000) { // > 5mb
                // Reduce 50% quality and send
                byte[] newImage = Functions.reduceQualityImage(bitmap);
                sendImage(newImage);
            } else {
                //We send the image to the web service
                sendImage(image);
            }
        }
    }

    private void sendImage(byte[] image) {
        queueVolley = QueueVolley.getInstance(PhotoAddedActivity.this);
        volleyMultipartRequest = jarUploadImage(image);
        volleyMultipartRequest.setRetryPolicy((new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)));
        queueVolley.RequestQueueAdd(volleyMultipartRequest);
    }

    /**
     * VOLLEYS
     **/

    private VolleyMultipartRequest jarUploadImage(final byte[] image) {

        String url = getResources().getString(R.string.url_upload_image);

        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, url,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        try {
                            JSONObject jsonObject = new JSONObject(new String(response.data));
                            if (jsonObject.getInt("code") == 200) {
                                // If we receive 200 code, we put the image into the circle image view
                                civProfile.setImageBitmap(bitmap);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Toast.makeText(FotoActivity.this,error.toString(),Toast.LENGTH_LONG).show();
                        Log.d("volley", error.toString());
                        String err = getResources().getString(R.string.error_connection);
                        tvConnError.setText(err);
                        tvConnError.setVisibility(View.VISIBLE);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("profile", "1");
                params.put("userId", Preferences.getUserId(PhotoAddedActivity.this));
                //params.put("userId", "5e402cb7bb250f19ec0f447f");
                return params;
            }

            // Pass files using below method
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                long imagename = System.currentTimeMillis();
                params.put("image", new VolleyMultipartRequest.DataPart(imagename + ".jpeg", image));
                return params;
            }
        };
        return volleyMultipartRequest;
    }
}
