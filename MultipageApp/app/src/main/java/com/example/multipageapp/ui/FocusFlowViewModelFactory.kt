package com.example.multipageapp.ui

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.example.multipageapp.FocusFlowApplication
import com.example.multipageapp.ui.focus.FocusViewModel
import com.example.multipageapp.ui.home.HomeViewModel
import com.example.multipageapp.ui.insights.InsightsViewModel
import com.example.multipageapp.ui.settings.SettingsViewModel

class FocusFlowViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val app: FocusFlowApplication
) : AbstractSavedStateViewModelFactory(owner, null) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        val repo = app.repository
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repo) as T
            modelClass.isAssignableFrom(FocusViewModel::class.java) ->
                FocusViewModel(repo, handle) as T
            modelClass.isAssignableFrom(InsightsViewModel::class.java) ->
                InsightsViewModel(repo) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown VM: ${modelClass.name}")
        }
    }
}
