package com.example.multipageapp

import android.app.Application
import com.example.multipageapp.data.FocusRepository

class FocusFlowApplication : Application() {
    lateinit var repository: FocusRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FocusRepository(this)
    }
}
