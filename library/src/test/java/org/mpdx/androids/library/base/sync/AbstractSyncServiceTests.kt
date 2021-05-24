package org.mpdx.androids.library.base.sync

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.realm.RealmModel
import okhttp3.ResponseBody.Companion.toResponseBody
import org.ccci.gto.android.common.jsonapi.JsonApiConverter
import org.junit.Before
import org.mpdx.androids.library.base.model.UniqueItem
import org.mpdx.base.realm.BaseRealmTests
import org.mpdx.androids.library.base.sync.model.LastSyncTime
import retrofit2.Response

abstract class AbstractSyncServiceTests : BaseRealmTests() {
    protected val jsonApiConverter = JsonApiConverter.Builder().build()
    protected lateinit var syncDispatcher: SyncDispatcher

    @Before
    fun setupSyncManager() {
        syncDispatcher = mock()
    }

    protected fun whenGettingLastSyncTime() =
        whenever(realm.where(eq(LastSyncTime::class.java)).equalTo(any(), any<String>()).findFirst())

    protected inline fun <reified T : RealmModel> whenLookingUp(id: String) =
        whenever(realm.where(eq(T::class.java)).equalTo(UniqueItem.FIELD_ID, id).findFirst())

    protected fun <T> errorResponse(code: Int, json: String? = null): Response<T> =
        Response.error(code, json?.let { this::class.java.getResource(it)!!.readText() }.orEmpty().toResponseBody())
}
