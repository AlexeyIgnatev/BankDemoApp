package com.esom.bank.screens.auth.data

import com.tencent.mmkv.MMKV
import javax.inject.Inject

interface AuthLocalDataSource {
    fun getLogin(): String?
    fun setLogin(login: String?)

    fun getPassword(): String?
    fun setPassword(password: String?)
}

class AuthLocalDataSourceImpl @Inject constructor() : AuthLocalDataSource {
    private val storage by lazy {
        MMKV.mmkvWithID(
            "AuthLocalDataSource",
            MMKV.MULTI_PROCESS_MODE
        )
    }

    override fun getLogin(): String? = storage.getString("login", null)

    override fun setLogin(login: String?) {
        storage.encode("login", login)
    }

    override fun getPassword(): String? = storage.getString("password", null)

    override fun setPassword(password: String?) {
        storage.encode("password", password)
    }
}