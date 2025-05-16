package com.esom.bank.retrofit.interceptor

import android.util.Base64
import com.esom.bank.retrofit.exception.NotLoggedInException
import com.esom.bank.screens.auth.data.AuthLocalDataSource
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
import javax.inject.Inject


class AuthInterceptor @Inject constructor(
    private val authLocalDataSource: AuthLocalDataSource
) :
    Interceptor {
    companion object {
        private const val AUTH_HEADER = "Authorization"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val login = authLocalDataSource.getLogin()
        val password = authLocalDataSource.getPassword()

        if (login != null && password != null) {
            request = request.newBuilder().header(
                AUTH_HEADER,
                "Basic ${Base64.encodeToString("$login:$password".toByteArray(), Base64.NO_WRAP)}"
            ).build()
        }
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                authLocalDataSource.setLogin(null)
                authLocalDataSource.setPassword(null)
                throw NotLoggedInException()
            }
        }
        return response
    }
}
