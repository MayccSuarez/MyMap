package com.maycc.mymap

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo


fun isAvailableNetwork(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                                                as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo

    return networkInfo != null && networkInfo.isConnected
}
