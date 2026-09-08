package com.fluffnark.motoringdashboard.trip

import android.app.Application
import org.junit.Assert.assertEquals
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
class TripListRepositoryTest {
    private val context get() = RuntimeEnvironment.getApplication() as Application

    @Before fun clear() {
        context.getSharedPreferences("trip_list", 0).edit().clear().commit()
    }

    @Test fun addToggleAndRemovePersist() {
        val repository = TripListRepository(context)
        repository.add("  Pack   water  ")

        val item = repository.items().single()
        assertEquals("Pack water", item.title)
        assertFalse(item.done)

        repository.toggle(item.id)
        assertTrue(TripListRepository(context).items().single().done)

        repository.remove(item.id)
        assertTrue(repository.items().isEmpty())
    }
}
