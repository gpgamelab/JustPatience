package com.gpgamelab.justpatience.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED
import android.graphics.Bitmap
import android.graphics.Canvas
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
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
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
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.gpgamelab.justpatience.BuildConfig
import com.gpgamelab.justpatience.ads.AdManager
import com.gpgamelab.justpatience.assets.AndroidAssetResolver
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.databinding.ActivityGameBinding
import com.gpgamelab.justpatience.model.GameStatus
import com.gpgamelab.justpatience.data.GameStatsManager
import com.gpgamelab.justpatience.util.BaselineResolutionScaleUtil
import com.gpgamelab.justpatience.util.DeviceAspectCategory
import com.gpgamelab.justpatience.util.UiScaleUtil
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.random.Random
import kotlin.math.roundToInt

class GameActivity : AppCompatActivity(), GameMenuBottomSheetFragment.Host, TesterMenuDialogFragment.Host, DevelopMenuDialogFragment.Host {

    private data class PortraitAspectPileOffsets(
        val foundationOffsetX: Float = 0f,
        val foundationOffsetY: Float = 0f,
        val drawWasteOffsetX: Float = 0f,
        val drawWasteOffsetY: Float = 0f
    )

    private data class LayoutScopedDevAdjusters(
        val landscapePileOverallOffsetX: Float,
        val landscapePileOverallOffsetY: Float,
        val landscapePileFoundationOffsetX: Float,
        val landscapePileFoundationOffsetY: Float,
        val landscapePileDrawWasteOffsetX: Float,
        val landscapePileDrawWasteOffsetY: Float,
        val landscapePileStockOffsetX: Float,
        val landscapePileStockOffsetY: Float,
        val landscapePileWasteOffsetX: Float,
        val landscapePileWasteOffsetY: Float,
        val landscapePileTableauOffsetX: Float,
        val landscapePileTableauOffsetY: Float,
        val portraitPileOverallOffsetX: Float,
        val portraitPileOverallOffsetY: Float,
        val portraitAspectOffsetsSlimCompact: PortraitAspectPileOffsets,
        val portraitAspectOffsetsSlim: PortraitAspectPileOffsets,
        val portraitAspectOffsetsClassic: PortraitAspectPileOffsets,
        val portraitAspectOffsetsBroad: PortraitAspectPileOffsets,
        val portraitAspectOffsetsSquare: PortraitAspectPileOffsets,
        val portraitPileStockOffsetX: Float,
        val portraitPileStockOffsetY: Float,
        val portraitPileWasteOffsetX: Float,
        val portraitPileWasteOffsetY: Float,
        val portraitPileTableauOffsetX: Float,
        val portraitPileTableauOffsetY: Float,
        val landscapeBannerSmallOffsetX: Float,
        val landscapeBannerSmallOffsetY: Float,
        val landscapeBannerMediumOffsetX: Float,
        val landscapeBannerMediumOffsetY: Float,
        val landscapeBannerLargeOffsetX: Float,
        val landscapeBannerLargeOffsetY: Float,
        val scoreboardOffsetX: Float,
        val scoreboardOffsetY: Float,
        val gemRewardOffsetX: Float,
        val gemRewardOffsetY: Float,
        val ticketRewardOffsetX: Float,
        val ticketRewardOffsetY: Float
    )

    // Single source of hardcoded defaults for each hand-layout profile.
    // Edit these two blocks when you want fixed classic vs mirrored values in code.
    private val defaultClassicLayoutDevAdjusters = LayoutScopedDevAdjusters(
        landscapePileOverallOffsetX = -5f,
        landscapePileOverallOffsetY = -100f,
        landscapePileFoundationOffsetX = 0f,
        landscapePileFoundationOffsetY = 0f,
        landscapePileDrawWasteOffsetX = 0f,
        landscapePileDrawWasteOffsetY = 0f,
        landscapePileStockOffsetX = 0f,
        landscapePileStockOffsetY = 0f,
        landscapePileWasteOffsetX = 0f,
        landscapePileWasteOffsetY = 0f,
        landscapePileTableauOffsetX = 0f,
        landscapePileTableauOffsetY = 0f,
        portraitPileOverallOffsetX = 0f,
        portraitPileOverallOffsetY = 0f,
        portraitAspectOffsetsSlimCompact = PortraitAspectPileOffsets(
            foundationOffsetX = -25f
        ),
        portraitAspectOffsetsSlim = PortraitAspectPileOffsets(),
        portraitAspectOffsetsClassic = PortraitAspectPileOffsets(),
        portraitAspectOffsetsBroad = PortraitAspectPileOffsets(),
        portraitAspectOffsetsSquare = PortraitAspectPileOffsets(),
        portraitPileStockOffsetX = 0f,
        portraitPileStockOffsetY = 0f,
        portraitPileWasteOffsetX = 0f,
        portraitPileWasteOffsetY = 0f,
        portraitPileTableauOffsetX = 0f,
        portraitPileTableauOffsetY = 0f,
        landscapeBannerSmallOffsetX = 0f,
        landscapeBannerSmallOffsetY = 0f,
        landscapeBannerMediumOffsetX = 0f,
        landscapeBannerMediumOffsetY = 0f,
        landscapeBannerLargeOffsetX = -300f,
        landscapeBannerLargeOffsetY = 0f,
        scoreboardOffsetX = 0f,
        scoreboardOffsetY = 0f,
        gemRewardOffsetX = -10f,
        gemRewardOffsetY = 25f,
        ticketRewardOffsetX = -10f,
        ticketRewardOffsetY = 35f
    )

    private val defaultMirroredLayoutDevAdjusters = defaultClassicLayoutDevAdjusters.copy(
        landscapePileOverallOffsetX = 90f,
        landscapeBannerLargeOffsetX = 300f,
        portraitAspectOffsetsSlimCompact = PortraitAspectPileOffsets(
            foundationOffsetX = 25f,
            drawWasteOffsetX = 75f
        ),
        portraitAspectOffsetsClassic = PortraitAspectPileOffsets(
            drawWasteOffsetX = 45f
        ),
        portraitAspectOffsetsBroad = PortraitAspectPileOffsets(
            drawWasteOffsetX = 45f
        )
    )

    private enum class LandscapeBannerTier {
        SMALL,
        MEDIUM,
        LARGE
    }

    // Lets narrow SLIM phones (e.g. 720px portrait board) use a separate tuning set.
    private val portraitSlimCompactMaxBoardWidthPx = 800

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
    private var devLandscapePileOverallOffsetXDpState: Float = 0f
    private var devLandscapePileOverallOffsetYDpState: Float = -100f
    private var devLandscapePileFoundationOffsetXDpState: Float = 0f
    private var devLandscapePileFoundationOffsetYDpState: Float = 0f
    private var devLandscapePileDrawWasteOffsetXDpState: Float = 0f
    private var devLandscapePileDrawWasteOffsetYDpState: Float = 0f
    private var devLandscapePileStockOffsetXDpState: Float = 0f
    private var devLandscapePileStockOffsetYDpState: Float = 0f
    private var devLandscapePileWasteOffsetXDpState: Float = 0f
    private var devLandscapePileWasteOffsetYDpState: Float = 0f
    private var devLandscapePileTableauOffsetXDpState: Float = 0f
    private var devLandscapePileTableauOffsetYDpState: Float = 0f
    private var devPortraitPileOverallOffsetXDpState: Float = 0f
    private var devPortraitPileOverallOffsetYDpState: Float = -70f
    private var devPortraitFoundationOffsetXSlimCompactDpState: Float = 0f
    private var devPortraitFoundationOffsetYSlimCompactDpState: Float = 0f
    private var devPortraitDrawWasteOffsetXSlimCompactDpState:  Float = 0f
    private var devPortraitDrawWasteOffsetYSlimCompactDpState:  Float = 0f
    private var devPortraitFoundationOffsetXSlimDpState:    Float = 0f
    private var devPortraitFoundationOffsetYSlimDpState:    Float = 0f
    private var devPortraitDrawWasteOffsetXSlimDpState:     Float = 0f
    private var devPortraitDrawWasteOffsetYSlimDpState:     Float = 0f
    private var devPortraitFoundationOffsetXClassicDpState: Float = 0f
    private var devPortraitFoundationOffsetYClassicDpState: Float = 0f
    private var devPortraitDrawWasteOffsetXClassicDpState:  Float = 0f
    private var devPortraitDrawWasteOffsetYClassicDpState:  Float = 0f
    private var devPortraitFoundationOffsetXBroadDpState:   Float = 0f
    private var devPortraitFoundationOffsetYBroadDpState:   Float = 0f
    private var devPortraitDrawWasteOffsetXBroadDpState:    Float = 0f
    private var devPortraitDrawWasteOffsetYBroadDpState:    Float = 0f
    private var devPortraitFoundationOffsetXSquareDpState:  Float = 0f
    private var devPortraitFoundationOffsetYSquareDpState:  Float = 0f
    private var devPortraitDrawWasteOffsetXSquareDpState:   Float = 0f
    private var devPortraitDrawWasteOffsetYSquareDpState:   Float = 0f
    private var devPortraitPileStockOffsetXDpState: Float = 0f
    private var devPortraitPileStockOffsetYDpState: Float = 0f
    private var devPortraitPileWasteOffsetXDpState: Float = 0f
    private var devPortraitPileWasteOffsetYDpState: Float = 0f
    private var devPortraitPileTableauOffsetXDpState: Float = 0f
    private var devPortraitPileTableauOffsetYDpState: Float = 0f
    private var devLandscapeBannerSmallWidthDpState: Float = 320f
    private var devLandscapeBannerSmallHeightDpState: Float = 60f   // BANNER (50dp) + buffer
    private var devLandscapeBannerMediumWidthDpState: Float = 320f
    private var devLandscapeBannerMediumHeightDpState: Float = 110f  // LARGE_BANNER (100dp) + buffer
    private var devLandscapeBannerLargeWidthDpState: Float = 320f
    private var devLandscapeBannerLargeHeightDpState: Float = 260f  // MEDIUM_RECTANGLE (250dp) + buffer
    private var devSmallDeviceLandscapeBannerOffsetXDpState: Float = 0f
    private var devSmallDeviceLandscapeBannerOffsetYDpState: Float = 0f
    private var devMediumDeviceLandscapeBannerOffsetXDpState: Float = 0f
    private var devMediumDeviceLandscapeBannerOffsetYDpState: Float = 0f
    private var devLargeDeviceLandscapeBannerOffsetXDpState: Float = 0f
    private var devLargeDeviceLandscapeBannerOffsetYDpState: Float = 0f
    private var devScoreboardOffsetXDpState: Float = 0f
    private var devScoreboardOffsetYDpState: Float = 0f
    private var devGemRewardOffsetXDpState: Float = 0f
    private var devGemRewardOffsetYDpState: Float = 0f
    private var devTicketRewardOffsetXDpState: Float = 0f
    private var devTicketRewardOffsetYDpState: Float = 0f

    // Aspect-ratio category Y trims (dp).  Applied as the final boardStartY adjustment.
    // Positive = move piles DOWN, negative = move piles UP.
    private var devAspectPortraitSlimYDpState:    Float = 0f
    private var devAspectPortraitClassicYDpState: Float = 0f
    private var devAspectPortraitBroadYDpState:   Float = 0f
    private var devAspectPortraitSquareYDpState:  Float = 0f
    private var devAspectLandscapeSlimYDpState:    Float = 30f
    private var devAspectLandscapeClassicYDpState: Float = 0f
    private var devAspectLandscapeBroadYDpState:   Float = 0f
    private var devAspectLandscapeSquareYDpState:  Float = 0f

    // Aspect-ratio category X trims (dp).  Applied as the final boardStartX adjustment.
    // Positive = move piles RIGHT, negative = move piles LEFT.
    private var devAspectPortraitSlimXDpState:    Float = 0f
    private var devAspectPortraitClassicXDpState: Float = 0f
    private var devAspectPortraitBroadXDpState:   Float = 0f
    private var devAspectPortraitSquareXDpState:  Float = 0f
    private var devAspectLandscapeSlimXDpState:    Float = 105f
    private var devAspectLandscapeClassicXDpState: Float = 0f
    private var devAspectLandscapeBroadXDpState:   Float = 0f
    private var devAspectLandscapeSquareXDpState:  Float = 0f
    private var classicLayoutDevAdjustersState = defaultClassicLayoutDevAdjusters
    private var mirroredLayoutDevAdjustersState = defaultMirroredLayoutDevAdjusters

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

    private fun snapshotLayoutScopedDevAdjusters(): LayoutScopedDevAdjusters {
        return LayoutScopedDevAdjusters(
            landscapePileOverallOffsetX = devLandscapePileOverallOffsetXDpState,
            landscapePileOverallOffsetY = devLandscapePileOverallOffsetYDpState,
            landscapePileFoundationOffsetX = devLandscapePileFoundationOffsetXDpState,
            landscapePileFoundationOffsetY = devLandscapePileFoundationOffsetYDpState,
            landscapePileDrawWasteOffsetX = devLandscapePileDrawWasteOffsetXDpState,
            landscapePileDrawWasteOffsetY = devLandscapePileDrawWasteOffsetYDpState,
            landscapePileStockOffsetX = devLandscapePileStockOffsetXDpState,
            landscapePileStockOffsetY = devLandscapePileStockOffsetYDpState,
            landscapePileWasteOffsetX = devLandscapePileWasteOffsetXDpState,
            landscapePileWasteOffsetY = devLandscapePileWasteOffsetYDpState,
            landscapePileTableauOffsetX = devLandscapePileTableauOffsetXDpState,
            landscapePileTableauOffsetY = devLandscapePileTableauOffsetYDpState,
            portraitPileOverallOffsetX = devPortraitPileOverallOffsetXDpState,
            portraitPileOverallOffsetY = devPortraitPileOverallOffsetYDpState,
            portraitAspectOffsetsSlimCompact = PortraitAspectPileOffsets(
                foundationOffsetX = devPortraitFoundationOffsetXSlimCompactDpState,
                foundationOffsetY = devPortraitFoundationOffsetYSlimCompactDpState,
                drawWasteOffsetX  = devPortraitDrawWasteOffsetXSlimCompactDpState,
                drawWasteOffsetY  = devPortraitDrawWasteOffsetYSlimCompactDpState
            ),
            portraitAspectOffsetsSlim = PortraitAspectPileOffsets(
                foundationOffsetX = devPortraitFoundationOffsetXSlimDpState,
                foundationOffsetY = devPortraitFoundationOffsetYSlimDpState,
                drawWasteOffsetX  = devPortraitDrawWasteOffsetXSlimDpState,
                drawWasteOffsetY  = devPortraitDrawWasteOffsetYSlimDpState
            ),
            portraitAspectOffsetsClassic = PortraitAspectPileOffsets(
                foundationOffsetX = devPortraitFoundationOffsetXClassicDpState,
                foundationOffsetY = devPortraitFoundationOffsetYClassicDpState,
                drawWasteOffsetX  = devPortraitDrawWasteOffsetXClassicDpState,
                drawWasteOffsetY  = devPortraitDrawWasteOffsetYClassicDpState
            ),
            portraitAspectOffsetsBroad = PortraitAspectPileOffsets(
                foundationOffsetX = devPortraitFoundationOffsetXBroadDpState,
                foundationOffsetY = devPortraitFoundationOffsetYBroadDpState,
                drawWasteOffsetX  = devPortraitDrawWasteOffsetXBroadDpState,
                drawWasteOffsetY  = devPortraitDrawWasteOffsetYBroadDpState
            ),
            portraitAspectOffsetsSquare = PortraitAspectPileOffsets(
                foundationOffsetX = devPortraitFoundationOffsetXSquareDpState,
                foundationOffsetY = devPortraitFoundationOffsetYSquareDpState,
                drawWasteOffsetX  = devPortraitDrawWasteOffsetXSquareDpState,
                drawWasteOffsetY  = devPortraitDrawWasteOffsetYSquareDpState
            ),
            portraitPileStockOffsetX = devPortraitPileStockOffsetXDpState,
            portraitPileStockOffsetY = devPortraitPileStockOffsetYDpState,
            portraitPileWasteOffsetX = devPortraitPileWasteOffsetXDpState,
            portraitPileWasteOffsetY = devPortraitPileWasteOffsetYDpState,
            portraitPileTableauOffsetX = devPortraitPileTableauOffsetXDpState,
            portraitPileTableauOffsetY = devPortraitPileTableauOffsetYDpState,
            landscapeBannerSmallOffsetX = devSmallDeviceLandscapeBannerOffsetXDpState,
            landscapeBannerSmallOffsetY = devSmallDeviceLandscapeBannerOffsetYDpState,
            landscapeBannerMediumOffsetX = devMediumDeviceLandscapeBannerOffsetXDpState,
            landscapeBannerMediumOffsetY = devMediumDeviceLandscapeBannerOffsetYDpState,
            landscapeBannerLargeOffsetX = devLargeDeviceLandscapeBannerOffsetXDpState,
            landscapeBannerLargeOffsetY = devLargeDeviceLandscapeBannerOffsetYDpState,
            scoreboardOffsetX = devScoreboardOffsetXDpState,
            scoreboardOffsetY = devScoreboardOffsetYDpState,
            gemRewardOffsetX = devGemRewardOffsetXDpState,
            gemRewardOffsetY = devGemRewardOffsetYDpState,
            ticketRewardOffsetX = devTicketRewardOffsetXDpState,
            ticketRewardOffsetY = devTicketRewardOffsetYDpState
        )
    }

