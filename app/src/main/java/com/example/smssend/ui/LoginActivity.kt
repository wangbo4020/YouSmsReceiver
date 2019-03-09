package com.example.smssend.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.smssend.R
import com.example.smssend.background.CoreService
import com.example.smssend.content.AppPreferences
import com.example.smssend.ui.delegate.PermissionRequestDelegate
import com.example.smssend.ui.delegate.PermissionRequestDelegate.Companion.PERM_NOTIFICATION_ENABLE
import com.example.smssend.utils.versionName
import kotlinx.android.synthetic.main.activity_login.*

/**
 */
class LoginActivity : AppCompatActivity() {

    // http://148.70.133.97
    companion object {
        const val TAG = "LoginActivity"
    }

    private val mRequest by lazy { PermissionRequestDelegate(101, this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etServer.setText(AppPreferences.getString(R.string.pref_base_server_key))
        etMyPhone.setText(AppPreferences.getString(R.string.pref_my_phone_key))
        etPassword.setText(AppPreferences.getString(R.string.pref_password_key))
        tvStack.setText(AppPreferences.getString(R.string.pref_exception_key))
        tvVersion.setText("v$versionName")

        etSave.setOnClickListener {
            AppPreferences.put(R.string.pref_base_server_key, etServer.text.toString())
            AppPreferences.put(R.string.pref_my_phone_key, etMyPhone.text.toString())
            AppPreferences.put(R.string.pref_password_key, etPassword.text.toString())
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
        }
        tvStack.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Label", tvStack.text))
            Toast.makeText(this, "信息已复制到剪切板", Toast.LENGTH_SHORT).show()
        }

        mRequest.checkPermission(object : PermissionRequestDelegate.PermissionRequestCallback {
            override fun alert(
                delegate: PermissionRequestDelegate,
                permission: String,
                handler: PermissionRequestDelegate.PermissionAlertHandler
            ) {
                val message = when (permission) {
                    Manifest.permission.RECEIVE_SMS -> "监听短信"
                    PERM_NOTIFICATION_ENABLE -> "显示通知"
                    else -> "读取短信"
                }
                AlertDialog.Builder(this@LoginActivity)
                    .setCancelable(false)
                    .setMessage("请授予${message}权限")
                    .setPositiveButton("好的") { _, _ ->
                        handler.ok()
                    }
                    .setNegativeButton("退出") { _, _ ->
                        handler.abort()
                        finish()
                    }.show()
                    .setCanceledOnTouchOutside(false)
            }

            override fun complete() {
                startService(Intent(applicationContext, CoreService::class.java))
            }
        }, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, PERM_NOTIFICATION_ENABLE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mRequest.onResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mRequest.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tvStack.setText(intent.getStringExtra("exception"))
    }
}
