package com.esom.bank.screens.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esom.bank.common.model.UiState
import com.esom.bank.common.utils.SingleLiveEvent
import com.esom.bank.screens.main.data.MainRepository
import com.esom.bank.screens.main.model.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainRepository: MainRepository
) : ViewModel() {
    private val _myData = MutableLiveData<UiState<UserModel>>()
    val myData: LiveData<UiState<UserModel>> = _myData

    private val _tokenBalance = MutableLiveData<UiState<Double>>()
    val tokenBalance: LiveData<UiState<Double>> = _tokenBalance

    private val _swapRes = SingleLiveEvent<UiState<Unit>>()
    val swapRes: LiveData<UiState<Unit>> = _swapRes

    private val _transferRes = SingleLiveEvent<UiState<Unit>>()
    val transferRes: LiveData<UiState<Unit>> = _transferRes

    fun isAuthenticated() = mainRepository.isAuthenticated()

    fun authenticate(login: String, password: String) {
        _myData.value = UiState.Loading()
        mainRepository.authenticate(login, password).onEach {
            _myData.value = it
        }.launchIn(viewModelScope)
    }

    fun updateUserData() {
        _myData.value = UiState.Loading()
        mainRepository.getUserInfo().onEach {
            _myData.value = it
        }.launchIn(viewModelScope)
    }

    fun transferFromFiat(amount: Double) {
        _swapRes.value = UiState.Loading()
        mainRepository.transferFromFiat(amount).onEach {
            _swapRes.value = it
        }.launchIn(viewModelScope)
    }

    fun transferToFiat(amount: Double) {
        _swapRes.value = UiState.Loading()
        mainRepository.transferToFiat(amount).onEach {
            _swapRes.value = it
        }.launchIn(viewModelScope)
    }

    fun transferToUser(amount: Double, address: String) {
        _transferRes.value = UiState.Loading()
        mainRepository.transferToUser(amount, address).onEach {
            _transferRes.value = it
        }.launchIn(viewModelScope)
    }
}