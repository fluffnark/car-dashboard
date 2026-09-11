package com.fluffnark.motoringdashboard.data

import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DisplayPreferencesTest {
    private val context get() = RuntimeEnvironment.getApplication() as Application

    @Before fun clear() = DisplayPreferences.preferences(context).edit().clear().commit().let { Unit }

    @Test fun nightIsTheDefault() {
        assertTrue(DisplayPreferences.isNight(context, hostNight = false))
    }

    @Test fun autoTracksHostAndDayOverridesIt() {
        DisplayPreferences.setTheme(context, DisplayTheme.AUTO)
        assertFalse(DisplayPreferences.isNight(context, hostNight = false))
        assertTrue(DisplayPreferences.isNight(context, hostNight = true))

        DisplayPreferences.setTheme(context, DisplayTheme.DAY)
        assertFalse(DisplayPreferences.isNight(context, hostNight = true))
    }
}
