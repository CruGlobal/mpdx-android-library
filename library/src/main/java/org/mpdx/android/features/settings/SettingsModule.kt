package org.mpdx.android.features.settings

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.mpdx.android.features.settings.api.SettingsApi
import retrofit2.Retrofit

@InstallIn(SingletonComponent::class)
@Module
object SettingsModule {
    @Provides
    @Singleton
    internal fun provideSettingsApi(retrofit: Retrofit) = retrofit.create(SettingsApi::class.java)
}
