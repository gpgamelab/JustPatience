package com.gpgamelab.justpatience.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import com.google.android.material.button.MaterialButton
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.gpgamelab.justpatience.BuildConfig
import com.gpgamelab.justpatience.ads.AdManager
import com.gpgamelab.justpatience.assets.AndroidAssetResolver
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.SettingsActivity
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.databinding.ActivityGameBinding
import com.gpgamelab.justpatience.model.GameStatus
import com.gpgamelab.justpatience.data.GameStatsManager
import com.gpgamelab.justpatience.util.BaselineResolutionScaleUtil
import com.gpgamelab.justpatience.util.UiScaleUtil
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.random.Random
import kotlin.math.sqrt
import kotlin.math.roundToInt
import android.graphics.drawable.GradientDrawable
import java.util.Locale

class GameActivity : AppCompatActivity(), GameMenuBottomSheetFragment.Host, TesterMenuDialogFragment.Host, DevelopMenuDialogFragment.Host {

    private var devShuffleSecondClipDelayMsState: Float = 140f
    private var devShuffleTailDelayMsState: Float = 120f
    private var devDealCardIntervalMsState: Float = 70f
    private var devLockedPileAdOffsetXPortraitPxState: Float = 0f
    private var devLockedPileAdOffsetYPortraitPxState: Float = 0f
    private var devLockedPileAdScaleXPortraitState: Float = 1f
    private var devLockedPileAdScaleYPortraitState: Float = 1f
    private var devLockedPileAdOffsetXLandscapePxState: Float = 0f
    private var devLockedPileAdOffsetYLandscapePxState: Float = 0f
    private var devLockedPileAdScaleXLandscapeState: Float = 1f
    private var devLockedPileAdScaleYLandscapeState: Float = 1f

    private enum class HelpControlAction(
        val storageKey: String,
        val titleLabel: String
    ) {
        UNDO("undo", "UNDO"),
        REDO("redo", "REDO"),
        HINT("hint", "HINT"),
        RESTART("restart", "RESTART"),
        AUTO("auto", "AUTO")
    }

    private data class WinRewards(
        val gems: Int,
        val tickets: Int,
        val wands: Int
    ) {
        fun withMultiplier(multiplier: Int): WinRewards {
            val safeMultiplier = multiplier.coerceAtLeast(1)
            return copy(
                gems = gems * safeMultiplier,
                tickets = tickets * safeMultiplier,
                wands = wands * safeMultiplier
            )
        }
    }

    private data class WinPopupUiConfig(
        val dialogWidthPercentLandscape: Float = 0.40f,
        val dialogWidthPercentPortrait: Float = 0.95f,
        val dialogHeightPercent: Float = 0.80f,
        // Scale the final dialog footprint (1.0 = unchanged).
        val dialogScaleLandscape: Float = 0.90f,
        val dialogScalePortrait: Float = 0.60f,
        // Visual-only starburst tweaks for win popup.
        val starburstOffsetXPx: Float = 0f,
        val starburstOffsetYPx: Float = 0f,
        // Baseline visual memory value; auto-layout recalculates per device/orientation.
        val starburstScale: Float = 4.0f,
        // Portrait reward band (gems position is fine; tickets get an extra nudge below)
        val rewardTopPercentPortrait: Float = 0.63f,
        val rewardBottomPercentPortrait: Float = 0.75f,
        // Landscape reward band: shifted 5 % lower than portrait
        val rewardTopPercentLandscape: Float = 0.65f,
        val rewardBottomPercentLandscape: Float = 0.74f,
        val buttonsTopPercent: Float = 0.82f,
        val buttonsBottomPercent: Float = buttonsTopPercent + 0.1f,
        val continueButtonWidthPercent: Float = 0.30f,
        val multiplierButtonWidthPercent: Float = 0.30f,
        val buttonGapDp: Float = 18f,
        val rewardAmountTextSp: Float = 20f,
        // Landscape reward row (gems+tickets image/text) rendered at 50%.
        val landscapeRewardRowScale: Float = 0.5f,
        // Portrait reward row (gems+tickets+wand image/text) rendered ~35% smaller.
        val portraitRewardRowScale: Float = 0.65f,
        // Portrait only: push the ticket group down independently of the gem group (~1–2 % of popup height)
        val ticketGroupExtraTopDpPortrait: Float = 8f,
        val ticketGroupExtraTopDpLandscape: Float = 8f
    )

    // Single tweak cluster for the custom win popup. Adjust these values instead of
    // searching through the XML when you want to nudge positions/sizes.
    private val winPopupUiConfig = WinPopupUiConfig()
    private fun buildDailyBonusPopupUiConfig(): RewardPopupDialog.UiConfig {
        val landscapeRewardScale = 0.65f
        return RewardPopupDialog.UiConfig(
            dialogWidthPercentLandscape = 0.32f,
            dialogWidthPercentPortrait = 0.684f,
            dialogHeightPercentLandscape = 0.64f,
            dialogHeightPercentPortrait = 0.576f,
            buttonRowWidthPercentLandscape = 0.70f,
            buttonRowWidthPercentPortrait = 0.70f,
            titleBottomPercent = 0.16f,
            titleOffsetInchesLandscape = 0.10f,
            titleOffsetInchesPortrait = 0.20f,
            titleOffsetPxLandscape = devDailyTitleOffsetYPxState,
            titleOffsetPxPortrait = devDailyTitleOffsetYPxState,
            titleTextSp = devDailyTitleTextSizeSpState,
            rewardImageScalePortrait = 1f,
            rewardTextScalePortrait = 1f,
            rewardImageScaleLandscape = 1f,
            rewardTextScaleLandscape = 1f,
            rewardRowScalePortrait = 1f,
            rewardRowScaleLandscape = landscapeRewardScale,
            buttonTextOffsetInchesLandscape = 0f,
            buttonsTopPercent = 0.78f,
            showWinOnlyVictory = false,
            gemImageHeightDp = devDailyGemImageHeightDpState,
            gemOffsetXDp = devDailyGemOffsetXDpState,
            gemOffsetYDp = devDailyGemOffsetYDpState,
            ticketImageHeightDp = devDailyTicketImageHeightDpState,
            ticketOffsetXDp = devDailyTicketOffsetXDpState,
            ticketOffsetYDp = devDailyTicketOffsetYDpState,
            wandImageHeightDp = devDailyWandImageHeightDpState,
            wandOffsetXDp = devDailyWandOffsetXDpState,
            wandOffsetYDp = devDailyWandOffsetYDpState,
            rewardTextOverrideSp = devDailyRewardTextSizeSpState,
            gemNumberOffsetXDp = devDailyGemNumberOffsetXDpState,
            gemNumberOffsetYDp = devDailyGemNumberOffsetYDpState,
            ticketNumberOffsetXDp = devDailyTicketNumberOffsetXDpState,
            ticketNumberOffsetYDp = devDailyTicketNumberOffsetYDpState,
            wandNumberOffsetXDp = devDailyWandNumberOffsetXDpState,
            wandNumberOffsetYDp = devDailyWandNumberOffsetYDpState,
            buttonRowOffsetXDp = devDailyButtonRowOffsetXDpState,
            buttonRowOffsetYDp = devDailyButtonRowOffsetYDpState,
            claimButtonScaleX = devDailyClaimButtonScaleXState,
            claimButtonScaleY = devDailyClaimButtonScaleYState,
            claimButtonScale = devDailyClaimButtonScaleState,
            multiplierButtonScaleX = devDailyMultiplierButtonScaleXState,
            multiplierButtonScaleY = devDailyMultiplierButtonScaleYState,
            multiplierButtonScale = devDailyMultiplierButtonScaleState
        )
    }

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()

    // Optional helper UI manager (keeps minimal behavior; extend as needed)
    private lateinit var uiManager: CardStackUIManager

    // Ad management
    private lateinit var adManager: AdManager
    private lateinit var statsManager: GameStatsManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var rewardPopupDialog: RewardPopupDialog
    private var moveSoundPool: SoundPool? = null
    private var moveSoundId: Int = 0
    private var moveSoundLoaded: Boolean = false
    private var shuffleSound1Id: Int = 0
    private var shuffleSound1Loaded: Boolean = false
    private var shuffleSound2Id: Int = 0
    private var shuffleSound2Loaded: Boolean = false
    private var winPopupSoundId: Int = 0
    private var winPopupSoundLoaded: Boolean = false
    private var magicWandSoundId: Int = 0
    private var magicWandSoundLoaded: Boolean = false
    private var muteCardSounds: Boolean = false
    private var muteWinSound: Boolean = false

    private var helpControlFlowInProgress = false
    private var couponPendingOnRestartConfirm = false
    private var pendingCouponTargetView: View? = null

    private var winDialogShowing: Boolean = false
    private var winCelebrationPlayed: Boolean = false
    private var showWinAnimation: Boolean = true
    private var isPremiumAccount: Boolean = false
    private var forceNewGameOnLaunch: Boolean = false
    private var gameMenuExpandState = GameMenuBottomSheetFragment.ExpandState()
    private var developMenuExpandState = sessionDevelopMenuExpandState
    private var pendingWinUiAfterAnimation: Boolean = false
    private var gemTotal: Int = 0
    private var ticketTotal: Int = 0
    private var magicWandTotal: Int = 0
    private var isMagicWandSelectionMode: Boolean = false
    private var dailyBonusPromptShownThisLaunch: Boolean = false
    private var testerStarburstPositionXPx: Int = winPopupUiConfig.starburstOffsetXPx.toInt()
    private var testerStarburstPositionYPx: Int = winPopupUiConfig.starburstOffsetYPx.toInt()
    private var testerStarburstScale: Float = winPopupUiConfig.starburstScale
    private var testerStarburstPivotOffsetXPx: Int = 0
    private var testerStarburstPivotOffsetYPx: Int = 0
    private var testerStarburstRotationDurationMs: Int = DEFAULT_STARBURST_ROTATION_DURATION_MS.toInt()
    private var testerStarburstRotationEnabled: Boolean = true
    private var testerStarburstAutoLayoutEnabled: Boolean = true
    private var activeStarburstView: ImageView? = null
    private var activeStarburstAnimator: ObjectAnimator? = null
    private var activeWinPopupDialog: Dialog? = null
    private var activeWinPopupRoot: View? = null
    private var activeWinPopupBaseWidthPx: Int = 0
    private var activeWinPopupBaseHeightPx: Int = 0

    // Win popup element sizes – device-ratio-scaled defaults (reset by applyAutoWinPopupRatios).
    private var devGemImageHeightDpState: Float      = BASELINE_WIN_GEM_HEIGHT_DP
    private var devTicketImageHeightDpState: Float   = BASELINE_WIN_TICKET_HEIGHT_DP
    private var devGemOffsetXDpState: Float          = 0f
    private var devGemOffsetYDpState: Float          = -100f
    private var devTicketOffsetXDpState: Float       = 0f
    private var devTicketOffsetYDpState: Float       = -75f
    private var devWandImageHeightDpState: Float     = BASELINE_WIN_MAGIC_WAND_HEIGHT_DP
    private var devWandOffsetXDpState: Float         = 0f
    private var devWandOffsetYDpState: Float         = -250f
    private var devRewardTextSizeSpState: Float      = BASELINE_WIN_REWARD_TEXT_SP
    private var devGemNumberOffsetXDpState: Float    = 0f
    private var devGemNumberOffsetYDpState: Float    = -75f
    private var devTicketNumberOffsetXDpState: Float = 0f
    private var devTicketNumberOffsetYDpState: Float = -150f
    private var devWandNumberOffsetXDpState: Float   = 0f
    private var devWandNumberOffsetYDpState: Float   = -35f
    private var devButtonRowOffsetXDpState: Float    = 0f
    private var devButtonRowOffsetYDpState: Float    = -50f
    private var devClaimButtonScaleXState: Float     = 1f
    private var devClaimButtonScaleYState: Float     = 3f
    private var devClaimButtonScaleState: Float      = 1f
    private var devMultiplierButtonScaleXState: Float = 1f
    private var devMultiplierButtonScaleYState: Float = 3f
    private var devMultiplierButtonScaleState: Float = 1f
    private var devVictoryTextSizeSpState: Float     = BASELINE_WIN_VICTORY_TEXT_SP
    private var devVictoryOffsetXDpState: Float      = 0f
    private var devVictoryOffsetYDpState: Float      = 0f

    // Daily popup element sizes – separated from win popup tuning so both can be adjusted independently.
    private var devDailyTitleOffsetYPxState: Float       = 100f
    private var devDailyTitleTextSizeSpState: Float      = 60f
    private var devDailyGemImageHeightDpState: Float      = BASELINE_WIN_GEM_HEIGHT_DP
    private var devDailyTicketImageHeightDpState: Float   = BASELINE_WIN_TICKET_HEIGHT_DP
    private var devDailyWandImageHeightDpState: Float     = BASELINE_WIN_MAGIC_WAND_HEIGHT_DP
    private var devDailyGemOffsetXDpState: Float          = 0f
    private var devDailyGemOffsetYDpState: Float          = 40f
    private var devDailyTicketOffsetXDpState: Float       = 0f
    private var devDailyTicketOffsetYDpState: Float       = 75f
    private var devDailyWandOffsetXDpState: Float         = 0f
    private var devDailyWandOffsetYDpState: Float         = -40f
    private var devDailyRewardTextSizeSpState: Float      = BASELINE_WIN_REWARD_TEXT_SP
    private var devDailyGemNumberOffsetXDpState: Float    = 0f
    private var devDailyGemNumberOffsetYDpState: Float    = 65f
    private var devDailyTicketNumberOffsetXDpState: Float = 0f
    private var devDailyTicketNumberOffsetYDpState: Float = 0f
    private var devDailyWandNumberOffsetXDpState: Float   = 0f
    private var devDailyWandNumberOffsetYDpState: Float   = 95f
    private var devDailyButtonRowOffsetXDpState: Float    = 0f
    private var devDailyButtonRowOffsetYDpState: Float    = 0f
    private var devDailyClaimButtonScaleXState: Float     = 0.8f
    private var devDailyClaimButtonScaleYState: Float     = 3f
    private var devDailyClaimButtonScaleState: Float      = 1f
    private var devDailyMultiplierButtonScaleXState: Float = 0.7f
    private var devDailyMultiplierButtonScaleYState: Float = 3f
    private var devDailyMultiplierButtonScaleState: Float = 1f

    // Unlock popup dialog width scaling (for No Tickets popup)
    private var devUnlockDialogWidthPercentPortraitState: Float = 0.85f
    private var devUnlockDialogWidthPercentLandscapeState: Float = 0.65f

    // Unlock help popup dev state
    private var devUnlockFrameScaleXState: Float = 1.0f
    private var devUnlockFrameScaleYState: Float = 1.0f
    private var devUnlockDescTextSizeSpState: Float = 20.0f
    private var devUnlockDescOffsetXDpState: Float = 0.0f
    private var devUnlockDescOffsetYDpState: Float = 60.0f
    private var devUnlockAdBtnScaleXState: Float = 0.7f
    private var devUnlockAdBtnScaleYState: Float = 1.4f
    private var devUnlockAdBtnOffsetXDpState: Float = 50.0f
    private var devUnlockAdBtnOffsetYDpState: Float = 50.0f
    private var devUnlockCancelBtnScaleXState: Float = 0.7f
    private var devUnlockCancelBtnScaleYState: Float = 1.0f
    private var devUnlockCancelBtnOffsetXDpState: Float = -50.0f
    private var devUnlockCancelBtnOffsetYDpState: Float = 48.0f

    // -------------------------------------------------------------------------
    // Auto starburst layout profile
    // -------------------------------------------------------------------------

    /**
     * Auto-computed starburst scale and position for the current device and orientation.
     *
     * The medium tablet is the remembered baseline:
     *   • portrait baseline resolution = 1600 × 2560 px
     *   • baseline starburst scale     = 4.0x
     *   • baseline starburst posY      = -33 px
     *
     * The current device ratio is the average of width-ratio and height-ratio against the
     * orientation-matched baseline resolution. Feature values are then derived as:
     *   • scale = baselineScale × averageRatio
     *   • posY  = baselinePosY ÷ averageRatio
     */
    private data class AutoStarburstProfile(
        val scale: Float,
        val positionXPx: Int,
        val positionYPx: Int
    )

    private fun computeAutoStarburstProfile(): AutoStarburstProfile {
        val ratioProfile = BaselineResolutionScaleUtil.calculateAverageRatio(
            context = this,
            baselinePortraitWidthPx = WIN_POPUP_BASELINE_PORTRAIT_WIDTH_PX,
            baselinePortraitHeightPx = WIN_POPUP_BASELINE_PORTRAIT_HEIGHT_PX
        )

        val scale = BaselineResolutionScaleUtil
            .scaleFromBaseline(WIN_POPUP_BASELINE_STARBURST_SCALE, ratioProfile.averageRatio)
            .coerceAtLeast(MIN_STARBURST_SCALE)
        val posYPx = BaselineResolutionScaleUtil
            .inverseScaleFromBaseline(WIN_POPUP_BASELINE_STARBURST_POSITION_Y_PX.toFloat(), ratioProfile.averageRatio)
            .roundToInt()

        return AutoStarburstProfile(scale = scale, positionXPx = 0, positionYPx = posYPx)
    }

    /** Apply the auto-computed starburst profile to the active tester values. */
    private fun applyAutoStarburstProfile() {
        val profile = computeAutoStarburstProfile()
        testerStarburstPositionXPx = profile.positionXPx
        testerStarburstPositionYPx = profile.positionYPx
        testerStarburstScale       = profile.scale
        testerStarburstAutoLayoutEnabled = true
    }

