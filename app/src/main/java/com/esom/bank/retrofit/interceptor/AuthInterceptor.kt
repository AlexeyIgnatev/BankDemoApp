package com.esom.bank.retrofit.interceptor

import android.util.Base64
import android.util.Log
import com.esom.bank.common.session.SessionManager
import com.esom.bank.retrofit.exception.NotLoggedInException
import com.esom.bank.screens.auth.data.AuthLocalDataSource
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import javax.inject.Inject


class AuthInterceptor @Inject constructor(
    private val authLocalDataSource: AuthLocalDataSource,
    private val sessionManager: SessionManager
) :
    Interceptor {
    companion object {
        private const val AUTH_HEADER = "Authorization"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val login = authLocalDataSource.getLogin()?.trim()
        val password = authLocalDataSource.getPassword()?.trim()

        if (!login.isNullOrEmpty() && !password.isNullOrEmpty()) {
            Log.e("login+passowrd", "$login $password")
            request = request.newBuilder()
                .header(AUTH_HEADER, Credentials.basic(login, password))
                .build()
        }

        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                authLocalDataSource.setLogin(null)
                authLocalDataSource.setPassword(null)
                sessionManager.notifyLoggedOut()
                throw NotLoggedInException()
            }
        }
        return response
    }
}