    private fun applyLayoutScopedDevAdjusters(profile: LayoutScopedDevAdjusters) {
        devLandscapePileOverallOffsetXDpState = profile.landscapePileOverallOffsetX
        devLandscapePileOverallOffsetYDpState = profile.landscapePileOverallOffsetY
        devLandscapePileFoundationOffsetXDpState = profile.landscapePileFoundationOffsetX
        devLandscapePileFoundationOffsetYDpState = profile.landscapePileFoundationOffsetY
        devLandscapePileDrawWasteOffsetXDpState = profile.landscapePileDrawWasteOffsetX
        devLandscapePileDrawWasteOffsetYDpState = profile.landscapePileDrawWasteOffsetY
        devLandscapePileStockOffsetXDpState = profile.landscapePileStockOffsetX
        devLandscapePileStockOffsetYDpState = profile.landscapePileStockOffsetY
        devLandscapePileWasteOffsetXDpState = profile.landscapePileWasteOffsetX
        devLandscapePileWasteOffsetYDpState = profile.landscapePileWasteOffsetY
        devLandscapePileTableauOffsetXDpState = profile.landscapePileTableauOffsetX
        devLandscapePileTableauOffsetYDpState = profile.landscapePileTableauOffsetY
        devPortraitPileOverallOffsetXDpState = profile.portraitPileOverallOffsetX
        devPortraitPileOverallOffsetYDpState = profile.portraitPileOverallOffsetY
        devPortraitFoundationOffsetXSlimCompactDpState = profile.portraitAspectOffsetsSlimCompact.foundationOffsetX
        devPortraitFoundationOffsetYSlimCompactDpState = profile.portraitAspectOffsetsSlimCompact.foundationOffsetY
        devPortraitDrawWasteOffsetXSlimCompactDpState = profile.portraitAspectOffsetsSlimCompact.drawWasteOffsetX
        devPortraitDrawWasteOffsetYSlimCompactDpState = profile.portraitAspectOffsetsSlimCompact.drawWasteOffsetY
        devPortraitFoundationOffsetXSlimDpState    = profile.portraitAspectOffsetsSlim.foundationOffsetX
        devPortraitFoundationOffsetYSlimDpState    = profile.portraitAspectOffsetsSlim.foundationOffsetY
        devPortraitDrawWasteOffsetXSlimDpState     = profile.portraitAspectOffsetsSlim.drawWasteOffsetX
        devPortraitDrawWasteOffsetYSlimDpState     = profile.portraitAspectOffsetsSlim.drawWasteOffsetY
        devPortraitFoundationOffsetXClassicDpState = profile.portraitAspectOffsetsClassic.foundationOffsetX
        devPortraitFoundationOffsetYClassicDpState = profile.portraitAspectOffsetsClassic.foundationOffsetY
        devPortraitDrawWasteOffsetXClassicDpState  = profile.portraitAspectOffsetsClassic.drawWasteOffsetX
        devPortraitDrawWasteOffsetYClassicDpState  = profile.portraitAspectOffsetsClassic.drawWasteOffsetY
        devPortraitFoundationOffsetXBroadDpState   = profile.portraitAspectOffsetsBroad.foundationOffsetX
        devPortraitFoundationOffsetYBroadDpState   = profile.portraitAspectOffsetsBroad.foundationOffsetY
        devPortraitDrawWasteOffsetXBroadDpState    = profile.portraitAspectOffsetsBroad.drawWasteOffsetX
        devPortraitDrawWasteOffsetYBroadDpState    = profile.portraitAspectOffsetsBroad.drawWasteOffsetY
        devPortraitFoundationOffsetXSquareDpState  = profile.portraitAspectOffsetsSquare.foundationOffsetX
        devPortraitFoundationOffsetYSquareDpState  = profile.portraitAspectOffsetsSquare.foundationOffsetY
        devPortraitDrawWasteOffsetXSquareDpState   = profile.portraitAspectOffsetsSquare.drawWasteOffsetX
        devPortraitDrawWasteOffsetYSquareDpState   = profile.portraitAspectOffsetsSquare.drawWasteOffsetY
        devPortraitPileStockOffsetXDpState = profile.portraitPileStockOffsetX
        devPortraitPileStockOffsetYDpState = profile.portraitPileStockOffsetY
        devPortraitPileWasteOffsetXDpState = profile.portraitPileWasteOffsetX
        devPortraitPileWasteOffsetYDpState = profile.portraitPileWasteOffsetY
        devPortraitPileTableauOffsetXDpState = profile.portraitPileTableauOffsetX
        devPortraitPileTableauOffsetYDpState = profile.portraitPileTableauOffsetY
        devSmallDeviceLandscapeBannerOffsetXDpState = profile.landscapeBannerSmallOffsetX
        devSmallDeviceLandscapeBannerOffsetYDpState = profile.landscapeBannerSmallOffsetY
        devMediumDeviceLandscapeBannerOffsetXDpState = profile.landscapeBannerMediumOffsetX
        devMediumDeviceLandscapeBannerOffsetYDpState = profile.landscapeBannerMediumOffsetY
        devLargeDeviceLandscapeBannerOffsetXDpState = profile.landscapeBannerLargeOffsetX
        devLargeDeviceLandscapeBannerOffsetYDpState = profile.landscapeBannerLargeOffsetY
        devScoreboardOffsetXDpState = profile.scoreboardOffsetX
        devScoreboardOffsetYDpState = profile.scoreboardOffsetY
        devGemRewardOffsetXDpState = profile.gemRewardOffsetX
        devGemRewardOffsetYDpState = profile.gemRewardOffsetY
        devTicketRewardOffsetXDpState = profile.ticketRewardOffsetX
        devTicketRewardOffsetYDpState = profile.ticketRewardOffsetY
    }

    private fun persistActiveLayoutScopedDevAdjusters(mirrored: Boolean) {
        if (!hasAppliedLayoutScopedDevProfile) return
        if (mirrored) {
            mirroredLayoutDevAdjustersState = snapshotLayoutScopedDevAdjusters()
        } else {
            classicLayoutDevAdjustersState = snapshotLayoutScopedDevAdjusters()
        }
    }

    private fun currentPortraitAspectPileOffsets(): PortraitAspectPileOffsets {
        return when (binding.gameBoardView.getCurrentAspectCategory()) {
            DeviceAspectCategory.SLIM -> {
                if (isCompactSlimPortraitBoard()) {
                    PortraitAspectPileOffsets(
                        devPortraitFoundationOffsetXSlimCompactDpState,
                        devPortraitFoundationOffsetYSlimCompactDpState,
                        devPortraitDrawWasteOffsetXSlimCompactDpState,
                        devPortraitDrawWasteOffsetYSlimCompactDpState
                    )
                } else {
                    PortraitAspectPileOffsets(
                        devPortraitFoundationOffsetXSlimDpState,
                        devPortraitFoundationOffsetYSlimDpState,
                        devPortraitDrawWasteOffsetXSlimDpState,
                        devPortraitDrawWasteOffsetYSlimDpState
                    )
                }
            }
            DeviceAspectCategory.CLASSIC -> PortraitAspectPileOffsets(devPortraitFoundationOffsetXClassicDpState, devPortraitFoundationOffsetYClassicDpState, devPortraitDrawWasteOffsetXClassicDpState, devPortraitDrawWasteOffsetYClassicDpState)
            DeviceAspectCategory.BROAD   -> PortraitAspectPileOffsets(devPortraitFoundationOffsetXBroadDpState,   devPortraitFoundationOffsetYBroadDpState,   devPortraitDrawWasteOffsetXBroadDpState,   devPortraitDrawWasteOffsetYBroadDpState)
            DeviceAspectCategory.SQUARE  -> PortraitAspectPileOffsets(devPortraitFoundationOffsetXSquareDpState,  devPortraitFoundationOffsetYSquareDpState,  devPortraitDrawWasteOffsetXSquareDpState,  devPortraitDrawWasteOffsetYSquareDpState)
        }
    }

    private fun setCurrentPortraitAspectPileOffsets(offsets: PortraitAspectPileOffsets) {
        when (binding.gameBoardView.getCurrentAspectCategory()) {
            DeviceAspectCategory.SLIM -> {
                if (isCompactSlimPortraitBoard()) {
                    devPortraitFoundationOffsetXSlimCompactDpState = offsets.foundationOffsetX
                    devPortraitFoundationOffsetYSlimCompactDpState = offsets.foundationOffsetY
                    devPortraitDrawWasteOffsetXSlimCompactDpState = offsets.drawWasteOffsetX
                    devPortraitDrawWasteOffsetYSlimCompactDpState = offsets.drawWasteOffsetY
                } else {
                    devPortraitFoundationOffsetXSlimDpState = offsets.foundationOffsetX
                    devPortraitFoundationOffsetYSlimDpState = offsets.foundationOffsetY
                    devPortraitDrawWasteOffsetXSlimDpState = offsets.drawWasteOffsetX
                    devPortraitDrawWasteOffsetYSlimDpState = offsets.drawWasteOffsetY
                }
            }
            DeviceAspectCategory.CLASSIC -> {
                devPortraitFoundationOffsetXClassicDpState = offsets.foundationOffsetX
                devPortraitFoundationOffsetYClassicDpState = offsets.foundationOffsetY
                devPortraitDrawWasteOffsetXClassicDpState  = offsets.drawWasteOffsetX
                devPortraitDrawWasteOffsetYClassicDpState  = offsets.drawWasteOffsetY
            }
            DeviceAspectCategory.BROAD -> {
                devPortraitFoundationOffsetXBroadDpState = offsets.foundationOffsetX
                devPortraitFoundationOffsetYBroadDpState = offsets.foundationOffsetY
                devPortraitDrawWasteOffsetXBroadDpState  = offsets.drawWasteOffsetX
                devPortraitDrawWasteOffsetYBroadDpState  = offsets.drawWasteOffsetY
            }
            DeviceAspectCategory.SQUARE -> {
                devPortraitFoundationOffsetXSquareDpState = offsets.foundationOffsetX
                devPortraitFoundationOffsetYSquareDpState = offsets.foundationOffsetY
                devPortraitDrawWasteOffsetXSquareDpState  = offsets.drawWasteOffsetX
                devPortraitDrawWasteOffsetYSquareDpState  = offsets.drawWasteOffsetY
            }
        }
    }

    private fun isCompactSlimPortraitBoard(): Boolean {
        val boardWidthPx = binding.gameBoardView.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val isPortrait = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        return isPortrait && boardWidthPx <= portraitSlimCompactMaxBoardWidthPx
    }

