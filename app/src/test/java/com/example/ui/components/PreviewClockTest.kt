package com.example.ui.components

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.flowWithLifecycle
import java.time.LocalDateTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewClockTest {
    @Test fun previewUpdatesAcrossMinuteAndDateChange() = runTest {
        var now = LocalDateTime.of(2026, 10, 15, 23, 59, 59)
        val seen = mutableListOf<LocalDateTime>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { previewTimes { now }.collect { seen += it } }
        assertEquals("2026-10-15T23:59:59", seen.last().toString())
        now = LocalDateTime.of(2026, 10, 16, 0, 0, 0)
        advanceTimeBy(1_000); runCurrent()
        assertEquals("2026-10-16T00:00", seen.last().toString())
    }

    @Test fun stoppedPreviewDoesNotTickAndResumingRereadsWallClockImmediately() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val owner = object : LifecycleOwner {
                val registry = LifecycleRegistry.createUnsafe(this)
                override val lifecycle: Lifecycle = registry
            }
            var now = LocalDateTime.of(2026, 10, 15, 10, 0)
            val seen = mutableListOf<LocalDateTime>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                previewTimes { now }.flowWithLifecycle(owner.lifecycle, Lifecycle.State.STARTED).collect { seen += it }
            }
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            runCurrent()
            assertTrue(seen.isEmpty())
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            runCurrent()
            assertEquals(now, seen.last())
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            runCurrent()
            val count = seen.size
            now = LocalDateTime.of(2026, 10, 16, 11, 0)
            advanceTimeBy(5_000); runCurrent()
            assertEquals(count, seen.size)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            runCurrent()
            assertEquals(now, seen.last())
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        } finally { Dispatchers.resetMain() }
    }
}
