package ch.jacem.starclone.core.onboarding

import android.os.Bundle
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import ch.jacem.starclone.R
import com.github.appintro.AppIntroBaseFragment
import com.github.appintro.SlidePolicy
import com.github.appintro.model.SliderPage

class IdentifiableButtonAppIntroFragment : AppIntroBaseFragment(), SlidePolicy {

    private var slideLeaveCallback: SlideLeaveInterface? = null
    private var buttonCallback: SlideButtonCallback? = null

    override val isPolicyRespected: Boolean
        get() = slideLeaveCallback?.allowSlideLeave(slideId) ?: true

    override fun onUserIllegallyRequestedNextPage() {
        slideLeaveCallback?.onSlideLeavePrevented(slideId)
    }

    override val layoutId: Int get() = R.layout.appintro_fragment_intro_button

    var slideId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.action_button).setOnClickListener {
            buttonCallback?.onButtonClicked(slideId)
        }
    }

    companion object {
        @JvmStatic
        fun createInstance(
            title: CharSequence? = null,
            description: CharSequence? = null,
            @DrawableRes imageDrawable: Int = 0,
            @ColorRes backgroundColorRes: Int = 0,
            id: String,
            callback: SlideLeaveInterface,
            buttonCallback: SlideButtonCallback,
        ): IdentifiableButtonAppIntroFragment {
            val slide = IdentifiableButtonAppIntroFragment()
            slide.arguments = SliderPage(
                title = title,
                description = description,
                imageDrawable = imageDrawable,
                backgroundColorRes = backgroundColorRes,
            ).toBundle()
            slide.slideId = id
            slide.slideLeaveCallback = callback
            slide.buttonCallback = buttonCallback
            return slide
        }
    }
}
