package com.foxmaps.utils

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.extension.RegisterExtension

@Suppress("UnnecessaryAbstractClass")
internal abstract class CoroutineTest(testDispatcher: TestDispatcher = StandardTestDispatcher()) {

    @Suppress("Unused")
    @field:RegisterExtension
    val coroutinesExtension = CoroutinesExtension(testDispatcher)
}
