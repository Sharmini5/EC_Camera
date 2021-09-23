package com.example.ec_camera;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Helper {

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

}
