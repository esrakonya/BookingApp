package com.stellarforge.composebooking.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit 4 Rule that swaps the Main dispatcher with a TestDispatcher.
 *
 * This is essential for Unit Testing ViewModels or any code that uses `viewModelScope`
 * or `Dispatchers.Main`, as the Android Main Looper is not available in local unit tests.
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    val testScope = TestScope(testDispatcher)
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