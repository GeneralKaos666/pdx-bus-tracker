package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripPlan
import com.trimettransit.tracker.model.TripPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanVisibilityTest {

    private val a = TripPoint(45.5, -122.6, "A")
    private val b = TripPoint(45.4, -122.7, "B")
    private val c = TripPoint(45.3, -122.8, "C")

    @Test
    fun `same endpoints keep the previous plan visible`() {
        assertTrue(keepPlanVisible(TripPlan(from = a, to = b), from = a, to = b))
    }

    @Test
    fun `reversed endpoints keep the previous plan visible for a swap`() {
        assertTrue(keepPlanVisible(TripPlan(from = a, to = b), from = b, to = a))
    }

    @Test
    fun `a different endpoint pairing clears the plan`() {
        assertFalse(keepPlanVisible(TripPlan(from = a, to = b), from = a, to = c))
    }

    @Test
    fun `no current plan clears`() {
        assertFalse(keepPlanVisible(null, from = a, to = b))
    }
}
