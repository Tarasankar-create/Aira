package com.example.aira.service

import com.example.aira.data.model.ListeningState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantStateTest {

    @Test
    fun testInitialStateIsOff() {
        AssistantStateHolder.resetToOff()
        val current = AssistantStateHolder.status.value
        assertEquals(ListeningState.OFF, current.state)
        assertFalse(current.isListening)
    }

    @Test
    fun testWakeWordListeningStateIsListening() {
        AssistantStateHolder.setState(ListeningState.LISTENING_FOR_WAKE_WORD)
        val current = AssistantStateHolder.status.value
        assertEquals(ListeningState.LISTENING_FOR_WAKE_WORD, current.state)
        assertTrue(current.isListening)
    }

    @Test
    fun testCommandListeningStateIsListening() {
        AssistantStateHolder.setState(ListeningState.LISTENING_FOR_COMMAND)
        val current = AssistantStateHolder.status.value
        assertEquals(ListeningState.LISTENING_FOR_COMMAND, current.state)
        assertTrue(current.isListening)
    }

    @Test
    fun testResetToOffWithError() {
        AssistantStateHolder.resetToOff("Sample error message")
        val current = AssistantStateHolder.status.value
        assertEquals(ListeningState.ERROR, current.state)
        assertEquals("Sample error message", current.errorMessage)
        assertFalse(current.isListening)
    }
}
