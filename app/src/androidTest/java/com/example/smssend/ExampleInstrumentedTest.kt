package com.example.smssend

import android.net.Uri
import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.example.smssend.background.CoreService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    val appContext = InstrumentationRegistry.getTargetContext()
    @Test
    fun useAppContext() {

        // Context of the app under test.
        assertEquals("com.example.smssend", appContext.packageName)
    }

    @Test
    fun testReadSms() {
        Log.i(CoreService.TAG, "testReadSms:")
        //查询发送向箱中的短信
        val cursor = appContext.contentResolver.query(
            Uri.parse(
                "content://sms/inbox"
            ), arrayOf("address", "subject", "body", "date"), null, null, "date DESC LIMIT 1"
        ) ?: return
        Log.i(CoreService.TAG, "testReadSms: ${cursor.count}")
        //遍历查询结果获取用户正在发送的短信
        while (cursor.moveToNext()) {
            val sb = StringBuffer()
            //获取短信的发送地址
            sb.append("发送地址：" + cursor.getString(cursor.getColumnIndex("address")));
            //获取短信的标题
            sb.append("\n标题：" + cursor.getString(cursor.getColumnIndex("subject")));
            //获取短信的内容
            sb.append("\n内容：" + cursor.getString(cursor.getColumnIndex("body")));
            //获取短信的发送时间
            val date = Date(cursor.getLong(cursor.getColumnIndex("date")));
            //格式化以秒为单位的日期
            val sdf = SimpleDateFormat("yyyy年MM月dd日 hh时mm分ss秒");
            sb.append("\n时间：" + sdf.format(date));
            Log.i(CoreService.TAG, sb.toString())
        }
        cursor.close()
    }
}
