package uz.ownsms.sender

import android.app.Application

class OwnSmsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
