package com.siliconcircuits.toaststack

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** The ViewModel helpers, called from a real ViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class ToastStackViewModelTest {

    @get:Rule
    val reset = ToastStackReset()

    private val dispatcher = StandardTestDispatcher()
    private val state = ToastStackState(maxVisible = 10)

    private class TestViewModel : ViewModel()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ToastStack.registerHost("viewmodel", state)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `showToast and the typed helpers reach the registered host`() {
        val viewModel = TestViewModel()

        viewModel.showToast("Plain")
        viewModel.showSuccessToast("Saved")
        viewModel.showErrorToast("Failed")
        viewModel.showWarningToast("Careful")
        viewModel.showInfoToast("Note")
        viewModel.showLoadingToast("Uploading")

        assertEquals(
            listOf(ToastType.Default, ToastType.Success, ToastType.Error, ToastType.Warning, ToastType.Info, ToastType.Loading),
            state.toasts.map { it.type },
        )
    }

    @Test
    fun `showToastAndAwait gives the dismiss reason to onResult`() = runTest(dispatcher) {
        val viewModel = TestViewModel()
        var result: DismissReason? = null

        viewModel.showToastAndAwait("Delete?", duration = ToastDuration.Indefinite) { result = it }
        runCurrent()
        state.dismiss(state.toasts.single().id, DismissReason.Action)
        runCurrent()

        assertEquals(DismissReason.Action, result)
    }
}
