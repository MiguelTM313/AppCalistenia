package com.calistenia.app

import android.app.Application
import androidx.room.Room
import com.calistenia.app.data.AppRepository
import com.calistenia.app.data.PreferencesRepository
import com.calistenia.app.data.local.AppDatabase

class CalisthenicsApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, AppDatabase::class.java, "calisthenics.db").addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build() }
    val repository by lazy { AppRepository(database) }
    val preferences by lazy { PreferencesRepository(this) }
}
