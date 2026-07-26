package com.cloudring.commonlib.cmd;

/**
 * Created by o on 2018/1/16.
 */

public class JniCmd {
    static {
        System.loadLibrary("jnicmd_a133");
    }

    private static JniCmd instance;
    private JniCmd() {
    }
    public static JniCmd getInstance() {
        if (instance == null) {
            synchronized (JniCmd.class) {
                if (instance == null) {
                    instance = new JniCmd();
                }
            }
        }
        return instance;
    }

    public native int dcmotorForward();
    public native int dcmotorBackward();
    public native int dcmotorStop();
    public native int dcmotorTurnLeft();
    public native int dcmotorTurnRight();
}
