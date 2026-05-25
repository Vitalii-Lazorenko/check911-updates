package com.example.check_911

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.entity.IpAddressEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch


import com.example.check_911.data.db.dao.DaoIpAddress
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


class IpAddressViewModel(
    application: Application,
    private val daoIpAddress: DaoIpAddress

) : AndroidViewModel(application) {

    val ipAddress = daoIpAddress.getIpAddressFlow().map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val event = MutableSharedFlow<IpAddressEvent>(extraBufferCapacity = 1)

    fun setIpAddress(text: String) {
        viewModelScope.launch {
            try {

                daoIpAddress.setIpAddress(IpAddressEntity(text))

                event.tryEmit(IpAddressEvent.Added)

            } catch (e: Exception) {
                event.tryEmit(IpAddressEvent.Error(e.message ?: "невідома помилка"))
            }
        }
    }

    fun deleteIpAddress() {
        viewModelScope.launch {
            try {

                daoIpAddress.deleteIpAddress()

                event.tryEmit(IpAddressEvent.Added)

            } catch (e: Exception) {
                event.tryEmit(IpAddressEvent.Error(e.message ?: "невідома помилка"))
            }
        }
    }
}

sealed interface IpAddressEvent {
    object Added : IpAddressEvent
    class Error(val msg: String) : IpAddressEvent
}