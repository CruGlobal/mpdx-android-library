package org.mpdx.androids.library.base.sync

import android.os.Bundle
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmObject
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.ccci.gto.android.common.jsonapi.model.JsonApiError
import org.ccci.gto.android.common.jsonapi.model.JsonApiObject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.same
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.verification.VerificationMode
import org.mpdx.androids.library.base.model.ChangeAwareItem
import org.mpdx.androids.library.base.model.JsonApiModel
import org.mpdx.androids.library.base.model.UniqueItem
import retrofit2.Response

private const val ID1 = "obj1"
private const val ID2 = "obj2"
private const val ID3 = "obj3"
private const val ID4 = "obj4"
private const val ID5 = "obj5"

class BaseSyncServiceTest : AbstractSyncServiceTests() {
    private lateinit var syncService: TestSyncService

    private val obj1 = ChangeAwareTestObject(ID1)
    private val obj2 = ChangeAwareTestObject(ID2)
    private val obj3 = ChangeAwareTestObject(ID3)

    @Before
    fun setup() {
        syncService = TestSyncService(syncDispatcher, jsonApiConverter)
        whenLookingUp<ChangeAwareTestObject>(ID1).thenReturn(obj1)
        whenLookingUp<ChangeAwareTestObject>(ID2).thenReturn(obj2)
        whenLookingUp<ChangeAwareTestObject>(ID3).thenReturn(obj3)
    }

    @Test
    fun verifyAsExisting() {
        val existing = syncService.testAsExisting(listOf(obj1, obj2))
        assertTrue(existing.containsKey(ID1))
        assertTrue(existing.containsKey(ID2))
        assertFalse(existing.containsKey(ID3))
    }

    // region saveInRealm() Tests
    @Test
    fun verifySaveInRealmNoExistingObjects() {
        val items = listOf(obj1, obj2)
        whenLookingUp<ChangeAwareTestObject>(ID2).thenReturn(null)

        syncService.testSaveInRealm(realm, items)
        verify(realm).copyToRealmOrUpdate(obj1)
        verify(realm).copyToRealmOrUpdate(obj2)
        verify(realm, never()).copyToRealmOrUpdate(obj3)
    }

    @Test
    fun verifySaveInRealmSkipDeleted() {
        val deletedObj1 = ChangeAwareTestObject(ID1).apply { isDeleted = true }
        val items = listOf(obj1, obj2)
        whenLookingUp<ChangeAwareTestObject>(ID1).thenReturn(deletedObj1)

        syncService.testSaveInRealm(realm, items)
        verify(realm, never()).copyToRealmOrUpdate(obj1)
        verify(realm).copyToRealmOrUpdate(obj2)
        verify(realm, never()).copyToRealmOrUpdate(obj3)
    }

    @Test
    fun verifySaveInRealmOverwriteNew() {
        val newObj1 = ChangeAwareTestObject(ID1).apply { isNew = true }
        val items = listOf(obj1, obj2)
        whenLookingUp<ChangeAwareTestObject>(ID1).thenReturn(newObj1)

        syncService.testSaveInRealm(realm, items)
        verify(realm).copyToRealmOrUpdate(obj1)
        verify(realm).copyToRealmOrUpdate(obj2)
        verify(realm, never()).copyToRealmOrUpdate(obj3)
    }

    @Test
    fun verifySaveInRealmDeleteOrphaned() {
        val objNew = ChangeAwareTestObject("new").apply { isNew = true }
        val objDeleted = ChangeAwareTestObject("deleted").apply { isDeleted = true }
        val objNewAndDeleted = ChangeAwareTestObject("newAndDeleted").apply {
            isNew = true
            isDeleted = true
        }
        val existing = syncService.testAsExisting(listOf(obj2, obj3, objNew, objDeleted, objNewAndDeleted))
        val items = listOf(obj1, obj2)

        syncService.testSaveInRealm(realm, items, existing)
        verify(realm).copyToRealmOrUpdate(obj1)
        verify(realm).copyToRealmOrUpdate(obj2)
        verify(realm, never()).copyToRealmOrUpdate(obj3)
        verify(realm, never()).copyToRealmOrUpdate(objNew)
        verifyDeleteObj(obj1, never())
        verifyDeleteObj(obj2, never())
        verifyDeleteObj(obj3)
        verifyDeleteObj(objNew, never())
        verifyDeleteObj(objDeleted)
        verifyDeleteObj(objNewAndDeleted)
    }

    @Test
    fun verifySaveInRealmMergeChanges() {
        val obj1Dirty = ChangeAwareTestObject(ID1).apply {
            trackingChanges = true
            field1 = "dirty"
            trackingChanges = false
        }
        val obj1Fresh = ChangeAwareTestObject(ID1).apply {
            field1 = "clean"
            field2 = "clean"
        }
        whenLookingUp<ChangeAwareTestObject>(ID1).thenReturn(obj1Dirty)

        syncService.testSaveInRealm(realm, listOf(obj1Fresh))
        verify(realm).copyToRealmOrUpdate(obj1Fresh)
        assertEquals("dirty", obj1Fresh.field1)
        assertEquals("clean", obj1Fresh.field2)
        assertThat(obj1Fresh.changedFields.toList(), contains("field1"))
    }

