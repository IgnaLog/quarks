package com.quarks.android.CustomViews;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.quarks.android.R;

public class DownRecyclerButton {

//    private FrameLayout lyBtnDownRecycler;
//    private Animator animDisappear, animAppear;
//
//    public DownRecyclerButton(Context context, View view) {
//        lyBtnDownRecycler = view.findViewById(R.id.lyBtnDownRecycler);
//
//        animAppear = AnimatorInflater.loadAnimator(context, R.animator.down_recycler_btn_appear);
//        animAppear.setTarget(lyBtnDownRecycler);
//
//        animDisappear = AnimatorInflater.loadAnimator(context, R.animator.down_recycler_btn_disappear);
//        animDisappear.setTarget(lyBtnDownRecycler);
//
//    }
//
//    public void setVisible(Boolean loading, final String situation) {
//        if (loading) {
//            if ((lyBtnDownRecycler.getVisibility() == View.INVISIBLE) && !animAppear.isRunning() && !animDisappear.isRunning()) {
//                lyBtnDownRecycler.setVisibility(View.VISIBLE);
//                animAppear.addListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        if(situation.equals("off")){
//                            animDisappear.addListener(new AnimatorListenerAdapter() {
//                                @Override
//                                public void onAnimationEnd(Animator animation) {
//                                    super.onAnimationEnd(animation);
//                                    lyBtnDownRecycler.setVisibility(View.INVISIBLE);
//                                }
//                            });
//                            animDisappear.start();
//                        }
//                    }
//                });
//                animAppear.start();
//
//            }
//        } else {
//            if ((lyBtnDownRecycler.getVisibility() == View.VISIBLE) && !animDisappear.isRunning() && !animAppear.isRunning()) {
//                animDisappear.addListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        lyBtnDownRecycler.setVisibility(View.INVISIBLE);
//                        if(situation.equals("on")){
//                            lyBtnDownRecycler.setVisibility(View.VISIBLE);
//                            animAppear.start();
//                        }
//                    }
//                });
//                animDisappear.start();
//            }
//        }
//    }
}
