package com.foxmaps.maps.di

import android.content.Context
import com.foxmaps.maps.data.MapsRemoteSource
import com.foxmaps.maps.data.MapsRepositoryImpl
import com.foxmaps.maps.data.google.GoogleMapsRemoteSource
import com.foxmaps.maps.domain.MapsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapsModule {

    @Singleton
    @Provides
    fun providesMapsRepository(mapsRemoteSource: MapsRemoteSource): MapsRepository {
        return MapsRepositoryImpl(mapsRemoteSource)
    }

    @Singleton
    @Provides
    fun providesMapsRemoteSource(context: Context): MapsRemoteSource {
        return GoogleMapsRemoteSource(context)
    }
}
