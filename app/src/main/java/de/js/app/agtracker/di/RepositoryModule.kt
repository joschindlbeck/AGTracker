package de.js.app.agtracker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.components.SingletonComponent
import de.js.app.agtracker.data.TrackedPlaceRepositoryImpl
import de.js.app.agtracker.domain.repository.TrackedPlaceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract  class RepositoryModule {
    @Binds
    @Singleton
    abstract  fun bindTrackedPlaceRepository(trackedPlaceRepositoryImpl: TrackedPlaceRepositoryImpl): TrackedPlaceRepository
}