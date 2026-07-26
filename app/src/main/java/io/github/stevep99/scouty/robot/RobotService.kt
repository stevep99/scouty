package io.github.stevep99.scouty.robot

import io.github.stevep99.scouty.core.Action

interface RobotService {
    fun execute(action: Action)
    val isReady: Boolean
}
