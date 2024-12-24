package com.esom.bank.screens.main.data

import com.esom.bank.BuildConfig
import com.esom.bank.R
import com.esom.bank.common.model.ApiResponse
import com.esom.bank.common.model.ErrorResponse
import com.esom.bank.screens.main.dto.AdminDto
import com.esom.bank.screens.main.dto.UserDto
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

interface MainCloudDataSource {
    fun getMe(): Flow<ApiResponse<UserDto>>
    fun changeBalance(userId: Int, newBalance: Double): Flow<ApiResponse<Unit>>
    fun getAllUsers(): Flow<ApiResponse<List<UserDto>>>
    fun getAdmin(): Flow<ApiResponse<AdminDto>>
}

class MainCloudDataSourceImpl @Inject constructor() : MainCloudDataSource {
    override fun getMe(): Flow<ApiResponse<UserDto>> =
        getAllUsers().map { value: ApiResponse<List<UserDto>> ->
            when (value) {
                is ApiResponse.Error -> return@map ApiResponse.Error(
                    value.message,
                    value.data,
                    value.code
                )

                is ApiResponse.Success -> return@map ApiResponse.Success(
                    value.data.first { it.id == BuildConfig.USER_ID }, 200
                )
            }
        }.flowOn(Dispatchers.IO)

    override fun changeBalance(userId: Int, newBalance: Double): Flow<ApiResponse<Unit>> = flow {
        try {
            Firebase.database.reference.child("users").child(userId.toString()).child("balance")
                .setValue(newBalance).await()
            emit(ApiResponse.Success(Unit, 200))
        } catch (e: Exception) {
            emit(ApiResponse.Error(R.string.something_went_wrong, ErrorResponse(message = e.message)))
        }
    }.flowOn(Dispatchers.IO)

    override fun getAllUsers(): Flow<ApiResponse<List<UserDto>>> = flow {
        try {
            val data =
                Tasks.await(Firebase.database.reference.child("users").get(), 10, TimeUnit.SECONDS)
            val users = mutableListOf<UserDto>()
            for (user in data.children) {
                users.add(
                    user.getValue(UserDto::class.java)!!.copy(
                        id = user.key!!.toInt()
                    )
                )
            }
            emit(ApiResponse.Success(users, 200))
        } catch (e: Exception) {
            emit(ApiResponse.Error(R.string.something_went_wrong, ErrorResponse(message = e.message)))
        }
    }.flowOn(Dispatchers.IO)

    override fun getAdmin(): Flow<ApiResponse<AdminDto>> = flow {
        try {
            val data = Firebase.database.reference.child("admin").get().await()
            val admin = data.getValue(AdminDto::class.java)!!
            emit(ApiResponse.Success(admin, 200))
        } catch (e: Exception) {
            emit(ApiResponse.Error(R.string.something_went_wrong, ErrorResponse(message = e.message)))
        }
    }.flowOn(Dispatchers.IO)
}
