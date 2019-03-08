package com.example.smssend.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsMessage
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import com.example.smssend.R
import com.example.smssend.content.AppPreferences
import com.example.smssend.ui.LoginActivity
import com.example.smssend.utils.messageStack
import com.example.smssend.utils.versionName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class CoreService : Service() {

    companion object {
        const val TAG = "CoreService"
        const val NOTIFICATION_ID = 10086
    }

    private var mReported = 0

    private val mNM by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val mNotification by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "core",
                "core-handle",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            mNM.createNotificationChannel(channel)
        }
        NotificationCompat.Builder(this, "core")
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    1,
                    Intent.makeRestartActivityTask(ComponentName(this, LoginActivity::class.java)),
                    0
                )
            )
            .setContentTitle("短信监听器v$versionName")

    }

    private val mSmsObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null) return

            val extras = intent.extras ?: return

            val smsObj = extras["pdus"] as Array<ByteArray>
            val createFromPdu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) (extras["format"] as String).let {
                fun(sms: ByteArray) = SmsMessage.createFromPdu(sms, it)
            }
            else fun(sms: ByteArray) = SmsMessage.createFromPdu(sms)

            var from = ""
            val text =
                smsObj.joinToString { createFromPdu(it).also { from = it.originatingAddress ?: "" }.displayMessageBody }

            Log.i(TAG, "onReceive: $from $text")

            val server = AppPreferences.getString(R.string.pref_base_server_key)
            val password = AppPreferences.getString(R.string.pref_password_key, "")
            val myPhone = AppPreferences.getString(R.string.pref_my_phone_key, "")
            if (TextUtils.isEmpty(server)) {
                updateMessage("请正确配置上报地址")
                return
            }

            GlobalScope.launch(Dispatchers.Main) {

                val code = withContext(Dispatchers.IO) { reportSms(server!!, from, text, myPhone, password) }

                var suffix = "最近一条结果"

                mNotification.setContentIntent(
                    PendingIntent.getActivity(
                        this@CoreService, 1,
                        Intent.makeRestartActivityTask(
                            ComponentName(this@CoreService, LoginActivity::class.java)
                        ).apply {
                            putExtra("reported", mReported)
                            if (code == -1) {
                                putExtra("exception", AppPreferences.getString(R.string.pref_exception_key))
                                suffix += "异常，点我显示异常"
                            } else {
                                suffix += code.toString()
                            }
                        },
                        0
                    )
                )
                updateMessage("已上报${mReported}条，$suffix")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        startForeground(NOTIFICATION_ID, mNotification.build())
        val ret = registerReceiver(mSmsObserver, IntentFilter().apply {
            priority = Integer.MAX_VALUE
            addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        })
        Toast.makeText(this, "监听服务已开启", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "onCreate: $ret")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        unregisterReceiver(mSmsObserver)
        Log.i(TAG, "onDestroy: ")
        super.onDestroy()
    }

    @WorkerThread
    private fun reportSms(server: String, from: String, message: String, to: String, pwd: String): Int {

        var f = from.replace(Regex("[ \\-]"), "")
        if (f.startsWith("+")) {
            if (f.startsWith("+86")) {
                f = f.substring(3)
            } else {
                f = f.substring(1)
            }
        }
        var code = -1
        var retry = 0

        do {
            try {
                val url = "$server/api/submitmsg/$pwd/$f/$to"
                val conn = URL(url)
                    .openConnection() as HttpURLConnection

                conn.readTimeout = 15 * 1000
                conn.connectTimeout = 15 * 1000
                conn.requestMethod = "POST"
                conn.doInput = true
                conn.doOutput = true
                conn.setRequestProperty("Accept-Charset", "UTF-8")
                conn.setRequestProperty("Content-Type", "application/json")

                val data = JSONObject().put("SmsMessage", message).toString().toByteArray()
                conn.setRequestProperty("Content-Length", "" + data.size);

                conn.outputStream.apply {
                    write(data)
                    close()
                }

                code = conn.responseCode
                Log.i(TAG, "result: $url >> $code")
                conn.disconnect()
                if (code in 200..299) {
                    mReported++
                    AppPreferences.clear(R.string.pref_exception_key)
                    break;
                }
            } catch (e: Exception) {
                AppPreferences.putString(R.string.pref_exception_key, e.messageStack)
            }
        } while (retry++ < 2)

        return code
    }

    private fun updateMessage(msg: String) {
        mNM.notify(NOTIFICATION_ID, mNotification.setTicker(msg).setContentText(msg).build())
    }

}
/*
Log.i(
TAG, JSONObject()
.put("originatingAddress", msg.originatingAddress)
.put("displayOriginatingAddress", msg.displayOriginatingAddress)
.put("messageBody", msg.messageBody)
.put("displayMessageBody", msg.displayMessageBody)
.put("serviceCenterAddress", msg.serviceCenterAddress)
.put("indexOnIcc", msg.indexOnIcc)
.put("isCphsMwiMessage", msg.isCphsMwiMessage)
.put("isMWIClearMessage", msg.isMWIClearMessage)
.put("isMWISetMessage", msg.isMWISetMessage)
.put("isMwiDontStore", msg.isMwiDontStore)
.put("isEmail", msg.isEmail)
.put("emailFrom", msg.emailFrom)
.put("emailBody", msg.emailBody)
.put("isReplace", msg.isReplace)
.toString(4)
)*/