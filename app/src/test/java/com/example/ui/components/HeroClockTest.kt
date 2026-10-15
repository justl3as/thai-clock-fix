package com.example.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.model.PrimaryAction
import com.example.model.LocaleMode
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.ThaiClockFixTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeroClockTest {
    @get:Rule val compose = createComposeRule()

    @Test fun matchingSettingsStillShowAPreviewInsteadOfClaimingStatusBarPixels() {
        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                HeroClock(LocaleMode.THAI_BUDDHIST)
            }
        }
        compose.onNodeWithText("ตัวอย่างในแอป").assertExists()
        compose.onNodeWithText("Shizuku พร้อม").assertDoesNotExist()
        compose.onNodeWithText("Shizuku พร้อมใช้งาน").assertDoesNotExist()
        compose.onNodeWithTag("shizuku_status_card").assertDoesNotExist()
        compose.onNodeWithText("กำลังแสดงผลบนแถบสถานะ").assertDoesNotExist()
        compose.onNodeWithText("24 ชม. • ปฏิทิน พ.ศ.").assertDoesNotExist()
        compose.onNodeWithText("น.").assertDoesNotExist()
    }

    @Test fun unappliedThaiClockStillPreviewsSelectedFormatWithoutInventingSystemSuffix() {
        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                HeroClock(LocaleMode.THAI_BUDDHIST)
            }
        }
        compose.onNodeWithText("ตัวอย่างในแอป").assertExists()
        compose.onNodeWithText("น.").assertDoesNotExist()
    }

    @Test fun previewRendersUpdatedTimeAndDate() {
        val first = java.time.LocalDateTime.of(2026, 10, 15, 23, 59)
        val next = first.plusMinutes(1)
        val times = kotlinx.coroutines.flow.MutableStateFlow(first)
        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                HeroClock(LocaleMode.ENGLISH, timeFlow = times)
            }
        }
        compose.onNodeWithText("23:59").assertExists()
        compose.runOnIdle { times.value = next }
        compose.onNodeWithText("00:00").assertExists()
        compose.onNodeWithText(LocaleMode.ENGLISH.formatDatePreview(next)).assertExists()
    }

    @Test fun formatOptionsUseConsistentThaiLanguageAndEraLabels() {
        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                FormatToggleGroup(LocaleMode.ENGLISH, {})
            }
        }
        compose.onNodeWithText("ไทย • พ.ศ.").assertExists()
        compose.onNodeWithText("อังกฤษ • ค.ศ.").assertExists()
        compose.onNodeWithText("English · US").assertDoesNotExist()
    }

    @Test fun unappliedEnglishClockDoesNotShowNor() {
        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                HeroClock(LocaleMode.ENGLISH)
            }
        }
        compose.onNodeWithText("ตัวอย่างในแอป").assertExists()
        compose.onNodeWithText("น.").assertDoesNotExist()
    }

    private fun connectionAction(status: ShizukuStatus, locale: String? = null, checking: Boolean = false): PrimaryAction {
        val action = PrimaryAction.resolve(status, LocaleMode.ENGLISH, locale, false, false, true, checking)
        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                PrimaryActionBar(action, { clickedAction = it }, null)
            }
        }
        val buttonTag = if (action == PrimaryAction.AlreadyApplied) "primary_action_applied" else "primary_action_button"
        compose.onNodeWithTag(buttonTag).assertHeightIsAtLeast(48.dp)
        return action
    }

    private var clickedAction: PrimaryAction? = null

    @Test fun readyConnectionOffersApplyAndDoesNotClaimApplied() {
        connectionAction(ShizukuStatus.READY)
        compose.onNodeWithTag("primary_action_button").assertIsEnabled().assertHeightIsAtLeast(48.dp).performClick()
        org.junit.Assert.assertEquals(PrimaryAction.Apply, clickedAction)
    }

    @Test fun permissionConnectionOffersPermissionNotApplyEvenWithMatchingSettings() {
        connectionAction(ShizukuStatus.PERMISSION_REQUIRED, "en-US")
        compose.onNodeWithTag("primary_action_button").performClick()
        org.junit.Assert.assertEquals(PrimaryAction.GrantPermission, clickedAction)
    }

    @Test fun disconnectedConnectionOffersGuideEvenWithMatchingSettings() {
        connectionAction(ShizukuStatus.UNKNOWN, "en-US")
        compose.onNodeWithTag("primary_action_button").performClick()
        org.junit.Assert.assertEquals(PrimaryAction.StartShizuku, clickedAction)
    }

    @Test fun knownMissingShizukuOffersInstall() {
        connectionAction(ShizukuStatus.NOT_INSTALLED)
        compose.onNodeWithTag("primary_action_button").performClick()
        org.junit.Assert.assertEquals(PrimaryAction.InstallShizuku, clickedAction)
    }

    @Test fun readyMatchingSettingsRemainAlreadyApplied() {
        org.junit.Assert.assertEquals(PrimaryAction.AlreadyApplied, connectionAction(ShizukuStatus.READY, "en-US"))
        compose.onNodeWithTag("primary_action_applied").assertIsNotEnabled()
    }

    @Test fun actualReadbackDisablesActionWhileChecking() {
        org.junit.Assert.assertEquals(PrimaryAction.Checking, connectionAction(ShizukuStatus.READY, checking = true))
        compose.onNodeWithText("กำลังตรวจสอบ…").assertExists()
        compose.onNodeWithTag("primary_action_button").assertIsNotEnabled()
    }
}
