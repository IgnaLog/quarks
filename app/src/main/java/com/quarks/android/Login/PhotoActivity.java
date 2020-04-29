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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PhotoActivity extends AppCompatActivity {

    private Button btnAdd;
    private ImageView ivLogo;
    private TextView tvOmit, tvConnError;
    private CircleImageView civProfile;

    private VolleyMultipartRequest volleyMultipartRequest;
    private QueueVolley queueVolley;

    private Context context = PhotoActivity.this;
    private AlertDialog dialog;
    private Uri imageUri;
    private Bitmap bitmap;

    private static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        /** STATEMENTS */

        btnAdd = findViewById(R.id.btnAdd);
        civProfile = findViewById(R.id.civProfile);
        ivLogo = findViewById(R.id.ivLogo);
        tvOmit = findViewById(R.id.tvOmit);
        tvConnError = findViewById(R.id.tvConnError);

        /** DESIGN **/

        /* Status Bar */
        Functions.changeStatusBarColor(PhotoActivity.this, R.color.bg_negro);

        /** CODE */

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConnError.setVisibility(View.GONE);
                Intent gallery = new Intent();
                gallery.setType("image/*");
                gallery.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(gallery, "Select Picture"), PICK_IMAGE);
            }
        });

        tvOmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoActivity.this, MainActivity.class);
                intent.putExtra("fromLogin", "yes");
                PhotoActivity.this.startActivity(intent);
            }
        });
    }

    /**
     * METHODS
     */

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Dejamos la interfaz sin mostrar errores
        tvConnError.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            imageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                // We rotate the image
                bitmap = Functions.modifyOrientation(context, bitmap, imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // we transform the image into a byte array
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
        queueVolley = QueueVolley.getInstance(PhotoActivity.this);
        volleyMultipartRequest = jarUploadImage(image);
        volleyMultipartRequest.setRetryPolicy((new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)));
        queueVolley.RequestQueueAdd(volleyMultipartRequest);

        AlertDialog.Builder builder = new AlertDialog.Builder(PhotoActivity.this);
        dialog = builder.create();
        Functions.showDialogLoading(dialog);
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
                                // Get url image from params
                                String filename = jsonObject.getString("filename");
                                String cloudUrl = jsonObject.getString("cloudUrl");

                                // Launching the next Activity
                                Intent intent = new Intent(PhotoActivity.this, PhotoAddedActivity.class);
                                intent.putExtra("filename", filename);
                                intent.putExtra("cloudUrl", cloudUrl);
                                PhotoActivity.this.startActivity(intent);
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
                        dialog.dismiss();
                        String err = getResources().getString(R.string.error_connection);
                        tvConnError.setText(err);
                        tvConnError.setVisibility(View.VISIBLE);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("profile", "1");
                params.put("userId", Preferences.getUserId(PhotoActivity.this));
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
