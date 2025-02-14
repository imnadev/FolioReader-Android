package com.folioreader.ui.view

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.folioreader.Config
import com.folioreader.Constants
import com.folioreader.R
import com.folioreader.model.event.ReloadDataEvent
import com.folioreader.ui.activity.FolioActivity
import com.folioreader.ui.activity.FolioActivityCallback
import com.folioreader.util.AppUtil
import com.folioreader.util.UiUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.view_bottom_sheet.*
import org.greenrobot.eventbus.EventBus
import java.lang.Integer.max
import java.lang.Integer.min


/**
 * Created by Yolo on 30/09/2016.
 */
class ConfigBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val FADE_DAY_NIGHT_MODE = 500

        @JvmField
        val LOG_TAG: String = ConfigBottomSheetDialogFragment::class.java.simpleName
    }

    private lateinit var config: Config
    private var isNightMode = false
    private var isAlt = false
    private lateinit var activityCallback: FolioActivityCallback
    private var colorAnimation: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.view_bottom_sheet, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme).apply {
            setOnShowListener {
                this.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?.setBackgroundResource(android.R.color.transparent)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (activity is FolioActivity)
            activityCallback = activity as FolioActivity

        config = AppUtil.getSavedConfig(activity)!!
        initViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        view?.viewTreeObserver?.addOnGlobalLayoutListener(null)
    }

    private fun initViews() {
        configBrightness()
        inflateView()
        configFonts()
        configFontSize()
        selectFont(config.font, false)

        isNightMode = config.isNightMode
        isAlt = config.isAlt

        if (isNightMode) {
            body.background.setTint(ContextCompat.getColor(requireContext(), R.color.night))
        } else {
            body.background.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        }

        activityCallback.loadingView?.callback = { isLoading ->
            controlsEnabled(isLoading.not())
        }

        setBackgroundColor(isNightMode, isAlt)
    }

    private fun configFontSize() {
        card_text_size_minus.setOnClickListener {
            val size = max(0, config.fontSize - 1)
            if (config.fontSize == size) return@setOnClickListener
            config.fontSize = size
            AppUtil.saveConfig(activity, config)
            EventBus.getDefault().post(ReloadDataEvent())
        }
        card_text_size_add.setOnClickListener {
            val size = min(14, config.fontSize + 1)
            if (config.fontSize == size) return@setOnClickListener
            config.fontSize = size
            AppUtil.saveConfig(activity, config)
            EventBus.getDefault().post(ReloadDataEvent())
        }
    }

    private fun configBrightness() {
        size_seek_bar.max = 100
        size_seek_bar.keyProgressIncrement = 1
        size_seek_bar.progress = config.brightness
        size_seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                config = AppUtil.getSavedConfig(context)!!
                config.brightness = seekBar.progress
                AppUtil.saveConfig(context, config)
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                activityCallback.setBrightness(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun inflateView() {

        if (config.allowedDirection != Config.AllowedDirection.VERTICAL_AND_HORIZONTAL) {
            text_orientation.visibility = View.GONE
            card_orientation_vertical.visibility = View.GONE
            card_orientation_horizontal.visibility = View.GONE
        }

        card_normal.setOnClickListener {
            if (isNightMode.not() && isAlt.not()) return@setOnClickListener
            val animate = isNightMode
            isNightMode = false
            isAlt = false
            toggleTheme(animate)
            setToolBarColor()
        }

        card_white.setOnClickListener {
            if (isNightMode.not() && isAlt) return@setOnClickListener
            val animate = isNightMode
            isNightMode = false
            isAlt = true
            toggleTheme(animate)
            setToolBarColor()
        }

        card_gray.setOnClickListener {
            if (isNightMode && isAlt) return@setOnClickListener
            val animate = isNightMode.not()
            isNightMode = true
            isAlt = true
            toggleTheme(animate)
            setToolBarColor()
        }

        card_dark.setOnClickListener {
            if (isNightMode && isAlt.not()) return@setOnClickListener
            val animate = isNightMode.not()
            isNightMode = true
            isAlt = false
            toggleTheme(animate)
            setToolBarColor()
        }

        fun setDirection(direction: Config.Direction) {
            listOf(
                Config.Direction.HORIZONTAL to card_orientation_horizontal,
                Config.Direction.VERTICAL to card_orientation_vertical
            ).forEach {
                val color = if (it.first == direction) {
                    R.color.view_bottom_sheet_item_selected
                } else {
                    R.color.view_bottom_sheet_item
                }
                it.second.strokeColor = ResourcesCompat.getColor(resources, color, null)
            }
        }

        setDirection(activityCallback.direction)

        if (activityCallback.direction == Config.Direction.HORIZONTAL) {
            card_orientation_horizontal.isSelected = true
        } else if (activityCallback.direction == Config.Direction.VERTICAL) {
            card_orientation_vertical.isSelected = true
        }
        card_orientation_vertical.setOnClickListener {
            config = AppUtil.getSavedConfig(context)!!
            config.direction = Config.Direction.VERTICAL
            AppUtil.saveConfig(context, config)
            activityCallback.onDirectionChange(Config.Direction.VERTICAL)
            card_orientation_horizontal.isSelected = false
            card_orientation_vertical.isSelected = true
            setDirection(Config.Direction.VERTICAL)
        }

        card_orientation_horizontal.setOnClickListener {
            config = AppUtil.getSavedConfig(context)!!
            config.direction = Config.Direction.HORIZONTAL
            AppUtil.saveConfig(context, config)
            activityCallback.onDirectionChange(Config.Direction.HORIZONTAL)
            card_orientation_horizontal.isSelected = true
            card_orientation_vertical.isSelected = false
            setDirection(Config.Direction.HORIZONTAL)
        }
    }

    private fun configFonts() {
        val colorStateList = UiUtil.getColorList(
            config.themeColor,
            ContextCompat.getColor(requireContext(), R.color.grey_color)
        )
        font_andada.setTextColor(colorStateList)
        font_lato.setTextColor(colorStateList)
        font_lora.setTextColor(colorStateList)
        font_raleway.setTextColor(colorStateList)

        font_andada.setOnClickListener { selectFont(Constants.FONT_ANDADA, true) }
        font_lato.setOnClickListener { selectFont(Constants.FONT_LATO, true) }
        font_lora.setOnClickListener { selectFont(Constants.FONT_LORA, true) }
        font_raleway.setOnClickListener { selectFont(Constants.FONT_RALEWAY, true) }
    }

    private fun controlsEnabled(value: Boolean) {
        listOf(
            card_normal,
            card_dark,
            font_andada,
            font_lato,
            font_lora,
            font_raleway,
            card_text_size_minus,
            card_text_size_add,
            card_orientation_vertical,
            card_orientation_horizontal
        ).forEach {
            it ?: return
            it.isEnabled = value
            it.isClickable = value
        }
    }

    private fun selectFont(selectedFont: Int, isReloadNeeded: Boolean) {
        if (config.font == selectedFont) return

        setSelectedFont(selectedFont)

        config.font = selectedFont
        if (isAdded && isReloadNeeded) {
            AppUtil.saveConfig(activity, config)
            EventBus.getDefault().post(ReloadDataEvent())
        }
    }

    private fun setSelectedFont(font: Int) {
        listOf(
            Triple(Constants.FONT_ANDADA, font_andada, card_font_andada),
            Triple(Constants.FONT_LATO, font_lato, card_font_lato),
            Triple(Constants.FONT_LORA, font_lora, card_font_lora),
            Triple(Constants.FONT_RALEWAY, font_raleway, card_font_raleway)
        ).forEach {
            it.second.isSelected = it.first == font
            val color = if (it.first == font) {
                R.color.view_bottom_sheet_item_selected
            } else {
                R.color.view_bottom_sheet_item
            }
            it.third.strokeColor = ResourcesCompat.getColor(resources, color, null)
        }
    }

    private fun toggleTheme(animate: Boolean) {
        val day = ContextCompat.getColor(requireContext(), R.color.white)
        val night = ContextCompat.getColor(requireContext(), R.color.night)

        colorAnimation = ValueAnimator.ofObject(
            ArgbEvaluator(),
            if (isNightMode.not()) night else day, if (isNightMode.not()) day else night
        )

        colorAnimation!!.duration = if (animate) FADE_DAY_NIGHT_MODE.toLong() else 0L

        colorAnimation!!.addUpdateListener { animator ->
            val value = animator.animatedValue as Int
            body.background.setTint(value)
        }

        val mActivity = activity

        colorAnimation!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {
                setBackgroundColor(isNightMode, isAlt)
                config.isNightMode = isNightMode
                config.isAlt = isAlt
                AppUtil.saveConfig(mActivity, config)
                EventBus.getDefault().post(ReloadDataEvent())
            }

            override fun onAnimationEnd(animator: Animator) {}

            override fun onAnimationCancel(animator: Animator) {}

            override fun onAnimationRepeat(animator: Animator) {}
        })

        colorAnimation!!.duration = if (animate) FADE_DAY_NIGHT_MODE.toLong() else 0L

        val attrs = intArrayOf(android.R.attr.navigationBarColor)
        val typedArray = activity?.theme?.obtainStyledAttributes(attrs)
        val defaultNavigationBarColor = typedArray?.getColor(
            0,
            ContextCompat.getColor(requireContext(), R.color.white)
        )
        val black = ContextCompat.getColor(requireContext(), R.color.black)

        val navigationColorAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            if (isNightMode) black else defaultNavigationBarColor,
            if (isNightMode) defaultNavigationBarColor else black
        )

        navigationColorAnim.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            activity?.window?.navigationBarColor = value
        }

        navigationColorAnim.duration = if (animate) FADE_DAY_NIGHT_MODE.toLong() else 0L
        navigationColorAnim.start()

        colorAnimation!!.start()
    }

    private fun setToolBarColor() {
        if (isNightMode.not()) {
            activityCallback.setDayMode()
        } else {
            activityCallback.setNightMode()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activityCallback.loadingView?.callback = null
        colorAnimation?.removeAllUpdateListeners()
    }

    private fun setBackgroundColor(isNightMode: Boolean, isAlt: Boolean) {

        val selected =
            ContextCompat.getColor(requireContext(), R.color.view_bottom_sheet_item_selected)
        val unselected = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        card_normal.strokeColor = if (isNightMode.not() && isAlt.not()) selected else unselected
        card_white.strokeColor = if (isNightMode.not() && isAlt) selected else unselected
        card_gray.strokeColor = if (isNightMode && isAlt) selected else unselected
        card_dark.strokeColor = if (isNightMode && isAlt.not()) selected else unselected

        if (isNightMode.not()) {
            text_brightness.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            text_background.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            text_type.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            text_size.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            text_orientation.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))


            card_font_andada.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            font_andada.setTextColor((ContextCompat.getColor(requireContext(), R.color.black)))

            card_font_lato.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            font_lato.setTextColor((ContextCompat.getColor(requireContext(), R.color.black)))

            card_font_lora.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            font_lora.setTextColor((ContextCompat.getColor(requireContext(), R.color.black)))

            card_font_raleway.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            font_raleway.setTextColor((ContextCompat.getColor(requireContext(), R.color.black)))

            card_text_size_minus.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            text_size_minus.setTextColor((ContextCompat.getColor(requireContext(), R.color.black)))

            card_text_size_add.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            text_size_add.setTextColor((ContextCompat.getColor(requireContext(), R.color.black)))

            card_orientation_vertical.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            orientation_vertical.setTextColor(
                (ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                ))
            )

            card_orientation_horizontal.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            orientation_horizontal.setTextColor(
                (ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                ))
            )

            page.setTextColor((ContextCompat.getColor(requireContext(), R.color.white)))
        } else {
            text_brightness.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            text_background.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            text_type.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            text_size.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            text_orientation.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            card_font_andada.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
            font_andada.setTextColor((ContextCompat.getColor(requireContext(), R.color.white)))

            card_font_lato.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
            font_lato.setTextColor((ContextCompat.getColor(requireContext(), R.color.white)))

            card_font_lora.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
            font_lora.setTextColor((ContextCompat.getColor(requireContext(), R.color.white)))

            card_font_raleway.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
            font_raleway.setTextColor((ContextCompat.getColor(requireContext(), R.color.white)))

            card_text_size_minus.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
            text_size_minus.setTextColor((ContextCompat.getColor(requireContext(), R.color.white)))

            card_text_size_add.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
            text_size_add.setTextColor((ContextCompat.getColor(requireContext(), R.color.white)))

            card_orientation_vertical.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
            orientation_vertical.setTextColor(
                (ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                ))
            )

            card_orientation_horizontal.background.setTint(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
            orientation_horizontal.setTextColor(
                (ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                ))
            )

            page.setTextColor((ContextCompat.getColor(requireContext(), R.color.black)))
        }
    }
}
