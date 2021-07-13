package org.mpdx.android.features.dashboard

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.mpdx.android.features.dashboard.api.GoalApi
import retrofit2.Retrofit

@InstallIn(SingletonComponent::class)
@Module
object DashboardModule {
    @Provides
    @Singleton
    internal fun provideGoalApi(retrofit: Retrofit) = retrofit.create(GoalApi::class.java)
}
