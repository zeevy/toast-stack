package com.siliconcircuits.toaststack

import org.junit.rules.ExternalResource

/**
 * Puts the ToastStack singleton back to its start state before and after
 * each test, so a test that fails half way does not leak a host or a
 * configure() value into the next one.
 */
class ToastStackReset : ExternalResource() {
    override fun before() = ToastStack.resetForTest()
    override fun after() = ToastStack.resetForTest()
}
