package com.example.smssend.ui.delegate

import android.content.Intent
import android.os.Bundle

interface JavaScriptDelegate {

    fun clear()
}

interface OnActivityResultDelegate {

    fun onResult(requestCode: Int, resultCode: Int, data: Intent?)
}

interface OnPermissionResultDelegate {

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
}

interface SaveInstanceStateDelegate {

    fun onRestoreState(savedInstanceState: Bundle?)

    fun onSaveInstanceState(outState: Bundle)
}