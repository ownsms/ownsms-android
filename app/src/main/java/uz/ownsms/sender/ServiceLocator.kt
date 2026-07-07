package uz.ownsms.sender

import android.content.Context
import androidx.room.Room
import uz.ownsms.sender.data.db.AppDatabase
import uz.ownsms.sender.data.prefs.Settings
import uz.ownsms.sender.data.remote.ApiClient
import uz.ownsms.sender.data.remote.DevApi
import uz.ownsms.sender.data.remote.OwnSmsApi

/** Tiny manual DI container — initialized once in [OwnSmsApp]. */
object ServiceLocator {
    lateinit var db: AppDatabase
        private set
    lateinit var settings: Settings
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        db = Room.databaseBuilder(app, AppDatabase::class.java, "ownsms.db")
            .fallbackToDestructiveMigration()
            .build()
        settings = Settings(app)
    }

    /** A fresh API client bound to the currently configured base URL + device token. */
    fun api(): OwnSmsApi = ApiClient.create(settings.baseUrl, settings.deviceToken)

    /** The developer-facing API, authenticated with the account's api_key. */
    fun devApi(): DevApi = ApiClient.retrofit(settings.baseUrl, settings.apiKey).create(DevApi::class.java)
}
