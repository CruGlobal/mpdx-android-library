package org.mpdx.base.realm

import android.app.Application
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.RealmResults
import org.junit.After
import org.junit.Before
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

@Config(application = Application::class)
abstract class BaseRealmTests {
    private lateinit var realmStaticMock: MockedStatic<Realm>
    protected lateinit var realmObjectStaticMock: MockedStatic<RealmObject>

    protected lateinit var realm: Realm

    @Before
    fun setupRealm() {
        realm = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS) {
            on { executeTransaction(any()) } doAnswer { (it.arguments[0] as? Realm.Transaction)?.execute(realm) }
            on { copyFromRealm(any<RealmModel>()) } doAnswer { it.arguments[0] as RealmModel }
        }

        realmStaticMock = Mockito.mockStatic(Realm::class.java)
        realmStaticMock.`when`<Realm> { Realm.getDefaultInstance() }.thenReturn(realm)

        realmObjectStaticMock = Mockito.mockStatic(RealmObject::class.java)
        realmObjectStaticMock.`when`<Realm> { RealmObject.getRealm(any()) }.thenReturn(realm)
        realmObjectStaticMock.`when`<Boolean> { RealmObject.isManaged(any()) }.thenCallRealMethod()
    }

    @After
    fun cleanupRealm() {
        realmObjectStaticMock.close()
        realmStaticMock.close()
    }

    protected fun <T : RealmModel> mockRealmResults(data: List<T>): RealmResults<T> = mock {
        on { realm } doReturn realm
        on { size } doAnswer { data.size }
        on<Iterator<T>?> { iterator() } doAnswer { data.iterator() }
        on { toArray() } doAnswer { data.toTypedArray<Any>() }
    }
}
