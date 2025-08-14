package com.stellarforge.composebooking.utils // Veya uygun bir paket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit 4 Rule to set the Main dispatcher for tests.
 * This rule swaps the Main dispatcher with a TestDispatcher for the duration of the test.
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(), // Veya UnconfinedTestDispatcher()
) : TestWatcher() {
    val testScope = TestScope(testDispatcher) // Kendi TestScope'umuzu oluşturalım
    val scheduler = testDispatcher.scheduler

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}