    /**
     * Compute ratio-scaled defaults for win-popup element sizes (everything except starburst).
     * Baseline is the medium tablet at 1600 × 2560 px.
     */
    private fun applyAutoWinPopupRatios() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val ratio = BaselineResolutionScaleUtil.calculateAverageRatio(
            this,
            baselinePortraitWidthPx  = 1600,
            baselinePortraitHeightPx = 2560
        ).averageRatio
        devGemImageHeightDpState    = BaselineResolutionScaleUtil.scaleFromBaseline(BASELINE_WIN_GEM_HEIGHT_DP, ratio)
        devTicketImageHeightDpState = BaselineResolutionScaleUtil.scaleFromBaseline(BASELINE_WIN_TICKET_HEIGHT_DP, ratio)
        devWandImageHeightDpState   = BaselineResolutionScaleUtil.scaleFromBaseline(BASELINE_WIN_MAGIC_WAND_HEIGHT_DP, ratio)
        devRewardTextSizeSpState    = BaselineResolutionScaleUtil.scaleFromBaseline(BASELINE_WIN_REWARD_TEXT_SP, ratio)
        devVictoryTextSizeSpState   = BaselineResolutionScaleUtil.scaleFromBaseline(BASELINE_WIN_VICTORY_TEXT_SP, ratio)
        devGemOffsetXDpState      = 0f
        devGemOffsetYDpState      = (if (isLandscape) -175f else -100f) * ratio
        devTicketOffsetXDpState   = 0f
        devTicketOffsetYDpState   = (if (isLandscape) -150f else -75f) * ratio
        devWandOffsetXDpState     = 0f
        devWandOffsetYDpState     = (if (isLandscape) -200f else -125f) * ratio
        devGemNumberOffsetXDpState = 0f
        devGemNumberOffsetYDpState = (if (isLandscape) -125f else -75f) * ratio
        devTicketNumberOffsetXDpState = 0f
        devTicketNumberOffsetYDpState = (if (isLandscape) -200f else -150f) * ratio
        devWandNumberOffsetXDpState = 0f
        devWandNumberOffsetYDpState = (if (isLandscape) -150f else -100f) * ratio
        devButtonRowOffsetXDpState = 0f
        devButtonRowOffsetYDpState = -50f * ratio
        devClaimButtonScaleXState = 1f
        devClaimButtonScaleYState = 3f
        devClaimButtonScaleState = 1f
        devMultiplierButtonScaleXState = 1f
        devMultiplierButtonScaleYState = 3f
        devMultiplierButtonScaleState = 1f
        devVictoryOffsetXDpState  = 0f
        devVictoryOffsetYDpState  = 0f
    }

    private fun applyAutoDailyPopupRatios() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val ratio = BaselineResolutionScaleUtil.calculateAverageRatio(
            this,
            baselinePortraitWidthPx  = 1600,
            baselinePortraitHeightPx = 2560
        ).averageRatio
        val safeRatio = ratio.coerceIn(0.50f, 1.30f)
        val buttonScaleRatio = safeRatio.coerceIn(0.65f, 1.00f)

        val titleBaseSp = if (isLandscape) BASELINE_DAILY_TITLE_TEXT_SP_LANDSCAPE else BASELINE_DAILY_TITLE_TEXT_SP_PORTRAIT
        val rewardTextBaseSp = if (isLandscape) BASELINE_DAILY_REWARD_TEXT_SP_LANDSCAPE else BASELINE_DAILY_REWARD_TEXT_SP_PORTRAIT
        val imageScale = if (isLandscape) BASELINE_DAILY_IMAGE_SCALE_LANDSCAPE else BASELINE_DAILY_IMAGE_SCALE_PORTRAIT

        devDailyTitleOffsetYPxState = BaselineResolutionScaleUtil.scaleFromBaseline(BASELINE_DAILY_TITLE_OFFSET_Y_PX, safeRatio)
        devDailyTitleTextSizeSpState = BaselineResolutionScaleUtil.scaleFromBaseline(titleBaseSp, safeRatio)
        devDailyGemImageHeightDpState = BaselineResolutionScaleUtil.scaleFromBaseline(BASELINE_WIN_GEM_HEIGHT_DP * imageScale, safeRatio)
        devDailyTicketImageHeightDpState = BaselineResolutionScaleUtil.scaleFromBaseline(BASELINE_WIN_TICKET_HEIGHT_DP * imageScale, safeRatio)
        devDailyWandImageHeightDpState = BaselineResolutionScaleUtil.scaleFromBaseline(BASELINE_WIN_MAGIC_WAND_HEIGHT_DP * imageScale, safeRatio)
        devDailyRewardTextSizeSpState = BaselineResolutionScaleUtil.scaleFromBaseline(rewardTextBaseSp, safeRatio)
        devDailyGemOffsetXDpState = 0f
        devDailyGemOffsetYDpState = (if (isLandscape) BASELINE_DAILY_GEM_OFFSET_Y_DP_LANDSCAPE else BASELINE_DAILY_GEM_OFFSET_Y_DP_PORTRAIT) * safeRatio
        devDailyTicketOffsetXDpState = 0f
        devDailyTicketOffsetYDpState = (if (isLandscape) BASELINE_DAILY_TICKET_OFFSET_Y_DP_LANDSCAPE else BASELINE_DAILY_TICKET_OFFSET_Y_DP_PORTRAIT) * safeRatio
        devDailyWandOffsetXDpState = 0f
        devDailyWandOffsetYDpState = (if (isLandscape) BASELINE_DAILY_WAND_OFFSET_Y_DP_LANDSCAPE else BASELINE_DAILY_WAND_OFFSET_Y_DP_PORTRAIT) * safeRatio
        devDailyGemNumberOffsetXDpState = 0f
        devDailyGemNumberOffsetYDpState = (if (isLandscape) BASELINE_DAILY_GEM_NUMBER_OFFSET_Y_DP_LANDSCAPE else BASELINE_DAILY_GEM_NUMBER_OFFSET_Y_DP_PORTRAIT) * safeRatio
        devDailyTicketNumberOffsetXDpState = 0f
        devDailyTicketNumberOffsetYDpState = BASELINE_DAILY_TICKET_NUMBER_OFFSET_Y_DP * safeRatio
        devDailyWandNumberOffsetXDpState = 0f
        devDailyWandNumberOffsetYDpState = (if (isLandscape) BASELINE_DAILY_WAND_NUMBER_OFFSET_Y_DP_LANDSCAPE else BASELINE_DAILY_WAND_NUMBER_OFFSET_Y_DP_PORTRAIT) * safeRatio
        devDailyButtonRowOffsetXDpState = 0f
        devDailyButtonRowOffsetYDpState = 0f
        devDailyClaimButtonScaleXState = 0.8f * buttonScaleRatio
        devDailyClaimButtonScaleYState = 3f * buttonScaleRatio
        devDailyClaimButtonScaleState = 1f
        devDailyMultiplierButtonScaleXState = 0.7f * buttonScaleRatio
        devDailyMultiplierButtonScaleYState = 3f * buttonScaleRatio
        devDailyMultiplierButtonScaleState = 1f
    }

    private fun applyAutoUnlockPopupRatios() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val ratio = BaselineResolutionScaleUtil.calculateAverageRatio(
            this,
            baselinePortraitWidthPx = 1600,
            baselinePortraitHeightPx = 2560
        ).averageRatio
        val safeRatio = ratio.coerceIn(0.50f, 1.30f)
        val buttonScaleRatio = safeRatio.coerceIn(0.60f, 1.05f)
        val widthBoost = (1f / safeRatio).coerceIn(1f, 1.35f)

        devUnlockDialogWidthPercentPortraitState = (0.85f * widthBoost).coerceIn(0.80f, 0.95f)
        devUnlockDialogWidthPercentLandscapeState = (0.65f * widthBoost).coerceIn(0.62f, 0.92f)
        devUnlockFrameScaleXState = 1.0f
        devUnlockFrameScaleYState = 1.0f
        devUnlockDescTextSizeSpState = BaselineResolutionScaleUtil.scaleFromBaseline(20f, safeRatio)
        devUnlockDescOffsetXDpState = 0f
        devUnlockDescOffsetYDpState = (if (isLandscape) 36f else 60f) * safeRatio
        devUnlockAdBtnScaleXState = 0.7f * buttonScaleRatio
        devUnlockAdBtnScaleYState = (if (isLandscape) 1.1f else 1.4f) * buttonScaleRatio
        devUnlockAdBtnOffsetXDpState = 50f * safeRatio
        devUnlockAdBtnOffsetYDpState = (if (isLandscape) 28f else 50f) * safeRatio
        devUnlockCancelBtnScaleXState = 0.7f * buttonScaleRatio
        devUnlockCancelBtnScaleYState = (if (isLandscape) 0.9f else 1.0f) * buttonScaleRatio
        devUnlockCancelBtnOffsetXDpState = -50f * safeRatio
        devUnlockCancelBtnOffsetYDpState = (if (isLandscape) 28f else 48f) * safeRatio
    }

    // -------------------------------------------------------------------------

    private companion object {
        private const val WIN_POPUP_BASELINE_PORTRAIT_WIDTH_PX = 1_600
        private const val WIN_POPUP_BASELINE_PORTRAIT_HEIGHT_PX = 2_560
        private const val WIN_POPUP_BASELINE_STARBURST_SCALE = 4.0f
        private const val WIN_POPUP_BASELINE_STARBURST_POSITION_Y_PX = -33
        private const val WIN_POPUP_DEBUG_PAUSES_ENABLED = false
        private const val WIN_POPUP_DEBUG_PAUSE_MS = 3_000L
        private const val DEFAULT_STARBURST_ROTATION_DURATION_MS = 4_000L
        private const val MIN_STARBURST_SCALE = 0.25f
        private const val DEFAULT_STARBURST_TOP_OVERFLOW_DP = 140f
        private const val STARBURST_OVERFLOW_MARGIN_DP = 12f
        private const val SHOW_STARBURST_PIVOT_DEBUG_TEXT = false
        private const val STARBURST_PIVOT_MARKER_TAG = "starburst_pivot_marker"
        private const val STARBURST_PIVOT_DEBUG_TEXT_TAG = "starburst_pivot_debug_text"
        // Win popup element baselines tuned on the medium tablet (1600 × 2560 px).
        private const val BASELINE_WIN_GEM_HEIGHT_DP     = 60f
        private const val BASELINE_WIN_TICKET_HEIGHT_DP  = 180f
        private const val BASELINE_WIN_MAGIC_WAND_HEIGHT_DP  = 100f
        private const val BASELINE_WIN_REWARD_TEXT_SP    = 60f
        private const val BASELINE_WIN_VICTORY_TEXT_SP   = 60f

        // Daily popup baselines for ratio/orientation scaling
        private const val BASELINE_DAILY_TITLE_OFFSET_Y_PX = 200f
        private const val BASELINE_DAILY_TITLE_TEXT_SP_PORTRAIT = 52f
        private const val BASELINE_DAILY_TITLE_TEXT_SP_LANDSCAPE = 56f
        private const val BASELINE_DAILY_REWARD_TEXT_SP_PORTRAIT = 52f
        private const val BASELINE_DAILY_REWARD_TEXT_SP_LANDSCAPE = 60f
        private const val BASELINE_DAILY_IMAGE_SCALE_PORTRAIT = 0.80f
        private const val BASELINE_DAILY_IMAGE_SCALE_LANDSCAPE = 1.00f
        private const val BASELINE_DAILY_GEM_OFFSET_Y_DP_PORTRAIT = 40f
        private const val BASELINE_DAILY_TICKET_OFFSET_Y_DP_PORTRAIT = 75f
        private const val BASELINE_DAILY_WAND_OFFSET_Y_DP_PORTRAIT = 25f
        private const val BASELINE_DAILY_GEM_NUMBER_OFFSET_Y_DP_PORTRAIT = 65f
        private const val BASELINE_DAILY_WAND_NUMBER_OFFSET_Y_DP_PORTRAIT = 40f
        private const val BASELINE_DAILY_GEM_OFFSET_Y_DP_LANDSCAPE = 24f
        private const val BASELINE_DAILY_TICKET_OFFSET_Y_DP_LANDSCAPE = 48f
        private const val BASELINE_DAILY_WAND_OFFSET_Y_DP_LANDSCAPE = 18f
        private const val BASELINE_DAILY_GEM_NUMBER_OFFSET_Y_DP_LANDSCAPE = 52f
        private const val BASELINE_DAILY_WAND_NUMBER_OFFSET_Y_DP_LANDSCAPE = 28f
        private const val BASELINE_DAILY_TICKET_NUMBER_OFFSET_Y_DP = 0f

        private var sessionDevelopMenuExpandState = DevelopMenuDialogFragment.ExpandState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyImmersiveFullscreen()

        // Initialise starburst tester values from the auto-scaling profile so
        // every device/orientation gets sensible defaults without manual tuning.
        applyAutoStarburstProfile()
        applyAutoWinPopupRatios()
        applyAutoDailyPopupRatios()
        applyAutoUnlockPopupRatios()

        // Apply BuildConfig-driven visibility for overlay debug buttons.
        binding.btnTesters.visibility = if (BuildConfig.SHOW_TESTER_BUTTON) View.VISIBLE else View.GONE
        binding.btnDevelop.visibility = if (BuildConfig.SHOW_DEVELOP_BUTTON) View.VISIBLE else View.GONE

        // Scale the bottom control-button row (and info panel) vertically using the
        // raw baseline factor – no extreme-aspect correction – so portrait phones get
        // slightly taller touch targets while landscape phones stay compact.
        UiScaleUtil.applyScreenVerticalScale(binding.controlButtonsLayout, this)
        UiScaleUtil.applyScreenVerticalScale(binding.gameInfoPanel, this)

        // Wire viewModel into GameBoardView
        binding.gameBoardView.viewModel = viewModel
        
        // Wire GameBoardView back into viewModel for animation scheduling
        viewModel.gameBoardView = binding.gameBoardView

        // Wire AssetResolver into GameBoardView
        binding.gameBoardView.assetResolver = AndroidAssetResolver(this)
        binding.gameBoardView.onClickMoveSoundRequested = { playCardClickMoveSound() }
        binding.gameBoardView.onShuffleSoundRequested = { playShuffleSoundSequence() }
        binding.gameBoardView.onLockedTableauUnlockRequested = { onLockedTableauUnlockRequested() }
        binding.gameBoardView.onMagicWandTargetSelected = { type, index, cardIndex ->
            onMagicWandTargetSelected(type, index, cardIndex)
        }
        applyLockedPileAdIconDevConfigToBoard()
        binding.gameBoardView.bindToViewModel(this)

        // Optional manager (no heavy rendering here)
        uiManager = CardStackUIManager(this, binding.root, viewModel)

        // Initialize and load banner ads
        adManager = AdManager(this)
        adManager.initializeAds()
        adManager.loadBannerAd(binding.adView)
        adManager.loadRewardedAd()
        adManager.loadRewardedInterstitialAd()

        statsManager = GameStatsManager(applicationContext)
        settingsManager = SettingsManager(applicationContext)
        rewardPopupDialog = RewardPopupDialog(this)
        initializeMoveSoundPool()
        renderGemHud(gemTotal)
        renderTicketHud(ticketTotal)
        renderMagicWandHud(magicWandTotal)

        // Launcher starts directly in GameActivity; default behavior is resume-or-new.
        forceNewGameOnLaunch = intent.getBooleanExtra("force_new_game", false)
        viewModel.initializeForLaunch(forceNewGameOnLaunch)

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    settingsManager.gamePlaySettingsFlow.collect { settings ->
                        isPremiumAccount = settings.premiumAcct
                        muteCardSounds = settings.muteCardSound
                        muteWinSound = settings.muteWinSound
                    }
                }

                launch {
                    settingsManager.getTotalGemsFlow().collect { persistedTotal ->
                        gemTotal = persistedTotal
                        renderGemHud(gemTotal)
                    }
                }

                launch {
                    settingsManager.getTotalTicketsFlow().collect { persistedTotal ->
                        ticketTotal = persistedTotal
                        renderTicketHud(ticketTotal)
                    }
                }

                launch {
                    settingsManager.getTotalMagicWandsFlow().collect { persistedTotal ->
                        magicWandTotal = persistedTotal
                        renderMagicWandHud(magicWandTotal)
                    }
                }

                launch {
                    viewModel.game.collect { g ->
                        binding.tvScore.text = getString(R.string.score_format, g.score)
                        binding.tvMoves.text = getString(R.string.moves_format, g.moves)

                        if (g.status != GameStatus.WON) {
                            winCelebrationPlayed = false
                            pendingWinUiAfterAnimation = false
                        } else {
                            showWinCelebrationThenDialog()
                        }
                    }
                }

                launch {
                    viewModel.gameTime.collect { seconds ->
                        binding.tvTime.text = formatTime(seconds)
                    }
                }

                launch {
                    viewModel.showGameTimer.collect { shouldShow ->
                        binding.tvTime.visibility = if (shouldShow) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.showScore.collect { shouldShow ->
                        binding.tvScore.visibility = if (shouldShow) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.showMoves.collect { shouldShow ->
                        binding.tvMoves.visibility = if (shouldShow) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.showWinAnimation.collect { enabled ->
                        showWinAnimation = enabled
                    }
                }

                launch {
                    viewModel.canUndo.collect { canUndo ->
                        findViewById<ImageView>(R.id.undo_main)?.setImageResource(
                            if (canUndo) R.drawable.undo_red else R.drawable.undo_gray
                        )
                    }
                }

                launch {
                    viewModel.canRedo.collect { canRedo ->
                        findViewById<ImageView>(R.id.redo_main)?.setImageResource(
                            if (canRedo) R.drawable.redo_blue else R.drawable.redo_gray
                        )
                    }
                }
            }
        }

        // Simple button hookups (if present in layout)
        binding.btnUndo.setOnClickListener { buttonView ->
            onHelpControlClicked(HelpControlAction.UNDO, buttonView) { handleUndoClick() }
        }
        binding.btnRedo.setOnClickListener { buttonView ->
            onHelpControlClicked(HelpControlAction.REDO, buttonView) { handleRedoClick() }
        }
        binding.btnNewGame.setOnClickListener {
            winCelebrationPlayed = false
            startNewGameWithShuffleAndDealAnimation()
        }
        findViewById<Button>(R.id.btn_restart).setOnClickListener { buttonView ->
            onHelpControlClicked(HelpControlAction.RESTART, buttonView) { handleRestartClick() }
        }
        findViewById<Button>(R.id.btn_hint)?.setOnClickListener { buttonView ->
            onHelpControlClicked(HelpControlAction.HINT, buttonView) {
                if (!viewModel.showManualHints()) {
                    Toast.makeText(this@GameActivity, R.string.no_hints_available, Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.btnStats.setOnClickListener { showGameMenu() }
        binding.btnTesters.setOnClickListener { showTesterMenu() }
        binding.btnDevelop.setOnClickListener { showDevelopMenu() }
        binding.magicWandContainer?.setOnClickListener { onMagicWandClicked() }
        findViewById<Button>(R.id.btn_auto_move)?.setOnClickListener { buttonView ->
            onHelpControlClicked(HelpControlAction.AUTO, buttonView) {
                buttonView.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val movesMade = viewModel.performAutoMove(onCardMoved = { playCardClickMoveSound() })
                        if (movesMade == 0) {
                            Toast.makeText(this@GameActivity, "No moves available", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        buttonView.isEnabled = true
                    }
                }
            }
        }

        applyResponsiveControlSizing()
        maybeShowDailyBonusOnFirstLaunch()
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    private fun initializeMoveSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        moveSoundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(4)
            .build()
            .also { pool ->
                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        when (sampleId) {
                            moveSoundId    -> moveSoundLoaded    = true
                            shuffleSound1Id -> shuffleSound1Loaded = true
                            shuffleSound2Id -> shuffleSound2Loaded = true
                            winPopupSoundId -> winPopupSoundLoaded = true
                            magicWandSoundId -> magicWandSoundLoaded = true
                        }
                    }
                }
                moveSoundId     = pool.load(this, R.raw.card_game_movement_deal_single_whoosh_light_03, 1)
                shuffleSound1Id = pool.load(this, R.raw.card_game_movement_shuffle_light_02, 1)
                shuffleSound2Id = pool.load(this, R.raw.card_game_movement_shuffle_light_03, 1)
                winPopupSoundId = pool.load(this, R.raw.floraphonic_tada_military_3_183975, 1)
                magicWandSoundId = pool.load(this, R.raw.card_game_achievement_shimmer_long_01, 1)
            }
    }

    private fun playCardClickMoveSound() {
        if (muteCardSounds || !moveSoundLoaded || moveSoundId == 0) return
        moveSoundPool?.play(moveSoundId, 1f, 1f, 1, 0, 1f)
    }

    private fun playMagicWandSound() {
        if (muteCardSounds || !magicWandSoundLoaded || magicWandSoundId == 0) return
        moveSoundPool?.play(magicWandSoundId, 1f, 1f, 1, 0, 1f)
    }

    private fun playWinPopupSoundIfAllowed() {
        if (muteWinSound || !winPopupSoundLoaded || winPopupSoundId == 0) return
        moveSoundPool?.play(winPopupSoundId, 1f, 1f, 1, 0, 1f)
    }

    private fun playShuffleSoundSequence(onComplete: (() -> Unit)? = null) {
        if (muteCardSounds) {
            onComplete?.invoke()
            return
        }

        if (shuffleSound1Loaded && shuffleSound1Id != 0) {
            moveSoundPool?.play(shuffleSound1Id, 1f, 1f, 1, 0, 1f)
        }

        lifecycleScope.launch {
            delay(devShuffleSecondClipDelayMsState.toLong().coerceAtLeast(0L))
            if (shuffleSound2Loaded && shuffleSound2Id != 0) {
                moveSoundPool?.play(shuffleSound2Id, 1f, 1f, 1, 0, 1f)
            }
            delay(devShuffleTailDelayMsState.toLong().coerceAtLeast(0L))
            onComplete?.invoke()
        }
    }

    private fun startNewGameWithShuffleAndDealAnimation() {
        playShuffleSoundSequence {
            if (isFinishing || isDestroyed) return@playShuffleSoundSequence
            binding.gameBoardView.resetTransientVisualState()
            viewModel.startNewGame()
            binding.gameBoardView.post {
                if (isFinishing || isDestroyed) return@post
                binding.gameBoardView.startNewGameDealAnimation(
                    dealCardIntervalMs = devDealCardIntervalMsState.toLong().coerceAtLeast(0L),
                    onCardDealt = { playCardClickMoveSound() }
                )
            }
        }
    }

    private fun startDeckSwitchedNewGameWithShuffleAndDealAnimation(deckCount: Int) {
        playShuffleSoundSequence {
            if (isFinishing || isDestroyed) return@playShuffleSoundSequence
            binding.gameBoardView.resetTransientVisualState()
            viewModel.switchDeckCountAndStartNewGame(deckCount)
            binding.gameBoardView.post {
                if (isFinishing || isDestroyed) return@post
                binding.gameBoardView.startNewGameDealAnimation(
                    dealCardIntervalMs = devDealCardIntervalMsState.toLong().coerceAtLeast(0L),
                    onCardDealt = { playCardClickMoveSound() }
                )
            }
        }
    }

    private fun restartGameWithShuffleAndDealAnimation() {
        playShuffleSoundSequence {
            if (isFinishing || isDestroyed) return@playShuffleSoundSequence
            binding.gameBoardView.resetTransientVisualState()
            viewModel.restartGame()
            // Restart should immediately restore the hand anchor without replaying the visual deal.
            binding.gameBoardView.resetTransientVisualState()
        }
    }

    private fun renderGemHud(total: Int) {
        val safeTotal = total.coerceAtLeast(0)
        binding.ivGemBag.setImageResource(
            if (safeTotal == 0) R.drawable.gems_empty_bag_1536x1536
            else R.drawable.gems_purple_bag_1536x1536
        )
        binding.tvGemCount.text = safeTotal.toString()
        binding.ivGemBag.contentDescription = getString(R.string.gems_count_content_description, safeTotal)
    }

    private fun renderTicketHud(total: Int) {
        val safeTotal = total.coerceAtLeast(0)
        binding.tvTicketCount.text = safeTotal.toString()
        binding.ivTicketIcon.contentDescription = getString(R.string.tickets_count_content_description, safeTotal)
    }

    private fun renderMagicWandHud(total: Int) {
        val safeTotal = total.coerceAtLeast(0)
        binding.tvMagicWandCount?.text = safeTotal.toString()
        binding.tvMagicWandCount?.visibility = if (safeTotal > 0) View.VISIBLE else View.GONE
        binding.ivMagicWandAdBadge?.visibility = if (safeTotal == 0) View.VISIBLE else View.GONE
    }


    private fun showRestartDialog() {
        val restartButton = findViewById<Button>(R.id.btn_restart)

        AlertDialog.Builder(this)
            .setTitle(R.string.restart_game_title)
            .setMessage(R.string.restart_game_message)
            .setPositiveButton(R.string.restart_game_text) { _, _ ->
                lifecycleScope.launch {
                    if (couponPendingOnRestartConfirm) {
                        val targetView = pendingCouponTargetView ?: restartButton
                        couponPendingOnRestartConfirm = false
                        pendingCouponTargetView = null
                        animateAndConsumeHelpCoupon(targetView)
                    }
                    restartGameWithShuffleAndDealAnimation()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                couponPendingOnRestartConfirm = false
                pendingCouponTargetView = null
            }
            .show()
    }

    private fun showGameEndDialog(isWin: Boolean) {
        if (!isWin || isFinishing || isDestroyed || winDialogShowing) return

        winDialogShowing = true
        val baseRewards = WinRewards(
            gems = 10,
            tickets = if (isPremiumAccount) 4 else 2,
            wands = if (isPremiumAccount) 2 else 1
        )

        if (isPremiumAccount) {
            showWinRewardChoiceDialogWithDebugPause(baseRewards)
            return
        }

        val shown = adManager.showRewardedInterstitialAd(
            onCompleted = { showWinRewardChoiceDialogWithDebugPause(baseRewards) }
        )

        if (!shown) {
            adManager.loadRewardedInterstitialAd()
            showWinRewardChoiceDialogWithDebugPause(baseRewards)
        }
    }

    private fun showWinRewardChoiceDialogWithDebugPause(baseRewards: WinRewards) {
        lifecycleScope.launch {
            maybeDelayWinPopupDebugPause()
            if (isFinishing || isDestroyed) {
                winDialogShowing = false
                return@launch
            }
            showWinRewardChoiceDialog(baseRewards)
        }
    }

    private suspend fun maybeDelayWinPopupDebugPause() {
        if (WIN_POPUP_DEBUG_PAUSES_ENABLED) {
            delay(WIN_POPUP_DEBUG_PAUSE_MS)
        }
    }

    private fun showWinRewardChoiceDialog(baseRewards: WinRewards) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { showWinRewardChoiceDialog(baseRewards) }
            return
        }

        if (isFinishing || isDestroyed) {
            winDialogShowing = false
            return
        }

        val adMultiplier = if (isPremiumAccount) 3 else 2

        val dialogView = layoutInflater.inflate(R.layout.dialog_win_reward_choice, null)
        val starburstView = configureWinPopupArtwork(dialogView)
        val rewardAmount = dialogView.findViewById<TextView>(R.id.tv_main_reward_amount)
        val ticketRewardAmount = dialogView.findViewById<TextView>(R.id.tv_ticket_reward_amount)
        val wandRewardAmount = dialogView.findViewById<TextView>(R.id.tv_wand_reward_amount)
        val continueButton = dialogView.findViewById<Button>(R.id.btn_win_continue)
        val multiplierButton = dialogView.findViewById<Button>(R.id.btn_win_multiplier)

        applyWinPopupUiConfig(dialogView, winPopupUiConfig)
        applyWinPopupElementSizes(dialogView)

        rewardAmount.text = getString(R.string.win_reward_popup_amount, baseRewards.gems)
        ticketRewardAmount.text = getString(R.string.win_reward_popup_amount, baseRewards.tickets)
        wandRewardAmount?.text = getString(R.string.win_reward_popup_amount, baseRewards.wands)
        multiplierButton.apply {
            setBackgroundResource(
                if (isPremiumAccount) {
                    R.drawable.ic_button_orange_orange_x3_with_ad
                } else {
                    R.drawable.ic_button_orange_orange_x2_with_ad
                }
            )
            text = ""
            minWidth = 0
            minHeight = 0
            setPadding(0, 0, 0, 0)
        }

        val dialogScale = getWinPopupScale(winPopupUiConfig)
        val widthPercent = getWinPopupWidthPercent(winPopupUiConfig)
        val widthPx = (
            resources.displayMetrics.widthPixels * widthPercent * dialogScale
            ).toInt().coerceAtLeast(1)
        val baseHeightPx = (
            resources.displayMetrics.heightPixels * winPopupUiConfig.dialogHeightPercent * dialogScale
            ).toInt().coerceAtLeast(1)
        val initialOverflowTopPx = estimateInitialStarburstTopOverflowPx()
        val heightPx = (baseHeightPx + initialOverflowTopPx).coerceAtLeast(1)

        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogView)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window?.setLayout(widthPx, heightPx)
        }
        activeWinPopupDialog = dialog
        activeWinPopupRoot = dialogView
        activeWinPopupBaseWidthPx = widthPx
        activeWinPopupBaseHeightPx = baseHeightPx

        continueButton.setOnClickListener {
            continueButton.isEnabled = false
            multiplierButton.isEnabled = false
            lifecycleScope.launch {
                maybeDelayWinPopupDebugPause()
                dialog.dismiss()
                completeWinRewardFlow(baseRewards)
            }
        }
        multiplierButton.setOnClickListener {
            continueButton.isEnabled = false
            multiplierButton.isEnabled = false
            lifecycleScope.launch {
                maybeDelayWinPopupDebugPause()
                dialog.dismiss()
                showWinMultiplierRewardAd(baseRewards, adMultiplier)
            }
        }

        dialog.setOnDismissListener {
            activeStarburstAnimator?.cancel()
            activeStarburstAnimator = null
            activeStarburstView = null
            activeWinPopupDialog = null
            activeWinPopupRoot = null
            activeWinPopupBaseWidthPx = 0
            activeWinPopupBaseHeightPx = 0
            winDialogShowing = false
        }
        dialog.show()
        dialog.window?.setLayout(widthPx, heightPx)
        playWinPopupSoundIfAllowed()

        // Start rotation only after the view is measured, so pivot uses true center.
        starburstView?.post {
            if (!dialog.isShowing) return@post
            activeStarburstView = starburstView
            refreshActiveStarburstDebugAndMotion()
        }
    }

    private fun configureWinPopupArtwork(dialogView: View): ImageView? {
        dialogView.findViewById<ImageView>(R.id.iv_win_popup_bg)?.apply {
            setImageResource(R.drawable.ic_popup_rect_blue)
            visibility = View.VISIBLE
            alpha = 1f
        }

        dialogView.findViewById<TextView>(R.id.tv_reward_popup_title)?.visibility = View.GONE

        val starburstView = dialogView.findViewById<ImageView>(R.id.iv_win_popup_starburst)
        starburstView?.apply {
            setImageResource(R.drawable.ic_star_burst_yellow)
            visibility = View.VISIBLE
            alpha = 1f
            translationX = testerStarburstPositionXPx.toFloat()
            translationY = testerStarburstPositionYPx.toFloat()
            scaleX = testerStarburstScale
            scaleY = testerStarburstScale
            elevation = -1f
        }

        // Keep the rotating starburst behind the popup frame/content.
        dialogView.findViewById<View>(R.id.layout_popup_body)?.bringToFront()

        return starburstView
    }

    private fun applyStarburstPivot(view: ImageView) {
        view.rotation = 0f

        // Base pivot from the actual visible starburst pixels (min/max bounds midpoint),
        // not from the ImageView box and not from manual compensation.
        val displayBounds = calculateDisplayedDrawableBoundsInView(view)
        val basePivotX = displayBounds?.centerX() ?: (view.width / 2.0f)
        val basePivotY = displayBounds?.centerY() ?: (view.height / 2.0f)
        view.pivotX = basePivotX + testerStarburstPivotOffsetXPx
        view.pivotY = basePivotY + testerStarburstPivotOffsetYPx
    }

    private fun applyStarburstPlacement(view: ImageView) {
        view.translationX = testerStarburstPositionXPx.toFloat()
        view.translationY = testerStarburstPositionYPx.toFloat()
    }

    private fun applyStarburstScale(view: ImageView) {
        view.scaleX = testerStarburstScale
        view.scaleY = testerStarburstScale
    }

    private fun startStarburstRotation(view: ImageView): ObjectAnimator {
        applyStarburstPlacement(view)
        applyStarburstScale(view)
        applyStarburstPivot(view)

        return ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply {
            duration = testerStarburstRotationDurationMs.toLong()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = android.view.animation.LinearInterpolator()
            start()
        }
    }

    private fun refreshActiveStarburstDebugAndMotion() {
        val starburstView = activeStarburstView ?: return

        activeStarburstAnimator?.cancel()
        activeStarburstAnimator = null

        applyStarburstPlacement(starburstView)
        applyStarburstScale(starburstView)
        applyStarburstPivot(starburstView)
        updateActiveWinPopupOverflow(starburstView)
        if (testerStarburstRotationEnabled) {
            activeStarburstAnimator = startStarburstRotation(starburstView)
        }
    }

    private fun estimateInitialStarburstTopOverflowPx(): Int {
        val baseStarburstHeightPx = dpToPx(133f)
        val scaledOverflow = ((testerStarburstScale - 1f).coerceAtLeast(0f) * baseStarburstHeightPx * 0.5f)
        val translationReduction = testerStarburstPositionYPx.toFloat().coerceAtLeast(0f)
        return (scaledOverflow - translationReduction + dpToPx(DEFAULT_STARBURST_TOP_OVERFLOW_DP)).toInt().coerceAtLeast(dpToPx(32f))
    }

    private fun updateActiveWinPopupOverflow(starburstView: ImageView) {
        val root = activeWinPopupRoot ?: return
        val dialog = activeWinPopupDialog ?: return
        val spacer = root.findViewById<View>(R.id.view_starburst_overflow_spacer) ?: return

        val overflowTopPx = calculateRequiredStarburstTopOverflowPx(starburstView)
        val lp = spacer.layoutParams
        if (lp.height != overflowTopPx) {
            lp.height = overflowTopPx
            spacer.layoutParams = lp
        }

        val desiredHeightPx = (activeWinPopupBaseHeightPx + overflowTopPx).coerceAtLeast(1)
        dialog.window?.setLayout(activeWinPopupBaseWidthPx.coerceAtLeast(1), desiredHeightPx)
    }

    private fun calculateRequiredStarburstTopOverflowPx(starburstView: ImageView): Int {
        val points = floatArrayOf(
            0f, 0f,
            starburstView.width.toFloat(), 0f,
            starburstView.width.toFloat(), starburstView.height.toFloat(),
            0f, starburstView.height.toFloat()
        )
        starburstView.matrix.mapPoints(points)

        var minY = Float.POSITIVE_INFINITY
        var i = 1
        while (i < points.size) {
            val y = points[i]
            if (y < minY) minY = y
            i += 2
        }

        val marginPx = dpToPx(STARBURST_OVERFLOW_MARGIN_DP)
        return (-minY + marginPx).toInt().coerceAtLeast(marginPx)
    }

    private fun buildStarburstPivotDebugText(starburstView: ImageView, pivotParentX: Float, pivotParentY: Float): String {
        val displayBounds = calculateDisplayedDrawableBoundsInView(starburstView)
        val basePivotX = displayBounds?.centerX() ?: (starburstView.width / 2f)
        val basePivotY = displayBounds?.centerY() ?: (starburstView.height / 2f)
        val finalPivotX = starburstView.pivotX
        val finalPivotY = starburstView.pivotY

        val viewCenterX = starburstView.width / 2f
        val viewCenterY = starburstView.height / 2f

        return buildString {
            appendLine("Starburst Pivot Debug")
            appendLine("visible min/max: x=${fmt(displayBounds?.left)}..${fmt(displayBounds?.right)}, y=${fmt(displayBounds?.top)}..${fmt(displayBounds?.bottom)}")
            appendLine("position: (${testerStarburstPositionXPx}, ${testerStarburstPositionYPx})")
            appendLine("scale: ${fmtScale(testerStarburstScale)}x")
            appendLine("base center: (${fmt(basePivotX)}, ${fmt(basePivotY)})")
            appendLine("offsets: (${testerStarburstPivotOffsetXPx}, ${testerStarburstPivotOffsetYPx})")
            appendLine("final pivot local: (${fmt(finalPivotX)}, ${fmt(finalPivotY)})")
            appendLine("view center local: (${fmt(viewCenterX)}, ${fmt(viewCenterY)})")
            appendLine("rotation ms/turn: $testerStarburstRotationDurationMs")
            append("pivot parent: (${fmt(pivotParentX)}, ${fmt(pivotParentY)})")
        }
    }

    private fun fmt(v: Float?): String {
        if (v == null) return "n/a"
        return String.format(Locale.US, "%.1f", v)
    }

    private fun fmtScale(v: Float): String = String.format(Locale.US, "%.2f", v)

    private fun calculateDisplayedDrawableBoundsInView(imageView: ImageView): RectF? {
        val sourceDrawable = imageView.drawable ?: return null
        val drawableWidth = sourceDrawable.intrinsicWidth.takeIf { it > 0 } ?: return null
        val drawableHeight = sourceDrawable.intrinsicHeight.takeIf { it > 0 } ?: return null

        val drawable = sourceDrawable.constantState?.newDrawable()?.mutate() ?: sourceDrawable.mutate()
        val bitmap = Bitmap.createBitmap(drawableWidth, drawableHeight, Bitmap.Config.ARGB_8888)
        return try {
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, drawableWidth, drawableHeight)
            drawable.draw(canvas)

            val pixels = IntArray(drawableWidth * drawableHeight)
            bitmap.getPixels(pixels, 0, drawableWidth, 0, 0, drawableWidth, drawableHeight)

            var minX = drawableWidth
            var minY = drawableHeight
            var maxX = -1
            var maxY = -1

            for (y in 0 until drawableHeight) {
                val rowOffset = y * drawableWidth
                for (x in 0 until drawableWidth) {
                    val alpha = pixels[rowOffset + x] ushr 24
                    if (alpha > 8) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }

            if (maxX < minX || maxY < minY) {
                null
            } else {
                val points = floatArrayOf(
                    minX.toFloat(), minY.toFloat(),
                    (maxX + 1).toFloat(), minY.toFloat(),
                    (maxX + 1).toFloat(), (maxY + 1).toFloat(),
                    minX.toFloat(), (maxY + 1).toFloat()
                )
                imageView.imageMatrix.mapPoints(points)

                var mappedMinX = Float.POSITIVE_INFINITY
                var mappedMaxX = Float.NEGATIVE_INFINITY
                var mappedMinY = Float.POSITIVE_INFINITY
                var mappedMaxY = Float.NEGATIVE_INFINITY
                var i = 0
                while (i < points.size) {
                    val x = points[i]
                    val y = points[i + 1]
                    if (x < mappedMinX) mappedMinX = x
                    if (x > mappedMaxX) mappedMaxX = x
                    if (y < mappedMinY) mappedMinY = y
                    if (y > mappedMaxY) mappedMaxY = y
                    i += 2
                }

                RectF(mappedMinX, mappedMinY, mappedMaxX, mappedMaxY)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun mapLocalPointToParent(view: View, localX: Float, localY: Float): Pair<Float, Float> {
        val points = floatArrayOf(localX, localY)
        view.matrix.mapPoints(points)
        points[0] += view.left
        points[1] += view.top
        return Pair(points[0], points[1])
    }

    private fun applyWinPopupUiConfig(dialogView: View, config: WinPopupUiConfig) {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Pick reward-band guidelines based on orientation.
        val rewardTop = if (isLandscape) config.rewardTopPercentLandscape else config.rewardTopPercentPortrait
        val rewardBottom = if (isLandscape) config.rewardBottomPercentLandscape else config.rewardBottomPercentPortrait
        setGuidelinePercent(dialogView, R.id.guideline_reward_top, rewardTop)
        setGuidelinePercent(dialogView, R.id.guideline_reward_bottom, rewardBottom)
        setGuidelinePercent(dialogView, R.id.guideline_buttons_top, config.buttonsTopPercent)
        setGuidelinePercent(dialogView, R.id.guideline_buttons_bottom, config.buttonsBottomPercent)

        // Apply text size to both reward count labels (image sizing is handled in XML).
        dialogView.findViewById<TextView>(R.id.tv_main_reward_amount)
            ?.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.rewardAmountTextSp)
        dialogView.findViewById<TextView>(R.id.tv_ticket_reward_amount)
            ?.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.rewardAmountTextSp)
        dialogView.findViewById<TextView>(R.id.tv_wand_reward_amount)
            ?.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.rewardAmountTextSp)

        // Landscape: shrink the entire reward row (images + numbers) by 50%.
        dialogView.findViewById<LinearLayout>(R.id.layout_reward_row)?.let { rewardRow ->
            val scale = if (isLandscape) config.landscapeRewardRowScale else config.portraitRewardRowScale
            rewardRow.scaleX = scale
            rewardRow.scaleY = scale
        }

        // In portrait, nudge the ticket group down independently of the gem group.
        if (!isLandscape) {
            dialogView.findViewById<LinearLayout>(R.id.layout_tickets_group)?.let { ticketsGroup ->
                (ticketsGroup.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                    lp.topMargin = dpToPx(config.ticketGroupExtraTopDpPortrait)
                    ticketsGroup.layoutParams = lp
                }
            }
        } else {
            dialogView.findViewById<LinearLayout>(R.id.layout_tickets_group)?.let { ticketsGroup ->
                (ticketsGroup.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                    lp.topMargin = dpToPx(config.ticketGroupExtraTopDpLandscape)
                    ticketsGroup.layoutParams = lp
                }
            }
        }

        val widthPercent = getWinPopupWidthPercent(config)
        val popupWidthPx = (resources.displayMetrics.widthPixels * widthPercent * getWinPopupScale(config)).toInt().coerceAtLeast(1)
        val continueButtonWidthPx = (popupWidthPx * config.continueButtonWidthPercent).toInt().coerceAtLeast(1)
        val multiplierButtonWidthPx = (popupWidthPx * config.multiplierButtonWidthPercent).toInt().coerceAtLeast(1)
        val buttonGapPx = dpToPx(config.buttonGapDp)

        dialogView.findViewById<Button>(R.id.btn_win_continue)?.let { button ->
            applyWinPopupButtonConfig(button, continueButtonWidthPx, endMarginPx = buttonGapPx)
        }
        dialogView.findViewById<Button>(R.id.btn_win_multiplier)?.let { button ->
            applyWinPopupButtonConfig(button, multiplierButtonWidthPx, endMarginPx = 0)
        }
    }

    /**
     * Apply dev-adjustable (and ratio-scaled) sizes to individual win popup elements.
     */
    private fun applyWinPopupElementSizes(dialogView: View) {
        // Ensure no clipping on any ancestor that could crop translated children
        (dialogView as? ViewGroup)?.clipChildren = false
        (dialogView as? ViewGroup)?.clipToPadding = false
        dialogView.findViewById<ViewGroup>(R.id.layout_popup_body)?.let {
            it.clipChildren = false; it.clipToPadding = false
        }
        dialogView.findViewById<ViewGroup>(R.id.layout_reward_row)?.let {
            it.clipChildren = false; it.clipToPadding = false
        }

        // Gem image height
        dialogView.findViewById<ImageView>(R.id.iv_main_reward_gems)?.let { iv ->
            (iv.parent as? ViewGroup)?.let { p -> p.clipChildren = false; p.clipToPadding = false }
            val lp = iv.layoutParams
            lp.height = dpToPx(devGemImageHeightDpState)
            iv.layoutParams = lp
            iv.translationX = dpToPxFloatSigned(devGemOffsetXDpState)
            iv.translationY = dpToPxFloatSigned(devGemOffsetYDpState)
        }
        // Ticket image height
        dialogView.findViewById<ImageView>(R.id.iv_main_reward_tickets)?.let { iv ->
            (iv.parent as? ViewGroup)?.let { p -> p.clipChildren = false; p.clipToPadding = false }
            val lp = iv.layoutParams
            lp.height = dpToPx(devTicketImageHeightDpState)
            iv.layoutParams = lp
            iv.translationX = dpToPxFloatSigned(devTicketOffsetXDpState)
            iv.translationY = dpToPxFloatSigned(devTicketOffsetYDpState)
        }
        // Wand image height (same sizing as ticket)
        dialogView.findViewById<ImageView>(R.id.iv_main_reward_wand)?.let { iv ->
            (iv.parent as? ViewGroup)?.let { p -> p.clipChildren = false; p.clipToPadding = false }
            val lp = iv.layoutParams
            lp.height = dpToPx(devWandImageHeightDpState)
            iv.layoutParams = lp
            iv.translationX = dpToPxFloatSigned(devWandOffsetXDpState)
            iv.translationY = dpToPxFloatSigned(devWandOffsetYDpState)
        }
        // Reward count text sizes
        dialogView.findViewById<TextView>(R.id.tv_main_reward_amount)?.let { tv ->
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, devRewardTextSizeSpState)
            tv.translationX = dpToPxFloatSigned(devGemNumberOffsetXDpState)
            tv.translationY = dpToPxFloatSigned(devGemNumberOffsetYDpState)
        }
        dialogView.findViewById<TextView>(R.id.tv_ticket_reward_amount)?.let { tv ->
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, devRewardTextSizeSpState)
            tv.translationX = dpToPxFloatSigned(devTicketNumberOffsetXDpState)
            tv.translationY = dpToPxFloatSigned(devTicketNumberOffsetYDpState)
        }
        dialogView.findViewById<TextView>(R.id.tv_wand_reward_amount)?.let { tv ->
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, devRewardTextSizeSpState)
            tv.translationX = dpToPxFloatSigned(devWandNumberOffsetXDpState)
            tv.translationY = dpToPxFloatSigned(devWandNumberOffsetYDpState)
        }
        dialogView.findViewById<LinearLayout>(R.id.layout_buttons_row)?.let { row ->
            row.translationX = dpToPxFloatSigned(devButtonRowOffsetXDpState)
            row.translationY = dpToPxFloatSigned(devButtonRowOffsetYDpState)
        }
        dialogView.findViewById<Button>(R.id.btn_win_continue)?.let { button ->
            val scaleX = (devClaimButtonScaleXState * devClaimButtonScaleState).coerceAtLeast(0.1f)
            val scaleY = (devClaimButtonScaleYState * devClaimButtonScaleState).coerceAtLeast(0.1f)
            button.scaleX = scaleX
            button.scaleY = scaleY
        }
        dialogView.findViewById<Button>(R.id.btn_win_multiplier)?.let { button ->
            val scaleX = (devMultiplierButtonScaleXState * devMultiplierButtonScaleState).coerceAtLeast(0.1f)
            val scaleY = (devMultiplierButtonScaleYState * devMultiplierButtonScaleState).coerceAtLeast(0.1f)
            button.scaleX = scaleX
            button.scaleY = scaleY
        }
        // VICTORY! text
        dialogView.findViewById<TextView>(R.id.tv_win_victory)?.let { tv ->
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, devVictoryTextSizeSpState)
            tv.translationX = dpToPxFloatSigned(devVictoryOffsetXDpState)
            tv.translationY = dpToPxFloatSigned(devVictoryOffsetYDpState)
        }

        // Scaled popup buttons can render outside their original bounds; remap touch hit testing
        // against transformed geometry so the full visible button surface is clickable.
        dialogView.post { wireWinPopupScaledButtonTouchDelegation(dialogView) }
    }

    private fun wireWinPopupScaledButtonTouchDelegation(dialogView: View) {
        val popupBody = dialogView.findViewById<ViewGroup>(R.id.layout_popup_body) ?: return
        val continueButton = dialogView.findViewById<Button>(R.id.btn_win_continue) ?: return
        val multiplierButton = dialogView.findViewById<Button>(R.id.btn_win_multiplier) ?: return

        var activeTarget: Button? = null
        popupBody.setOnTouchListener { _, event ->
            val continueHit = isPointInsideTransformedChild(
                parentX = event.x,
                parentY = event.y,
                child = continueButton,
                ancestor = popupBody
            )
            val multiplierHit = isPointInsideTransformedChild(
                parentX = event.x,
                parentY = event.y,
                child = multiplierButton,
                ancestor = popupBody
            )

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
                    val upTarget = target
                    val shouldClick = activeTarget != null && activeTarget == upTarget
                    activeTarget = null
                    if (shouldClick) {
                        upTarget?.performClick()
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

    /**
     * Returns the child's visual (post-transform) bounding rect in the coordinate space of
     * [ancestor], walking every intermediate parent and applying each view's matrix + offset.
     */
    private fun getTransformedBoundsInAncestor(child: View, ancestor: ViewGroup): RectF {
        val bounds = RectF(0f, 0f, child.width.toFloat(), child.height.toFloat())
        var current: View = child
        var currentParent = current.parent as? ViewGroup
        while (currentParent != null) {
            // Apply the current view's own transformation (scale/rotation/translation).
            current.matrix.mapRect(bounds)
            // Shift by the view's layout position inside its parent.
            bounds.offset(current.left.toFloat() - currentParent.scrollX, current.top.toFloat() - currentParent.scrollY)
            if (currentParent === ancestor) break
            current = currentParent
            currentParent = current.parent as? ViewGroup
        }
        return bounds
    }

    private fun refreshActiveWinPopupElementSizing() {
        val root = activeWinPopupRoot ?: return
        applyWinPopupElementSizes(root)
    }

    private fun setGuidelinePercent(root: View, guidelineId: Int, percent: Float) {
        val guideline = root.findViewById<View>(guidelineId) ?: return
        (guideline.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
            lp.guidePercent = percent.coerceIn(0f, 1f)
            guideline.layoutParams = lp
        }
    }

    private fun getWinPopupWidthPercent(config: WinPopupUiConfig): Float {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            config.dialogWidthPercentLandscape
        } else {
            config.dialogWidthPercentPortrait
        }
    }

    private fun getWinPopupScale(config: WinPopupUiConfig): Float {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            config.dialogScaleLandscape
        } else {
            config.dialogScalePortrait
        }
    }

    private fun applyWinPopupButtonConfig(button: Button, widthPx: Int, endMarginPx: Int) {
        // Win popup button artwork contains text/icons; keep as edge-to-edge images.
        button.text = ""
        button.minWidth = 0
        button.minHeight = 0
        button.setPadding(0, 0, 0, 0)
        (button.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            lp.width = widthPx
            lp.marginEnd = endMarginPx
            button.layoutParams = lp
        }
    }

    private fun showWinMultiplierRewardAd(baseRewards: WinRewards, multiplier: Int) {
        val shown = adManager.showRewardedAd(
            onFinished = { rewardEarned ->
                val rewardsToAward = if (rewardEarned) {
                    baseRewards.withMultiplier(multiplier)
                } else {
                    baseRewards
                }
                completeWinRewardFlow(rewardsToAward)
            }
        )

        if (!shown) {
            Toast.makeText(this, R.string.optional_ad_not_ready, Toast.LENGTH_SHORT).show()
            completeWinRewardFlow(baseRewards)
            adManager.loadRewardedAd()
        }
    }

    private fun awardGems(amount: Int) {
        gemTotal = (gemTotal + amount).coerceAtLeast(0)
        renderGemHud(gemTotal)
        lifecycleScope.launch {
            settingsManager.setTotalGems(gemTotal)
        }
    }

    private fun awardTickets(amount: Int) {
        ticketTotal = (ticketTotal + amount).coerceAtLeast(0)
        renderTicketHud(ticketTotal)
        lifecycleScope.launch {
            settingsManager.setTotalTickets(ticketTotal)
        }
    }

    private fun awardMagicWands(amount: Int) {
        magicWandTotal = (magicWandTotal + amount).coerceAtLeast(0)
        renderMagicWandHud(magicWandTotal)
        lifecycleScope.launch {
            settingsManager.setTotalMagicWands(magicWandTotal)
        }
    }

    private fun consumeMagicWand(): Boolean {
        if (magicWandTotal <= 0) return false
        magicWandTotal = (magicWandTotal - 1).coerceAtLeast(0)
        renderMagicWandHud(magicWandTotal)
        lifecycleScope.launch {
            settingsManager.setTotalMagicWands(magicWandTotal)
        }
        return true
    }

    private fun completeWinRewardFlow(rewardsToAward: WinRewards) {
        awardGems(rewardsToAward.gems)
        awardTickets(rewardsToAward.tickets)
        awardMagicWands(rewardsToAward.wands)
        winCelebrationPlayed = false
        startNewGameWithShuffleAndDealAnimation()
    }

    private fun setMagicWandSelectionMode(enabled: Boolean) {
        isMagicWandSelectionMode = enabled
        binding.magicWandContainer?.alpha = if (enabled) 0.7f else 1f
        binding.gameBoardView.setMagicWandSelectionMode(enabled)
    }

    private fun onMagicWandClicked() {
        if (isMagicWandSelectionMode) {
            setMagicWandSelectionMode(false)
            return
        }

        if (magicWandTotal > 0) {
            setMagicWandSelectionMode(true)
            Toast.makeText(this, R.string.magic_wand_select_target_prompt, Toast.LENGTH_SHORT).show()
            return
        }

        showMagicWandAdPrompt()
    }

    private fun showMagicWandAdPrompt() {
        AlertDialog.Builder(this)
            .setTitle(R.string.magic_wand_ad_title)
            .setMessage(R.string.magic_wand_ad_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.magic_wand_watch_ad) { _, _ ->
                val shown = adManager.showRewardedAd(
                    onFinished = { rewardEarned ->
                        if (rewardEarned) {
                            awardMagicWands(1)
                        } else {
                            Toast.makeText(this, R.string.help_unlock_ad_not_ready, Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                if (!shown) {
                    adManager.loadRewardedAd()
                    Toast.makeText(this, R.string.help_unlock_ad_not_ready, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun onMagicWandTargetSelected(
        targetType: com.gpgamelab.justpatience.model.StackType,
        targetIndex: Int,
        targetCardIndex: Int
    ) {
        if (!isMagicWandSelectionMode) return
        val used = viewModel.tryUseMagicWandOnTarget(targetType, targetIndex, targetCardIndex)
        if (used) {
            consumeMagicWand()
            playMagicWandSound()
        } else {
            Toast.makeText(this, R.string.magic_wand_no_match, Toast.LENGTH_SHORT).show()
        }
        setMagicWandSelectionMode(false)
    }

    private fun maybeShowDailyBonusOnFirstLaunch() {
        if (dailyBonusPromptShownThisLaunch) return

        lifecycleScope.launch {
            val today = currentUtcIsoDate()
            val lastClaimDate = settingsManager.getLastDailyBonusDate()
            if (lastClaimDate == today) return@launch
            showDailyBonusPopup(today)
        }
    }

    private fun showDailyBonusPopup(todayIsoDate: String?) {
        if (isFinishing || isDestroyed) return

        val baseRewards = selectDailyBonusRewards(isPremiumAccount)
        val adMultiplier = if (isPremiumAccount) 3 else 2

        val popupModel = RewardPopupDialog.Model(
            title = getString(R.string.daily_bonus_title),
            rewards = listOf(
                RewardPopupDialog.RewardItem(
                    count = baseRewards.gems,
                    imageResId = R.drawable.ic_treasure_3_gem_green
                ),
                RewardPopupDialog.RewardItem(
                    count = baseRewards.tickets,
                    imageResId = R.drawable.ic_ticket_green_yellow_helper
                ),
                RewardPopupDialog.RewardItem(
                    count = baseRewards.wands,
                    imageResId = R.drawable.ic_magic_wand_yellow
                )
            ),
            buttons = listOf(
                RewardPopupDialog.ButtonItem(
                    backgroundResId = R.drawable.ic_button_orange_orange_claim,
                    contentDescription = getString(R.string.daily_bonus_claim)
                ),
                RewardPopupDialog.ButtonItem(
                    backgroundResId = if (adMultiplier == 3) {
                        R.drawable.ic_button_orange_orange_x3_with_ad
                    } else {
                        R.drawable.ic_button_orange_orange_x2_with_ad
                    },
                    contentDescription = getString(
                        if (adMultiplier == 3) {
                            R.string.daily_bonus_x3
                        } else {
                            R.string.daily_bonus_x2
                        }
                    )
                )
            )
        )

        rewardPopupDialog.show(
            model = popupModel,
            baseImageResId = R.drawable.ic_popup_rect_blue,
            uiConfig = buildDailyBonusPopupUiConfig(),
            onButtonClick = { index, dialog ->
                dialog.dismiss()
                val claimDate = todayIsoDate ?: return@show
                when (index) {
                    0 -> claimDailyBonus(baseRewards, claimDate)
                    1 -> claimDailyBonusWithMultiplier(
                        baseRewards,
                        multiplier = adMultiplier,
                        todayIsoDate = claimDate
                    )
                }
            }
        )
    }

    private fun claimDailyBonus(baseRewards: WinRewards, todayIsoDate: String) {
        awardGems(baseRewards.gems)
        awardTickets(baseRewards.tickets)
        awardMagicWands(baseRewards.wands)
        dailyBonusPromptShownThisLaunch = true
        lifecycleScope.launch {
            settingsManager.setLastDailyBonusDate(todayIsoDate)
        }
    }

    private fun currentUtcIsoDate(): String = LocalDate.now(java.time.ZoneOffset.UTC).toString()

    private enum class DailyBonusBand {
        LOW,
        MID,
        HIGH
    }

    private fun selectDailyBonusRewards(isPremium: Boolean): WinRewards {
        val wandReward = if (isPremium) 2 else 1
        val band = pickDailyBonusBand()
        return if (isPremium) {
            when (band) {
                DailyBonusBand.LOW -> WinRewards(gems = 10, tickets = 4, wands = wandReward)
                DailyBonusBand.MID -> WinRewards(gems = 20, tickets = 5, wands = wandReward)
                DailyBonusBand.HIGH -> WinRewards(gems = 40, tickets = 6, wands = wandReward)
            }
        } else {
            when (band) {
                DailyBonusBand.LOW -> WinRewards(gems = 5, tickets = 2, wands = wandReward)
                DailyBonusBand.MID -> WinRewards(gems = 10, tickets = 3, wands = wandReward)
                DailyBonusBand.HIGH -> WinRewards(gems = 20, tickets = 4, wands = wandReward)
            }
        }
    }

    // 10% / 80% / 10% weighted random picker.
    private fun pickDailyBonusBand(): DailyBonusBand {
        val roll = Random.nextInt(100)
        return when {
            roll < 80 -> DailyBonusBand.LOW
            roll < 90 -> DailyBonusBand.MID
            else -> DailyBonusBand.HIGH
        }
    }

    private fun claimDailyBonusWithMultiplier(
        baseRewards: WinRewards,
        multiplier: Int,
        todayIsoDate: String
    ) {
        val fallbackMessage = getString(R.string.daily_bonus_multiplier_unavailable_fallback, multiplier)
        val shown = adManager.showRewardedAd(
            onFinished = { rewardEarned ->
                val rewardsToAward = if (rewardEarned) {
                    baseRewards.withMultiplier(multiplier)
                } else {
                    baseRewards
                }
                if (!rewardEarned) {
                    Toast.makeText(this, fallbackMessage, Toast.LENGTH_SHORT).show()
                }
                claimDailyBonus(rewardsToAward, todayIsoDate)
            }
        )

        if (!shown) {
            Toast.makeText(this, fallbackMessage, Toast.LENGTH_SHORT).show()
            claimDailyBonus(baseRewards, todayIsoDate)
            adManager.loadRewardedAd()
        }
    }

    private fun showStatsDialog() {
        StatsDialogFragment.newInstance().show(supportFragmentManager, "stats_dialog")
    }

    private fun showGameMenu() {
        lifecycleScope.launch {
            if (supportFragmentManager.findFragmentByTag(GameMenuBottomSheetFragment.TAG) != null) return@launch
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            GameMenuBottomSheetFragment.newInstance(
                state = gameMenuExpandState,
                currentNickname = currentSettings.playerDisplayName,
                currentDrawSize = currentSettings.drawSize,
                currentDeckCount = currentSettings.deckCount,
                currentInfiniteRecycles = currentSettings.infiniteRecycles,
                currentRecycleCount = currentSettings.recycleCount,
                currentMuteMusic = currentSettings.muteMusic,
                currentMuteCardSounds = currentSettings.muteCardSound,
                currentMuteWinSound = currentSettings.muteWinSound,
                currentShowGameTimer = currentSettings.showGameTimer,
                currentShowScore = currentSettings.showScore,
                currentShowMoves = currentSettings.showMoves,
                currentShowCardAnimations = currentSettings.showCardAnimations,
                currentAutoComplete = currentSettings.autoComplete,
                currentHaptics = currentSettings.haptics,
                currentTapToMove = currentSettings.tapToMove,
                currentScoreMethod = currentSettings.scoreMethod,
                currentFoundationToTableau = currentSettings.allowFoundationToTableauDrag,
                currentEnforceFoundationBalance = currentSettings.enforceFoundationBalance
            ).show(
                supportFragmentManager,
                GameMenuBottomSheetFragment.TAG
            )
        }
    }

    private fun showTesterMenu() {
        if (supportFragmentManager.findFragmentByTag(TesterMenuDialogFragment.TAG) != null) return
        TesterMenuDialogFragment.newInstance().show(supportFragmentManager, TesterMenuDialogFragment.TAG)
    }

    private fun showDevelopMenu() {
        if (supportFragmentManager.findFragmentByTag(DevelopMenuDialogFragment.TAG) != null) return
        DevelopMenuDialogFragment.newInstance(developMenuExpandState)
            .show(supportFragmentManager, DevelopMenuDialogFragment.TAG)
    }

    // ------------------------------------------------------------------
    // TesterMenuDialogFragment.Host implementation
    // ------------------------------------------------------------------

    override fun testerCurrentGems(): Int = gemTotal
    override fun testerCurrentCoupons(): Int = ticketTotal
    override fun testerCurrentMagicWands(): Int = magicWandTotal
    override fun testerIsPremium(): Boolean = isPremiumAccount
    override fun testerStarburstPositionX(): Int = testerStarburstPositionXPx
    override fun testerStarburstPositionY(): Int = testerStarburstPositionYPx
    override fun testerStarburstScale(): Float = testerStarburstScale
    override fun testerStarburstPivotOffsetX(): Int = testerStarburstPivotOffsetXPx
    override fun testerStarburstPivotOffsetY(): Int = testerStarburstPivotOffsetYPx
    override fun testerStarburstRotationDurationMs(): Int = testerStarburstRotationDurationMs
    override fun testerIsStarburstRotationEnabled(): Boolean = testerStarburstRotationEnabled

    override fun onTesterAdjustGems(delta: Int) {
        awardGems(delta)
    }

    override fun onTesterSetGems(value: Int) {
        gemTotal = value.coerceAtLeast(0)
        renderGemHud(gemTotal)
        lifecycleScope.launch { settingsManager.setTotalGems(gemTotal) }
    }

    override fun onTesterAdjustCoupons(delta: Int) {
        awardTickets(delta)
    }

    override fun onTesterSetCoupons(value: Int) {
        ticketTotal = value.coerceAtLeast(0)
        renderTicketHud(ticketTotal)
        lifecycleScope.launch { settingsManager.setTotalTickets(ticketTotal) }
    }

    override fun onTesterAdjustMagicWands(delta: Int) {
        awardMagicWands(delta)
    }

    override fun onTesterSetMagicWands(value: Int) {
        magicWandTotal = value.coerceAtLeast(0)
        if (magicWandTotal == 0 && isMagicWandSelectionMode) {
            setMagicWandSelectionMode(false)
        }
        renderMagicWandHud(magicWandTotal)
        lifecycleScope.launch { settingsManager.setTotalMagicWands(magicWandTotal) }
    }

    override fun onTesterSetPremium(enabled: Boolean) {
        lifecycleScope.launch {
            val latest = settingsManager.gamePlaySettingsFlow.first()
            settingsManager.saveGamePlaySettings(latest.copy(premiumAcct = enabled))
        }
    }

    override fun onTesterAdjustStarburstPivotOffsetX(delta: Int) {
        testerStarburstPivotOffsetXPx += delta
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterAdjustStarburstPivotOffsetY(delta: Int) {
        testerStarburstPivotOffsetYPx += delta
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterSetStarburstPivotOffsetX(value: Int) {
        testerStarburstPivotOffsetXPx = value
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterSetStarburstPivotOffsetY(value: Int) {
        testerStarburstPivotOffsetYPx = value
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterAdjustStarburstPositionX(delta: Int) {
        testerStarburstPositionXPx += delta
        testerStarburstAutoLayoutEnabled = false
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterAdjustStarburstPositionY(delta: Int) {
        testerStarburstPositionYPx += delta
        testerStarburstAutoLayoutEnabled = false
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterSetStarburstPositionX(value: Int) {
        testerStarburstPositionXPx = value
        testerStarburstAutoLayoutEnabled = false
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterSetStarburstPositionY(value: Int) {
        testerStarburstPositionYPx = value
        testerStarburstAutoLayoutEnabled = false
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterAdjustStarburstScale(delta: Float) {
        testerStarburstScale = (testerStarburstScale + delta).coerceAtLeast(MIN_STARBURST_SCALE)
        testerStarburstAutoLayoutEnabled = false
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterSetStarburstScale(value: Float) {
        testerStarburstScale = value.coerceAtLeast(MIN_STARBURST_SCALE)
        testerStarburstAutoLayoutEnabled = false
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterAdjustStarburstRotationDurationMs(delta: Int) {
        testerStarburstRotationDurationMs = (testerStarburstRotationDurationMs + delta).coerceAtLeast(100)
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterSetStarburstRotationDurationMs(value: Int) {
        testerStarburstRotationDurationMs = value.coerceAtLeast(100)
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterSetStarburstRotationEnabled(enabled: Boolean) {
        testerStarburstRotationEnabled = enabled
        refreshActiveStarburstDebugAndMotion()
    }

    override fun onTesterResetEverything(onComplete: () -> Unit) {

        lifecycleScope.launch {
            statsManager.deleteAllGameRecords()
            settingsManager.resetAllData()
            // Reset in-memory state immediately (flows will also catch up asynchronously)
            gemTotal = 0
            ticketTotal = 0
            magicWandTotal = 0
            setMagicWandSelectionMode(false)
            isPremiumAccount = false
            winCelebrationPlayed = false
            dailyBonusPromptShownThisLaunch = false
            // Reset starburst to the auto-computed defaults for the current device/orientation.
            applyAutoStarburstProfile()
            testerStarburstPivotOffsetXPx = 0
            testerStarburstPivotOffsetYPx = 0
            testerStarburstRotationDurationMs = DEFAULT_STARBURST_ROTATION_DURATION_MS.toInt()
            testerStarburstRotationEnabled = false
            // Reset win popup element sizes to auto-scaled defaults.
            applyAutoWinPopupRatios()
            applyAutoDailyPopupRatios()
            applyAutoUnlockPopupRatios()
            renderGemHud(gemTotal)
            renderTicketHud(ticketTotal)
            renderMagicWandHud(magicWandTotal)
            startNewGameWithShuffleAndDealAnimation()
            onComplete()
        }
    }

    override fun onTesterApplyAutoStarburstLayout() {
        applyAutoStarburstProfile()
        refreshActiveStarburstDebugAndMotion()
    }

    // ------------------------------------------------------------------
    // DevelopMenuDialogFragment.Host – win popup element sizing
    // ------------------------------------------------------------------

    override fun devGemImageHeightDp(): Float = devGemImageHeightDpState
    override fun devGemOffsetXDp(): Float = devGemOffsetXDpState
    override fun devGemOffsetYDp(): Float = devGemOffsetYDpState
    override fun devTicketImageHeightDp(): Float = devTicketImageHeightDpState
    override fun devTicketOffsetXDp(): Float = devTicketOffsetXDpState
    override fun devTicketOffsetYDp(): Float = devTicketOffsetYDpState
    override fun devWandImageHeightDp(): Float = devWandImageHeightDpState
    override fun devWandOffsetXDp(): Float = devWandOffsetXDpState
    override fun devWandOffsetYDp(): Float = devWandOffsetYDpState
    override fun devRewardTextSizeSp(): Float = devRewardTextSizeSpState
    override fun devGemNumberOffsetXDp(): Float = devGemNumberOffsetXDpState
    override fun devGemNumberOffsetYDp(): Float = devGemNumberOffsetYDpState
    override fun devTicketNumberOffsetXDp(): Float = devTicketNumberOffsetXDpState
    override fun devTicketNumberOffsetYDp(): Float = devTicketNumberOffsetYDpState
    override fun devWandNumberOffsetXDp(): Float = devWandNumberOffsetXDpState
    override fun devWandNumberOffsetYDp(): Float = devWandNumberOffsetYDpState
    override fun devButtonRowOffsetXDp(): Float = devButtonRowOffsetXDpState
    override fun devButtonRowOffsetYDp(): Float = devButtonRowOffsetYDpState
    override fun devClaimScaleX(): Float = devClaimButtonScaleXState
    override fun devClaimScaleY(): Float = devClaimButtonScaleYState
    override fun devClaimScale(): Float = devClaimButtonScaleState
    override fun devMultiplierScaleX(): Float = devMultiplierButtonScaleXState
    override fun devMultiplierScaleY(): Float = devMultiplierButtonScaleYState
    override fun devMultiplierScale(): Float = devMultiplierButtonScaleState
    override fun devVictoryTextSizeSp(): Float = devVictoryTextSizeSpState
    override fun devVictoryOffsetXDp(): Float = devVictoryOffsetXDpState
    override fun devVictoryOffsetYDp(): Float = devVictoryOffsetYDpState
    override fun devDailyTitleOffsetYPx(): Float = devDailyTitleOffsetYPxState
    override fun devDailyTitleTextSizeSp(): Float = devDailyTitleTextSizeSpState
    override fun devDailyGemImageHeightDp(): Float = devDailyGemImageHeightDpState
    override fun devDailyGemOffsetXDp(): Float = devDailyGemOffsetXDpState
    override fun devDailyGemOffsetYDp(): Float = devDailyGemOffsetYDpState
    override fun devDailyTicketImageHeightDp(): Float = devDailyTicketImageHeightDpState
    override fun devDailyTicketOffsetXDp(): Float = devDailyTicketOffsetXDpState
    override fun devDailyTicketOffsetYDp(): Float = devDailyTicketOffsetYDpState
    override fun devDailyWandImageHeightDp(): Float = devDailyWandImageHeightDpState
    override fun devDailyWandOffsetXDp(): Float = devDailyWandOffsetXDpState
    override fun devDailyWandOffsetYDp(): Float = devDailyWandOffsetYDpState
    override fun devDailyRewardTextSizeSp(): Float = devDailyRewardTextSizeSpState
    override fun devDailyGemNumberOffsetXDp(): Float = devDailyGemNumberOffsetXDpState
    override fun devDailyGemNumberOffsetYDp(): Float = devDailyGemNumberOffsetYDpState
    override fun devDailyTicketNumberOffsetXDp(): Float = devDailyTicketNumberOffsetXDpState
    override fun devDailyTicketNumberOffsetYDp(): Float = devDailyTicketNumberOffsetYDpState
    override fun devDailyWandNumberOffsetXDp(): Float = devDailyWandNumberOffsetXDpState
    override fun devDailyWandNumberOffsetYDp(): Float = devDailyWandNumberOffsetYDpState
    override fun devDailyButtonRowOffsetXDp(): Float = devDailyButtonRowOffsetXDpState
    override fun devDailyButtonRowOffsetYDp(): Float = devDailyButtonRowOffsetYDpState
    override fun devDailyClaimScaleX(): Float = devDailyClaimButtonScaleXState
    override fun devDailyClaimScaleY(): Float = devDailyClaimButtonScaleYState
    override fun devDailyClaimScale(): Float = devDailyClaimButtonScaleState
    override fun devDailyMultiplierScaleX(): Float = devDailyMultiplierButtonScaleXState
    override fun devDailyMultiplierScaleY(): Float = devDailyMultiplierButtonScaleYState
    override fun devDailyMultiplierScale(): Float = devDailyMultiplierButtonScaleState

    override fun onDevSetGemImageHeight(value: Float) { devGemImageHeightDpState = value.coerceAtLeast(4f) }
    override fun onDevSetGemOffsetX(value: Float) { devGemOffsetXDpState = value }
    override fun onDevSetGemOffsetY(value: Float) { devGemOffsetYDpState = value }
    override fun onDevSetTicketImageHeight(value: Float) { devTicketImageHeightDpState = value.coerceAtLeast(4f) }
    override fun onDevSetTicketOffsetX(value: Float) { devTicketOffsetXDpState = value }
    override fun onDevSetTicketOffsetY(value: Float) { devTicketOffsetYDpState = value }
    override fun onDevSetWandImageHeight(value: Float) { devWandImageHeightDpState = value.coerceAtLeast(4f) }
    override fun onDevSetWandOffsetX(value: Float) { devWandOffsetXDpState = value }
    override fun onDevSetWandOffsetY(value: Float) { devWandOffsetYDpState = value }
    override fun onDevSetRewardTextSize(value: Float) { devRewardTextSizeSpState = value.coerceAtLeast(4f) }
    override fun onDevSetGemNumberOffsetX(value: Float) { devGemNumberOffsetXDpState = value }
    override fun onDevSetGemNumberOffsetY(value: Float) { devGemNumberOffsetYDpState = value }
    override fun onDevSetTicketNumberOffsetX(value: Float) { devTicketNumberOffsetXDpState = value }
    override fun onDevSetTicketNumberOffsetY(value: Float) { devTicketNumberOffsetYDpState = value }
    override fun onDevSetWandNumberOffsetX(value: Float) { devWandNumberOffsetXDpState = value }
    override fun onDevSetWandNumberOffsetY(value: Float) { devWandNumberOffsetYDpState = value }
    override fun onDevSetButtonRowOffsetX(value: Float) { devButtonRowOffsetXDpState = value }
    override fun onDevSetButtonRowOffsetY(value: Float) { devButtonRowOffsetYDpState = value }
    override fun onDevSetClaimScaleX(value: Float) { devClaimButtonScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetClaimScaleY(value: Float) { devClaimButtonScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetClaimScale(value: Float) { devClaimButtonScaleState = value.coerceAtLeast(0.1f) }
    override fun onDevSetMultiplierScaleX(value: Float) { devMultiplierButtonScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetMultiplierScaleY(value: Float) { devMultiplierButtonScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetMultiplierScale(value: Float) { devMultiplierButtonScaleState = value.coerceAtLeast(0.1f) }
    override fun onDevSetVictoryTextSize(value: Float) { devVictoryTextSizeSpState = value.coerceAtLeast(4f) }
    override fun onDevSetVictoryOffsetX(value: Float) { devVictoryOffsetXDpState = value }
    override fun onDevSetVictoryOffsetY(value: Float) { devVictoryOffsetYDpState = value }
    override fun onDevSetDailyTitleOffsetY(value: Float) { devDailyTitleOffsetYPxState = value }
    override fun onDevSetDailyTitleTextSize(value: Float) { devDailyTitleTextSizeSpState = value.coerceAtLeast(4f) }
    override fun onDevSetDailyGemImageHeight(value: Float) { devDailyGemImageHeightDpState = value.coerceAtLeast(4f) }
    override fun onDevSetDailyGemOffsetX(value: Float) { devDailyGemOffsetXDpState = value }
    override fun onDevSetDailyGemOffsetY(value: Float) { devDailyGemOffsetYDpState = value }
    override fun onDevSetDailyTicketImageHeight(value: Float) { devDailyTicketImageHeightDpState = value.coerceAtLeast(4f) }
    override fun onDevSetDailyTicketOffsetX(value: Float) { devDailyTicketOffsetXDpState = value }
    override fun onDevSetDailyTicketOffsetY(value: Float) { devDailyTicketOffsetYDpState = value }
    override fun onDevSetDailyWandImageHeight(value: Float) { devDailyWandImageHeightDpState = value.coerceAtLeast(4f) }
    override fun onDevSetDailyWandOffsetX(value: Float) { devDailyWandOffsetXDpState = value }
    override fun onDevSetDailyWandOffsetY(value: Float) { devDailyWandOffsetYDpState = value }
    override fun onDevSetDailyRewardTextSize(value: Float) { devDailyRewardTextSizeSpState = value.coerceAtLeast(4f) }
    override fun onDevSetDailyGemNumberOffsetX(value: Float) { devDailyGemNumberOffsetXDpState = value }
    override fun onDevSetDailyGemNumberOffsetY(value: Float) { devDailyGemNumberOffsetYDpState = value }
    override fun onDevSetDailyTicketNumberOffsetX(value: Float) { devDailyTicketNumberOffsetXDpState = value }
    override fun onDevSetDailyTicketNumberOffsetY(value: Float) { devDailyTicketNumberOffsetYDpState = value }
    override fun onDevSetDailyWandNumberOffsetX(value: Float) { devDailyWandNumberOffsetXDpState = value }
    override fun onDevSetDailyWandNumberOffsetY(value: Float) { devDailyWandNumberOffsetYDpState = value }
    override fun onDevSetDailyButtonRowOffsetX(value: Float) { devDailyButtonRowOffsetXDpState = value }
    override fun onDevSetDailyButtonRowOffsetY(value: Float) { devDailyButtonRowOffsetYDpState = value }
    override fun onDevSetDailyClaimScaleX(value: Float) { devDailyClaimButtonScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetDailyClaimScaleY(value: Float) { devDailyClaimButtonScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetDailyClaimScale(value: Float) { devDailyClaimButtonScaleState = value.coerceAtLeast(0.1f) }
    override fun onDevSetDailyMultiplierScaleX(value: Float) { devDailyMultiplierButtonScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetDailyMultiplierScaleY(value: Float) { devDailyMultiplierButtonScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetDailyMultiplierScale(value: Float) { devDailyMultiplierButtonScaleState = value.coerceAtLeast(0.1f) }

    // Unlock popup dev getters/setters
    override fun devUnlockFrameScaleX(): Float = devUnlockFrameScaleXState
    override fun devUnlockFrameScaleY(): Float = devUnlockFrameScaleYState
    override fun devUnlockDescTextSizeSp(): Float = devUnlockDescTextSizeSpState
    override fun devUnlockDescOffsetXDp(): Float = devUnlockDescOffsetXDpState
    override fun devUnlockDescOffsetYDp(): Float = devUnlockDescOffsetYDpState
    override fun devUnlockAdBtnScaleX(): Float = devUnlockAdBtnScaleXState
    override fun devUnlockAdBtnScaleY(): Float = devUnlockAdBtnScaleYState
    override fun devUnlockAdBtnOffsetXDp(): Float = devUnlockAdBtnOffsetXDpState
    override fun devUnlockAdBtnOffsetYDp(): Float = devUnlockAdBtnOffsetYDpState
    override fun devUnlockCancelBtnScaleX(): Float = devUnlockCancelBtnScaleXState
    override fun devUnlockCancelBtnScaleY(): Float = devUnlockCancelBtnScaleYState
    override fun devUnlockCancelBtnOffsetXDp(): Float = devUnlockCancelBtnOffsetXDpState
    override fun devUnlockCancelBtnOffsetYDp(): Float = devUnlockCancelBtnOffsetYDpState
    override fun devLockedPileAdOffsetXPortraitPx(): Float = devLockedPileAdOffsetXPortraitPxState
    override fun devLockedPileAdOffsetYPortraitPx(): Float = devLockedPileAdOffsetYPortraitPxState
    override fun devLockedPileAdScaleXPortrait(): Float = devLockedPileAdScaleXPortraitState
    override fun devLockedPileAdScaleYPortrait(): Float = devLockedPileAdScaleYPortraitState
    override fun devLockedPileAdOffsetXLandscapePx(): Float = devLockedPileAdOffsetXLandscapePxState
    override fun devLockedPileAdOffsetYLandscapePx(): Float = devLockedPileAdOffsetYLandscapePxState
    override fun devLockedPileAdScaleXLandscape(): Float = devLockedPileAdScaleXLandscapeState
    override fun devLockedPileAdScaleYLandscape(): Float = devLockedPileAdScaleYLandscapeState
    override fun devShuffleSecondClipDelayMs(): Float = devShuffleSecondClipDelayMsState
    override fun devShuffleTailDelayMs(): Float = devShuffleTailDelayMsState
    override fun devDealCardIntervalMs(): Float = devDealCardIntervalMsState

    override fun onDevSetUnlockFrameScaleX(value: Float) { devUnlockFrameScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetUnlockFrameScaleY(value: Float) { devUnlockFrameScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetUnlockDescTextSize(value: Float) { devUnlockDescTextSizeSpState = value.coerceAtLeast(4f) }
    override fun onDevSetUnlockDescOffsetX(value: Float) { devUnlockDescOffsetXDpState = value }
    override fun onDevSetUnlockDescOffsetY(value: Float) { devUnlockDescOffsetYDpState = value }
    override fun onDevSetUnlockAdBtnScaleX(value: Float) { devUnlockAdBtnScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetUnlockAdBtnScaleY(value: Float) { devUnlockAdBtnScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetUnlockAdBtnOffsetX(value: Float) { devUnlockAdBtnOffsetXDpState = value }
    override fun onDevSetUnlockAdBtnOffsetY(value: Float) { devUnlockAdBtnOffsetYDpState = value }
    override fun onDevSetUnlockCancelBtnScaleX(value: Float) { devUnlockCancelBtnScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetUnlockCancelBtnScaleY(value: Float) { devUnlockCancelBtnScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetUnlockCancelBtnOffsetX(value: Float) { devUnlockCancelBtnOffsetXDpState = value }
    override fun onDevSetUnlockCancelBtnOffsetY(value: Float) { devUnlockCancelBtnOffsetYDpState = value }
    override fun onDevSetLockedPileAdOffsetXPortraitPx(value: Float) {
        devLockedPileAdOffsetXPortraitPxState = value
        applyLockedPileAdIconDevConfigToBoard()
    }
    override fun onDevSetLockedPileAdOffsetYPortraitPx(value: Float) {
        devLockedPileAdOffsetYPortraitPxState = value
        applyLockedPileAdIconDevConfigToBoard()
    }
    override fun onDevSetLockedPileAdScaleXPortrait(value: Float) {
        devLockedPileAdScaleXPortraitState = value.coerceAtLeast(0.1f)
        applyLockedPileAdIconDevConfigToBoard()
    }
    override fun onDevSetLockedPileAdScaleYPortrait(value: Float) {
        devLockedPileAdScaleYPortraitState = value.coerceAtLeast(0.1f)
        applyLockedPileAdIconDevConfigToBoard()
    }
    override fun onDevSetLockedPileAdOffsetXLandscapePx(value: Float) {
        devLockedPileAdOffsetXLandscapePxState = value
        applyLockedPileAdIconDevConfigToBoard()
    }
    override fun onDevSetLockedPileAdOffsetYLandscapePx(value: Float) {
        devLockedPileAdOffsetYLandscapePxState = value
        applyLockedPileAdIconDevConfigToBoard()
    }
    override fun onDevSetLockedPileAdScaleXLandscape(value: Float) {
        devLockedPileAdScaleXLandscapeState = value.coerceAtLeast(0.1f)
        applyLockedPileAdIconDevConfigToBoard()
    }
    override fun onDevSetLockedPileAdScaleYLandscape(value: Float) {
        devLockedPileAdScaleYLandscapeState = value.coerceAtLeast(0.1f)
        applyLockedPileAdIconDevConfigToBoard()
    }
    override fun onDevSetShuffleSecondClipDelayMs(value: Float) { devShuffleSecondClipDelayMsState = value.coerceAtLeast(0f) }
    override fun onDevSetShuffleTailDelayMs(value: Float) { devShuffleTailDelayMsState = value.coerceAtLeast(0f) }
    override fun onDevSetDealCardIntervalMs(value: Float) { devDealCardIntervalMsState = value.coerceAtLeast(0f) }

    override fun onDevApplyAutoWinPopupRatios() {
        applyAutoWinPopupRatios()
    }

    override fun onDevApplyAutoDailyPopupRatios() {
        applyAutoDailyPopupRatios()
    }

    override fun onDevExpandStateChanged(state: DevelopMenuDialogFragment.ExpandState) {
        developMenuExpandState = state
        sessionDevelopMenuExpandState = state
    }

    // ------------------------------------------------------------------

    override fun onTesterTriggerWinSequence() {
        // Reset win-flow guards so the sequence runs even if a win was already shown.
        winCelebrationPlayed = false
        winDialogShowing = false
        showGameEndDialog(true)
    }

    override fun onTesterTriggerDailyBonus() {
        showDailyBonusPopup(currentUtcIsoDate())
    }

    override fun onTesterTriggerNoTicketsPopup() {
        showNoCouponsDialog(HelpControlAction.HINT) {
            // Action callback for when hint is executed
        }
    }

    // ------------------------------------------------------------------

    override fun onGameMenuStatisticsSummary() {
        StatsSummaryDialogFragment.newInstance().show(supportFragmentManager, "stats_summary_dialog")
    }

    override fun onGameMenuStatisticsHistory() {
        showStatsDialog()
    }

    override fun onGameMenuResetStats() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_reset_stats_title)
            .setMessage(R.string.confirm_reset_stats_message)
            .setPositiveButton(R.string.reset) { _, _ ->
                lifecycleScope.launch {
                    statsManager.deleteAllGameRecords()
                    settingsManager.resetStats()
                    Toast.makeText(this@GameActivity, R.string.stats_reset_success, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onGameMenuOpenAbout() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    override fun onGameMenuOpenHowToPlay() {
        startActivity(Intent(this, HowToPlayActivity::class.java))
    }

    override fun onGameMenuRateUs() {
        // FakeReviewManager is used in debug builds so the review sheet can be tested
        // without the app being installed from the Play Store.
        // In release builds the real manager is used; Google Play may silently suppress
        // the sheet (quota, already reviewed, etc.) — openPlayStoreListing() is the fallback.
        val manager = if (BuildConfig.DEBUG) {
            FakeReviewManager(this)
        } else {
            ReviewManagerFactory.create(this)
        }

        manager.requestReviewFlow()
            .addOnCompleteListener { task: com.google.android.gms.tasks.Task<ReviewInfo> ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(this, task.result)
                        .addOnCompleteListener {
                            Log.d("GameActivity", "In-app review flow completed")
                        }
                } else {
                    Log.w("GameActivity", "In-app review unavailable, opening Play Store")
                    openPlayStoreListing()
                }
            }
    }

    override fun onGameMenuShareApp() {
        val appUrl = "https://play.google.com/store/apps/details?id=$packageName"
        val shareText = getString(R.string.share_app_message_format, appUrl)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject))
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        if (shareIntent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, R.string.share_app_no_target, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app_chooser_title)))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.share_app_no_target, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onGameMenuContactUs() {
        val supportEmail = getString(R.string.contact_us_email_address)
        val subject = getString(R.string.contact_us_subject)
        val body = buildContactUsBody()
        val emailUri = Uri.fromParts("mailto", supportEmail, null)
            .buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()
        val emailIntent = Intent(Intent.ACTION_SENDTO, emailUri)

        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_us_chooser_title)))
            return
        }

        val fallbackUrl = getString(
            R.string.contact_us_web_fallback_url_format,
            Uri.encode(supportEmail),
            Uri.encode(subject),
            Uri.encode(body)
        )
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
        if (webIntent.resolveActivity(packageManager) != null) {
            startActivity(webIntent)
        } else {
            Toast.makeText(this, R.string.contact_us_no_target, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onGameMenuOpenTermsOfService() {
        val tosHtml = readRawResourceText(R.raw.terms_of_service_2026_03_27)
        val tosUrl = getString(R.string.terms_of_service_website_url)
        val linkBlock = """
            <p><b>${getString(R.string.terms_of_service_link_title)}</b><br>
            <a href="$tosUrl">$tosUrl</a></p>
        """.trimIndent()
        val tosText = HtmlCompat.fromHtml(tosHtml + linkBlock, HtmlCompat.FROM_HTML_MODE_LEGACY)

        val textView = TextView(this).apply {
            text = tosText
            movementMethod = LinkMovementMethod.getInstance()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(40, 28, 40, 28)
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
        }
        UiScaleUtil.applyBaselineScale(scrollView, this)

        AlertDialog.Builder(this)
            .setTitle(R.string.terms_of_service_dialog_title)
            .setView(scrollView)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.terms_of_service_open_website) { _, _ ->
                openTermsOfServiceWebsite(tosUrl)
            }
            .show()
    }

    override fun onGameMenuEditNickname() {
        lifecycleScope.launch {
            val currentNickname = settingsManager.gamePlaySettingsFlow.first().playerDisplayName
            showNicknameDialog(currentNickname)
        }
    }

    override fun onGameMenuDrawCards() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showDrawCardsDialog(currentSettings.drawSize)
        }
    }

    override fun onGameMenuDeckCount() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showDeckCountDialog(currentSettings.deckCount)
        }
    }

    override fun onGameMenuWasteRecycles() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showWasteRecyclesDialog(currentSettings.infiniteRecycles, currentSettings.recycleCount)
        }
    }

    override fun onGameMenuMuteMusicToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_mute_game_music),
                enabled = currentSettings.muteMusic
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(muteMusic = enabled))
                }
            }
        }
    }

    override fun onGameMenuMuteCardSoundsToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_mute_card_movement_sounds),
                enabled = currentSettings.muteCardSound
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(muteCardSound = enabled))
                }
            }
        }
    }

    override fun onGameMenuMuteWinSoundToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_mute_win_sound),
                enabled = currentSettings.muteWinSound
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(muteWinSound = enabled))
                }
            }
        }
    }

    override fun onGameMenuShowGameTimerToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_show_game_timer),
                enabled = currentSettings.showGameTimer
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(showGameTimer = enabled))
                }
            }
        }
    }

    override fun onGameMenuShowScoreToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_show_score),
                enabled = currentSettings.showScore
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(showScore = enabled))
                }
            }
        }
    }

    override fun onGameMenuShowMovesToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_show_moves),
                enabled = currentSettings.showMoves
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(showMoves = enabled))
                }
            }
        }
    }

    override fun onGameMenuShowCardAnimationsToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_show_card_movement_animations),
                enabled = currentSettings.showCardAnimations
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(showCardAnimations = enabled))
                }
            }
        }
    }


    override fun onGameMenuAutoCompleteToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_auto_complete),
                enabled = currentSettings.autoComplete
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(autoComplete = enabled))
                }
            }
        }
    }

    override fun onGameMenuHapticsToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_haptics),
                enabled = currentSettings.haptics
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(haptics = enabled))
                }
            }
        }
    }

    override fun onGameMenuTapToMoveToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_tap_to_move),
                enabled = currentSettings.tapToMove
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(tapToMove = enabled))
                }
            }
        }
    }

    override fun onGameMenuBoardLayout() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showBoardLayoutDialog(currentSettings.boardLayout)
        }
    }

    override fun onGameMenuScoreMethod() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showScoreMethodDialog(currentSettings.scoreMethod)
        }
    }

    override fun onGameMenuFoundationToTableauToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_foundation_to_tableau),
                enabled = currentSettings.allowFoundationToTableauDrag
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        latest.copy(allowFoundationToTableauDrag = enabled)
                    )
                }
            }
        }
    }

    override fun onGameMenuEnforceFoundationBalanceToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_enforce_foundation_balance),
                enabled = currentSettings.enforceFoundationBalance
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        latest.copy(enforceFoundationBalance = enabled)
                    )
                }
            }
        }
    }


    override fun onGameMenuOpenPrivacyPolicy() {
        val policyHtml = readRawResourceText(R.raw.privacy_policy_2026_03_17)
        val policyUrl = getString(R.string.privacy_policy_website_url)
        val linkBlock = """
            <p><b>${getString(R.string.privacy_policy_link_title)}</b><br>
            <a href="$policyUrl">$policyUrl</a></p>
        """.trimIndent()
        val policyText = HtmlCompat.fromHtml(policyHtml + linkBlock, HtmlCompat.FROM_HTML_MODE_LEGACY)

        val textView = TextView(this).apply {
            text = policyText
            movementMethod = LinkMovementMethod.getInstance()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(40, 28, 40, 28)
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
        }
        UiScaleUtil.applyBaselineScale(scrollView, this)

        AlertDialog.Builder(this)
            .setTitle(R.string.privacy_policy_dialog_title)
            .setView(scrollView)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.privacy_policy_open_website) { _, _ ->
                openPrivacyPolicyWebsite(policyUrl)
            }
            .show()
    }

    private fun buildContactUsBody(): String {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val androidVersion = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        val localeTag = resources.configuration.locales[0].toLanguageTag()

        return getString(
            R.string.contact_us_body_template,
            getString(R.string.app_name),
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            packageName,
            androidVersion,
            deviceName,
            localeTag
        )
    }

    private fun showNicknameDialog(currentNickname: String) {
        val nicknameInput = EditText(this).apply {
            hint = getString(R.string.nickname_dialog_hint)
            setText(currentNickname)
            setSelection(text.length)
            setSingleLine()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nickname_dialog_title)
            .setView(nicknameInput)
            .setPositiveButton(R.string.nickname_dialog_save) { _, _ ->
                val updatedNickname = nicknameInput.text?.toString()?.trim().orEmpty()
                lifecycleScope.launch {
                    settingsManager.setPlayerDisplayName(updatedNickname)
                    Toast.makeText(this@GameActivity, R.string.nickname_dialog_saved, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    private fun showDrawCardsDialog(currentDrawSize: Int) {
        val drawOptions = arrayOf(
            getString(R.string.settings_draw_1),
            getString(R.string.settings_draw_3)
        )
        val checkedItem = if (currentDrawSize == 3) 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_menu_draw_cards)
            .setSingleChoiceItems(drawOptions, checkedItem) { dialog, which ->
                val selectedDrawSize = if (which == 1) 3 else 1
                lifecycleScope.launch {
                    val currentSettings = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(currentSettings.copy(drawSize = selectedDrawSize))
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeckCountDialog(currentDeckCount: Int) {
        val deckOptions = arrayOf(
            getString(R.string.game_menu_deck_one),
            getString(R.string.game_menu_deck_two)
        )
        val checkedItem = if (currentDeckCount == 2) 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_menu_deck_count)
            .setSingleChoiceItems(deckOptions, checkedItem) { dialog, which ->
                val selectedDeckCount = if (which == 1) 2 else 1
                if (selectedDeckCount == currentDeckCount) {
                    dialog.dismiss()
                    return@setSingleChoiceItems
                }

                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(deckCount = selectedDeckCount))
                }

                val shouldWarn = viewModel.game.value.status == GameStatus.IN_PROGRESS && viewModel.game.value.moves >= 5
                if (shouldWarn) {
                    val label = if (selectedDeckCount == 2) {
                        getString(R.string.game_menu_deck_two)
                    } else {
                        getString(R.string.game_menu_deck_one)
                    }
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.game_menu_switch_deck_warning_title)
                        .setMessage(getString(R.string.game_menu_switch_deck_warning_message, label))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            startDeckSwitchedNewGameWithShuffleAndDealAnimation(selectedDeckCount)
                        }
                        .show()
                } else {
                    startDeckSwitchedNewGameWithShuffleAndDealAnimation(selectedDeckCount)
                }

                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showWasteRecyclesDialog(isInfinite: Boolean, recycleCount: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_waste_recycles, null)
        UiScaleUtil.applyBaselineScale(dialogView, this)
        val btnMinus = dialogView.findViewById<android.widget.Button>(R.id.btn_recycles_minus)
        val btnPlus  = dialogView.findViewById<android.widget.Button>(R.id.btn_recycles_plus)
        val countText = dialogView.findViewById<TextView>(R.id.text_recycles_count)
        val unlimitedSwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_recycles_unlimited)
        val countRow = dialogView.findViewById<android.view.View>(R.id.recycles_count_row)

        var currentCount = recycleCount.coerceAtLeast(1)

        fun updateCountDisplay() {
            countText.text = currentCount.toString()
        }

        fun updateEnabled() {
            val limited = !unlimitedSwitch.isChecked
            countRow.alpha = if (limited) 1f else 0.35f
            btnMinus.isEnabled = limited
            btnPlus.isEnabled = limited
        }

        unlimitedSwitch.isChecked = isInfinite
        updateCountDisplay()
        updateEnabled()

        btnMinus.setOnClickListener {
            if (currentCount > 1) {
                currentCount--
                updateCountDisplay()
            }
        }
        btnPlus.setOnClickListener {
            if (currentCount < 99) {
                currentCount++
                updateCountDisplay()
            }
        }
        unlimitedSwitch.setOnCheckedChangeListener { _, _ -> updateEnabled() }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_menu_waste_recycles)
            .setView(dialogView)
            .setPositiveButton(R.string.nickname_dialog_save) { _, _ ->
                val saveInfinite = unlimitedSwitch.isChecked
                val saveCount = currentCount
                lifecycleScope.launch {
                    val currentSettings = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        currentSettings.copy(
                            infiniteRecycles = saveInfinite,
                            recycleCount = saveCount
                        )
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showBoardLayoutDialog(currentLayout: String) {
        val boardLayoutOptions = arrayOf(
            getString(R.string.settings_board_layout_right),
            getString(R.string.settings_board_layout_left)
        )
        val boardLayoutKeys = listOf("right_hand", "left_hand")
        val checkedItem = boardLayoutKeys.indexOf(currentLayout).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_menu_board_layout)
            .setSingleChoiceItems(boardLayoutOptions, checkedItem) { dialog, which ->
                val selectedLayout = boardLayoutKeys[which]
                lifecycleScope.launch {
                    val currentSettings = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        currentSettings.copy(boardLayout = selectedLayout)
                    )
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showScoreMethodDialog(currentMethod: String) {
        val scoreMethodKeys = listOf("windows", "vegas", "vegas_cumulative", "completion")
        val scoreMethodOptions = arrayOf(
            getString(R.string.score_method_windows),
            getString(R.string.score_method_vegas),
            getString(R.string.score_method_vegas_cumulative),
            getString(R.string.score_method_completion)
        )
        val checkedItem = scoreMethodKeys.indexOf(currentMethod).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_menu_score_method)
            .setSingleChoiceItems(scoreMethodOptions, checkedItem) { dialog, which ->
                val selectedMethod = scoreMethodKeys[which]
                lifecycleScope.launch {
                    val currentSettings = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        currentSettings.copy(scoreMethod = selectedMethod)
                    )
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEnableDisableDialog(
        title: String,
        enabled: Boolean,
        onSelection: (Boolean) -> Unit
    ) {
        val options = arrayOf(
            getString(R.string.setting_state_enabled),
            getString(R.string.setting_state_disabled)
        )
        val checkedItem = if (enabled) 0 else 1

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                onSelection(which == 0)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openPrivacyPolicyWebsite(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            Toast.makeText(this, R.string.privacy_policy_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTermsOfServiceWebsite(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            Toast.makeText(this, R.string.terms_of_service_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun readRawResourceText(@RawRes resId: Int): String {
        return resources.openRawResource(resId).bufferedReader().use { it.readText() }
    }

    private fun openPlayStoreListing() {
        val appPackageName = packageName
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        } catch (_: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }

    override fun onGameMenuOpenSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onGameMenuExitApp() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finishAffinity()
    }

    override fun onGameMenuExpandStateChanged(state: GameMenuBottomSheetFragment.ExpandState) {
        gameMenuExpandState = state
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveFullscreen()
        // Player has returned – restart the hint inactivity countdown from scratch.
        viewModel.resumeHintTimerAfterNonPlayerActivity()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveFullscreen()
    }

    override fun onPause() {
        super.onPause()
        setMagicWandSelectionMode(false)
        // Avoid recording losses on transient pauses (ads, dialogs, rotation).
        viewModel.saveGame()
        // Pause hints while the activity is not in foreground (ad overlay, etc.).
        viewModel.pauseHintTimerForNonPlayerActivity()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            viewModel.stopGame()
        }
    }

    override fun onDestroy() {
        moveSoundPool?.release()
        moveSoundPool = null
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyImmersiveFullscreen()
        applyResponsiveControlSizing()
        applyAutoWinPopupRatios()
        applyAutoDailyPopupRatios()
        applyAutoUnlockPopupRatios()
        if (testerStarburstAutoLayoutEnabled) {
            applyAutoStarburstProfile()
            refreshActiveStarburstDebugAndMotion()
        }
        applyLockedPileAdIconDevConfigToBoard()
    }

    private fun onLockedTableauUnlockRequested() {
        if (viewModel.game.value.extraTableauUnlocked) return
        viewModel.pauseHintTimerForNonPlayerActivity()
        val shown = adManager.showRewardedAd(
            onFinished = { rewardEarned ->
                lifecycleScope.launch {
                    if (rewardEarned) {
                        viewModel.unlockExtraTableauPile()
                    } else {
                        Toast.makeText(this@GameActivity, R.string.help_unlock_ad_not_ready, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        if (!shown) {
            adManager.loadRewardedAd()
            Toast.makeText(this, R.string.help_unlock_ad_not_ready, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyLockedPileAdIconDevConfigToBoard() {
        binding.gameBoardView.setLockedPileAdIconTuning(
            portraitOffsetX = devLockedPileAdOffsetXPortraitPxState,
            portraitOffsetY = devLockedPileAdOffsetYPortraitPxState,
            portraitScaleX = devLockedPileAdScaleXPortraitState,
            portraitScaleY = devLockedPileAdScaleYPortraitState,
            landscapeOffsetX = devLockedPileAdOffsetXLandscapePxState,
            landscapeOffsetY = devLockedPileAdOffsetYLandscapePxState,
            landscapeScaleX = devLockedPileAdScaleXLandscapeState,
            landscapeScaleY = devLockedPileAdScaleYLandscapeState
        )
    }

    private fun applyImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun onHelpControlClicked(control: HelpControlAction, targetView: View?, action: () -> Unit) {
        if (helpControlFlowInProgress) return

        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val unlockExpiry = settingsManager.getHelpControlUnlockExpiry(control.storageKey)
            if (unlockExpiry > now) {
                action()
                return@launch
            }

            if (ticketTotal > 0) {
                when (control) {
                    HelpControlAction.RESTART -> {
                        // Restart has confirm/cancel semantics, so consume only on positive confirm.
                        couponPendingOnRestartConfirm = true
                        pendingCouponTargetView = targetView
                        action()
                        return@launch
                    }

                    else -> {
                        animateAndConsumeHelpCoupon(targetView)
                        action()
                        return@launch
                    }
                }
            }

            showNoCouponsDialog(control, action)
        }
    }

    private suspend fun consumeHelpCoupon() {
        ticketTotal = (ticketTotal - 1).coerceAtLeast(0)
        renderTicketHud(ticketTotal)
        settingsManager.setTotalTickets(ticketTotal)
    }

    private suspend fun animateAndConsumeHelpCoupon(targetView: View?) {
        val boardView = binding.gameBoardView
        val boardLocation = IntArray(2)
        boardView.getLocationOnScreen(boardLocation)

        val sourceRect = clampRectToBoardBounds(
            viewRectInBoardSpace(binding.ivTicketIcon, boardLocation),
            boardView
        )
        val targetRect = targetView?.let {
            clampRectToBoardBounds(viewRectInBoardSpace(it, boardLocation), boardView)
        }
            ?: android.graphics.RectF(
                boardView.width * 0.5f - sourceRect.width() * 0.5f,
                boardView.height * 0.5f - sourceRect.height() * 0.5f,
                boardView.width * 0.5f + sourceRect.width() * 0.5f,
                boardView.height * 0.5f + sourceRect.height() * 0.5f
            )

        boardView.scheduleCouponAnimation(sourceRect, targetRect)

        // Keep deduction synced with full coupon animation runtime, including midpoint pause.
        kotlinx.coroutines.delay(CouponFlightAnimator.TOTAL_RUNTIME_MS + 40L)
        consumeHelpCoupon()
    }

    private fun viewRectInBoardSpace(
        view: View,
        boardLocationOnScreen: IntArray
    ): android.graphics.RectF {
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val left = (viewLocation[0] - boardLocationOnScreen[0]).toFloat()
        val top = (viewLocation[1] - boardLocationOnScreen[1]).toFloat()
        return android.graphics.RectF(
            left,
            top,
            left + view.width,
            top + view.height
        )
    }

    private fun clampRectToBoardBounds(
        rect: android.graphics.RectF,
        boardView: View,
        insetPx: Float = 4f
    ): android.graphics.RectF {
        val boardW = boardView.width.toFloat().coerceAtLeast(1f)
        val boardH = boardView.height.toFloat().coerceAtLeast(1f)

        val maxHalfW = (boardW * 0.5f - insetPx).coerceAtLeast(1f)
        val maxHalfH = (boardH * 0.5f - insetPx).coerceAtLeast(1f)
        val halfW = (rect.width() * 0.5f).coerceAtLeast(1f).coerceAtMost(maxHalfW)
        val halfH = (rect.height() * 0.5f).coerceAtLeast(1f).coerceAtMost(maxHalfH)

        val minX = (halfW + insetPx).coerceAtMost(boardW - insetPx)
        val maxX = (boardW - halfW - insetPx).coerceAtLeast(insetPx)
        val minY = (halfH + insetPx).coerceAtMost(boardH - insetPx)
        val maxY = (boardH - halfH - insetPx).coerceAtLeast(insetPx)

        val centerX = rect.centerX().coerceIn(minX, maxX)
        val centerY = rect.centerY().coerceIn(minY, maxY)

        return android.graphics.RectF(
            centerX - halfW,
            centerY - halfH,
            centerX + halfW,
            centerY + halfH
        )
    }

    private fun showNoCouponsDialog(control: HelpControlAction, action: () -> Unit) {
        val unlockHours = if (isPremiumAccount) 10 else 6
        val dialogView = layoutInflater.inflate(R.layout.dialog_help_coupon_unlock, null)
        val descriptionView = dialogView.findViewById<TextView>(R.id.tv_help_unlock_description)
        val adButton = dialogView.findViewById<AppCompatImageButton>(R.id.btn_unlock_with_ad)
        val cancelButton = dialogView.findViewById<AppCompatImageButton>(R.id.btn_unlock_cancel)

        descriptionView.text = getString(R.string.help_unlock_ad_only_description_format, unlockHours)

        applyUnlockPopupDevConfig(dialogView)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        adButton.setOnClickListener {
            if (helpControlFlowInProgress) return@setOnClickListener
            helpControlFlowInProgress = true
            val shown = adManager.showRewardedAd(
                onFinished = { rewardEarned ->
                    lifecycleScope.launch {
                        helpControlFlowInProgress = false
                        if (rewardEarned) {
                            unlockHelpControl(control, unlockHours)
                            dialog.dismiss()
                            action()
                        } else {
                            Toast.makeText(this@GameActivity, R.string.help_unlock_ad_not_ready, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            if (!shown) {
                helpControlFlowInProgress = false
                adManager.loadRewardedAd()
                Toast.makeText(this, R.string.help_unlock_ad_not_ready, Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val widthPercent = if (isLandscape) {
                devUnlockDialogWidthPercentLandscapeState
            } else {
                devUnlockDialogWidthPercentPortraitState
            }
            val widthPx = (resources.displayMetrics.widthPixels * widthPercent)
                .toInt()
                .coerceAtLeast(1)
            setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun applyUnlockPopupDevConfig(dialogView: View) {
        dialogView.findViewById<ImageView>(R.id.iv_unlock_popup_bg)?.let { bg ->
            bg.scaleX = devUnlockFrameScaleXState
            bg.scaleY = devUnlockFrameScaleYState
        }
        dialogView.findViewById<TextView>(R.id.tv_help_unlock_description)?.let { tv ->
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, devUnlockDescTextSizeSpState)
            tv.translationX = dpToPxFloatSigned(devUnlockDescOffsetXDpState)
            tv.translationY = dpToPxFloatSigned(devUnlockDescOffsetYDpState)
        }
        dialogView.findViewById<AppCompatImageButton>(R.id.btn_unlock_with_ad)?.let { btn ->
            btn.scaleX = devUnlockAdBtnScaleXState
            btn.scaleY = devUnlockAdBtnScaleYState
            btn.translationX = dpToPxFloatSigned(devUnlockAdBtnOffsetXDpState)
            btn.translationY = dpToPxFloatSigned(devUnlockAdBtnOffsetYDpState)
        }
        dialogView.findViewById<AppCompatImageButton>(R.id.btn_unlock_cancel)?.let { btn ->
            btn.scaleX = devUnlockCancelBtnScaleXState
            btn.scaleY = devUnlockCancelBtnScaleYState
            btn.translationX = dpToPxFloatSigned(devUnlockCancelBtnOffsetXDpState)
            btn.translationY = dpToPxFloatSigned(devUnlockCancelBtnOffsetYDpState)
        }
    }

    private suspend fun unlockHelpControl(control: HelpControlAction, hours: Int) {
        val durationMillis = hours.coerceAtLeast(1) * 60L * 60L * 1000L
        val expiry = System.currentTimeMillis() + durationMillis
        settingsManager.setHelpControlUnlockExpiry(control.storageKey, expiry)
    }

    private fun handleUndoClick() {
        if (viewModel.undo()) playCardClickMoveSound()
    }

    private fun handleRedoClick() {
        if (viewModel.redo()) playCardClickMoveSound()
    }

    private fun handleRestartClick() {
        showRestartDialog()
    }

    private fun applyResponsiveControlSizing() {
        val config = resources.configuration
        val widthDp = config.screenWidthDp.toFloat()
        val heightDp = config.screenHeightDp.toFloat()
        val isLandscapeNow = config.orientation == Configuration.ORIENTATION_LANDSCAPE

        val factors = UiScaleUtil.calculateBaselineScaleFactors(widthDp, heightDp)
        val widthScale = factors.horizontal.coerceIn(0.56f, 1.70f)
        val heightScale = factors.vertical.coerceIn(0.56f, 1.70f)
        val textScale = factors.text.coerceIn(0.64f, 1.35f)

        // Portrait button widths already have a 1.75× boost to compensate for the narrow
        // phone screen.  Applying the extreme-aspect compression on top (which the full
        // widthScale carries) would double-penalise the width.  Strip the per-axis factor
        // out so portrait button widths track only the baseline (screen vs. baseline tablet)
        // ratio, not the additional phone-shape compression.
        val portraitButtonWidthScale = if (!isLandscapeNow && factors.isExtremeAspect && factors.horizontalFactor != 0f)
            (factors.horizontal / factors.horizontalFactor).coerceIn(0.56f, 1.70f)
        else
            widthScale

        applyTopHudSizing(isLandscapeNow, widthScale, heightScale, textScale)
        applyBottomControlsSizing(isLandscapeNow, widthScale, heightScale, textScale, portraitButtonWidthScale)
    }

    private fun applyTopHudSizing(
        isLandscape: Boolean,
        widthScale: Float,
        heightScale: Float,
        textScale: Float
    ) {
        val panel = findViewById<LinearLayout>(R.id.game_info_panel) ?: return

        panel.setPaddingRelative(
            dpToPx(16f * widthScale),
            dpToPx(8f * heightScale),
            dpToPx(16f * widthScale),
            dpToPx(8f * heightScale)
        )

        val statTextSp = (if (isLandscape) 14f else 16f) * textScale
        binding.tvScore.setTextSize(TypedValue.COMPLEX_UNIT_SP, statTextSp)
        binding.tvMoves.setTextSize(TypedValue.COMPLEX_UNIT_SP, statTextSp)
        binding.tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, statTextSp)

        val baseGemBagDp = if (isLandscape) 22f else 24f
        // gem bag reduced to ~0.75 of old size (user tuned: 80dp → 60dp in portrait XML)
        val gemBagBaseDp = baseGemBagDp * 2.5f
        val gemCountBaseSp = if (isLandscape) 10f else 11f
        val gemMinWidthBaseDp = if (isLandscape) 26f else 28f
        val wandContainerBaseDp = 56f * 1.6f
        // wand icon height reduced ~0.86 (user tuned: 58dp → 50dp in portrait XML)
        val wandIconBaseDp = 36f * 1.38f
        val gemSizePx = dpToPx(gemBagBaseDp * textScale)
        val ticketSizePx = dpToPx((baseGemBagDp * 3.7f) * textScale)
        val wandContainerPx = dpToPx(wandContainerBaseDp * textScale)
        val wandIconPx = dpToPx(wandIconBaseDp * textScale)

        resizeFrame(binding.ivGemBag, gemSizePx, gemSizePx)
        resizeFrame(binding.ivTicketIcon, ticketSizePx, ticketSizePx)
        resizeFrame(binding.magicWandContainer, wandContainerPx, wandContainerPx)
        resizeFrame(binding.ivMagicWand, wandIconPx, wandIconPx)
        binding.tvGemCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, gemCountBaseSp * textScale)
        binding.tvTicketCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, gemCountBaseSp * textScale)
        binding.tvGemCount.minWidth = dpToPx(gemMinWidthBaseDp * widthScale)
        binding.tvTicketCount.minWidth = dpToPx(gemMinWidthBaseDp * widthScale)
        binding.tvGemCount.setPaddingRelative(
            dpToPx(6f * widthScale),
            dpToPx(1f * heightScale),
            dpToPx(6f * widthScale),
            dpToPx(1f * heightScale)
        )
        binding.tvTicketCount.setPaddingRelative(
            dpToPx(6f * widthScale),
            dpToPx(1f * heightScale),
            dpToPx(6f * widthScale),
            dpToPx(1f * heightScale)
        )
        // Allow count ovals to move outside their container bounds when nudged upward.
        binding.boardCurrencyHudContainer.clipChildren = false
        binding.boardCurrencyHudContainer.clipToPadding = false
        binding.gemsContainer.clipChildren = false
        binding.gemsContainer.clipToPadding = false

        // Gem count number: use signed conversion. dpToPx() clamps negatives to +1,
        // so it cannot be used for upward offsets.
        val gemCountTranslationY = if (isLandscape) {
            dpToPxFloatSigned(-14f * heightScale)
        } else {
            dpToPxFloatSigned(-10f * heightScale)
        }
        when (val lp = binding.tvGemCount.layoutParams) {
            is LinearLayout.LayoutParams -> {
                lp.topMargin = 0
                lp.marginEnd = dpToPx(6f * widthScale)
                binding.tvGemCount.layoutParams = lp
            }
            is FrameLayout.LayoutParams -> {
                lp.gravity = Gravity.END or Gravity.TOP
                lp.topMargin = 0
                lp.marginEnd = 0
                binding.tvGemCount.layoutParams = lp
            }
        }
        binding.tvGemCount.translationY = gemCountTranslationY
        (binding.magicWandContainer.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            lp.marginStart = 0
            lp.marginEnd = 0
            binding.magicWandContainer.layoutParams = lp
        }
        // Wand container: landscape moves further up, portrait moves up more than ticket
        val wandTranslationY = when {
            isLandscape -> dpToPx(-8f * heightScale).toFloat()   // landscape: move up
            else        -> dpToPx(-10f * heightScale).toFloat()  // portrait: move up more than ticket
        }
        binding.magicWandContainer.translationY = wandTranslationY

        // Wand count number: move upward in both orientations (same amount as gem count)
        val wandCountTopOffset = if (isLandscape) dpToPx(-10f * heightScale) else dpToPx(-8f * heightScale)
        (binding.tvMagicWandCount.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
            lp.gravity = Gravity.END or Gravity.TOP
            lp.topMargin = wandCountTopOffset
            binding.tvMagicWandCount.layoutParams = lp
        }

        // Ticket count number: unchanged (user confirmed fine)
        when (val lp = binding.tvTicketCount.layoutParams) {
            is LinearLayout.LayoutParams -> {
                lp.topMargin = 0
                lp.marginEnd = dpToPx(6f * widthScale)
                binding.tvTicketCount.layoutParams = lp
            }
            is FrameLayout.LayoutParams -> {
                lp.gravity = Gravity.END or Gravity.TOP
                lp.topMargin = 0
                lp.marginEnd = 0
                binding.tvTicketCount.layoutParams = lp
            }
        }
        when (val lp = binding.ticketsContainer.layoutParams) {
            is LinearLayout.LayoutParams -> {
                lp.marginStart = 0
                lp.marginEnd = dpToPx(8f * widthScale)
                binding.ticketsContainer.layoutParams = lp
            }
            is ConstraintLayout.LayoutParams -> {
                lp.marginStart = 0
                lp.marginEnd = dpToPx(8f * widthScale)
                lp.bottomMargin = dpToPx(10f * heightScale)
                binding.ticketsContainer.layoutParams = lp
            }
        }
        // Ticket image: landscape fine as-is; portrait move up a bit
        binding.ticketsContainer.translationY = 0f
        val ticketImageTranslationY = when {
            isLandscape -> dpToPx(20f * heightScale).toFloat()  // landscape: unchanged
            else        -> dpToPx(10f * heightScale).toFloat()  // portrait: move up (~10dp less than before)
        }
        binding.ivTicketIcon.translationY = ticketImageTranslationY
        (binding.gemsContainer.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            lp.marginStart = 0
            lp.marginEnd = 0
            binding.gemsContainer.layoutParams = lp
        }
        findViewById<LinearLayout>(R.id.board_currency_hud_container)?.let { boardCurrencyHud ->
            (boardCurrencyHud.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
                lp.marginEnd = dpToPx(8f * widthScale)
                lp.bottomMargin = dpToPx(10f * heightScale)
                boardCurrencyHud.layoutParams = lp
            }
        }
    }

    private fun applyBottomControlsSizing(
        isLandscape: Boolean,
        widthScale: Float,
        heightScale: Float,
        textScale: Float,
        portraitButtonWidthScale: Float = widthScale
    ) {
        val controlPanel = findViewById<LinearLayout>(R.id.control_buttons_layout)
        val moveGroup = findViewById<FrameLayout>(R.id.move_controls_group)
        val btnHint = findViewById<Button?>(R.id.btn_hint)
        val btnRestart = findViewById<Button?>(R.id.btn_restart)
        val btnAuto = findViewById<Button?>(R.id.btn_auto_move)
        val undoMain = findViewById<ImageView?>(R.id.undo_main)
        val redoMain = findViewById<ImageView?>(R.id.redo_main)

        val controlTextSp = 8f * textScale
        // Both orientations use heightScale:
        //   Portrait  → heightScale carries the expansion factor (long axis) → buttons grow taller ✓
        //   Landscape → heightScale carries the compression factor (short axis) → buttons shrink ✓
        val controlHeightScale = heightScale
        val portraitWidthBoost = 1.75f

        if (!isLandscape) {
            // Portrait uses fixed button widths; portraitButtonWidthScale strips the extreme-aspect
            // compression so the existing portraitWidthBoost is not double-penalised.
            setButtonSizeDp(binding.btnNewGame, 50f * portraitWidthBoost * portraitButtonWidthScale, 80f * controlHeightScale)
            setButtonSizeDp(binding.btnStats, 56f * portraitWidthBoost * portraitButtonWidthScale, 80f * controlHeightScale)
            setButtonSizeDp(btnHint, 50f * portraitWidthBoost * portraitButtonWidthScale, 72f * controlHeightScale)
            setButtonSizeDp(btnRestart, 62f * portraitWidthBoost * portraitButtonWidthScale, 72f * controlHeightScale)
            setButtonSizeDp(btnAuto, 52f * portraitWidthBoost * portraitButtonWidthScale, 72f * controlHeightScale)
            (controlPanel?.layoutParams)?.let { lp ->
                lp.height = dpToPx(80f * controlHeightScale)
                controlPanel.layoutParams = lp
            }
        } else {
            controlPanel?.setPaddingRelative(
                dpToPx(8f * widthScale),
                controlPanel.paddingTop,
                dpToPx(8f * widthScale),
                controlPanel.paddingBottom
            )
        }

        applyButtonScale(binding.btnNewGame, controlTextSp, textScale)
        applyButtonScale(binding.btnStats, controlTextSp, textScale)
        btnHint?.let { applyButtonScale(it, controlTextSp, textScale) }
        btnRestart?.let { applyButtonScale(it, controlTextSp, textScale) }
        btnAuto?.let { applyButtonScale(it, controlTextSp, textScale) }

        // Portrait undo/redo widths also use the baseline-only scale (no extra compression).
        val undoWidthScale = if (isLandscape) widthScale else portraitButtonWidthScale
        val undoFrameW = if (isLandscape) 40f else 30f * portraitWidthBoost
        val undoFrameH = if (isLandscape) 40f else 72f
        val undoImgW = if (isLandscape) 40f else 30f * portraitWidthBoost
        val undoImgH = if (isLandscape) 40f else 66f

        resizeFrame(binding.btnUndo, dpToPx(undoFrameW * undoWidthScale), dpToPx(undoFrameH * controlHeightScale))
        resizeFrame(binding.btnRedo, dpToPx(undoFrameW * undoWidthScale), dpToPx(undoFrameH * controlHeightScale))
        undoMain?.let { resizeFrame(it, dpToPx(undoImgW * undoWidthScale), dpToPx(undoImgH * controlHeightScale)) }
        redoMain?.let { resizeFrame(it, dpToPx(undoImgW * undoWidthScale), dpToPx(undoImgH * controlHeightScale)) }

        moveGroup?.setPaddingRelative(
            dpToPx((if (isLandscape) 4f else 3f) * widthScale),
            dpToPx((if (isLandscape) 4f else 2f) * heightScale),
            dpToPx((if (isLandscape) 4f else 3f) * widthScale),
            dpToPx((if (isLandscape) 4f else 2f) * heightScale)
        )

        binding.btnStats.iconSize = dpToPx(48f * textScale)
        binding.btnStats.iconPadding = dpToPx(if (isLandscape) -12f * textScale else -4f * textScale)
    }

    private fun applyButtonScale(button: Button, textSp: Float, scale: Float) {
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp)
        button.minWidth = dpToPx(76f * scale)
        val horizontal = dpToPx(12f * scale)
        val vertical = dpToPx(6f * scale)
        button.setPaddingRelative(horizontal, vertical, horizontal, vertical)
    }

    private fun setButtonSizeDp(button: Button?, widthDp: Float, heightDp: Float) {
        button ?: return
        val lp = button.layoutParams ?: return
        lp.width = dpToPx(widthDp)
        lp.height = dpToPx(heightDp)
        button.layoutParams = lp
    }

    private fun resizeFrame(view: View, widthPx: Int, heightPx: Int) {
        val lp = view.layoutParams ?: return
        lp.width = widthPx
        lp.height = heightPx
        view.layoutParams = lp
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    private fun dpToPxFloatSigned(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    private fun showWinCelebrationThenDialog() {
        if (isFinishing || isDestroyed || winDialogShowing) return

        if (binding.gameBoardView.isCardAnimationActive()) {
            if (!pendingWinUiAfterAnimation) {
                pendingWinUiAfterAnimation = true
                waitForBoardAnimationThenShowWinUi()
            }
            return
        }

        pendingWinUiAfterAnimation = false

        // Win videos are removed. Keep the win flow one-shot while status remains WON.
        if (winCelebrationPlayed) return
        winCelebrationPlayed = true
        showGameEndDialog(true)
    }

    private fun waitForBoardAnimationThenShowWinUi() {
        binding.gameBoardView.postOnAnimation {
            if (isFinishing || isDestroyed) {
                pendingWinUiAfterAnimation = false
                return@postOnAnimation
            }

            if (viewModel.game.value.status != GameStatus.WON) {
                pendingWinUiAfterAnimation = false
                return@postOnAnimation
            }

            if (binding.gameBoardView.isCardAnimationActive()) {
                waitForBoardAnimationThenShowWinUi()
            } else {
                pendingWinUiAfterAnimation = false
                showWinCelebrationThenDialog()
            }
        }
    }

    private fun playWinVideo(onFinished: () -> Unit): Boolean {
        // Win videos have been removed - this method is now a no-op
        return false
    }

    private fun stopWinVideoPlayback() {
        // Win videos have been removed - this method is now a no-op
    }

    /**
     * Extension function for weighted random selection.
     * Takes a list of (item, weight) pairs and returns a randomly selected item
     * based on the weights.
     */
    private fun <T> List<Pair<T, Int>>.weightedRandom(): T {
        val totalWeight = this.sumOf { it.second }
        var random = (0 until totalWeight).random()

        for ((item, weight) in this) {
            if (random < weight) {
                return item
            }
            random -= weight
        }

        return this.first().first // Fallback (should never reach here)
    }
}
