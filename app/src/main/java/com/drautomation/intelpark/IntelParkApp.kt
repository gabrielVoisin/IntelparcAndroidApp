package com.drautomation.intelpark

import android.app.Application
import android.content.Context
import com.drautomation.intelpark.ble.BleIdentifier
import com.drautomation.intelpark.data.ApiClient
import com.drautomation.intelpark.data.DriverRepository
import com.drautomation.intelpark.data.SessionStore
import com.drautomation.intelpark.data.StaffRepository
import org.osmdroid.config.Configuration

/** Everything the screens need, created once. */
class AppContainer(context: Context) {
    val store = SessionStore(context)
    val api = ApiClient(store)
    val driver = DriverRepository(api, store)
    val staff = StaffRepository(api, store)
    val ble = BleIdentifier(context)
}

class IntelParkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // OpenStreetMap tiles require an identifying user agent
        Configuration.getInstance().userAgentValue = packageName
    }
}
