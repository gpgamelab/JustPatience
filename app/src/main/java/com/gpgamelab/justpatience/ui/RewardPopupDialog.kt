package com.gpgamelab.justpatience.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.gpgamelab.justpatience.R

/**
 * Reusable popup renderer.
 *
 * Callers pass:
 * - title text
 * - rewards array (count + image)
 * - button text array
 */
class RewardPopupDialog(private val activity: AppCompatActivity) {

    data class RewardItem(
        val count: Int,
        @DrawableRes val imageResId: Int
    )

    data class Model(
        val title: String,
        val rewards: List<RewardItem>,
        val buttonTexts: List<String>
    )

    data class UiConfig(
        val dialogWidthPercentLandscape: Float = 0.60f,
        val dialogWidthPercentPortrait: Float = 0.95f,
        val dialogHeightPercentLandscape: Float = 0.80f,
        val dialogHeightPercentPortrait: Float = 0.80f,
        val buttonRowWidthPercentLandscape: Float = 1f,
        val buttonRowWidthPercentPortrait: Float = 1f,
        val titleBottomPercent: Float = 0.15f,
        val buttonsTopPercent: Float = 0.80f,
        val titleOffsetInchesPortrait: Float = 0f,
        val titleOffsetInchesLandscape: Float = 0f,
        val titleTextSp: Float = 34f,
        val rewardTextSp: Float = 22f,
        val rewardImageScalePortrait: Float = 1f,
        val rewardImageScaleLandscape: Float = 1f,
        val rewardTextScalePortrait: Float = 1f,
        val rewardTextScaleLandscape: Float = 1f,
        val buttonTextOffsetInchesPortrait: Float = 0f,
        val buttonTextOffsetInchesLandscape: Float = 0f,
        val buttonTextSp: Float = 20f,
        val buttonGapDp: Float = 12f,
        val rewardGapDp: Float = 24f
    )

    fun show(
        model: Model,
        @DrawableRes baseImageResId: Int,
        onButtonClick: (index: Int, dialog: Dialog) -> Unit,
        uiConfig: UiConfig = UiConfig()
    ): Dialog {
        val root = LayoutInflater.from(activity).inflate(R.layout.dialog_win_reward_choice, null, false)

        val titleView = root.findViewById<TextView>(R.id.tv_reward_popup_title)
        val rewardsRow = root.findViewById<LinearLayout>(R.id.layout_reward_row)
        val buttonsRow = root.findViewById<LinearLayout>(R.id.layout_buttons_row)
        val background = root.findViewById<ImageView>(R.id.iv_win_popup_bg)

        // Reposition sections to generic popup zoning.
        setGuidelinePercent(root, R.id.guideline_reward_top, uiConfig.titleBottomPercent)
        setGuidelinePercent(root, R.id.guideline_buttons_top, uiConfig.buttonsTopPercent)

        titleView.visibility = View.VISIBLE
        titleView.text = model.title
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, uiConfig.titleTextSp)
        titleView.translationY = -inchesToPx(getTitleOffsetInches(uiConfig))

        background.setImageResource(baseImageResId)

        bindRewards(rewardsRow, model.rewards, uiConfig)
        bindButtons(buttonsRow, model.buttonTexts, uiConfig)

        val dialog = Dialog(activity)
        dialog.setContentView(root)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val widthPx = (activity.resources.displayMetrics.widthPixels * getDialogWidthPercent(uiConfig))
            .toInt()
            .coerceAtLeast(1)
        val heightPx = (activity.resources.displayMetrics.heightPixels * getDialogHeightPercent(uiConfig))
            .toInt()
            .coerceAtLeast(1)
        applyButtonsRowWidth(buttonsRow, widthPx, uiConfig)
        dialog.window?.setLayout(widthPx, heightPx)

        // Keep the generic two-button look requested: ~40% + ~50% widths.
        if (buttonsRow.childCount == 2) {
            (buttonsRow.getChildAt(0).layoutParams as? LinearLayout.LayoutParams)?.let {
                it.width = 0
                it.weight = 4f
                buttonsRow.getChildAt(0).layoutParams = it
            }
            (buttonsRow.getChildAt(1).layoutParams as? LinearLayout.LayoutParams)?.let {
                it.width = 0
                it.weight = 5f
                buttonsRow.getChildAt(1).layoutParams = it
            }
        }

        for (index in 0 until buttonsRow.childCount) {
            val button = buttonsRow.getChildAt(index) as? AppCompatButton ?: continue
            button.setOnClickListener { onButtonClick(index, dialog) }
        }

