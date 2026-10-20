package com.esom.bank.retrofit.interceptor

import com.esom.bank.common.session.SessionManager
import com.esom.bank.common.session.SessionSnapshot
import com.esom.bank.common.session.SessionTokenGuard
import com.esom.bank.retrofit.exception.NotLoggedInException
import com.esom.bank.screens.auth.data.AuthLocalDataSource
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class AuthInterceptorTest {
    private val auth = MemoryAuth()
    private val sessionManager = SessionManager()
    private var sentRequest: Request? = null

    @Test
    fun currentUnauthorizedSessionIsClearedAndNotified() {
        auth.saveSession("user", "token")

        assertUnauthorized { respond(401) }

        assertEquals("Bearer token", sentRequest?.header("Authorization"))
        assertNull(auth.getAccessToken())
        assertNull(auth.getLogin())
        assertEquals(1L, sessionManager.loggedOutAt.value)
    }

    @Test
    fun lateUnauthorizedResponsePreservesNewSession() {
        auth.saveSession("old-user", "old-token")

        assertUnauthorized { respond(401) { auth.saveSession("new-user", "new-token") } }

        assertEquals("Bearer old-token", sentRequest?.header("Authorization"))
        assertEquals("new-token", auth.getAccessToken())
        assertEquals("new-user", auth.getLogin())
        assertEquals(0L, sessionManager.loggedOutAt.value)
    }

    @Test
    fun reauthenticationWithSameTokenHasNewGeneration() {
        auth.saveSession("old-user", "same-token")

        assertUnauthorized { respond(401) { auth.saveSession("new-user", "same-token") } }

        assertEquals("same-token", auth.getAccessToken())
        assertEquals("new-user", auth.getLogin())
        assertEquals(0L, sessionManager.loggedOutAt.value)
    }

    @Test
    fun logoutAndLoginWithSameTokenPreserveReplacement() {
        auth.saveSession("user", "same-token")

        assertUnauthorized {
            respond(401) {
                auth.clearAuthData()
                auth.saveSession("user", "same-token")
            }
        }

        assertEquals("same-token", auth.getAccessToken())
        assertEquals(0L, sessionManager.loggedOutAt.value)
    }

    @Test
    fun anonymousUnauthorizedResponseDoesNotClearNewLogin() {
        respond(401) { auth.saveSession("user", "new-token") }.use {
            assertEquals(401, it.code)
        }

        assertNull(sentRequest?.header("Authorization"))
        assertEquals("new-token", auth.getAccessToken())
        assertEquals(0L, sessionManager.loggedOutAt.value)
    }

    @Test
    fun failedLoginDoesNotAttachBearerOrClearExistingSession() {
        auth.saveSession("user", "token")

        respond(401, "/users/auth/login").use { assertEquals(401, it.code) }

        assertNull(sentRequest?.header("Authorization"))
        assertEquals("token", auth.getAccessToken())
        assertEquals(0L, sessionManager.loggedOutAt.value)
    }

    @Test
    fun nonUnauthorizedResponsePreservesSession() {
        auth.saveSession("user", "token")

        respond(403).use { assertEquals(403, it.code) }

        assertEquals("token", auth.getAccessToken())
        assertEquals(0L, sessionManager.loggedOutAt.value)
    }

    @Test
    fun failedOldLoginCleanupCannotClearReplacementSession() {
        val pending = auth.saveSession("old", "old-token")
        auth.saveSession("new", "new-token")

        assertFalse(auth.clearAuthDataIfCurrent(pending))
        assertEquals("new-token", auth.getAccessToken())
    }

    private fun respond(
        code: Int,
        path: String = "/users/me",
        beforeResponse: () -> Unit = {}
    ): Response {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(auth, sessionManager))
            .addInterceptor { chain ->
                sentRequest = chain.request()
                beforeResponse()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("test")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()
        return client.newCall(Request.Builder().url("https://example.invalid$path").build()).execute()
    }

    private fun assertUnauthorized(request: () -> Response) {
        assertThrows(NotLoggedInException::class.java) { request().close() }
    }

    private class MemoryAuth : AuthLocalDataSource {
        private val guard = SessionTokenGuard()
        private var login: String? = null
        private var token: String? = null

        override fun getLogin(): String? = login
        override fun getAccessToken(): String? = getSessionSnapshot().accessToken
        override fun getSessionSnapshot(): SessionSnapshot = guard.snapshot { token }
        override fun saveSession(login: String, accessToken: String): SessionSnapshot = guard.change {
            this.login = login
            token = accessToken
            getSessionSnapshot()
        }
        override fun clearAuthDataIfCurrent(session: SessionSnapshot): Boolean =
            guard.invalidate(session, { token }, ::clearCredentials)
        override fun clearAuthData() = guard.change { clearCredentials() }

        private fun clearCredentials() {
            login = null
            token = null
        }
    }
}
