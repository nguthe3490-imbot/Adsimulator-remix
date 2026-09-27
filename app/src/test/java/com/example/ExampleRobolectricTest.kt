package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.AdsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    assertEquals("Ads simulator", appName)
  }

  @Test
  fun `test viewModel instantiation`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = AdsViewModel(app)
    assertNotNull(viewModel)
    assertNotNull(viewModel.metricsHistory.value)
  }

  @Test
  fun `test secret heist mini game flow`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = AdsViewModel(app)
    
    // We can simulate banner clicks to trigger jewelry ad.
    // Let's increment jewelry ad count directly or cycle banners to click them.
    // Find index of jewelry ads or call onBannerClicked multiple times
    var clicks = 0
    while (viewModel.jewelryAdCount.value < 2 && clicks < 20) {
      viewModel.onBannerClicked()
      clicks++
    }
    
    // Verify that showSecretDialog became true
    assertTrue(viewModel.jewelryAdCount.value >= 2)
    assertTrue(viewModel.showSecretDialog.value)
    
    // Start heist game
    viewModel.startHeistGame()
    assertEquals("PLAYING", viewModel.heistStatus.value)
    assertTrue(viewModel.isHeistGameVisible.value)
    
    // Perform heist actions
    viewModel.heistQuietLoot()
    viewModel.heistRobDiamond()
    viewModel.heistHackCamera()
    
    // Verify list of items in loot state is non-empty
    assertTrue(viewModel.heistLootBag.value.isNotEmpty())
    
    // Escape
    viewModel.heistEscape()
    assertEquals("ESCAPED", viewModel.heistStatus.value)
  }
}

