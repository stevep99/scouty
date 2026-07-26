package io.github.stevep99.scouty.motors.sdk

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
abstract class SdkService : Service(), SdkCommon {
    private val binder = LocalBinder()
    override var sdkEventListener: SdkEventListener? = null
    inner class LocalBinder : Binder() {
        fun getService(): SdkService = this@SdkService
    }
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

}