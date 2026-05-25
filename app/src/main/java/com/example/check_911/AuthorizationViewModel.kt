package com.example.check_911

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.repository.AuthorizationRepository
import com.example.check_911.data.networking.networking.models.reserve.AuthorizationResponse
import kotlinx.coroutines.launch


class AuthorizationViewModel(private val authorizationRepository: AuthorizationRepository) : ViewModel() {
    private val _authState = MutableLiveData<Result<AuthorizationResponse>>()
    val authState: LiveData<Result<AuthorizationResponse>> = _authState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = authorizationRepository.login(username, password)
                _authState.postValue(Result.success(response))
            } catch (e: Exception) {
                _authState.postValue(Result.failure(e))
            }
        }
    }
}


//class AuthorizationViewModel (private val authorizationRepository: AuthorizationRepository) : ViewModel() {
//    private val _authState = MutableLiveData<Result<AuthorizationResponse>>()
//    val authState: LiveData<Result<AuthorizationResponse>> = _authState
//
//    fun login(username: String, password: String) {
//        viewModelScope.launch {
//            try {
//                val response = authorizationRepository.login(username, password)
//                _authState.postValue(Result.success(response))
//            } catch (e: Exception) {
//                _authState.postValue(Result.failure(e))
//            }
//        }
//    }
//}