package com.sajenko.glyphtoys.toys

import java.time.LocalTime

class FakeTimeProvider(
    private var currentHour: Int = 12,
    private var currentMinute: Int = 0,
) : TimeProvider {
    override fun now(): LocalTime = LocalTime.of(currentHour, currentMinute)

    fun setTime(hour: Int, minute: Int) {
        currentHour = hour
        currentMinute = minute
    }
}
