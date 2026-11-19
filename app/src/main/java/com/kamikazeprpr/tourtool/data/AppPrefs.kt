package com.kamikazeprpr.tourtool.data

import android.content.Context

/**
 * 极简本地偏好设置。首启说明的确认标记就存在这里。
 */
object AppPrefs {
    private const val FILE = "app_prefs"
    private const val KEY_FIRST_RUN_ACK = "first_run_acked"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isFirstRunAcked(ctx: Context): Boolean =
        sp(ctx).getBoolean(KEY_FIRST_RUN_ACK, false)

    fun setFirstRunAcked(ctx: Context) {
        sp(ctx).edit().putBoolean(KEY_FIRST_RUN_ACK, true).apply()
    }
}
