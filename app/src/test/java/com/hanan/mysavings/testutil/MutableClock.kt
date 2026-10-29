package com.hanan.mysavings.testutil

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** A clock tests can move forward, e.g. to cross midnight at the end of a month. */
class MutableClock(
    var instant: Instant,
    private val zone: ZoneId = ZoneId.of("Asia/Jakarta"),
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    override fun instant(): Instant = instant
}
