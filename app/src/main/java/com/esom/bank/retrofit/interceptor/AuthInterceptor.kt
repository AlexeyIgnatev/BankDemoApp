package com.esom.bank.retrofit.interceptor

import android.util.Log
import com.esom.bank.common.session.SessionManager
import com.esom.bank.retrofit.exception.NotLoggedInException
import com.esom.bank.screens.auth.data.AuthLocalDataSource
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
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

        val accessToken = authLocalDataSource.getAccessToken()?.trim()
        val isLoginRequest = request.url.encodedPath.endsWith("/users/auth/login")

        if (!accessToken.isNullOrEmpty() && !isLoginRequest) {
            request = request.newBuilder()
                .header(AUTH_HEADER, "Bearer $accessToken")
                .build()
        }

        Log.d(
            "AuthInterceptor",
            "request ${request.method} ${request.url.encodedPath} " +
                "hasBearer=${!accessToken.isNullOrEmpty()}"
        )

        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            Log.w("AuthInterceptor", "response ${response.code} ${request.url.encodedPath}")
            if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                authLocalDataSource.clearAuthData()
                sessionManager.notifyLoggedOut()
                response.close()
                throw NotLoggedInException()
            }
        }
        return response
    }
}
