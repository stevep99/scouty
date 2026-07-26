package io.github.stevep99.scouty.robot

import co.touchlab.kermit.Logger
import io.github.stevep99.scouty.core.Action

private val log = Logger.withTag("StubRobotService")

class StubRobotService : RobotService {

    override val isReady: Boolean = true

    override fun execute(action: Action) {
        log.d("Robot action: $action")
    }
}
