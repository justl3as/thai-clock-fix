package com.example.ui.components

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.model.LocaleMode
import com.example.ui.theme.ThaiClockFixTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DialogsUiTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun restoreConfirmDialogReportsConfirmAndCancel() {
        var confirmed = false
        var cancelled = false
        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                RestoreConfirmDialog(
                    onConfirm = { confirmed = true },
                    onDismiss = { cancelled = true },
                )
            }
        }
        compose.onNodeWithTag("confirm_restore_button").assertHasClickAction().performClick()
        assertTrue(confirmed)
        compose.onNodeWithTag("cancel_restore_button").performClick()
        assertTrue(cancelled)
    }

    @Test
    fun adbCommandsDialogListsCommandsAndSupportsCopyAndClose() {
        var closed = false
        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                AdbCommandsDialog(
                    mode = LocaleMode.THAI_BUDDHIST,
                    commands = listOf("adb shell fixture-apply"),
                    restoreCommands = listOf("adb shell fixture-restore"),
                    onDismiss = { closed = true },
                )
            }
        }
        compose.onNodeWithText("adb shell fixture-apply").assertExists()
        compose.onNodeWithText("adb shell fixture-restore").assertExists()
        compose.onNodeWithText("คัดลอกทั้งหมด").performClick()
        compose.onNodeWithText("ปิด").performClick()
        assertTrue(closed)
    }

    @Test
    fun faqDialogListsEveryQuestionAndAcknowledges() {
        var dismissed = false
        compose.setContent {
            ThaiClockFixTheme(dynamicColor = false) {
                FaqAndTipsDialog(onDismiss = { dismissed = true })
            }
        }
        listOf(
            "นาฬิกายังไม่เปลี่ยนทันทีหลังกดตั้งค่า?",
            "ทำไมนาฬิกาภาษาไทยบน Android ถึงมี 'น.'?",
            "ปลอดภัยหรือไม่ และมีผลต่อประกันเครื่องหรือไม่?",
            "หากรีสตาร์ตเครื่อง การตั้งค่าจะยังอยู่หรือไม่?",
            "วิธีคืนค่ากลับเป็นนาฬิกาแบบเดิม",
        ).forEach { question ->
            compose.onNodeWithText(question).assertExists()
        }
        compose.onNodeWithText("เข้าใจแล้ว").performClick()
        assertTrue(dismissed)
    }
}
