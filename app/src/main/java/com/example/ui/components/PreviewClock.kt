package com.example.ui.components

import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal fun previewTimes(now: () -> LocalDateTime = { LocalDateTime.now() }): Flow<LocalDateTime> = flow {
    while (true) {
        emit(now())
        delay(1_000)
    }
}
