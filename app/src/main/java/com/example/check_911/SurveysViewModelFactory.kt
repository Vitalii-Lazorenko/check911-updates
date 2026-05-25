package com.example.check_911

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.check_911.data.db.MainDb

class SurveysViewModelFactory(
    private val database: MainDb,
    private val app: Application,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(SurveysViewModel::class.java)) {
            return SurveysViewModel(database, handle, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
