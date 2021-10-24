package com.gowtham.library.utils;

import android.util.Log;

public class LogMessage {

   public static final boolean IS_LOG = true;

    public static void v(String msg) {
        if (IS_LOG)
            Log.v("A.lee ::", msg);
    }

    public static void e(String msg) {
        if (IS_LOG)
            Log.e("A.lee ::", msg);
    }
}
