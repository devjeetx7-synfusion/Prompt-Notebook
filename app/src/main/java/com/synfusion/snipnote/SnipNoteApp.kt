package com.synfusion.snipnote

import android.app.Application

class SnipNoteApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.seedDefaults()
    }
}
