package com.gpgamelab.justpatience.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gpgamelab.justpatience.assets.AssetResolver
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.model.Card
import com.gpgamelab.justpatience.model.CardSuit
import com.gpgamelab.justpatience.model.Game
import com.gpgamelab.justpatience.model.HintDisplayState
import com.gpgamelab.justpatience.model.HintPhase
import com.gpgamelab.justpatience.model.SingleClickGlowState
import com.gpgamelab.justpatience.model.StackType
import com.gpgamelab.justpatience.model.TableauPile
import com.gpgamelab.justpatience.model.Verso
import com.gpgamelab.justpatience.util.UiScaleUtil
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import android.os.SystemClock

private const val DEFAULT_STOCK_BACK_IMAGE_PATH = "drawable:b_0001"
private const val NEW_GAME_DEAL_CARD_INTERVAL_MS = 70L
private const val MAX_VISIBLE_WASTE_CARDS = 4
private const val WASTE_COUNT_BADGE_MIN_VISIBLE_COUNT = 4

class GameBoardView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    lateinit var viewModel: GameViewModel
    lateinit var assetResolver: AssetResolver
    var onClickMoveSoundRequested: (() -> Unit)? = null
    var onShuffleSoundRequested: (() -> Unit)? = null
    var onLockedTableauUnlockRequested: (() -> Unit)? = null
    var onMagicWandTargetSelected: ((StackType, Int, Int) -> Unit)? = null

    private val currentSetId = "default"

    private var lastTapTime = 0L
    private val doubleTapTimeout = 300L
    private var pendingSingleTap: Runnable? = null
    private var lastTapStackType: StackType? = null
    private var lastTapStackIndex: Int = -1
    private var lastTapCardIndex: Int = -1

    // Orientation-aware dimensions and offsets
    private var isLandscape = false
    private var isMirrored = false
    private var BOARD_WIDTH_FRACTION = 0.70f
    private var BOARD_SHIFT_LEFT_PX = 150f
    private var BOARD_SHIFT_DOWN_PX = 120f
    private val LANDSCAPE_DIRECTIONAL_SHIFT_PX = 20f

    // Portrait-specific offsets
    private val PORTRAIT_BOARD_WIDTH_FRACTION = 0.75f
    private val PORTRAIT_BOARD_SHIFT_LEFT_PX = 0f
    private val PORTRAIT_BOARD_SHIFT_DOWN_PX = 120f

    // Landscape-specific offsets (estimates - can be adjusted later)
    private val LANDSCAPE_BOARD_WIDTH_FRACTION = 0.90f  // Use more of the width
    private val LANDSCAPE_BOARD_SHIFT_LEFT_PX = 40f * context.resources.displayMetrics.density  // density-aware dp offset
    private val LANDSCAPE_BOARD_SHIFT_DOWN_PX = 20f     // Less top shift (landscape is shorter)

    // Card size multipliers for different orientations
    private var CARD_SIZE_MULTIPLIER = 1.0f  // Will be updated based on orientation
    private val PORTRAIT_CARD_SIZE_MULTIPLIER = 1.0f   // Full size in portrait
    private val LANDSCAPE_CARD_SIZE_MULTIPLIER = 0.72f  // Larger cards in landscape while still fitting

    private val cardPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.card_placeholder); style =
        Paint.Style.STROKE; strokeWidth = 4f
    }
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 48f; textAlign = Paint.Align.CENTER }
    private val pileLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 30f
        style = Paint.Style.FILL
        setShadowLayer(6f, 0f, 2f, Color.BLACK)
    }
    private val pileCountBadgeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D32F2F")
        style = Paint.Style.FILL
    }
    private val pileCountBadgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val pileCountBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 24f
        style = Paint.Style.FILL
        isFakeBoldText = true
    }
    private var pileCountBadgeRadius = 12f
    private var pileCountBadgeInset = 6f

    // Board and Card dimensions
    private val cardWidthRatio = 1.5f
    private val cardHeightRatio = 2.0f
    private var cardRadius = 20f
    private var cardPadding = 1f
    private var tableauOffset = 40f

    private val baseCardRadius = 20f
    private val baseCardPadding = 1f
    private val baseTableauOffset = 40f
    private val LANDSCAPE_TOP_PILE_SHIFT_RATIO = 0.22f
    private val LANDSCAPE_WASTE_EXTRA_SHIFT_RATIO = 0.22f
    private val PORTRAIT_TOP_PILE_SHIFT_RATIO = 0.10f
    private val LANDSCAPE_OUTER_GAP_FACTOR = 1.0f
    private val LANDSCAPE_FOUNDATION_BASE_Y_SHIFT_RATIO = 0.78f
    private val LANDSCAPE_FOUNDATION_STEP_RATIO_MULTI_COLUMN = 1.02f

    private var boardStartY = 0f

    private var cardW = 0f
    private var cardH = 0f
    private var columns = Game.TOTAL_TABLEAU_PILES_1_DECK
    private val columnX = mutableListOf<Float>()

    // drag state
    private var dragStackType: StackType? = null
    private var dragStackIndex: Int = -1
    private var dragCardIndex: Int = -1
    private var dragX = 600f
    private var dragY = 600f
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var dropTargetTableauIndex: Int? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var isDragging = false
    private var tabletopBitmap: Bitmap? = null
    private val recycleIndicatorBitmap: Bitmap? by lazy(LazyThreadSafetyMode.NONE) {
        BitmapFactory.decodeResource(resources, R.drawable.ic_recycle_green_512x512)
    }
    private val lockedPileAdDrawable by lazy(LazyThreadSafetyMode.NONE) {
        ContextCompat.getDrawable(context, R.drawable.ic_play_ad_lt_blue)
    }
    private val lockedPileLockDrawable by lazy(LazyThreadSafetyMode.NONE) {
        ContextCompat.getDrawable(context, R.drawable.ic_lock_02_a)
    }
    private val lockedPileOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A000000")
        style = Paint.Style.FILL
    }
    private var lockedPileAdOffsetXPxPortrait = 0f
    private var lockedPileAdOffsetYPxPortrait = 0f
    private var lockedPileAdScaleXPortrait = 1f
    private var lockedPileAdScaleYPortrait = 1f
    private var lockedPileAdOffsetXPxLandscape = 0f
    private var lockedPileAdOffsetYPxLandscape = 0f
    private var lockedPileAdScaleXLandscape = 1f
    private var lockedPileAdScaleYLandscape = 1f

    // animation state for auto moves
    private var animationCard: Card? = null
    private var animationStartRect: RectF? = null
    private var animationEndRect: RectF? = null
    private var animationDestStackType: StackType? = null
    private var animationDestStackIndex: Int = -1
    private var animationMovedCardCount: Int = 1
    private var animationStartTimeMs: Long = 0
    private var animationActiveThisFrame = false
    private val ANIMATION_DURATION_MS = 250L
    private var isAnimating = false

    // New-game deal reveal animation state.
    private var newGameDealActive = false
    private var newGameDealRevealCount = 0
    private var newGameDealOrderByCard = hashMapOf<Int, Int>()
    private var newGameDealStepRunnable: Runnable? = null
    private var newGameDealStartedAtMs: Long = 0L
    private var newGameDealMaxDurationMs: Long = 0L

    // hint glow state (driven by GameViewModel.hintDisplayState)
    private var hintDisplayState: HintDisplayState? = null

    // single-click glow state (shows all valid destinations simultaneously)
    private var singleClickGlowState: SingleClickGlowState? = null
    private var magicWandSelectionMode = false

    // coupon flight animation state
    private val couponFlightAnimator = CouponFlightAnimator()
    private var couponDrawable: android.graphics.drawable.Drawable? = null

    private val hintGlowSourcePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hint_glow_source_color)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val hintGlowDestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hint_glow_destination_color)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    init {
        // Detect current orientation
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        updateOffsetsForOrientation()
        loadTabletopImage()
        // Initialize coupon drawable
        couponDrawable = ContextCompat.getDrawable(context, R.drawable.ic_ticket_green_yellow_helper)
    }

    private fun updateOffsetsForOrientation() {
        if (isLandscape) {
            BOARD_WIDTH_FRACTION = LANDSCAPE_BOARD_WIDTH_FRACTION
            BOARD_SHIFT_LEFT_PX = LANDSCAPE_BOARD_SHIFT_LEFT_PX
            BOARD_SHIFT_DOWN_PX = LANDSCAPE_BOARD_SHIFT_DOWN_PX
            CARD_SIZE_MULTIPLIER = LANDSCAPE_CARD_SIZE_MULTIPLIER
        } else {
            BOARD_WIDTH_FRACTION = PORTRAIT_BOARD_WIDTH_FRACTION
            BOARD_SHIFT_LEFT_PX = PORTRAIT_BOARD_SHIFT_LEFT_PX
            BOARD_SHIFT_DOWN_PX = PORTRAIT_BOARD_SHIFT_DOWN_PX
            CARD_SIZE_MULTIPLIER = PORTRAIT_CARD_SIZE_MULTIPLIER
        }
    }

    private fun loadTabletopImage() {
        tabletopBitmap?.recycle()
        tabletopBitmap = try {
            val resourceId = if (isLandscape) {
                resources.getIdentifier("tabletop_green_felt_01_320x500_l", "drawable", context.packageName)
            } else {
                resources.getIdentifier("tabletop_green_felt_01_320x500_p", "drawable", context.packageName)
            }

            if (resourceId != 0) {
                BitmapFactory.decodeResource(resources, resourceId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("GameBoardView", "Failed to load tabletop image: ${e.message}")
            null
        }
    }

    fun bindToViewModel(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.game.collect {
                        // Game state changed -> redraw
                        syncColumnsWithGame(it)
                        clearDragState()
                        invalidate()
                    }
                }

                // Redraw labels when draw/recycle settings change.
                launch {
                    viewModel.drawCountForDisplay.collect { invalidate() }
                }
                launch {
                    viewModel.recycleLimitForDisplay.collect { invalidate() }
                }
                launch {
                    viewModel.isInfiniteRecycles.collect { invalidate() }
                }
                launch {
                    viewModel.isMirroredLayout.collect { mirrored ->
                        isMirrored = mirrored
                        if (width > 0 && height > 0 && cardW > 0f) {
                            computeColumnX()
                        }
                        invalidate()
                    }
                }
                launch {
                    viewModel.hintDisplayState.collect { state ->
                        hintDisplayState = state
                        invalidate()
                    }
                }
                launch {
                    viewModel.singleClickGlowState.collect { state ->
                        singleClickGlowState = state
                        invalidate()
                    }
                }
            }
        }
    }

    fun setLockedPileAdIconTuning(
        portraitOffsetX: Float,
        portraitOffsetY: Float,
        portraitScaleX: Float,
        portraitScaleY: Float,
        landscapeOffsetX: Float,
        landscapeOffsetY: Float,
        landscapeScaleX: Float,
        landscapeScaleY: Float
    ) {
        lockedPileAdOffsetXPxPortrait = portraitOffsetX
        lockedPileAdOffsetYPxPortrait = portraitOffsetY
        lockedPileAdScaleXPortrait = portraitScaleX.coerceAtLeast(0.1f)
        lockedPileAdScaleYPortrait = portraitScaleY.coerceAtLeast(0.1f)
        lockedPileAdOffsetXPxLandscape = landscapeOffsetX
        lockedPileAdOffsetYPxLandscape = landscapeOffsetY
        lockedPileAdScaleXLandscape = landscapeScaleX.coerceAtLeast(0.1f)
        lockedPileAdScaleYLandscape = landscapeScaleY.coerceAtLeast(0.1f)
        invalidate()
    }

    private fun isLockedTableauPile(index: Int): Boolean {
        if (!::viewModel.isInitialized) return false
        return !viewModel.game.value.extraTableauUnlocked && index == Game.LOCKED_TABLEAU_INDEX
    }

    private fun syncColumnsWithGame(game: Game = viewModel.game.value) {
        val desiredColumns = game.tableau.size.coerceAtLeast(1)
        if (columns != desiredColumns) {
            columns = desiredColumns
            if (width > 0 && height > 0 && cardW > 0f) {
                // Deck count changes can alter tableau/foundation geometry without a size change.
                // Recompute full board metrics immediately so layout does not wait for rotation.
                recomputeBoardGeometry(width, height)
            }
        }
    }

    private fun getFoundationCountForLayout(): Int {
        return if (::viewModel.isInitialized) {
            viewModel.game.value.foundations.size.coerceAtLeast(1)
        } else {
            Game.FOUNDATION_COUNT_1_DECK
        }
    }

    /**
     * Get rect for a card at a specific position.
     * Used for calculating animation source positions.
     */
    fun getCardRectForAnimation(
        stackType: StackType,
        stackIndex: Int,
        cardIndex: Int
    ): RectF? {
        return when (stackType) {
            StackType.STOCK -> getStockRect()
            StackType.WASTE -> getWasteTopCardRect()
            StackType.FOUNDATION -> getFoundationRect(stackIndex)
            StackType.TABLEAU -> {
                val x = columnX.getOrNull(stackIndex) ?: return null
                var y = getTableauStartY()
                val pile = viewModel.game.value.tableau.getOrNull(stackIndex) ?: return null
                val cards = pile.asList()
                // cardIndex < 0 means destination drop position (bottom of current pile)
                val limit = if (cardIndex < 0) cards.size else cardIndex.coerceAtMost(cards.size)
                for (i in 0 until limit) {
                    y += if (cards[i].isFaceUp) cardW * 0.4f else cardW * 0.1f
                }
                RectF(x, y, x + cardW, y + cardH)
            }
        }
    }

    /**
     * Schedule a card move animation from startRect to endRect.
     * Used by auto-move and auto-foundation moves.
     */
    fun scheduleCardAnimation(
        card: Card,
        startRect: RectF,
        endRect: RectF,
        destStackType: StackType,
        destStackIndex: Int,
        movedCardCount: Int = 1
    ) {
        if (::viewModel.isInitialized) viewModel.pauseHintTimerForNonPlayerActivity()
        animationCard = card
        animationStartRect = RectF(startRect)
        animationEndRect = RectF(endRect)
        animationDestStackType = destStackType
        animationDestStackIndex = destStackIndex
        animationMovedCardCount = movedCardCount.coerceAtLeast(1)
        animationStartTimeMs = SystemClock.elapsedRealtime()
        isAnimating = true
        postInvalidateOnAnimation()
    }

    private fun isAnimationActive(): Boolean = isAnimating && SystemClock.elapsedRealtime() - animationStartTimeMs < ANIMATION_DURATION_MS

    /**
     * Exposed to activity-level UI flow so win dialogs/overlays can wait
     * until the final card animation is visible to the player.
     */
    fun isCardAnimationActive(): Boolean = isAnimationActive()

    private fun clearAnimationState() {
        val wasAnimating = isAnimating
        isAnimating = false
        animationCard = null
        animationStartRect = null
        animationEndRect = null
        animationDestStackType = null
        animationDestStackIndex = -1
        animationMovedCardCount = 1
        // Only resume hint timer when we're finishing a real animation, not on cold calls.
        if (wasAnimating && ::viewModel.isInitialized) {
            viewModel.resumeHintTimerAfterNonPlayerActivity()
        }
    }

    /**
     * Clears transient rendering state that can otherwise survive across a hard board reset
     * (restart/new hand) and hide cards until the next interaction.
     */
    fun resetTransientVisualState() {
        clearDragState()
        clearAnimationState()
        cancelNewGameDealAnimation()
        invalidate()
    }

    fun setMagicWandSelectionMode(enabled: Boolean) {
        magicWandSelectionMode = enabled
        clearDragState()
        invalidate()
    }

    private fun isAnimatingIntoFoundation(index: Int): Boolean {
        return animationActiveThisFrame &&
            animationDestStackType == StackType.FOUNDATION &&
            animationDestStackIndex == index
    }

    private fun isAnimatingIntoTableau(index: Int): Boolean {
        return animationActiveThisFrame &&
            animationDestStackType == StackType.TABLEAU &&
            animationDestStackIndex == index
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Detect orientation change
        val newIsLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (newIsLandscape != isLandscape) {
            isLandscape = newIsLandscape
            updateOffsetsForOrientation()
            loadTabletopImage()
            // Trigger recalculation of board dimensions
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w <= 0 || h <= 0) return

        if (::viewModel.isInitialized) {
            syncColumnsWithGame(viewModel.game.value)
        }

        recomputeBoardGeometry(w, h)
    }

    private fun recomputeBoardGeometry(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return

        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        // aspectFactors is still used for text-paint scaling; card dimensions use raw pixels
        // so that extreme-aspect compression never shrinks cards below what actually fits.
        val aspectFactors = UiScaleUtil.calculateBaselineScaleFactors(w / density, h / density)

        // Size cards from both width and height limits so narrow landscape layouts fit.
        // Do NOT pass the axis-compression/expansion factors here: on a phone in landscape
        // the GameBoardView itself has an extreme aspect ratio, which would make
        // verticalFactor = 0.5 and artificially halve the already-small height budget,
        // producing cards that are nearly invisible.
        val widthLimitedCardW = calculateWidthLimitedCardWidth(w)
        val heightLimitedCardW = calculateHeightLimitedCardWidth(h)

        cardW = min(widthLimitedCardW, heightLimitedCardW).coerceAtLeast(28f)

        // Deck/geometry fit pass: shrink until both horizontal and vertical budgets fit.
        val maxLayoutWidth = (w - (abs(BOARD_SHIFT_LEFT_PX) * 2f)).coerceAtLeast(1f)
        val maxLayoutHeight = (h * if (isLandscape) 0.98f else 0.94f).coerceAtLeast(1f)
        repeat(6) {
            val widthRatio = maxLayoutWidth / estimateBoardWidth(cardW)
            val heightRatio = maxLayoutHeight / estimateBoardHeight(cardW)
            val fitRatio = min(widthRatio, heightRatio)
            if (fitRatio >= 1f) return@repeat
            cardW = (cardW * fitRatio * 0.98f).coerceAtLeast(24f)
        }

        cardH = cardW * cardHeightRatio

        val spacingScale = (cardW / 70f).coerceIn(0.70f, 1.15f)
        cardPadding = baseCardPadding * spacingScale
        tableauOffset = baseTableauOffset * spacingScale
        cardRadius = baseCardRadius * spacingScale

        // Scale border and placeholder stroke widths with card size
        borderPaint.strokeWidth = (cardW * 0.025f).coerceIn(1.5f, 4f)
        placeholderPaint.strokeWidth = (cardW * 0.05f).coerceIn(2f, 7f)

        computeColumnX()
        computeBoardStartY(h)

        // Keep pile labels legible across phone/tablet sizes.
        pileLabelPaint.textSize = (cardW * 0.22f).coerceIn(22f, 40f)
        textPaint.textSize = ((min(w, h) * 0.10f) * aspectFactors.textCompression).coerceIn(28f, 60f)

        // Count badge scales with card size.
        pileCountBadgeRadius = (cardW * 0.16f).coerceIn(10f, 22f)
        pileCountBadgeInset = (cardW * 0.06f).coerceIn(4f, 10f)
        pileCountBadgeStrokePaint.strokeWidth = (cardW * 0.02f).coerceIn(1.5f, 3.5f)
        pileCountBadgeTextPaint.textSize = (pileCountBadgeRadius * 1.15f).coerceIn(12f, 24f)
    }

    /**
     * Maximum card width derived from the available horizontal space.
     * Uses the actual view width; no aspect-ratio compression is applied.
     */
    private fun calculateWidthLimitedCardWidth(viewWidth: Int): Float {
        val usableWidth = viewWidth * BOARD_WIDTH_FRACTION
        val totalPad = baseCardPadding * (columns + 1)
        return ((usableWidth - totalPad) / (columns + 1)) * cardWidthRatio * CARD_SIZE_MULTIPLIER
    }

    /**
     * Maximum card width derived from the available vertical space.
     * Uses the actual view height; no aspect-ratio compression is applied so
     * that landscape phone layouts do not compress cards beyond what fits.
     */
    private fun calculateHeightLimitedCardWidth(viewHeight: Int): Float {
        val effectiveHeight = viewHeight * if (isLandscape) 0.95f else 0.92f
        val cardHeightSlots = if (isLandscape) 3.60f else 4.85f
        val heightBasedCardH = effectiveHeight / cardHeightSlots
        return (heightBasedCardH / cardHeightRatio) * CARD_SIZE_MULTIPLIER
    }

    private fun estimateBoardWidth(candidateCardW: Float): Float {
        val spacingScale = (candidateCardW / 70f).coerceIn(0.70f, 1.15f)
        val candidatePadding = baseCardPadding * spacingScale
        val tableauWidth =
            (candidateCardW * columns) + (candidatePadding * (columns - 1).coerceAtLeast(0))

        if (!isLandscape) return tableauWidth

        val outerGap = landscapeOuterGap(candidateCardW)
        val sideReserve = 2f * (candidateCardW + outerGap)
        val wasteFanReserve = getWasteFanOffsetXForCardWidth(candidateCardW) * (MAX_VISIBLE_WASTE_CARDS - 1)
        val extraFoundationColumnReserve = if (getFoundationCountForLayout() > 4) {
            candidateCardW + (candidatePadding * 0.6f)
        } else {
            0f
        }
        return tableauWidth + sideReserve + wasteFanReserve + extraFoundationColumnReserve
    }

    private fun estimateBoardHeight(candidateCardW: Float): Float {
        val spacingScale = (candidateCardW / 70f).coerceIn(0.70f, 1.15f)
        val candidatePadding = baseCardPadding * spacingScale
        val candidateTableauOffset = baseTableauOffset * spacingScale
        val candidateCardH = candidateCardW * cardHeightRatio
        val dealtCount = if (::viewModel.isInitialized) {
            Game.dealtTableauCountFor(viewModel.game.value.deckCount)
        } else {
            (columns - 1).coerceAtLeast(1)
        }
        val maxCascadeCards = (dealtCount - 1).coerceAtLeast(1)
        val estimatedTableauHeight = candidateCardH + (maxCascadeCards * candidateCardW * 0.4f)

        val extraPortraitFoundationRowHeight = if (!isLandscape && getFoundationCountForLayout() > 4) {
            candidateCardH + (candidatePadding * 0.85f)
        } else {
            0f
        }

        return candidateCardH +
            (candidatePadding + candidateTableauOffset) +
            estimatedTableauHeight +
            extraPortraitFoundationRowHeight
    }

    private fun landscapeOuterGap(candidateCardW: Float = cardW): Float {
        return (candidateCardW * LANDSCAPE_OUTER_GAP_FACTOR).coerceAtLeast(10f)
    }

    private fun computeColumnX() {
        columnX.clear()

        val boardWidth =
            (cardW * columns) + (cardPadding * (columns - 1))

        var startX = (width - boardWidth) / 2f - BOARD_SHIFT_LEFT_PX

        if (isLandscape) {
            startX += if (isMirrored) {
                LANDSCAPE_DIRECTIONAL_SHIFT_PX
            } else {
                -LANDSCAPE_DIRECTIONAL_SHIFT_PX
            }
        }

        // In 2-deck landscape, foundations consume a second side column.
        // Nudge tableau toward stock/waste so both foundation columns remain visible.
        if (isLandscape && getFoundationCountForLayout() > 4) {
            val shiftTowardStockWaste = (cardW + cardPadding * 0.6f) * 0.55f
            startX += if (isMirrored) shiftTowardStockWaste else -shiftTowardStockWaste
        }

        var x = startX
        repeat(columns) {
            columnX.add(x)
            x += cardW + cardPadding
        }
    }

    private fun getTopRowY(): Float = boardStartY + cardPadding

    private fun getTableauStartY(): Float {
        if (!isLandscape) {
            val extraFoundationRow = if (getFoundationCountForLayout() > 4) {
                cardH + (cardPadding * 0.85f)
            } else {
                0f
            }
            return getTopRowY() + cardH + cardPadding + tableauOffset + extraFoundationRow
        }

        // In landscape, align tableau top with foundation pile 1 (and stock pile)
        val foundationRect = getFoundationRect(0)
        return foundationRect.top
    }

    private fun getStockRect(): RectF {
        if (!isLandscape) {
            val topY = getTopRowY()
            val y = topY + topPileVerticalShiftPx()
            val stockColumnIndex = if (isMirrored) {
                columnX.lastIndex.coerceAtLeast(0)
            } else {
                1.coerceAtMost(columnX.lastIndex.coerceAtLeast(0))
            }
            val x = columnX.getOrElse(stockColumnIndex) { 0f }
            return RectF(x, y, x + cardW, y + cardH)
        }

        // Landscape: stock is outside the tableau columns (swapped — now below waste, aligned to foundation[1])
        val foundationRect = getFoundationRect(1)
        val y = foundationRect.top + topPileVerticalShiftPx() + (cardH * LANDSCAPE_WASTE_EXTRA_SHIFT_RATIO) + 50f
        val landscapeOuterGap = landscapeOuterGap()
        val x = if (isMirrored) {
            // Mirrored: stock to the RIGHT of tableau
            (columnX.lastOrNull()?.let { it + cardW } ?: (width - cardPadding)) + landscapeOuterGap
        } else {
            // Classic: stock to the LEFT of tableau
            (columnX.firstOrNull() ?: cardPadding) - cardW - landscapeOuterGap
        }
        return RectF(x, y, x + cardW, y + cardH)
    }

    private fun getWasteRect(): RectF {
        if (!isLandscape) {
            val topY = getTopRowY()
            val y = topY + topPileVerticalShiftPx()
            val wasteColumnIndex = if (isMirrored) {
                (columnX.lastIndex - 1).coerceAtLeast(0)
            } else {
                2.coerceAtMost(columnX.lastIndex.coerceAtLeast(0))
            }
            val x = columnX.getOrElse(wasteColumnIndex) { 0f }
            return RectF(x, y, x + cardW, y + cardH)
        }

        // Landscape: waste is outside the tableau columns (swapped — now above stock, aligned to foundation[0])
        val foundationRect = getFoundationRect(0)
        val y = foundationRect.top + topPileVerticalShiftPx()
        val landscapeOuterGap = landscapeOuterGap()
        val x = if (isMirrored) {
            // Mirrored: waste to the RIGHT of tableau (same X column as mirrored stock)
            (columnX.lastOrNull()?.let { it + cardW } ?: (width - cardPadding)) + landscapeOuterGap
        } else {
            // Classic: waste to the LEFT of tableau (same X column as classic stock)
            (columnX.firstOrNull() ?: cardPadding) - cardW - landscapeOuterGap
        }
        return RectF(x, y, x + cardW, y + cardH)
    }

    private fun getWasteFanOffsetXForCardWidth(candidateCardW: Float): Float =
        (candidateCardW * 0.3f).coerceIn(12f, 45f)

    private fun getWasteFanOffsetX(): Float = getWasteFanOffsetXForCardWidth(cardW)

    private fun getWasteFanDirection(): Float = if (isMirrored) -1f else 1f

    private fun getVisibleWasteCardCount(wasteSize: Int): Int = min(wasteSize, MAX_VISIBLE_WASTE_CARDS)

    private fun getWasteVisibleCardRect(visibleIndex: Int): RectF {
        val base = getWasteRect()
        val shift = getWasteFanOffsetX() * visibleIndex * getWasteFanDirection()
        return RectF(base.left + shift, base.top, base.right + shift, base.bottom)
    }

    private fun getWasteTopCardRect(): RectF {
        val visibleCount = getVisibleWasteCardCount(viewModel.game.value.waste.size())
        val topVisibleIndex = (visibleCount - 1).coerceAtLeast(0)
        return getWasteVisibleCardRect(topVisibleIndex)
    }

    private fun getWasteHitRect(): RectF {
        val base = getWasteRect()
        val top = getWasteTopCardRect()
        return RectF(
            min(base.left, top.left),
            min(base.top, top.top),
            max(base.right, top.right),
            max(base.bottom, top.bottom)
        )
    }

    private fun getFoundationRect(index: Int): RectF {
        if (!isLandscape) {
            val topY = getTopRowY()
            val foundationCount = getFoundationCountForLayout()

            val row = if (foundationCount > 4) index / 4 else 0
            val col = if (foundationCount > 4) index % 4 else index
            val colsInRow = if (foundationCount > 4) 4 else foundationCount
            val foundationStartIndex = if (isMirrored) 0 else (columns - colsInRow).coerceAtLeast(0)
            val columnIndex = (foundationStartIndex + col).coerceIn(0, columnX.lastIndex.coerceAtLeast(0))
            val x = columnX.getOrElse(columnIndex) { 0f }
            val y = topY + row * (cardH + cardPadding * 0.85f)
            return RectF(x, y, x + cardW, y + cardH)
        }

        // Landscape: foundations are outside the tableau columns
        val landscapeOuterGap = landscapeOuterGap()
        val baseX = if (isMirrored) {
            // Mirrored: foundations to the LEFT of tableau
            (columnX.firstOrNull() ?: cardPadding) - cardW - landscapeOuterGap
        } else {
            // Classic: foundations to the RIGHT of tableau
            (columnX.lastOrNull()?.let { it + cardW } ?: (width - cardPadding)) + landscapeOuterGap
        }

        val foundationCount = getFoundationCountForLayout()
        val row = if (foundationCount > 4) index % 4 else index
        val col = if (foundationCount > 4) (index / 4) else 0
        val columnStep = cardW + (cardPadding * 0.6f)
        val x = if (isMirrored) {
            baseX - (col * columnStep)
        } else {
            baseX + (col * columnStep)
        }

        // Y position: vertically stacked foundations in landscape.
        // Multi-column (2-deck) foundations use full-height spacing to avoid vertical overlap.
        val topRowY = getTopRowY()
        val baseY = topRowY + cardH + cardPadding + tableauOffset -
            (cardH * LANDSCAPE_FOUNDATION_BASE_Y_SHIFT_RATIO)
        val stepY = if (foundationCount > 4) {
            cardH * LANDSCAPE_FOUNDATION_STEP_RATIO_MULTI_COLUMN
        } else {
            cardH * 0.88f
        }
        val y = if (foundationCount > 4) {
            baseY + (row * stepY)
        } else {
            val pileAdjustmentFractions = floatArrayOf(-0.65f, -0.40f, -0.15f, 0.15f)
            val pileAdjust = pileAdjustmentFractions.getOrElse(index) { 0f } * cardH
            baseY + (index * stepY) + pileAdjust
        }

        return RectF(x, y, x + cardW, y + cardH)
    }

    private fun computeBoardStartY(viewHeight: Int) {

        val estimatedBoardHeight = estimateBoardHeight(cardW)

        boardStartY = (viewHeight - estimatedBoardHeight) / 6f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawTabletop(canvas)
        if (!::viewModel.isInitialized) {
            animationActiveThisFrame = false
            drawLoading(canvas)
            return
        }
        syncColumnsWithGame()
        if (cardW <= 0f || cardH <= 0f || columnX.size < columns) {
            animationActiveThisFrame = false
            drawLoading(canvas)
            return
        }
        val frameNowMs = SystemClock.elapsedRealtime()
        animationActiveThisFrame = isAnimating && (frameNowMs - animationStartTimeMs) < ANIMATION_DURATION_MS
        drawTopRow(canvas)
        drawTableau(canvas)
        drawHintGlows(canvas)
        drawSingleClickGlows(canvas)
        drawDragGhost(canvas)
        drawAnimatedCard(canvas, frameNowMs)
        drawCouponFlight(canvas)
    }

    private fun drawTabletop(canvas: Canvas) {
        tabletopBitmap?.let { bitmap ->
            val destRect = RectF(0F, 0F, width.toFloat(), height.toFloat())
            canvas.drawBitmap(bitmap, null, destRect, null)
        }
    }

    private fun drawDragGhost(canvas: Canvas) {
        if (!isDragging) return

        val x = dragX - dragOffsetX
        val y = dragY - dragOffsetY

        when (dragStackType) {

            StackType.TABLEAU -> {
                val pile = viewModel.game.value.tableau.getOrNull(dragStackIndex) ?: return
                var curY = y

                pile.asList()
                    .drop(dragCardIndex)
                    .forEach { card ->
                        val rect = RectF(x, curY, x + cardW, curY + cardH)
                        drawCard(canvas, card, rect)
                        curY += cardW * 0.4f
                    }
            }

            StackType.WASTE -> {
                val card = viewModel.game.value.waste.peek() ?: return
                val rect = RectF(x, y, x + cardW, y + cardH)
                drawCard(canvas, card, rect)
            }

            StackType.FOUNDATION -> {
                val card = viewModel.game.value.foundations
                    .getOrNull(dragStackIndex)
                    ?.peek() ?: return
                val rect = RectF(x, y, x + cardW, y + cardH)
                drawCard(canvas, card, rect)
            }

            else -> Unit
        }
    }

    private fun drawAnimatedCard(canvas: Canvas, frameNowMs: Long) {
        // Fast-exit: no animation was ever scheduled, nothing to do.
        if (!isAnimating) return

        if (!animationActiveThisFrame) {
            // Animation just finished – transition out cleanly (this triggers resume of hint timer).
            clearAnimationState()
            return
        }

        val card = animationCard ?: return
        val startRect = animationStartRect ?: return
        val endRect = animationEndRect ?: return

        val elapsedMs = frameNowMs - animationStartTimeMs
        val progress = (elapsedMs.toFloat() / ANIMATION_DURATION_MS).coerceIn(0f, 1f)
        val eased = easeInOutCubic(progress)

        // Move along a curve: linear base path + parabolic upward lift.
        val baseRect = RectF(
            lerp(startRect.left, endRect.left, eased),
            lerp(startRect.top, endRect.top, eased),
            lerp(startRect.right, endRect.right, eased),
            lerp(startRect.bottom, endRect.bottom, eased)
        )

        val dx = endRect.centerX() - startRect.centerX()
        val dy = endRect.centerY() - startRect.centerY()
        val distance = kotlin.math.hypot(dx, dy)

        // Keep the current arc for short moves, then scale it up as travel distance grows.
        val baselineArcHeight = (distance * 0.14f).coerceIn(cardH * 0.10f, cardH * 0.42f)
        val shortestMoveDistance = (cardW * 1.20f).coerceAtLeast(1f)
        val longMoveRange = (cardW * 4.5f).coerceAtLeast(1f)
        val longMoveProgress = ((distance - shortestMoveDistance) / longMoveRange).coerceIn(0f, 1f)
        val dramaticMultiplier = 1f + (longMoveProgress * longMoveProgress) * 1.45f
        val arcHeight = (baselineArcHeight * dramaticMultiplier).coerceIn(cardH * 0.10f, cardH * 0.82f)
        val arcLift = -4f * arcHeight * eased * (1f - eased)

        val interpolatedRect = RectF(
            baseRect.left,
            baseRect.top + arcLift,
            baseRect.right,
            baseRect.bottom + arcLift
        )

        drawCard(canvas, card, interpolatedRect)

        // Keep redrawing while animating
        if (progress < 1f) {
            postInvalidateOnAnimation()
        }
    }

    private fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t

    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) {
            4f * t * t * t
        } else {
            1f - ((-2f * t + 2f) * (-2f * t + 2f) * (-2f * t + 2f)) / 2f
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Coupon flight animation
    // ─────────────────────────────────────────────────────────────

    /**
     * Schedule a coupon flight animation from the ticket icon to a target button.
     * Call from GameActivity when a coupon is about to be consumed.
     */
    fun scheduleCouponAnimation(sourceRect: RectF, targetRect: RectF) {
        val landscapeMinArcPx = if (isLandscape) {
            val boardHeight = height.toFloat().coerceAtLeast(1f)
            // Force a theatrical dip into the board even for short control-to-control hops.
            // Keep this tied to board height (not card height) so it remains clearly visible.
            (boardHeight * 0.45f).coerceIn(cardH * 1.5f, boardHeight * 0.60f)
        } else {
            0f
        }

        val landscapeControlPoint = if (isLandscape) {
            val waste = getWasteRect()
            // Route toward waste area first, then come back to the selected helper button.
            val controlX = waste.centerX().coerceIn(cardW * 0.5f, width - cardW * 0.5f)
            val controlY = (waste.centerY() + cardH * 0.9f).coerceIn(cardH * 0.5f, height - cardH * 0.5f)
            PointF(controlX, controlY)
        } else {
            null
        }

        couponFlightAnimator.scheduleCouponAnimation(
            sourceRect,
            targetRect,
            isLandscape = isLandscape,
            landscapeMinArcPx = landscapeMinArcPx,
            controlPoint = landscapeControlPoint
        )
        postInvalidateOnAnimation()
    }

    private fun drawCouponFlight(canvas: Canvas) {
        if (!couponFlightAnimator.isAnimating()) return

        val wasAnimating = couponFlightAnimator.isAnimating()
        couponFlightAnimator.drawAnimatedCoupon(canvas, couponDrawable)
        val isAnimatingNow = couponFlightAnimator.isAnimating()

        // Continue while active; when it just finished, invalidate once more to clear last frame.
        if (isAnimatingNow || (wasAnimating && !isAnimatingNow)) {
            postInvalidateOnAnimation()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Hint glow drawing
    // ─────────────────────────────────────────────────────────────

    private fun drawHintGlows(canvas: Canvas) {
        val state = hintDisplayState ?: return
        val move  = state.move

        val showSource = state.phase == HintPhase.SOURCE_ONLY || state.phase == HintPhase.SOURCE_AND_DEST
        val showDest   = state.phase == HintPhase.SOURCE_AND_DEST || state.phase == HintPhase.DEST_ONLY

        if (showSource) {
            getHintSourceRect(move)?.let { drawGlowRing(canvas, it, hintGlowSourcePaint) }
        }
        if (showDest) {
            getHintDestRect(move)?.let { drawGlowRing(canvas, it, hintGlowDestPaint) }
        }
    }

    /**
     * Draw a layered glow ring around [rect].
     * Four concentric strokes with increasing opacity simulate a bloom/blur glow.
     * Works on hardware-accelerated canvas (no BlurMaskFilter needed).
     */
    private fun drawGlowRing(canvas: Canvas, rect: RectF, basePaint: Paint) {
        // Scale glow ring geometry relative to card width so it looks consistent
        // across phone (~60px cardW) and tablet (~120px cardW) sizes.
        val cw = cardW.coerceAtLeast(30f)
        val expansions   = floatArrayOf(cw * 0.20f, cw * 0.14f, cw * 0.09f, cw * 0.04f)
        val alphas       = intArrayOf(55, 110, 175, 255)
        val strokeWidths = floatArrayOf(cw * 0.065f, cw * 0.065f, cw * 0.055f, cw * 0.040f)
        val savedAlpha = basePaint.alpha

        for (i in expansions.indices) {
            val exp = expansions[i]
            val r = RectF(rect.left - exp, rect.top - exp, rect.right + exp, rect.bottom + exp)
            basePaint.alpha = alphas[i]
            basePaint.strokeWidth = strokeWidths[i]
            canvas.drawRoundRect(r, cardRadius + exp, cardRadius + exp, basePaint)
        }
        basePaint.alpha = savedAlpha
    }

    /** Returns the rect of the source card in a hint move. */
    private fun getHintSourceRect(move: com.gpgamelab.justpatience.model.HintMove): RectF? =
        getCardRectForAnimation(move.sourceStackType, move.sourceStackIndex, move.sourceCardIndex)

    /**
     * Returns the rect at the destination of a hint move.
     * For FOUNDATION: the fixed pile rect.
     * For TABLEAU: the position immediately on top of any existing cards (drop zone).
     */
    private fun getHintDestRect(move: com.gpgamelab.justpatience.model.HintMove): RectF? {
        return when (move.destStackType) {
            StackType.FOUNDATION -> getFoundationRect(move.destStackIndex)
            StackType.TABLEAU -> {
                val x = columnX.getOrNull(move.destStackIndex) ?: return null
                val pile = viewModel.game.value.tableau.getOrNull(move.destStackIndex) ?: return null
                var y = getTableauStartY()
                for (card in pile.asList()) {
                    y += if (card.isFaceUp) cardW * 0.4f else cardW * 0.1f
                }
                RectF(x, y, x + cardW, y + cardH)
            }
            else -> null
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Single-click glow drawing (destination disambiguation)
    // ─────────────────────────────────────────────────────────────

    private fun drawSingleClickGlows(canvas: Canvas) {
        val glowState = singleClickGlowState ?: return

        // Draw source card glow
        getSourceGlowRect(glowState)?.let { drawGlowRing(canvas, it, hintGlowSourcePaint) }

        // Draw all destination glows simultaneously
        for (dest in glowState.destinations) {
            getDestinationGlowRect(dest)?.let { drawGlowRing(canvas, it, hintGlowDestPaint) }
        }
    }

    private fun getSourceGlowRect(glowState: SingleClickGlowState): RectF? =
        getCardRectForAnimation(glowState.sourceStackType, glowState.sourceStackIndex, glowState.sourceCardIndex)

    private fun getDestinationGlowRect(dest: com.gpgamelab.justpatience.model.GlowDestination): RectF? {
        return when (dest.destStackType) {
            StackType.FOUNDATION -> getFoundationRect(dest.destStackIndex)
            StackType.TABLEAU -> {
                val x = columnX.getOrNull(dest.destStackIndex) ?: return null
                val pile = viewModel.game.value.tableau.getOrNull(dest.destStackIndex) ?: return null
                var y = getTableauStartY()
                for (card in pile.asList()) {
                    y += if (card.isFaceUp) cardW * 0.4f else cardW * 0.1f
                }
                RectF(x, y, x + cardW, y + cardH)
            }
            else -> null
        }
    }

    private fun drawLoading(c: Canvas) {
        textPaint.color = Color.GRAY
        c.drawText("Loading Game...", width / 2f, height / 2f, textPaint)
    }

    private fun drawTopRow(canvas: Canvas) {
        // Stock
        val stockRect = getStockRect()
        val stock = viewModel.game.value.stock
        if (!stock.isEmpty()) {
            drawStockBack(canvas, stockRect)
        } else {
            drawEmptyStockPlaceholder(canvas, stockRect)
        }

        // Waste
        val wasteRect = getWasteRect()
        val waste = viewModel.game.value.waste
        if (!waste.isEmpty()) {
            val visibleCount = getVisibleWasteCardCount(waste.size())
            val visibleCards = waste.asList().takeLast(visibleCount)
            visibleCards.forEachIndexed { visibleIndex, card ->
                val isTopVisible = visibleIndex == visibleCards.lastIndex
                if (isDragging && dragStackType == StackType.WASTE && isTopVisible) return@forEachIndexed
                drawCard(canvas, card, getWasteVisibleCardRect(visibleIndex))
            }
        } else {
            canvas.drawRoundRect(wasteRect, cardRadius, cardRadius, placeholderPaint)
        }

        drawPileCountBadge(canvas, stockRect, stock.size())
        if (waste.size() >= WASTE_COUNT_BADGE_MIN_VISIBLE_COUNT) {
            drawPileCountBadge(canvas, getWasteTopCardRect(), waste.size())
        }

        // Foundations: portrait keeps top row placement; landscape moves them left/right of tableau.
        for (i in viewModel.game.value.foundations.indices) {
            val rect = getFoundationRect(i)
            canvas.drawRoundRect(rect, cardRadius, cardRadius, placeholderPaint)

            val foundationPile = viewModel.game.value.foundations.getOrNull(i) ?: continue
            val cards = foundationPile.asList()
            if (cards.isEmpty()) continue

            val cardToDraw = if (isAnimatingIntoFoundation(i)) {
                cards.getOrNull(cards.lastIndex - 1)
            } else if (isDragging && dragStackType == StackType.FOUNDATION && dragStackIndex == i) {
                cards.getOrNull(cards.lastIndex - 1)
            } else {
                cards.last()
            }

            cardToDraw?.let { drawCard(canvas, it, rect) }
        }

        // Labels above stock and waste piles.
        drawTopPileLabels(canvas, stockRect, wasteRect)
    }

    private fun drawTopPileLabels(canvas: Canvas, stockRect: RectF, wasteRect: RectF) {
        val drawCount = viewModel.getDrawCountLabelValue()
        val drawTopLine = resources.getQuantityString(R.plurals.stock_draw_label, drawCount, drawCount)
        val drawLines = listOf(drawTopLine, context.getString(R.string.stock_draw_label_line_2))

        val remainingRecycles = viewModel.getRemainingRecycleCount()
        val recycleLines = if (remainingRecycles == null) {
            listOf(
                context.getString(R.string.recycle_remaining_unlimited),
                context.getString(R.string.recycle_remaining_unlimited_line_2)
            )
        } else {
            listOf(
                context.getString(R.string.recycle_remaining_format, remainingRecycles),
                context.getString(R.string.recycle_remaining_limited_line_2)
            )
        }

        drawLabelAboveRect(canvas, stockRect, drawLines)
        val recycleLabelColor = if (remainingRecycles == 0) Color.RED else Color.WHITE
        drawLabelAboveRect(canvas, wasteRect, recycleLines, recycleLabelColor)
    }

    private fun drawPileCountBadge(canvas: Canvas, pileRect: RectF, count: Int) {
        if (count < 1) return

        val badgeText = if (count > 99) "99+" else count.toString()
        val cx = pileRect.right - pileCountBadgeInset - pileCountBadgeRadius
        val cy = pileRect.top + pileCountBadgeInset + pileCountBadgeRadius

        canvas.drawCircle(cx, cy, pileCountBadgeRadius, pileCountBadgeFillPaint)
        canvas.drawCircle(cx, cy, pileCountBadgeRadius, pileCountBadgeStrokePaint)

        val fm = pileCountBadgeTextPaint.fontMetrics
        val baseline = cy - (fm.ascent + fm.descent) / 2f
        canvas.drawText(badgeText, cx, baseline, pileCountBadgeTextPaint)
    }

    private fun topPileVerticalShiftPx(): Float {
        val ratio = if (isLandscape) LANDSCAPE_TOP_PILE_SHIFT_RATIO else PORTRAIT_TOP_PILE_SHIFT_RATIO
        return cardH * ratio
    }

    private fun drawEmptyStockPlaceholder(canvas: Canvas, rect: RectF) {
        canvas.drawRoundRect(rect, cardRadius, cardRadius, placeholderPaint)

        if (!shouldShowRecycleIndicator()) return

        val bitmap = recycleIndicatorBitmap ?: return
        val iconSize = min(rect.width(), rect.height()) * 0.58f
        val iconRect = RectF(
            rect.centerX() - iconSize / 2f,
            rect.centerY() - iconSize / 2f,
            rect.centerX() + iconSize / 2f,
            rect.centerY() + iconSize / 2f
        )
        canvas.drawBitmap(bitmap, null, iconRect, null)
    }

    private fun shouldShowRecycleIndicator(): Boolean {
        val game = viewModel.game.value
        if (!game.stock.isEmpty() || game.waste.isEmpty()) return false

        val remainingRecycles = viewModel.getRemainingRecycleCount()
        return remainingRecycles == null || remainingRecycles > 0
    }

    private fun drawLabelAboveRect(canvas: Canvas, rect: RectF, lines: List<String>, color: Int = Color.WHITE) {
        if (lines.isEmpty()) return

        val density = resources.displayMetrics.density
        val lineSpacing = 2f * density
        val fontMetrics = pileLabelPaint.fontMetrics
        val lineHeight = (fontMetrics.descent - fontMetrics.ascent).coerceAtLeast(1f)
        val totalTextHeight = lines.size * lineHeight + (lines.size - 1) * lineSpacing
        val minBottomBaseline = (-fontMetrics.ascent) + totalTextHeight - lineHeight + 2f
        val labelBottomY = (rect.top - (6f * density)).coerceAtLeast(minBottomBaseline)

        val previousColor = pileLabelPaint.color
        pileLabelPaint.color = color

        var baseline = labelBottomY - (lines.size - 1) * (lineHeight + lineSpacing)
        lines.forEach { line ->
            canvas.drawText(line, rect.centerX(), baseline, pileLabelPaint)
            baseline += lineHeight + lineSpacing
        }

        pileLabelPaint.color = previousColor
    }

    private fun drawTableau(canvas: Canvas) {
        val startY = getTableauStartY()
        viewModel.game.value.tableau.forEachIndexed { colIdx, pile ->
            val x = columnX.getOrNull(colIdx) ?: return@forEachIndexed
            var y = startY
            val cards = pile.asList()
            if (isLockedTableauPile(colIdx)) {
                val rect = RectF(x, y, x + cardW, y + cardH)
                drawLockedTableauPlaceholder(canvas, rect)
                return@forEachIndexed
            }
            if (cards.isEmpty()) {
                val rect = RectF(x, y, x + cardW, y + cardH)
                canvas.drawRoundRect(rect, cardRadius, cardRadius, placeholderPaint)
            } else {
                cards.forEachIndexed { index, card ->
                    if (shouldHideCardForNewGameDeal(colIdx, index)) {
                        y += if (card.isFaceUp) cardW * 0.4f else cardW * 0.1f
                        return@forEachIndexed
                    }

                    // Skip cards that are currently being dragged
                    if (isDragging &&
                        dragStackType == StackType.TABLEAU &&
                        colIdx == dragStackIndex &&
                        index >= dragCardIndex
                    ) {
                        y += if (card.isFaceUp) cardW * 0.4f else cardW * 0.1f
                        return@forEachIndexed
                    }

                    // Suppress cards that just arrived at animation destination.
                    // They are represented by the flying animation card during flight.
                    if (isAnimatingIntoTableau(colIdx) && index >= cards.size - animationMovedCardCount) {
                        y += if (card.isFaceUp) cardW * 0.4f else cardW * 0.1f
                        return@forEachIndexed
                    }

                    val rect = RectF(x, y, x + cardW, y + cardH)
                    if (card.isFaceUp)
                        drawCard(canvas, card, rect)
                    else
                        drawCardBack(canvas, rect, card.verso)

                    y += if (card.isFaceUp) cardW * 0.4f else cardW * 0.1f
                }
            }
        }
    }

    private fun drawLockedTableauPlaceholder(canvas: Canvas, rect: RectF) {
        canvas.drawRoundRect(rect, cardRadius, cardRadius, placeholderPaint)
        canvas.drawRoundRect(rect, cardRadius, cardRadius, lockedPileOverlayPaint)

        val isLandscapeNow = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val adScaleX = if (isLandscapeNow) lockedPileAdScaleXLandscape else lockedPileAdScaleXPortrait
        val adScaleY = if (isLandscapeNow) lockedPileAdScaleYLandscape else lockedPileAdScaleYPortrait
        val adOffsetX = if (isLandscapeNow) lockedPileAdOffsetXPxLandscape else lockedPileAdOffsetXPxPortrait
        val adOffsetY = if (isLandscapeNow) lockedPileAdOffsetYPxLandscape else lockedPileAdOffsetYPxPortrait

        lockedPileAdDrawable?.let { drawable ->
            val baseW = rect.width() * 0.60f
            val baseH = rect.height() * 0.45f
            val iconW = (baseW * adScaleX).coerceAtLeast(12f)
            val iconH = (baseH * adScaleY).coerceAtLeast(12f)
            val left = rect.centerX() - iconW / 2f + adOffsetX
            val top = rect.centerY() - iconH / 2f + adOffsetY
            drawable.bounds = Rect(left.toInt(), top.toInt(), (left + iconW).toInt(), (top + iconH).toInt())
            drawable.draw(canvas)
        }

        lockedPileLockDrawable?.let { drawable ->
            val size = min(rect.width(), rect.height()) * 0.28f
            val left = rect.left + cardPadding * 0.35f
            val top = rect.top + cardPadding * 0.35f
            drawable.bounds = Rect(left.toInt(), top.toInt(), (left + size).toInt(), (top + size).toInt())
            drawable.draw(canvas)
        }
    }

    /**
     * Plays a simple tableau reveal in dealing order and invokes [onCardDealt] for SFX sync.
     */
    fun startNewGameDealAnimation(
        dealCardIntervalMs: Long = NEW_GAME_DEAL_CARD_INTERVAL_MS,
        onCardDealt: (() -> Unit)? = null
    ) {
        if (!::viewModel.isInitialized) return
        cancelNewGameDealAnimation()

        val game = viewModel.game.value
        val order = hashMapOf<Int, Int>()
        var ordinal = 0
        val dealtCount = Game.dealtTableauCountFor(game.deckCount)
        for (row in 0 until dealtCount) {
            for (dealtCol in row until dealtCount) {
                val col = dealtCol + 1
                val pileSize = game.tableau.getOrNull(col)?.size() ?: 0
                if (row < pileSize) {
                    order[dealKey(col, row)] = ordinal++
                }
            }
        }

        if (order.isEmpty()) return

        val safeInterval = dealCardIntervalMs.coerceAtLeast(0L)
        newGameDealOrderByCard = order
        newGameDealRevealCount = 0
        newGameDealActive = true
        newGameDealStartedAtMs = SystemClock.elapsedRealtime()
        newGameDealMaxDurationMs = (order.size * safeInterval) + 1500L
        invalidate()

        val step = object : Runnable {
            override fun run() {
                if (!newGameDealActive) return
                newGameDealRevealCount++
                onCardDealt?.invoke()
                invalidate()

                if (newGameDealRevealCount >= newGameDealOrderByCard.size) {
                    cancelNewGameDealAnimation()
                    return
                }
                postDelayed(this, safeInterval)
            }
        }
        newGameDealStepRunnable = step
        post(step)
    }

    fun cancelNewGameDealAnimation() {
        newGameDealStepRunnable?.let { removeCallbacks(it) }
        newGameDealStepRunnable = null
        if (newGameDealActive || newGameDealRevealCount != 0 || newGameDealOrderByCard.isNotEmpty()) {
            newGameDealActive = false
            newGameDealRevealCount = 0
            newGameDealOrderByCard.clear()
            newGameDealStartedAtMs = 0L
            newGameDealMaxDurationMs = 0L
            invalidate()
        }
    }

    private fun shouldHideCardForNewGameDeal(colIdx: Int, cardIdx: Int): Boolean {
        if (!newGameDealActive) return false
        val elapsed = SystemClock.elapsedRealtime() - newGameDealStartedAtMs
        if (newGameDealMaxDurationMs > 0L && elapsed > newGameDealMaxDurationMs) {
            // Safety net: never keep cards hidden if a reveal runnable gets interrupted.
            cancelNewGameDealAnimation()
            return false
        }
        val order = newGameDealOrderByCard[dealKey(colIdx, cardIdx)] ?: return false
        return order >= newGameDealRevealCount
    }

    private fun dealKey(colIdx: Int, cardIdx: Int): Int = (colIdx shl 8) or (cardIdx and 0xFF)

    private fun drawCard(canvas: Canvas, card: Card, rect: RectF) {
        val targetW = max(1, rect.width().toInt())
        val targetH = max(1, rect.height().toInt())

        val bitmap = assetResolver.resolve(
            currentSetId,
            card.recto.imagePath,
            targetW,
            targetH
        )

        canvas.drawBitmap(
            bitmap,
            null,          // draw entire bitmap
            rect,          // scale to card rect
            null
        )
    }

    private fun drawCardBack(canvas: Canvas, rect: RectF, verso: Verso) {
        val targetW = max(1, rect.width().toInt())
        val targetH = max(1, rect.height().toInt())

        val bitmap = assetResolver.resolve(
            currentSetId,
            verso.imagePath,
            targetW,
            targetH
        )

        canvas.drawBitmap(
            bitmap,
            null,
            rect,
            null
        )
    }

    private fun drawStockBack(canvas: Canvas, rect: RectF) {
        val bitmap = assetResolver.resolve(
            currentSetId,
            DEFAULT_STOCK_BACK_IMAGE_PATH,
            max(1, cardW.toInt()),
            max(1, cardH.toInt())
        )
        canvas.drawBitmap(bitmap, null, rect, null)
    }

    private fun handleTap(x: Float, y: Float) {
        if (magicWandSelectionMode) {
            val (type, stackIndex, cardIndex) = findStackAt(x, y)
            when (type) {
                StackType.TABLEAU -> {
                    val pile = viewModel.game.value.tableau.getOrNull(stackIndex) ?: return
                    if (pile.isEmpty()) {
                        // Empty tableau – let the activity decide which king to fetch
                        onMagicWandTargetSelected?.invoke(type, stackIndex, -1)
                    } else {
                        val card = pile.peekAt(cardIndex)
                        if (card?.isFaceUp == true) {
                            onMagicWandTargetSelected?.invoke(type, stackIndex, cardIndex)
                        }
                    }
                }
                StackType.FOUNDATION -> {
                    val foundation = viewModel.game.value.foundations.getOrNull(stackIndex)
                    val top = foundation?.peek()
                    if (foundation != null && foundation.isEmpty()) {
                        // Empty foundation – let the activity decide which ace to fetch
                        onMagicWandTargetSelected?.invoke(type, stackIndex, -1)
                    } else if (top?.isFaceUp == true) {
                        onMagicWandTargetSelected?.invoke(type, stackIndex, -1)
                    }
                }
                else -> Unit
            }
            return
        }

        // ── Glow-destination intercept ────────────────────────────
        // When multiple destinations are highlighted, the next tap either:
        //   • lands inside a destination rect  → execute the move there
        //   • lands anywhere else              → dismiss the glow (player chose to do something else)
        val glowState = singleClickGlowState
        if (glowState != null) {
            for (dest in glowState.destinations) {
                val destRect = getDestinationGlowRect(dest)
                if (destRect != null && destRect.contains(x, y)) {
                    executeSingleClickGlowMove(glowState, dest)
                    return
                }
            }
            // Tapped outside every destination → dismiss
            viewModel.clearSingleClickGlow()
            return
        }

        // ── Normal tap handling (no glow active) ─────────────────
        val (type, stackIndex, cardIndex) = findStackAt(x, y)

        when (type) {
            StackType.STOCK -> {
                when (viewModel.drawFromStock()) {
                    GameViewModel.DrawResult.NORMAL_DRAW   -> onClickMoveSoundRequested?.invoke()
                    GameViewModel.DrawResult.RECYCLE_SHUFFLE -> onShuffleSoundRequested?.invoke()
                    GameViewModel.DrawResult.NO_MOVE       -> Unit
                }
            }

            StackType.WASTE -> {
                if (viewModel.handleSingleClickOnWaste()) {
                    onClickMoveSoundRequested?.invoke()
                }
            }

            StackType.TABLEAU -> {
                if (stackIndex == Game.LOCKED_TABLEAU_INDEX && !viewModel.game.value.extraTableauUnlocked) {
                    onLockedTableauUnlockRequested?.invoke()
                    return
                }
                if (viewModel.handleSingleClickOnTableau(stackIndex, cardIndex)) {
                    onClickMoveSoundRequested?.invoke()
                }
            }

            StackType.FOUNDATION -> {
                // Explicit no-op for foundation taps.
            }

            else -> {
                // Tap on empty space → dismiss any glow
                viewModel.clearSingleClickGlow()
            }
        }
    }

    private fun handleDoubleTap(x: Float, y: Float) {
        val (type, stackIndex, _) = findStackAt(x, y)
        when (type) {
            // Double-tap is an explicit user action; always allow auto-move attempt.
            StackType.WASTE -> if (viewModel.tryAutoMoveWasteToFoundation()) {
                onClickMoveSoundRequested?.invoke()
            }
            StackType.TABLEAU -> if (viewModel.tryAutoMoveTableauTopToFoundation(stackIndex)) {
                onClickMoveSoundRequested?.invoke()
            }
            else -> handleTap(x, y)
        }
    }

    private fun cancelPendingSingleTap() {
        pendingSingleTap?.let { removeCallbacks(it) }
        pendingSingleTap = null
        lastTapTime = 0L
        lastTapStackType = null
        lastTapStackIndex = -1
        lastTapCardIndex = -1
    }

    private fun dispatchTapWithDoubleTapSupport(x: Float, y: Float) {
        val (type, stackIndex, cardIndex) = findStackAt(x, y)
        if (type != StackType.WASTE && type != StackType.TABLEAU) {
            cancelPendingSingleTap()
            handleTap(x, y)
            return
        }

        val now = SystemClock.elapsedRealtime()
        val isDoubleTap =
            pendingSingleTap != null &&
                (now - lastTapTime) <= doubleTapTimeout &&
                type == lastTapStackType &&
                stackIndex == lastTapStackIndex &&
                cardIndex == lastTapCardIndex

        if (isDoubleTap) {
            cancelPendingSingleTap()
            handleDoubleTap(x, y)
            return
        }

        cancelPendingSingleTap()
        lastTapTime = now
        lastTapStackType = type
        lastTapStackIndex = stackIndex
        lastTapCardIndex = cardIndex
        pendingSingleTap = Runnable {
            pendingSingleTap = null
            handleTap(x, y)
        }
        postDelayed(pendingSingleTap, doubleTapTimeout)
    }

    /**
     * Execute the glow-destination move chosen by the player.
     * The tryMove* methods already call clearSingleClickGlow() on success;
     * we also clear on failure so the player never gets stuck with stale glows.
     */
    private fun executeSingleClickGlowMove(
        glowState: SingleClickGlowState,
        dest: com.gpgamelab.justpatience.model.GlowDestination
    ) {
        val moved = when (glowState.sourceStackType) {
            StackType.WASTE -> when (dest.destStackType) {
                StackType.TABLEAU   -> { viewModel.tryMoveWasteToTableau(dest.destStackIndex); true }
                StackType.FOUNDATION -> viewModel.tryMoveWasteToFoundation(dest.destStackIndex)
                else -> false
            }
            StackType.TABLEAU -> when (dest.destStackType) {
                StackType.TABLEAU   -> viewModel.tryMoveTableauToTableau(
                    glowState.sourceStackIndex, glowState.sourceCardIndex, dest.destStackIndex
                )
                StackType.FOUNDATION -> viewModel.tryMoveTableauToFoundation(
                    glowState.sourceStackIndex, glowState.sourceCardIndex, dest.destStackIndex
                )
                else -> false
            }
            else -> false
        }
        // Safety-net: clear glow if the move somehow didn't clear it
        if (!moved) viewModel.clearSingleClickGlow()
        if (moved) onClickMoveSoundRequested?.invoke()
    }

    private fun clearDragState() {
        dragStackType = null
        dragStackIndex = -1
        dragCardIndex = -1
        isDragging = false
    }

    private fun getCardRect(
        type: StackType,
        stackIndex: Int,
        cardIndex: Int
    ): RectF {

        val startTableauY = getTableauStartY()

        return when (type) {

            StackType.STOCK -> {
                getStockRect()
            }

            StackType.WASTE -> {
                getWasteTopCardRect()
            }

            StackType.FOUNDATION -> {
                getFoundationRect(stackIndex)
            }

            StackType.TABLEAU -> {
                val x = columnX.getOrElse(stackIndex) { return RectF(0f, 0f, cardW, cardH) }
                var y = startTableauY

                val pile = viewModel.game.value.tableau.getOrNull(stackIndex)
                    ?: return RectF(x, y, x + cardW, y + cardH)
                val cards = pile.asList()

                for (i in 0 until cardIndex) {
                    y += if (cards[i].isFaceUp) cardW * 0.4f else cardW * 0.1f
                }

                RectF(x, y, x + cardW, y + cardH)
            }
        }
    }

    private fun tableauYOffsetForIndex(
        pile: TableauPile,
        index: Int
    ): Float {
        var y = 0f
        val cards = pile.asList()

        for (i in 0 until index) {
            val card = cards[i]
            y += if (card.isFaceUp) {
                cardW * 0.4f
            } else {
                cardW * 0.1f
            }
        }

        return y
    }

    // touch handling for drag & drop
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (magicWandSelectionMode) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> return true
                MotionEvent.ACTION_UP -> {
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)
                    if (dx < touchSlop && dy < touchSlop) {
                        handleTap(event.x, event.y)
                    }
                    return true
                }
                else -> return true
            }
        }

        // Any touch = player activity → reset the hint inactivity timer
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (::viewModel.isInitialized) viewModel.resetHintTimer()
        }

        // Block all input while animation is running
        if (isAnimationActive() || newGameDealActive) {
            return true  // Consume the event but don't process it
        }

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isDragging = false

                val (stackType, stackIndex, cardIndex) = findStackAt(event.x, event.y)

                if (stackType == StackType.TABLEAU) {
                    if (stackIndex == Game.LOCKED_TABLEAU_INDEX && !viewModel.game.value.extraTableauUnlocked) {
                        return true
                    }
                    val pile = viewModel.game.value.tableau[stackIndex]
                    val cards = pile.asList()

                    // Bounds check (defensive)
                    if (cardIndex < 0 || cardIndex >= cards.size) {
                        return false
                    }

                    // Find first face-up card at or below touched index
                    var dragStartIndex: Int? = null
                    for (i in cardIndex until cards.size) {
                        if (cards[i].isFaceUp) {
                            dragStartIndex = i
                            break
                        }
                    }

                    // 🚫 No face-up cards → no drag
                    if (dragStartIndex == null) {
                        return false
                    }

                    // ✅ Valid tableau drag
                    isDragging = true
                    dragX = event.x
                    dragY = event.y
                    dragStackType = StackType.TABLEAU
                    dragStackIndex = stackIndex
                    dragCardIndex = dragStartIndex

                    val startY = getTableauStartY()

                    val cardTopY =
                        startY + tableauYOffsetForIndex(pile, dragCardIndex)

                    dragOffsetX = event.x - columnX[stackIndex]
                    dragOffsetY = event.y - cardTopY

                    return true

                } else if (stackType == StackType.WASTE) {
                    val waste = viewModel.game.value.waste
                    if (!waste.isEmpty()) {

                        dragStackType = StackType.WASTE
                        dragStackIndex = 0
                        dragCardIndex = waste.size() - 1   // top card

                        val cardRect = getCardRect(StackType.WASTE, 0, dragCardIndex)
                        dragOffsetX = event.x - cardRect.left
                        dragOffsetY = event.y - cardRect.top
                    } else {
                        clearDragState()
                    }
                } else if (stackType == StackType.FOUNDATION && viewModel.allowFoundationToTableauDrag.value) {
                    val foundation = viewModel.game.value.foundations.getOrNull(stackIndex)
                    if (foundation != null && !foundation.isEmpty()) {
                        dragStackType = StackType.FOUNDATION
                        dragStackIndex = stackIndex
                        dragCardIndex = foundation.size() - 1 // top card only

                        val cardRect = getCardRect(StackType.FOUNDATION, stackIndex, dragCardIndex)
                        dragOffsetX = event.x - cardRect.left
                        dragOffsetY = event.y - cardRect.top
                    } else {
                        clearDragState()
                    }
                } else {
                    clearDragState()
                }


                return true
            }

            MotionEvent.ACTION_MOVE -> {
                dragX = event.x
                dragY = event.y

                if (!isDragging) {
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)

                    if (dx > touchSlop || dy > touchSlop) {
                        if (dragStackType == StackType.TABLEAU || dragStackType == StackType.WASTE || dragStackType == StackType.FOUNDATION) {
                            isDragging = true
                            postInvalidateOnAnimation()
                        } else {
                            clearDragState()
                        }
                    }
                } else {
                    postInvalidateOnAnimation()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {

                var moveSucceeded = false

                if (isDragging) {
                    // Drag-and-drop already provides motion feedback via the ghost card,
                    // so post-drop card-flight animation is skipped (animate = false).

                    // 1️⃣ FOUNDATION DROP (highest priority)
                    for (i in viewModel.game.value.foundations.indices) {
                        val rect = getFoundationRect(i)

                        if (rect.contains(event.x, event.y)) {
                            when (dragStackType) {

                                StackType.WASTE -> {
                                    moveSucceeded = viewModel.tryMoveWasteToFoundation(i, animate = false)
                                }

                                StackType.TABLEAU -> {
                                    moveSucceeded = viewModel.tryMoveTableauToFoundation(
                                        dragStackIndex,
                                        dragCardIndex,
                                        i,
                                        animate = false
                                    )
                                }

                                else -> {}
                            }
                            break
                        }
                    }

                    // 2️⃣ TABLEAU DROP (only if foundation failed)
                    if (!moveSucceeded) {
                        val startY = getTableauStartY()

                        columnX.forEachIndexed { index, colX ->
                            val rect = RectF(colX, startY, colX + cardW, height.toFloat())

                            if (rect.contains(event.x, event.y)) {

                                when (dragStackType) {

                                    StackType.WASTE -> {
                                        moveSucceeded = viewModel.tryMoveWasteToTableau(index, animate = false)
                                    }

                                    StackType.TABLEAU -> {
                                        moveSucceeded = viewModel.tryMoveTableauToTableau(
                                            dragStackIndex,
                                            dragCardIndex,
                                            index,
                                            animate = false
                                        )
                                    }

                                    StackType.FOUNDATION -> {
                                        moveSucceeded = viewModel.tryMoveFoundationToTableau(
                                            dragStackIndex,
                                            index
                                        )
                                    }

                                    else -> {
                                        moveSucceeded = false
                                    }
                                }

                                return@forEachIndexed
                            }
                        }
                    }

                }

                // 3️⃣ Tap fallback
                if (!moveSucceeded) {
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)

                    if (dx < touchSlop && dy < touchSlop) {
                        dispatchTapWithDoubleTapSupport(event.x, event.y)
                    }
                }

                // 4️⃣ ALWAYS clear drag state ONCE
                clearDragState()
                postInvalidateOnAnimation()
                return true
            }
        }
        return false
    }

    private fun findStackAt(x: Float, y: Float): Triple<StackType?, Int, Int> {
        // Check stock and waste first in landscape (they're in the left column)
        if (isLandscape) {
            if (getStockRect().contains(x, y)) {
                return Triple(StackType.STOCK, 0, -1)
            }
            if (getWasteHitRect().contains(x, y)) {
                return Triple(StackType.WASTE, 0, -1)
            }
        }

        // Check foundations (in left column in landscape, top row in portrait)
        for (i in viewModel.game.value.foundations.indices) {
            if (getFoundationRect(i).contains(x, y)) {
                return Triple(StackType.FOUNDATION, i, -1)
            }
        }

        // In portrait mode, check stock and waste in top row using their actual rects
        if (!isLandscape) {
            if (getStockRect().contains(x, y)) return Triple(StackType.STOCK, 0, -1)
            if (getWasteHitRect().contains(x, y)) return Triple(StackType.WASTE, 0, -1)
        }

        // Check tableau
        val startY = getTableauStartY()
        if (y >= startY) {
            val visibleColumns = min(columns, columnX.size)
            for (i in 0 until visibleColumns) {
                val cx = columnX[i]
                if (x in cx..(cx + cardW)) {

                    val pile = viewModel.game.value.tableau.getOrNull(i)
                    if (pile == null || pile.isEmpty()) {
                        return Triple(StackType.TABLEAU, i, 0)
                    }

                    val cards = pile.asList()

                    // Precompute all Y positions first
                    val positions = mutableListOf<Float>()
                    var curY = startY
                    for (c in cards) {
                        positions.add(curY)
                        curY += if (c.isFaceUp) cardW * 0.4f else cardW * 0.1f
                    }

                    // Iterate TOP → DOWN
                    for (index in cards.lastIndex downTo 0) {

                        val top = positions[index]

                        val bottom =
                            if (index == cards.lastIndex)
                                top + cardH // top card full height
                            else
                                top + (if (cards[index].isFaceUp) cardW * 0.4f else cardW * 0.1f)

                        if (y in top..bottom) {
                            return Triple(StackType.TABLEAU, i, index)
                        }
                    }

                    // If below all exposed areas, default to top card
                    return Triple(StackType.TABLEAU, i, cards.lastIndex)
                }
            }
        }

        return Triple(null, -1, -1)
    }


    private fun getStack(type: StackType, index: Int) = when (type) {
        StackType.STOCK -> viewModel.game.value.stock
        StackType.WASTE -> viewModel.game.value.waste
        StackType.TABLEAU -> viewModel.game.value.tableau.getOrNull(index)
        StackType.FOUNDATION -> viewModel.game.value.foundations.getOrNull(index)
    }

    private fun hitTestTableau(x: Float, y: Float): Int? {
        val startY = getTableauStartY()

        columnX.forEachIndexed { index, colX ->
            val rect = RectF(
                colX,
                startY,
                colX + cardW,
                height.toFloat()
            )
            if (rect.contains(x, y)) return index
        }
        return null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelPendingSingleTap()
        cancelNewGameDealAnimation()
        tabletopBitmap?.recycle()
        tabletopBitmap = null
    }
}
