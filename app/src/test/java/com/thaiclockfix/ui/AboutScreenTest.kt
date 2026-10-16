package com.thaiclockfix.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.thaiclockfix.ui.theme.ThaiClockFixTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutScreenTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun aboutScreenShowsIdentityPrivacyAndProjectLinks() {
        var openedUrl: String? = null

        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                AboutScreen(
                    versionName = "9.8.7",
                    versionCode = 9080007,
                    applicationId = "com.thaiclockfix",
                    diagnosticInfo = "fixture diagnostics",
                    onBack = {},
                    onOpenUrl = { openedUrl = it },
                    onCopyDiagnostic = {},
                )
            }
        }

        compose.onNodeWithText("Thai Clock Fix").assertExists()
        compose.onNodeWithText("เวอร์ชัน 9.8.7 (9080007)").assertExists()
        compose.onNodeWithText("ความเป็นส่วนตัว").assertExists()
        compose.onNodeWithText("แอปไม่มีสิทธิ์เข้าถึงอินเทอร์เน็ต").assertExists()
        compose.onNodeWithText("com.thaiclockfix").performScrollTo().assertExists()

        val links =
            listOf(
                "about_link_source" to "https://github.com/justl3as/thai-clock-fix",
                "about_link_releases" to "https://github.com/justl3as/thai-clock-fix/releases/latest",
                "about_link_issues" to "https://github.com/justl3as/thai-clock-fix/issues",
                "about_link_security" to "https://github.com/justl3as/thai-clock-fix/security/policy",
                "about_link_shizuku" to "https://github.com/RikkaApps/Shizuku",
            )

        links.forEach { (tag, expectedUrl) ->
            openedUrl = null
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            compose.waitForIdle()
            assertEquals(expectedUrl, openedUrl)
        }
    }

    @Test
    fun copyDiagnosticsUsesProvidedReportText() {
        var copiedText: String? = null

        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                AboutScreen(
                    versionName = "9.8.7",
                    versionCode = 9080007,
                    applicationId = "com.thaiclockfix",
                    diagnosticInfo = "Thai Clock Fix 9.8.7\nShizuku: SERVICE_STOPPED",
                    onBack = {},
                    onOpenUrl = {},
                    onCopyDiagnostic = { copiedText = it },
                )
            }
        }

        compose.onNodeWithTag("about_copy_diagnostics").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals("Thai Clock Fix 9.8.7\nShizuku: SERVICE_STOPPED", copiedText)
    }
}
