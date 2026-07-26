package io.github.stevep99.scouty.motors.sdk

import android.content.Intent
import co.touchlab.kermit.Logger
import com.cloudring.commonlib.cmd.JniCmd
import kotlinx.coroutines.delay

private val log = Logger.withTag("SdkA133Service")

const val TURN_TIME = 940L
const val MOVE_TIME = 2200L
class SdkA133Service: SdkService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        log.v("onStartCommand")
        return START_STICKY
    }
    override fun onDestroy() {
        super.onDestroy()
        log.v("onDestroy")
    }
    override fun moveStop() {
        JniCmd.getInstance().dcmotorStop()
    }
    override suspend fun moveForwards() {
        JniCmd.getInstance().dcmotorForward()
        delay(MOVE_TIME)
        JniCmd.getInstance().dcmotorStop()
    }
    override suspend fun moveBackwards() {
        JniCmd.getInstance().dcmotorBackward()
        delay(MOVE_TIME)
        JniCmd.getInstance().dcmotorStop()
    }
    override suspend fun moveLeft() {
        JniCmd.getInstance().dcmotorTurnLeft()
        delay(TURN_TIME)
        JniCmd.getInstance().dcmotorStop()
    }
    override suspend fun moveRight() {
        JniCmd.getInstance().dcmotorTurnRight()
        delay(TURN_TIME)
        JniCmd.getInstance().dcmotorStop()
    }
    override suspend fun performWiggle() {
        repeat(5) {
            JniCmd.getInstance().dcmotorTurnLeft()
            delay(100L)
            JniCmd.getInstance().dcmotorTurnRight()
            delay(100L)
        }
        JniCmd.getInstance().dcmotorStop()
    }
    override suspend fun performDanceDemo() {
        delay(500L)
        JniCmd.getInstance().dcmotorForward()
        delay(600L)
        JniCmd.getInstance().dcmotorBackward()
        delay(600L)
        JniCmd.getInstance().dcmotorForward()
        delay(400L)
        JniCmd.getInstance().dcmotorTurnLeft()
        delay(300L)
        JniCmd.getInstance().dcmotorTurnRight()
        delay(600L)
        JniCmd.getInstance().dcmotorTurnLeft()
        delay(600L)
        JniCmd.getInstance().dcmotorTurnRight()
        delay(300L)
        JniCmd.getInstance().dcmotorBackward()
        delay(400L)
        JniCmd.getInstance().dcmotorTurnRight()
        delay(200L)
        JniCmd.getInstance().dcmotorTurnLeft()
        delay(400L)
        JniCmd.getInstance().dcmotorTurnRight()
        delay(200L)
        JniCmd.getInstance().dcmotorForward()
        delay(200L)
        JniCmd.getInstance().dcmotorStop()
        delay(100L)
        JniCmd.getInstance().dcmotorBackward()
        delay(300L)
        JniCmd.getInstance().dcmotorStop()
        delay(100L)
        JniCmd.getInstance().dcmotorForward()
        delay(300L)
        repeat(3) {
            JniCmd.getInstance().dcmotorTurnLeft()
            delay(100L)
            JniCmd.getInstance().dcmotorTurnRight()
            delay(100L)
        }
        JniCmd.getInstance().dcmotorBackward()
        delay(300L)
        JniCmd.getInstance().dcmotorStop()
    }

}
