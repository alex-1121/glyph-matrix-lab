package com.sajenko.glyphtoys.toys

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for service lifecycle only.
 *
 * These tests verify that [CompositeToyService] can start and bind without crashing and that the
 * binder returned from [GlyphToyBase.onBind] is non-null.
 *
 * They do not verify:
 * - asynchronous [com.nothing.ketchum.GlyphMatrixManager] connection
 * - rendered frame correctness on the matrix
 * - Nothing system picker registration
 * - mode transitions across call, clock, and equalizer states
 */
@RunWith(AndroidJUnit4::class)
class CompositeToyServiceInstrumentedTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun service_starts_without_crashing() {
        val intent = Intent(context, CompositeToyService::class.java)
        serviceRule.startService(intent)
    }

    @Test
    fun service_binds_and_returns_binder() {
        val intent = Intent(context, CompositeToyService::class.java)
        val binder = serviceRule.bindService(intent)
        assertNotNull("Service should return a non-null binder", binder)
    }

    @Test
    fun service_unbinds_without_crash() {
        val intent = Intent(context, CompositeToyService::class.java)
        serviceRule.bindService(intent)
        // Unbind is handled by ServiceTestRule cleanup. The test passes if teardown completes.
    }
}
