package io.realm

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.realm.internal.RealmObjectProxy

class MockRealmObjectProxy(realm: Realm) : RealmObjectProxy {
    private val proxyState: ProxyState<*> = mock {
        on { `realm$realm` } doReturn realm
    }

    override fun `realmGet$proxyState`() = proxyState
    override fun `realm$injectObjectContext`() = TODO("not implemented")
}
