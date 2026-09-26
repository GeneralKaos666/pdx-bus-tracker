package com.trimettransit.tracker.model.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorCopyTest {
    @Test
    fun `local failure without network error is LOCAL, never CONNECTION`() {
        assertEquals(ErrorCopyKind.LOCAL, errorCopyKind(isNetworkError = false, hasCachedData = false))
    }

    @Test
    fun `network error without cache is CONNECTION`() {
        assertEquals(ErrorCopyKind.CONNECTION, errorCopyKind(isNetworkError = true, hasCachedData = false))
    }

    @Test
    fun `stale cache served during network error is LOCAL`() {
        assertEquals(ErrorCopyKind.LOCAL, errorCopyKind(isNetworkError = true, hasCachedData = true))
    }
}