    private fun switchLayoutScopedDevAdjustersIfNeeded(mirrored: Boolean) {        val previous = appliedMirroredLayout
        if (previous == mirrored) return
        if (previous != null) {
            persistActiveLayoutScopedDevAdjusters(previous)
        }
        val nextProfile = if (mirrored) mirroredLayoutDevAdjustersState else classicLayoutDevAdjustersState
        applyLayoutScopedDevAdjusters(nextProfile)
        hasAppliedLayoutScopedDevProfile = true
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

    private data class StandardPopupUiConfig(
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
        val rewardTopPercentPortrait: Float = 0.53f,
        val rewardBottomPercentPortrait: Float = 0.65f,
        // Landscape reward band: shifted 5 % lower than portrait
        val rewardTopPercentLandscape: Float = 0.59f,
        val rewardBottomPercentLandscape: Float = 0.71f,
        val buttonsTopPercent: Float = 0.87f,
        val buttonsBottomPercent: Float = buttonsTopPercent + 0.1f,
        val continueButtonWidthPercent: Float = 0.30f,
        val multiplierButtonWidthPercent: Float = 0.30f,
        val buttonGapDp: Float = 10f,
        val rewardAmountTextSp: Float = 20f,
        // Landscape reward row (gems+tickets image/text) rendered at 50%.
        val landscapeRewardRowScale: Float = 0.5f,
        // Portrait reward row (gems+tickets+wand image/text) rendered ~35% smaller.
        val portraitRewardRowScale: Float = 0.5f,
        // Portrait only: push the ticket group down independently of the gem group (~1–2 % of popup height)
        val ticketGroupExtraTopDpPortrait: Float = 8f,
        val ticketGroupExtraTopDpLandscape: Float = 8f,
        // Title display parameters
        val titleBottomPercent: Float = 0.06f,
        val titleOffsetPxLandscape: Float = 0f,
        val titleOffsetPxPortrait: Float = 0f,
        val titleTextSp: Float = 48f
    )

    // Single tweak cluster for the custom win popup. Adjust these values instead of
    // searching through the XML when you want to nudge positions/sizes.
    private val winPopupUiConfig = WinPopupUiConfig()

    private fun resolveRewardPopupTitleTextSp(baseTitleTextSp: Float): Float {
        val titleDeltaSp = devVictoryTextSizeSpState - BASELINE_WIN_VICTORY_TEXT_SP
        return (baseTitleTextSp + titleDeltaSp).coerceAtLeast(4f)
    }

    private fun buildWinRewardPopupUiConfig(): RewardPopupDialog.UiConfig {
        val widthLandscape = (winPopupUiConfig.dialogWidthPercentLandscape * winPopupUiConfig.dialogScaleLandscape)
            .coerceIn(0.1f, 1f)
        val widthPortrait = (winPopupUiConfig.dialogWidthPercentPortrait * winPopupUiConfig.dialogScalePortrait)
            .coerceIn(0.1f, 1f)
        val heightLandscape = (winPopupUiConfig.dialogHeightPercent * winPopupUiConfig.dialogScaleLandscape)
            .coerceIn(0.1f, 1f)
        val heightPortrait = (winPopupUiConfig.dialogHeightPercent * winPopupUiConfig.dialogScalePortrait)
            .coerceIn(0.1f, 1f)

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return RewardPopupDialog.UiConfig(
            dialogWidthPercentLandscape = widthLandscape,
            dialogWidthPercentPortrait = widthPortrait,
            dialogHeightPercentLandscape = heightLandscape,
            dialogHeightPercentPortrait = heightPortrait,
            titleBottomPercentLandscape = winPopupUiConfig.rewardTopPercentLandscape,
            titleBottomPercentPortrait = winPopupUiConfig.rewardTopPercentPortrait,
            rewardBottomPercentLandscape = winPopupUiConfig.rewardBottomPercentLandscape,
            rewardBottomPercentPortrait = winPopupUiConfig.rewardBottomPercentPortrait,
            buttonsTopPercent = winPopupUiConfig.buttonsTopPercent,
            buttonsBottomPercent = winPopupUiConfig.buttonsBottomPercent,
            showTitle = true,
            showStarburst = !(isLandscape
                    && resources.configuration.smallestScreenWidthDp < 600),
            starburstImageResId = R.drawable.ic_star_burst_yellow,
            starburstOffsetXPx = testerStarburstPositionXPx.toFloat(),
            starburstOffsetYPx = testerStarburstPositionYPx.toFloat(),
            starburstScale = testerStarburstScale,
            continueButtonWidthPercent = winPopupUiConfig.continueButtonWidthPercent,
            multiplierButtonWidthPercent = winPopupUiConfig.multiplierButtonWidthPercent,
            buttonGapDp = winPopupUiConfig.buttonGapDp,
            rewardTextOverrideSp = devRewardTextSizeSpState,
            rewardRowScalePortrait = winPopupUiConfig.portraitRewardRowScale,
            rewardRowScaleLandscape = winPopupUiConfig.landscapeRewardRowScale,
            gemImageHeightDp = devGemImageHeightDpState,
            gemOffsetXDp = devGemOffsetXDpState,
            gemOffsetYDp = devGemOffsetYDpState,
            ticketImageHeightDp = devTicketImageHeightDpState,
            ticketOffsetXDp = devTicketOffsetXDpState,
            ticketOffsetYDp = devTicketOffsetYDpState,
            wandImageHeightDp = devWandImageHeightDpState,
            wandOffsetXDp = devWandOffsetXDpState,
            wandOffsetYDp = devWandOffsetYDpState,
            gemNumberOffsetXDp = devGemNumberOffsetXDpState,
            gemNumberOffsetYDp = devGemNumberOffsetYDpState,
            ticketNumberOffsetXDp = devTicketNumberOffsetXDpState,
            ticketNumberOffsetYDp = devTicketNumberOffsetYDpState,
            wandNumberOffsetXDp = devWandNumberOffsetXDpState,
            wandNumberOffsetYDp = devWandNumberOffsetYDpState,
            buttonRowOffsetXDp = devButtonRowOffsetXDpState,
            buttonRowOffsetYDp = devButtonRowOffsetYDpState,
            button0ScaleX = devPopupButton0ScaleXState,
            button0ScaleY = devPopupButton0ScaleYState,
            button0Scale = devPopupButton0ScaleState,
            button1ScaleX = devPopupButton1ScaleXState,
            button1ScaleY = devPopupButton1ScaleYState,
            button1Scale = devPopupButton1ScaleState,
            button2ScaleX = devPopupButton2ScaleXState,
            button2ScaleY = devPopupButton2ScaleYState,
            button2Scale = devPopupButton2ScaleState,
            descriptionTextSp = devPopupDescriptionTextSizeSpState,
            descriptionOffsetXDp = devPopupDescriptionOffsetXDpState,
            descriptionOffsetYDp = devPopupDescriptionOffsetYDpState,
            titleOffsetXPxLandscape = dpToPxFloatSigned(devVictoryOffsetXDpState),
            titleOffsetXPxPortrait = dpToPxFloatSigned(devVictoryOffsetXDpState),
            titleOffsetPxLandscape = dpToPxFloatSigned(devVictoryOffsetYDpState),
            titleOffsetPxPortrait = dpToPxFloatSigned(devVictoryOffsetYDpState),
            titleTextSp = resolveRewardPopupTitleTextSp(standardPopupUiConfig.titleTextSp)
        )
    }
    private val standardPopupUiConfig = StandardPopupUiConfig()

    private fun buildStandardUnifiedRewardPopupUiConfig(): RewardPopupDialog.UiConfig {
        val widthLandscape = (standardPopupUiConfig.dialogWidthPercentLandscape * standardPopupUiConfig.dialogScaleLandscape)
            .coerceIn(0.1f, 1f)
        val widthPortrait = (standardPopupUiConfig.dialogWidthPercentPortrait * standardPopupUiConfig.dialogScalePortrait)
            .coerceIn(0.1f, 1f)
        val heightLandscape = (standardPopupUiConfig.dialogHeightPercent * standardPopupUiConfig.dialogScaleLandscape)
            .coerceIn(0.1f, 1f)
        val heightPortrait = (standardPopupUiConfig.dialogHeightPercent * standardPopupUiConfig.dialogScalePortrait)
            .coerceIn(0.1f, 1f)

        return RewardPopupDialog.UiConfig(
            dialogWidthPercentLandscape = widthLandscape,
            dialogWidthPercentPortrait = widthPortrait,
            dialogHeightPercentLandscape = heightLandscape,
            dialogHeightPercentPortrait = heightPortrait,
            titleBottomPercentLandscape = standardPopupUiConfig.rewardTopPercentLandscape,
            titleBottomPercentPortrait = standardPopupUiConfig.rewardTopPercentPortrait,
            rewardBottomPercentLandscape = standardPopupUiConfig.rewardBottomPercentLandscape,
            rewardBottomPercentPortrait = standardPopupUiConfig.rewardBottomPercentPortrait,
            buttonsTopPercent = standardPopupUiConfig.buttonsTopPercent,
            buttonsBottomPercent = standardPopupUiConfig.buttonsBottomPercent,
            showTitle = true,
            showStarburst = false,
            starburstImageResId = R.drawable.ic_star_burst_yellow,
            starburstOffsetXPx = testerStarburstPositionXPx.toFloat(),
            starburstOffsetYPx = testerStarburstPositionYPx.toFloat(),
            starburstScale = testerStarburstScale,
            continueButtonWidthPercent = standardPopupUiConfig.continueButtonWidthPercent,
            multiplierButtonWidthPercent = standardPopupUiConfig.multiplierButtonWidthPercent,
            buttonGapDp = standardPopupUiConfig.buttonGapDp,
            rewardTextOverrideSp = devRewardTextSizeSpState,
            rewardRowScalePortrait = standardPopupUiConfig.portraitRewardRowScale,
            rewardRowScaleLandscape = standardPopupUiConfig.landscapeRewardRowScale,
            gemImageHeightDp = devGemImageHeightDpState,
            gemOffsetXDp = devGemOffsetXDpState,
            gemOffsetYDp = devGemOffsetYDpState,
            ticketImageHeightDp = devTicketImageHeightDpState,
            ticketOffsetXDp = devTicketOffsetXDpState,
            ticketOffsetYDp = devTicketOffsetYDpState,
            wandImageHeightDp = devWandImageHeightDpState,
            wandOffsetXDp = devWandOffsetXDpState,
            wandOffsetYDp = devWandOffsetYDpState,
            gemNumberOffsetXDp = devGemNumberOffsetXDpState,
            gemNumberOffsetYDp = devGemNumberOffsetYDpState,
            ticketNumberOffsetXDp = devTicketNumberOffsetXDpState,
            ticketNumberOffsetYDp = devTicketNumberOffsetYDpState,
            wandNumberOffsetXDp = devWandNumberOffsetXDpState,
            wandNumberOffsetYDp = devWandNumberOffsetYDpState,
            buttonRowOffsetXDp = devButtonRowOffsetXDpState,
            buttonRowOffsetYDp = devButtonRowOffsetYDpState,
            button0ScaleX = devPopupButton0ScaleXState,
            button0ScaleY = devPopupButton0ScaleYState,
            button0Scale = devPopupButton0ScaleState,
            button1ScaleX = devPopupButton1ScaleXState,
            button1ScaleY = devPopupButton1ScaleYState,
            button1Scale = devPopupButton1ScaleState,
            button2ScaleX = devPopupButton2ScaleXState,
            button2ScaleY = devPopupButton2ScaleYState,
            button2Scale = devPopupButton2ScaleState,
            descriptionTextSp = devPopupDescriptionTextSizeSpState,
            descriptionOffsetXDp = devPopupDescriptionOffsetXDpState,
            descriptionOffsetYDp = devPopupDescriptionOffsetYDpState,
            titleBottomPercent = standardPopupUiConfig.titleBottomPercent,
            titleOffsetXPxLandscape = dpToPxFloatSigned(devVictoryOffsetXDpState),
            titleOffsetXPxPortrait = dpToPxFloatSigned(devVictoryOffsetXDpState),
            titleOffsetPxLandscape = standardPopupUiConfig.titleOffsetPxLandscape + dpToPxFloatSigned(devVictoryOffsetYDpState),
            titleOffsetPxPortrait = standardPopupUiConfig.titleOffsetPxPortrait + dpToPxFloatSigned(devVictoryOffsetYDpState),
            titleTextSp = resolveRewardPopupTitleTextSp(standardPopupUiConfig.titleTextSp)
        )
    }

    private enum class RewardPopupStyle {
        WIN,
        STANDARD
    }

    private fun buildUnifiedRewardPopupUiConfig(style: RewardPopupStyle): RewardPopupDialog.UiConfig {
        val winBase = buildWinRewardPopupUiConfig()
        val unifiedBase = buildStandardUnifiedRewardPopupUiConfig()
        return when (style) {
            RewardPopupStyle.WIN -> winBase
            RewardPopupStyle.STANDARD -> unifiedBase
        }
    }

    private fun showUnifiedRewardPopup(
        model: RewardPopupDialog.PopupModel,
        uiConfig: RewardPopupDialog.UiConfig,
        isWinPopup: Boolean,
        onButtonClick: (index: Int, dialog: Dialog) -> Unit
    ): Dialog {
        val dialog = rewardPopupDialog.showPopup(
            model = model,
            baseImageResId = R.drawable.ic_popup_rect_blue,
            uiConfig = uiConfig,
            onButtonClick = onButtonClick
        )

        if (!isWinPopup) {
            return dialog
        }

        val shouldShowStarburst = uiConfig.showStarburst && model.showStarburst
        val isLandscapeNow = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val dialogWidthPercent = if (isLandscapeNow) uiConfig.dialogWidthPercentLandscape else uiConfig.dialogWidthPercentPortrait
        val dialogHeightPercent = if (isLandscapeNow) uiConfig.dialogHeightPercentLandscape else uiConfig.dialogHeightPercentPortrait
        val (usableWidthPx, usableHeightPx) = getUsableWindowSizePx()
        val widthPx = (usableWidthPx * dialogWidthPercent).toInt().coerceAtLeast(1)
        val baseHeightPx = (usableHeightPx * dialogHeightPercent).toInt().coerceAtLeast(1)
        val initialOverflowTopPx = if (shouldShowStarburst) estimateInitialStarburstTopOverflowPx() else 0
        val heightPx = (baseHeightPx + initialOverflowTopPx).coerceAtLeast(1)
        dialog.window?.setLayout(widthPx, heightPx)

        val dialogView = dialog.findViewById<View>(R.id.layout_popup_body)?.rootView
        val starburstView = dialog.findViewById<ImageView>(R.id.iv_win_popup_starburst)
        if (!shouldShowStarburst) {
            starburstView?.visibility = View.GONE
        }
        activeWinPopupDialog = dialog
        activeWinPopupRoot = dialogView
        activeWinPopupBaseWidthPx = widthPx
        activeWinPopupBaseHeightPx = baseHeightPx

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
        dialog.window?.setLayout(widthPx, heightPx)
        playWinPopupSoundIfAllowed()

        if (shouldShowStarburst) {
            starburstView?.post {
                if (!dialog.isShowing) return@post
                activeStarburstView = starburstView
                refreshActiveStarburstDebugAndMotion()
            }
        } else {
            activeStarburstAnimator?.cancel()
            activeStarburstAnimator = null
            activeStarburstView = null
        }

        return dialog
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
    private var soundOn: Boolean = true
    private var hapticsOn: Boolean = true

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
    private var showTimerRow: Boolean = true
    private var showMovesRow: Boolean = true
    private var showScoreRow: Boolean = true
    private var autoCompleteButtonEnabled: Boolean = true
    private var gemTotal: Int = 0
    private var ticketTotal: Int = 0
    private var magicWandTotal: Int = 0
    private var isMagicWandSelectionMode: Boolean = false
    private var dailyBonusPromptShownThisLaunch: Boolean = false
    /** Tracks which mirrored state has been applied to the control panel, to avoid double-reversals. */
    private var appliedMirroredLayout: Boolean? = null
    /** True after classic/mirrored profile defaults have been applied into active dev state at least once. */
    private var hasAppliedLayoutScopedDevProfile: Boolean = false
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
    private var devGemOffsetXDpState: Float          = 0f
    private var devGemOffsetYDpState: Float          = 0f
    private var devTicketImageHeightDpState: Float   = BASELINE_WIN_TICKET_HEIGHT_DP
    private var devTicketOffsetXDpState: Float       = 0f
    private var devTicketOffsetYDpState: Float       = 0f
    private var devWandImageHeightDpState: Float     = BASELINE_WIN_MAGIC_WAND_HEIGHT_DP
    private var devWandOffsetXDpState: Float         = 0f
    private var devWandOffsetYDpState: Float         = 0f
    private var devRewardTextSizeSpState: Float      = BASELINE_WIN_REWARD_TEXT_SP
    private var devGemNumberOffsetXDpState: Float    = 0f
    private var devGemNumberOffsetYDpState: Float    = 0f
    private var devTicketNumberOffsetXDpState: Float = 0f
    private var devTicketNumberOffsetYDpState: Float = 0f
    private var devWandNumberOffsetXDpState: Float   = 0f
    private var devWandNumberOffsetYDpState: Float   = 0f
    private var devButtonRowOffsetXDpState: Float    = 0f
    private var devButtonRowOffsetYDpState: Float    = 0f
    private var devPopupButton0ScaleXState: Float    = 1f
    private var devPopupButton0ScaleYState: Float    = 3f
    private var devPopupButton0ScaleState: Float     = 1f
    private var devPopupButton1ScaleXState: Float    = 1f
    private var devPopupButton1ScaleYState: Float    = 3f
    private var devPopupButton1ScaleState: Float     = 1f
    private var devPopupButton2ScaleXState: Float    = 1f
    private var devPopupButton2ScaleYState: Float    = 3f
    private var devPopupButton2ScaleState: Float     = 1f
    private var devPopupDescriptionTextSizeSpState: Float = 20f
    private var devPopupDescriptionOffsetXDpState: Float = 0f
    private var devPopupDescriptionOffsetYDpState: Float = 0f
    private var devVictoryTextSizeSpState: Float     = BASELINE_WIN_VICTORY_TEXT_SP
    private var devVictoryOffsetXDpState: Float      = 0f
    private var devVictoryOffsetYDpState: Float      = 30f

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
         devGemOffsetXDpState      = (if (isLandscape) BASELINE_WIN_GEM_OFFSET_X_DP_LANDSCAPE else BASELINE_WIN_GEM_OFFSET_X_DP_PORTRAIT) * ratio
         devGemOffsetYDpState      = (if (isLandscape) BASELINE_WIN_GEM_OFFSET_Y_DP_LANDSCAPE else BASELINE_WIN_GEM_OFFSET_Y_DP_PORTRAIT) * ratio
         devTicketOffsetXDpState   = (if (isLandscape) BASELINE_WIN_TICKET_OFFSET_X_DP_LANDSCAPE else BASELINE_WIN_TICKET_OFFSET_X_DP_PORTRAIT) * ratio
         devTicketOffsetYDpState   = (if (isLandscape) BASELINE_WIN_TICKET_OFFSET_Y_DP_LANDSCAPE else BASELINE_WIN_TICKET_OFFSET_Y_DP_PORTRAIT) * ratio
         devWandOffsetXDpState     = (if (isLandscape) BASELINE_WIN_WAND_OFFSET_X_DP_LANDSCAPE else BASELINE_WIN_WAND_OFFSET_X_DP_PORTRAIT) * ratio
         devWandOffsetYDpState     = (if (isLandscape) BASELINE_WIN_WAND_OFFSET_Y_DP_LANDSCAPE else BASELINE_WIN_WAND_OFFSET_Y_DP_PORTRAIT) * ratio
         devGemNumberOffsetXDpState = (if (isLandscape) BASELINE_WIN_GEM_NUMBER_OFFSET_X_DP_LANDSCAPE else BASELINE_WIN_GEM_NUMBER_OFFSET_X_DP_PORTRAIT) * ratio
         devGemNumberOffsetYDpState = (if (isLandscape) BASELINE_WIN_GEM_NUMBER_OFFSET_Y_DP_LANDSCAPE else BASELINE_WIN_GEM_NUMBER_OFFSET_Y_DP_PORTRAIT) * ratio
         devTicketNumberOffsetXDpState = (if (isLandscape) BASELINE_WIN_TICKET_NUMBER_OFFSET_X_DP_LANDSCAPE else BASELINE_WIN_TICKET_NUMBER_OFFSET_X_DP_PORTRAIT) * ratio
         devTicketNumberOffsetYDpState = (if (isLandscape) BASELINE_WIN_TICKET_NUMBER_OFFSET_Y_DP_LANDSCAPE else BASELINE_WIN_TICKET_NUMBER_OFFSET_Y_DP_PORTRAIT) * ratio
         devWandNumberOffsetXDpState = (if (isLandscape) BASELINE_WIN_WAND_NUMBER_OFFSET_X_DP_LANDSCAPE else BASELINE_WIN_WAND_NUMBER_OFFSET_X_DP_PORTRAIT) * ratio
         devWandNumberOffsetYDpState = (if (isLandscape) BASELINE_WIN_WAND_NUMBER_OFFSET_Y_DP_LANDSCAPE else BASELINE_WIN_WAND_NUMBER_OFFSET_Y_DP_PORTRAIT) * ratio
         devButtonRowOffsetXDpState = (if (isLandscape) BASELINE_WIN_BUTTON_ROW_OFFSET_X_DP_LANDSCAPE else (if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.BAKLAVA) BASELINE_WIN_BUTTON_ROW_OFFSET_X_DP_PORTRAIT else BASELINE_WIN_BUTTON_ROW_OFFSET_X_DP_PORTRAIT_BAKLAVA)) * ratio
         devButtonRowOffsetYDpState = (if (isLandscape) BASELINE_WIN_BUTTON_ROW_OFFSET_Y_DP_LANDSCAPE else BASELINE_WIN_BUTTON_ROW_OFFSET_Y_DP_PORTRAIT) * ratio
         devPopupButton0ScaleXState = 1f
         devPopupButton0ScaleYState = 3f
         devPopupButton0ScaleState = 1f
         devPopupButton1ScaleXState = 1f
         devPopupButton1ScaleYState = 3f
         devPopupButton1ScaleState = 1f
         devPopupButton2ScaleXState = 1f
         devPopupButton2ScaleYState = 3f
         devPopupButton2ScaleState = 1f
         devPopupDescriptionTextSizeSpState = BaselineResolutionScaleUtil.scaleFromBaseline(20f, ratio)
         devPopupDescriptionOffsetXDpState = 0f
         devPopupDescriptionOffsetYDpState = 30f
         devVictoryOffsetXDpState  = 0f
         devVictoryOffsetYDpState  = 30f
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
         // Win popup element baselines tuned on the medium tablet (1600 × 2560 px).
         private const val BASELINE_WIN_GEM_HEIGHT_DP     = 60f
         private const val BASELINE_WIN_TICKET_HEIGHT_DP  = 180f
         private const val BASELINE_WIN_MAGIC_WAND_HEIGHT_DP  = 100f
         private const val BASELINE_WIN_REWARD_TEXT_SP    = 60f
         private const val BASELINE_WIN_VICTORY_TEXT_SP   = 60f

        // Gem image offset
        private const val BASELINE_WIN_GEM_OFFSET_X_DP_PORTRAIT = 0f
        private const val BASELINE_WIN_GEM_OFFSET_Y_DP_PORTRAIT = -100f
        private const val BASELINE_WIN_GEM_OFFSET_X_DP_LANDSCAPE = 20f
        private const val BASELINE_WIN_GEM_OFFSET_Y_DP_LANDSCAPE = -100f

        // Ticket/wand image offset
        private const val BASELINE_WIN_TICKET_OFFSET_X_DP_PORTRAIT = 0f
        private const val BASELINE_WIN_TICKET_OFFSET_Y_DP_PORTRAIT = -60f
        private const val BASELINE_WIN_TICKET_OFFSET_X_DP_LANDSCAPE = 40f
        private const val BASELINE_WIN_TICKET_OFFSET_Y_DP_LANDSCAPE = -55f
        // Magic wand image offset
        private const val BASELINE_WIN_WAND_OFFSET_X_DP_PORTRAIT = 0f
        private const val BASELINE_WIN_WAND_OFFSET_Y_DP_PORTRAIT = -100f
        private const val BASELINE_WIN_WAND_OFFSET_X_DP_LANDSCAPE = 40f
        private const val BASELINE_WIN_WAND_OFFSET_Y_DP_LANDSCAPE = -90f
        // Gem number offset
        private const val BASELINE_WIN_GEM_NUMBER_OFFSET_X_DP_PORTRAIT = 0f
        private const val BASELINE_WIN_GEM_NUMBER_OFFSET_Y_DP_PORTRAIT = -75f
        private const val BASELINE_WIN_GEM_NUMBER_OFFSET_X_DP_LANDSCAPE = 0f
        private const val BASELINE_WIN_GEM_NUMBER_OFFSET_Y_DP_LANDSCAPE = -25f
        // Ticket number offset
        private const val BASELINE_WIN_TICKET_NUMBER_OFFSET_X_DP_PORTRAIT = 0f
        private const val BASELINE_WIN_TICKET_NUMBER_OFFSET_Y_DP_PORTRAIT = -135f
        private const val BASELINE_WIN_TICKET_NUMBER_OFFSET_X_DP_LANDSCAPE = 40f
        private const val BASELINE_WIN_TICKET_NUMBER_OFFSET_Y_DP_LANDSCAPE = -85f
        // Magic wand number offset
        private const val BASELINE_WIN_WAND_NUMBER_OFFSET_X_DP_PORTRAIT = 0f
        private const val BASELINE_WIN_WAND_NUMBER_OFFSET_Y_DP_PORTRAIT = -100f
        private const val BASELINE_WIN_WAND_NUMBER_OFFSET_X_DP_LANDSCAPE = 40f
        private const val BASELINE_WIN_WAND_NUMBER_OFFSET_Y_DP_LANDSCAPE = -50f
        // Button row offset
        private const val BASELINE_WIN_BUTTON_ROW_OFFSET_X_DP_PORTRAIT = 85f
        private const val BASELINE_WIN_BUTTON_ROW_OFFSET_X_DP_PORTRAIT_BAKLAVA = 50f
        private const val BASELINE_WIN_BUTTON_ROW_OFFSET_X_DP_LANDSCAPE = 90f
        private const val BASELINE_WIN_BUTTON_ROW_OFFSET_Y_DP_PORTRAIT = -45f
        private const val BASELINE_WIN_BUTTON_ROW_OFFSET_Y_DP_LANDSCAPE = -30f

        // Daily popup baselines for ratio/orientation scaling
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

        // Scale the bottom control area via Flow helper now that wrapper LinearLayout is removed.
        findViewById<View?>(R.id.control_buttons_flow)?.let { UiScaleUtil.applyScreenVerticalScale(it, this) }
        // Removed: binding.gameInfoPanel (top bar no longer exists)

        // Wire viewModel into GameBoardView
        binding.gameBoardView.viewModel = viewModel
        
        // Wire GameBoardView back into viewModel for animation scheduling
        viewModel.gameBoardView = binding.gameBoardView

        // Wire AssetResolver into GameBoardView
        binding.gameBoardView.assetResolver = AndroidAssetResolver(this)
        binding.gameBoardView.onClickMoveSoundRequested = {
            playCardClickMoveSound()
            performSuccessHaptic(binding.gameBoardView)
        }
        binding.gameBoardView.onDragDropResult = { success ->
            if (success) {
                performSuccessHaptic(binding.gameBoardView)
            } else {
                performErrorHaptic(binding.gameBoardView)
            }
        }
        binding.gameBoardView.onShuffleSoundRequested = { onComplete -> playShuffleSoundSequence(onComplete) }
        binding.gameBoardView.onLockedTableauUnlockRequested = { onLockedTableauUnlockRequested() }
        binding.gameBoardView.onMagicWandTargetSelected = { type, index, cardIndex ->
            onMagicWandTargetSelected(type, index, cardIndex)
        }
        applyLockedPileAdIconDevConfigToBoard()
        applyLandscapePileLayoutDevConfigToBoard()
        applyPortraitPileLayoutDevConfigToBoard()
        applyAspectCategoryPileTrimsToBoard()
        applyLandscapeBannerOverlayDevOffsets()
        applyTopHudDevOffsets()
        
        // Apply device scale ratio to control buttons after the board geometry is computed
        binding.gameBoardView.post {
            scaleControlButtonsBasedOnDeviceRatio()
        }
        
        binding.gameBoardView.bindToViewModel(this)

        // Optional manager (no heavy rendering here)
        uiManager = CardStackUIManager(this, binding.root, viewModel)

        // Initialize and load banner ads
        adManager = AdManager(this)
        adManager.initializeAds()
        val initialBannerAdSizes = configureLandscapeBannerBoxAndResolveAdSizes()
        val initialBannerAdView = resolveActiveBannerAdView(initialBannerAdSizes)
        adManager.loadBannerAd(initialBannerAdView, initialBannerAdSizes)
        adManager.loadRewardedAd()
        adManager.loadRewardedInterstitialAd()

        statsManager = GameStatsManager(applicationContext)
        settingsManager = SettingsManager(applicationContext)
        rewardPopupDialog = RewardPopupDialog(this)
        initializeMoveSoundPool()
        renderGemHud(gemTotal)
        renderTicketHud(ticketTotal)
        renderMagicWandHud(magicWandTotal)
        binding.btnAutoMove.visibility = View.GONE

        // Launcher starts directly in GameActivity; default behavior is resume-or-new.
        forceNewGameOnLaunch = intent.getBooleanExtra("force_new_game", false)
        viewModel.initializeForLaunch(forceNewGameOnLaunch)

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    settingsManager.gamePlaySettingsFlow.collect { settings ->
                        isPremiumAccount = settings.premiumAcct
                        soundOn = settings.soundOn
                        hapticsOn = settings.haptics
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
                        updateScoreLabel(g)
                        binding.tvMovesValue.text = formatMovesValue(g.moves)
                        updateAutoCompleteButtonVisibility(g)

                        if (g.status != GameStatus.WON) {
                            winCelebrationPlayed = false
                            pendingWinUiAfterAnimation = false
                        } else {
                            showWinCelebrationThenDialog()
                        }
                    }
                }

