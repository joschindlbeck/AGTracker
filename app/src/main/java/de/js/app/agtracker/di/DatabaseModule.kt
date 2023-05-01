package de.js.app.agtracker.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.js.app.agtracker.data.AppDatabase
import de.js.app.agtracker.data.TrackedPlaceDao
import de.js.app.agtracker.data.TrackedPlaceRepositoryImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    @Provides
    fun provideTrackedPlaceDao(appDatabase: AppDatabase): TrackedPlaceDao {
        return appDatabase.trackedPlaceDao()
    }
}
