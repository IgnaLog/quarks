package com.quarks.android.Utils;

import android.content.Context;

public class Preferences {

    private static final String NOMBRE_PREFERENCIAS = "Preferences";

    /* USER ID */
    //----------------------------------------------------------------------------------------------

    public static void setUserId(Context context, String userId) {
        android.content.SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("userId", userId);
        editor.apply();
    }

    public static String getUserId(Context context) {
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  sharedPreferences.getString("userId", "");
    }

    /* USER EMAIL */
    //----------------------------------------------------------------------------------------------

    public static void setUserEmail(Context context, String userEmail) {
        android.content.SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("userEmail", userEmail);
        editor.apply();
    }

    public static String getUserEmail(Context context) {
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  sharedPreferences.getString("userEmail", "");
    }

    /* USER NAME */
    //----------------------------------------------------------------------------------------------

    public static void setUserName(Context context, String userName) {
        android.content.SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("userName", userName);
        editor.apply();
    }

    public static String getUserName(Context context) {
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  sharedPreferences.getString("userName", "");
    }

    /* USER PASS */
    //----------------------------------------------------------------------------------------------

    public static void setUserPass(Context context, String userPass) {
        android.content.SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("userPass", userPass);
        editor.apply();
    }

    public static String getUserPass(Context context) {
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  sharedPreferences.getString("userPass", "");
    }

    /* FCM TOKEN */
    //----------------------------------------------------------------------------------------------

    public static void setFcmToken(Context context, String token) {
        android.content.SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("token", token);
        editor.apply();
    }

    public static String getFcmToken(Context context) {
        android.content.SharedPreferences sharedPreferences = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  sharedPreferences.getString("token", "");
    }
}
