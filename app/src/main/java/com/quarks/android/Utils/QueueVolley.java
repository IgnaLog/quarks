package com.quarks.android.Utils;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class QueueVolley {
    private RequestQueue requestQueue;
    private static QueueVolley mInstance;

    public QueueVolley(Context context) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context);
        }
    }

    public static synchronized QueueVolley getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new QueueVolley(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public <T> void RequestQueueAdd(Request<T> req) {
        getRequestQueue().add(req);
    }
}
