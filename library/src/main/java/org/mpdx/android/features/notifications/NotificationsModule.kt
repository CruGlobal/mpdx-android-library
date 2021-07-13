package org.mpdx.android.features.notifications

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.mpdx.android.features.notifications.api.NotificationsApi
import retrofit2.Retrofit

@InstallIn(SingletonComponent::class)
@Module
object NotificationsModule {
    @Provides
    @Singleton
    internal fun provideNotificationsApi(retrofit: Retrofit) = retrofit.create(NotificationsApi::class.java)
}
