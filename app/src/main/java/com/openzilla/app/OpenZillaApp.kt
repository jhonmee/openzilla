package com.openzilla.app

import android.app.Application
import com.openzilla.app.data.ExportImportManager
import com.openzilla.app.data.HabitRepository
import com.openzilla.app.data.PinManager
import com.openzilla.app.data.SettingsRepository
import com.openzilla.app.notification.NotificationScheduler

/**
 * Tiny manual dependency container — deliberately not pulling in a DI framework for an app
 * this size; fewer moving parts means fewer places for something to leak or misbehave.
 * Everything here is a plain singleton created once and reused; nothing holds an Activity
 * or Compose reference, so nothing here can leak a screen.
 */
class OpenZillaApp : Application() {
    lateinit var repository: HabitRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var pinManager: PinManager
        private set
    lateinit var exportImportManager: ExportImportManager
        private set

    override fun onCreate() {
        super.onCreate()
        repository = HabitRepository(this)
        settingsRepository = SettingsRepository(this)
        pinManager = PinManager(this)
        exportImportManager = ExportImportManager(this, repository)
        NotificationScheduler.ensureChannel(this)
    }
}
