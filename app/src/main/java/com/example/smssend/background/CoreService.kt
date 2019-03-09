package com.example.smssend.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsMessage
import android.text.TextUtils
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import com.example.smssend.R
import com.example.smssend.content.AppPreferences
import com.example.smssend.ui.LoginActivity
import com.example.smssend.utils.messageStack
import com.example.smssend.utils.versionName
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat


class CoreService : Service() {

    companion object {
        const val TAG = "CoreService"
        const val NOTIFICATION_ID = 10086
    }

    private var mReported = 0
    private var mReporting = 0

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
            .setTicker("监听服务已启动")
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

    private val mSmsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null) return

            val extras = intent.extras ?: return
//            updateMessage("已收到短信，准备上报")

            val smsObj = extras["pdus"] as Array<ByteArray>
            val createFromPdu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) (extras["format"] as String).let {
                fun(sms: ByteArray) = SmsMessage.createFromPdu(sms, it)
            }
            else fun(sms: ByteArray) = SmsMessage.createFromPdu(sms)

            var from = ""
            val text =
                smsObj.joinToString { createFromPdu(it).also { from = it.originatingAddress ?: "" }.displayMessageBody }

            Log.i(TAG, "onReceive: $from $text")

//                dispatchSms(from, text)
        }
    }

    private val mSmsObserver = object : ContentObserver(Handler()) {

        private var mLastId = 0L
        private val sdf = SimpleDateFormat("yyyy年MM月dd日 hh时mm分ss秒");
        override fun onChange(selfChange: Boolean) {

            val map = querySmsNewest()

            if (map.isEmpty()) return
            val id = map["_id"]!!.toLong()
            if (id <= mLastId) return
            mLastId = id
            if (selfChange) return

            //格式化以秒为单位的日期
            Log.i(TAG, "onChanged: " + sdf.format(map["date"]!!.toLong()) + ", $map")
            dispatchSms(map["address"]!!, map["body"]!!)
        }
    }

    override fun onCreate() {
        super.onCreate()

        startForeground(NOTIFICATION_ID, mNotification.build())
        registerReceiver(mSmsReceiver, IntentFilter().apply {
            priority = Integer.MAX_VALUE
            addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            addAction("android.provider.OppoSpeechAssist.SMS_RECEIVED")
        })
        contentResolver.registerContentObserver(
            Uri.parse("content://sms"), true, mSmsObserver.apply {
                onChange(true)// 触发一次ID刷新
            }
        )

        Log.i(TAG, "onCreate: ")
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(mSmsObserver)
        unregisterReceiver(mSmsReceiver)
        Log.i(TAG, "onDestroy: ")
        super.onDestroy()
    }

    private fun dispatchSms(from: String, message: String) = GlobalScope.launch(Dispatchers.Main) {

        val server = AppPreferences.getString(R.string.pref_base_server_key)
        val password = AppPreferences.getString(R.string.pref_password_key, "")
        val myPhone = AppPreferences.getString(R.string.pref_my_phone_key, "")
        if (TextUtils.isEmpty(server)) {
            updateMessage("请正确配置上报地址")
            return@launch
        }

        mReporting++

        updateMessage("$mReporting 条正在上报中...")
        val code = withContext(Dispatchers.IO) {
            delay(mReporting * 3000L)
            reportSms(server!!, from, message, myPhone, password)
        }

        mReporting--

        if (mReporting != 0) {
            updateMessage("$mReporting 条正在上报中...")
            return@launch
        }
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

                conn.readTimeout = 5 * 1000
                conn.connectTimeout = 5 * 1000
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

    private fun querySmsNewest(): Map<String, String> {

        //查询发送向箱中的短信
        (contentResolver.query(
            Uri.parse(
                "content://sms/inbox"
            ), null, "type=? AND protocol=?", arrayOf("1", "0"), "date DESC, _id DESC LIMIT 1"
        ) ?: return emptyMap()).use { cursor ->
            Log.i(TAG, "querySmsNewest: " + cursor.count)
            //遍历查询结果获取用户正在发送的短信
            return if (cursor.moveToNext()) {
                mapOf(*cursor.columnNames.map {
                    it to cursor.getString(cursor.getColumnIndex(it))
                }.toTypedArray())
            } else emptyMap()
        }
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