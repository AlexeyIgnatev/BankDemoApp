package com.esom.bank.screens.auth.data

import com.tencent.mmkv.MMKV
import javax.inject.Inject

interface AuthLocalDataSource {
    fun getLogin(): String?
    fun setLogin(login: String?)
    fun getPassword(): String?
    fun setPassword(password: String?)
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

    override fun getPassword(): String? = storage.decodeString("password")
    override fun setPassword(password: String?) {
        storage.encode("password", password)
    }

    override fun clearAuthData() {
        storage.removeValueForKey("login")
        storage.removeValueForKey("password")
    }
}