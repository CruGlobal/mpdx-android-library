package org.mpdx.android.features.tasks

import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.mpdx.android.features.tasks.api.TasksApi
import retrofit2.Retrofit
import retrofit2.create

@InstallIn(SingletonComponent::class)
@Module
object TaskModule {
    @Provides
    @Reusable
    fun provideTasksApi(retrofit: Retrofit): TasksApi = retrofit.create()
}
