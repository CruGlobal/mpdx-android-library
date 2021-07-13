package org.mpdx.android.features.constants

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.mpdx.android.features.constants.api.ConstantsApi
import retrofit2.Retrofit
import retrofit2.create

@InstallIn(SingletonComponent::class)
@Module
class ConstantsModule {
    @Provides
    @Singleton
    fun provideConstantsApi(retrofit: Retrofit) = retrofit.create<ConstantsApi>()
}
