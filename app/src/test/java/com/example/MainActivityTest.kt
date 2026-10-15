package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Launch smoke test: the real MainActivity with its real default wiring
 * (DataStore + ShizukuManager) on a device without Shizuku installed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityTest {

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsHeroPreviewAndInstallActionWhenShizukuMissing() {
        compose.onNodeWithTag("hero_clock_time").assertExists()
        compose.onNodeWithText("ตัวอย่างในแอป").assertExists()
        compose.onNodeWithText("ติดตั้ง Shizuku").assertExists()
    }
}
