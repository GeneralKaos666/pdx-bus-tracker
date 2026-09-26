package com.trimettransit.tracker.widget

import com.trimettransit.tracker.model.ArrivalsResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetRefreshWorkerTest {

    @Test
    fun `failed fetch must not persist`() {
        assertFalse(shouldPersistSnapshot(null))
    }

    @Test
    fun `successful fetch persists, even when empty`() {
        assertTrue(shouldPersistSnapshot(ArrivalsResult()))
    }
}
