package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Pixel Funds", appName)
  }

  @Test
  fun `MainActivity launches cleanly without Firebase configuration`() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      scenario.onActivity { activity ->
        assertFalse(activity.isFinishing)
        assertFalse(activity.isDestroyed)
      }
    }
  }
}
