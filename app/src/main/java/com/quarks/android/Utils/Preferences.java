package com.quarks.android.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    private static final String NOMBRE_PREFERENCIAS = "preferencias";

    /* USER ID */
    //----------------------------------------------------------------------------------------------

    public static void setUserId(Context context, String userId) {
        SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("userId", userId);
        editor.apply();
    }

    public static String getUserId(Context context) {
        SharedPreferences preferecias = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  preferecias.getString("userId", "");
    }

    /* USER EMAIL */
    //----------------------------------------------------------------------------------------------

    public static void setUserEmail(Context context, String userEmail) {
        SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("userEmail", userEmail);
        editor.apply();
    }

    public static String getUserEmail(Context context) {
        SharedPreferences preferecias = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  preferecias.getString("userEmail", "");
    }

    /* USER NAME */
    //----------------------------------------------------------------------------------------------

    public static void setUserName(Context context, String userName) {
        SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("userName", userName);
        editor.apply();
    }

    public static String getUserName(Context context) {
        SharedPreferences preferecias = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  preferecias.getString("userName", "");
    }

    /* USER PASS */
    //----------------------------------------------------------------------------------------------

    public static void setUserPass(Context context, String userPass) {
        SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("userPass", userPass);
        editor.apply();
    }

    public static String getUserPass(Context context) {
        SharedPreferences preferecias = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  preferecias.getString("userPass", "");
    }

    /* FCM TOKEN */
    //----------------------------------------------------------------------------------------------

    public static void setFCMToken(Context context, String token) {
        SharedPreferences.Editor editor = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE).edit();
        editor.putString("token", token);
        editor.apply();
    }

    public static String getFCMToken(Context context) {
        SharedPreferences preferecias = context.getSharedPreferences(NOMBRE_PREFERENCIAS, Context.MODE_PRIVATE);
        return  preferecias.getString("userPass", "");
    }
}
