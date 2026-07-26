package io.github.stevep99.scouty.motors.sdk
interface SdkCommon {
    var sdkEventListener: SdkEventListener?
    fun moveStop()
    suspend fun moveForwards()
    suspend fun moveBackwards()
    suspend fun moveLeft()
    suspend fun moveRight()
    suspend fun performWiggle()
    suspend fun performDanceDemo()

}
