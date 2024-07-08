package com.foxmaps.maps.presentation

import app.cash.turbine.test
import com.foxmaps.maps.data.fakes.FakeMapsRepository
import com.foxmaps.maps.domain.LocationPermission
import com.foxmaps.utils.CoroutineTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class MapHostViewModelTest : CoroutineTest() {

    private val fakeMapsRepository = FakeMapsRepository()

    private fun getUnderTest(): MapHostViewModel {
        return MapHostViewModel(fakeMapsRepository)
    }

    @Test
    fun `emits loading while map is loading`() = runTest {
        val underTest = getUnderTest()
        underTest.screenStateStream.test {

            val actual = awaitItem()

            assertThat(actual).isInstanceOf(ScreenState.Loading::class.java)
        }
    }

    @Test
    fun `emits loading after loaded when map is loaded`() = runTest {
        val underTest = getUnderTest()
        underTest.screenStateStream.test {
            underTest.setLocationPermission(LocationPermission.Denied)
            underTest.setMapLoading(false)

            awaitItem()
            val actual = awaitItem()

            assertThat(actual).isInstanceOf(ScreenState.Loaded::class.java)
        }
    }
}
