package io.github.stevep99.scouty.motors.sdk

/**
 * Per-build (product flavor) description of the physical robot movement SDK.
 *
 * Each product flavor provides its own implementation (via a flavor source set):
 * a robot flavor (e.g. `a133`) binds a concrete [SdkService]; a generic/tablet
 * flavor returns `supportsMovement = false` and never binds a service, so the
 * app runs without any robot control.
 */
interface RobotSdkConfig {
    /** True when this build has a physical robot to control. */
    val supportsMovement: Boolean

    /**
     * The Android [SdkService] class to bind for robot control, or `null` when
     * [supportsMovement] is false.
     */
    fun sdkServiceClass(): Class<out SdkService>?
}
