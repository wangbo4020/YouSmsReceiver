package com.example.smssend

import android.annotation.SuppressLint
import android.app.Activity
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

open class PermissionRequestDelegate(val requestCode: Int, val activity: Activity? = null) : OnPermissionResultDelegate,
    OnActivityResultDelegate {

    companion object {
        const val TAG = "PermissionRequest"

        const val PERM_NOTIFICATION_ENABLE = "custom.NOTIFICATION_ENABLE"

        fun hasAccessUsage(context: Context): Boolean {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.packageManager.queryIntentActivities(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            } else {
                return true
            }
            return list.isNotEmpty()
        }

        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun isStatUsageEnabled(context: Context): Boolean {
            val ts = System.currentTimeMillis();
            val um = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            } else {
                context.getSystemService("usagestats") as UsageStatsManager
            }
            val queryUsageStats = um.queryUsageStats(UsageStatsManager.INTERVAL_BEST, 0, ts);
//        Log.i(TAG, "isStatUsageEnabled: $queryUsageStats")
            if (queryUsageStats == null || queryUsageStats.isEmpty()) {
                return false;
            }
            return true;
        }

        fun isNotificationEnabled(context: Context): Boolean =
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private val context = activity!!

    init {
        if (activity == null) {
            throw NullPointerException("fragment or activity must be one can't be null.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (this.requestCode == requestCode) {
            checkPermission()
        }
    }

    override fun onResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == this.requestCode) {
            checkPermission()
        }
    }

    private var permissions: Array<out String>? = null
    private var callback: PermissionRequestCallback? = null

    private fun checkPermission() {

        permissions!!.filter {
            when (it) {
                PERM_NOTIFICATION_ENABLE -> !isNotificationEnabled(context)
                "android.permission.PACKAGE_USAGE_STATS" -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !isStatUsageEnabled(
                    context
                )
                else -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(
                    context,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }
        }.also {
            Log.i(TAG, "UnGrant: $it ")
            if (it.isEmpty()) {
                callback?.complete()
                callback = null
            } else {
                callback?.alert(this, it[0], object : PermissionAlertHandler {
                    override fun ok() {
                        startRequestPermission(it[0])
                    }

                    override fun abort() {
                        callback = null
                    }

                    override fun ignore() {
                        val list = permissions!!.toMutableList()
                        list.remove(it[0])
                        permissions = list.toTypedArray()
                        checkPermission()
                    }
                })
            }
        }
    }

    fun checkPermission(cb: PermissionRequestCallback, vararg permissions: String) {

        if (permissions.isEmpty()) {
            cb.complete()
        } else {
            this.callback = cb
            this.permissions = permissions
            checkPermission()
        }
    }

    private fun requestPermission(vararg permissions: String) {
        ActivityCompat.requestPermissions(activity!!, permissions, this.requestCode)
    }

    private fun shouldShowRequestPermissionRationale(permission: String): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity!!, permission)

    private fun startRequestPermission(permission: String) {
        /*val should = shouldShowRequestPermissionRationale(permission)
        Log.i(TAG, "shouldShowRequestPermissionRationale($permission): $should")
        if (should) {
            // 跳转到应用详情
            startActivityForResult(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null)))
        } else */
        when (permission) {
            PERM_NOTIFICATION_ENABLE -> {

                var intent: Intent?
                val details = Intent()
                    .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
// 华为定制的通知权限界面需要特定权限
// java.lang.SecurityException: Permission Denial:
// starting Intent { dat=package:client.android.com.wejob cmp=com.huawei.systemmanager/com.huawei.notificationmanager.ui.NotificationSettingsActivity }
// from ProcessRecord{931b0d1 496:client.android.com.wejob/u0a299} (pid=496, uid=10299)
// requires com.huawei.systemmanager.permission.ACCESS_INTERFACE
                /*intent = Intent()
                        .setClassName("com.huawei.systemmanager", "com.huawei.notificationmanager.ui.NotificationSettingsActivity")
                        .setData(Uri.fromParts("package", context.packageName, null))
                if (intent.resolveActivity(context.packageManager) != null){
                } else */if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName);

                    // 有的手机没有该Activity，比如锤子
                    if (intent.resolveActivity(context.packageManager) == null) {
                        intent = details
                    }
                } else {
                    intent = details
                }
                startActivityForResult(intent!!)
            }
            "android.permission.PACKAGE_USAGE_STATS" -> {
                startActivityForResult(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            else -> requestPermission(permission)
        }
    }

    private fun startActivityForResult(intent: Intent) {
        try {
            activity!!.startActivityForResult(intent, this.requestCode)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    interface PermissionRequestCallback {

        fun alert(delegate: PermissionRequestDelegate, permission: String, handler: PermissionAlertHandler)

        fun complete()
    }

    interface PermissionAlertHandler {

        /**
         * 去开启权限
         */
        fun ok()

        /**
         * 中止检查权限
         */
        fun abort()

        /**
         * 忽略该权限
         */
        fun ignore()
    }
}