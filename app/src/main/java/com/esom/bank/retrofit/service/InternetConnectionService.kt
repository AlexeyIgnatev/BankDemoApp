package com.esom.bank.retrofit.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface InternetConnectionService {
    fun checkInternetConnection(): Boolean
}

class InternetConnectionServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : InternetConnectionService {
    override fun checkInternetConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return false
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
