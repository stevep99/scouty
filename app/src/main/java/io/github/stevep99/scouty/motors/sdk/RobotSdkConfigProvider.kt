package io.github.stevep99.scouty.motors.sdk

import io.github.stevep99.scouty.BuildConfig
import co.touchlab.kermit.Logger

/**
 * Resolves the flavor-specific [RobotSdkConfig] at runtime.
 *
 * Each product flavor sets [BuildConfig.ROBOT_SDK] to the fully-qualified name of its
 * own `Scouty...RobotSdkConfig` implementation (e.g. `ScoutyGenericRobotSdkConfig` on the
 * `generic` flavor, `ScoutyA133RobotSdkConfig` on the `a133` flavor), so main source never
 * has to reference a flavor class directly.
 */
object RobotSdkConfigProvider {
    private val log = Logger.withTag("RobotSdkConfigProvider")
    private val config: RobotSdkConfig by lazy {
        val className = BuildConfig.ROBOT_SDK
        try {
            val cls = Class.forName(className)
            val instance = cls.getDeclaredConstructor().newInstance()
            if (instance is RobotSdkConfig) {
                instance
            } else {
                log.e("BuildConfig.ROBOT_SDK '$className' is not a RobotSdkConfig")
                defaultConfig()
            }
        } catch (e: Exception) {
            log.e("Failed to instantiate RobotSdkConfig '$className': ${e.message}")
            defaultConfig()
        }
    }
    val instance: RobotSdkConfig
        get() = config
    private fun defaultConfig(): RobotSdkConfig = object : RobotSdkConfig {
        override val supportsMovement: Boolean = false
        override fun sdkServiceClass(): Class<out SdkService>? = null
    }
}
