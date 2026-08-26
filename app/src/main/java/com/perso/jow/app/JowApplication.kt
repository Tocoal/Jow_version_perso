package com.perso.jow.app

import android.app.Application
import com.perso.jow.app.di.AppContainer

class JowApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
