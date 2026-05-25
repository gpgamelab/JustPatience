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

    private data class GameBoardLayoutConfig(
        val pileOverallOffsetX: Float = 0f,
        val pileOverallOffsetY: Float = 0f,
        val foundationOffsetX: Float = 0f,
        val foundationOffsetY: Float = 0f,
        val drawWasteOffsetX: Float = 0f,
        val drawWasteOffsetY: Float = 0f,
        val tableauOffsetX: Float = 0f,
        val tableauOffsetY: Float = 0f,
        val bannerSmallOffsetX: Float = 0f,
        val bannerSmallOffsetY: Float = 0f,
        val bannerMediumOffsetX: Float = 0f,
        val bannerMediumOffsetY: Float = 0f,
        val bannerLargeOffsetX: Float = 0f,
        val bannerLargeOffsetY: Float = 0f,
        val adBoxChoice: BannerAdBoxChoice = BannerAdBoxChoice.MEDIUM,
        val undoControlAdjustments: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val redoControlAdjustments: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val hintControlAdjustments: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val magicWandControlAdjustments: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val playControlAdjustments: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val autoControlAdjustments: BottomControlButtonAdjustments = BottomControlButtonAdjustments()
    )

    private data class BottomControlButtonAdjustments(
        val scale: Float = 1f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f
    )

    private data class RewardHudAdjustments(
        val iconScale: Float = 1f,
        val counterOffsetX: Float = 0f,
        val counterOffsetY: Float = 0f,
        val counterScale: Float = 1f
    )

    private data class BottomControlAdjustmentsSet(
        val undo: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val redo: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val hint: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val magicWand: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val play: BottomControlButtonAdjustments = BottomControlButtonAdjustments(),
        val auto: BottomControlButtonAdjustments = BottomControlButtonAdjustments()
    )

    // Consolidated device adjusters for a layout profile (one instance per deck layout type)
    private data class LayoutProfileDevAdjusters(
        val portraitSlimCompact: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val portraitSlim: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val portraitClassic: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val portraitBroad: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val portraitSquare: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val portraitPileStockOffsetX: Float = 0f,
        val portraitPileStockOffsetY: Float = 0f,
        val portraitPileWasteOffsetX: Float = 0f,
        val portraitPileWasteOffsetY: Float = 0f,
        val portraitScoreboardOffsetX: Float = 0f,
        val portraitScoreboardOffsetY: Float = 0f,
        val portraitGemRewardOffsetX: Float = 0f,
        val portraitGemRewardOffsetY: Float = 0f,
        val portraitTicketRewardOffsetX: Float = 0f,
        val portraitTicketRewardOffsetY: Float = 0f,
        val landscapeSlimCompact: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val landscapeSlim: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val landscapeClassic: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val landscapeBroad: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val landscapeSquare: GameBoardLayoutConfig = GameBoardLayoutConfig(),
        val landscapePileStockOffsetX: Float = 0f,
        val landscapePileStockOffsetY: Float = 0f,
        val landscapePileWasteOffsetX: Float = 0f,
        val landscapePileWasteOffsetY: Float = 0f,
        val landscapeScoreboardOffsetX: Float = 0f,
        val landscapeScoreboardOffsetY: Float = 0f,
        val landscapeGemRewardOffsetX: Float = 0f,
        val landscapeGemRewardOffsetY: Float = 0f,
        val landscapeTicketRewardOffsetX: Float = 0f,
        val landscapeTicketRewardOffsetY: Float = 0f,
        val gemRewardAdjustments: RewardHudAdjustments = RewardHudAdjustments(),
        val ticketRewardAdjustments: RewardHudAdjustments = RewardHudAdjustments()
    )

    private data class LayoutProfileKey(
        val mirrored: Boolean,
        val deckCount: Int
    )

    // Single source of hardcoded defaults for each hand/deck layout profile.
    // Edit these blocks when you want fixed per-profile values in code.
    private val defaultClassic1DeckLayoutDevAdjusters = LayoutProfileDevAdjusters(
        portraitSlimCompact = GameBoardLayoutConfig(
            foundationOffsetX = -25f,
            tableauOffsetY = 100f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 25f,
                offsetY = -75f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 80f,
                offsetY = -75f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 135f,
                offsetY = -75f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 170f,
                offsetY = -75f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 250f,
                offsetY = -75f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitSlim = GameBoardLayoutConfig(
            pileOverallOffsetY = 0f,
            foundationOffsetX = -50f,
            drawWasteOffsetX = -10f,
            tableauOffsetY = 80f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.MEDIUM,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1f,
                offsetX = 25f,
                offsetY = -100f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1f,
                offsetX = 50f,
                offsetY = -100f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 0.9f,
                offsetX = 75f,
                offsetY = -100f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 90f,
                offsetY = -100f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 135f,
                offsetY = -100f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitClassic = GameBoardLayoutConfig(
            pileOverallOffsetY = 90f,
            foundationOffsetX = -40f,
            drawWasteOffsetX = -20f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitBroad = GameBoardLayoutConfig(
            pileOverallOffsetY = -10f,
            tableauOffsetY = 25f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitSquare = GameBoardLayoutConfig(
            pileOverallOffsetY = -70f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitPileStockOffsetX = 0f,
        portraitPileStockOffsetY = 0f,
        portraitPileWasteOffsetX = 0f,
        portraitPileWasteOffsetY = 0f,
        portraitScoreboardOffsetX = 0f,
        portraitScoreboardOffsetY = 0f,
        portraitGemRewardOffsetX = -10f,
        portraitGemRewardOffsetY = 25f,
        portraitTicketRewardOffsetX = -10f,
        portraitTicketRewardOffsetY = 35f,
        landscapeSlimCompact = GameBoardLayoutConfig(
            pileOverallOffsetX = -280f,
            pileOverallOffsetY = 0f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 25f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 75f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 110f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 130f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 225f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeSlim = GameBoardLayoutConfig(
            pileOverallOffsetX = -130f,
            pileOverallOffsetY = 0f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.MEDIUM,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 25f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 75f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 110f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 130f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 225f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeClassic = GameBoardLayoutConfig(
            pileOverallOffsetX = -115f,
            pileOverallOffsetY = -25f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeBroad = GameBoardLayoutConfig(
            pileOverallOffsetX = -120f,
            pileOverallOffsetY = 0f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeSquare = GameBoardLayoutConfig(
            pileOverallOffsetX = -5f,
            pileOverallOffsetY = -100f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapePileStockOffsetX = 0f,
        landscapePileStockOffsetY = 0f,
        landscapePileWasteOffsetX = 0f,
        landscapePileWasteOffsetY = 0f,
        landscapeScoreboardOffsetX = 0f,
        landscapeScoreboardOffsetY = 0f,
        landscapeGemRewardOffsetX = -10f,
        landscapeGemRewardOffsetY = 25f,
        landscapeTicketRewardOffsetX = -10f,
        landscapeTicketRewardOffsetY = 35f
    )

    private val defaultClassic2DeckLayoutDevAdjusters = LayoutProfileDevAdjusters(
        portraitSlimCompact = GameBoardLayoutConfig(
            tableauOffsetY = 0f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 25f,
                offsetY = -75f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 80f,
                offsetY = -75f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 135f,
                offsetY = -75f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 170f,
                offsetY = -75f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 250f,
                offsetY = -75f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitSlim = GameBoardLayoutConfig(
            pileOverallOffsetY = 0f,
            foundationOffsetX = 0f,
            drawWasteOffsetX = 0f,
            tableauOffsetY = 0f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.MEDIUM,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1f,
                offsetX = 25f,
                offsetY = -100f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1f,
                offsetX = 50f,
                offsetY = -100f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 0.9f,
                offsetX = 75f,
                offsetY = -100f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 90f,
                offsetY = -100f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 135f,
                offsetY = -100f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitClassic = GameBoardLayoutConfig(
            pileOverallOffsetX = 0f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL
        ),
        portraitBroad = GameBoardLayoutConfig(
            pileOverallOffsetY = -10f,
            tableauOffsetY = 25f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitSquare = GameBoardLayoutConfig(
            pileOverallOffsetY = -70f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE
        ),
        portraitPileStockOffsetX = 0f,
        portraitPileStockOffsetY = 0f,
        portraitPileWasteOffsetX = 0f,
        portraitPileWasteOffsetY = 0f,
        portraitScoreboardOffsetX = 0f,
        portraitScoreboardOffsetY = 0f,
        portraitGemRewardOffsetX = -10f,
        portraitGemRewardOffsetY = 25f,
        portraitTicketRewardOffsetX = -10f,
        portraitTicketRewardOffsetY = 35f,
        landscapeSlimCompact = GameBoardLayoutConfig(
            pileOverallOffsetX = -180f,
            pileOverallOffsetY = -150f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 25f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 75f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 110f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 130f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 225f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeSlim = GameBoardLayoutConfig(
            pileOverallOffsetX = -110f,
            pileOverallOffsetY = -80f,
            foundationOffsetX = -30f,
            drawWasteOffsetX = 0f,
            tableauOffsetY = -10f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.MEDIUM,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 25f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 75f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 110f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 130f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 225f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )        ),
        landscapeClassic = GameBoardLayoutConfig(
            pileOverallOffsetX = -5f,
            pileOverallOffsetY = -100f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL
        ),
        landscapeBroad = GameBoardLayoutConfig(
            pileOverallOffsetX = -5f,
            pileOverallOffsetY = -100f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = -300f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeSquare = GameBoardLayoutConfig(
            pileOverallOffsetX = -5f,
            pileOverallOffsetY = -100f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = -300f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE
        ),
        landscapePileStockOffsetX = 0f,
        landscapePileStockOffsetY = 0f,
        landscapePileWasteOffsetX = 0f,
        landscapePileWasteOffsetY = 0f,
        landscapeScoreboardOffsetX = 0f,
        landscapeScoreboardOffsetY = 0f,
        landscapeGemRewardOffsetX = -10f,
        landscapeGemRewardOffsetY = 25f,
        landscapeTicketRewardOffsetX = -10f,
        landscapeTicketRewardOffsetY = 35f
    )

    private val defaultMirrored1DeckLayoutDevAdjusters = LayoutProfileDevAdjusters(
        portraitSlimCompact = GameBoardLayoutConfig(
            foundationOffsetX = 25f,
            drawWasteOffsetX = 75f,
            tableauOffsetY = 100f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 25f,
                offsetY = -75f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 80f,
                offsetY = -75f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 135f,
                offsetY = -75f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 170f,
                offsetY = -75f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 200f,
                offsetY = -75f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitSlim = GameBoardLayoutConfig(
            pileOverallOffsetY = -0f,
            foundationOffsetX = 35f,
            drawWasteOffsetX = 55f,
            tableauOffsetY = 80f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.MEDIUM,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1f,
                offsetX = 25f,
                offsetY = -100f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1f,
                offsetX = 50f,
                offsetY = -100f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 0.9f,
                offsetX = 75f,
                offsetY = -100f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 90f,
                offsetY = -100f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 135f,
                offsetY = -100f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitClassic = GameBoardLayoutConfig(
            pileOverallOffsetY = 100f,
            foundationOffsetX = 40f,
            drawWasteOffsetX = 75f,
            tableauOffsetY = 10f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL
        ),
        portraitBroad = GameBoardLayoutConfig(
            pileOverallOffsetY = -10f,
            drawWasteOffsetX = 45f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitSquare = GameBoardLayoutConfig(
            pileOverallOffsetY = -70f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE
        ),
        portraitPileStockOffsetX = 0f,
        portraitPileStockOffsetY = 0f,
        portraitPileWasteOffsetX = 0f,
        portraitPileWasteOffsetY = 0f,
        portraitScoreboardOffsetX = 0f,
        portraitScoreboardOffsetY = 0f,
        portraitGemRewardOffsetX = -10f,
        portraitGemRewardOffsetY = 25f,
        portraitTicketRewardOffsetX = -10f,
        portraitTicketRewardOffsetY = 35f,
        landscapeSlimCompact = GameBoardLayoutConfig(
            pileOverallOffsetX = 380f,
            pileOverallOffsetY = 0f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 25f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 75f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 110f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 130f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 225f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeSlim = GameBoardLayoutConfig(
            pileOverallOffsetX = 260f,
            pileOverallOffsetY = 0f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.MEDIUM,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 25f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 75f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 110f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 130f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 160f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )        ),
        landscapeClassic = GameBoardLayoutConfig(
            pileOverallOffsetX = 275f,
            pileOverallOffsetY = -25f,
            foundationOffsetX = -15f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL
        ),
        landscapeBroad = GameBoardLayoutConfig(
            pileOverallOffsetX = 190f,
            pileOverallOffsetY = 0f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeSquare = GameBoardLayoutConfig(
            pileOverallOffsetX = 90f,
            pileOverallOffsetY = -100f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE
        ),
        landscapePileStockOffsetX = 0f,
        landscapePileStockOffsetY = 0f,
        landscapePileWasteOffsetX = 0f,
        landscapePileWasteOffsetY = 0f,
        landscapeScoreboardOffsetX = 0f,
        landscapeScoreboardOffsetY = 0f,
        landscapeGemRewardOffsetX = -10f,
        landscapeGemRewardOffsetY = 25f,
        landscapeTicketRewardOffsetX = -10f,
        landscapeTicketRewardOffsetY = 35f
    )

    private val defaultMirrored2DeckLayoutDevAdjusters = LayoutProfileDevAdjusters(
        portraitSlimCompact = GameBoardLayoutConfig(
            tableauOffsetY = 0f,
            drawWasteOffsetX = 70f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 25f,
                offsetY = -75f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 80f,
                offsetY = -75f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 135f,
                offsetY = -75f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 170f,
                offsetY = -75f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 200f,
                offsetY = -75f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitSlim = GameBoardLayoutConfig(
            pileOverallOffsetY = 0f,
            foundationOffsetX = 35f,
            drawWasteOffsetX = 45f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.MEDIUM,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1f,
                offsetX = 25f,
                offsetY = -100f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1f,
                offsetX = 50f,
                offsetY = -100f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 0.9f,
                offsetX = 75f,
                offsetY = -100f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 90f,
                offsetY = -100f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 135f,
                offsetY = -100f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitClassic = GameBoardLayoutConfig(
            pileOverallOffsetY = 0f,
            foundationOffsetX = 35f,
            drawWasteOffsetX = 65f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL
        ),
        portraitBroad = GameBoardLayoutConfig(
            pileOverallOffsetY = -10f,
            drawWasteOffsetX = 45f,
            tableauOffsetY = 25f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.9f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        portraitSquare = GameBoardLayoutConfig(
            pileOverallOffsetY = -70f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE
        ),
        portraitPileStockOffsetX = 0f,
        portraitPileStockOffsetY = 0f,
        portraitPileWasteOffsetX = 0f,
        portraitPileWasteOffsetY = 0f,
        portraitScoreboardOffsetX = 0f,
        portraitScoreboardOffsetY = 0f,
        portraitGemRewardOffsetX = -10f,
        portraitGemRewardOffsetY = 25f,
        portraitTicketRewardOffsetX = -10f,
        portraitTicketRewardOffsetY = 35f,
        landscapeSlimCompact = GameBoardLayoutConfig(
            pileOverallOffsetX = 280f,
            pileOverallOffsetY = -150f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 25f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 75f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 110f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 130f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 225f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeSlim = GameBoardLayoutConfig(
            pileOverallOffsetX = 195f,
            pileOverallOffsetY = -80f,
            foundationOffsetX = 30f,
            tableauOffsetY = 10f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.MEDIUM,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 25f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2.7f,
                offsetX = 75f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 110f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 130f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 3f,
                offsetX = 225f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )        ),
        landscapeClassic = GameBoardLayoutConfig(
            pileOverallOffsetX = 165f,
            pileOverallOffsetY = -100f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 0f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.SMALL
        ),
        landscapeBroad = GameBoardLayoutConfig(
            pileOverallOffsetX = 90f,
            pileOverallOffsetY = -80f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 300f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE,
            undoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 20f,
                offsetY = 0f
            ),
            redoControlAdjustments = BottomControlButtonAdjustments(
                scale = 2f,
                offsetX = 70f,
                offsetY = 0f
            ),
            hintControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 120f,
                offsetY = 0f
            ),
            magicWandControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.5f,
                offsetX = 150f,
                offsetY = 0f
            ),
            playControlAdjustments = BottomControlButtonAdjustments(
                scale = 1.7f,
                offsetX = 190f,
                offsetY = 0f
            ),
            autoControlAdjustments = BottomControlButtonAdjustments(
                scale = 3.8f,
                offsetX = 0f,
                offsetY = -200f
            )
        ),
        landscapeSquare = GameBoardLayoutConfig(
            pileOverallOffsetX = 90f,
            pileOverallOffsetY = -100f,
            bannerSmallOffsetX = 0f,
            bannerSmallOffsetY = 0f,
            bannerMediumOffsetX = 0f,
            bannerMediumOffsetY = 0f,
            bannerLargeOffsetX = 300f,
            bannerLargeOffsetY = 0f,
            adBoxChoice = BannerAdBoxChoice.LARGE
        ),
        landscapePileStockOffsetX = 0f,
        landscapePileStockOffsetY = 0f,
        landscapePileWasteOffsetX = 0f,
        landscapePileWasteOffsetY = 0f,
        landscapeScoreboardOffsetX = 0f,
        landscapeScoreboardOffsetY = 0f,
        landscapeGemRewardOffsetX = -10f,
        landscapeGemRewardOffsetY = 25f,
        landscapeTicketRewardOffsetX = -10f,
        landscapeTicketRewardOffsetY = 35f
    )

    private enum class BannerAdTier {
        SMALL,
        MEDIUM,
        LARGE
    }

    private fun resolveCurrentAspectConfig(profile: LayoutProfileDevAdjusters): GameBoardLayoutConfig {
        return if (isLandscapeNow()) {
            when (binding.gameBoardView.getCurrentAspectCategory()) {
                DeviceAspectCategory.SLIM -> if (isCompactSlimLandscapeBoard()) profile.landscapeSlimCompact else profile.landscapeSlim
                DeviceAspectCategory.CLASSIC -> profile.landscapeClassic
                DeviceAspectCategory.BROAD -> profile.landscapeBroad
                DeviceAspectCategory.SQUARE -> profile.landscapeSquare
            }
        } else {
            when (binding.gameBoardView.getCurrentAspectCategory()) {
                DeviceAspectCategory.SLIM -> if (isCompactSlimPortraitBoard()) profile.portraitSlimCompact else profile.portraitSlim
                DeviceAspectCategory.CLASSIC -> profile.portraitClassic
                DeviceAspectCategory.BROAD -> profile.portraitBroad
                DeviceAspectCategory.SQUARE -> profile.portraitSquare
            }
        }
    }

    private fun applyCurrentBannerOffsetsFromProfile(profile: LayoutProfileDevAdjusters) {
        val cfg = resolveCurrentAspectConfig(profile)
        if (isLandscapeNow()) {
            devSmallDeviceLandscapeBannerOffsetXDpState = cfg.bannerSmallOffsetX
            devSmallDeviceLandscapeBannerOffsetYDpState = cfg.bannerSmallOffsetY
            devMediumDeviceLandscapeBannerOffsetXDpState = cfg.bannerMediumOffsetX
            devMediumDeviceLandscapeBannerOffsetYDpState = cfg.bannerMediumOffsetY
            devLargeDeviceLandscapeBannerOffsetXDpState = cfg.bannerLargeOffsetX
            devLargeDeviceLandscapeBannerOffsetYDpState = cfg.bannerLargeOffsetY
        } else {
            devSmallDevicePortraitBannerOffsetXDpState = cfg.bannerSmallOffsetX
            devSmallDevicePortraitBannerOffsetYDpState = cfg.bannerSmallOffsetY
            devMediumDevicePortraitBannerOffsetXDpState = cfg.bannerMediumOffsetX
            devMediumDevicePortraitBannerOffsetYDpState = cfg.bannerMediumOffsetY
            devLargeDevicePortraitBannerOffsetXDpState = cfg.bannerLargeOffsetX
            devLargeDevicePortraitBannerOffsetYDpState = cfg.bannerLargeOffsetY
        }
    }

    private enum class BannerAdBoxChoice {
        SMALL,
        MEDIUM,
        LARGE
    }

    private fun isLandscapeNow(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    // Lets narrow SLIM phones (e.g. 720px portrait board) use a separate tuning set.
    private val portraitSlimCompactMaxBoardWidthPx = 800
    private val landscapeSlimCompactMaxBoardHeightPx = 800

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
    private var devLandscapeOverallOffsetXSlimCompactDpState: Float = -5f
    private var devLandscapeOverallOffsetYSlimCompactDpState: Float = -100f
    private var devLandscapeOverallOffsetXSlimDpState: Float = -5f
    private var devLandscapeOverallOffsetYSlimDpState: Float = -100f
    private var devLandscapeOverallOffsetXClassicDpState: Float = -5f
    private var devLandscapeOverallOffsetYClassicDpState: Float = -100f
    private var devLandscapeOverallOffsetXBroadDpState: Float = -5f
    private var devLandscapeOverallOffsetYBroadDpState: Float = -100f
    private var devLandscapeOverallOffsetXSquareDpState: Float = -5f
    private var devLandscapeOverallOffsetYSquareDpState: Float = -100f
    private var devLandscapeFoundationOffsetXSlimCompactDpState: Float = 0f
    private var devLandscapeFoundationOffsetYSlimCompactDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetXSlimCompactDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetYSlimCompactDpState: Float = 0f
    private var devLandscapeFoundationOffsetXSlimDpState: Float = 0f
    private var devLandscapeFoundationOffsetYSlimDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetXSlimDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetYSlimDpState: Float = 0f
    private var devLandscapeFoundationOffsetXClassicDpState: Float = 0f
    private var devLandscapeFoundationOffsetYClassicDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetXClassicDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetYClassicDpState: Float = 0f
    private var devLandscapeFoundationOffsetXBroadDpState: Float = 0f
    private var devLandscapeFoundationOffsetYBroadDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetXBroadDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetYBroadDpState: Float = 0f
    private var devLandscapeFoundationOffsetXSquareDpState: Float = 0f
    private var devLandscapeFoundationOffsetYSquareDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetXSquareDpState: Float = 0f
    private var devLandscapeDrawWasteOffsetYSquareDpState: Float = 0f
    private var devLandscapePileStockOffsetXDpState: Float = 0f
    private var devLandscapePileStockOffsetYDpState: Float = 0f
    private var devLandscapePileWasteOffsetXDpState: Float = 0f
    private var devLandscapePileWasteOffsetYDpState: Float = 0f
    private var devLandscapeTableauOffsetXSlimCompactDpState: Float = 0f
    private var devLandscapeTableauOffsetYSlimCompactDpState: Float = 0f
    private var devLandscapeTableauOffsetXSlimDpState: Float = 0f
    private var devLandscapeTableauOffsetYSlimDpState: Float = 0f
    private var devLandscapeTableauOffsetXClassicDpState: Float = 0f
    private var devLandscapeTableauOffsetYClassicDpState: Float = 0f
    private var devLandscapeTableauOffsetXBroadDpState: Float = 0f
    private var devLandscapeTableauOffsetYBroadDpState: Float = 0f
    private var devLandscapeTableauOffsetXSquareDpState: Float = 0f
    private var devLandscapeTableauOffsetYSquareDpState: Float = 0f
    private var devPortraitOverallOffsetXSlimCompactDpState: Float = 0f
    private var devPortraitOverallOffsetYSlimCompactDpState: Float = -70f
    private var devPortraitOverallOffsetXSlimDpState: Float = 0f
    private var devPortraitOverallOffsetYSlimDpState: Float = -70f
    private var devPortraitOverallOffsetXClassicDpState: Float = 0f
    private var devPortraitOverallOffsetYClassicDpState: Float = -70f
    private var devPortraitOverallOffsetXBroadDpState: Float = 0f
    private var devPortraitOverallOffsetYBroadDpState: Float = 0f
    private var devPortraitOverallOffsetXSquareDpState: Float = 0f
    private var devPortraitOverallOffsetYSquareDpState: Float = -70f
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
    private var devPortraitTableauOffsetXSlimCompactDpState: Float = 0f
    private var devPortraitTableauOffsetYSlimCompactDpState: Float = 0f
    private var devPortraitTableauOffsetXSlimDpState: Float = 0f
    private var devPortraitTableauOffsetYSlimDpState: Float = 0f
    private var devPortraitTableauOffsetXClassicDpState: Float = 0f
    private var devPortraitTableauOffsetYClassicDpState: Float = 0f
    private var devPortraitTableauOffsetXBroadDpState: Float = 0f
    private var devPortraitTableauOffsetYBroadDpState: Float = 0f
    private var devPortraitTableauOffsetXSquareDpState: Float = 0f
    private var devPortraitTableauOffsetYSquareDpState: Float = 0f
    private var devPortraitBannerSmallWidthDpState: Float = 320f
    private var devPortraitBannerSmallHeightDpState: Float = 60f
    private var devPortraitBannerMediumWidthDpState: Float = 320f
    private var devPortraitBannerMediumHeightDpState: Float = 110f
    private var devPortraitBannerLargeWidthDpState: Float = 320f
    private var devPortraitBannerLargeHeightDpState: Float = 260f
    private var devLandscapeBannerSmallWidthDpState: Float = 320f
    private var devLandscapeBannerSmallHeightDpState: Float = 60f
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
    private var devSmallDevicePortraitBannerOffsetXDpState: Float = 0f
    private var devSmallDevicePortraitBannerOffsetYDpState: Float = 0f
    private var devMediumDevicePortraitBannerOffsetXDpState: Float = 0f
    private var devMediumDevicePortraitBannerOffsetYDpState: Float = 0f
    private var devLargeDevicePortraitBannerOffsetXDpState: Float = 0f
    private var devLargeDevicePortraitBannerOffsetYDpState: Float = 0f
    private var devScoreboardOffsetXDpState: Float = 0f
    private var devScoreboardOffsetYDpState: Float = 0f
    private var devGemRewardOffsetXDpState: Float = 0f
    private var devGemRewardOffsetYDpState: Float = 0f
    private var devTicketRewardOffsetXDpState: Float = 0f
    private var devTicketRewardOffsetYDpState: Float = 0f
    private var devUndoControlScaleState: Float = 1f
    private var devUndoControlOffsetXDpState: Float = 0f
    private var devUndoControlOffsetYDpState: Float = 0f
    private var devRedoControlScaleState: Float = 1f
    private var devRedoControlOffsetXDpState: Float = 0f
    private var devRedoControlOffsetYDpState: Float = 0f
    private var devHintControlScaleState: Float = 1f
    private var devHintControlOffsetXDpState: Float = 0f
    private var devHintControlOffsetYDpState: Float = 0f
    private var devMagicWandControlScaleState: Float = 1f
    private var devMagicWandControlOffsetXDpState: Float = 0f
    private var devMagicWandControlOffsetYDpState: Float = 0f
    private var devPlayControlScaleState: Float = 1f
    private var devPlayControlOffsetXDpState: Float = 0f
    private var devPlayControlOffsetYDpState: Float = 0f
    private var devAutoControlScaleState: Float = 1f
    private var devAutoControlOffsetXDpState: Float = 0f
    private var devAutoControlOffsetYDpState: Float = 0f
    private var devGemRewardScaleState: Float = 1f
    private var devGemRewardCounterOffsetXDpState: Float = 0f
    private var devGemRewardCounterOffsetYDpState: Float = 0f
    private var devGemRewardCounterScaleState: Float = 1f
    private var devTicketRewardScaleState: Float = 1f
    private var devTicketRewardCounterOffsetXDpState: Float = 0f
    private var devTicketRewardCounterOffsetYDpState: Float = 0f
    private var devTicketRewardCounterScaleState: Float = 1f
//    private var devAdBoxChoiceState: BannerAdBoxChoice = BannerAdBoxChoice.SMALL
    private var devLandscapeAdBoxChoiceSlimCompactState: BannerAdBoxChoice = BannerAdBoxChoice.SMALL
    private var devLandscapeAdBoxChoiceSlimState: BannerAdBoxChoice = BannerAdBoxChoice.SMALL
    private var devLandscapeAdBoxChoiceClassicState: BannerAdBoxChoice = BannerAdBoxChoice.MEDIUM
    private var devLandscapeAdBoxChoiceBroadState: BannerAdBoxChoice = BannerAdBoxChoice.LARGE
    private var devLandscapeAdBoxChoiceSquareState: BannerAdBoxChoice = BannerAdBoxChoice.LARGE
    private var devPortraitAdBoxChoiceSlimCompactState: BannerAdBoxChoice = BannerAdBoxChoice.SMALL
    private var devPortraitAdBoxChoiceSlimState: BannerAdBoxChoice = BannerAdBoxChoice.SMALL
    private var devPortraitAdBoxChoiceClassicState: BannerAdBoxChoice = BannerAdBoxChoice.MEDIUM
    private var devPortraitAdBoxChoiceBroadState: BannerAdBoxChoice = BannerAdBoxChoice.LARGE
    private var devPortraitAdBoxChoiceSquareState: BannerAdBoxChoice = BannerAdBoxChoice.LARGE

    // Aspect-ratio category Y trims (dp).  Applied as the final boardStartY adjustment.
    // Positive = move piles DOWN, negative = move piles UP.
    private var devAspectPortraitSlimYDpState:    Float = 0f
    private var devAspectPortraitClassicYDpState: Float = 0f
    private var devAspectPortraitBroadYDpState:   Float = 0f
    private var devAspectPortraitSquareYDpState:  Float = 0f
    private var devAspectLandscapeSlimYDpState:    Float = 0f
    private var devAspectLandscapeClassicYDpState: Float = 0f
    private var devAspectLandscapeBroadYDpState:   Float = 0f
    private var devAspectLandscapeSquareYDpState:  Float = 0f

    // Aspect-ratio category X trims (dp).  Applied as the final boardStartX adjustment.
    // Positive = move piles RIGHT, negative = move piles LEFT.
    private var devAspectPortraitSlimXDpState:    Float = 0f
    private var devAspectPortraitClassicXDpState: Float = 0f
    private var devAspectPortraitBroadXDpState:   Float = 0f
    private var devAspectPortraitSquareXDpState:  Float = 0f
    private var devAspectLandscapeSlimXDpState:    Float = 0f
    private var devAspectLandscapeClassicXDpState: Float = 0f
    private var devAspectLandscapeBroadXDpState:   Float = 0f
    private var devAspectLandscapeSquareXDpState:  Float = 0f
    private var classic1DeckLayoutDevAdjustersState = defaultClassic1DeckLayoutDevAdjusters
    private var classic2DeckLayoutDevAdjustersState = defaultClassic2DeckLayoutDevAdjusters
    private var mirrored1DeckLayoutDevAdjustersState = defaultMirrored1DeckLayoutDevAdjusters
    private var mirrored2DeckLayoutDevAdjustersState = defaultMirrored2DeckLayoutDevAdjusters

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

    private fun snapshotLayoutScopedDevAdjusters(): LayoutProfileDevAdjusters {
        val snapshot = LayoutProfileDevAdjusters(
            portraitSlimCompact = GameBoardLayoutConfig(
                pileOverallOffsetX = devPortraitOverallOffsetXSlimCompactDpState,
                pileOverallOffsetY = devPortraitOverallOffsetYSlimCompactDpState,
                foundationOffsetX = devPortraitFoundationOffsetXSlimCompactDpState,
                foundationOffsetY = devPortraitFoundationOffsetYSlimCompactDpState,
                drawWasteOffsetX = devPortraitDrawWasteOffsetXSlimCompactDpState,
                drawWasteOffsetY = devPortraitDrawWasteOffsetYSlimCompactDpState,
                tableauOffsetX = devPortraitTableauOffsetXSlimCompactDpState,
                tableauOffsetY = devPortraitTableauOffsetYSlimCompactDpState,
                bannerSmallOffsetX = devSmallDevicePortraitBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDevicePortraitBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDevicePortraitBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDevicePortraitBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDevicePortraitBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDevicePortraitBannerOffsetYDpState,
                adBoxChoice = devPortraitAdBoxChoiceSlimCompactState
            ),
            portraitSlim = GameBoardLayoutConfig(
                pileOverallOffsetX = devPortraitOverallOffsetXSlimDpState,
                pileOverallOffsetY = devPortraitOverallOffsetYSlimDpState,
                foundationOffsetX = devPortraitFoundationOffsetXSlimDpState,
                foundationOffsetY = devPortraitFoundationOffsetYSlimDpState,
                drawWasteOffsetX = devPortraitDrawWasteOffsetXSlimDpState,
                drawWasteOffsetY = devPortraitDrawWasteOffsetYSlimDpState,
                tableauOffsetX = devPortraitTableauOffsetXSlimDpState,
                tableauOffsetY = devPortraitTableauOffsetYSlimDpState,
                bannerSmallOffsetX = devSmallDevicePortraitBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDevicePortraitBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDevicePortraitBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDevicePortraitBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDevicePortraitBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDevicePortraitBannerOffsetYDpState,
                adBoxChoice = devPortraitAdBoxChoiceSlimState
            ),
            portraitClassic = GameBoardLayoutConfig(
                pileOverallOffsetX = devPortraitOverallOffsetXClassicDpState,
                pileOverallOffsetY = devPortraitOverallOffsetYClassicDpState,
                foundationOffsetX = devPortraitFoundationOffsetXClassicDpState,
                foundationOffsetY = devPortraitFoundationOffsetYClassicDpState,
                drawWasteOffsetX = devPortraitDrawWasteOffsetXClassicDpState,
                drawWasteOffsetY = devPortraitDrawWasteOffsetYClassicDpState,
                tableauOffsetX = devPortraitTableauOffsetXClassicDpState,
                tableauOffsetY = devPortraitTableauOffsetYClassicDpState,
                bannerSmallOffsetX = devSmallDevicePortraitBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDevicePortraitBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDevicePortraitBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDevicePortraitBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDevicePortraitBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDevicePortraitBannerOffsetYDpState,
                adBoxChoice = devPortraitAdBoxChoiceClassicState
            ),
            portraitBroad = GameBoardLayoutConfig(
                pileOverallOffsetX = devPortraitOverallOffsetXBroadDpState,
                pileOverallOffsetY = devPortraitOverallOffsetYBroadDpState,
                foundationOffsetX = devPortraitFoundationOffsetXBroadDpState,
                foundationOffsetY = devPortraitFoundationOffsetYBroadDpState,
                drawWasteOffsetX = devPortraitDrawWasteOffsetXBroadDpState,
                drawWasteOffsetY = devPortraitDrawWasteOffsetYBroadDpState,
                tableauOffsetX = devPortraitTableauOffsetXBroadDpState,
                tableauOffsetY = devPortraitTableauOffsetYBroadDpState,
                bannerSmallOffsetX = devSmallDevicePortraitBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDevicePortraitBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDevicePortraitBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDevicePortraitBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDevicePortraitBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDevicePortraitBannerOffsetYDpState,
                adBoxChoice = devPortraitAdBoxChoiceBroadState
            ),
            portraitSquare = GameBoardLayoutConfig(
                pileOverallOffsetX = devPortraitOverallOffsetXSquareDpState,
                pileOverallOffsetY = devPortraitOverallOffsetYSquareDpState,
                foundationOffsetX = devPortraitFoundationOffsetXSquareDpState,
                foundationOffsetY = devPortraitFoundationOffsetYSquareDpState,
                drawWasteOffsetX = devPortraitDrawWasteOffsetXSquareDpState,
                drawWasteOffsetY = devPortraitDrawWasteOffsetYSquareDpState,
                tableauOffsetX = devPortraitTableauOffsetXSquareDpState,
                tableauOffsetY = devPortraitTableauOffsetYSquareDpState,
                bannerSmallOffsetX = devSmallDevicePortraitBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDevicePortraitBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDevicePortraitBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDevicePortraitBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDevicePortraitBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDevicePortraitBannerOffsetYDpState,
                adBoxChoice = devPortraitAdBoxChoiceSquareState
            ),
            portraitPileStockOffsetX = devPortraitPileStockOffsetXDpState,
            portraitPileStockOffsetY = devPortraitPileStockOffsetYDpState,
            portraitPileWasteOffsetX = devPortraitPileWasteOffsetXDpState,
            portraitPileWasteOffsetY = devPortraitPileWasteOffsetYDpState,
            portraitScoreboardOffsetX = devScoreboardOffsetXDpState,
            portraitScoreboardOffsetY = devScoreboardOffsetYDpState,
            portraitGemRewardOffsetX = devGemRewardOffsetXDpState,
            portraitGemRewardOffsetY = devGemRewardOffsetYDpState,
            portraitTicketRewardOffsetX = devTicketRewardOffsetXDpState,
            portraitTicketRewardOffsetY = devTicketRewardOffsetYDpState,
            gemRewardAdjustments = RewardHudAdjustments(
                iconScale = devGemRewardScaleState,
                counterOffsetX = devGemRewardCounterOffsetXDpState,
                counterOffsetY = devGemRewardCounterOffsetYDpState,
                counterScale = devGemRewardCounterScaleState
            ),
            ticketRewardAdjustments = RewardHudAdjustments(
                iconScale = devTicketRewardScaleState,
                counterOffsetX = devTicketRewardCounterOffsetXDpState,
                counterOffsetY = devTicketRewardCounterOffsetYDpState,
                counterScale = devTicketRewardCounterScaleState
            ),
            landscapeSlimCompact = GameBoardLayoutConfig(
                pileOverallOffsetX = devLandscapeOverallOffsetXSlimCompactDpState,
                pileOverallOffsetY = devLandscapeOverallOffsetYSlimCompactDpState,
                foundationOffsetX = devLandscapeFoundationOffsetXSlimCompactDpState,
                foundationOffsetY = devLandscapeFoundationOffsetYSlimCompactDpState,
                drawWasteOffsetX = devLandscapeDrawWasteOffsetXSlimCompactDpState,
                drawWasteOffsetY = devLandscapeDrawWasteOffsetYSlimCompactDpState,
                tableauOffsetX = devLandscapeTableauOffsetXSlimCompactDpState,
                tableauOffsetY = devLandscapeTableauOffsetYSlimCompactDpState,
                bannerSmallOffsetX = devSmallDeviceLandscapeBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDeviceLandscapeBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDeviceLandscapeBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDeviceLandscapeBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDeviceLandscapeBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDeviceLandscapeBannerOffsetYDpState,
                adBoxChoice = devLandscapeAdBoxChoiceSlimCompactState
            ),
            landscapeSlim = GameBoardLayoutConfig(
                pileOverallOffsetX = devLandscapeOverallOffsetXSlimDpState,
                pileOverallOffsetY = devLandscapeOverallOffsetYSlimDpState,
                foundationOffsetX = devLandscapeFoundationOffsetXSlimDpState,
                foundationOffsetY = devLandscapeFoundationOffsetYSlimDpState,
                drawWasteOffsetX = devLandscapeDrawWasteOffsetXSlimDpState,
                drawWasteOffsetY = devLandscapeDrawWasteOffsetYSlimDpState,
                tableauOffsetX = devLandscapeTableauOffsetXSlimDpState,
                tableauOffsetY = devLandscapeTableauOffsetYSlimDpState,
                bannerSmallOffsetX = devSmallDeviceLandscapeBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDeviceLandscapeBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDeviceLandscapeBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDeviceLandscapeBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDeviceLandscapeBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDeviceLandscapeBannerOffsetYDpState,
                adBoxChoice = devLandscapeAdBoxChoiceSlimState
            ),
            landscapeClassic = GameBoardLayoutConfig(
                pileOverallOffsetX = devLandscapeOverallOffsetXClassicDpState,
                pileOverallOffsetY = devLandscapeOverallOffsetYClassicDpState,
                foundationOffsetX = devLandscapeFoundationOffsetXClassicDpState,
                foundationOffsetY = devLandscapeFoundationOffsetYClassicDpState,
                drawWasteOffsetX = devLandscapeDrawWasteOffsetXClassicDpState,
                drawWasteOffsetY = devLandscapeDrawWasteOffsetYClassicDpState,
                tableauOffsetX = devLandscapeTableauOffsetXClassicDpState,
                tableauOffsetY = devLandscapeTableauOffsetYClassicDpState,
                bannerSmallOffsetX = devSmallDeviceLandscapeBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDeviceLandscapeBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDeviceLandscapeBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDeviceLandscapeBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDeviceLandscapeBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDeviceLandscapeBannerOffsetYDpState,
                adBoxChoice = devLandscapeAdBoxChoiceClassicState
            ),
            landscapeBroad = GameBoardLayoutConfig(
                pileOverallOffsetX = devLandscapeOverallOffsetXBroadDpState,
                pileOverallOffsetY = devLandscapeOverallOffsetYBroadDpState,
                foundationOffsetX = devLandscapeFoundationOffsetXBroadDpState,
                foundationOffsetY = devLandscapeFoundationOffsetYBroadDpState,
                drawWasteOffsetX = devLandscapeDrawWasteOffsetXBroadDpState,
                drawWasteOffsetY = devLandscapeDrawWasteOffsetYBroadDpState,
                tableauOffsetX = devLandscapeTableauOffsetXBroadDpState,
                tableauOffsetY = devLandscapeTableauOffsetYBroadDpState,
                bannerSmallOffsetX = devSmallDeviceLandscapeBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDeviceLandscapeBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDeviceLandscapeBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDeviceLandscapeBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDeviceLandscapeBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDeviceLandscapeBannerOffsetYDpState,
                adBoxChoice = devLandscapeAdBoxChoiceBroadState
            ),
            landscapeSquare = GameBoardLayoutConfig(
                pileOverallOffsetX = devLandscapeOverallOffsetXSquareDpState,
                pileOverallOffsetY = devLandscapeOverallOffsetYSquareDpState,
                foundationOffsetX = devLandscapeFoundationOffsetXSquareDpState,
                foundationOffsetY = devLandscapeFoundationOffsetYSquareDpState,
                drawWasteOffsetX = devLandscapeDrawWasteOffsetXSquareDpState,
                drawWasteOffsetY = devLandscapeDrawWasteOffsetYSquareDpState,
                tableauOffsetX = devLandscapeTableauOffsetXSquareDpState,
                tableauOffsetY = devLandscapeTableauOffsetYSquareDpState,
                bannerSmallOffsetX = devSmallDeviceLandscapeBannerOffsetXDpState,
                bannerSmallOffsetY = devSmallDeviceLandscapeBannerOffsetYDpState,
                bannerMediumOffsetX = devMediumDeviceLandscapeBannerOffsetXDpState,
                bannerMediumOffsetY = devMediumDeviceLandscapeBannerOffsetYDpState,
                bannerLargeOffsetX = devLargeDeviceLandscapeBannerOffsetXDpState,
                bannerLargeOffsetY = devLargeDeviceLandscapeBannerOffsetYDpState,
                adBoxChoice = devLandscapeAdBoxChoiceSquareState
            ),
            landscapePileStockOffsetX = devLandscapePileStockOffsetXDpState,
            landscapePileStockOffsetY = devLandscapePileStockOffsetYDpState,
            landscapePileWasteOffsetX = devLandscapePileWasteOffsetXDpState,
            landscapePileWasteOffsetY = devLandscapePileWasteOffsetYDpState,
            landscapeScoreboardOffsetX = devScoreboardOffsetXDpState,
            landscapeScoreboardOffsetY = devScoreboardOffsetYDpState,
            landscapeGemRewardOffsetX = devGemRewardOffsetXDpState,
            landscapeGemRewardOffsetY = devGemRewardOffsetYDpState,
            landscapeTicketRewardOffsetX = devTicketRewardOffsetXDpState,
            landscapeTicketRewardOffsetY = devTicketRewardOffsetYDpState
        )

        val existingProfile = profileStateFor(resolveActiveLayoutProfileKey())
        return snapshot
            .withBottomControlAdjustmentsFrom(existingProfile)
            .withCurrentDeviceBottomControlAdjustments(
                BottomControlAdjustmentsSet(
                    undo = BottomControlButtonAdjustments(
                        scale = devUndoControlScaleState,
                        offsetX = devUndoControlOffsetXDpState,
                        offsetY = devUndoControlOffsetYDpState
                    ),
                    redo = BottomControlButtonAdjustments(
                        scale = devRedoControlScaleState,
                        offsetX = devRedoControlOffsetXDpState,
                        offsetY = devRedoControlOffsetYDpState
                    ),
                    hint = BottomControlButtonAdjustments(
                        scale = devHintControlScaleState,
                        offsetX = devHintControlOffsetXDpState,
                        offsetY = devHintControlOffsetYDpState
                    ),
                    magicWand = BottomControlButtonAdjustments(
                        scale = devMagicWandControlScaleState,
                        offsetX = devMagicWandControlOffsetXDpState,
                        offsetY = devMagicWandControlOffsetYDpState
                    ),
                    play = BottomControlButtonAdjustments(
                        scale = devPlayControlScaleState,
                        offsetX = devPlayControlOffsetXDpState,
                        offsetY = devPlayControlOffsetYDpState
                    ),
                    auto = BottomControlButtonAdjustments(
                        scale = devAutoControlScaleState,
                        offsetX = devAutoControlOffsetXDpState,
                        offsetY = devAutoControlOffsetYDpState
                    )
                )
            )
    }

    private fun GameBoardLayoutConfig.withBottomControlAdjustments(values: BottomControlAdjustmentsSet): GameBoardLayoutConfig {
        return copy(
            undoControlAdjustments = values.undo,
            redoControlAdjustments = values.redo,
            hintControlAdjustments = values.hint,
            magicWandControlAdjustments = values.magicWand,
            playControlAdjustments = values.play,
            autoControlAdjustments = values.auto
        )
    }

    private fun LayoutProfileDevAdjusters.withBottomControlAdjustmentsFrom(source: LayoutProfileDevAdjusters): LayoutProfileDevAdjusters {
        return copy(
            portraitSlimCompact = portraitSlimCompact.withBottomControlAdjustments(source.portraitSlimCompact.bottomControlAdjustments()),
            portraitSlim = portraitSlim.withBottomControlAdjustments(source.portraitSlim.bottomControlAdjustments()),
            portraitClassic = portraitClassic.withBottomControlAdjustments(source.portraitClassic.bottomControlAdjustments()),
            portraitBroad = portraitBroad.withBottomControlAdjustments(source.portraitBroad.bottomControlAdjustments()),
            portraitSquare = portraitSquare.withBottomControlAdjustments(source.portraitSquare.bottomControlAdjustments()),
            landscapeSlimCompact = landscapeSlimCompact.withBottomControlAdjustments(source.landscapeSlimCompact.bottomControlAdjustments()),
            landscapeSlim = landscapeSlim.withBottomControlAdjustments(source.landscapeSlim.bottomControlAdjustments()),
            landscapeClassic = landscapeClassic.withBottomControlAdjustments(source.landscapeClassic.bottomControlAdjustments()),
            landscapeBroad = landscapeBroad.withBottomControlAdjustments(source.landscapeBroad.bottomControlAdjustments()),
            landscapeSquare = landscapeSquare.withBottomControlAdjustments(source.landscapeSquare.bottomControlAdjustments())
        )
    }

    private fun GameBoardLayoutConfig.bottomControlAdjustments(): BottomControlAdjustmentsSet {
        return BottomControlAdjustmentsSet(
            undo = undoControlAdjustments,
            redo = redoControlAdjustments,
            hint = hintControlAdjustments,
            magicWand = magicWandControlAdjustments,
            play = playControlAdjustments,
            auto = autoControlAdjustments
        )
    }

    private fun LayoutProfileDevAdjusters.currentDeviceLayoutConfig(): GameBoardLayoutConfig {
        val isLandscape = isLandscapeNow()
        return when (binding.gameBoardView.getCurrentAspectCategory()) {
            DeviceAspectCategory.SLIM -> {
                if (isLandscape) {
                    if (isCompactSlimLandscapeBoard()) landscapeSlimCompact else landscapeSlim
                } else {
                    if (isCompactSlimPortraitBoard()) portraitSlimCompact else portraitSlim
                }
            }
            DeviceAspectCategory.CLASSIC -> if (isLandscape) landscapeClassic else portraitClassic
            DeviceAspectCategory.BROAD -> if (isLandscape) landscapeBroad else portraitBroad
            DeviceAspectCategory.SQUARE -> if (isLandscape) landscapeSquare else portraitSquare
        }
    }

    private fun LayoutProfileDevAdjusters.withCurrentDeviceBottomControlAdjustments(values: BottomControlAdjustmentsSet): LayoutProfileDevAdjusters {
        val isLandscape = isLandscapeNow()
        return when (binding.gameBoardView.getCurrentAspectCategory()) {
            DeviceAspectCategory.SLIM -> {
                if (isLandscape) {
                    if (isCompactSlimLandscapeBoard()) copy(landscapeSlimCompact = landscapeSlimCompact.withBottomControlAdjustments(values))
                    else copy(landscapeSlim = landscapeSlim.withBottomControlAdjustments(values))
                } else {
                    if (isCompactSlimPortraitBoard()) copy(portraitSlimCompact = portraitSlimCompact.withBottomControlAdjustments(values))
                    else copy(portraitSlim = portraitSlim.withBottomControlAdjustments(values))
                }
            }
            DeviceAspectCategory.CLASSIC -> if (isLandscape) copy(landscapeClassic = landscapeClassic.withBottomControlAdjustments(values)) else copy(portraitClassic = portraitClassic.withBottomControlAdjustments(values))
            DeviceAspectCategory.BROAD -> if (isLandscape) copy(landscapeBroad = landscapeBroad.withBottomControlAdjustments(values)) else copy(portraitBroad = portraitBroad.withBottomControlAdjustments(values))
            DeviceAspectCategory.SQUARE -> if (isLandscape) copy(landscapeSquare = landscapeSquare.withBottomControlAdjustments(values)) else copy(portraitSquare = portraitSquare.withBottomControlAdjustments(values))
        }
    }

    private fun applyCurrentBottomControlAdjustersFromProfile(profile: LayoutProfileDevAdjusters) {
        val controls = profile.currentDeviceLayoutConfig().bottomControlAdjustments()
        devUndoControlScaleState = controls.undo.scale
        devUndoControlOffsetXDpState = controls.undo.offsetX
        devUndoControlOffsetYDpState = controls.undo.offsetY
        devRedoControlScaleState = controls.redo.scale
        devRedoControlOffsetXDpState = controls.redo.offsetX
        devRedoControlOffsetYDpState = controls.redo.offsetY
        devHintControlScaleState = controls.hint.scale
        devHintControlOffsetXDpState = controls.hint.offsetX
        devHintControlOffsetYDpState = controls.hint.offsetY
        devMagicWandControlScaleState = controls.magicWand.scale
        devMagicWandControlOffsetXDpState = controls.magicWand.offsetX
        devMagicWandControlOffsetYDpState = controls.magicWand.offsetY
        devPlayControlScaleState = controls.play.scale
        devPlayControlOffsetXDpState = controls.play.offsetX
        devPlayControlOffsetYDpState = controls.play.offsetY
        devAutoControlScaleState = controls.auto.scale
        devAutoControlOffsetXDpState = controls.auto.offsetX
        devAutoControlOffsetYDpState = controls.auto.offsetY
    }

    private fun applyLayoutScopedDevAdjusters(profile: LayoutProfileDevAdjusters) {
        val isPortrait = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        devLandscapeOverallOffsetXSlimCompactDpState = profile.landscapeSlimCompact.pileOverallOffsetX
        devLandscapeOverallOffsetYSlimCompactDpState = profile.landscapeSlimCompact.pileOverallOffsetY
        devLandscapeFoundationOffsetXSlimCompactDpState = profile.landscapeSlimCompact.foundationOffsetX
        devLandscapeFoundationOffsetYSlimCompactDpState = profile.landscapeSlimCompact.foundationOffsetY
        devLandscapeDrawWasteOffsetXSlimCompactDpState = profile.landscapeSlimCompact.drawWasteOffsetX
        devLandscapeDrawWasteOffsetYSlimCompactDpState = profile.landscapeSlimCompact.drawWasteOffsetY
        devLandscapeTableauOffsetXSlimCompactDpState = profile.landscapeSlimCompact.tableauOffsetX
        devLandscapeTableauOffsetYSlimCompactDpState = profile.landscapeSlimCompact.tableauOffsetY
        devLandscapeOverallOffsetXSlimDpState = profile.landscapeSlim.pileOverallOffsetX
        devLandscapeOverallOffsetYSlimDpState = profile.landscapeSlim.pileOverallOffsetY
        devLandscapeFoundationOffsetXSlimDpState = profile.landscapeSlim.foundationOffsetX
        devLandscapeFoundationOffsetYSlimDpState = profile.landscapeSlim.foundationOffsetY
        devLandscapeDrawWasteOffsetXSlimDpState = profile.landscapeSlim.drawWasteOffsetX
        devLandscapeDrawWasteOffsetYSlimDpState = profile.landscapeSlim.drawWasteOffsetY
        devLandscapeTableauOffsetXSlimDpState = profile.landscapeSlim.tableauOffsetX
        devLandscapeTableauOffsetYSlimDpState = profile.landscapeSlim.tableauOffsetY
        devLandscapeOverallOffsetXClassicDpState = profile.landscapeClassic.pileOverallOffsetX
        devLandscapeOverallOffsetYClassicDpState = profile.landscapeClassic.pileOverallOffsetY
        devLandscapeFoundationOffsetXClassicDpState = profile.landscapeClassic.foundationOffsetX
        devLandscapeFoundationOffsetYClassicDpState = profile.landscapeClassic.foundationOffsetY
        devLandscapeDrawWasteOffsetXClassicDpState = profile.landscapeClassic.drawWasteOffsetX
        devLandscapeDrawWasteOffsetYClassicDpState = profile.landscapeClassic.drawWasteOffsetY
        devLandscapeTableauOffsetXClassicDpState = profile.landscapeClassic.tableauOffsetX
        devLandscapeTableauOffsetYClassicDpState = profile.landscapeClassic.tableauOffsetY
        devLandscapeOverallOffsetXBroadDpState = profile.landscapeBroad.pileOverallOffsetX
        devLandscapeOverallOffsetYBroadDpState = profile.landscapeBroad.pileOverallOffsetY
        devLandscapeFoundationOffsetXBroadDpState = profile.landscapeBroad.foundationOffsetX
        devLandscapeFoundationOffsetYBroadDpState = profile.landscapeBroad.foundationOffsetY
        devLandscapeDrawWasteOffsetXBroadDpState = profile.landscapeBroad.drawWasteOffsetX
        devLandscapeDrawWasteOffsetYBroadDpState = profile.landscapeBroad.drawWasteOffsetY
        devLandscapeTableauOffsetXBroadDpState = profile.landscapeBroad.tableauOffsetX
        devLandscapeTableauOffsetYBroadDpState = profile.landscapeBroad.tableauOffsetY
        devLandscapeOverallOffsetXSquareDpState = profile.landscapeSquare.pileOverallOffsetX
        devLandscapeOverallOffsetYSquareDpState = profile.landscapeSquare.pileOverallOffsetY
        devLandscapeFoundationOffsetXSquareDpState = profile.landscapeSquare.foundationOffsetX
        devLandscapeFoundationOffsetYSquareDpState = profile.landscapeSquare.foundationOffsetY
        devLandscapeDrawWasteOffsetXSquareDpState = profile.landscapeSquare.drawWasteOffsetX
        devLandscapeDrawWasteOffsetYSquareDpState = profile.landscapeSquare.drawWasteOffsetY
        devLandscapeTableauOffsetXSquareDpState = profile.landscapeSquare.tableauOffsetX
        devLandscapeTableauOffsetYSquareDpState = profile.landscapeSquare.tableauOffsetY
        devLandscapePileStockOffsetXDpState = profile.landscapePileStockOffsetX
        devLandscapePileStockOffsetYDpState = profile.landscapePileStockOffsetY
        devLandscapePileWasteOffsetXDpState = profile.landscapePileWasteOffsetX
        devLandscapePileWasteOffsetYDpState = profile.landscapePileWasteOffsetY
        devPortraitOverallOffsetXSlimCompactDpState = profile.portraitSlimCompact.pileOverallOffsetX
        devPortraitOverallOffsetYSlimCompactDpState = profile.portraitSlimCompact.pileOverallOffsetY
        devPortraitFoundationOffsetXSlimCompactDpState = profile.portraitSlimCompact.foundationOffsetX
        devPortraitFoundationOffsetYSlimCompactDpState = profile.portraitSlimCompact.foundationOffsetY
        devPortraitDrawWasteOffsetXSlimCompactDpState = profile.portraitSlimCompact.drawWasteOffsetX
        devPortraitDrawWasteOffsetYSlimCompactDpState = profile.portraitSlimCompact.drawWasteOffsetY
        devPortraitTableauOffsetXSlimCompactDpState = profile.portraitSlimCompact.tableauOffsetX
        devPortraitTableauOffsetYSlimCompactDpState = profile.portraitSlimCompact.tableauOffsetY
        // Apply HUD offsets from orientation-specific fields
        devScoreboardOffsetXDpState = if (isPortrait) profile.portraitScoreboardOffsetX else profile.landscapeScoreboardOffsetX
        devScoreboardOffsetYDpState = if (isPortrait) profile.portraitScoreboardOffsetY else profile.landscapeScoreboardOffsetY
        devGemRewardOffsetXDpState = if (isPortrait) profile.portraitGemRewardOffsetX else profile.landscapeGemRewardOffsetX
        devGemRewardOffsetYDpState = if (isPortrait) profile.portraitGemRewardOffsetY else profile.landscapeGemRewardOffsetY
        devTicketRewardOffsetXDpState = if (isPortrait) profile.portraitTicketRewardOffsetX else profile.landscapeTicketRewardOffsetX
        devTicketRewardOffsetYDpState = if (isPortrait) profile.portraitTicketRewardOffsetY else profile.landscapeTicketRewardOffsetY
        applyCurrentBottomControlAdjustersFromProfile(profile)
        devGemRewardScaleState = profile.gemRewardAdjustments.iconScale
        devGemRewardCounterOffsetXDpState = profile.gemRewardAdjustments.counterOffsetX
        devGemRewardCounterOffsetYDpState = profile.gemRewardAdjustments.counterOffsetY
        devGemRewardCounterScaleState = profile.gemRewardAdjustments.counterScale
        devTicketRewardScaleState = profile.ticketRewardAdjustments.iconScale
        devTicketRewardCounterOffsetXDpState = profile.ticketRewardAdjustments.counterOffsetX
        devTicketRewardCounterOffsetYDpState = profile.ticketRewardAdjustments.counterOffsetY
        devTicketRewardCounterScaleState = profile.ticketRewardAdjustments.counterScale
//        devAdBoxChoiceState = resolveLandscapeAdBoxChoice(profile)
        devLandscapeAdBoxChoiceSlimCompactState = profile.landscapeSlimCompact.adBoxChoice
        devLandscapeAdBoxChoiceSlimState = profile.landscapeSlim.adBoxChoice
        devLandscapeAdBoxChoiceClassicState = profile.landscapeClassic.adBoxChoice
        devLandscapeAdBoxChoiceBroadState = profile.landscapeBroad.adBoxChoice
        devLandscapeAdBoxChoiceSquareState = profile.landscapeSquare.adBoxChoice
        devPortraitAdBoxChoiceSlimCompactState = profile.portraitSlimCompact.adBoxChoice
        devPortraitAdBoxChoiceSlimState = profile.portraitSlim.adBoxChoice
        devPortraitAdBoxChoiceClassicState = profile.portraitClassic.adBoxChoice
        devPortraitAdBoxChoiceBroadState = profile.portraitBroad.adBoxChoice
        devPortraitAdBoxChoiceSquareState = profile.portraitSquare.adBoxChoice
        devPortraitOverallOffsetXSlimDpState = profile.portraitSlim.pileOverallOffsetX
        devPortraitOverallOffsetYSlimDpState = profile.portraitSlim.pileOverallOffsetY
        devPortraitFoundationOffsetXSlimDpState    = profile.portraitSlim.foundationOffsetX
        devPortraitFoundationOffsetYSlimDpState    = profile.portraitSlim.foundationOffsetY
        devPortraitDrawWasteOffsetXSlimDpState     = profile.portraitSlim.drawWasteOffsetX
        devPortraitDrawWasteOffsetYSlimDpState     = profile.portraitSlim.drawWasteOffsetY
        devPortraitTableauOffsetXSlimDpState = profile.portraitSlim.tableauOffsetX
        devPortraitTableauOffsetYSlimDpState = profile.portraitSlim.tableauOffsetY
        devPortraitOverallOffsetXClassicDpState = profile.portraitClassic.pileOverallOffsetX
        devPortraitOverallOffsetYClassicDpState = profile.portraitClassic.pileOverallOffsetY
        devPortraitFoundationOffsetXClassicDpState = profile.portraitClassic.foundationOffsetX
        devPortraitFoundationOffsetYClassicDpState = profile.portraitClassic.foundationOffsetY
        devPortraitDrawWasteOffsetXClassicDpState  = profile.portraitClassic.drawWasteOffsetX
        devPortraitDrawWasteOffsetYClassicDpState  = profile.portraitClassic.drawWasteOffsetY
        devPortraitTableauOffsetXClassicDpState = profile.portraitClassic.tableauOffsetX
        devPortraitTableauOffsetYClassicDpState = profile.portraitClassic.tableauOffsetY
        devPortraitOverallOffsetXBroadDpState = profile.portraitBroad.pileOverallOffsetX
        devPortraitOverallOffsetYBroadDpState = profile.portraitBroad.pileOverallOffsetY
        devPortraitFoundationOffsetXBroadDpState   = profile.portraitBroad.foundationOffsetX
        devPortraitFoundationOffsetYBroadDpState   = profile.portraitBroad.foundationOffsetY
        devPortraitDrawWasteOffsetXBroadDpState    = profile.portraitBroad.drawWasteOffsetX
        devPortraitDrawWasteOffsetYBroadDpState    = profile.portraitBroad.drawWasteOffsetY
        devPortraitTableauOffsetXBroadDpState = profile.portraitBroad.tableauOffsetX
        devPortraitTableauOffsetYBroadDpState = profile.portraitBroad.tableauOffsetY
        applyCurrentBannerOffsetsFromProfile(profile)
        devPortraitOverallOffsetXSquareDpState = profile.portraitSquare.pileOverallOffsetX
        devPortraitOverallOffsetYSquareDpState = profile.portraitSquare.pileOverallOffsetY
        devPortraitFoundationOffsetXSquareDpState  = profile.portraitSquare.foundationOffsetX
        devPortraitFoundationOffsetYSquareDpState  = profile.portraitSquare.foundationOffsetY
        devPortraitDrawWasteOffsetXSquareDpState   = profile.portraitSquare.drawWasteOffsetX
        devPortraitDrawWasteOffsetYSquareDpState   = profile.portraitSquare.drawWasteOffsetY
        devPortraitTableauOffsetXSquareDpState = profile.portraitSquare.tableauOffsetX
        devPortraitTableauOffsetYSquareDpState = profile.portraitSquare.tableauOffsetY
        devPortraitPileStockOffsetXDpState = profile.portraitPileStockOffsetX
        devPortraitPileStockOffsetYDpState = profile.portraitPileStockOffsetY
        devPortraitPileWasteOffsetXDpState = profile.portraitPileWasteOffsetX
        devPortraitPileWasteOffsetYDpState = profile.portraitPileWasteOffsetY
    }

    private fun normalizeDeckCountForLayoutProfile(rawDeckCount: Int): Int {
        return if (rawDeckCount == 2) 2 else 1
    }

    private fun resolveActiveLayoutProfileKey(): LayoutProfileKey {
        return LayoutProfileKey(
            mirrored = viewModel.isMirroredLayout.value,
            deckCount = normalizeDeckCountForLayoutProfile(viewModel.game.value.deckCount)
        )
    }

    private fun profileStateFor(key: LayoutProfileKey): LayoutProfileDevAdjusters {
        return when {
            key.mirrored && key.deckCount == 2 -> mirrored2DeckLayoutDevAdjustersState
            key.mirrored -> mirrored1DeckLayoutDevAdjustersState
            key.deckCount == 2 -> classic2DeckLayoutDevAdjustersState
            else -> classic1DeckLayoutDevAdjustersState
        }
    }

    private fun setProfileStateFor(key: LayoutProfileKey, profile: LayoutProfileDevAdjusters) {
        when {
            key.mirrored && key.deckCount == 2 -> mirrored2DeckLayoutDevAdjustersState = profile
            key.mirrored -> mirrored1DeckLayoutDevAdjustersState = profile
            key.deckCount == 2 -> classic2DeckLayoutDevAdjustersState = profile
            else -> classic1DeckLayoutDevAdjustersState = profile
        }
    }

    private fun persistActiveLayoutScopedDevAdjusters(profileKey: LayoutProfileKey) {
        if (!hasAppliedLayoutScopedDevProfile) return
        setProfileStateFor(profileKey, snapshotLayoutScopedDevAdjusters())
    }

    private fun currentPortraitAspectPileOffsets(): GameBoardLayoutConfig {
        return when (binding.gameBoardView.getCurrentAspectCategory()) {
            DeviceAspectCategory.SLIM -> {
                if (isCompactSlimPortraitBoard()) {
                    GameBoardLayoutConfig(
                        pileOverallOffsetX = devPortraitOverallOffsetXSlimCompactDpState,
                        pileOverallOffsetY = devPortraitOverallOffsetYSlimCompactDpState,
                        foundationOffsetX = devPortraitFoundationOffsetXSlimCompactDpState,
                        foundationOffsetY = devPortraitFoundationOffsetYSlimCompactDpState,
                        drawWasteOffsetX = devPortraitDrawWasteOffsetXSlimCompactDpState,
                        drawWasteOffsetY = devPortraitDrawWasteOffsetYSlimCompactDpState,
                        tableauOffsetX = devPortraitTableauOffsetXSlimCompactDpState,
                        tableauOffsetY = devPortraitTableauOffsetYSlimCompactDpState
                    )
                } else {
                    GameBoardLayoutConfig(
                        pileOverallOffsetX = devPortraitOverallOffsetXSlimDpState,
                        pileOverallOffsetY = devPortraitOverallOffsetYSlimDpState,
                        foundationOffsetX = devPortraitFoundationOffsetXSlimDpState,
                        foundationOffsetY = devPortraitFoundationOffsetYSlimDpState,
                        drawWasteOffsetX = devPortraitDrawWasteOffsetXSlimDpState,
                        drawWasteOffsetY = devPortraitDrawWasteOffsetYSlimDpState,
                        tableauOffsetX = devPortraitTableauOffsetXSlimDpState,
                        tableauOffsetY = devPortraitTableauOffsetYSlimDpState
                    )
                }
            }
            DeviceAspectCategory.CLASSIC -> GameBoardLayoutConfig(
                pileOverallOffsetX = devPortraitOverallOffsetXClassicDpState,
                pileOverallOffsetY = devPortraitOverallOffsetYClassicDpState,
                foundationOffsetX = devPortraitFoundationOffsetXClassicDpState,
                foundationOffsetY = devPortraitFoundationOffsetYClassicDpState,
                drawWasteOffsetX = devPortraitDrawWasteOffsetXClassicDpState,
                drawWasteOffsetY = devPortraitDrawWasteOffsetYClassicDpState,
                tableauOffsetX = devPortraitTableauOffsetXClassicDpState,
                tableauOffsetY = devPortraitTableauOffsetYClassicDpState
            )
            DeviceAspectCategory.BROAD   -> GameBoardLayoutConfig(
                pileOverallOffsetX = devPortraitOverallOffsetXBroadDpState,
                pileOverallOffsetY = devPortraitOverallOffsetYBroadDpState,
                foundationOffsetX = devPortraitFoundationOffsetXBroadDpState,
                foundationOffsetY = devPortraitFoundationOffsetYBroadDpState,
                drawWasteOffsetX = devPortraitDrawWasteOffsetXBroadDpState,
                drawWasteOffsetY = devPortraitDrawWasteOffsetYBroadDpState,
                tableauOffsetX = devPortraitTableauOffsetXBroadDpState,
                tableauOffsetY = devPortraitTableauOffsetYBroadDpState
            )
            DeviceAspectCategory.SQUARE  -> GameBoardLayoutConfig(
                pileOverallOffsetX = devPortraitOverallOffsetXSquareDpState,
                pileOverallOffsetY = devPortraitOverallOffsetYSquareDpState,
                foundationOffsetX = devPortraitFoundationOffsetXSquareDpState,
                foundationOffsetY = devPortraitFoundationOffsetYSquareDpState,
                drawWasteOffsetX = devPortraitDrawWasteOffsetXSquareDpState,
                drawWasteOffsetY = devPortraitDrawWasteOffsetYSquareDpState,
                tableauOffsetX = devPortraitTableauOffsetXSquareDpState,
                tableauOffsetY = devPortraitTableauOffsetYSquareDpState
            )
        }
    }

    private fun setCurrentPortraitAspectPileOffsets(offsets: GameBoardLayoutConfig) {
        when (binding.gameBoardView.getCurrentAspectCategory()) {
            DeviceAspectCategory.SLIM -> {
                if (isCompactSlimPortraitBoard()) {
                    devPortraitOverallOffsetXSlimCompactDpState = offsets.pileOverallOffsetX
                    devPortraitOverallOffsetYSlimCompactDpState = offsets.pileOverallOffsetY
                    devPortraitFoundationOffsetXSlimCompactDpState = offsets.foundationOffsetX
                    devPortraitFoundationOffsetYSlimCompactDpState = offsets.foundationOffsetY
                    devPortraitDrawWasteOffsetXSlimCompactDpState = offsets.drawWasteOffsetX
                    devPortraitDrawWasteOffsetYSlimCompactDpState = offsets.drawWasteOffsetY
                    devPortraitTableauOffsetXSlimCompactDpState = offsets.tableauOffsetX
                    devPortraitTableauOffsetYSlimCompactDpState = offsets.tableauOffsetY
                } else {
                    devPortraitOverallOffsetXSlimDpState = offsets.pileOverallOffsetX
                    devPortraitOverallOffsetYSlimDpState = offsets.pileOverallOffsetY
                    devPortraitFoundationOffsetXSlimDpState = offsets.foundationOffsetX
                    devPortraitFoundationOffsetYSlimDpState = offsets.foundationOffsetY
                    devPortraitDrawWasteOffsetXSlimDpState = offsets.drawWasteOffsetX
                    devPortraitDrawWasteOffsetYSlimDpState = offsets.drawWasteOffsetY
                    devPortraitTableauOffsetXSlimDpState = offsets.tableauOffsetX
                    devPortraitTableauOffsetYSlimDpState = offsets.tableauOffsetY
                }
            }
            DeviceAspectCategory.CLASSIC -> {
                devPortraitOverallOffsetXClassicDpState = offsets.pileOverallOffsetX
                devPortraitOverallOffsetYClassicDpState = offsets.pileOverallOffsetY
                devPortraitFoundationOffsetXClassicDpState = offsets.foundationOffsetX
                devPortraitFoundationOffsetYClassicDpState = offsets.foundationOffsetY
                devPortraitDrawWasteOffsetXClassicDpState  = offsets.drawWasteOffsetX
                devPortraitDrawWasteOffsetYClassicDpState  = offsets.drawWasteOffsetY
                devPortraitTableauOffsetXClassicDpState = offsets.tableauOffsetX
                devPortraitTableauOffsetYClassicDpState = offsets.tableauOffsetY
            }
            DeviceAspectCategory.BROAD -> {
                devPortraitOverallOffsetXBroadDpState = offsets.pileOverallOffsetX
                devPortraitOverallOffsetYBroadDpState = offsets.pileOverallOffsetY
                devPortraitFoundationOffsetXBroadDpState = offsets.foundationOffsetX
                devPortraitFoundationOffsetYBroadDpState = offsets.foundationOffsetY
                devPortraitDrawWasteOffsetXBroadDpState  = offsets.drawWasteOffsetX
                devPortraitDrawWasteOffsetYBroadDpState  = offsets.drawWasteOffsetY
                devPortraitTableauOffsetXBroadDpState = offsets.tableauOffsetX
                devPortraitTableauOffsetYBroadDpState = offsets.tableauOffsetY
            }
            DeviceAspectCategory.SQUARE -> {
                devPortraitOverallOffsetXSquareDpState = offsets.pileOverallOffsetX
                devPortraitOverallOffsetYSquareDpState = offsets.pileOverallOffsetY
                devPortraitFoundationOffsetXSquareDpState = offsets.foundationOffsetX
                devPortraitFoundationOffsetYSquareDpState = offsets.foundationOffsetY
                devPortraitDrawWasteOffsetXSquareDpState  = offsets.drawWasteOffsetX
                devPortraitDrawWasteOffsetYSquareDpState  = offsets.drawWasteOffsetY
                devPortraitTableauOffsetXSquareDpState = offsets.tableauOffsetX
                devPortraitTableauOffsetYSquareDpState = offsets.tableauOffsetY
            }
        }
    }

    private fun resolveLandscapeAdBoxChoice(profile: LayoutProfileDevAdjusters): BannerAdBoxChoice {
        return when (binding.gameBoardView.getCurrentAspectCategory()) {
            DeviceAspectCategory.SLIM -> {
                if (isLandscapeNow()){
                    if (isCompactSlimLandscapeBoard()) {
                        profile.landscapeSlimCompact.adBoxChoice
                    } else {
                        profile.landscapeSlim.adBoxChoice
                    }
                } else {
                    if (isCompactSlimPortraitBoard()) {
                        profile.portraitSlimCompact.adBoxChoice
                    } else {
                        profile.portraitSlim.adBoxChoice
                    }
                }
            }
            DeviceAspectCategory.CLASSIC -> {
                if (isLandscapeNow()){
                    profile.landscapeClassic.adBoxChoice
                } else {
                    profile.portraitClassic.adBoxChoice
                }
            }
            DeviceAspectCategory.BROAD -> {
                if (isLandscapeNow()){
                    profile.landscapeBroad.adBoxChoice
                } else {
                    profile.portraitBroad.adBoxChoice
                }
            }
            DeviceAspectCategory.SQUARE -> {
                if (isLandscapeNow()){
                    profile.landscapeSquare.adBoxChoice
                } else {
                    profile.portraitSquare.adBoxChoice
                }
            }
        }
    }

    private fun isCompactSlimPortraitBoard(): Boolean {
        val boardWidthPx = binding.gameBoardView.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val isPortrait = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        return isPortrait && boardWidthPx <= portraitSlimCompactMaxBoardWidthPx
    }

    private fun currentLandscapeAspectPileOffsets(): GameBoardLayoutConfig {
        return when (binding.gameBoardView.getCurrentAspectCategory()) {
            DeviceAspectCategory.SLIM -> {
                if (isCompactSlimLandscapeBoard()) {
                    GameBoardLayoutConfig(
                        pileOverallOffsetX = devLandscapeOverallOffsetXSlimCompactDpState,
                        pileOverallOffsetY = devLandscapeOverallOffsetYSlimCompactDpState,
                        foundationOffsetX = devLandscapeFoundationOffsetXSlimCompactDpState,
                        foundationOffsetY = devLandscapeFoundationOffsetYSlimCompactDpState,
                        drawWasteOffsetX = devLandscapeDrawWasteOffsetXSlimCompactDpState,
                        drawWasteOffsetY = devLandscapeDrawWasteOffsetYSlimCompactDpState,
                        tableauOffsetX = devLandscapeTableauOffsetXSlimCompactDpState,
                        tableauOffsetY = devLandscapeTableauOffsetYSlimCompactDpState,
                        adBoxChoice = devLandscapeAdBoxChoiceSlimCompactState
                    )
                } else {
                    GameBoardLayoutConfig(
                        pileOverallOffsetX = devLandscapeOverallOffsetXSlimDpState,
                        pileOverallOffsetY = devLandscapeOverallOffsetYSlimDpState,
                        foundationOffsetX = devLandscapeFoundationOffsetXSlimDpState,
                        foundationOffsetY = devLandscapeFoundationOffsetYSlimDpState,
                        drawWasteOffsetX = devLandscapeDrawWasteOffsetXSlimDpState,
                        drawWasteOffsetY = devLandscapeDrawWasteOffsetYSlimDpState,
                        tableauOffsetX = devLandscapeTableauOffsetXSlimDpState,
                        tableauOffsetY = devLandscapeTableauOffsetYSlimDpState,
                        adBoxChoice = devLandscapeAdBoxChoiceSlimState
                    )
                }
            }
            DeviceAspectCategory.CLASSIC -> GameBoardLayoutConfig(
                pileOverallOffsetX = devLandscapeOverallOffsetXClassicDpState,
                pileOverallOffsetY = devLandscapeOverallOffsetYClassicDpState,
                foundationOffsetX = devLandscapeFoundationOffsetXClassicDpState,
                foundationOffsetY = devLandscapeFoundationOffsetYClassicDpState,
                drawWasteOffsetX = devLandscapeDrawWasteOffsetXClassicDpState,
                drawWasteOffsetY = devLandscapeDrawWasteOffsetYClassicDpState,
                tableauOffsetX = devLandscapeTableauOffsetXClassicDpState,
                tableauOffsetY = devLandscapeTableauOffsetYClassicDpState,
                adBoxChoice = devLandscapeAdBoxChoiceClassicState
            )
            DeviceAspectCategory.BROAD -> GameBoardLayoutConfig(
                pileOverallOffsetX = devLandscapeOverallOffsetXBroadDpState,
                pileOverallOffsetY = devLandscapeOverallOffsetYBroadDpState,
                foundationOffsetX = devLandscapeFoundationOffsetXBroadDpState,
                foundationOffsetY = devLandscapeFoundationOffsetYBroadDpState,
                drawWasteOffsetX = devLandscapeDrawWasteOffsetXBroadDpState,
                drawWasteOffsetY = devLandscapeDrawWasteOffsetYBroadDpState,
                tableauOffsetX = devLandscapeTableauOffsetXBroadDpState,
                tableauOffsetY = devLandscapeTableauOffsetYBroadDpState,
                adBoxChoice = devLandscapeAdBoxChoiceBroadState
            )
            DeviceAspectCategory.SQUARE -> GameBoardLayoutConfig(
                pileOverallOffsetX = devLandscapeOverallOffsetXSquareDpState,
                pileOverallOffsetY = devLandscapeOverallOffsetYSquareDpState,
                foundationOffsetX = devLandscapeFoundationOffsetXSquareDpState,
                foundationOffsetY = devLandscapeFoundationOffsetYSquareDpState,
                drawWasteOffsetX = devLandscapeDrawWasteOffsetXSquareDpState,
                drawWasteOffsetY = devLandscapeDrawWasteOffsetYSquareDpState,
                tableauOffsetX = devLandscapeTableauOffsetXSquareDpState,
                tableauOffsetY = devLandscapeTableauOffsetYSquareDpState,
                adBoxChoice = devLandscapeAdBoxChoiceSquareState
            )
        }
    }

    private fun setCurrentLandscapeAspectPileOffsets(cfg: GameBoardLayoutConfig) {
        when (binding.gameBoardView.getCurrentAspectCategory()) {
            DeviceAspectCategory.SLIM -> {
                if (isCompactSlimLandscapeBoard()) {
                    devLandscapeOverallOffsetXSlimCompactDpState = cfg.pileOverallOffsetX
                    devLandscapeOverallOffsetYSlimCompactDpState = cfg.pileOverallOffsetY
                    devLandscapeFoundationOffsetXSlimCompactDpState = cfg.foundationOffsetX
                    devLandscapeFoundationOffsetYSlimCompactDpState = cfg.foundationOffsetY
                    devLandscapeDrawWasteOffsetXSlimCompactDpState = cfg.drawWasteOffsetX
                    devLandscapeDrawWasteOffsetYSlimCompactDpState = cfg.drawWasteOffsetY
                    devLandscapeTableauOffsetXSlimCompactDpState = cfg.tableauOffsetX
                    devLandscapeTableauOffsetYSlimCompactDpState = cfg.tableauOffsetY
                    devLandscapeAdBoxChoiceSlimCompactState = cfg.adBoxChoice
                } else {
                    devLandscapeOverallOffsetXSlimDpState = cfg.pileOverallOffsetX
                    devLandscapeOverallOffsetYSlimDpState = cfg.pileOverallOffsetY
                    devLandscapeFoundationOffsetXSlimDpState = cfg.foundationOffsetX
                    devLandscapeFoundationOffsetYSlimDpState = cfg.foundationOffsetY
                    devLandscapeDrawWasteOffsetXSlimDpState = cfg.drawWasteOffsetX
                    devLandscapeDrawWasteOffsetYSlimDpState = cfg.drawWasteOffsetY
                    devLandscapeTableauOffsetXSlimDpState = cfg.tableauOffsetX
                    devLandscapeTableauOffsetYSlimDpState = cfg.tableauOffsetY
                    devLandscapeAdBoxChoiceSlimState = cfg.adBoxChoice
                }
            }
            DeviceAspectCategory.CLASSIC -> {
                devLandscapeOverallOffsetXClassicDpState = cfg.pileOverallOffsetX
                devLandscapeOverallOffsetYClassicDpState = cfg.pileOverallOffsetY
                devLandscapeFoundationOffsetXClassicDpState = cfg.foundationOffsetX
                devLandscapeFoundationOffsetYClassicDpState = cfg.foundationOffsetY
                devLandscapeDrawWasteOffsetXClassicDpState = cfg.drawWasteOffsetX
                devLandscapeDrawWasteOffsetYClassicDpState = cfg.drawWasteOffsetY
                devLandscapeTableauOffsetXClassicDpState = cfg.tableauOffsetX
                devLandscapeTableauOffsetYClassicDpState = cfg.tableauOffsetY
                devLandscapeAdBoxChoiceClassicState = cfg.adBoxChoice
            }
            DeviceAspectCategory.BROAD -> {
                devLandscapeOverallOffsetXBroadDpState = cfg.pileOverallOffsetX
                devLandscapeOverallOffsetYBroadDpState = cfg.pileOverallOffsetY
                devLandscapeFoundationOffsetXBroadDpState = cfg.foundationOffsetX
                devLandscapeFoundationOffsetYBroadDpState = cfg.foundationOffsetY
                devLandscapeDrawWasteOffsetXBroadDpState = cfg.drawWasteOffsetX
                devLandscapeDrawWasteOffsetYBroadDpState = cfg.drawWasteOffsetY
                devLandscapeTableauOffsetXBroadDpState = cfg.tableauOffsetX
                devLandscapeTableauOffsetYBroadDpState = cfg.tableauOffsetY
                devLandscapeAdBoxChoiceBroadState = cfg.adBoxChoice
            }
            DeviceAspectCategory.SQUARE -> {
                devLandscapeOverallOffsetXSquareDpState = cfg.pileOverallOffsetX
                devLandscapeOverallOffsetYSquareDpState = cfg.pileOverallOffsetY
                devLandscapeFoundationOffsetXSquareDpState = cfg.foundationOffsetX
                devLandscapeFoundationOffsetYSquareDpState = cfg.foundationOffsetY
                devLandscapeDrawWasteOffsetXSquareDpState = cfg.drawWasteOffsetX
                devLandscapeDrawWasteOffsetYSquareDpState = cfg.drawWasteOffsetY
                devLandscapeTableauOffsetXSquareDpState = cfg.tableauOffsetX
                devLandscapeTableauOffsetYSquareDpState = cfg.tableauOffsetY
                devLandscapeAdBoxChoiceSquareState = cfg.adBoxChoice
            }
        }
    }

    private fun isCompactSlimLandscapeBoard(): Boolean {
        val boardHeightPx = binding.gameBoardView.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return isLandscape && boardHeightPx <= landscapeSlimCompactMaxBoardHeightPx
    }

    private fun switchLayoutScopedDevAdjustersIfNeeded(profileKey: LayoutProfileKey): Boolean {
        val previous = appliedLayoutProfileKey
        if (previous == profileKey) return false
        if (previous != null) {
            persistActiveLayoutScopedDevAdjusters(previous)
        }
        val nextProfile = profileStateFor(profileKey)
        applyLayoutScopedDevAdjusters(nextProfile)
        appliedLayoutProfileKey = profileKey
        hasAppliedLayoutScopedDevProfile = true
        return true
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
    /** Tracks which dev profile (layout + deck count) is loaded into active dev state. */
    private var appliedLayoutProfileKey: LayoutProfileKey? = null
    /** True after deck-aware profile defaults have been applied into active dev state at least once. */
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
         devButtonRowOffsetXDpState = (if (isLandscape) BASELINE_WIN_BUTTON_ROW_OFFSET_X_DP_LANDSCAPE else (if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) BASELINE_WIN_BUTTON_ROW_OFFSET_X_DP_PORTRAIT else BASELINE_WIN_BUTTON_ROW_OFFSET_X_DP_PORTRAIT_BAKLAVA)) * ratio
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
        configureHudClipBehavior()
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
        applyLandscapePileLayoutDevConfigToBoard(persistProfile = false)
        applyPortraitPileLayoutDevConfigToBoard(persistProfile = false)
        applyAspectCategoryPileTrimsToBoard()
        persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
        applyTopHudDevOffsets(persistProfile = false)
        
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
        // Delay banner reload until after GameBoardView is laid out so aspect category can be determined
        binding.gameBoardView.post {
            reloadBannerForCurrentConfiguration()
        }
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
                        val profileChanged = switchLayoutScopedDevAdjustersIfNeeded(resolveActiveLayoutProfileKey())
                        if (profileChanged) {
                            applyLandscapePileLayoutDevConfigToBoard(persistProfile = false)
                            applyPortraitPileLayoutDevConfigToBoard(persistProfile = false)
                            applyResponsiveControlSizing()
                            persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
                            applyTopHudDevOffsets(persistProfile = false)
                            // Ensure startup banner size matches the active profile/aspect immediately.
                            reloadBannerForCurrentConfiguration()
                        }
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
                            if (canUndo) R.drawable.button_undo_red else R.drawable.button_undo_grey
                        )
                    }
                }

                launch {
                    viewModel.canRedo.collect { canRedo ->
                        findViewById<ImageView>(R.id.redo_main)?.setImageResource(
                            if (canRedo) R.drawable.button_redo_blue else R.drawable.button_redo_grey
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
            handleUndoClick()
        }
        binding.btnRedo.setOnClickListener { buttonView ->
            performUiActionHaptic(buttonView)
            handleRedoClick()
        }
        // btn_new_game and btn_restart are hidden stubs; actions are in the Play popup.
        binding.btnNewGame.setOnClickListener {
            winCelebrationPlayed = false
            startNewGameWithShuffleAndDealAnimation()
        }
        // btn_restart stub kept for ViewBinding; handled via Play popup
        findViewById<View>(R.id.btn_hint)?.setOnClickListener { buttonView ->
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
        binding.magicWandContainer.setOnClickListener { onMagicWandClicked() }
        findViewById<View>(R.id.btn_auto_move)?.setOnClickListener { buttonView ->
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
        val successConstant = HapticFeedbackConstants.CONFIRM
        performHapticFeedback(successConstant, sourceView)
    }

    private fun performErrorHaptic(sourceView: View? = null) {
        val errorConstant = HapticFeedbackConstants.REJECT
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
        binding.tvMagicWandCount.text = safeTotal.toString()
        binding.tvMagicWandCount.visibility = if (safeTotal > 0) View.VISIBLE else View.GONE
        binding.ivMagicWandAdBadge.visibility = if (safeTotal == 0) View.VISIBLE else View.GONE
    }


    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.restart_game_title)
            .setMessage(R.string.restart_game_message)
            .setPositiveButton(R.string.restart_game_text) { _, _ ->
                lifecycleScope.launch {
                    restartGameWithShuffleAndDealAnimation()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
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
        binding.magicWandContainer.alpha = if (enabled) 0.7f else 1f
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
        val popupView = layoutInflater.inflate(R.layout.popup_play_menu, binding.root, false)
        val popup = android.widget.PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popup.elevation = 16f

        popupView.findViewById<TextView>(R.id.popup_btn_new_game).setOnClickListener {
            popup.dismiss()
            winCelebrationPlayed = false
            startNewGameWithShuffleAndDealAnimation()
        }
        popupView.findViewById<TextView>(R.id.popup_btn_restart).setOnClickListener {
            popup.dismiss()
            handleRestartClick()
        }
        popupView.findViewById<TextView>(R.id.popup_btn_menu).setOnClickListener {
            popup.dismiss()
            showGameMenu()
        }
        popupView.findViewById<TextView>(R.id.popup_btn_testers).setOnClickListener {
            popup.dismiss()
            showTesterMenu()
        }
        popupView.findViewById<TextView>(R.id.popup_btn_develop).setOnClickListener {
            popup.dismiss()
            showDevelopMenu()
        }

        // Show above the anchor button
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val offsetY = -popupView.measuredHeight - anchor.height
        popup.showAsDropDown(anchor, 0, offsetY)
    }

    private fun applyMirroredLayoutUi(mirrored: Boolean) {
        switchLayoutScopedDevAdjustersIfNeeded(resolveActiveLayoutProfileKey())
        applyLandscapePileLayoutDevConfigToBoard(persistProfile = false)
        applyPortraitPileLayoutDevConfigToBoard(persistProfile = false)
        applyAspectCategoryPileTrimsToBoard()
        persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())

        // Flip the info_side_panel to the opposite side
        val infoPanel = findViewById<View>(R.id.info_side_panel) ?: return
        val clp = infoPanel.layoutParams as? ConstraintLayout.LayoutParams ?: return
        val marginPx = dpToPx(6f)
        if (mirrored) {
            clp.startToStart = R.id.game_board_view
            clp.endToEnd = ConstraintLayout.LayoutParams.UNSET
            clp.marginStart = marginPx
            clp.marginEnd = 0
        } else {
            clp.endToEnd = R.id.game_board_view
            clp.startToStart = ConstraintLayout.LayoutParams.UNSET
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

        updateBannerPlacementForCurrentConfiguration()

        // Keep HUD/control sizing tied to device + deck state, not mirrored hand preference.
        applyResponsiveControlSizing()
        infoPanel.post { applyTopHudDevOffsets(persistProfile = false) }
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
    override fun devLandscapePileOverallOffsetXDp(): Float = currentLandscapeAspectPileOffsets().pileOverallOffsetX
    override fun devLandscapePileOverallOffsetYDp(): Float = currentLandscapeAspectPileOffsets().pileOverallOffsetY
    override fun devLandscapePileFoundationOffsetXDp(): Float = currentLandscapeAspectPileOffsets().foundationOffsetX
    override fun devLandscapePileFoundationOffsetYDp(): Float = currentLandscapeAspectPileOffsets().foundationOffsetY
    override fun devLandscapePileDrawWasteOffsetXDp(): Float = currentLandscapeAspectPileOffsets().drawWasteOffsetX
    override fun devLandscapePileDrawWasteOffsetYDp(): Float = currentLandscapeAspectPileOffsets().drawWasteOffsetY
    override fun devLandscapePileStockOffsetXDp(): Float = devLandscapePileStockOffsetXDpState
    override fun devLandscapePileStockOffsetYDp(): Float = devLandscapePileStockOffsetYDpState
    override fun devLandscapePileWasteOffsetXDp(): Float = devLandscapePileWasteOffsetXDpState
    override fun devLandscapePileWasteOffsetYDp(): Float = devLandscapePileWasteOffsetYDpState
    override fun devLandscapePileTableauOffsetXDp(): Float = currentLandscapeAspectPileOffsets().tableauOffsetX
    override fun devLandscapePileTableauOffsetYDp(): Float = currentLandscapeAspectPileOffsets().tableauOffsetY
    override fun devPortraitPileOverallOffsetXDp(): Float = currentPortraitAspectPileOffsets().pileOverallOffsetX
    override fun devPortraitPileOverallOffsetYDp(): Float = currentPortraitAspectPileOffsets().pileOverallOffsetY
    override fun devPortraitPileFoundationOffsetXDp(): Float = currentPortraitAspectPileOffsets().foundationOffsetX
    override fun devPortraitPileFoundationOffsetYDp(): Float = currentPortraitAspectPileOffsets().foundationOffsetY
    override fun devPortraitPileDrawWasteOffsetXDp(): Float = currentPortraitAspectPileOffsets().drawWasteOffsetX
    override fun devPortraitPileDrawWasteOffsetYDp(): Float = currentPortraitAspectPileOffsets().drawWasteOffsetY
    override fun devPortraitPileStockOffsetXDp(): Float = devPortraitPileStockOffsetXDpState
    override fun devPortraitPileStockOffsetYDp(): Float = devPortraitPileStockOffsetYDpState
    override fun devPortraitPileWasteOffsetXDp(): Float = devPortraitPileWasteOffsetXDpState
    override fun devPortraitPileWasteOffsetYDp(): Float = devPortraitPileWasteOffsetYDpState
    override fun devPortraitPileTableauOffsetXDp(): Float = currentPortraitAspectPileOffsets().tableauOffsetX
    override fun devPortraitPileTableauOffsetYDp(): Float = currentPortraitAspectPileOffsets().tableauOffsetY
    override fun devLandscapeBannerSmallWidthDp(): Float = devLandscapeBannerSmallWidthDpState
    override fun devLandscapeBannerSmallHeightDp(): Float = devLandscapeBannerSmallHeightDpState
    override fun devLandscapeBannerMediumWidthDp(): Float = devLandscapeBannerMediumWidthDpState
    override fun devLandscapeBannerMediumHeightDp(): Float = devLandscapeBannerMediumHeightDpState
    override fun devLandscapeBannerLargeWidthDp(): Float = devLandscapeBannerLargeWidthDpState
    override fun devLandscapeBannerLargeHeightDp(): Float = devLandscapeBannerLargeHeightDpState
    override fun devSmallDeviceLandscapeBannerOffsetXDp(): Float = if (isLandscapeNow()) devSmallDeviceLandscapeBannerOffsetXDpState else devSmallDevicePortraitBannerOffsetXDpState
    override fun devSmallDeviceLandscapeBannerOffsetYDp(): Float = if (isLandscapeNow()) devSmallDeviceLandscapeBannerOffsetYDpState else devSmallDevicePortraitBannerOffsetYDpState
    override fun devMediumDeviceLandscapeBannerOffsetXDp(): Float = if (isLandscapeNow()) devMediumDeviceLandscapeBannerOffsetXDpState else devMediumDevicePortraitBannerOffsetXDpState
    override fun devMediumDeviceLandscapeBannerOffsetYDp(): Float = if (isLandscapeNow()) devMediumDeviceLandscapeBannerOffsetYDpState else devMediumDevicePortraitBannerOffsetYDpState
    override fun devLargeDeviceLandscapeBannerOffsetXDp(): Float = if (isLandscapeNow()) devLargeDeviceLandscapeBannerOffsetXDpState else devLargeDevicePortraitBannerOffsetXDpState
    override fun devLargeDeviceLandscapeBannerOffsetYDp(): Float = if (isLandscapeNow()) devLargeDeviceLandscapeBannerOffsetYDpState else devLargeDevicePortraitBannerOffsetYDpState
    override fun devScoreboardOffsetXDp(): Float = devScoreboardOffsetXDpState
    override fun devScoreboardOffsetYDp(): Float = devScoreboardOffsetYDpState
    override fun devGemRewardOffsetXDp(): Float = devGemRewardOffsetXDpState
    override fun devGemRewardOffsetYDp(): Float = devGemRewardOffsetYDpState
    override fun devTicketRewardOffsetXDp(): Float = devTicketRewardOffsetXDpState
    override fun devTicketRewardOffsetYDp(): Float = devTicketRewardOffsetYDpState
    override fun devUndoControlScale(): Float = devUndoControlScaleState
    override fun devUndoControlOffsetXDp(): Float = devUndoControlOffsetXDpState
    override fun devUndoControlOffsetYDp(): Float = devUndoControlOffsetYDpState
    override fun devRedoControlScale(): Float = devRedoControlScaleState
    override fun devRedoControlOffsetXDp(): Float = devRedoControlOffsetXDpState
    override fun devRedoControlOffsetYDp(): Float = devRedoControlOffsetYDpState
    override fun devHintControlScale(): Float = devHintControlScaleState
    override fun devHintControlOffsetXDp(): Float = devHintControlOffsetXDpState
    override fun devHintControlOffsetYDp(): Float = devHintControlOffsetYDpState
    override fun devMagicWandControlScale(): Float = devMagicWandControlScaleState
    override fun devMagicWandControlOffsetXDp(): Float = devMagicWandControlOffsetXDpState
    override fun devMagicWandControlOffsetYDp(): Float = devMagicWandControlOffsetYDpState
    override fun devPlayControlScale(): Float = devPlayControlScaleState
    override fun devPlayControlOffsetXDp(): Float = devPlayControlOffsetXDpState
    override fun devPlayControlOffsetYDp(): Float = devPlayControlOffsetYDpState
    override fun devAutoControlScale(): Float = devAutoControlScaleState
    override fun devAutoControlOffsetXDp(): Float = devAutoControlOffsetXDpState
    override fun devAutoControlOffsetYDp(): Float = devAutoControlOffsetYDpState
    override fun devGemRewardScale(): Float = devGemRewardScaleState
    override fun devGemRewardCounterOffsetXDp(): Float = devGemRewardCounterOffsetXDpState
    override fun devGemRewardCounterOffsetYDp(): Float = devGemRewardCounterOffsetYDpState
    override fun devGemRewardCounterScale(): Float = devGemRewardCounterScaleState
    override fun devTicketRewardScale(): Float = devTicketRewardScaleState
    override fun devTicketRewardCounterOffsetXDp(): Float = devTicketRewardCounterOffsetXDpState
    override fun devTicketRewardCounterOffsetYDp(): Float = devTicketRewardCounterOffsetYDpState
    override fun devTicketRewardCounterScale(): Float = devTicketRewardCounterScaleState
    //    override fun devLandscapeBannerAdBoxChoiceLabel(): String = devAdBoxChoiceState.name
    override fun devLandscapeBannerAdBoxChoiceLabel(): String = resolveCurrentBannerAdBoxChoice().name
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
    override fun devCurrentAspectCategoryLabel(): String {
        val category = binding.gameBoardView.getCurrentAspectCategory()
        val isCompactSlim = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> category == DeviceAspectCategory.SLIM && isCompactSlimLandscapeBoard()
            else -> category == DeviceAspectCategory.SLIM && isCompactSlimPortraitBoard()
        }
        return if (isCompactSlim) "${category.name}_COMPACT" else category.name
    }

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
        setCurrentLandscapeAspectPileOffsets(currentLandscapeAspectPileOffsets().copy(pileOverallOffsetX = value))
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileOverallOffsetY(value: Float) {
        setCurrentLandscapeAspectPileOffsets(currentLandscapeAspectPileOffsets().copy(pileOverallOffsetY = value))
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileFoundationOffsetX(value: Float) {
        setCurrentLandscapeAspectPileOffsets(currentLandscapeAspectPileOffsets().copy(foundationOffsetX = value))
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileFoundationOffsetY(value: Float) {
        setCurrentLandscapeAspectPileOffsets(currentLandscapeAspectPileOffsets().copy(foundationOffsetY = value))
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileDrawWasteOffsetX(value: Float) {
        setCurrentLandscapeAspectPileOffsets(currentLandscapeAspectPileOffsets().copy(drawWasteOffsetX = value))
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileDrawWasteOffsetY(value: Float) {
        setCurrentLandscapeAspectPileOffsets(currentLandscapeAspectPileOffsets().copy(drawWasteOffsetY = value))
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
        setCurrentLandscapeAspectPileOffsets(currentLandscapeAspectPileOffsets().copy(tableauOffsetX = value))
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetLandscapePileTableauOffsetY(value: Float) {
        setCurrentLandscapeAspectPileOffsets(currentLandscapeAspectPileOffsets().copy(tableauOffsetY = value))
        applyLandscapePileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileOverallOffsetX(value: Float) {
        setCurrentPortraitAspectPileOffsets(currentPortraitAspectPileOffsets().copy(pileOverallOffsetX = value))
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileOverallOffsetY(value: Float) {
        setCurrentPortraitAspectPileOffsets(currentPortraitAspectPileOffsets().copy(pileOverallOffsetY = value))
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
        setCurrentPortraitAspectPileOffsets(currentPortraitAspectPileOffsets().copy(tableauOffsetX = value))
        applyPortraitPileLayoutDevConfigToBoard()
    }
    override fun onDevSetPortraitPileTableauOffsetY(value: Float) {
        setCurrentPortraitAspectPileOffsets(currentPortraitAspectPileOffsets().copy(tableauOffsetY = value))
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
        if (isLandscapeNow()) devSmallDeviceLandscapeBannerOffsetXDpState = value
        else devSmallDevicePortraitBannerOffsetXDpState = value
        updateBannerPlacementForCurrentConfiguration()
        persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
    }
    override fun onDevSetSmallDeviceLandscapeBannerOffsetY(value: Float) {
        if (isLandscapeNow()) devSmallDeviceLandscapeBannerOffsetYDpState = value
        else devSmallDevicePortraitBannerOffsetYDpState = value
        updateBannerPlacementForCurrentConfiguration()
        persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
    }
    override fun onDevSetMediumDeviceLandscapeBannerOffsetX(value: Float) {
        if (isLandscapeNow()) devMediumDeviceLandscapeBannerOffsetXDpState = value
        else devMediumDevicePortraitBannerOffsetXDpState = value
        updateBannerPlacementForCurrentConfiguration()
        persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
    }
    override fun onDevSetMediumDeviceLandscapeBannerOffsetY(value: Float) {
        if (isLandscapeNow()) devMediumDeviceLandscapeBannerOffsetYDpState = value
        else devMediumDevicePortraitBannerOffsetYDpState = value
        updateBannerPlacementForCurrentConfiguration()
        persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
    }
    override fun onDevSetLargeDeviceLandscapeBannerOffsetX(value: Float) {
        if (isLandscapeNow()) devLargeDeviceLandscapeBannerOffsetXDpState = value
        else devLargeDevicePortraitBannerOffsetXDpState = value
        updateBannerPlacementForCurrentConfiguration()
        persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
    }
    override fun onDevSetLargeDeviceLandscapeBannerOffsetY(value: Float) {
        if (isLandscapeNow()) devLargeDeviceLandscapeBannerOffsetYDpState = value
        else devLargeDevicePortraitBannerOffsetYDpState = value
        updateBannerPlacementForCurrentConfiguration()
        persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
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
    override fun onDevSetUndoControlScale(value: Float) {
        devUndoControlScaleState = value.coerceAtLeast(0.1f)
        applyResponsiveControlSizing()
    }
    override fun onDevSetUndoControlOffsetX(value: Float) {
        devUndoControlOffsetXDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetUndoControlOffsetY(value: Float) {
        devUndoControlOffsetYDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetRedoControlScale(value: Float) {
        devRedoControlScaleState = value.coerceAtLeast(0.1f)
        applyResponsiveControlSizing()
    }
    override fun onDevSetRedoControlOffsetX(value: Float) {
        devRedoControlOffsetXDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetRedoControlOffsetY(value: Float) {
        devRedoControlOffsetYDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetHintControlScale(value: Float) {
        devHintControlScaleState = value.coerceAtLeast(0.1f)
        applyResponsiveControlSizing()
    }
    override fun onDevSetHintControlOffsetX(value: Float) {
        devHintControlOffsetXDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetHintControlOffsetY(value: Float) {
        devHintControlOffsetYDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetMagicWandControlScale(value: Float) {
        devMagicWandControlScaleState = value.coerceAtLeast(0.1f)
        applyResponsiveControlSizing()
    }
    override fun onDevSetMagicWandControlOffsetX(value: Float) {
        devMagicWandControlOffsetXDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetMagicWandControlOffsetY(value: Float) {
        devMagicWandControlOffsetYDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetPlayControlScale(value: Float) {
        devPlayControlScaleState = value.coerceAtLeast(0.1f)
        applyResponsiveControlSizing()
    }
    override fun onDevSetPlayControlOffsetX(value: Float) {
        devPlayControlOffsetXDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetPlayControlOffsetY(value: Float) {
        devPlayControlOffsetYDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetAutoControlScale(value: Float) {
        devAutoControlScaleState = value.coerceAtLeast(0.1f)
        applyResponsiveControlSizing()
    }
    override fun onDevSetAutoControlOffsetX(value: Float) {
        devAutoControlOffsetXDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetAutoControlOffsetY(value: Float) {
        devAutoControlOffsetYDpState = value
        applyResponsiveControlSizing()
    }
    override fun onDevSetGemRewardScale(value: Float) {
        devGemRewardScaleState = value.coerceAtLeast(0.1f)
        applyTopHudDevOffsets()
    }
    override fun onDevSetGemRewardCounterOffsetX(value: Float) {
        devGemRewardCounterOffsetXDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetGemRewardCounterOffsetY(value: Float) {
        devGemRewardCounterOffsetYDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetGemRewardCounterScale(value: Float) {
        devGemRewardCounterScaleState = value.coerceAtLeast(0.1f)
        applyTopHudDevOffsets()
    }
    override fun onDevSetTicketRewardScale(value: Float) {
        devTicketRewardScaleState = value.coerceAtLeast(0.1f)
        applyTopHudDevOffsets()
    }
    override fun onDevSetTicketRewardCounterOffsetX(value: Float) {
        devTicketRewardCounterOffsetXDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetTicketRewardCounterOffsetY(value: Float) {
        devTicketRewardCounterOffsetYDpState = value
        applyTopHudDevOffsets()
    }
    override fun onDevSetTicketRewardCounterScale(value: Float) {
        devTicketRewardCounterScaleState = value.coerceAtLeast(0.1f)
        applyTopHudDevOffsets()
    }
    override fun onDevCycleBannerBoxChoice() {
        val currentAdBoxChoice = resolveCurrentBannerAdBoxChoice()
        val nextAdBoxChoice = when (currentAdBoxChoice) {
            BannerAdBoxChoice.SMALL -> BannerAdBoxChoice.MEDIUM
            BannerAdBoxChoice.MEDIUM -> BannerAdBoxChoice.LARGE
            BannerAdBoxChoice.LARGE -> BannerAdBoxChoice.SMALL
        }

        // Persist the newly selected tier first so reload uses the new setting immediately.
        setCurrentBannerAdBoxChoice(nextAdBoxChoice)
        reloadBannerForCurrentConfiguration()
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
        val btnMinus = dialogView.findViewById<Button>(R.id.btn_recycles_minus)
        val btnPlus  = dialogView.findViewById<Button>(R.id.btn_recycles_plus)
        val countText = dialogView.findViewById<TextView>(R.id.text_recycles_count)
        val unlimitedSwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_recycles_unlimited)
        val countRow = dialogView.findViewById<View>(R.id.recycles_count_row)

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
        // Rotation is handled via configChanges, so wait for fresh layout bounds before choosing
        // compact-vs-regular slim configs and banner/control variants.
        binding.gameBoardView.post {
            reapplyActiveLayoutScopedDevAdjustersForCurrentConfiguration()
            applyResponsiveControlSizing()
            applyMirroredLayoutUi(viewModel.isMirroredLayout.value)
            applyAutoWinPopupRatios()
            if (testerStarburstAutoLayoutEnabled) {
                applyAutoStarburstProfile()
                refreshActiveStarburstDebugAndMotion()
            }
            applyLockedPileAdIconDevConfigToBoard()
            applyLandscapePileLayoutDevConfigToBoard(persistProfile = false)
            applyPortraitPileLayoutDevConfigToBoard(persistProfile = false)
            applyTopHudDevOffsets(persistProfile = false)
            reloadBannerForCurrentConfiguration()
        }
    }

    private fun reapplyActiveLayoutScopedDevAdjustersForCurrentConfiguration() {
        val key = resolveActiveLayoutProfileKey()
        applyLayoutScopedDevAdjusters(profileStateFor(key))
        appliedLayoutProfileKey = key
        hasAppliedLayoutScopedDevProfile = true
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

    private fun applyLandscapePileLayoutDevConfigToBoard(persistProfile: Boolean = true) {
        if (persistProfile) {
            persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
        }
        val ratioProfile = calculateDevOffsetRatioProfile()
        val aspectOffsets = currentLandscapeAspectPileOffsets()
        binding.gameBoardView.setLandscapePileLayoutTuning(
            overallOffsetX = scaleDevDpOffsetX(aspectOffsets.pileOverallOffsetX, ratioProfile),
            overallOffsetY = scaleDevDpOffsetY(aspectOffsets.pileOverallOffsetY, ratioProfile),
            foundationOffsetX = scaleDevDpOffsetX(aspectOffsets.foundationOffsetX, ratioProfile),
            foundationOffsetY = scaleDevDpOffsetY(aspectOffsets.foundationOffsetY, ratioProfile),
            drawWasteOffsetX = scaleDevDpOffsetX(aspectOffsets.drawWasteOffsetX, ratioProfile),
            drawWasteOffsetY = scaleDevDpOffsetY(aspectOffsets.drawWasteOffsetY, ratioProfile),
            stockOffsetX = scaleDevDpOffsetX(devLandscapePileStockOffsetXDpState, ratioProfile),
            stockOffsetY = scaleDevDpOffsetY(devLandscapePileStockOffsetYDpState, ratioProfile),
            wasteOffsetX = scaleDevDpOffsetX(devLandscapePileWasteOffsetXDpState, ratioProfile),
            wasteOffsetY = scaleDevDpOffsetY(devLandscapePileWasteOffsetYDpState, ratioProfile),
            tableauOffsetX = scaleDevDpOffsetX(aspectOffsets.tableauOffsetX, ratioProfile),
            tableauOffsetY = scaleDevDpOffsetY(aspectOffsets.tableauOffsetY, ratioProfile)
        )
        binding.gameBoardView.dumpPileLayoutDebug("applyLandscapePileLayoutDevConfigToBoard")
    }

    private fun applyPortraitPileLayoutDevConfigToBoard(persistProfile: Boolean = true) {
        if (persistProfile) {
            persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
        }
        val ratioProfile = calculateDevOffsetRatioProfile()
        val aspectOffsets = currentPortraitAspectPileOffsets()
        binding.gameBoardView.setPortraitPileLayoutTuning(
            overallOffsetX = scaleDevDpOffsetX(aspectOffsets.pileOverallOffsetX, ratioProfile),
            overallOffsetY = scaleDevDpOffsetY(aspectOffsets.pileOverallOffsetY, ratioProfile),
            foundationOffsetX = scaleDevDpOffsetX(aspectOffsets.foundationOffsetX, ratioProfile),
            foundationOffsetY = scaleDevDpOffsetY(aspectOffsets.foundationOffsetY, ratioProfile),
            drawWasteOffsetX = scaleDevDpOffsetX(aspectOffsets.drawWasteOffsetX, ratioProfile),
            drawWasteOffsetY = scaleDevDpOffsetY(aspectOffsets.drawWasteOffsetY, ratioProfile),
            stockOffsetX = scaleDevDpOffsetX(devPortraitPileStockOffsetXDpState, ratioProfile),
            stockOffsetY = scaleDevDpOffsetY(devPortraitPileStockOffsetYDpState, ratioProfile),
            wasteOffsetX = scaleDevDpOffsetX(devPortraitPileWasteOffsetXDpState, ratioProfile),
            wasteOffsetY = scaleDevDpOffsetY(devPortraitPileWasteOffsetYDpState, ratioProfile),
            tableauOffsetX = scaleDevDpOffsetX(aspectOffsets.tableauOffsetX, ratioProfile),
            tableauOffsetY = scaleDevDpOffsetY(aspectOffsets.tableauOffsetY, ratioProfile)
        )
        binding.gameBoardView.dumpPileLayoutDebug("applyPortraitPileLayoutDevConfigToBoard")
    }

    private fun applyTopHudDevOffsets(persistProfile: Boolean = true) {
        if (persistProfile) {
            persistActiveLayoutScopedDevAdjusters(resolveActiveLayoutProfileKey())
        }
        val ratioProfile = calculateDevOffsetRatioProfile()
        val scoreboardX = scaleDevDpOffsetX(devScoreboardOffsetXDpState, ratioProfile)
        val scoreboardY = scaleDevDpOffsetY(devScoreboardOffsetYDpState, ratioProfile)
        val (clampedScoreboardX, clampedScoreboardY) = clampHudTranslationToRoot(
            binding.boardScoreboardContainer,
            scoreboardX,
            scoreboardY
        )
        binding.boardScoreboardContainer.translationX = clampedScoreboardX
        binding.boardScoreboardContainer.translationY = clampedScoreboardY

        val gemsX = scaleDevDpOffsetX(devGemRewardOffsetXDpState, ratioProfile)
        val gemsY = scaleDevDpOffsetY(devGemRewardOffsetYDpState, ratioProfile)
        val (clampedGemsX, clampedGemsY) = clampHudTranslationToRoot(binding.gemsContainer, gemsX, gemsY)
        binding.gemsContainer.translationX = clampedGemsX
        binding.gemsContainer.translationY = clampedGemsY
        binding.ivGemBag.scaleX = devGemRewardScaleState
        binding.ivGemBag.scaleY = devGemRewardScaleState
        binding.tvGemCount.translationX = scaleDevDpOffsetX(devGemRewardCounterOffsetXDpState, ratioProfile)
        binding.tvGemCount.translationY = scaleDevDpOffsetY(devGemRewardCounterOffsetYDpState, ratioProfile)
        binding.tvGemCount.scaleX = devGemRewardCounterScaleState
        binding.tvGemCount.scaleY = devGemRewardCounterScaleState

        val ticketsX = scaleDevDpOffsetX(devTicketRewardOffsetXDpState, ratioProfile)
        val ticketsY = scaleDevDpOffsetY(devTicketRewardOffsetYDpState, ratioProfile)
        val (clampedTicketsX, clampedTicketsY) = clampHudTranslationToRoot(binding.ticketsContainer, ticketsX, ticketsY)
        binding.ticketsContainer.translationX = clampedTicketsX
        binding.ticketsContainer.translationY = clampedTicketsY
        binding.ivTicketIcon.scaleX = devTicketRewardScaleState
        binding.ivTicketIcon.scaleY = devTicketRewardScaleState
        binding.tvTicketCount.translationX = scaleDevDpOffsetX(devTicketRewardCounterOffsetXDpState, ratioProfile)
        binding.tvTicketCount.translationY = scaleDevDpOffsetY(devTicketRewardCounterOffsetYDpState, ratioProfile)
        binding.tvTicketCount.scaleX = devTicketRewardCounterScaleState
        binding.tvTicketCount.scaleY = devTicketRewardCounterScaleState
    }

    private fun configureHudClipBehavior() {
        binding.root.clipChildren = false
        binding.root.clipToPadding = false
        binding.infoSidePanel.clipChildren = false
        binding.infoSidePanel.clipToPadding = false
        binding.gemsContainer.clipChildren = false
        binding.gemsContainer.clipToPadding = false
        binding.ticketsContainer.clipChildren = false
        binding.ticketsContainer.clipToPadding = false
        (binding.infoSidePanel.parent as? ViewGroup)?.let { parent ->
            parent.clipChildren = false
            parent.clipToPadding = false
        }
    }

    private fun clampHudTranslationToRoot(target: View, desiredX: Float, desiredY: Float): Pair<Float, Float> {
        if (binding.root.width <= 0 || binding.root.height <= 0 || target.width <= 0 || target.height <= 0) {
            return desiredX to desiredY
        }

        val rootLocation = IntArray(2)
        val targetLocation = IntArray(2)
        binding.root.getLocationInWindow(rootLocation)
        target.getLocationInWindow(targetLocation)

        // getLocationInWindow includes any currently-applied translation, so subtract it to get
        // the view's natural (pre-translation) position. This ensures clamping bounds are stable
        // regardless of what translations are already applied, preventing X from resetting when Y
        // is changed (and vice-versa).
        val currentLeft = (targetLocation[0] - rootLocation[0]).toFloat()
        val currentTop = (targetLocation[1] - rootLocation[1]).toFloat()
        val naturalLeft = currentLeft - target.translationX
        val naturalTop = currentTop - target.translationY
        val minTranslationX = -naturalLeft
        val maxTranslationX = binding.root.width.toFloat() - (naturalLeft + target.width)
        val minTranslationY = -naturalTop
        val maxTranslationY = binding.root.height.toFloat() - (naturalTop + target.height)

        return desiredX.coerceIn(minTranslationX, maxTranslationX) to
            desiredY.coerceIn(minTranslationY, maxTranslationY)
    }

    private fun resolveLandscapeBannerTier(): BannerAdTier {
        val swDp = resources.configuration.smallestScreenWidthDp
        return when {
            swDp == SMALLEST_SCREEN_WIDTH_DP_UNDEFINED -> BannerAdTier.SMALL
            swDp < 400 -> BannerAdTier.SMALL
            swDp >= 800 -> BannerAdTier.LARGE
            else -> BannerAdTier.MEDIUM
        }
    }

    private fun resolveCurrentBannerAdBoxChoice(): BannerAdBoxChoice {
        return if (isLandscapeNow()) {
            when (binding.gameBoardView.getCurrentAspectCategory()) {
                DeviceAspectCategory.SLIM -> if (isCompactSlimLandscapeBoard()) devLandscapeAdBoxChoiceSlimCompactState else devLandscapeAdBoxChoiceSlimState
                DeviceAspectCategory.CLASSIC -> devLandscapeAdBoxChoiceClassicState
                DeviceAspectCategory.BROAD -> devLandscapeAdBoxChoiceBroadState
                DeviceAspectCategory.SQUARE -> devLandscapeAdBoxChoiceSquareState
            }
        } else {
            when (binding.gameBoardView.getCurrentAspectCategory()) {
                DeviceAspectCategory.SLIM -> if (isCompactSlimPortraitBoard()) devPortraitAdBoxChoiceSlimCompactState else devPortraitAdBoxChoiceSlimState
                DeviceAspectCategory.CLASSIC -> devPortraitAdBoxChoiceClassicState
                DeviceAspectCategory.BROAD -> devPortraitAdBoxChoiceBroadState
                DeviceAspectCategory.SQUARE -> devPortraitAdBoxChoiceSquareState
            }
        }
    }

    private fun setCurrentBannerAdBoxChoice(choice: BannerAdBoxChoice) {
        if (isLandscapeNow()) {
            when (binding.gameBoardView.getCurrentAspectCategory()) {
                DeviceAspectCategory.SLIM -> {
                    if (isCompactSlimLandscapeBoard()) devLandscapeAdBoxChoiceSlimCompactState = choice
                    else devLandscapeAdBoxChoiceSlimState = choice
                }
                DeviceAspectCategory.CLASSIC -> devLandscapeAdBoxChoiceClassicState = choice
                DeviceAspectCategory.BROAD -> devLandscapeAdBoxChoiceBroadState = choice
                DeviceAspectCategory.SQUARE -> devLandscapeAdBoxChoiceSquareState = choice
            }
        } else {
            when (binding.gameBoardView.getCurrentAspectCategory()) {
                DeviceAspectCategory.SLIM -> {
                    if (isCompactSlimPortraitBoard()) devPortraitAdBoxChoiceSlimCompactState = choice
                    else devPortraitAdBoxChoiceSlimState = choice
                }
                DeviceAspectCategory.CLASSIC -> devPortraitAdBoxChoiceClassicState = choice
                DeviceAspectCategory.BROAD -> devPortraitAdBoxChoiceBroadState = choice
                DeviceAspectCategory.SQUARE -> devPortraitAdBoxChoiceSquareState = choice
            }
        }
    }

    private fun resolveBannerTier(choice: BannerAdBoxChoice): BannerAdTier {
        return when (choice) {
            BannerAdBoxChoice.SMALL -> BannerAdTier.SMALL
            BannerAdBoxChoice.MEDIUM -> BannerAdTier.MEDIUM
            BannerAdBoxChoice.LARGE -> BannerAdTier.LARGE
        }
    }

    private fun resolveBannerSizesForTier(tier: BannerAdTier): List<AdSize> {
        return when (tier) {
            BannerAdTier.SMALL -> listOf(AdSize.BANNER)
            BannerAdTier.MEDIUM -> listOf(AdSize.LARGE_BANNER)
            BannerAdTier.LARGE -> listOf(AdSize.MEDIUM_RECTANGLE)
        }
    }

    private fun resolveCurrentBannerOffsetDp(): Pair<Float, Float> {
        val isLandscape = isLandscapeNow()
        return when (resolveBannerTier(resolveCurrentBannerAdBoxChoice())) {
            BannerAdTier.SMALL -> {
                if (isLandscape) devSmallDeviceLandscapeBannerOffsetXDpState to devSmallDeviceLandscapeBannerOffsetYDpState
                else devSmallDevicePortraitBannerOffsetXDpState to devSmallDevicePortraitBannerOffsetYDpState
            }
            BannerAdTier.MEDIUM -> {
                if (isLandscape) devMediumDeviceLandscapeBannerOffsetXDpState to devMediumDeviceLandscapeBannerOffsetYDpState
                else devMediumDevicePortraitBannerOffsetXDpState to devMediumDevicePortraitBannerOffsetYDpState
            }
            BannerAdTier.LARGE -> {
                if (isLandscape) devLargeDeviceLandscapeBannerOffsetXDpState to devLargeDeviceLandscapeBannerOffsetYDpState
                else devLargeDevicePortraitBannerOffsetXDpState to devLargeDevicePortraitBannerOffsetYDpState
            }
        }
    }

    private fun resolveBannerContainer(): FrameLayout? {
        return findViewById<FrameLayout?>(R.id.banner_overlay_container)
            ?: findViewById(R.id.portrait_banner_container)
    }

    private fun updateBannerPlacementForCurrentConfiguration() {
        val container = resolveBannerContainer() ?: return
        val isLandscape = isLandscapeNow()
        val mirrored = viewModel.isMirroredLayout.value

        // Keep box side consistent with hand-side in landscape; portrait keeps full-width box.
        (container.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
            if (isLandscape) {
                if (mirrored) {
                    lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    lp.endToEnd = ConstraintLayout.LayoutParams.UNSET
                } else {
                    lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    lp.startToStart = ConstraintLayout.LayoutParams.UNSET
                }
            } else {
                if (mirrored) {
                    lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    lp.endToEnd = ConstraintLayout.LayoutParams.UNSET
                } else {
                    lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    lp.startToStart = ConstraintLayout.LayoutParams.UNSET
                }
            }
            container.layoutParams = lp
        }

        val ratioProfile = calculateDevOffsetRatioProfile()
        val (offsetXDp, offsetYDp) = resolveCurrentBannerOffsetDp()
        container.translationX = scaleDevDpOffsetX(offsetXDp, ratioProfile)
        container.translationY = scaleDevDpOffsetY(offsetYDp, ratioProfile)

        val portraitGravity = if (mirrored) {
            Gravity.START or Gravity.CENTER_VERTICAL
        } else {
            Gravity.END or Gravity.CENTER_VERTICAL
        }
        val landscapeGravity = Gravity.CENTER
        val targetGravity = if (isLandscape) landscapeGravity else portraitGravity

        listOfNotNull(
            findViewById<AdView?>(R.id.adView),
            findViewById(R.id.adViewBannerMedium),
            findViewById(R.id.adViewBannerSmall)
        ).forEach { adView ->
            (adView.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.gravity = targetGravity
                adView.layoutParams = lp
            }
        }
    }

    private fun resolveCurrentBannerBoxDimensions(tier: BannerAdTier): Pair<Float, Float> {
        return if (isLandscapeNow()) {
            when (tier) {
                BannerAdTier.SMALL -> devLandscapeBannerSmallWidthDpState to devLandscapeBannerSmallHeightDpState
                BannerAdTier.MEDIUM -> devLandscapeBannerMediumWidthDpState to devLandscapeBannerMediumHeightDpState
                BannerAdTier.LARGE -> devLandscapeBannerLargeWidthDpState to devLandscapeBannerLargeHeightDpState
            }
        } else {
            when (tier) {
                BannerAdTier.SMALL -> devPortraitBannerSmallWidthDpState to devPortraitBannerSmallHeightDpState
                BannerAdTier.MEDIUM -> devPortraitBannerMediumWidthDpState to devPortraitBannerMediumHeightDpState
                BannerAdTier.LARGE -> devPortraitBannerLargeWidthDpState to devPortraitBannerLargeHeightDpState
            }
        }
    }

    private fun configureCurrentBannerBoxAndResolveAdSizes(): List<AdSize> {
        val container = resolveBannerContainer()
        if (container == null) {
            val tierWithoutContainer = resolveBannerTier(resolveCurrentBannerAdBoxChoice())
            Log.d("GameActivityAds", "Banner container missing; tier=$tierWithoutContainer")
            return resolveBannerSizesForTier(tierWithoutContainer)
        }

        val choice = resolveCurrentBannerAdBoxChoice()
        val resolvedTier = resolveBannerTier(choice)
        val (boxWidthDp, boxHeightDp) = resolveCurrentBannerBoxDimensions(resolvedTier)

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
            "Banner tier=$resolvedTier choice=$choice isLandscape=${isLandscapeNow()} containerId=${container.id} boxDp=${boxWidthDp}x${boxHeightDp} boxPx=${targetWidthPx}x${targetHeightPx}"
        )

        return resolveBannerSizesForTier(resolvedTier)
    }

    private fun resolveActiveBannerAdView(requestedAdSizes: List<AdSize>): AdView {

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
        val requestedAdSizes = configureCurrentBannerBoxAndResolveAdSizes()
        val bannerAdView = resolveActiveBannerAdView(requestedAdSizes)
        updateBannerPlacementForCurrentConfiguration()
        val container = resolveBannerContainer()
        val loadBanner = {
            // Guard: if another reloadBannerForCurrentConfiguration() ran while this post
            // was pending, bannerAdView may have been set to GONE by the newer call.
            if (bannerAdView.visibility != View.GONE) {
                adManager.loadBannerAd(bannerAdView, requestedAdSizes)
            }
        }
        // Always defer actual ad load to post-layout so rotation has settled bounds/container size.
        if (container != null) {
            container.post { loadBanner() }
        } else {
            binding.root.post { loadBanner() }
        }
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

        lifecycleScope.launch {
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
        }
    }

    private fun onHelpControlClicked(control: HelpControlAction, targetView: View?, action: () -> Unit) {
        if (control != HelpControlAction.HINT) {
            action()
            return
        }
        if (helpControlFlowInProgress) return

        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val unlockExpiry = settingsManager.getHelpControlUnlockExpiry(control.storageKey)
            if (unlockExpiry > now) {
                action()
                return@launch
            }

            if (ticketTotal > 0) {
                animateAndConsumeHelpCoupon(targetView, control)
                action()
                return@launch
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
            ?: RectF(
                boardView.width * 0.5f - sourceRect.width() * 0.5f,
                boardView.height * 0.5f - sourceRect.height() * 0.5f,
                boardView.width * 0.5f + sourceRect.width() * 0.5f,
                boardView.height * 0.5f + sourceRect.height() * 0.5f
            )

        boardView.scheduleCouponAnimation(sourceRect, targetRect)

        // Keep deduction synced with full coupon animation runtime, including midpoint pause.
        delay(CouponFlightAnimator.TOTAL_RUNTIME_MS + 40L)
        consumeHelpCoupon(control)
    }

    private fun viewRectInBoardSpace(
        view: View,
        boardLocationOnScreen: IntArray
    ): RectF {
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val left = (viewLocation[0] - boardLocationOnScreen[0]).toFloat()
        val top = (viewLocation[1] - boardLocationOnScreen[1]).toFloat()
        return RectF(
            left,
            top,
            left + view.width,
            top + view.height
        )
    }

    private fun clampRectToBoardBounds(
        rect: RectF,
        boardView: View,
        insetPx: Float = 4f
    ): RectF {
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

        return RectF(
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
        val btnHint = findViewById<View?>(R.id.btn_hint)
        val btnRestart = findViewById<Button?>(R.id.btn_restart)
        val btnAuto = findViewById<View?>(R.id.btn_auto_move)
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
            setButtonSizeDp(btnHint, 32f * portraitWidthBoost * portraitButtonWidthScale * ratioProfile.averageRatio, 72f * controlHeightScale)
            setButtonSizeDp(btnRestart, 67f * portraitWidthBoost * portraitButtonWidthScale * ratioProfile.averageRatio, 50f * controlHeightScale)
            setButtonSizeDp(btnAuto, 96f * portraitWidthBoost * portraitButtonWidthScale * ratioProfile.averageRatio, 48f * controlHeightScale)
        } else {
            setButtonSizeDp(btnHint, 56f * widthScale, 56f * controlHeightScale)
            setButtonSizeDp(btnAuto, 108f * widthScale, 44f * controlHeightScale)
        }

        applyButtonScale(binding.btnNewGame, controlTextSp, textScale * ratioProfile.averageRatio)
        binding.btnStats.minWidth = 0
        binding.btnStats.minimumWidth = 0
        binding.btnStats.minimumHeight = 0
        binding.btnStats.setPaddingRelative(0, 0, 0, 0)
        btnRestart?.let { applyButtonScale(it, controlTextSp, textScale * ratioProfile.averageRatio) }

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

        // Keep mirrored controls inset toward the center; using the same positive X offsets
        // in both hand modes pushes mirrored controls further off the right edge.
        val mirroredDirectionX = if (viewModel.isMirroredLayout.value) -1f else 1f
        applyControlAdjustments(binding.btnUndo, devUndoControlScaleState, devUndoControlOffsetXDpState * mirroredDirectionX, devUndoControlOffsetYDpState, ratioProfile)
        applyControlAdjustments(binding.btnRedo, devRedoControlScaleState, devRedoControlOffsetXDpState * mirroredDirectionX, devRedoControlOffsetYDpState, ratioProfile)
        applyControlAdjustments(btnHint, devHintControlScaleState, devHintControlOffsetXDpState * mirroredDirectionX, devHintControlOffsetYDpState, ratioProfile)
        applyControlAdjustments(findViewById(R.id.magic_wand_container), devMagicWandControlScaleState, devMagicWandControlOffsetXDpState * mirroredDirectionX, devMagicWandControlOffsetYDpState, ratioProfile)
        applyControlAdjustments(binding.btnStats, devPlayControlScaleState, devPlayControlOffsetXDpState * mirroredDirectionX, devPlayControlOffsetYDpState, ratioProfile)
        applyControlAdjustments(btnAuto, devAutoControlScaleState, devAutoControlOffsetXDpState * mirroredDirectionX, devAutoControlOffsetYDpState, ratioProfile)
    }

    private fun applyControlAdjustments(
        view: View?,
        scale: Float,
        offsetXDp: Float,
        offsetYDp: Float,
        ratioProfile: BaselineResolutionScaleUtil.ResolutionRatioProfile
    ) {
        view ?: return
        view.scaleX = scale
        view.scaleY = scale
        view.translationX = scaleDevDpOffsetX(offsetXDp, ratioProfile)
        view.translationY = scaleDevDpOffsetY(offsetYDp, ratioProfile)
    }

    private fun applyButtonScale(button: Button, textSp: Float, scale: Float) {
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp)
        button.minWidth = dpToPx(76f * scale)
        val horizontal = dpToPx(12f * scale)
        val vertical = dpToPx(6f * scale)
        button.setPaddingRelative(horizontal, vertical, horizontal, vertical)
    }

    private fun setButtonSizeDp(view: View?, widthDp: Float, heightDp: Float) {
        view ?: return
        val lp = view.layoutParams ?: return
        lp.width = dpToPx(widthDp)
        lp.height = dpToPx(heightDp)
        view.layoutParams = lp
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
