package io.github.stevep99.scouty.command

import io.github.stevep99.scouty.core.Action
import io.github.stevep99.scouty.core.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RobotCommandParserTest {

    private lateinit var parser: RobotCommandParser

    @Before
    fun setUp() {
        parser = RobotCommandParser()
    }

    @Test
    fun `plain text returns no actions`() {
        val result = parser.parse("Hello there! I'm Scouty your robot friend.")
        assertEquals("Hello there! I'm Scouty your robot friend.", result.speech)
        assertTrue(result.actions.isEmpty())
    }

    @Test
    fun `empty input returns no actions`() {
        val result = parser.parse("")
        assertEquals("", result.speech)
        assertTrue(result.actions.isEmpty())
    }

    // --- Single commands ---

    @Test
    fun `go left returns TurnLeft`() {
        val result = parser.parse("Go left now!")
        assertEquals(listOf(Action.MovementAction(Motion.Left)), result.actions)
    }

    @Test
    fun `go right returns TurnRight`() {
        val result = parser.parse("Go right!")
        assertEquals(listOf(Action.MovementAction(Motion.Right)), result.actions)
    }

    @Test
    fun `go forward returns MoveForward`() {
        val result = parser.parse("Moving forward!")
        assertEquals(listOf(Action.MovementAction(Motion.Forward)), result.actions)
    }

    @Test
    fun `go back returns MoveBackward`() {
        val result = parser.parse("Go back!")
        assertEquals(listOf(Action.MovementAction(Motion.Backward)), result.actions)
    }

    @Test
    fun `halt returns Stop`() {
        val result = parser.parse("Halt!")
        assertEquals(listOf(Action.Stop), result.actions)
    }

    @Test
    fun `wiggle returns Wiggle`() {
        val result = parser.parse("Time to wiggle!")
        assertEquals(listOf(Action.MovementAction(Motion.Wiggle)), result.actions)
    }

    @Test
    fun `dance returns Dance`() {
        val result = parser.parse("Time to dance!")
        assertEquals(listOf(Action.MovementAction(Motion.Dance)), result.actions)
    }

    @Test
    fun `get time returns GetTime`() {
        val result = parser.parse("What's time?")
        assertEquals(listOf(Action.GetTime), result.actions)
    }

    @Test
    fun `what is the time returns GetTime`() {
        val result = parser.parse("What is the time?")
        assertEquals(listOf(Action.GetTime), result.actions)
    }

    @Test
    fun `what's the time returns GetTime`() {
        val result = parser.parse("What's the time?")
        assertEquals(listOf(Action.GetTime), result.actions)
    }

    @Test
    fun `what time returns GetTime`() {
        val result = parser.parse("What time is it?")
        assertEquals(listOf(Action.GetTime), result.actions)
    }

    @Test
    fun `get date returns GetDate`() {
        val result = parser.parse("What day is it?")
        assertEquals(listOf(Action.GetDate), result.actions)
    }

    @Test
    fun `today returns GetDate`() {
        val result = parser.parse("It's today!")
        assertEquals(listOf(Action.GetDate), result.actions)
    }

    // --- Aliases ---

    @Test
    fun `turn left alias`() {
        assertEquals(listOf(Action.MovementAction(Motion.Left)), parser.parse("Turn left!").actions)
    }

    @Test
    fun `move right alias`() {
        assertEquals(listOf(Action.MovementAction(Motion.Right)), parser.parse("Move right!").actions)
    }

    @Test
    fun `lean left alias`() {
        assertEquals(listOf(Action.MovementAction(Motion.Left)), parser.parse("Lean left!").actions)
    }

    @Test
    fun `ahead alias`() {
        assertEquals(listOf(Action.MovementAction(Motion.Forward)), parser.parse("Go ahead!").actions)
    }

    @Test
    fun `backward alias`() {
        assertEquals(listOf(Action.MovementAction(Motion.Backward)), parser.parse("Go backward!").actions)
    }

    @Test
    fun `whoa alias`() {
        assertEquals(listOf(Action.Stop), parser.parse("Whoa!").actions)
    }

    @Test
    fun `freeze alias`() {
        assertEquals(listOf(Action.Stop), parser.parse("Freeze!").actions)
    }

    @Test
    fun `shake alias`() {
        assertEquals(listOf(Action.MovementAction(Motion.Wiggle)), parser.parse("Shake it!").actions)
    }

    // --- Chaining ---

    @Test
    fun `two commands preserve word order`() {
        val result = parser.parse("Go left, then go forward")
        assertEquals(listOf(Action.MovementAction(listOf(Motion.Left, Motion.Forward))), result.actions)
    }

    @Test
    fun `three commands preserve word order`() {
        val result = parser.parse("Go left, then forward, and stop")
        assertEquals(listOf(Action.MovementAction(listOf(Motion.Left, Motion.Forward)), Action.Stop), result.actions)
    }

    @Test
    fun `reverse order preserved`() {
        val result = parser.parse("Stop, then go right, then go back")
        assertEquals(listOf(Action.Stop, Action.MovementAction(listOf(Motion.Right, Motion.Backward))), result.actions)
    }

    @Test
    fun `dancing and wiggling together`() {
        val result = parser.parse("Wiggle and dance!")
        assertEquals(listOf(Action.MovementAction(listOf(Motion.Wiggle, Motion.Dance))), result.actions)
    }

    @Test
    fun `movements separated by non-movement action stay separate`() {
        val result = parser.parse("turn left, what time is it, turn right")
        assertEquals(
            listOf(Action.MovementAction(listOf(Motion.Left)), Action.GetTime, Action.MovementAction(listOf(Motion.Right))),
            result.actions
        )
    }

    @Test
    fun `repeated identical moves preserved in sequence`() {
        val result = parser.parse("Go left, go left, go left")
        assertEquals(listOf(Action.MovementAction(listOf(Motion.Left, Motion.Left, Motion.Left))), result.actions)
    }

    @Test
    fun `left then forward then left preserved`() {
        val result = parser.parse("go left, then go forward, then go left")
        assertEquals(listOf(Action.MovementAction(listOf(Motion.Left, Motion.Forward, Motion.Left))), result.actions)
    }

    // --- JSON movement (LLM) ---

    @Test
    fun `json movement parses to single sequence action`() {
        val result = parser.parse("""{"motions": ["left", "forward", "left"]}""")
        assertEquals(listOf(Action.MovementAction(listOf(Motion.Left, Motion.Forward, Motion.Left))), result.actions)
    }

    @Test
    fun `json movement names map to motions`() {
        val result = parser.parse("""{"motions": ["backward", "wiggle", "dance"]}""")
        assertEquals(listOf(Action.MovementAction(listOf(Motion.Backward, Motion.Wiggle, Motion.Dance))), result.actions)
    }

    @Test
    fun `json with prose around it still parses`() {
        val result = parser.parse("""Sure! {"motions": ["right", "right"]}""")
        assertEquals(listOf(Action.MovementAction(listOf(Motion.Right, Motion.Right))), result.actions)
    }

    // --- LLM output (free-form response) ---

    @Test
    fun `llm casual greeting with 'today' returns no action`() {
        val result = parser.parseLlmOutput("Hi there! How are you today?")
        assertEquals("Hi there! How are you today?", result.speech)
        assertTrue(result.actions.isEmpty())
    }

    @Test
    fun `llm chat mentioning direction returns no action`() {
        val result = parser.parseLlmOutput("Sure, I can help you with that.")
        assertEquals("Sure, I can help you with that.", result.speech)
        assertTrue(result.actions.isEmpty())
    }

    @Test
    fun `llm output with movement json returns movement action`() {
        val result = parser.parseLlmOutput("""{"motions": ["left", "forward"]}""")
        assertEquals(listOf(Action.MovementAction(listOf(Motion.Left, Motion.Forward))), result.actions)
    }

    @Test
    fun `llm output with prose around movement json still parses`() {
        val result = parser.parseLlmOutput("""Sure! {"motions": ["dance"]}""")
        assertEquals(listOf(Action.MovementAction(Motion.Dance)), result.actions)
    }

    // --- Speech preserved ---    @Test
    fun `speech is original text preserved`() {
        val text = "Going left now!"
        val result = parser.parse(text)
        assertEquals(text, result.speech)
    }

    @Test
    fun `speech trims whitespace`() {
        val result = parser.parse("  go left  ")
        assertEquals("go left", result.speech)
    }

    // --- Case insensitive ---

    @Test
    fun `case insensitive matching`() {
        assertEquals(listOf(Action.MovementAction(Motion.Left)), parser.parse("GO LEFT").actions)
        assertEquals(listOf(Action.MovementAction(Motion.Dance)), parser.parse("DANCE").actions)
        assertEquals(listOf(Action.Stop), parser.parse("STOP").actions)
    }

    // --- No false positives ---

    @Test
    fun `left in middle of word does not trigger`() {
        val result = parser.parse("That's左侧")
        assertTrue(result.actions.isEmpty())
    }

    @Test
    fun `right in middle of word does not trigger`() {
        val result = parser.parse("correctly done")
        assertTrue(result.actions.isEmpty())
    }

    // --- Navigation ---

    @Test
    fun `go to movement screen`() {
        assertEquals(listOf(Action.NavigateTo(io.github.stevep99.scouty.core.Screen.Movement)),
            parser.parse("go to the movement screen").actions)
    }

    @Test
    fun `open drive screen`() {
        assertEquals(listOf(Action.NavigateTo(io.github.stevep99.scouty.core.Screen.Movement)),
            parser.parse("open the drive screen").actions)
    }

    @Test
    fun `go home`() {
        assertEquals(listOf(Action.NavigateTo(io.github.stevep99.scouty.core.Screen.Face)),
            parser.parse("go home").actions)
    }

    @Test
    fun `show your face`() {
        assertEquals(listOf(Action.NavigateTo(io.github.stevep99.scouty.core.Screen.Face)),
            parser.parse("show your face").actions)
    }
}
