package com.hchjeong.springiscool.cinematic.renderer

enum class TimingProfile {
    CINEMATIC,
    FAST,
    INSTANT,
    ;

    fun lineDelay(delayMillis: Long): Long {
        return when (this) {
            CINEMATIC -> delayMillis
            FAST -> delayMillis / 4
            INSTANT -> 0
        }
    }

    fun characterDelay(delayMillis: Long): Long {
        return when (this) {
            CINEMATIC -> delayMillis
            FAST -> delayMillis / 4
            INSTANT -> 0
        }
    }
}
