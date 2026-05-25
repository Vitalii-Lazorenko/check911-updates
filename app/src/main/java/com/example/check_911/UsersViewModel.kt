package com.example.check_911

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.dao.DaoUsers
import com.example.check_911.data.db.entity.UsersEntity
import com.example.check_911.data.db.repository.UsersRepository
import com.example.check_911.data.mapper.toEntity
import kotlinx.coroutines.launch

class UsersViewModel(private val usersRepository: UsersRepository, private val usersDao: DaoUsers, private val service: ApiServiceData,) : ViewModel() {
    private val _usersState = MutableLiveData<Result<Unit>>()
    val usersState: LiveData<Result<Unit>> = _usersState

    fun loadUsers(token: String) {
        viewModelScope.launch {
            try {
                val users = service.getUsers(token).toEntity()
                usersDao.refreshUsers(users)
                Log.e("UsersViewModel", "загрузки пользователей: ${users}")
//                usersRepository.getAndSaveUsers(token)
                _usersState.postValue(Result.success(Unit))
            } catch (e: Exception) {
                _usersState.postValue(Result.failure(e))
                // Логируем ошибку, но не выбрасываем, так как загрузка опросников не должна прерываться
                Log.e("UsersViewModel", "Ошибка загрузки пользователей: ${e.message}")
            }
        }
    }
}
