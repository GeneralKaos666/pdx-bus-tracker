package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripRequestTime

/**
 * True when this arrive-by request asks for a time that has already gone, which the planner cannot
 * answer. A depart-now request is never in the past, and neither is an arrive-by request whose time
 * has not been chosen yet.
 *
 * A time of exactly [nowMillis] counts as gone: the request would be for this instant, which the
 * planner has already passed by the time it is sent.
 */
fun TripRequestTime.isInThePast(nowMillis: Long): Boolean =
    arriveBy && timeMillis != null && timeMillis <= nowMillis
