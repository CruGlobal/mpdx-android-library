package org.mpdx.android.features.sync

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import org.ccci.gto.android.common.dagger.eager.EagerSingleton

@InstallIn(SingletonComponent::class)
@Module
object SyncModule {
    @Provides
    @ElementsIntoSet
    @EagerSingleton(threadMode = EagerSingleton.ThreadMode.MAIN_ASYNC)
    internal fun asyncEagerSingletons(manager: DirtySyncManager) = setOf<Any>(manager)
}
