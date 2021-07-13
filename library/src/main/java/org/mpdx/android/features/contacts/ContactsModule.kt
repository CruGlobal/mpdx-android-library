package org.mpdx.android.features.contacts

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.mpdx.android.features.contacts.api.ContactsApi
import org.mpdx.android.features.contacts.api.DonorAccountsApi
import org.mpdx.android.features.contacts.api.EmailAddressesApi
import org.mpdx.android.features.contacts.api.PhoneNumbersApi
import org.mpdx.android.features.contacts.api.SocialMediaApi
import retrofit2.Retrofit
import retrofit2.create

@InstallIn(SingletonComponent::class)
@Module
object ContactsModule {
    @Provides
    @Singleton
    fun provideContactsApi(retrofit: Retrofit) = retrofit.create<ContactsApi>()

    @Provides
    @Singleton
    fun provideDonorAccountsApi(retrofit: Retrofit) = retrofit.create<DonorAccountsApi>()

    @Provides
    @Singleton
    fun provideEmailAddressesApi(retrofit: Retrofit): EmailAddressesApi = retrofit.create()

    @Provides
    @Singleton
    fun providePhoneNumbersApi(retrofit: Retrofit): PhoneNumbersApi = retrofit.create()

    @Provides
    @Singleton
    fun provideSocialMediaApi(retrofit: Retrofit) = retrofit.create<SocialMediaApi>()
}
