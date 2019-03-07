package com.example.smssend

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.example.smssend.content.AppPreferences
import com.example.smssend.utils.messageStack
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.example.smssend", appContext.packageName)
        AppPreferences.putString(R.string.pref_exception_key, Exception().messageStack())
    }
}
