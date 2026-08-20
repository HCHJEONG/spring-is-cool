package com.hchjeong.springiscool.world

class WorldSession {
    var telephoneRinging: Boolean = true
        private set

    var lineAnswered: Boolean = false
        private set

    fun answerTelephone(): Boolean {
        if (lineAnswered) {
            return false
        }

        lineAnswered = true
        telephoneRinging = false
        return true
    }
}
