package com.example.check_911

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.check_911.data.db.dao.DaoUsers
import com.example.check_911.data.db.entity.UsersEntity
import com.example.check_911.data.utils.AppLogger

class SelectionViewModel(private val daoUsers: DaoUsers) : ViewModel() {
    val users: LiveData<List<UsersEntity>> = daoUsers.getUsers().asLiveData()

    fun saveSelectedUser(user: UsersEntity, context: Context) {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
//            .putLong("selected_user_id", user.id)
            .putString("selected_user_id", user.positionName)
            .putLong("selected_user_idGamma", user.idGamma)
            .putString("selected_user_name", user.userName)
            .apply()

        AppLogger.log("SelectionUser", "Обрано користувача: ${user.userName} (${user.positionName}, idGamma = ${user.idGamma}")
    }

    fun getSelectedUserId(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("selected_user_id", -1)
    }
}
