package com.esom.bank.screens.pinCreate.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import javax.inject.Inject

enum class LockType(val value: String) {
    PIN("PIN"),
    PATTERN("PATTERN");

    companion object {
        fun from(value: String?): LockType? {
            return values().firstOrNull { it.value == value }
        }
    }
}

interface PinLocalDataSource {
    fun isBio(): Boolean
    fun setBio(bio: Boolean)
    fun hasLock(): Boolean
    fun getLockType(): LockType?
    fun savePin(pin: String)
    fun verifyPin(pin: String): Boolean
    fun savePattern(pattern: List<Int>)
    fun verifyPattern(pattern: List<Int>): Boolean
    fun clearLock()
}

class PinLocalDataSourceImpl @Inject constructor() : PinLocalDataSource {
    private val storage by lazy {
        MMKV.mmkvWithID(
            "PinLocalDataSource",
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val gson = Gson()

    override fun isBio(): Boolean = storage.decodeBool("bio", false)

    override fun setBio(bio: Boolean) {
        storage.encode("bio", bio)
    }

    override fun hasLock(): Boolean {
        return when (getLockType()) {
            LockType.PIN -> !storage.decodeString("pinCode").isNullOrBlank()
            LockType.PATTERN -> getSavedPattern().isNotEmpty()
            null -> false
        }
    }

    override fun getLockType(): LockType? = LockType.from(storage.decodeString("lockType"))

    override fun savePin(pin: String) {
        storage.encode("lockType", LockType.PIN.value)
        storage.encode("pinCode", pin)
    }

    override fun verifyPin(pin: String): Boolean {
        if (getLockType() != LockType.PIN) return false
        return storage.decodeString("pinCode") == pin
    }

    override fun savePattern(pattern: List<Int>) {
        storage.encode("lockType", LockType.PATTERN.value)
        storage.encode("pattern", gson.toJson(pattern))
    }

    override fun verifyPattern(pattern: List<Int>): Boolean {
        if (getLockType() != LockType.PATTERN) return false
        return getSavedPattern() == pattern
    }

    override fun clearLock() {
        storage.removeValueForKey("lockType")
        storage.removeValueForKey("pinCode")
        storage.removeValueForKey("pattern")
        storage.removeValueForKey("bio")
    }

    private fun getSavedPattern(): List<Int> {
        val raw = storage.decodeString("pattern") ?: return emptyList()
        return try {
            gson.fromJson(raw, object : TypeToken<List<Int>>() {}.type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

}
