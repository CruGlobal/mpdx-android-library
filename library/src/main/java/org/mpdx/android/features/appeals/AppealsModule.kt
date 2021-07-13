package org.mpdx.android.features.appeals

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.mpdx.android.features.appeals.api.AppealsApi
import org.mpdx.android.features.appeals.api.AskedContactsApi
import org.mpdx.android.features.appeals.api.PledgesApi
import retrofit2.Retrofit
import retrofit2.create

@InstallIn(SingletonComponent::class)
@Module
object AppealsModule {
    @Provides
    @Singleton
    fun provideAppealsApi(retrofit: Retrofit): AppealsApi = retrofit.create()

    @Provides
    @Singleton
    fun provideAskedContactsApi(retrofit: Retrofit): AskedContactsApi = retrofit.create()

    @Provides
    @Singleton
    fun providePledgesApi(retrofit: Retrofit): PledgesApi = retrofit.create()
}
