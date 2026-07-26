package io.github.stevep99.scouty.motors.sdk

/**
 * A133 robot build: binds the A133 SDK service to drive the robot.
 */
class ScoutyA133RobotSdkConfig : RobotSdkConfig {
    override val supportsMovement: Boolean = true
    override fun sdkServiceClass(): Class<out SdkService>? = SdkA133Service::class.java
}
