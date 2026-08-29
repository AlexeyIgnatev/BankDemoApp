package com.esom.bank.screens.auth.data

import com.tencent.mmkv.MMKV
import javax.inject.Inject

interface AuthLocalDataSource {
    fun getLogin(): String?
    fun setLogin(login: String?)
    fun getAccessToken(): String?
    fun setAccessToken(accessToken: String?)
    fun clearAuthData()
}

class AuthLocalDataSourceImpl @Inject constructor(): AuthLocalDataSource {
    private val storage by lazy {
        MMKV.mmkvWithID(
            "AuthLocalDataSource",
            MMKV.MULTI_PROCESS_MODE
        )
    }

    override fun getLogin(): String? = storage.decodeString("login")
    override fun setLogin(login: String?) {
        storage.encode("login", login)
    }

    override fun getAccessToken(): String? = storage.decodeString("access_token")
    override fun setAccessToken(accessToken: String?) {
        storage.encode("access_token", accessToken)
    }

    override fun clearAuthData() {
        storage.removeValueForKey("login")
        storage.removeValueForKey("access_token")
        // Remove credentials saved by older app versions.
        storage.removeValueForKey("password")
    }
}
