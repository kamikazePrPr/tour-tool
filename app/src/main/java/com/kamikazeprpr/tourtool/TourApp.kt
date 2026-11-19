package com.kamikazeprpr.tourtool

import android.app.Application
import com.kamikazeprpr.tourtool.data.AppLog

class TourApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 日志系统要在一切之前起来：崩溃捕获靠它
        AppLog.init(this)
    }
}
