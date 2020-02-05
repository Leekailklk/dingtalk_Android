package com.example.administrator.networktest.http;

import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;

public class VolleyResponseErrorListener implements Response.ErrorListener {
    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e("TAG", error.getMessage(), error);

    }
}