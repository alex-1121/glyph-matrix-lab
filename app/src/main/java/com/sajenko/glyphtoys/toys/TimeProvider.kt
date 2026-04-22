package com.sajenko.glyphtoys.toys

import java.time.LocalTime

interface TimeProvider {
    fun now(): LocalTime

    fun minute(): Int = now().minute

    fun hour(): Int = now().hour
}

object SystemTimeProvider : TimeProvider {
    override fun now(): LocalTime = LocalTime.now()
}
