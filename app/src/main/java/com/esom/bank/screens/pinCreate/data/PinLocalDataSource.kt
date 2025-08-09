package com.esom.bank.screens.pinCreate.data

import com.tencent.mmkv.MMKV
import javax.inject.Inject

interface  PinLocalDataSource {
    fun isBio(): Boolean
    fun setBio(bio: Boolean)
}
class PinLocalDataSourceImpl @Inject constructor(): PinLocalDataSource {
    private val storage by lazy {
        MMKV.mmkvWithID(
            "PinLocalDataSource",
            MMKV.MULTI_PROCESS_MODE
        )
    }

    override fun isBio(): Boolean = storage.decodeBool("bio", false)

    override fun setBio(bio: Boolean) {
        storage.encode("bio", bio)
    }


}