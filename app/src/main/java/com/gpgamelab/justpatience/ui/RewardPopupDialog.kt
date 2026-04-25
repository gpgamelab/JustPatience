package com.gpgamelab.justpatience.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.RectF
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
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

    data class ButtonItem(
        val text: String = "",
        @DrawableRes val backgroundResId: Int = R.drawable.button_horizontal_lt_green_512x256,
        val contentDescription: String? = null
    )

    data class Model(
        val title: String,
        val rewards: List<RewardItem>,
        val buttons: List<ButtonItem>
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
        val rewardRowScalePortrait: Float = 1f,
        val rewardRowScaleLandscape: Float = 1f,
        val buttonTextOffsetInchesPortrait: Float = 0f,
        val buttonTextOffsetInchesLandscape: Float = 0f,
        val buttonTextSp: Float = 20f,
        val buttonGapDp: Float = 12f,
        val rewardGapDp: Float = 24f,
        val titleOffsetPxPortrait: Float = 0f,
        val titleOffsetPxLandscape: Float = 0f,
        val showWinOnlyVictory: Boolean = false,
        val gemImageHeightDp: Float? = null,
        val gemOffsetXDp: Float = 0f,
        val gemOffsetYDp: Float = 0f,
        val ticketImageHeightDp: Float? = null,
        val ticketOffsetXDp: Float = 0f,
        val ticketOffsetYDp: Float = 0f,
        val wandImageHeightDp: Float? = null,
        val wandOffsetXDp: Float = 0f,
        val wandOffsetYDp: Float = 0f,
        val rewardTextOverrideSp: Float? = null,
        val gemNumberOffsetXDp: Float = 0f,
        val gemNumberOffsetYDp: Float = 0f,
        val ticketNumberOffsetXDp: Float = 0f,
        val ticketNumberOffsetYDp: Float = 0f,
        val wandNumberOffsetXDp: Float = 0f,
        val wandNumberOffsetYDp: Float = 0f,
        val buttonRowOffsetXDp: Float = 0f,
        val buttonRowOffsetYDp: Float = 0f,
        val claimButtonScaleX: Float = 1f,
        val claimButtonScaleY: Float = 1f,
        val claimButtonScale: Float = 1f,
        val multiplierButtonScaleX: Float = 1f,
        val multiplierButtonScaleY: Float = 1f,
        val multiplierButtonScale: Float = 1f
    )

    fun show(
        model: Model,
        @DrawableRes baseImageResId: Int,
        onButtonClick: (index: Int, dialog: Dialog) -> Unit,
        uiConfig: UiConfig = UiConfig()
    ): Dialog {
        val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
        val root = LayoutInflater.from(activity).inflate(R.layout.dialog_win_reward_choice, contentRoot, false)

        val titleView = root.findViewById<TextView>(R.id.tv_reward_popup_title)
        val buttonsRow = root.findViewById<LinearLayout>(R.id.layout_buttons_row)
        val background = root.findViewById<ImageView>(R.id.iv_win_popup_bg)
        val winOnlyVictoryView = root.findViewById<TextView>(R.id.tv_win_victory)
        val starburstView = root.findViewById<ImageView>(R.id.iv_win_popup_starburst)

        // Reposition sections to generic popup zoning.
        setGuidelinePercent(root, R.id.guideline_reward_top, uiConfig.titleBottomPercent)
        setGuidelinePercent(root, R.id.guideline_buttons_top, uiConfig.buttonsTopPercent)

        titleView.visibility = View.VISIBLE
        titleView.text = model.title
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, uiConfig.titleTextSp)
        titleView.translationY = -inchesToPx(getTitleOffsetInches(uiConfig)) + getTitleOffsetPx(uiConfig)
        winOnlyVictoryView.visibility = if (uiConfig.showWinOnlyVictory) View.VISIBLE else View.GONE
        starburstView.visibility = View.GONE

        background.setImageResource(baseImageResId)

        bindRewards(root, model.rewards, uiConfig)
        bindButtons(root, model.buttons, uiConfig)

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
        root.post { wireScaledButtonTouchDelegation(root) }
        return dialog
    }

    private fun bindRewards(
        root: View,
        rewards: List<RewardItem>,
        uiConfig: UiConfig
    ) {
        val gemImageView = root.findViewById<ImageView>(R.id.iv_main_reward_gems)
        val gemCountView = root.findViewById<TextView>(R.id.tv_main_reward_amount)
        val ticketGroup = root.findViewById<LinearLayout>(R.id.layout_tickets_group)
        val ticketImageView = root.findViewById<ImageView>(R.id.iv_main_reward_tickets)
        val ticketCountView = root.findViewById<TextView>(R.id.tv_ticket_reward_amount)
        val wandGroup = root.findViewById<LinearLayout>(R.id.layout_wand_group)
        val wandImageView = root.findViewById<ImageView>(R.id.iv_main_reward_wand)
        val wandCountView = root.findViewById<TextView>(R.id.tv_wand_reward_amount)
        val rewardRow = root.findViewById<LinearLayout>(R.id.layout_reward_row)

        val gemReward = rewards.getOrNull(0)
        val ticketReward = rewards.getOrNull(1)
        val wandReward = rewards.getOrNull(2)
        val rewardTextSp = uiConfig.rewardTextOverrideSp
            ?: (uiConfig.rewardTextSp * getRewardTextScale(uiConfig)).coerceAtLeast(1f)

        val rewardRowScale = if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            uiConfig.rewardRowScaleLandscape
        } else {
            uiConfig.rewardRowScalePortrait
        }.coerceAtLeast(0.1f)
        rewardRow.scaleX = rewardRowScale
        rewardRow.scaleY = rewardRowScale

        gemImageView.visibility = if (gemReward != null) View.VISIBLE else View.GONE
        gemCountView.visibility = if (gemReward != null) View.VISIBLE else View.GONE
        gemReward?.let { reward ->
            gemImageView.setImageResource(reward.imageResId)
            gemCountView.text = reward.count.coerceAtLeast(0).toString()
            applyImageHeight(gemImageView, uiConfig.gemImageHeightDp ?: (52f * getRewardImageScale(uiConfig)))
            gemImageView.translationX = dpToPxFloatSigned(uiConfig.gemOffsetXDp)
            gemImageView.translationY = dpToPxFloatSigned(uiConfig.gemOffsetYDp)
            gemCountView.setTextSize(TypedValue.COMPLEX_UNIT_SP, rewardTextSp)
            gemCountView.translationX = dpToPxFloatSigned(uiConfig.gemNumberOffsetXDp)
            gemCountView.translationY = dpToPxFloatSigned(uiConfig.gemNumberOffsetYDp)
        }

        ticketGroup.visibility = if (ticketReward != null) View.VISIBLE else View.GONE
        ticketImageView.visibility = if (ticketReward != null) View.VISIBLE else View.GONE
        ticketCountView.visibility = if (ticketReward != null) View.VISIBLE else View.GONE
        ticketReward?.let { reward ->
            ticketImageView.setImageResource(reward.imageResId)
            ticketCountView.text = reward.count.coerceAtLeast(0).toString()
            applyImageHeight(ticketImageView, uiConfig.ticketImageHeightDp ?: (35f * getRewardImageScale(uiConfig)))
            ticketImageView.translationX = dpToPxFloatSigned(uiConfig.ticketOffsetXDp)
            ticketImageView.translationY = dpToPxFloatSigned(uiConfig.ticketOffsetYDp)
            ticketCountView.setTextSize(TypedValue.COMPLEX_UNIT_SP, rewardTextSp)
            ticketCountView.translationX = dpToPxFloatSigned(uiConfig.ticketNumberOffsetXDp)
            ticketCountView.translationY = dpToPxFloatSigned(uiConfig.ticketNumberOffsetYDp)
        }

        wandGroup.visibility = if (wandReward != null) View.VISIBLE else View.GONE
        wandImageView.visibility = if (wandReward != null) View.VISIBLE else View.GONE
        wandCountView.visibility = if (wandReward != null) View.VISIBLE else View.GONE
        wandReward?.let { reward ->
            wandImageView.setImageResource(reward.imageResId)
            wandCountView.text = reward.count.coerceAtLeast(0).toString()
            applyImageHeight(wandImageView, uiConfig.wandImageHeightDp ?: (35f * getRewardImageScale(uiConfig)))
            wandImageView.translationX = dpToPxFloatSigned(uiConfig.wandOffsetXDp)
            wandImageView.translationY = dpToPxFloatSigned(uiConfig.wandOffsetYDp)
            wandCountView.setTextSize(TypedValue.COMPLEX_UNIT_SP, rewardTextSp)
            wandCountView.translationX = dpToPxFloatSigned(uiConfig.wandNumberOffsetXDp)
            wandCountView.translationY = dpToPxFloatSigned(uiConfig.wandNumberOffsetYDp)
        }
    }

    private fun bindButtons(
        root: View,
        buttons: List<ButtonItem>,
        uiConfig: UiConfig
    ) {
        val buttonsRow = root.findViewById<LinearLayout>(R.id.layout_buttons_row)
        val continueButton = root.findViewById<AppCompatButton>(R.id.btn_win_continue)
        val multiplierButton = root.findViewById<AppCompatButton>(R.id.btn_win_multiplier)
        val horizontalPadding = dpToPx(14f)
        val baseVerticalPadding = dpToPx(8f)
        val upwardTextOffsetPx = inchesToPx(getButtonTextOffsetInches(uiConfig)).toInt().coerceAtLeast(0)
        val topPaddingPx = (baseVerticalPadding - upwardTextOffsetPx).coerceAtLeast(0)
        val bottomPaddingPx = baseVerticalPadding + upwardTextOffsetPx

        val buttonViews = listOf(continueButton, multiplierButton)
        buttonViews.forEachIndexed { index, button ->
            val buttonItem = buttons.getOrNull(index)
            if (buttonItem == null) {
                button.visibility = View.GONE
                return@forEachIndexed
            }

            val accessibleLabel = (buttonItem.contentDescription ?: buttonItem.text).takeIf { it.isNotBlank() }
            button.visibility = View.VISIBLE
            button.text = buttonItem.text
            button.background = activity.getDrawable(buttonItem.backgroundResId)
            button.setTextColor(activity.getColor(R.color.white))
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, uiConfig.buttonTextSp)
            button.isAllCaps = false
            button.gravity = Gravity.CENTER
            button.includeFontPadding = false
            button.minWidth = 0
            button.minHeight = 0
            if (buttonItem.text.isBlank()) {
                button.setPadding(0, 0, 0, 0)
            } else {
                button.setPadding(
                    horizontalPadding,
                    topPaddingPx,
                    horizontalPadding,
                    bottomPaddingPx
                )
            }
            button.contentDescription = accessibleLabel
        }

        buttonsRow.translationX = dpToPxFloatSigned(uiConfig.buttonRowOffsetXDp)
        buttonsRow.translationY = dpToPxFloatSigned(uiConfig.buttonRowOffsetYDp)
        continueButton.scaleX = (uiConfig.claimButtonScaleX * uiConfig.claimButtonScale).coerceAtLeast(0.1f)
        continueButton.scaleY = (uiConfig.claimButtonScaleY * uiConfig.claimButtonScale).coerceAtLeast(0.1f)
        multiplierButton.scaleX = (uiConfig.multiplierButtonScaleX * uiConfig.multiplierButtonScale).coerceAtLeast(0.1f)
        multiplierButton.scaleY = (uiConfig.multiplierButtonScaleY * uiConfig.multiplierButtonScale).coerceAtLeast(0.1f)
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

    private fun getTitleOffsetPx(uiConfig: UiConfig): Float {
        return if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            uiConfig.titleOffsetPxLandscape
        } else {
            uiConfig.titleOffsetPxPortrait
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

    private fun applyImageHeight(imageView: ImageView, heightDp: Float) {
        val lp = imageView.layoutParams ?: return
        lp.height = dpToPx(heightDp)
        imageView.layoutParams = lp
    }

    private fun dpToPxFloatSigned(dp: Float): Float {
        return dp * activity.resources.displayMetrics.density
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * activity.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    private fun wireScaledButtonTouchDelegation(dialogView: View) {
        val popupBody = dialogView.findViewById<ViewGroup>(R.id.layout_popup_body) ?: return
        val continueButton = dialogView.findViewById<AppCompatButton>(R.id.btn_win_continue) ?: return
        val multiplierButton = dialogView.findViewById<AppCompatButton>(R.id.btn_win_multiplier) ?: return

        var activeTarget: AppCompatButton? = null
        popupBody.setOnTouchListener { _, event ->
            val continueHit = isPointInsideTransformedChild(event.x, event.y, continueButton, popupBody)
            val multiplierHit = isPointInsideTransformedChild(event.x, event.y, multiplierButton, popupBody)
            val target = when {
                continueHit -> continueButton
                multiplierHit -> multiplierButton
                else -> null
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activeTarget = target
                    activeTarget != null
                }
                MotionEvent.ACTION_MOVE -> activeTarget != null
                MotionEvent.ACTION_UP -> {
                    val shouldClick = activeTarget != null && activeTarget == target
                    activeTarget = null
                    if (shouldClick) {
                        target?.performClick()
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    activeTarget = null
                    false
                }
                else -> false
            }
        }
    }

    private fun isPointInsideTransformedChild(
        parentX: Float,
        parentY: Float,
        child: View,
        ancestor: ViewGroup
    ): Boolean {
        if (child.visibility != View.VISIBLE || child.width <= 0 || child.height <= 0) return false
        val bounds = getTransformedBoundsInAncestor(child, ancestor)
        return bounds.contains(parentX + ancestor.scrollX, parentY + ancestor.scrollY)
    }

    private fun getTransformedBoundsInAncestor(child: View, ancestor: ViewGroup): RectF {
        val bounds = RectF(0f, 0f, child.width.toFloat(), child.height.toFloat())
        var current: View = child
        var currentParent = current.parent as? ViewGroup
        while (currentParent != null) {
            current.matrix.mapRect(bounds)
            bounds.offset(current.left.toFloat() - currentParent.scrollX, current.top.toFloat() - currentParent.scrollY)
            if (currentParent === ancestor) break
            current = currentParent
            currentParent = current.parent as? ViewGroup
        }
        return bounds
    }
}







