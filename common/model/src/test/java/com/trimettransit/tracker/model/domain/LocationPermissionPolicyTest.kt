package com.trimettransit.tracker.model.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the location-permission recovery matrix. The PERMANENTLY_DENIED case is the one that
 * matters: it is what lets a screen offer App Settings instead of a prompt Android will ignore.
 */
class LocationPermissionPolicyTest {

    @Test
    fun `fine grant is granted`() {
        assertEquals(
            LocationPermissionState.GRANTED,
            locationPermissionState(
                fineGranted = true,
                coarseGranted = false,
                hasRequested = false,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `coarse-only approximate grant is granted`() {
        assertEquals(
            LocationPermissionState.GRANTED,
            locationPermissionState(
                fineGranted = false,
                coarseGranted = true,
                hasRequested = true,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `not yet asked is promptable`() {
        assertEquals(
            LocationPermissionState.PROMPTABLE,
            locationPermissionState(
                fineGranted = false,
                coarseGranted = false,
                hasRequested = false,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `denied but rationale still shows is promptable`() {
        assertEquals(
            LocationPermissionState.PROMPTABLE,
            locationPermissionState(
                fineGranted = false,
                coarseGranted = false,
                hasRequested = true,
                shouldShowRationale = true
            )
        )
    }

    @Test
    fun `denied with no rationale left is permanently denied`() {
        assertEquals(
            LocationPermissionState.PERMANENTLY_DENIED,
            locationPermissionState(
                fineGranted = false,
                coarseGranted = false,
                hasRequested = true,
                shouldShowRationale = false
            )
        )
    }
}