                launch {
                    viewModel.autoCompleteEnabled.collect { enabled ->
                        autoCompleteButtonEnabled = enabled
                        updateAutoCompleteButtonVisibility(viewModel.game.value)
                    }
                }

                launch {
                    viewModel.gameTime.collect { seconds ->
                        binding.tvTimerValue.text = formatTime(seconds)
                    }
                }

                launch {
                    viewModel.showGameTimer.collect { shouldShow ->
                        showTimerRow = shouldShow
                        binding.rowTimer.visibility = if (shouldShow) View.VISIBLE else View.GONE
                        updateScoreboardVisibility()
                    }
                }

                launch {
                    viewModel.scoreMethod.collect {
                        updateScoreLabel(viewModel.game.value)
                    }
                }

                launch {
                    viewModel.showScore.collect { shouldShow ->
                        showScoreRow = shouldShow
                        binding.rowScore.visibility = if (shouldShow) View.VISIBLE else View.GONE
                        updateScoreboardVisibility()
                    }
                }

                launch {
                    viewModel.showMoves.collect { shouldShow ->
                        showMovesRow = shouldShow
                        binding.rowMoves.visibility = if (shouldShow) View.VISIBLE else View.GONE
                        updateScoreboardVisibility()
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

                launch {
                    viewModel.isMirroredLayout.collect { mirrored ->
                        applyMirroredLayoutUi(mirrored)
                    }
                }
            }
        }

        // Simple button hookups (if present in layout)
        binding.btnUndo.setOnClickListener { buttonView ->
            performUiActionHaptic(buttonView)
            onHelpControlClicked(HelpControlAction.UNDO, buttonView) { handleUndoClick() }
        }
        binding.btnRedo.setOnClickListener { buttonView ->
            performUiActionHaptic(buttonView)
            onHelpControlClicked(HelpControlAction.REDO, buttonView) { handleRedoClick() }
        }
        // btn_new_game and btn_restart are hidden stubs; actions are in the Play popup.
        binding.btnNewGame.setOnClickListener {
            winCelebrationPlayed = false
            startNewGameWithShuffleAndDealAnimation()
        }
        // btn_restart stub kept for ViewBinding; handled via Play popup
        findViewById<Button>(R.id.btn_hint)?.setOnClickListener { buttonView ->
            performUiActionHaptic(buttonView)
            onHelpControlClicked(HelpControlAction.HINT, buttonView) {
                if (!viewModel.showManualHints()) {
                    showErrorFeedback(R.string.no_hints_available, buttonView)
                }
            }
        }
        binding.btnStats.setOnClickListener { anchor ->
            performUiActionHaptic(anchor)
            showPlayPopup(anchor)
        }
        binding.magicWandContainer?.setOnClickListener { onMagicWandClicked() }
        findViewById<Button>(R.id.btn_auto_move)?.setOnClickListener { buttonView ->
            onAutoMoveClicked(buttonView)
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

    private fun updateScoreLabel(game: com.gpgamelab.justpatience.model.Game) {
        val score = game.scoreForMethod(viewModel.scoreMethod.value)
        binding.tvScoreValue.text = formatScoreValue(score)
    }

    private fun formatMovesValue(moves: Int): String {
        return getString(R.string.scoreboard_moves_value_format, moves.coerceIn(0, 999))
    }

    private fun formatScoreValue(score: Int): String {
        return if (score >= 0) {
            getString(R.string.scoreboard_score_value_format, score.coerceAtMost(9999))
        } else {
            "-${kotlin.math.abs(score).coerceAtMost(999).toString().padStart(3, '0')}"
        }
    }

    private fun updateScoreboardVisibility() {
        val visibleRows = listOf(showTimerRow, showMovesRow, showScoreRow).count { it }
        binding.boardScoreboardContainer.visibility = if (visibleRows == 0) View.GONE else View.VISIBLE
    }

    private fun updateAutoCompleteButtonVisibility(game: com.gpgamelab.justpatience.model.Game) {
        binding.btnAutoMove.visibility =
            if (autoCompleteButtonEnabled && areAllCardsFaceUp(game)) View.VISIBLE else View.GONE
    }

    private fun areAllCardsFaceUp(game: com.gpgamelab.justpatience.model.Game): Boolean {
        val stockAllFaceUp = game.stock.asList().all { it.isFaceUp }
        val wasteAllFaceUp = game.waste.asList().all { it.isFaceUp }
        val tableauAllFaceUp = game.tableau.all { pile -> pile.asList().all { it.isFaceUp } }
        val foundationAllFaceUp = game.foundations.all { pile -> pile.asList().all { it.isFaceUp } }
        return stockAllFaceUp && wasteAllFaceUp && tableauAllFaceUp && foundationAllFaceUp
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
        if (!soundOn || !moveSoundLoaded || moveSoundId == 0) return
        moveSoundPool?.play(moveSoundId, 1f, 1f, 1, 0, 0.5f)
    }

    private fun playMagicWandSound() {
        if (!soundOn || !magicWandSoundLoaded || magicWandSoundId == 0) return
        moveSoundPool?.play(magicWandSoundId, 1f, 1f, 1, 0, 0.5f)
    }

    private fun playWinPopupSoundIfAllowed() {
        if (!soundOn || !winPopupSoundLoaded || winPopupSoundId == 0) return
        moveSoundPool?.play(winPopupSoundId, 1f, 1f, 1, 0, 1f)
    }

    private fun performUiActionHaptic(sourceView: View? = null) {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, sourceView)
    }

