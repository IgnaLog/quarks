package com.quarks.android.Utils;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.quarks.android.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static android.content.Context.NOTIFICATION_SERVICE;


public class Functions {

    public static String formatMongoTime(String mongoTime) {
        String[] parts = mongoTime.split("T");
        String date = parts[0];
        String time = parts[1];

        String[] partsOfTime = time.split("\\.");
        String realTime = partsOfTime[0];

        return date + " " + realTime;
    }

    public static String formatConversationDate(String time, Context context) {
        String convertedDate = "";

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        simpleDateFormat.setTimeZone(TimeZone.getDefault());

        SimpleDateFormat requiredDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        requiredDateFormat.setTimeZone(TimeZone.getDefault());

        try {
            Date date = dateTimeFormat.parse(time);
            String dateToCompare = simpleDateFormat.format(date);
            String dateNow = simpleDateFormat.format(new Date());
            if (dateToCompare.equals(dateNow)) { // Same day, show only hours and minutes
                convertedDate = requiredDateFormat.format(date);
            }else if(dateToCompare.equals(getYesterdayDateString())){
                convertedDate = context.getResources().getString(R.string.yesterday);
            }else{ // Another day, show only the date
                convertedDate = dateToCompare;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return convertedDate;
    }

    /* Get the day before in string format */
    private static String getYesterdayDateString() {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return dateFormat.format(cal.getTime());
    }

    /* It converts the dateTime of the messages saved in the local database into date format with its default timezone */
    public static String formatDate(String time, Context context) {
        String convertedDate = "";

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        simpleDateFormat.setTimeZone(TimeZone.getDefault());

        SimpleDateFormat spanishDateFormat = new SimpleDateFormat("d 'DE' MMMM 'DE' yyyy", Locale.getDefault());
        spanishDateFormat.setTimeZone(TimeZone.getDefault());

        SimpleDateFormat defaultDateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
        defaultDateFormat.setTimeZone(TimeZone.getDefault());

        try {
            Date date = dateTimeFormat.parse(time);

            String dateToCompare = simpleDateFormat.format(date);
            String dateNow = simpleDateFormat.format(new Date());
            String dateYesterday = getYesterdayDateString();

            if (dateToCompare.equals(dateNow)) { // Same day
                convertedDate = context.getResources().getString(R.string.today).toUpperCase();
            } else if (dateToCompare.equals(dateYesterday)) { // Yesterday
                convertedDate = context.getResources().getString(R.string.yesterday).toUpperCase();
            } else { // Other day
                if (Locale.getDefault().toString().equals("es_ES")) { // Spanish format
                    convertedDate = spanishDateFormat.format(date).toUpperCase();
                } else {
                    convertedDate = defaultDateFormat.format(date).toUpperCase();
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return convertedDate;
    }

    /* It converts the date and time of the messages saved in the local database into hour minute format with its default timezone */
    public static String formatTime(String time, Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = sdf.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat requiredDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        requiredDateFormat.setTimeZone(TimeZone.getDefault());
        String convertedTime = null;
        if (date != null) {
            convertedTime = requiredDateFormat.format(date);
        }
        return convertedTime;
    }

    private static boolean isTimeAutomatic(Context c) {
        return Settings.Global.getInt(c.getContentResolver(), Settings.Global.AUTO_TIME, 0) == 1;
    }

    private static boolean isTimeZoneAutomatic(Context c) {
        return Settings.Global.getInt(c.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0) == 1;
    }

    /* Show the loading dialog */
    public static void showDialogLoading(AlertDialog dialog) {
        dialog.show();
        dialog.setContentView(R.layout.dialog_processing);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(470, 178);
    }

    /* Show the registering dialog */
    public static void showDialogRegistering(AlertDialog dialog) {
        dialog.show();
        dialog.setContentView(R.layout.dialog_registering);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(470, 178);
    }

    /* Reduce quality of image */
    public static byte[] reduceQualityImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] imageInByte = byteArrayOutputStream.toByteArray();
        return imageInByte;
    }

    /* Transform bitmap image into ByteArray*/
    public static byte[] imageToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageInByte = byteArrayOutputStream.toByteArray();
        return imageInByte;
    }

    /* Get the bitmap orientation according to its metadata to put it in the correct position */
    public static Bitmap modifyOrientation(Context context, Bitmap bitmap, Uri imageUri) {
        InputStream inputStream = null;
        ExifInterface exif = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            exif = new ExifInterface(inputStream);
        } catch (IOException e) {
            System.out.println(e.toString());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    System.out.println(ignored.toString());
                }
            }
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);
            default:
                return bitmap;
        }
    }

    /* To rotate the bitmap */
    private static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /* To flip the bitmap */
    private static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /* Check if there is Internet connection */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /* Check if keyboard is closed */
    public static void closeKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    /* Transform dp to pixels */
    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    /* Get the inches of your screen */
    public static double getScreenInches(Activity activity) {
        int mWidthPixels = 0;
        int mHeightPixels = 0;
        Point realSize = new Point();

        WindowManager windowManager = activity.getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        try {
            Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        mWidthPixels = realSize.x;
        mHeightPixels = realSize.y;

        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        double x = Math.pow(mWidthPixels / dm.xdpi, 2);
        double y = Math.pow(mHeightPixels / dm.ydpi, 2);
        double screenInches = Math.sqrt(x + y);
        double roundOff = Math.round(screenInches * 10.0) / 10.0;
        return roundOff;
    }

    /* Cambiar color Status Bar */
    public static void changeStatusBarColor(Activity activity, int color) {
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(activity, color));
    }

    /* adjustResize with slow transition - set the android parameter: animateLayoutChanges = "true" in the layout */
    public static void setLayoutTransition(ViewGroup view) {
        LayoutTransition layoutTransition = view.getLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
    }

    /* Check if it is a valid email */
    public static boolean isValidEmail(CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    /* To show or not the error under the EditText */
    public static void setTvError(TextView tvError, EditText editText, String text, Boolean show) {
        if (show) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(text);
            editText.setBackgroundResource(R.drawable.et_bg_error);
        } else {
            if (tvError.getVisibility() == View.VISIBLE) {
                tvError.setVisibility(View.GONE);
                tvError.setText(text);
                editText.setBackgroundResource(R.drawable.et_bg);
            }
        }
    }
}
