package com.esom.bank.retrofit.interceptor

import android.util.Base64
import android.util.Log
import com.esom.bank.retrofit.exception.NotLoggedInException
import com.esom.bank.screens.auth.data.AuthLocalDataSource
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
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
            Log.e("login+passowrd", "$login $password")
            val auth = Credentials.basic(login, password, StandardCharsets.UTF_8)

            request = request.newBuilder()
                .header("Authorization", auth)
                .build()
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
