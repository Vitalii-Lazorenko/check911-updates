package com.example.check_911.data.db.repository

import com.example.check_911.data.db.dao.DaoIpAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class AppUrlRepository(ipAddressDao: DaoIpAddress) {
    private val scope = CoroutineScope(SupervisorJob())

    val host: SharedFlow<String> =
//        ipAddressDao.getIpAddressFlow().map { it ?: "server" }
        ipAddressDao.getIpAddressFlow().map { it ?: "server" }
//        ipAddressDao.getIpAddressFlow().map { it ?: "" }
//        ipAddressDao.getIpAddressFlow().map { it ?: "10.128.233.15" }
//        ipAddressDao.getIpAddressFlow().map { it ?: "10.132.163.11" }
//        ipAddressDao.getIpAddressFlow().map { it ?: "10.133.132.13" }
            .shareIn(scope, SharingStarted.Lazily, 1)
}