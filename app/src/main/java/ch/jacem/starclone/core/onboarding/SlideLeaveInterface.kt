package ch.jacem.starclone.core.onboarding

interface SlideLeaveInterface {

    fun allowSlideLeave(id: String): Boolean

    fun onSlideLeavePrevented(id: String)
}