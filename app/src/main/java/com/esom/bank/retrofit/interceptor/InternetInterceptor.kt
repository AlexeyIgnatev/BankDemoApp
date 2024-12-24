package com.esom.bank.retrofit.interceptor

import com.esom.bank.retrofit.exception.NoInternetException
import com.esom.bank.retrofit.service.InternetConnectionService
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * Interceptor : Internet
 */
class InternetInterceptor @Inject constructor(
    private val connectionService: InternetConnectionService
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            if (!connectionService.checkInternetConnection()) {
                throw NoInternetException(RuntimeException())
            }
            return chain.proceed(chain.request())
        } catch (e: IOException) {
            if (!connectionService.checkInternetConnection()) {
                throw NoInternetException(e)
            }
            throw e
        }
    }
}