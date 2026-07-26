package io.github.stevep99.scouty.motors.sdk

/**
 * Generic/tablet build: no physical robot, so no movement SDK service to bind.
 */
class ScoutyGenericRobotSdkConfig : RobotSdkConfig {
    override val supportsMovement: Boolean = false
    override fun sdkServiceClass(): Class<out SdkService>? = null
}
