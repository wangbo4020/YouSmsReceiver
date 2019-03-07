package com.example.smssend.content

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import androidx.annotation.StringRes

/**
 * Created by Dylan on 2017/12/14.
 * 请将 key 或默认值都定义到 values/strings_preferences 中
 *
 */
object AppPreferences {

    private lateinit var res: Resources
    private lateinit var profile: SharedPreferences

    fun initialize(context: Context) {
        res = context.resources
        profile = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun has(@StringRes resId: Int): Boolean = profile.contains(res.getString(resId))

    fun clear(@StringRes resId: Int) {
        profile.edit().remove(res.getString(resId)).apply()
    }

    // 全都使用String保存
    fun put(@StringRes resId: Int, value: Any?) = profile.edit().apply {
        val key = res.getString(resId)
        if (value == null) {
            remove(key)
        } else {
            putString(key, value.toString())
        }.apply()
    }

    fun putString(@StringRes resId: Int, value: String?) = profile.edit().apply {
        val key = res.getString(resId)
        if (value == null) {
            remove(key)
        } else {
            putString(key, value)
        }.apply()
    }

    fun getString(@StringRes resId: Int): String? =
        profile.getString(res.getString(resId), null)

    fun getString(@StringRes resId: Int, defVal: String): String =
        profile.getString(res.getString(resId), defVal)!!

    fun getString(@StringRes resId: Int, @StringRes defResId: Int): String =
        profile.getString(res.getString(resId), null) ?: res.getString(defResId)

    fun putBoolean(@StringRes resId: Int, value: Boolean) {
        profile.edit().putBoolean(res.getString(resId), value).apply()
    }

    fun getBoolean(@StringRes resId: Int): Boolean =
        profile.getBoolean(res.getString(resId), false)

    fun getBoolean(@StringRes resId: Int, defVal: Boolean): Boolean =
//        profile.getString(res.getString(resId), null)?.toBoolean() ?: defVal
        profile.getBoolean(res.getString(resId), defVal)

    fun putInt(@StringRes resId: Int, value: Int) =
        profile.edit().putString(res.getString(resId), value.toString()).apply()

    fun getInt(@StringRes resId: Int): Int =
        getInt(resId, 0)

    fun getInt(@StringRes resId: Int, defVal: Int): Int =
        profile.getString(res.getString(resId), null)?.toInt() ?: defVal

    fun putFloat(@StringRes resId: Int, value: Float) =
        profile.edit().putString(res.getString(resId), value.toString()).apply()

    fun getFloat(@StringRes resId: Int): Float = getFloat(resId, 0f)

    fun getFloat(@StringRes resId: Int, defVal: Float): Float =
        profile.getString(res.getString(resId), null)?.toFloat() ?: defVal

    fun putLong(@StringRes resId: Int, value: Long) =
        profile.edit().putString(res.getString(resId), value.toString()).apply()

    fun getLong(@StringRes resId: Int): Long =
        getLong(resId, 0L)

    fun getLong(@StringRes resId: Int, defVal: Long): Long =
        profile.getString(res.getString(resId), null)?.toLong() ?: defVal
}
