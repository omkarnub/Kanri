package com.omkarnub.kanri.ui.onboarding

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for OnboardingHeroStateMachine:
 * Validates pure state machine behavior with an injected virtual clock.
 */
class OnboardingHeroDemoStateMachineTest {

    @Test
    fun testInitialStateIsIdle() {
        val machine = OnboardingHeroStateMachine()
        assertEquals(DemoStageState.Idle, machine.state)
        assertEquals(0, machine.currentScenarioIndex)
        assertEquals(0, machine.loopCount)
        assertFalse(machine.isPaused)
        assertFalse(machine.reduceMotion)
    }

    @Test
    fun testStateOrderWithVirtualClock() = runBlocking {
        val transitions = mutableListOf<Pair<DemoStageState, Long>>()
        var elapsed = 0L

        lateinit var machine: OnboardingHeroStateMachine
        machine = OnboardingHeroStateMachine(
            delayProvider = { ms ->
                transitions.add(machine.state to ms)
                elapsed += ms
                if (transitions.size >= 8) {
                    throw CancellationException("Completed 1 scenario loop")
                }
            }
        )

        try {
            machine.run()
        } catch (_: CancellationException) {}

        // State progression must match Section 3.3 specification
        assertEquals(DemoStageState.Idle, transitions[0].first)
        assertEquals(OnboardingHeroStateMachine.T_INTRO_DELAY, transitions[0].second)

        assertEquals(DemoStageState.SmsEntering, transitions[1].first)
        assertEquals(OnboardingHeroStateMachine.T_SMS_ENTER, transitions[1].second)

        assertEquals(DemoStageState.SmsHolding, transitions[2].first)
        assertEquals(OnboardingHeroStateMachine.T_SMS_HOLD, transitions[2].second)

        assertEquals(DemoStageState.Morphing, transitions[3].first)
        assertEquals(OnboardingHeroStateMachine.T_MORPH, transitions[3].second)

        assertEquals(DemoStageState.CardLanded, transitions[4].first)
        assertEquals(OnboardingHeroStateMachine.T_DIGITS_ROLL, transitions[4].second)

        assertEquals(DemoStageState.GaugeReacting, transitions[5].first)
        assertEquals(OnboardingHeroStateMachine.T_GAUGE_REACT, transitions[5].second)

        assertEquals(DemoStageState.Holding, transitions[6].first)
        assertEquals(OnboardingHeroStateMachine.T_HOLD, transitions[6].second)

        assertEquals(DemoStageState.Exiting, transitions[7].first)
        assertEquals(OnboardingHeroStateMachine.T_EXIT, transitions[7].second)

        // Total time for first scenario matches sum of phase durations
        val expectedTotal = OnboardingHeroStateMachine.T_INTRO_DELAY +
            OnboardingHeroStateMachine.T_SMS_ENTER +
            OnboardingHeroStateMachine.T_SMS_HOLD +
            OnboardingHeroStateMachine.T_MORPH +
            OnboardingHeroStateMachine.T_DIGITS_ROLL +
            OnboardingHeroStateMachine.T_GAUGE_REACT +
            OnboardingHeroStateMachine.T_HOLD +
            OnboardingHeroStateMachine.T_EXIT
        assertEquals(expectedTotal, elapsed)
    }

    @Test
    fun testLoopingThroughAllThreeScenarios() = runBlocking {
        val scenarioTransitions = mutableListOf<Int>()
        var stepCount = 0

        lateinit var machine: OnboardingHeroStateMachine
        machine = OnboardingHeroStateMachine(
            delayProvider = { _ ->
                stepCount++
                scenarioTransitions.add(machine.currentScenarioIndex)
                // Stop after looping through all 3 scenarios and returning to 0
                if (machine.loopCount >= 1 && machine.currentScenarioIndex == 0 && machine.state == DemoStageState.SmsEntering) {
                    throw CancellationException("Completed all scenarios and looped")
                }
            }
        )

        try {
            machine.run()
        } catch (_: CancellationException) {}

        // Verifies loopCount advanced and scenarios 0, 1, 2 were each active
        assertTrue(scenarioTransitions.contains(0))
        assertTrue(scenarioTransitions.contains(1))
        assertTrue(scenarioTransitions.contains(2))
        assertEquals(1, machine.loopCount)
    }

    @Test
    fun testHapticsFiredOnlyOnLoopZero() = runBlocking {
        var tickCount = 0
        var landCount = 0

        lateinit var machine: OnboardingHeroStateMachine
        machine = OnboardingHeroStateMachine(
            delayProvider = { _ ->
                if (machine.loopCount >= 2) {
                    throw CancellationException("Tested 2 full loops")
                }
            }
        )

        machine.onHapticTick = { tickCount++ }
        machine.onHapticLand = { landCount++ }

        try {
            machine.run()
        } catch (_: CancellationException) {}

        // Haptics must fire exactly once on loop 0, and never on subsequent loops
        assertEquals(1, tickCount)
        assertEquals(1, landCount)
    }

    @Test
    fun testReduceMotionShowsStaticHoldingFrame() = runBlocking {
        val machine = OnboardingHeroStateMachine().apply {
            reduceMotion = true
        }

        machine.run()

        // When reduce-motion is enabled, machine sets state to Holding immediately and does not loop
        assertEquals(DemoStageState.Holding, machine.state)
        assertEquals(0, machine.loopCount)
    }

    @Test
    fun testPauseAndResume() = runBlocking {
        var isPausedChecked = false
        var iterations = 0

        lateinit var machine: OnboardingHeroStateMachine
        machine = OnboardingHeroStateMachine(
            delayProvider = { _ ->
                iterations++
                if (iterations == 2) {
                    machine.pause()
                }
                if (machine.isPaused) {
                    isPausedChecked = true
                    machine.resume()
                }
                if (iterations >= 4) {
                    throw CancellationException("Pause resume verified")
                }
            }
        )

        try {
            machine.run()
        } catch (_: CancellationException) {}

        assertTrue("Pause condition was checked", isPausedChecked)
        assertFalse("Resumed cleanly", machine.isPaused)
    }

    @Test
    fun testCleanCancellationOnDispose() = runBlocking {
        val machine = OnboardingHeroStateMachine(
            delayProvider = { delay(100L) }
        )

        val job = launch {
            machine.run()
        }

        // Simulates component unmount / dispose
        job.cancelAndJoin()
        assertTrue("Job cancelled cleanly", job.isCancelled)
    }
}