        dialog.show()
        return dialog
    }

    private fun bindRewards(
        rewardsRow: LinearLayout,
        rewards: List<RewardItem>,
        uiConfig: UiConfig
    ) {
        rewardsRow.removeAllViews()
        val imageScale = getRewardImageScale(uiConfig)
        val textScale = getRewardTextScale(uiConfig)
        val scaledImageHeightPx = (dpToPx(56f) * imageScale).toInt().coerceAtLeast(1)
        val scaledTextSp = (uiConfig.rewardTextSp * textScale).coerceAtLeast(1f)
        val scaledTextOverlap = -dpToPx(12f * textScale)

        rewards.forEachIndexed { index, reward ->
            val group = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = if (index < rewards.lastIndex) dpToPx(uiConfig.rewardGapDp) else 0
                }
            }

            val imageView = ImageView(activity).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(reward.imageResId)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    scaledImageHeightPx
                )
            }

            val countView = TextView(activity).apply {
                text = reward.count.coerceAtLeast(0).toString()
                setTextColor(activity.getColor(R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledTextSp)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                includeFontPadding = false
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = scaledTextOverlap
                }
            }

            group.addView(imageView)
            group.addView(countView)
            rewardsRow.addView(group)
        }
    }

    private fun bindButtons(
        buttonsRow: LinearLayout,
        buttonTexts: List<String>,
        uiConfig: UiConfig
    ) {
        buttonsRow.removeAllViews()
        val horizontalPadding = dpToPx(14f)
        val baseVerticalPadding = dpToPx(8f)
        val upwardTextOffsetPx = inchesToPx(getButtonTextOffsetInches(uiConfig)).toInt().coerceAtLeast(0)
        val topPaddingPx = (baseVerticalPadding - upwardTextOffsetPx).coerceAtLeast(0)
        val bottomPaddingPx = baseVerticalPadding + upwardTextOffsetPx

        buttonTexts.forEachIndexed { index, text ->
            val button = AppCompatButton(activity).apply {
                this.text = text
                background = activity.getDrawable(R.drawable.button_horizontal_lt_green_512x256)
                setTextColor(activity.getColor(R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, uiConfig.buttonTextSp)
                isAllCaps = false
                gravity = Gravity.CENTER
                includeFontPadding = false
                // Keep the button frame fixed; shift text up by increasing only bottom padding.
                setPadding(
                    horizontalPadding,
                    topPaddingPx,
                    horizontalPadding,
                    bottomPaddingPx
                )
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    marginEnd = if (index < buttonTexts.lastIndex) dpToPx(uiConfig.buttonGapDp) else 0
                }
            }
            buttonsRow.addView(button)
        }
    }

    private fun setGuidelinePercent(root: View, guidelineId: Int, percent: Float) {
        val guideline = root.findViewById<View>(guidelineId) ?: return
        (guideline.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
            lp.guidePercent = percent.coerceIn(0f, 1f)
            guideline.layoutParams = lp
        }
    }

    private fun applyButtonsRowWidth(buttonsRow: LinearLayout, dialogWidthPx: Int, uiConfig: UiConfig) {
        val widthPercent = getButtonRowWidthPercent(uiConfig)
        val targetWidthPx = (dialogWidthPx * widthPercent).toInt().coerceAtLeast(1)
        (buttonsRow.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
            lp.width = targetWidthPx
            buttonsRow.layoutParams = lp
        }
    }

    private fun getDialogWidthPercent(uiConfig: UiConfig): Float {
        return if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            uiConfig.dialogWidthPercentLandscape
        } else {
            uiConfig.dialogWidthPercentPortrait
        }
    }

    private fun getDialogHeightPercent(uiConfig: UiConfig): Float {
        return if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            uiConfig.dialogHeightPercentLandscape
        } else {
            uiConfig.dialogHeightPercentPortrait
        }
    }

    private fun getButtonRowWidthPercent(uiConfig: UiConfig): Float {
        return if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            uiConfig.buttonRowWidthPercentLandscape
        } else {
            uiConfig.buttonRowWidthPercentPortrait
        }
    }

    private fun getRewardImageScale(uiConfig: UiConfig): Float {
        return if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            uiConfig.rewardImageScaleLandscape
        } else {
            uiConfig.rewardImageScalePortrait
        }
    }

    private fun getRewardTextScale(uiConfig: UiConfig): Float {
        return if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            uiConfig.rewardTextScaleLandscape
        } else {
            uiConfig.rewardTextScalePortrait
        }
    }

    private fun getTitleOffsetInches(uiConfig: UiConfig): Float {
        return if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            uiConfig.titleOffsetInchesLandscape
        } else {
            uiConfig.titleOffsetInchesPortrait
        }
    }

    private fun getButtonTextOffsetInches(uiConfig: UiConfig): Float {
        return if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            uiConfig.buttonTextOffsetInchesLandscape
        } else {
            uiConfig.buttonTextOffsetInchesPortrait
        }
    }

    private fun inchesToPx(inches: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_IN,
            inches,
            activity.resources.displayMetrics
        )
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * activity.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }
}