    @Test
    fun verifySaveInRealmDontReplaceFullObjWithPlaceholder() {
        val obj1Full = ChangeAwareTestObject(ID1)
        val obj1Placeholder = ChangeAwareTestObject(ID1).apply {
            isPlaceholder = true
            replacePlaceholder = true
        }
        whenLookingUp<ChangeAwareTestObject>(ID1).thenReturn(obj1Full)

        syncService.testSaveInRealm(realm, listOf(obj1Placeholder))
        verify(realm, never()).copyToRealmOrUpdate(same(obj1Placeholder))
    }

    @Test
    fun verifySaveInRealmDontReplacePlaceholderObjWithPlaceholder() {
        val obj1Current = ChangeAwareTestObject(ID1).apply {
            isPlaceholder = true
        }
        val obj1Placeholder = ChangeAwareTestObject(ID1).apply {
            isPlaceholder = true
        }
        whenLookingUp<ChangeAwareTestObject>(ID1).thenReturn(obj1Current)

        syncService.testSaveInRealm(realm, listOf(obj1Placeholder))
        verify(realm, never()).copyToRealmOrUpdate(same(obj1Placeholder))
    }

    @Test
    fun verifySaveInRealmReplacePartialWithPartialWhenRequested() {
        val obj1Current = ChangeAwareTestObject(ID1).apply {
            isPlaceholder = true
        }
        val obj1Placeholder = ChangeAwareTestObject(ID1).apply {
            isPlaceholder = true
            replacePlaceholder = true
        }
        whenLookingUp<ChangeAwareTestObject>(ID1).thenReturn(obj1Current)

        syncService.testSaveInRealm(realm, listOf(obj1Placeholder))
        verify(realm).copyToRealmOrUpdate(same(obj1Placeholder))
    }
    // region saveInRealm() Tests

    // region onSuccess() tests
    @Test
    fun verifyOnSuccessNormal() {
        val handled = syncService.run {
            Response.success(JsonApiObject.single(obj1))
                .onSuccess { assertEquals(obj1, it.dataSingle) }
        }
        assertNull("response wasn't handled", handled)
    }

    @Test
    fun verifyOnSuccessNoContent() {
        val handled = syncService.run {
            Response.success<JsonApiObject<Any>>(null)
                .onSuccess { fail() }
                .onSuccess(requireBody = false) { assertNull(it) }
        }
        assertNull("response wasn't handled", handled)
    }

    @Test
    fun verifyOnSuccessJsonApiError() {
        val handled = syncService.run {
            Response.success<JsonApiObject<Any>>(JsonApiObject.error(JsonApiError()))
                .onSuccess(requireBody = false) { fail() }
        }
        assertNotNull("response wasn't handled", handled)
    }
    // endregion onSuccess() tests

    private fun verifyDeleteObj(obj: RealmModel, mode: VerificationMode = times(1)) =
        realmObjectStaticMock.verify(mode) { RealmObject.deleteFromRealm(obj) }
}

data class ChangeAwareTestObject(override var id: String?) : RealmObject(), UniqueItem,
    JsonApiModel,
    ChangeAwareItem {
    override val jsonApiType get() = "Test"
    override var isPlaceholder = false
    override var replacePlaceholder = false

    override var isNew = false
    override var isDeleted = false

    override var trackingChanges = false
    override var changedFieldsStr = ""

    override fun mergeChangedField(source: ChangeAwareItem, field: String) {
        if (source is ChangeAwareTestObject) {
            when (field) {
                "field1" -> field1 = source.field1
                "field2" -> field2 = source.field2
            }
        }
    }

    var field1: String? = null
        set(value) {
            if (trackingChanges) markChanged("field1")
            field = value
        }
    var field2: String? = null
        set(value) {
            if (trackingChanges) markChanged("field2")
            field = value
        }
}

internal class TestSyncService(syncDispatcher: SyncDispatcher, jsonApiConverter: JsonApiConverter) :
    BaseSyncService(syncDispatcher, jsonApiConverter) {
    fun <T : UniqueItem> testAsExisting(collection: Collection<T>) = collection.asExisting()

    fun testSaveInRealm(
        realm: Realm,
        items: Collection<RealmObject>,
        existingItems: MutableMap<String?, out RealmObject>? = null,
        deleteOrphanedExistingItems: Boolean = true
    ) = realm.saveInRealm(items, existingItems, deleteOrphanedExistingItems)

    override suspend fun sync(args: Bundle) = TODO("not implemented")
}
