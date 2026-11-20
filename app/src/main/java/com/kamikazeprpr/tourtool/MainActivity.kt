package com.kamikazeprpr.tourtool

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kamikazeprpr.tourtool.data.AppPrefs
import com.kamikazeprpr.tourtool.ui.FirstRunScreen
import com.kamikazeprpr.tourtool.ui.HomeScreen
import com.kamikazeprpr.tourtool.ui.theme.TourToolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 走 edge-to-edge，Compose 才能拿到键盘高度做 imePadding；
        // 各页面自己用 systemBarsPadding 让开状态栏和导航栏
        enableEdgeToEdge()
        setContent {
            TourToolTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    var acked by remember { mutableStateOf(AppPrefs.isFirstRunAcked(ctx)) }

    if (!acked) {
        FirstRunScreen(
            onAgree = {
                AppPrefs.setFirstRunAcked(ctx)
                acked = true
            },
            onExit = {
                (ctx as? Activity)?.finishAffinity()
            },
        )
    } else {
        HomeScreen()
    }
}
