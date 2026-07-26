package io.github.stevep99.scouty.core

sealed interface Motion {
    data object Forward : Motion
    data object Backward : Motion
    data object Left : Motion
    data object Right : Motion
    data object Wiggle : Motion
    data object Dance : Motion
}

sealed interface Action {
    data class MovementAction(val motions: List<Motion>) : Action {
        constructor(motion: Motion) : this(listOf(motion))
    }
    data class NavigateTo(val screen: Screen) : Action
    data object Stop : Action
    data object FlashLed : Action
    data object GetTime : Action
    data object GetDate : Action
    data object Undo : Action
    data object Execute : Action
    data object Clear : Action
}
