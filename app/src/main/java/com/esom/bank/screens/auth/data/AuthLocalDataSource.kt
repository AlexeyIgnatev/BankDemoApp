package com.esom.bank.screens.auth.data

import com.esom.bank.common.security.SecureMmkv
import com.esom.bank.common.session.SessionSnapshot
import com.esom.bank.common.session.SessionTokenGuard
import javax.inject.Inject
import javax.inject.Singleton

interface AuthLocalDataSource {
    fun getLogin(): String?
    fun getAccessToken(): String?
    fun getSessionSnapshot(): SessionSnapshot
    fun saveSession(login: String, accessToken: String): SessionSnapshot
    fun clearAuthDataIfCurrent(session: SessionSnapshot): Boolean
    fun clearAuthData()
}

@Singleton
class AuthLocalDataSourceImpl @Inject constructor(): AuthLocalDataSource {
    private val sessionGuard = SessionTokenGuard()
    private val storage by lazy {
        SecureMmkv.storage("AuthLocalDataSource")
    }

    override fun getLogin(): String? = storage.decodeString("login")
    override fun getAccessToken(): String? = getSessionSnapshot().accessToken

    override fun getSessionSnapshot(): SessionSnapshot =
        sessionGuard.snapshot { storage.decodeString("access_token") }

    override fun saveSession(login: String, accessToken: String): SessionSnapshot = sessionGuard.change {
        storage.encode("login", login)
        storage.encode("access_token", accessToken)
        getSessionSnapshot()
    }

    override fun clearAuthDataIfCurrent(session: SessionSnapshot): Boolean =
        sessionGuard.invalidate(session, { storage.decodeString("access_token") }, ::clearStoredCredentials)

    override fun clearAuthData() = sessionGuard.change { clearStoredCredentials() }

    private fun clearStoredCredentials() {
        storage.removeValueForKey("login")
        storage.removeValueForKey("access_token")
        // Remove credentials saved by older app versions.
        storage.removeValueForKey("password")
    }
}
