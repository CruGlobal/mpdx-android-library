package org.mpdx.android.core

import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import retrofit2.Retrofit
import retrofit2.create

@InstallIn(SingletonComponent::class)
@Module
class ApiModule {
    @Provides
    @Reusable
    fun provideAuthApi(@Named("baseRetrofit") retrofit: Retrofit): AuthApi = retrofit.create()

    @Provides
    @Reusable
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create()

    @Provides
    @Reusable
    fun provideAccountListApi(retrofit: Retrofit): AccountListApi = retrofit.create()

    @Provides
    @Reusable
    fun provideUserDeviceApi(retrofit: Retrofit): UserDeviceApi = retrofit.create()

    @Provides
    @Reusable
    fun provideDeletedRecordsApi(retrofit: Retrofit): DeletedRecordsApi = retrofit.create()

    @Provides
    @Reusable
    fun provideTagsApi(retrofit: Retrofit): TagsApi = retrofit.create()
}
