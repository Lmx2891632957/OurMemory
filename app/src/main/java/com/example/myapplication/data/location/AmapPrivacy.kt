package com.example.myapplication.data.location

import android.content.Context
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import com.amap.api.services.core.ServiceSettings

/**
 * 高德 SDK 的隐私合规初始化。
 *
 * 高德从定位 5.6.0 / 地图 8.1.0 / 搜索 SDK 起强制要求：在创建任何 SDK 对象之前，
 * 必须调用 updatePrivacyShow / updatePrivacyAgree，否则 SDK 不可用。
 * 首次启动时由隐私弹窗获取用户同意后调用；已同意过的用户会在 App 启动时补调一次。
 */
object AmapPrivacy {

    private const val PREFS = "privacy"
    private const val KEY_AGREED = "amap_privacy_agreed"
    private var initialized = false

    fun isAgreed(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AGREED, false)

    fun agree(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AGREED, true).apply()
        applyCompliance(context)
    }

    /** 幂等：多次调用只会真正执行一次。必须在构造任何高德对象前调用。 */
    fun applyCompliance(context: Context) {
        if (initialized) return
        runCatching {
            AMapLocationClient.updatePrivacyShow(context, true, true)
            AMapLocationClient.updatePrivacyAgree(context, true)
            MapsInitializer.updatePrivacyShow(context, true, true)
            MapsInitializer.updatePrivacyAgree(context, true)
            ServiceSettings.updatePrivacyShow(context, true, true)
            ServiceSettings.updatePrivacyAgree(context, true)
        }
        initialized = true
    }
}
