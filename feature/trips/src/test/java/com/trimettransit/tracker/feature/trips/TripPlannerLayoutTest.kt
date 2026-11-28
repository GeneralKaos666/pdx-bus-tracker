package com.trimettransit.tracker.feature.trips

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlannerLayoutTest {

    @Test
    fun `a 720dp-tall phone starts collapsed`() {
        assertTrue(plannerStartsCollapsed(720))
    }

    @Test
    fun `a medium-height window just under expanded starts collapsed`() {
        assertTrue(plannerStartsCollapsed(899))
    }

    @Test
    fun `an expanded-height tablet starts expanded`() {
        assertFalse(plannerStartsCollapsed(900))
    }
}
