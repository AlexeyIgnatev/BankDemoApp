package com.esom.bank.retrofit.service

import android.content.Context
import android.net.ConnectivityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface InternetConnectionService {
    fun checkInternetConnection(): Boolean
}

class InternetConnectionServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : InternetConnectionService {
    override fun checkInternetConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}