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
import org.json.JSONArray
import org.json.JSONObject

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

    @Test fun localChangesAreJournaledForOfflineSync() {
        val repository = TripListRepository(context)
        repository.add("Check tire pressure")
        val item = repository.items().single()

        assertTrue(item.id.startsWith("local:"))
        assertEquals(item, repository.pendingChanges().upserts.single())

        repository.toggle(item.id)
        assertTrue(repository.pendingChanges().upserts.single().done)
        repository.remove(item.id)
        assertTrue(repository.pendingChanges().upserts.isEmpty())
        assertTrue(repository.pendingChanges().deletions.isEmpty())
    }

    @Test fun remoteDeletesAreJournaledAndSuccessfulSyncClearsJournal() {
        val repository = TripListRepository(context)
        val remote = TripItem("google:remote-1", "Fuel stop", false)
        repository.replaceSynced(listOf(remote))
        repository.toggle(remote.id)
        repository.remove(remote.id)

        assertTrue(repository.pendingChanges().upserts.isEmpty())
        assertEquals(setOf(remote.id), repository.pendingChanges().deletions)

        repository.replaceSynced(emptyList())
        assertEquals(TripChanges(emptyList(), emptySet()), repository.pendingChanges())
    }

    @Test fun createdRemoteIdReplacesLocalIdWithoutLosingPendingState() {
        val repository = TripListRepository(context)
        repository.add("Coffee")
        val local = repository.items().single()
        val remote = local.copy(id = "google:remote-2")

        repository.acknowledgeCreated(local.id, remote)

        assertEquals(remote, repository.items().single())
        assertEquals(remote, repository.pendingChanges().upserts.single())
    }

    @Test fun preJournalItemsAreMigratedIntoFirstSync() {
        context.getSharedPreferences("trip_list", 0).edit().putString("items", JSONArray()
            .put(JSONObject().put("id", "old-uuid").put("title", "Old item").put("done", false))
            .toString()).commit()

        val pending = TripListRepository(context).pendingChanges().upserts.single()

        assertEquals("old-uuid", pending.id)
        assertEquals("Old item", pending.title)
    }
}
