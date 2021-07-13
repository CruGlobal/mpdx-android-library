package org.mpdx.android.core.services

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import org.ccci.gto.android.common.dagger.eager.EagerSingleton

@InstallIn(SingletonComponent::class)
@Module
abstract class ServicesModule {
    companion object {
        @Provides
        @ElementsIntoSet
        @EagerSingleton(threadMode = EagerSingleton.ThreadMode.MAIN_ASYNC)
        internal fun asyncEagerSingletons(connectivityService: ConnectivityService) = setOf<Any>(connectivityService)
    }
}