    private fun performSuccessHaptic(sourceView: View? = null) {
        val successConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        performHapticFeedback(successConstant, sourceView)
    }

    private fun performErrorHaptic(sourceView: View? = null) {
        val errorConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        performHapticFeedback(errorConstant, sourceView)
    }

    private fun performHapticFeedback(constant: Int, sourceView: View? = null) {
        if (!hapticsOn) return
        (sourceView ?: binding.root).performHapticFeedback(constant)
    }

    private fun showErrorFeedback(messageResId: Int, sourceView: View? = null) {
        performErrorHaptic(sourceView)
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorFeedback(message: CharSequence, sourceView: View? = null) {
        performErrorHaptic(sourceView)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAdNotReadyFeedback(messageResId: Int, sourceView: View? = null) {
        showErrorFeedback(messageResId, sourceView)
    }

    private fun showAdNotReadyFeedback(message: CharSequence, sourceView: View? = null) {
        showErrorFeedback(message, sourceView)
    }

    private fun playShuffleSoundSequence(onComplete: (() -> Unit)? = null) {
        if (!soundOn) {
            onComplete?.invoke()
            return
        }

        if (shuffleSound1Loaded && shuffleSound1Id != 0) {
            moveSoundPool?.play(shuffleSound1Id, 1f, 1f, 1, 0, 0.7f)
        }

        lifecycleScope.launch {
            delay(devShuffleSecondClipDelayMsState.toLong().coerceAtLeast(0L))
            if (shuffleSound2Loaded && shuffleSound2Id != 0) {
                moveSoundPool?.play(shuffleSound2Id, 1f, 1f, 1, 0, 0.7f)
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
        binding.ivGemBag.setImageResource(R.drawable.ic_treasure_3_gem_green)
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
                        animateAndConsumeHelpCoupon(targetView, HelpControlAction.RESTART)
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

    private fun showGameEndDialog() {
        if ( isFinishing || isDestroyed || winDialogShowing) return

        winDialogShowing = true
        val baseRewards = WinRewards(
            gems = 10,
            tickets = 0,
            wands = 0
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
        val popupModel = RewardPopupDialog.PopupModel(
            title = getString(R.string.win_popup_victory_title),
            titleColorInt = android.graphics.Color.parseColor("#FFD740"),
            rewards = listOf(
                RewardPopupDialog.PopupRewardItem(
                    count = baseRewards.gems,
                    imageResId = R.drawable.ic_treasure_3_gem_green
                )
            ),
            buttons = listOf(
                RewardPopupDialog.PopupButtonItem(
                    backgroundResId = R.drawable.ic_button_orange_orange_claim,
                    contentDescription = getString(R.string.continue_without_reward)
                ),
                RewardPopupDialog.PopupButtonItem(
                    backgroundResId = if (adMultiplier == 3) {
                        R.drawable.ic_button_orange_orange_x3_with_ad
                    } else {
                        R.drawable.ic_button_orange_orange_x2_with_ad
                    },
                    contentDescription = getString(R.string.watch_optional_ad)
                )
            ),
            showStarburst = true
        )
        val uiConfig = buildUnifiedRewardPopupUiConfig(RewardPopupStyle.WIN)
        showUnifiedRewardPopup(
            model = popupModel,
            uiConfig = uiConfig,
            isWinPopup = true,
            onButtonClick = { index, popupDialog ->
                when (index) {
                    0 -> {
                        lifecycleScope.launch {
                            settingsManager.recordWinRewardSelection(selectedMultiplier = false)
                            maybeDelayWinPopupDebugPause()
                            popupDialog.dismiss()
                            completeWinRewardFlow(baseRewards)
                        }
                    }
                    1 -> {
                        lifecycleScope.launch {
                            settingsManager.recordWinRewardSelection(selectedMultiplier = true)
                            maybeDelayWinPopupDebugPause()
                            popupDialog.dismiss()
                            showWinMultiplierRewardAd(baseRewards, adMultiplier)
                        }
                    }
                }
            }
        )
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
            showAdNotReadyFeedback(R.string.optional_ad_not_ready)
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
            settingsManager.incrementMagicWandsUsed()
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
                            showAdNotReadyFeedback(R.string.help_unlock_ad_not_ready)
                        }
                    }
                )

                if (!shown) {
                    adManager.loadRewardedAd()
                    showAdNotReadyFeedback(R.string.help_unlock_ad_not_ready)
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

        // Determine whether the target slot is empty (requires king/ace fetch)
        val isEmptyTarget = when (targetType) {
            com.gpgamelab.justpatience.model.StackType.TABLEAU ->
                viewModel.game.value.tableau.getOrNull(targetIndex)?.isEmpty() == true
            com.gpgamelab.justpatience.model.StackType.FOUNDATION ->
                viewModel.game.value.foundations.getOrNull(targetIndex)?.isEmpty() == true
            else -> false
        }

        if (isEmptyTarget) {
            val candidates = viewModel.getMagicWandCandidatesForEmpty(targetType, targetIndex)
            setMagicWandSelectionMode(false)
            when {
                candidates.isEmpty() -> {
                    val rankName = if (targetType == com.gpgamelab.justpatience.model.StackType.FOUNDATION)
                        getString(R.string.card_rank_ace) else getString(R.string.card_rank_king)
                    showErrorFeedback(getString(R.string.magic_wand_no_card_available, rankName), binding.gameBoardView)
                }
                candidates.size == 1 -> {
                    val used = viewModel.tryUseMagicWandWithCandidate(targetType, targetIndex, candidates[0])
                    if (used) {
                        consumeMagicWand()
                        playMagicWandSound()
                        performSuccessHaptic(binding.gameBoardView)
                    } else {
                        showErrorFeedback(R.string.magic_wand_no_match, binding.gameBoardView)
                    }
                }
                else -> showMagicWandCardPicker(targetType, targetIndex, candidates)
            }
            return
        }

        val used = viewModel.tryUseMagicWandOnTarget(targetType, targetIndex, targetCardIndex)
        if (used) {
            consumeMagicWand()
            playMagicWandSound()
            performSuccessHaptic(binding.gameBoardView)
        } else {
            showErrorFeedback(R.string.magic_wand_no_match, binding.gameBoardView)
        }
        setMagicWandSelectionMode(false)
    }

    private fun showMagicWandCardPicker(
        targetType: com.gpgamelab.justpatience.model.StackType,
        targetIndex: Int,
        candidates: List<GameViewModel.MagicWandCandidate>
    ) {
        val labels = candidates.map { c ->
            val suit = c.card.suit?.displayName ?: ""
            getString(R.string.magic_wand_card_label_format, c.card.rank.displayName, suit)
        }.toTypedArray()

        val titleRes = if (targetType == com.gpgamelab.justpatience.model.StackType.FOUNDATION)
            R.string.magic_wand_pick_ace_title else R.string.magic_wand_pick_king_title

        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setItems(labels) { _, which ->
                val used = viewModel.tryUseMagicWandWithCandidate(targetType, targetIndex, candidates[which])
                if (used) {
                    consumeMagicWand()
                    playMagicWandSound()
                    performSuccessHaptic(binding.gameBoardView)
                } else {
                    showErrorFeedback(R.string.magic_wand_no_match, binding.gameBoardView)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
        val claimDate = todayIsoDate ?: return

        val baseRewards = selectDailyBonusRewards(isPremiumAccount)
        val adMultiplier = if (isPremiumAccount) 3 else 2

        val popupModel = RewardPopupDialog.PopupModel(
            title = getString(R.string.daily_bonus_title),
            descriptionText = "",
            rewards = listOf(
                RewardPopupDialog.PopupRewardItem(
                    count = baseRewards.gems,
                    imageResId = R.drawable.ic_treasure_3_gem_green
                ),
                RewardPopupDialog.PopupRewardItem(
                    count = baseRewards.tickets,
                    imageResId = R.drawable.ic_ticket_green_yellow_helper
                ),
                RewardPopupDialog.PopupRewardItem(
                    count = baseRewards.wands,
                    imageResId = R.drawable.ic_magic_wand_yellow
                )
            ),
            buttons = listOf(
                RewardPopupDialog.PopupButtonItem(
                    backgroundResId = R.drawable.ic_button_orange_orange_claim,
                    contentDescription = getString(R.string.daily_bonus_claim)
                ),
                RewardPopupDialog.PopupButtonItem(
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

        showUnifiedRewardPopup(
            model = popupModel,
            uiConfig = buildUnifiedRewardPopupUiConfig(RewardPopupStyle.STANDARD),
            isWinPopup = false,
            onButtonClick = { index, dialog ->
                dialog.dismiss()
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
        performSuccessHaptic(binding.root)
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
                    showAdNotReadyFeedback(fallbackMessage)
                }
                claimDailyBonus(rewardsToAward, todayIsoDate)
            }
        )

        if (!shown) {
            showAdNotReadyFeedback(fallbackMessage)
            claimDailyBonus(baseRewards, todayIsoDate)
            adManager.loadRewardedAd()
        }
    }

    private fun showStatsDialog() {
        StatsDialogFragment.newInstance().show(supportFragmentManager, "stats_dialog")
    }

    private fun showPlayPopup(anchor: View) {
        val popupView = layoutInflater.inflate(R.layout.popup_play_menu, null)
        val popup = android.widget.PopupWindow(
            popupView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popup.elevation = 16f

        popupView.findViewById<android.widget.TextView>(R.id.popup_btn_new_game).setOnClickListener {
            popup.dismiss()
            winCelebrationPlayed = false
            startNewGameWithShuffleAndDealAnimation()
        }
        popupView.findViewById<android.widget.TextView>(R.id.popup_btn_restart).setOnClickListener {
            popup.dismiss()
            onHelpControlClicked(HelpControlAction.RESTART, anchor) { handleRestartClick() }
        }
        popupView.findViewById<android.widget.TextView>(R.id.popup_btn_menu).setOnClickListener {
            popup.dismiss()
            showGameMenu()
        }
        popupView.findViewById<android.widget.TextView>(R.id.popup_btn_testers).setOnClickListener {
            popup.dismiss()
            showTesterMenu()
        }
        popupView.findViewById<android.widget.TextView>(R.id.popup_btn_develop).setOnClickListener {
            popup.dismiss()
            showDevelopMenu()
        }

        // Show above the anchor button
        popupView.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        val offsetY = -popupView.measuredHeight - anchor.height
        popup.showAsDropDown(anchor, 0, offsetY)
    }

    private fun applyMirroredLayoutUi(mirrored: Boolean) {
        switchLayoutScopedDevAdjustersIfNeeded(mirrored)
        applyLandscapePileLayoutDevConfigToBoard()
        applyPortraitPileLayoutDevConfigToBoard()
        applyAspectCategoryPileTrimsToBoard()
        applyLandscapeBannerOverlayDevOffsets()
        applyTopHudDevOffsets()

        // Flip the info_side_panel to the opposite side
        val infoPanel = findViewById<android.view.View>(R.id.info_side_panel) ?: return
        val clp = infoPanel.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams ?: return
        val marginPx = dpToPx(6f)
        if (mirrored) {
            clp.startToStart = R.id.game_board_view
            clp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            clp.marginStart = marginPx
            clp.marginEnd = 0
        } else {
            clp.endToEnd = R.id.game_board_view
            clp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            clp.marginEnd = marginPx
            clp.marginStart = 0
        }
        infoPanel.layoutParams = clp

        // Position controls by hand mode: classic on left, mirrored on right.
        // This must target Flow's internal packed-content bias, not the Flow view's own layout bias.
        findViewById<androidx.constraintlayout.helper.widget.Flow?>(R.id.control_buttons_flow)?.let { flow ->
            flow.setHorizontalBias(if (mirrored) 1f else 0f)
            flow.requestLayout()
        }

        // Reverse button order in the Flow helper only when state changes.
         if (appliedMirroredLayout != mirrored) {
             appliedMirroredLayout = mirrored
             val flow = findViewById<androidx.constraintlayout.helper.widget.Flow?>(R.id.control_buttons_flow)
             if (flow != null) {
                 flow.referencedIds = if (mirrored) {
                     intArrayOf(R.id.btn_stats, R.id.magic_wand_container, R.id.btn_hint, R.id.btnRedo, R.id.btnUndo)
                 } else {
                     intArrayOf(R.id.btnUndo, R.id.btnRedo, R.id.btn_hint, R.id.magic_wand_container, R.id.btn_stats)
                 }
                 flow.requestLayout()
             }
         }

        // Portrait banner: align ad left (mirrored) or right (classic) to avoid accidental taps
        val isLandscape = resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
        if (!isLandscape) {
            // Portrait: keep full-width banner box, align ad away from active hand.
            (binding.adView.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.gravity = if (mirrored) (Gravity.START or Gravity.CENTER_VERTICAL) else (Gravity.END or Gravity.CENTER_VERTICAL)
                binding.adView.layoutParams = lp
            }
        } else {
            val bannerOverlay = findViewById<android.widget.FrameLayout?>(R.id.banner_overlay_container)
            if (bannerOverlay != null) {
                val blp = bannerOverlay.layoutParams as?
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                if (blp != null) {
                    if (mirrored) {
                        // Mirrored buttons are on right, so keep ad box on left.
                        blp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                        blp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    } else {
                        // Classic buttons are on left, so keep ad box on right.
                        blp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                        blp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    }
                    bannerOverlay.layoutParams = blp
                }
                applyLandscapeBannerOverlayDevOffsets()
            }
        }
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
                currentSoundOn = currentSettings.soundOn,
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
    override fun devPopupButton0ScaleX(): Float = devPopupButton0ScaleXState
    override fun devPopupButton0ScaleY(): Float = devPopupButton0ScaleYState
    override fun devPopupButton0Scale(): Float = devPopupButton0ScaleState
    override fun devPopupButton1ScaleX(): Float = devPopupButton1ScaleXState
    override fun devPopupButton1ScaleY(): Float = devPopupButton1ScaleYState
    override fun devPopupButton1Scale(): Float = devPopupButton1ScaleState
    override fun devPopupButton2ScaleX(): Float = devPopupButton2ScaleXState
    override fun devPopupButton2ScaleY(): Float = devPopupButton2ScaleYState
    override fun devPopupButton2Scale(): Float = devPopupButton2ScaleState
    override fun devPopupDescriptionTextSizeSp(): Float = devPopupDescriptionTextSizeSpState
    override fun devPopupDescriptionOffsetXDp(): Float = devPopupDescriptionOffsetXDpState
    override fun devPopupDescriptionOffsetYDp(): Float = devPopupDescriptionOffsetYDpState
    override fun devVictoryTextSizeSp(): Float = devVictoryTextSizeSpState
    override fun devVictoryOffsetXDp(): Float = devVictoryOffsetXDpState
    override fun devVictoryOffsetYDp(): Float = devVictoryOffsetYDpState

    // Daily-popup-specific dev getters removed from DevelopMenu host contract.

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
    override fun onDevSetPopupButton0ScaleX(value: Float) { devPopupButton0ScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetPopupButton0ScaleY(value: Float) { devPopupButton0ScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetPopupButton0Scale(value: Float) { devPopupButton0ScaleState = value.coerceAtLeast(0.1f) }
    override fun onDevSetPopupButton1ScaleX(value: Float) { devPopupButton1ScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetPopupButton1ScaleY(value: Float) { devPopupButton1ScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetPopupButton1Scale(value: Float) { devPopupButton1ScaleState = value.coerceAtLeast(0.1f) }
    override fun onDevSetPopupButton2ScaleX(value: Float) { devPopupButton2ScaleXState = value.coerceAtLeast(0.1f) }
    override fun onDevSetPopupButton2ScaleY(value: Float) { devPopupButton2ScaleYState = value.coerceAtLeast(0.1f) }
    override fun onDevSetPopupButton2Scale(value: Float) { devPopupButton2ScaleState = value.coerceAtLeast(0.1f) }
    override fun onDevSetPopupDescriptionTextSize(value: Float) { devPopupDescriptionTextSizeSpState = value.coerceAtLeast(4f) }
    override fun onDevSetPopupDescriptionOffsetX(value: Float) { devPopupDescriptionOffsetXDpState = value }
    override fun onDevSetPopupDescriptionOffsetY(value: Float) { devPopupDescriptionOffsetYDpState = value }
    override fun onDevSetVictoryTextSize(value: Float) { devVictoryTextSizeSpState = value.coerceAtLeast(4f) }
    override fun onDevSetVictoryOffsetX(value: Float) { devVictoryOffsetXDpState = value }
    override fun onDevSetVictoryOffsetY(value: Float) { devVictoryOffsetYDpState = value }

    // Daily-popup-specific dev setters removed from DevelopMenu host contract.

    override fun devLockedPileAdOffsetXPortraitPx(): Float = devLockedPileAdOffsetXPortraitPxState
    override fun devLockedPileAdOffsetYPortraitPx(): Float = devLockedPileAdOffsetYPortraitPxState
    override fun devLockedPileAdScaleXPortrait(): Float = devLockedPileAdScaleXPortraitState
    override fun devLockedPileAdScaleYPortrait(): Float = devLockedPileAdScaleYPortraitState
    override fun devLockedPileAdOffsetXLandscapePx(): Float = devLockedPileAdOffsetXLandscapePxState
    override fun devLockedPileAdOffsetYLandscapePx(): Float = devLockedPileAdOffsetYLandscapePxState
    override fun devLockedPileAdScaleXLandscape(): Float = devLockedPileAdScaleXLandscapeState
    override fun devLockedPileAdScaleYLandscape(): Float = devLockedPileAdScaleYLandscapeState
    override fun devLandscapePileOverallOffsetXDp(): Float = devLandscapePileOverallOffsetXDpState
    override fun devLandscapePileOverallOffsetYDp(): Float = devLandscapePileOverallOffsetYDpState
    override fun devLandscapePileFoundationOffsetXDp(): Float = devLandscapePileFoundationOffsetXDpState
    override fun devLandscapePileFoundationOffsetYDp(): Float = devLandscapePileFoundationOffsetYDpState
    override fun devLandscapePileDrawWasteOffsetXDp(): Float = devLandscapePileDrawWasteOffsetXDpState
    override fun devLandscapePileDrawWasteOffsetYDp(): Float = devLandscapePileDrawWasteOffsetYDpState
    override fun devLandscapePileStockOffsetXDp(): Float = devLandscapePileStockOffsetXDpState
    override fun devLandscapePileStockOffsetYDp(): Float = devLandscapePileStockOffsetYDpState
    override fun devLandscapePileWasteOffsetXDp(): Float = devLandscapePileWasteOffsetXDpState
    override fun devLandscapePileWasteOffsetYDp(): Float = devLandscapePileWasteOffsetYDpState
    override fun devLandscapePileTableauOffsetXDp(): Float = devLandscapePileTableauOffsetXDpState
    override fun devLandscapePileTableauOffsetYDp(): Float = devLandscapePileTableauOffsetYDpState
    override fun devPortraitPileOverallOffsetXDp(): Float = devPortraitPileOverallOffsetXDpState
    override fun devPortraitPileOverallOffsetYDp(): Float = devPortraitPileOverallOffsetYDpState
    override fun devPortraitPileFoundationOffsetXDp(): Float = currentPortraitAspectPileOffsets().foundationOffsetX
    override fun devPortraitPileFoundationOffsetYDp(): Float = currentPortraitAspectPileOffsets().foundationOffsetY
    override fun devPortraitPileDrawWasteOffsetXDp(): Float = currentPortraitAspectPileOffsets().drawWasteOffsetX
    override fun devPortraitPileDrawWasteOffsetYDp(): Float = currentPortraitAspectPileOffsets().drawWasteOffsetY
    override fun devPortraitPileStockOffsetXDp(): Float = devPortraitPileStockOffsetXDpState
    override fun devPortraitPileStockOffsetYDp(): Float = devPortraitPileStockOffsetYDpState
    override fun devPortraitPileWasteOffsetXDp(): Float = devPortraitPileWasteOffsetXDpState
    override fun devPortraitPileWasteOffsetYDp(): Float = devPortraitPileWasteOffsetYDpState
    override fun devPortraitPileTableauOffsetXDp(): Float = devPortraitPileTableauOffsetXDpState
    override fun devPortraitPileTableauOffsetYDp(): Float = devPortraitPileTableauOffsetYDpState
    override fun devLandscapeBannerSmallWidthDp(): Float = devLandscapeBannerSmallWidthDpState
    override fun devLandscapeBannerSmallHeightDp(): Float = devLandscapeBannerSmallHeightDpState
    override fun devLandscapeBannerMediumWidthDp(): Float = devLandscapeBannerMediumWidthDpState
    override fun devLandscapeBannerMediumHeightDp(): Float = devLandscapeBannerMediumHeightDpState
    override fun devLandscapeBannerLargeWidthDp(): Float = devLandscapeBannerLargeWidthDpState
    override fun devLandscapeBannerLargeHeightDp(): Float = devLandscapeBannerLargeHeightDpState
    override fun devSmallDeviceLandscapeBannerOffsetXDp(): Float = devSmallDeviceLandscapeBannerOffsetXDpState
    override fun devSmallDeviceLandscapeBannerOffsetYDp(): Float = devSmallDeviceLandscapeBannerOffsetYDpState
    override fun devMediumDeviceLandscapeBannerOffsetXDp(): Float = devMediumDeviceLandscapeBannerOffsetXDpState
    override fun devMediumDeviceLandscapeBannerOffsetYDp(): Float = devMediumDeviceLandscapeBannerOffsetYDpState
    override fun devLargeDeviceLandscapeBannerOffsetXDp(): Float = devLargeDeviceLandscapeBannerOffsetXDpState
    override fun devLargeDeviceLandscapeBannerOffsetYDp(): Float = devLargeDeviceLandscapeBannerOffsetYDpState
    override fun devScoreboardOffsetXDp(): Float = devScoreboardOffsetXDpState
    override fun devScoreboardOffsetYDp(): Float = devScoreboardOffsetYDpState
    override fun devGemRewardOffsetXDp(): Float = devGemRewardOffsetXDpState
    override fun devGemRewardOffsetYDp(): Float = devGemRewardOffsetYDpState
    override fun devTicketRewardOffsetXDp(): Float = devTicketRewardOffsetXDpState
    override fun devTicketRewardOffsetYDp(): Float = devTicketRewardOffsetYDpState
    override fun devShuffleSecondClipDelayMs(): Float = devShuffleSecondClipDelayMsState
    override fun devShuffleTailDelayMs(): Float = devShuffleTailDelayMsState
    override fun devDealCardIntervalMs(): Float = devDealCardIntervalMsState

    // Aspect-ratio category
    override fun devAspectPortraitSlimXDp(): Float    = devAspectPortraitSlimXDpState
    override fun devAspectPortraitClassicXDp(): Float = devAspectPortraitClassicXDpState
    override fun devAspectPortraitBroadXDp(): Float   = devAspectPortraitBroadXDpState
    override fun devAspectPortraitSquareXDp(): Float  = devAspectPortraitSquareXDpState
    override fun devAspectPortraitSlimYDp(): Float    = devAspectPortraitSlimYDpState
    override fun devAspectPortraitClassicYDp(): Float = devAspectPortraitClassicYDpState
    override fun devAspectPortraitBroadYDp(): Float   = devAspectPortraitBroadYDpState
    override fun devAspectPortraitSquareYDp(): Float  = devAspectPortraitSquareYDpState
    override fun devAspectLandscapeSlimXDp(): Float    = devAspectLandscapeSlimXDpState
    override fun devAspectLandscapeClassicXDp(): Float = devAspectLandscapeClassicXDpState
    override fun devAspectLandscapeBroadXDp(): Float   = devAspectLandscapeBroadXDpState
    override fun devAspectLandscapeSquareXDp(): Float  = devAspectLandscapeSquareXDpState
    override fun devAspectLandscapeSlimYDp(): Float    = devAspectLandscapeSlimYDpState
    override fun devAspectLandscapeClassicYDp(): Float = devAspectLandscapeClassicYDpState
    override fun devAspectLandscapeBroadYDp(): Float   = devAspectLandscapeBroadYDpState
    override fun devAspectLandscapeSquareYDp(): Float  = devAspectLandscapeSquareYDpState
    override fun devCurrentAspectCategoryLabel(): String =
        binding.gameBoardView.getCurrentAspectCategory().name

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
    override fun onDevSetLandscapePileOverallOffsetX(value: Float) {
        devLandscapePileOverallOffsetXDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileOverallOffsetY(value: Float) {
        devLandscapePileOverallOffsetYDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileFoundationOffsetX(value: Float) {
        devLandscapePileFoundationOffsetXDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileFoundationOffsetY(value: Float) {
        devLandscapePileFoundationOffsetYDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileDrawWasteOffsetX(value: Float) {
        devLandscapePileDrawWasteOffsetXDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileDrawWasteOffsetY(value: Float) {
        devLandscapePileDrawWasteOffsetYDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileStockOffsetX(value: Float) {
        devLandscapePileStockOffsetXDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileStockOffsetY(value: Float) {
        devLandscapePileStockOffsetYDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileWasteOffsetX(value: Float) {
        devLandscapePileWasteOffsetXDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileWasteOffsetY(value: Float) {
        devLandscapePileWasteOffsetYDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileTableauOffsetX(value: Float) {
        devLandscapePileTableauOffsetXDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileTableauOffsetY(value: Float) {
        devLandscapePileTableauOffsetYDpState = value
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileOverallOffsetX(value: Float) {
        devPortraitPileOverallOffsetXDpState = value
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileOverallOffsetY(value: Float) {
        devPortraitPileOverallOffsetYDpState = value
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileFoundationOffsetX(value: Float) {
        setCurrentPortraitAspectPileOffsets(currentPortraitAspectPileOffsets().copy(foundationOffsetX = value))
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileFoundationOffsetY(value: Float) {
        setCurrentPortraitAspectPileOffsets(currentPortraitAspectPileOffsets().copy(foundationOffsetY = value))
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileDrawWasteOffsetX(value: Float) {
        setCurrentPortraitAspectPileOffsets(currentPortraitAspectPileOffsets().copy(drawWasteOffsetX = value))
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileDrawWasteOffsetY(value: Float) {
        setCurrentPortraitAspectPileOffsets(currentPortraitAspectPileOffsets().copy(drawWasteOffsetY = value))
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileStockOffsetX(value: Float) {
        devPortraitPileStockOffsetXDpState = value
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileStockOffsetY(value: Float) {
        devPortraitPileStockOffsetYDpState = value
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileWasteOffsetX(value: Float) {
        devPortraitPileWasteOffsetXDpState = value
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileWasteOffsetY(value: Float) {
        devPortraitPileWasteOffsetYDpState = value
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileTableauOffsetX(value: Float) {
        devPortraitPileTableauOffsetXDpState = value
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileTableauOffsetY(value: Float) {
        devPortraitPileTableauOffsetYDpState = value
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapeBannerSmallWidthDp(value: Float) {
        devLandscapeBannerSmallWidthDpState = value.coerceAtLeast(1f)
        reloadBannerForCurrentConfiguration()
    }
    override fun onDevSetLandscapeBannerSmallHeightDp(value: Float) {
        devLandscapeBannerSmallHeightDpState = value.coerceAtLeast(1f)
        reloadBannerForCurrentConfiguration()
    }
    override fun onDevSetLandscapeBannerMediumWidthDp(value: Float) {
        devLandscapeBannerMediumWidthDpState = value.coerceAtLeast(1f)
        reloadBannerForCurrentConfiguration()
    }
    override fun onDevSetLandscapeBannerMediumHeightDp(value: Float) {
        devLandscapeBannerMediumHeightDpState = value.coerceAtLeast(1f)
        reloadBannerForCurrentConfiguration()
    }
    override fun onDevSetLandscapeBannerLargeWidthDp(value: Float) {
        devLandscapeBannerLargeWidthDpState = value.coerceAtLeast(1f)
        reloadBannerForCurrentConfiguration()
    }
    override fun onDevSetLandscapeBannerLargeHeightDp(value: Float) {
        devLandscapeBannerLargeHeightDpState = value.coerceAtLeast(1f)
        reloadBannerForCurrentConfiguration()
    }
    override fun onDevSetSmallDeviceLandscapeBannerOffsetX(value: Float) {
        devSmallDeviceLandscapeBannerOffsetXDpState = value
        applyLandscapeBannerOverlayDevOffsets()
    }
    override fun onDevSetSmallDeviceLandscapeBannerOffsetY(value: Float) {
        devSmallDeviceLandscapeBannerOffsetYDpState = value
        applyLandscapeBannerOverlayDevOffsets()
    }
    override fun onDevSetMediumDeviceLandscapeBannerOffsetX(value: Float) {
        devMediumDeviceLandscapeBannerOffsetXDpState = value
        applyLandscapeBannerOverlayDevOffsets()
    }
    override fun onDevSetMediumDeviceLandscapeBannerOffsetY(value: Float) {
        devMediumDeviceLandscapeBannerOffsetYDpState = value
        applyLandscapeBannerOverlayDevOffsets()
    }
    override fun onDevSetLargeDeviceLandscapeBannerOffsetX(value: Float) {
        devLargeDeviceLandscapeBannerOffsetXDpState = value
        applyLandscapeBannerOverlayDevOffsets()
    }
    override fun onDevSetLargeDeviceLandscapeBannerOffsetY(value: Float) {
        devLargeDeviceLandscapeBannerOffsetYDpState = value
        applyLandscapeBannerOverlayDevOffsets()
    }
    override fun onDevSetScoreboardOffsetX(value: Float) {
        devScoreboardOffsetXDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetScoreboardOffsetY(value: Float) {
        devScoreboardOffsetYDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetGemRewardOffsetX(value: Float) {
        devGemRewardOffsetXDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetGemRewardOffsetY(value: Float) {
        devGemRewardOffsetYDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetTicketRewardOffsetX(value: Float) {
        devTicketRewardOffsetXDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetTicketRewardOffsetY(value: Float) {
        devTicketRewardOffsetYDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetAspectPortraitSlimY(value: Float) {
        devAspectPortraitSlimYDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectPortraitClassicY(value: Float) {
        devAspectPortraitClassicYDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectPortraitBroadY(value: Float) {
        devAspectPortraitBroadYDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectPortraitSquareY(value: Float) {
        devAspectPortraitSquareYDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectLandscapeSlimY(value: Float) {
        devAspectLandscapeSlimYDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectLandscapeClassicY(value: Float) {
        devAspectLandscapeClassicYDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectLandscapeBroadY(value: Float) {
        devAspectLandscapeBroadYDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectLandscapeSquareY(value: Float) {
        devAspectLandscapeSquareYDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectPortraitSlimX(value: Float) {
        devAspectPortraitSlimXDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectPortraitClassicX(value: Float) {
        devAspectPortraitClassicXDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectPortraitBroadX(value: Float) {
        devAspectPortraitBroadXDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectPortraitSquareX(value: Float) {
        devAspectPortraitSquareXDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectLandscapeSlimX(value: Float) {
        devAspectLandscapeSlimXDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectLandscapeClassicX(value: Float) {
        devAspectLandscapeClassicXDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectLandscapeBroadX(value: Float) {
        devAspectLandscapeBroadXDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetAspectLandscapeSquareX(value: Float) {
        devAspectLandscapeSquareXDpState = value
        applyAspectCategoryPileTrimsToBoard()
    }
    override fun onDevSetShuffleSecondClipDelayMs(value: Float) { devShuffleSecondClipDelayMsState = value.coerceAtLeast(0f) }
    override fun onDevSetShuffleTailDelayMs(value: Float) { devShuffleTailDelayMsState = value.coerceAtLeast(0f) }
    override fun onDevSetDealCardIntervalMs(value: Float) { devDealCardIntervalMsState = value.coerceAtLeast(0f) }

    override fun onDevApplyAutoWinPopupRatios() {
        applyAutoWinPopupRatios()
    }

    // Daily-popup auto-adjust action removed from DevelopMenu host contract.

    override fun onDevExpandStateChanged(state: DevelopMenuDialogFragment.ExpandState) {
        developMenuExpandState = state
        sessionDevelopMenuExpandState = state
    }

    // ------------------------------------------------------------------

    override fun onTesterTriggerWinSequence() {
        // Reset win-flow guards so the sequence runs even if a win was already shown.
        winCelebrationPlayed = false
        winDialogShowing = false
        showGameEndDialog()
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
            showErrorFeedback(R.string.share_app_no_target)
            return
        }

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app_chooser_title)))
        } catch (_: android.content.ActivityNotFoundException) {
            showErrorFeedback(R.string.share_app_no_target)
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
            showErrorFeedback(R.string.contact_us_no_target)
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

    override fun onGameMenuSoundToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showOnOffDialog(
                title = getString(R.string.game_menu_sound),
                isOn = currentSettings.soundOn
            ) { isOn ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(soundOn = isOn))
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
            showOnOffDialog(
                title = getString(R.string.game_menu_haptics),
                isOn = currentSettings.haptics
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

    private fun showOnOffDialog(
        title: String,
        isOn: Boolean,
        onSelection: (Boolean) -> Unit
    ) {
        val options = arrayOf(
            getString(R.string.setting_state_on),
            getString(R.string.setting_state_off)
        )
        val checkedItem = if (isOn) 0 else 1

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
            showErrorFeedback(R.string.privacy_policy_open_failed)
        }
    }

    private fun openTermsOfServiceWebsite(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            showErrorFeedback(R.string.terms_of_service_open_failed)
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
        if (::adManager.isInitialized) adManager.cancelBannerRetry()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyImmersiveFullscreen()
        applyResponsiveControlSizing()
        applyMirroredLayoutUi(viewModel.isMirroredLayout.value)
        applyAutoWinPopupRatios()
        if (testerStarburstAutoLayoutEnabled) {
            applyAutoStarburstProfile()
            refreshActiveStarburstDebugAndMotion()
        }
        applyLockedPileAdIconDevConfigToBoard()
        applyLandscapePileLayoutDevConfigToBoard()
        applyPortraitPileLayoutDevConfigToBoard()
        applyLandscapeBannerOverlayDevOffsets()
        applyTopHudDevOffsets()
        reloadBannerForCurrentConfiguration()
    }

    private fun onLockedTableauUnlockRequested() {
        if (viewModel.game.value.extraTableauUnlocked) return
        viewModel.pauseHintTimerForNonPlayerActivity()
        val shown = adManager.showRewardedAd(
            onFinished = { rewardEarned ->
                lifecycleScope.launch {
                    if (rewardEarned) {
                        viewModel.unlockExtraTableauPile()
                        settingsManager.incrementExtraTableauUnlockedByAd()
                    } else {
                        showAdNotReadyFeedback(R.string.help_unlock_ad_not_ready)
                    }
                }
            }
        )
        if (!shown) {
            adManager.loadRewardedAd()
            showAdNotReadyFeedback(R.string.help_unlock_ad_not_ready)
        }
    }

    private fun applyLockedPileAdIconDevConfigToBoard() {
        val ratioProfile = calculateDevOffsetRatioProfile()
        binding.gameBoardView.setLockedPileAdIconTuning(
            portraitOffsetX = scaleDevPxOffsetX(devLockedPileAdOffsetXPortraitPxState, ratioProfile),
            portraitOffsetY = scaleDevPxOffsetY(devLockedPileAdOffsetYPortraitPxState, ratioProfile),
            portraitScaleX = devLockedPileAdScaleXPortraitState,
            portraitScaleY = devLockedPileAdScaleYPortraitState,
            landscapeOffsetX = scaleDevPxOffsetX(devLockedPileAdOffsetXLandscapePxState, ratioProfile),
            landscapeOffsetY = scaleDevPxOffsetY(devLockedPileAdOffsetYLandscapePxState, ratioProfile),
            landscapeScaleX = devLockedPileAdScaleXLandscapeState,
            landscapeScaleY = devLockedPileAdScaleYLandscapeState
        )
    }

    private fun applyAspectCategoryPileTrimsToBoard() {
        binding.gameBoardView.setAspectCategoryPileXTrimsDp(
            portraitSlimDp    = devAspectPortraitSlimXDpState,
            portraitClassicDp = devAspectPortraitClassicXDpState,
            portraitBroadDp   = devAspectPortraitBroadXDpState,
            portraitSquareDp  = devAspectPortraitSquareXDpState,
            landscapeSlimDp   = devAspectLandscapeSlimXDpState,
            landscapeClassicDp = devAspectLandscapeClassicXDpState,
            landscapeBroadDp  = devAspectLandscapeBroadXDpState,
            landscapeSquareDp = devAspectLandscapeSquareXDpState
        )
        binding.gameBoardView.setAspectCategoryPileYTrimsDp(
            portraitSlimDp    = devAspectPortraitSlimYDpState,
            portraitClassicDp = devAspectPortraitClassicYDpState,
            portraitBroadDp   = devAspectPortraitBroadYDpState,
            portraitSquareDp  = devAspectPortraitSquareYDpState,
            landscapeSlimDp   = devAspectLandscapeSlimYDpState,
            landscapeClassicDp = devAspectLandscapeClassicYDpState,
            landscapeBroadDp  = devAspectLandscapeBroadYDpState,
            landscapeSquareDp = devAspectLandscapeSquareYDpState
        )
    }

    private fun applyLandscapePileLayoutDevConfigToBoard() {
        persistActiveLayoutScopedDevAdjusters(viewModel.isMirroredLayout.value)
        val ratioProfile = calculateDevOffsetRatioProfile()
        binding.gameBoardView.setLandscapePileLayoutTuning(
            overallOffsetX = scaleDevDpOffsetX(devLandscapePileOverallOffsetXDpState, ratioProfile),
            overallOffsetY = scaleDevDpOffsetY(devLandscapePileOverallOffsetYDpState, ratioProfile),
            foundationOffsetX = scaleDevDpOffsetX(devLandscapePileFoundationOffsetXDpState, ratioProfile),
            foundationOffsetY = scaleDevDpOffsetY(devLandscapePileFoundationOffsetYDpState, ratioProfile),
            drawWasteOffsetX = scaleDevDpOffsetX(devLandscapePileDrawWasteOffsetXDpState, ratioProfile),
            drawWasteOffsetY = scaleDevDpOffsetY(devLandscapePileDrawWasteOffsetYDpState, ratioProfile),
            stockOffsetX = scaleDevDpOffsetX(devLandscapePileStockOffsetXDpState, ratioProfile),
            stockOffsetY = scaleDevDpOffsetY(devLandscapePileStockOffsetYDpState, ratioProfile),
            wasteOffsetX = scaleDevDpOffsetX(devLandscapePileWasteOffsetXDpState, ratioProfile),
            wasteOffsetY = scaleDevDpOffsetY(devLandscapePileWasteOffsetYDpState, ratioProfile),
            tableauOffsetX = scaleDevDpOffsetX(devLandscapePileTableauOffsetXDpState, ratioProfile),
            tableauOffsetY = scaleDevDpOffsetY(devLandscapePileTableauOffsetYDpState, ratioProfile)
        )
        binding.gameBoardView.dumpPileLayoutDebug("applyLandscapePileLayoutDevConfigToBoard")
    }

    private fun applyPortraitPileLayoutDevConfigToBoard() {
        persistActiveLayoutScopedDevAdjusters(viewModel.isMirroredLayout.value)
        val ratioProfile = calculateDevOffsetRatioProfile()
        val aspectOffsets = currentPortraitAspectPileOffsets()
        binding.gameBoardView.setPortraitPileLayoutTuning(
            overallOffsetX = scaleDevDpOffsetX(devPortraitPileOverallOffsetXDpState, ratioProfile),
            overallOffsetY = scaleDevDpOffsetY(devPortraitPileOverallOffsetYDpState, ratioProfile),
            foundationOffsetX = scaleDevDpOffsetX(aspectOffsets.foundationOffsetX, ratioProfile),
            foundationOffsetY = scaleDevDpOffsetY(aspectOffsets.foundationOffsetY, ratioProfile),
            drawWasteOffsetX = scaleDevDpOffsetX(aspectOffsets.drawWasteOffsetX, ratioProfile),
            drawWasteOffsetY = scaleDevDpOffsetY(aspectOffsets.drawWasteOffsetY, ratioProfile),
            stockOffsetX = scaleDevDpOffsetX(devPortraitPileStockOffsetXDpState, ratioProfile),
            stockOffsetY = scaleDevDpOffsetY(devPortraitPileStockOffsetYDpState, ratioProfile),
            wasteOffsetX = scaleDevDpOffsetX(devPortraitPileWasteOffsetXDpState, ratioProfile),
            wasteOffsetY = scaleDevDpOffsetY(devPortraitPileWasteOffsetYDpState, ratioProfile),
            tableauOffsetX = scaleDevDpOffsetX(devPortraitPileTableauOffsetXDpState, ratioProfile),
            tableauOffsetY = scaleDevDpOffsetY(devPortraitPileTableauOffsetYDpState, ratioProfile)
        )
        binding.gameBoardView.dumpPileLayoutDebug("applyPortraitPileLayoutDevConfigToBoard")
    }

    private fun applyLandscapeBannerOverlayDevOffsets() {
        persistActiveLayoutScopedDevAdjusters(viewModel.isMirroredLayout.value)
        val ratioProfile = calculateDevOffsetRatioProfile()
        val isLandscapeNow = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val bannerOverlay = findViewById<android.widget.FrameLayout?>(R.id.banner_overlay_container) ?: return
        if (!isLandscapeNow) {
            bannerOverlay.translationX = 0f
            bannerOverlay.translationY = 0f
            return
        }
        val (offsetXDp, offsetYDp) = when (resolveLandscapeBannerTier()) {
            LandscapeBannerTier.SMALL -> devSmallDeviceLandscapeBannerOffsetXDpState to devSmallDeviceLandscapeBannerOffsetYDpState
            LandscapeBannerTier.MEDIUM -> devMediumDeviceLandscapeBannerOffsetXDpState to devMediumDeviceLandscapeBannerOffsetYDpState
            LandscapeBannerTier.LARGE -> devLargeDeviceLandscapeBannerOffsetXDpState to devLargeDeviceLandscapeBannerOffsetYDpState
        }
        bannerOverlay.translationX = scaleDevDpOffsetX(offsetXDp, ratioProfile)
        bannerOverlay.translationY = scaleDevDpOffsetY(offsetYDp, ratioProfile)
    }

    private fun applyTopHudDevOffsets() {
        persistActiveLayoutScopedDevAdjusters(viewModel.isMirroredLayout.value)
        val ratioProfile = calculateDevOffsetRatioProfile()
        binding.boardScoreboardContainer.translationX = scaleDevDpOffsetX(devScoreboardOffsetXDpState, ratioProfile)
        binding.boardScoreboardContainer.translationY = scaleDevDpOffsetY(devScoreboardOffsetYDpState, ratioProfile)
        binding.gemsContainer.translationX = scaleDevDpOffsetX(devGemRewardOffsetXDpState, ratioProfile)
        binding.gemsContainer.translationY = scaleDevDpOffsetY(devGemRewardOffsetYDpState, ratioProfile)
        binding.ticketsContainer.translationX = scaleDevDpOffsetX(devTicketRewardOffsetXDpState, ratioProfile)
        binding.ticketsContainer.translationY = scaleDevDpOffsetY(devTicketRewardOffsetYDpState, ratioProfile)
    }

    private fun resolveLandscapeBannerTier(): LandscapeBannerTier {
        val swDp = resources.configuration.smallestScreenWidthDp
        return when {
            swDp == SMALLEST_SCREEN_WIDTH_DP_UNDEFINED -> LandscapeBannerTier.SMALL
            swDp < 400 -> LandscapeBannerTier.SMALL
            swDp >= 800 -> LandscapeBannerTier.LARGE
            else -> LandscapeBannerTier.MEDIUM
        }
    }

    private fun configureLandscapeBannerBoxAndResolveAdSizes(): List<AdSize> {
        val container = findViewById<FrameLayout?>(R.id.banner_overlay_container)
        val isLandscapeNow = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (!isLandscapeNow || container == null) {
            Log.d(
                "GameActivityAds",
                "Landscape banner - isLandscapeNow=${isLandscapeNow} container=${container}"
            )
            return listOf(AdSize.LARGE_BANNER)
        }

        val tier = resolveLandscapeBannerTier()
        val (boxWidthDp, boxHeightDp) = when (tier) {
            LandscapeBannerTier.SMALL -> devLandscapeBannerSmallWidthDpState to devLandscapeBannerSmallHeightDpState
            LandscapeBannerTier.MEDIUM -> devLandscapeBannerMediumWidthDpState to devLandscapeBannerMediumHeightDpState
            LandscapeBannerTier.LARGE -> devLandscapeBannerLargeWidthDpState to devLandscapeBannerLargeHeightDpState
        }

        val lp = container.layoutParams ?: return emptyList()
        val targetWidthPx = dpToPx(boxWidthDp)
        val targetHeightPx = dpToPx(boxHeightDp)
        if (lp.width != targetWidthPx || lp.height != targetHeightPx) {
            lp.width = targetWidthPx
            lp.height = targetHeightPx
            container.layoutParams = lp
            container.requestLayout()
        }

        Log.d(
            "GameActivityAds",
            "Landscape banner tier=$tier boxDp=${boxWidthDp}x${boxHeightDp} boxPx=${targetWidthPx}x${targetHeightPx}"
        )

        // Return one fixed size; AdView size is XML-fixed and cannot be changed at runtime.
        return when (tier) {
            LandscapeBannerTier.SMALL  -> listOf(AdSize.BANNER)
            LandscapeBannerTier.MEDIUM -> listOf(AdSize.LARGE_BANNER)
            LandscapeBannerTier.LARGE  -> listOf(AdSize.MEDIUM_RECTANGLE)
        }
    }

    private fun resolveActiveBannerAdView(requestedAdSizes: List<AdSize>): AdView {
        val isLandscapeNow = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (!isLandscapeNow) {
            return binding.adView
        }

        val largeView = findViewById<AdView?>(R.id.adView)
        val mediumView = findViewById<AdView?>(R.id.adViewBannerMedium)
        val smallView = findViewById<AdView?>(R.id.adViewBannerSmall)
        val primary = requestedAdSizes.firstOrNull()

        val selected = when {
            primary?.width == AdSize.BANNER.width && primary.height == AdSize.BANNER.height -> smallView
            primary?.width == AdSize.LARGE_BANNER.width && primary.height == AdSize.LARGE_BANNER.height -> mediumView
            else -> largeView
        } ?: binding.adView

        listOfNotNull(largeView, mediumView, smallView).forEach { view ->
            view.visibility = if (view === selected) View.VISIBLE else View.GONE
        }
        return selected
    }

    private fun reloadBannerForCurrentConfiguration() {
        if (!::adManager.isInitialized) return
        val requestedAdSizes = configureLandscapeBannerBoxAndResolveAdSizes()
        val bannerAdView = resolveActiveBannerAdView(requestedAdSizes)
        val container = findViewById<FrameLayout?>(R.id.banner_overlay_container)
        val isLandscapeNow = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscapeNow && container != null) {
            container.post {
                adManager.loadBannerAd(bannerAdView, requestedAdSizes)
            }
            return
        }
        adManager.loadBannerAd(bannerAdView, requestedAdSizes)
    }

    private fun applyImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun onAutoMoveClicked(buttonView: View) {
        performUiActionHaptic(buttonView)
        if (helpControlFlowInProgress) return
        val control = HelpControlAction.AUTO

        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val unlockExpiry = settingsManager.getHelpControlUnlockExpiry(control.storageKey)
            val isFree = unlockExpiry > now

            if (!isFree && ticketTotal <= 0) {
                showNoCouponsDialog(control) {
                    // user unlocked via ad — run auto move without extra consume
                    buttonView.isEnabled = false
                    lifecycleScope.launch {
                        try {
                            val movesMade = viewModel.performAutoMove(onCardMoved = { playCardClickMoveSound() })
                            if (movesMade == 0) {
                                showErrorFeedback(R.string.no_moves_available, buttonView)
                            }
                        } finally {
                            buttonView.isEnabled = true
                        }
                    }
                }
                return@launch
            }

            // Run the auto move first, then conditionally consume a ticket
            buttonView.isEnabled = false
            val movesMade: Int
            try {
                movesMade = viewModel.performAutoMove(onCardMoved = { playCardClickMoveSound() })
            } finally {
                buttonView.isEnabled = true
            }

            if (movesMade == 0) {
                showErrorFeedback(R.string.no_moves_available, buttonView)
                return@launch
            }

            performSuccessHaptic(buttonView)

            // Moves were made — consume a ticket (unless free period)
            if (!isFree) {
                animateAndConsumeHelpCoupon(buttonView, control)
            }
        }
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
                        animateAndConsumeHelpCoupon(targetView, control)
                        action()
                        return@launch
                    }
                }
            }

            showNoCouponsDialog(control, action)
        }
    }

    private suspend fun consumeHelpCoupon(control: HelpControlAction) {
        ticketTotal = (ticketTotal - 1).coerceAtLeast(0)
        renderTicketHud(ticketTotal)
        settingsManager.setTotalTickets(ticketTotal)
        settingsManager.incrementCouponUsedForHelpControl(control.storageKey)
    }

    private suspend fun animateAndConsumeHelpCoupon(targetView: View?, control: HelpControlAction) {
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
        consumeHelpCoupon(control)
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
        val model = RewardPopupDialog.PopupModel(
            title = "${control.titleLabel} UNLOCK",
            descriptionText = getString(R.string.help_unlock_ad_only_description_format, unlockHours),
            rewards = emptyList(),
            buttons = listOf(
                RewardPopupDialog.PopupButtonItem(
                    backgroundResId = R.drawable.ic_button_orange_orange_ad_unlock,
                    contentDescription = getString(R.string.help_unlock_watch_ad)
                ),
                RewardPopupDialog.PopupButtonItem(
                    backgroundResId = R.drawable.ic_button_orange_orange_cancel,
                    contentDescription = getString(R.string.cancel)
                )
            ),
            showStarburst = false
        )
        val unlockUiConfig = buildUnifiedRewardPopupUiConfig(RewardPopupStyle.STANDARD)

        rewardPopupDialog.showPopup(
            model = model,
            baseImageResId = R.drawable.ic_popup_rect_blue,
            uiConfig = unlockUiConfig,
            isCancelable = true,
            cancelOnTouchOutside = true,
            onButtonClick = { index, dialog ->
                when (index) {
                    0 -> {
                        if (helpControlFlowInProgress) return@showPopup
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
                                        showAdNotReadyFeedback(R.string.help_unlock_ad_not_ready)
                                    }
                                }
                            }
                        )

                        if (!shown) {
                            helpControlFlowInProgress = false
                            adManager.loadRewardedAd()
                            showAdNotReadyFeedback(R.string.help_unlock_ad_not_ready)
                        }
                    }

                    else -> dialog.dismiss()
                }
            }
        )
    }

    private suspend fun unlockHelpControl(control: HelpControlAction, hours: Int) {
        val durationMillis = hours.coerceAtLeast(1) * 60L * 60L * 1000L
        val expiry = System.currentTimeMillis() + durationMillis
        settingsManager.setHelpControlUnlockExpiry(control.storageKey, expiry)
        settingsManager.incrementAdUnlockForHelpControl(control.storageKey)
    }

    private fun handleUndoClick() {
        if (viewModel.undo()) {
            playCardClickMoveSound()
            performSuccessHaptic(binding.btnUndo)
        } else {
            performErrorHaptic(binding.btnUndo)
        }
    }

    private fun handleRedoClick() {
        if (viewModel.redo()) {
            playCardClickMoveSound()
            performSuccessHaptic(binding.btnRedo)
        } else {
            performErrorHaptic(binding.btnRedo)
        }
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
        val widthScale = factors.horizontal
        val heightScale = factors.vertical
        val textScale = factors.text

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
        applyTopHudDevOffsets()
    }

    private fun applyTopHudSizing(
        isLandscape: Boolean,
        widthScale: Float,
        heightScale: Float,
        textScale: Float
    ) {
        val boardScale = ((widthScale + heightScale) * 0.5f)
        val scoreboardWidthDp = 120f * widthScale
        val scoreboardPaddingDp = 3f * boardScale
        val scoreboardMarginTopDp = 3f * heightScale
        val scoreboardMarginEndDp = 4f * widthScale
        val rowHeightDp = (if (isLandscape) 60f else 20f) * heightScale
        val rowGapDp = 2f * heightScale
        val rowHorizontalPaddingDp = 6f * widthScale
        val labelTextSp = 11f * textScale
        val valueTextSp = 13f * textScale

        resizeFrame(
            binding.boardScoreboardContainer,
            dpToPx(scoreboardWidthDp),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.boardScoreboardContainer.setPadding(
            dpToPx(scoreboardPaddingDp),
            dpToPx(scoreboardPaddingDp),
            dpToPx(scoreboardPaddingDp),
            dpToPx(scoreboardPaddingDp)
        )
        (binding.boardScoreboardContainer.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
            lp.topMargin = dpToPx(scoreboardMarginTopDp)
            lp.marginEnd = dpToPx(scoreboardMarginEndDp)
            binding.boardScoreboardContainer.layoutParams = lp
        }

        val rows = listOf(binding.rowTimer, binding.rowMoves, binding.rowScore)
        rows.forEachIndexed { index, row ->
            val lp = row.layoutParams
            lp.height = dpToPx(rowHeightDp)
            if (lp is ViewGroup.MarginLayoutParams) {
                lp.bottomMargin = if (index == rows.lastIndex) 0 else dpToPx(rowGapDp)
            }
            row.layoutParams = lp
            row.setPadding(dpToPx(rowHorizontalPaddingDp), 0, dpToPx(rowHorizontalPaddingDp), 0)
        }

        listOf(binding.tvTimerLabel, binding.tvMovesLabel, binding.tvScoreLabel).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, labelTextSp)
        }
        listOf(binding.tvTimerValue, binding.tvMovesValue, binding.tvScoreValue).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, valueTextSp)
        }

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

        // Portrait keeps the original horizontal row; landscape stacks wand -> tickets -> gems.
        binding.boardCurrencyHudContainer.orientation = if (isLandscape) {
            LinearLayout.VERTICAL
        } else {
            LinearLayout.HORIZONTAL
        }
        binding.boardCurrencyHudContainer.gravity = if (isLandscape) {
            Gravity.END
        } else {
            Gravity.CENTER_VERTICAL
        }

        // Gem count number: use signed conversion. dpToPx() clamps negatives to +1,
        // so it cannot be used for upward offsets.
        val gemCountTranslationY = if (isLandscape) {
            dpToPxFloatSigned(-14f * heightScale)
        } else {
            dpToPxFloatSigned(-8f * heightScale)
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
            lp.bottomMargin = if (isLandscape) dpToPx(4f * heightScale) else 0
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
                lp.marginEnd = if (isLandscape) 0 else dpToPx(8f * widthScale)
                lp.bottomMargin = if (isLandscape) dpToPx(4f * heightScale) else 0
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
            lp.bottomMargin = 0
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

        val ratioProfile = BaselineResolutionScaleUtil.calculateAverageRatio(
            context = this,
            baselinePortraitWidthPx = WIN_POPUP_BASELINE_PORTRAIT_WIDTH_PX,
            baselinePortraitHeightPx = WIN_POPUP_BASELINE_PORTRAIT_HEIGHT_PX
        )

        if (!isLandscape) {
            // Portrait uses fixed button widths; portraitButtonWidthScale strips the extreme-aspect
            // compression so the existing portraitWidthBoost is not double-penalised.
            setButtonSizeDp(binding.btnNewGame, 50f * portraitWidthBoost * portraitButtonWidthScale * ratioProfile.averageRatio, 50f * controlHeightScale)
            setButtonSizeDp(binding.btnStats, 50f * portraitWidthBoost * portraitButtonWidthScale * ratioProfile.averageRatio, 50f * controlHeightScale)
            setButtonSizeDp(btnHint, 50f * portraitWidthBoost * portraitButtonWidthScale * ratioProfile.averageRatio, 50f * controlHeightScale)
            setButtonSizeDp(btnRestart, 67f * portraitWidthBoost * portraitButtonWidthScale * ratioProfile.averageRatio, 50f * controlHeightScale)
        }

        btnAuto?.let { autoButton ->
            val autoLp = autoButton.layoutParams
            if (autoLp != null) {
                autoLp.width = 0
                autoLp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                if (autoLp is ViewGroup.MarginLayoutParams) {
                    autoLp.marginStart = dpToPx(24f * widthScale)
                    autoLp.marginEnd = dpToPx(24f * widthScale)
                    autoLp.bottomMargin = dpToPx(6f * heightScale)
                }
                autoButton.layoutParams = autoLp
            }
            autoButton.minHeight = 0
            autoButton.minimumHeight = 0
            autoButton.minWidth = 0
            autoButton.minimumWidth = 0
            autoButton.maxLines = 1
            autoButton.setSingleLine(true)
        }

        applyButtonScale(binding.btnNewGame, controlTextSp, textScale * ratioProfile.averageRatio)
        applyButtonScale(binding.btnStats, controlTextSp, textScale * ratioProfile.averageRatio)
        btnHint?.let { applyButtonScale(it, controlTextSp, textScale * ratioProfile.averageRatio) }
        btnRestart?.let { applyButtonScale(it, controlTextSp, textScale * ratioProfile.averageRatio) }
        btnAuto?.let {
            applyButtonScale(it, controlTextSp, textScale * ratioProfile.averageRatio)
            it.minWidth = 0
            it.minimumWidth = 0
        }

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

        binding.btnStats.iconSize = dpToPx(48f * textScale * ratioProfile.averageRatio)
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

    private fun calculateDevOffsetRatioProfile(): BaselineResolutionScaleUtil.ResolutionRatioProfile {
        return BaselineResolutionScaleUtil.calculateAverageRatio(
            context = this,
            baselinePortraitWidthPx = WIN_POPUP_BASELINE_PORTRAIT_WIDTH_PX,
            baselinePortraitHeightPx = WIN_POPUP_BASELINE_PORTRAIT_HEIGHT_PX
        )
    }

    private fun scaleDevDpOffsetX(
        dp: Float,
        ratioProfile: BaselineResolutionScaleUtil.ResolutionRatioProfile
    ): Float = dpToPxFloatSigned(dp * ratioProfile.widthRatio)

    private fun scaleDevDpOffsetY(
        dp: Float,
        ratioProfile: BaselineResolutionScaleUtil.ResolutionRatioProfile
    ): Float = dpToPxFloatSigned(dp * ratioProfile.heightRatio)

    private fun scaleDevPxOffsetX(
        px: Float,
        ratioProfile: BaselineResolutionScaleUtil.ResolutionRatioProfile
    ): Float = px * ratioProfile.widthRatio

    private fun scaleDevPxOffsetY(
        px: Float,
        ratioProfile: BaselineResolutionScaleUtil.ResolutionRatioProfile
    ): Float = px * ratioProfile.heightRatio

    private fun getUsableWindowSizePx(): Pair<Int, Int> {
        val bounds = windowManager.currentWindowMetrics.bounds
        val insets = windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
            android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout()
        )
        val usableWidth = (bounds.width() - insets.left - insets.right).coerceAtLeast(1)
        val usableHeight = (bounds.height() - insets.top - insets.bottom).coerceAtLeast(1)
        return usableWidth to usableHeight
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
        performSuccessHaptic(binding.root)
        showGameEndDialog()
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

    /**
     * Scale control buttons based on the device's baseline scale ratio.
     * The ratio represents how the current device compares to a reference device (1600x2560).
     * On smaller phones (ratio < 1.0), buttons scale down; on larger tablets (ratio > 1.0), they scale up.
     */
    private fun scaleControlButtonsBasedOnDeviceRatio() {
        val scaleRatio = binding.gameBoardView.getCurrentDeviceScaleRatio()

        if (scaleRatio <= 0f || scaleRatio >= 5f) {
            // Sanity check: ratio should be reasonably close to 1.0
            return
        }
        // Wrapper container was removed; per-control scaling is handled in applyBottomControlsSizing().
    }
}
