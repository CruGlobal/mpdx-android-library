package org.mpdx.android.features.donations

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.mpdx.android.features.donations.api.DonationsApi
import retrofit2.Retrofit

@InstallIn(SingletonComponent::class)
@Module
object DonationModule {
    @Provides
    @Singleton
    internal fun providesDonationsApi(retrofit: Retrofit) = retrofit.create(DonationsApi::class.java)
}
