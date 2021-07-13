package org.mpdx.android.features.coaching

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import javax.inject.Singleton
import org.ccci.gto.android.common.dagger.eager.EagerSingleton
import org.mpdx.android.features.coaching.api.CoachingApi
import org.mpdx.android.features.coaching.notifications.CoachingNotificationManager
import retrofit2.Retrofit

@InstallIn(SingletonComponent::class)
@Module
object CoachingModule {
    @Provides
    @Singleton
    internal fun providesCoachingApi(retrofit: Retrofit) = retrofit.create(CoachingApi::class.java)

    @Provides
    @ElementsIntoSet
    @EagerSingleton(threadMode = EagerSingleton.ThreadMode.MAIN_ASYNC)
    internal fun asyncEagerSingletons(manager: CoachingNotificationManager) = setOf<Any>(manager)
}
