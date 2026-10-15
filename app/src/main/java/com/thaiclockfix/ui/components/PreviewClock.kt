package com.thaiclockfix.ui.components

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

internal fun previewTimes(now: () -> LocalDateTime = { LocalDateTime.now() }): Flow<LocalDateTime> = flow {
    while (true) {
        emit(now())
        delay(1_000)
    }
}
