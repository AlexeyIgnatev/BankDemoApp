package com.esom.bank.retrofit.interceptor

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

        val session = authLocalDataSource.getSessionSnapshot()
        val accessToken = session.accessToken
        val isLoginRequest = request.url.encodedPath.endsWith("/users/auth/login")

        if (!accessToken.isNullOrEmpty() && !isLoginRequest) {
            request = request.newBuilder()
                .header(AUTH_HEADER, "Bearer $accessToken")
                .build()
        }

        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            // Anonymous startup calls (for example FCM registration) and failed login
            // attempts can legitimately return 401. Only a protected request made with
            // an existing bearer token proves that the current session has expired.
            if (
                response.code == HttpURLConnection.HTTP_UNAUTHORIZED &&
                !isLoginRequest &&
                !accessToken.isNullOrEmpty()
            ) {
                if (authLocalDataSource.clearAuthDataIfCurrent(session)) {
                    sessionManager.notifyLoggedOut()
                }
                response.close()
                throw NotLoggedInException()
            }
        }
        return response
    }
}
