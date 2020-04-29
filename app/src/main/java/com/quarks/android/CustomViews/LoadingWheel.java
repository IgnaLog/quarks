package com.quarks.android.CustomViews;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.quarks.android.R;

public class LoadingWheel {

    private LinearLayout lyCircularProgressBar;
    private ProgressBar progressBar;
    private Animation zoomAppear,zoomDisappear;

    public LoadingWheel(Context context, View view) {
        lyCircularProgressBar = view.findViewById(R.id.lyCiruclarProgressBar);
        progressBar = view.findViewById(R.id.progressBar);
        zoomAppear = AnimationUtils.loadAnimation(context, R.anim.loading_wheel_appear);
        zoomDisappear = AnimationUtils.loadAnimation(context, R.anim.loading_wheel_disappear);
    }

    public void setLoading(Boolean loading){
        if(loading){
            lyCircularProgressBar.setVisibility(View.VISIBLE);
            lyCircularProgressBar.startAnimation(zoomAppear);
        }else{
            lyCircularProgressBar.startAnimation(zoomDisappear);
            lyCircularProgressBar.setVisibility(View.INVISIBLE);
        }
    }
}
