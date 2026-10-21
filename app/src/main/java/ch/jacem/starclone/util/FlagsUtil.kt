package ch.jacem.starclone.util

import android.app.PendingIntent

class FlagsUtil {

    companion object {

        fun getFlagImmutable() : Int = PendingIntent.FLAG_IMMUTABLE

        fun getFlagImmutable(flag: Int) : Int = PendingIntent.FLAG_IMMUTABLE or flag
    }

}