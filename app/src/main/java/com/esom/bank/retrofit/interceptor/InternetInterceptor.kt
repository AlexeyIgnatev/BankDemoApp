package com.esom.bank.retrofit.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * Interceptor : Internet
 */
class InternetInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.i(
            "InternetInterceptor",
            "dispatching ${request.method} ${request.url.encodedPath}",
        )
        try {
            return chain.proceed(request)
        } catch (e: IOException) {
            Log.e("InternetInterceptor", "network request failed", e)
            throw e
        } catch (e: Exception) {
            Log.e("InternetInterceptor", "request failed before receiving an HTTP response", e)
            throw e
        }
    }
}
