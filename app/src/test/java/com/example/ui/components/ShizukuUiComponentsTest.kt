package com.example.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.ThaiClockFixTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShizukuUiComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun testShizukuBottomSheetNotInstalled() {
        var openInstallClicked = false
        var dismissed = false

        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                ShizukuBottomSheet(
                    status = ShizukuStatus.NOT_INSTALLED,
                    onOpenShizuku = { openInstallClicked = true },
                    onRequestPermission = {},
                    onRefreshStatus = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithTag("shizuku_bottom_sheet").assertExists()
        compose.onNodeWithText("การเชื่อมต่อ Shizuku").assertExists()

        // Checkpoints
        compose.onNodeWithText("ติดตั้งแอป").assertExists()
        compose.onNodeWithText("บริการ Shizuku").assertExists()
        compose.onNodeWithText("สิทธิ์อนุญาต").assertExists()

        // Click install
        compose
            .onNodeWithText("ติดตั้ง Shizuku จาก Google Play")
            .assertExists()
            .assertHasClickAction()
            .performClick()
        assertTrue(openInstallClicked)
        assertTrue(dismissed) // onOpenShizuku calls onDismiss as well
    }

    @Test
    fun testShizukuBottomSheetServiceStoppedWithGuide() {
        var openShizukuClicked = false
        var refreshClicked = false

        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                ShizukuBottomSheet(
                    status = ShizukuStatus.SERVICE_STOPPED,
                    onOpenShizuku = { openShizukuClicked = true },
                    onRequestPermission = {},
                    onRefreshStatus = { refreshClicked = true },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("เปิด Shizuku").assertExists().performClick()
        assertTrue(openShizukuClicked)

        compose.onNodeWithText("รีเฟรช").assertExists().performClick()
        assertTrue(refreshClicked)

        // Accordion Guide elements
        compose.onNodeWithText("วิธีเปิดใช้งานผ่าน Wi-Fi (ไม่ต้องใช้คอมพิวเตอร์)").assertExists()
        compose.onNodeWithText("เปิด Developer Options").assertExists()
        compose.onNodeWithText("เปิด Wireless Debugging").assertExists()
        compose.onNodeWithText("จับคู่อุปกรณ์ (Pairing)").assertExists()
        compose.onNodeWithText("กดเริ่ม (Start) ใน Shizuku").assertExists()
        compose.onNodeWithText("เปิด Developer Options ในเครื่อง").assertExists()
    }

    @Test
    fun testShizukuBottomSheetPermissionRequired() {
        var grantClicked = false
        var dismissed = false

        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                ShizukuBottomSheet(
                    status = ShizukuStatus.PERMISSION_REQUIRED,
                    onOpenShizuku = {},
                    onRequestPermission = { grantClicked = true },
                    onRefreshStatus = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText("อนุญาตสิทธิ์ทันที").assertExists().performClick()
        assertTrue(grantClicked)
        assertTrue(dismissed)
    }

    @Test
    fun testShizukuBottomSheetDenied() {
        var openClicked = false
        var retryPermissionClicked = false

        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                ShizukuBottomSheet(
                    status = ShizukuStatus.DENIED,
                    onOpenShizuku = { openClicked = true },
                    onRequestPermission = { retryPermissionClicked = true },
                    onRefreshStatus = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("เปิดแอป Shizuku > 'แอปพลิเคชันที่ได้รับอนุญาต' แล้วเปิดสวิตช์ให้ Thai Clock Fix").assertExists()
        compose.onNodeWithText("เปิด Shizuku").assertExists().performClick()
        assertTrue(openClicked)

        compose.onNodeWithText("ขอสิทธิ์ใหม่").assertExists().performClick()
        assertTrue(retryPermissionClicked)
    }

    @Test
    fun testShizukuBottomSheetReadyState() {
        var openClicked = false
        var refreshClicked = false

        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                ShizukuBottomSheet(
                    status = ShizukuStatus.READY,
                    onOpenShizuku = { openClicked = true },
                    onRequestPermission = {},
                    onRefreshStatus = { refreshClicked = true },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText("เปิดแอป Shizuku").assertExists().performClick()
        assertTrue(openClicked)

        compose.onNodeWithText("รีเฟรช").assertExists().performClick()
        assertTrue(refreshClicked)
    }

    @Test
    fun testShizukuBottomSheetDismissActions() {
        var dismissed = false

        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                ShizukuBottomSheet(
                    status = ShizukuStatus.READY,
                    onOpenShizuku = {},
                    onRequestPermission = {},
                    onRefreshStatus = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithContentDescription("ปิด").assertExists().performClick()
        assertTrue(dismissed)
    }
}
