package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Gemini Live", appName)
  }

  @Test
  fun `api key storage in shared preferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("gemini_live_prefs", Context.MODE_PRIVATE)
    val testKey = "AIzaSyFakeTestKey123456789"
    prefs.edit().putString("custom_gemini_api_key", testKey).commit()

    val retrieved = prefs.getString("custom_gemini_api_key", "")
    assertEquals(testKey, retrieved)

    prefs.edit().remove("custom_gemini_api_key").commit()
    assertEquals("", prefs.getString("custom_gemini_api_key", ""))
  }
}
