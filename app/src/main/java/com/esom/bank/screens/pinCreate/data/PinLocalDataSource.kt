package com.esom.bank.screens.pinCreate.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.esom.bank.common.security.SecureMmkv
import com.esom.bank.common.security.LocalUnlockProtection
import com.esom.bank.common.security.UnlockAttemptState
import javax.inject.Inject
import javax.inject.Singleton

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
    fun hasPin(): Boolean
    fun hasPattern(): Boolean
    fun getLockType(): LockType?
    fun savePin(pin: String)
    fun verifyPin(pin: String): Boolean
    fun savePattern(pattern: List<Int>)
    fun verifyPattern(pattern: List<Int>): Boolean
    fun unlockRetryAfterMillis(): Long
    fun resetUnlockAttempts()
    fun clearLock()
}

@Singleton
class PinLocalDataSourceImpl @Inject constructor() : PinLocalDataSource {
    private val storage by lazy {
        SecureMmkv.storage("PinLocalDataSource")
    }
    private val gson = Gson()
    private val unlockProtection = LocalUnlockProtection(
        readState = {
            val raw = storage.decodeString("unlockAttempts")
            if (raw == null) UnlockAttemptState() else {
                gson.fromJson(raw, UnlockAttemptState::class.java) ?: UnlockAttemptState()
            }
        },
        writeState = {
            check(storage.encode("unlockAttempts", gson.toJson(it)))
            storage.sync()
        }
    )

    override fun isBio(): Boolean = storage.decodeBool("bio", false)

    override fun setBio(bio: Boolean) {
        storage.encode("bio", bio)
    }

    override fun hasLock(): Boolean {
        return hasPin() || hasPattern()
    }

    override fun hasPin(): Boolean = !storage.decodeString("pinCode").isNullOrBlank()

    override fun hasPattern(): Boolean = getSavedPattern().isNotEmpty()

    override fun getLockType(): LockType? = LockType.from(storage.decodeString("lockType"))

    override fun savePin(pin: String) {
        storage.encode("lockType", LockType.PIN.value)
        storage.encode("pinCode", pin)
        resetUnlockAttempts()
    }

    override fun verifyPin(pin: String): Boolean {
        return unlockProtection.verify {
            pin.length == 4 && pin.all { it in '0'..'9' } &&
                hasPin() && storage.decodeString("pinCode") == pin
        }
    }

    override fun savePattern(pattern: List<Int>) {
        storage.encode("lockType", LockType.PATTERN.value)
        storage.encode("pattern", gson.toJson(pattern))
        resetUnlockAttempts()
    }

    override fun verifyPattern(pattern: List<Int>): Boolean {
        return unlockProtection.verify {
            pattern.size >= 4 && hasPattern() && getSavedPattern() == pattern
        }
    }

    override fun unlockRetryAfterMillis(): Long = unlockProtection.retryAfterMillis()

    override fun resetUnlockAttempts() = unlockProtection.reset()

    override fun clearLock() {
        storage.removeValueForKey("lockType")
        storage.removeValueForKey("pinCode")
        storage.removeValueForKey("pattern")
        storage.removeValueForKey("bio")
        resetUnlockAttempts()
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
