package com.example

import com.example.emergency.domain.EmergencyState
import com.example.emergency.domain.EmergencyStateMachine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyStateMachineTest {

    @Test
    fun testValidTransitions() {
        assertTrue(EmergencyStateMachine.canTransition(EmergencyState.IDLE, EmergencyState.ARMED))
        assertTrue(EmergencyStateMachine.canTransition(EmergencyState.IDLE, EmergencyState.TRIGGERED))
        assertTrue(EmergencyStateMachine.canTransition(EmergencyState.ARMED, EmergencyState.TRIGGERED))
        assertTrue(EmergencyStateMachine.canTransition(EmergencyState.TRIGGERED, EmergencyState.ACTIVE))
        assertTrue(EmergencyStateMachine.canTransition(EmergencyState.ACTIVE, EmergencyState.ESCALATING))
        assertTrue(EmergencyStateMachine.canTransition(EmergencyState.ACTIVE, EmergencyState.RESOLVED))
        assertTrue(EmergencyStateMachine.canTransition(EmergencyState.ACTIVE, EmergencyState.CANCELLED))
    }

    @Test
    fun testInvalidTransitions() {
        // IDLE cannot jump directly to RESOLVED without active emergency
        assertFalse(EmergencyStateMachine.canTransition(EmergencyState.IDLE, EmergencyState.RESOLVED))
        // Terminal states cannot transition to anything
        assertFalse(EmergencyStateMachine.canTransition(EmergencyState.RESOLVED, EmergencyState.ACTIVE))
        assertFalse(EmergencyStateMachine.canTransition(EmergencyState.CANCELLED, EmergencyState.ARMED))
    }

    @Test(expected = IllegalStateException::class)
    fun testValidateTransitionThrowsOnInvalid() {
        EmergencyStateMachine.validateTransition(EmergencyState.RESOLVED, EmergencyState.ACTIVE)
    }
}
