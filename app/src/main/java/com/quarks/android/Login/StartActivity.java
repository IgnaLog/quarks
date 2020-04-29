/**
 * 1,2-,3,4,5,6,7,8,9,10,11-,12-,13-,14
 **/
package com.quarks.android.Login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.quarks.android.R;
import com.quarks.android.Utils.Functions;

public class StartActivity extends AppCompatActivity {

    private Button btnCreateAccount;
    private TextView tvEnter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        /**  STATEMENTS  **/

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvEnter = findViewById(R.id.tvEnter);

        /**  DESIGN  **/
        /* Status Bar */
        Functions.changeStatusBarColor(StartActivity.this, R.color.bg_negro);

        /**  CODE  **/

        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(StartActivity.this, SignInActivity.class);
                StartActivity.this.startActivity(myIntent);
            }
        });

        tvEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(StartActivity.this, LoginActivity.class);
                StartActivity.this.startActivity(myIntent);
            }
        });
    }

    /**
     * METHODS
     **/

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
