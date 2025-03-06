package com.example.rcapp

import android.app.Application
import com.example.rcapp.data.database.UserDatabaseHelper


class MyApplication : Application() {
    private lateinit var databaseHelper: UserDatabaseHelper

    override fun onCreate() {
        super.onCreate()
        databaseHelper = UserDatabaseHelper(this)
    }
}
