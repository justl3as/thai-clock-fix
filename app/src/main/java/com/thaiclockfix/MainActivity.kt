package com.thaiclockfix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.thaiclockfix.ui.ClockFixMainScreen
import com.thaiclockfix.ui.theme.ThaiClockFixTheme
import com.thaiclockfix.viewmodel.ClockFixViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ClockFixViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThaiClockFixTheme {
                ClockFixMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically re-check Shizuku connection and SystemUI state when returning to the app
        viewModel.refreshShizukuStatus()
    }
}
