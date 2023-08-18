package com.gitlab.mudlej.MjPdfReader.util;

import android.os.CountDownTimer;

public  class Timers {

    public static CountDownTimer timer(){
        return new CountDownTimer(150000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                Globals.TIMER_FINISHED = true;
            }
        };
    }

}