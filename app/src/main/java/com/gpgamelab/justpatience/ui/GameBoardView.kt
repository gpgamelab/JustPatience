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
import com.gpgamelab.justpatience.model.Game
import com.gpgamelab.justpatience.model.HintDisplayState
import com.gpgamelab.justpatience.model.HintPhase
import com.gpgamelab.justpatience.model.SingleClickGlowState
import com.gpgamelab.justpatience.model.StackType
import com.gpgamelab.justpatience.model.TableauPile
import com.gpgamelab.justpatience.model.Verso
import com.gpgamelab.justpatience.util.UiScaleUtil
import com.gpgamelab.justpatience.ui.layout.BoardGroupLayoutEngine
import com.gpgamelab.justpatience.ui.layout.BoardOrientation
import com.gpgamelab.justpatience.ui.layout.GroupBox
import com.gpgamelab.justpatience.ui.layout.GroupId
import com.gpgamelab.justpatience.ui.layout.LayoutConfig
import com.gpgamelab.justpatience.ui.layout.RefToScreenTransform
import com.gpgamelab.justpatience.ui.layout.buildRefToScreenTransform
import com.gpgamelab.justpatience.ui.layout.containsTouch
import com.gpgamelab.justpatience.ui.layout.defaultCardSpec
import com.gpgamelab.justpatience.ui.layout.toScreenRectF
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import android.os.SystemClock
import com.gpgamelab.justpatience.util.BaselineResolutionScaleUtil
import com.gpgamelab.justpatience.util.DeviceAspectCategory
import java.util.Locale

private const val DEFAULT_STOCK_BACK_IMAGE_PATH = "drawable:card_back_crosshatch_001"
private const val NEW_GAME_DEAL_CARD_INTERVAL_MS = 70L
private const val MAX_VISIBLE_WASTE_CARDS = 3
private const val WASTE_COUNT_BADGE_MIN_VISIBLE_COUNT = 3

class GameBoardView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    lateinit var viewModel: GameViewModel
    lateinit var assetResolver: AssetResolver
    var onClickMoveSoundRequested: (() -> Unit)? = null
    var onDragDropResult: ((Boolean) -> Unit)? = null
    var onShuffleSoundRequested: ((() -> Unit) -> Unit)? = null
    var onLockedTableauUnlockRequested: (() -> Unit)? = null
    var onMagicWandTargetSelected: ((StackType, Int, Int) -> Unit)? = null

    /**
     * Called every time the Phase2 reference layout is recalculated (i.e. on every
     * [onSizeChanged] / orientation change).  The callback receives a snapshot of the
     * current screen-space [RectF] for each [GroupId] so that the host Activity can
     * position native Android views (controls, ad container, stats panel) to match
     * the reference-model geometry without needing to duplicate the layout math.
     *
     * Guaranteed to be called on the main thread.  The map is a new snapshot each time.
     */
    var onPhase2BoardLayoutReady: ((Map<GroupId, RectF>) -> Unit)? = null

    /**
     * Returns the current screen-space rectangle for the given [GroupId], or null if
     * the Phase2 layout has not been computed yet (before first [onSizeChanged]).
     */
    fun getPhase2GroupScreenRect(id: GroupId): RectF? = getPhase2GroupRect(id)

    private var currentDeviceScaleRatio = 1f
    // Device aspect category (SLIM, CLASSIC, BROAD, SQUARE) detected from physical display size.
    // Used for device-specific tuning and display in tester/about menus.
    private var currentAspectCategory: DeviceAspectCategory = DeviceAspectCategory.SLIM

    // Per-category Y trim applied as the very last step of board-start positioning.
    // Positive = move piles DOWN, negative = move piles UP.  All values are raw px.
    // (Currently all hardcoded to 0; kept for future category-specific tuning.)
    private var aspectCategoryPortraitTrimSlimPx   = 0f
    private var aspectCategoryPortraitTrimClassicPx = 0f
    private var aspectCategoryPortraitTrimBroadPx  = 0f
    private var aspectCategoryPortraitTrimSquarePx = 0f
    private var aspectCategoryLandscapeTrimSlimPx   = 0f
    private var aspectCategoryLandscapeTrimClassicPx = 0f
    private var aspectCategoryLandscapeTrimBroadPx  = 0f
    private var aspectCategoryLandscapeTrimSquarePx = 0f

    // Per-category X trim applied as the very last step of board-start positioning.
    // Positive = move piles RIGHT, negative = move piles LEFT. All values are raw px.
    // (Currently all hardcoded to 0; kept for future category-specific tuning.)
    private var aspectCategoryPortraitTrimXSlimPx   = 0f
    private var aspectCategoryPortraitTrimXClassicPx = 0f
    private var aspectCategoryPortraitTrimXBroadPx   = 0f
    private var aspectCategoryPortraitTrimXSquarePx  = 0f
    private var aspectCategoryLandscapeTrimXSlimPx   = 0f
    private var aspectCategoryLandscapeTrimXClassicPx = 0f
    private var aspectCategoryLandscapeTrimXBroadPx  = 0f
    private var aspectCategoryLandscapeTrimXSquarePx  = 0f

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
    private val cardWidthRatio = 1.4f
    private val cardHeightRatio = 1.7f
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
    private val LANDSCAPE_FOUNDATION_BASE_Y_SHIFT_RATIO = 0f
    private val LANDSCAPE_FOUNDATION_STEP_RATIO_MULTI_COLUMN = 1.02f
    private val PORTRAIT_TOP_ROW_EDGE_INSET_DP = 10f
    private val PORTRAIT_TOP_ROW_GROUP_GAP_RATIO = 0.28f
    private val PORTRAIT_FOUNDATION_STEP_PADDING_RATIO = 0.6f
    private val LANDSCAPE_TOP_ROW_INSET_CARD_HEIGHT_RATIO = 0.28f
    private val PORTRAIT_TOP_ROW_INSET_CARD_HEIGHT_RATIO = 0.18f
    private val TOP_ROW_INSET_MIN_DP = 10f
    private val LANDSCAPE_STOCK_EXTRA_GAP_RATIO = 0.25f

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

    // Landscape-only developer tuning for card-pile placement (all values are px).
    private var landscapeOverallOffsetXPx = 0f
    private var landscapeOverallOffsetYPx = 0f
    private var landscapeFoundationOffsetXPx = 0f
    private var landscapeFoundationOffsetYPx = 0f
    private var landscapeDrawWasteOffsetXPx = 0f
    private var landscapeDrawWasteOffsetYPx = 0f
    private var landscapeStockOffsetXPx = 0f
    private var landscapeStockOffsetYPx = 0f
    private var landscapeWasteOffsetXPx = 0f
    private var landscapeWasteOffsetYPx = 0f
    private var landscapeTableauOffsetXPx = 0f
    private var landscapeTableauOffsetYPx = 0f

    // Portrait-only developer tuning for card-pile placement (all values are px).
    private var portraitOverallOffsetXPx = 0f
    private var portraitOverallOffsetYPx = 0f
    private var portraitFoundationOffsetXPx = 0f
    private var portraitFoundationOffsetYPx = 0f
    private var portraitDrawWasteOffsetXPx = 0f
    private var portraitDrawWasteOffsetYPx = 0f
    private var portraitStockOffsetXPx = 0f
    private var portraitStockOffsetYPx = 0f
    private var portraitWasteOffsetXPx = 0f
    private var portraitWasteOffsetYPx = 0f
    private var portraitTableauOffsetXPx = 0f
    private var portraitTableauOffsetYPx = 0f
    private var lastPileLayoutDebugFingerprint: String? = null

    // Phase 2 bootstrap: reference-model layout cache (currently diagnostic/scaffolding only).
    private var phase2RefTransform: RefToScreenTransform? = null
    private var phase2GroupBoxesRef: Map<GroupId, GroupBox> = emptyMap()
    private var lastPhase2TopRowParityFingerprint: String? = null
    private var lastPhase2DrawWasteGuardFingerprint: String? = null
    private var lastPhase2DrawWasteSpacingFingerprint: String? = null

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

    fun getCurrentDeviceScaleRatio(): Float = currentDeviceScaleRatio
    fun getCurrentAspectCategory(): DeviceAspectCategory = currentAspectCategory

    /**
     * Unused stub: per-category Y trim values reserved for future category-specific tuning.
     * Currently all trim values remain 0 (no repositioning by device category).
     * The aspect-category system itself (SLIM/CLASSIC/BROAD/SQUARE) is still detected and
     * exported to tester/about menus for device identification.
     * @deprecated No longer called from GameActivity; kept as placeholder for future enhancement.
     */
    fun setAspectCategoryPileYTrimsDp(
        portraitSlimDp:    Float, portraitClassicDp: Float,
        portraitBroadDp:   Float, portraitSquareDp:  Float,
        landscapeSlimDp:   Float, landscapeClassicDp: Float,
        landscapeBroadDp:  Float, landscapeSquareDp:  Float
    ) {
        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        aspectCategoryPortraitTrimSlimPx    = portraitSlimDp    * density
        aspectCategoryPortraitTrimClassicPx = portraitClassicDp * density
        aspectCategoryPortraitTrimBroadPx   = portraitBroadDp   * density
        aspectCategoryPortraitTrimSquarePx  = portraitSquareDp  * density
        aspectCategoryLandscapeTrimSlimPx    = landscapeSlimDp    * density
        aspectCategoryLandscapeTrimClassicPx = landscapeClassicDp * density
        aspectCategoryLandscapeTrimBroadPx   = landscapeBroadDp   * density
        aspectCategoryLandscapeTrimSquarePx  = landscapeSquareDp  * density
        if (width > 0 && height > 0) recomputeBoardGeometry(width, height)
        invalidate()
    }

    /**
     * Unused stub: per-category X trim values reserved for future category-specific tuning.
     * Currently all trim values remain 0 (no repositioning by device category).
     * The aspect-category system itself (SLIM/CLASSIC/BROAD/SQUARE) is still detected and
     * exported to tester/about menus for device identification.
     * @deprecated No longer called from GameActivity; kept as placeholder for future enhancement.
     */
    fun setAspectCategoryPileXTrimsDp(
        portraitSlimDp:    Float, portraitClassicDp: Float,
        portraitBroadDp:   Float, portraitSquareDp:  Float,
        landscapeSlimDp:   Float, landscapeClassicDp: Float,
        landscapeBroadDp:  Float, landscapeSquareDp:  Float
    ) {
        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        aspectCategoryPortraitTrimXSlimPx    = portraitSlimDp    * density
        aspectCategoryPortraitTrimXClassicPx = portraitClassicDp * density
        aspectCategoryPortraitTrimXBroadPx   = portraitBroadDp   * density
        aspectCategoryPortraitTrimXSquarePx  = portraitSquareDp  * density
        aspectCategoryLandscapeTrimXSlimPx    = landscapeSlimDp    * density
        aspectCategoryLandscapeTrimXClassicPx = landscapeClassicDp * density
        aspectCategoryLandscapeTrimXBroadPx   = landscapeBroadDp   * density
        aspectCategoryLandscapeTrimXSquarePx  = landscapeSquareDp  * density
        if (width > 0 && height > 0) recomputeBoardGeometry(width, height)
        invalidate()
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
                            logPileLayoutDebugIfNeeded("mirroredLayoutChanged")
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

    fun setLandscapePileLayoutTuning(
        overallOffsetX: Float,
        overallOffsetY: Float,
        foundationOffsetX: Float,
        foundationOffsetY: Float,
        drawWasteOffsetX: Float,
        drawWasteOffsetY: Float,
        stockOffsetX: Float,
        stockOffsetY: Float,
        wasteOffsetX: Float,
        wasteOffsetY: Float,
        tableauOffsetX: Float,
        tableauOffsetY: Float
    ) {
        landscapeOverallOffsetXPx = overallOffsetX
        landscapeOverallOffsetYPx = overallOffsetY
        landscapeFoundationOffsetXPx = foundationOffsetX
        landscapeFoundationOffsetYPx = foundationOffsetY
        landscapeDrawWasteOffsetXPx = drawWasteOffsetX
        landscapeDrawWasteOffsetYPx = drawWasteOffsetY
        landscapeStockOffsetXPx = stockOffsetX
        landscapeStockOffsetYPx = stockOffsetY
        landscapeWasteOffsetXPx = wasteOffsetX
        landscapeWasteOffsetYPx = wasteOffsetY
        landscapeTableauOffsetXPx = tableauOffsetX
        landscapeTableauOffsetYPx = tableauOffsetY

        if (width > 0 && height > 0) {
            recomputeBoardGeometry(width, height)
            logPileLayoutDebugIfNeeded("setLandscapePileLayoutTuning", force = true)
        }
        invalidate()
    }

    fun setPortraitPileLayoutTuning(
        overallOffsetX: Float,
        overallOffsetY: Float,
        foundationOffsetX: Float,
        foundationOffsetY: Float,
        drawWasteOffsetX: Float,
        drawWasteOffsetY: Float,
        stockOffsetX: Float,
        stockOffsetY: Float,
        wasteOffsetX: Float,
        wasteOffsetY: Float,
        tableauOffsetX: Float,
        tableauOffsetY: Float
    ) {
        portraitOverallOffsetXPx = overallOffsetX
        portraitOverallOffsetYPx = overallOffsetY
        portraitFoundationOffsetXPx = foundationOffsetX
        portraitFoundationOffsetYPx = foundationOffsetY
        portraitDrawWasteOffsetXPx = drawWasteOffsetX
        portraitDrawWasteOffsetYPx = drawWasteOffsetY
        portraitStockOffsetXPx = stockOffsetX
        portraitStockOffsetYPx = stockOffsetY
        portraitWasteOffsetXPx = wasteOffsetX
        portraitWasteOffsetYPx = wasteOffsetY
        portraitTableauOffsetXPx = tableauOffsetX
        portraitTableauOffsetYPx = tableauOffsetY

        if (width > 0 && height > 0) {
            recomputeBoardGeometry(width, height)
            logPileLayoutDebugIfNeeded("setPortraitPileLayoutTuning", force = true)
        }
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
                val pile = viewModel.game.value.tableau.getOrNull(stackIndex) ?: return null
                getTableauCardRectVisual(stackIndex, pile, cardIndex)
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

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)

        if (width <= 0 || height <= 0) return

        if (::viewModel.isInitialized) {
            syncColumnsWithGame(viewModel.game.value)
        }

        recomputeBoardGeometry(width, height)
    }

    private fun recomputeBoardGeometry(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        currentDeviceScaleRatio = BaselineResolutionScaleUtil.calculateAverageRatio(width,height,
            baselinePortraitWidthPx  = 1600,
            baselinePortraitHeightPx = 2560).averageRatio
        val (displayWidthPx, displayHeightPx) = getPhysicalDisplaySizePx()
        currentAspectCategory = DeviceAspectCategory.classify(displayWidthPx, displayHeightPx)
        val displayRatio = minOf(displayWidthPx, displayHeightPx).toFloat() / maxOf(displayWidthPx, displayHeightPx).toFloat()
        val displayRatioText = String.format(Locale.US, "%.3f", displayRatio)
        Log.d(
            "GameBoardViewLayout",
            "aspectDetect board=${width}x${height} display=${displayWidthPx}x${displayHeightPx} ratio=$displayRatioText category=$currentAspectCategory"
        )
        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        // aspectFactors is still used for text-paint scaling; card dimensions use raw pixels
        // so that extreme-aspect compression never shrinks cards below what actually fits.
        val aspectFactors = UiScaleUtil.calculateBaselineScaleFactors(width / density, height / density)

        // Size cards from both width and height limits so narrow landscape layouts fit.
        // Do NOT pass the axis-compression/expansion factors here: on a phone in landscape
        // the GameBoardView itself has an extreme aspect ratio, which would make
        // verticalFactor = 0.5 and artificially halve the already-small height budget,
        // producing cards that are nearly invisible.
        val widthLimitedCardW = calculateWidthLimitedCardWidth(width)
        val heightLimitedCardW = calculateHeightLimitedCardWidth(height)

        cardW = min(widthLimitedCardW, heightLimitedCardW).coerceAtLeast(28f)

        // Deck/geometry fit pass: shrink until both horizontal and vertical budgets fit.
        val maxLayoutWidth = (width - (abs(BOARD_SHIFT_LEFT_PX) * 2f)).coerceAtLeast(1f)
        val maxLayoutHeight = (height * if (isLandscape) 0.98f else 0.94f).coerceAtLeast(1f)
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
        cardRadius = baseCardRadius * spacingScale * currentDeviceScaleRatio

        // Scale border and placeholder stroke widths with card size
        borderPaint.strokeWidth = (cardW * 0.025f).coerceIn(1.5f, 4f)
        placeholderPaint.strokeWidth = (cardW * 0.05f).coerceIn(2f, 7f)

        computeColumnX()
        computeBoardStartY(height)

        // Keep pile labels legible across phone/tablet sizes.
        pileLabelPaint.textSize = (cardW * 0.18f).coerceIn(10f, 40f)
        textPaint.textSize = ((min(width, height) * 0.10f) * aspectFactors.textCompression).coerceIn(10f, 60f)

        // Count badge scales with card size.
        pileCountBadgeRadius = (cardW * 0.16f).coerceIn(10f, 22f)
        pileCountBadgeInset = (cardW * 0.06f).coerceIn(4f, 10f)
        pileCountBadgeStrokePaint.strokeWidth = (cardW * 0.02f).coerceIn(1.5f, 3.5f)
        pileCountBadgeTextPaint.textSize = (pileCountBadgeRadius * 1.15f).coerceIn(12f, 24f)

        recomputePhase2ReferenceLayout(width, height)
        logPhase2TopRowParityIfNeeded()
        logPhase2DrawWasteSpacingIfNeeded()

        logPileLayoutDebugIfNeeded("recomputeBoardGeometry")
    }

    private fun recomputePhase2ReferenceLayout(viewWidth: Int, viewHeight: Int) {
        val orientation = if (isLandscape) BoardOrientation.LANDSCAPE else BoardOrientation.PORTRAIT
        val deckCount = if (::viewModel.isInitialized) {
            Game.normalizeDeckCount(viewModel.game.value.deckCount)
        } else {
            Game.DEFAULT_DECK_COUNT
        }
        val config = LayoutConfig(
            orientation = orientation,
            deckCount = deckCount,
            isMirrored = isMirrored
        )
        phase2RefTransform = buildRefToScreenTransform(
            screenW = viewWidth,
            screenH = viewHeight,
            orientation = orientation,
            density = resources.displayMetrics.density.coerceAtLeast(1f)
        )
        phase2GroupBoxesRef = BoardGroupLayoutEngine.computeGroupBoxes(
            config = config,
            cardSpec = defaultCardSpec(deckCount)
        )

        // Notify host Activity so it can position native views (controls, ads, stats)
        // using reference-model-derived screen rects rather than hard-coded dp values.
        val callback = onPhase2BoardLayoutReady
        if (callback != null) {
            val modelWidth = orientation.modelWidth
            val transform = phase2RefTransform!!
            val screenRects = phase2GroupBoxesRef.mapValues { (_, box) ->
                box.toScreenRectF(transform, modelWidth, isMirrored)
            }
            callback(screenRects)
        }
    }

    private fun getPhysicalDisplaySizePx(): Pair<Int, Int> {
        val attachedDisplay = display
        if (attachedDisplay != null) {
            val mode = attachedDisplay.mode
            if (mode != null && mode.physicalWidth > 0 && mode.physicalHeight > 0) {
                return mode.physicalWidth to mode.physicalHeight
            }

            @Suppress("DEPRECATION")
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            attachedDisplay.getRealMetrics(metrics)
            if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
                return metrics.widthPixels to metrics.heightPixels
            }
        }

        val fallbackMetrics = resources.displayMetrics
        return fallbackMetrics.widthPixels to fallbackMetrics.heightPixels
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

        startX += currentAspectCategoryXTrimPx()

        var x = startX
        repeat(columns) {
            columnX.add(x)
            x += cardW + cardPadding
        }
    }

    private fun getTableauGlobalOffsetX(): Float {
        return if (isLandscape) {
            landscapeOverallOffsetXPx + landscapeTableauOffsetXPx
        } else {
            portraitOverallOffsetXPx + portraitTableauOffsetXPx
        }
    }

    private fun getTableauColumnX(index: Int): Float? {
        return columnX.getOrNull(index)?.plus(getTableauGlobalOffsetX())
    }

    private fun getTableauGroupRectPhase2OrNull(): RectF? {
        val rect = getPhase2GroupRect(GroupId.TABLEAU) ?: return null
        if (rect.width() < cardW || rect.height() < cardH) return null
        return rect
    }

    private fun getTableauColumnXVisual(index: Int): Float? {
        val tableauRect = getTableauGroupRectPhase2OrNull() ?: return getTableauColumnX(index)
        if (index !in 0 until columns) return null

        val boardWidth = (cardW * columns) + (cardPadding * (columns - 1).coerceAtLeast(0))
        if (boardWidth > tableauRect.width() + 1f) return getTableauColumnX(index)

        val startX = tableauRect.left + ((tableauRect.width() - boardWidth) / 2f)
        return startX + index * (cardW + cardPadding)
    }

    private fun getTableauStartYVisual(): Float {
        val tableauRect = getTableauGroupRectPhase2OrNull() ?: return getTableauStartY()
        return tableauRect.top
    }

    private fun getTableauCardRectVisual(
        stackIndex: Int,
        pile: TableauPile,
        cardIndex: Int
    ): RectF? {
        val x = getTableauColumnXVisual(stackIndex) ?: return null
        var y = getTableauStartYVisual()
        val cards = pile.asList()
        val limit = if (cardIndex < 0) cards.size else cardIndex.coerceAtMost(cards.size)
        for (i in 0 until limit) {
            y += if (cards[i].isFaceUp) cardW * 0.4f else cardW * 0.1f
        }
        return RectF(x, y, x + cardW, y + cardH)
    }

    fun dumpPileLayoutDebug(reason: String = "manual") {
        logPileLayoutDebugIfNeeded(reason, force = true)
    }

    private fun logPileLayoutDebugIfNeeded(reason: String, force: Boolean = false) {
        if (width <= 0 || height <= 0 || cardW <= 0f || cardH <= 0f || columnX.isEmpty()) return

        val stockRect = getStockRect()
        val wasteRect = getWasteRect()
        val foundationRect = getFoundationRect(0)
        val tableauBaseX = columnX.firstOrNull() ?: return
        val tableauFinalX = getTableauColumnX(0) ?: return

        val debugSnapshot = buildString {
            append(if (isLandscape) "L" else "P")
            append('|').append(if (isMirrored) "M" else "C")
            append('|').append(stockRect.left.toInt())
            append('|').append(wasteRect.left.toInt())
            append('|').append(foundationRect.left.toInt())
            append('|').append(tableauFinalX.toInt())
            append('|').append(cardW.toInt())
            append('|').append(getFoundationCountForLayout())
            append('|').append(landscapeOverallOffsetXPx.toInt())
            append('|').append(portraitOverallOffsetXPx.toInt())
            append('|').append(landscapeDrawWasteOffsetXPx.toInt())
            append('|').append(portraitDrawWasteOffsetXPx.toInt())
            append('|').append(landscapeFoundationOffsetXPx.toInt())
            append('|').append(portraitFoundationOffsetXPx.toInt())
            append('|').append(landscapeTableauOffsetXPx.toInt())
            append('|').append(portraitTableauOffsetXPx.toInt())
        }

        if (!force && debugSnapshot == lastPileLayoutDebugFingerprint) return
        lastPileLayoutDebugFingerprint = debugSnapshot

        fun fmt(value: Float): String = String.format(Locale.US, "%.1f", value)

        val overallX = if (isLandscape) landscapeOverallOffsetXPx else portraitOverallOffsetXPx
        val drawWasteGroupX = if (isLandscape) landscapeDrawWasteOffsetXPx else portraitDrawWasteOffsetXPx
        val foundationGroupX = if (isLandscape) landscapeFoundationOffsetXPx else portraitFoundationOffsetXPx
        val tableauGroupX = if (isLandscape) landscapeTableauOffsetXPx else portraitTableauOffsetXPx
        val stockSpecificX = if (isLandscape) landscapeStockOffsetXPx else portraitStockOffsetXPx
        val wasteSpecificX = if (isLandscape) landscapeWasteOffsetXPx else portraitWasteOffsetXPx
        val stockAnchorX = if (!isLandscape) {
            portraitStockBaseX()
        } else {
            stockRect.left - overallX - drawWasteGroupX - stockSpecificX
        }

        val wasteAnchorX = if (!isLandscape) {
            portraitWasteBaseX()
        } else {
            wasteRect.left - overallX - drawWasteGroupX - wasteSpecificX
        }

        val foundationAnchorX = if (!isLandscape) {
            portraitFoundationColumnBaseX(0)
        } else {
            foundationRect.left - overallX - foundationGroupX
        }

        Log.d(
            "GameBoardViewLayout",
            "reason=$reason orient=${if (isLandscape) "landscape" else "portrait"} mirrored=$isMirrored cardW=${fmt(cardW)} board=${width}x${height}"
        )
        Log.i(
            "GameBoardViewLayout",
            "tableau0 base=${fmt(tableauBaseX)} overall=${fmt(overallX)} tableau=${fmt(tableauGroupX)} final=${fmt(tableauFinalX)}"
        )
        Log.i(
            "GameBoardViewLayout",
            "stock anchor=${fmt(stockAnchorX)} overall=${fmt(overallX)} drawWaste=${fmt(drawWasteGroupX)} specific=${fmt(stockSpecificX)} final=${fmt(stockRect.left)}"
        )
        Log.i(
            "GameBoardViewLayout",
            "waste anchor=${fmt(wasteAnchorX)} overall=${fmt(overallX)} drawWaste=${fmt(drawWasteGroupX)} specific=${fmt(wasteSpecificX)} final=${fmt(wasteRect.left)}"
        )
        Log.i(
            "GameBoardViewLayout",
            "foundation0 anchor=${fmt(foundationAnchorX)} overall=${fmt(overallX)} foundation=${fmt(foundationGroupX)} final=${fmt(foundationRect.left)}"
        )
    }

    private fun getTopRowY(): Float = boardStartY + cardPadding

    private fun getTopRowInsetTargetPx(): Float {
        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        val minInsetPx = TOP_ROW_INSET_MIN_DP * density
        val ratio = if (isLandscape) {
            LANDSCAPE_TOP_ROW_INSET_CARD_HEIGHT_RATIO
        } else {
            PORTRAIT_TOP_ROW_INSET_CARD_HEIGHT_RATIO
        }
        return minInsetPx + (cardH * ratio)
    }

     private fun applyPortraitPileYOffset(
         rawBaseY: Float,
         groupOffsetY: Float = 0f,
         specificOffsetY: Float = 0f
     ): Float = rawBaseY + portraitOverallOffsetYPx + groupOffsetY + specificOffsetY

     private fun applyLandscapePileYOffset(
         rawBaseY: Float,
         groupOffsetY: Float = 0f,
         specificOffsetY: Float = 0f
     ): Float = rawBaseY + landscapeOverallOffsetYPx + groupOffsetY + specificOffsetY

    private fun getPortraitTopPileBaseY(): Float = getTopRowY() + topPileVerticalShiftPx()

    private fun getPortraitFoundationBaseY(row: Int): Float {
        return getTopRowY() + row * (cardH + cardPadding * 0.85f)
    }

    private fun getPortraitTableauBaseY(): Float {
        val extraFoundationRow = if (getFoundationCountForLayout() > 4) {
            cardH + (cardPadding * 0.85f)
        } else {
            0f
        }
        // Stock and waste are now stacked vertically, so tableau starts below both
        val stockWasteStackHeight = (cardH * 2f) + cardPadding
        return getTopRowY() + stockWasteStackHeight + cardPadding + tableauOffset + extraFoundationRow
    }

    private fun getLandscapeFoundationBaseY(index: Int): Float {
        val foundationCount = getFoundationCountForLayout()
        val row = if (foundationCount > 4) index % 4 else index
        val baseY = getTopRowY() + cardH + cardPadding + tableauOffset -
            (cardH * LANDSCAPE_FOUNDATION_BASE_Y_SHIFT_RATIO)
        val stepY = if (foundationCount > 4) {
            cardH * LANDSCAPE_FOUNDATION_STEP_RATIO_MULTI_COLUMN
        } else {
            cardH * 0.88f
        }
        return if (foundationCount > 4) {
            baseY + (row * stepY)
        } else {
            val pileAdjustmentFractions = floatArrayOf(-0.65f, -0.40f, -0.15f, 0.15f)
            val pileAdjust = pileAdjustmentFractions.getOrElse(index) { 0f } * cardH
            baseY + (index * stepY) + pileAdjust
        }
    }

    private fun getLandscapeWasteBaseY(): Float {
        return getLandscapeFoundationBaseY(0) + topPileVerticalShiftPx()
    }

    private fun getLandscapeStockBaseY(): Float {
        return getLandscapeFoundationBaseY(1) +
            topPileVerticalShiftPx() +
            (cardH * LANDSCAPE_WASTE_EXTRA_SHIFT_RATIO) +
            (cardH * LANDSCAPE_STOCK_EXTRA_GAP_RATIO)
    }

    private fun portraitTopRowEdgeInsetPx(): Float {
        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        return max(cardPadding, PORTRAIT_TOP_ROW_EDGE_INSET_DP * density)
    }

    private fun portraitTopRowGroupGapPx(): Float {
        return max(cardPadding * 2f, cardW * PORTRAIT_TOP_ROW_GROUP_GAP_RATIO)
    }

    private fun portraitFoundationStepPx(): Float {
        return cardW + (cardPadding * PORTRAIT_FOUNDATION_STEP_PADDING_RATIO)
    }

    private fun portraitTopRowFoundationCols(): Int {
        return min(getFoundationCountForLayout(), 4).coerceAtLeast(1)
    }

    private fun portraitTopRowStockWasteBlockWidth(): Float {
        // Stock and waste are now stacked vertically, so width is just one card + fan spread
        val wasteFanSpread = getWasteFanOffsetX() * (MAX_VISIBLE_WASTE_CARDS - 1)
        return cardW + wasteFanSpread
    }

    private fun portraitTopRowFoundationBlockWidth(): Float {
        val foundationCols = portraitTopRowFoundationCols()
        return cardW + ((foundationCols - 1) * portraitFoundationStepPx())
    }

    private fun portraitTopRowContainerWidth(): Float {
        return portraitTopRowStockWasteBlockWidth() +
            portraitTopRowGroupGapPx() +
            portraitTopRowFoundationBlockWidth()
    }

    private fun portraitTopRowContainerLeft(): Float {
        val edgeInset = portraitTopRowEdgeInsetPx()
        return if (isMirrored) {
            width - edgeInset - portraitTopRowContainerWidth()
        } else {
            edgeInset
        }
    }

    private fun portraitTopRowDrawWasteBaseX(): Float {
        return if (isMirrored) {
            portraitTopRowContainerLeft() + portraitTopRowFoundationBlockWidth() + portraitTopRowGroupGapPx()
        } else {
            portraitTopRowContainerLeft()
        }
    }

    private fun portraitTopRowFoundationBaseX(): Float {
        return if (isMirrored) {
            portraitTopRowContainerLeft()
        } else {
            portraitTopRowContainerLeft() + portraitTopRowStockWasteBlockWidth() + portraitTopRowGroupGapPx()
        }
    }

    private fun portraitStockBaseX(): Float {
        return portraitTopRowDrawWasteBaseX()
    }

    private fun portraitWasteBaseX(): Float {
        return portraitTopRowDrawWasteBaseX()
    }

    private fun portraitFoundationColumnBaseX(column: Int): Float {
        return portraitTopRowFoundationBaseX() + (column * portraitFoundationStepPx())
    }

    private fun getTableauStartY(): Float {
        if (!isLandscape) {
            return applyPortraitPileYOffset(
                rawBaseY = getPortraitTableauBaseY(),
                specificOffsetY = portraitTableauOffsetYPx
            )
        }

        return applyLandscapePileYOffset(
            rawBaseY = getLandscapeFoundationBaseY(0),
            specificOffsetY = landscapeTableauOffsetYPx
        )
    }

    private fun getPhase2GroupRect(id: GroupId): RectF? {
        val transform = phase2RefTransform ?: return null
        val box = phase2GroupBoxesRef[id] ?: return null
        val modelWidth = if (isLandscape) {
            BoardOrientation.LANDSCAPE.modelWidth
        } else {
            BoardOrientation.PORTRAIT.modelWidth
        }
        return box.toScreenRectF(transform, modelWidth, isMirrored)
    }

    private fun getWasteRectPhase2OrNull(): RectF? {
        val drawWasteRect = getPhase2GroupRect(GroupId.DRAW_WASTE) ?: return null
        if (cardW <= 0f || cardH <= 0f) return null
        val x = if (isMirrored) drawWasteRect.right - cardW else drawWasteRect.left
        val y = drawWasteRect.top
        return RectF(x, y, x + cardW, y + cardH)
    }

    private fun getStockRectPhase2OrNull(): RectF? {
        val wasteRect = getWasteRectPhase2OrNull() ?: return null
        val drawWasteRect = getPhase2GroupRect(GroupId.DRAW_WASTE) ?: return null
        val x = wasteRect.left
        // Keep stock directly below waste in both orientations.
        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        val stackGap = max(cardPadding, density * 2f)
        val y = wasteRect.bottom + stackGap
        val stockRect = RectF(x, y, x + cardW, y + cardH)

        // Guardrail for small/short screens: never collapse stock into waste to keep it "inside" the box.
        // If the DRAW_WASTE group is too short for current card metrics, keep non-overlap and log once.
        val overflow = stockRect.bottom - drawWasteRect.bottom
        if (overflow > 1f) {
            val fingerprint = buildString {
                append(if (isLandscape) 'L' else 'P')
                append('|').append(if (isMirrored) 'M' else 'C')
                append('|').append(currentAspectCategory.name)
                append('|').append(cardW.toInt()).append('x').append(cardH.toInt())
                append('|').append(drawWasteRect.height().toInt())
                append('|').append(overflow.toInt())
            }
            if (fingerprint != lastPhase2DrawWasteGuardFingerprint) {
                lastPhase2DrawWasteGuardFingerprint = fingerprint
                Log.w(
                    "GameBoardViewLayout",
                    "phase2DrawWasteGuard overflow=${overflow.toInt()} orient=${if (isLandscape) "landscape" else "portrait"} mirrored=$isMirrored aspect=$currentAspectCategory drawWasteH=${drawWasteRect.height().toInt()} card=${cardW.toInt()}x${cardH.toInt()}"
                )
            }
        }

        return stockRect
    }

    private fun getFoundationRectPhase2OrNull(index: Int): RectF? {
        val foundationCount = getFoundationCountForLayout()
        if (index !in 0 until foundationCount) return null
        if (cardW <= 0f || cardH <= 0f) return null

        val foundationRect = getPhase2GroupRect(GroupId.FOUNDATION) ?: return null
        val gap = cardPadding * 0.6f

        return if (isLandscape) {
            val row = if (foundationCount > 4) index % 4 else index
            val col = if (foundationCount > 4) index / 4 else 0
            val totalRows = 4
            val totalHeight = (totalRows * cardH) + ((totalRows - 1) * gap)
            // Keep foundations anchored at the group top when card metrics outgrow the group box.
            val centeredOffsetY = ((foundationRect.height() - totalHeight) / 2f).coerceAtLeast(0f)
            val startY = foundationRect.top + centeredOffsetY
            val y = startY + row * (cardH + gap)

            // Landscape 1-deck uses the column closest to tableau.
            val closestColumnX = if (isMirrored) foundationRect.right - cardW else foundationRect.left
            val x = if (isMirrored) {
                closestColumnX - col * (cardW + gap)
            } else {
                closestColumnX + col * (cardW + gap)
            }
            RectF(x, y, x + cardW, y + cardH)
        } else {
            val row = if (foundationCount > 4) index / 4 else 1
            val col = if (foundationCount > 4) index % 4 else index
            val totalCols = 4
            val totalRows = 2
            val totalWidth = (totalCols * cardW) + ((totalCols - 1) * gap)
            val totalHeight = (totalRows * cardH) + ((totalRows - 1) * gap)
            val centeredOffsetX = ((foundationRect.width() - totalWidth) / 2f).coerceAtLeast(0f)
            val centeredOffsetY = ((foundationRect.height() - totalHeight) / 2f).coerceAtLeast(0f)
            val startX = foundationRect.left + centeredOffsetX
            val startY = foundationRect.top + centeredOffsetY
            val x = startX + col * (cardW + gap)
            val y = startY + row * (cardH + gap)
            RectF(x, y, x + cardW, y + cardH)
        }
    }

    private fun getStockRectLegacy(): RectF {
        if (!isLandscape) {
            val yWithOffset = applyPortraitPileYOffset(
                // Portrait matches landscape ordering: waste on top, stock below.
                rawBaseY = getPortraitTopPileBaseY() + cardH + cardPadding,
                groupOffsetY = portraitDrawWasteOffsetYPx,
                specificOffsetY = portraitStockOffsetYPx
            )
            val x = portraitStockBaseX() +
                portraitOverallOffsetXPx + portraitDrawWasteOffsetXPx + portraitStockOffsetXPx
            return RectF(x, yWithOffset, x + cardW, yWithOffset + cardH)
        }

        val y = applyLandscapePileYOffset(
            rawBaseY = getLandscapeStockBaseY(),
            groupOffsetY = landscapeDrawWasteOffsetYPx,
            specificOffsetY = landscapeStockOffsetYPx
        )
        val landscapeOuterGap = landscapeOuterGap()
        val baseX = if (isMirrored) {
            // Mirrored: stock to the RIGHT of tableau
            (columnX.lastOrNull()?.let { it + cardW } ?: (width - cardPadding)) + landscapeOuterGap
        } else {
            // Classic: stock to the LEFT of tableau
            (columnX.firstOrNull() ?: cardPadding) - cardW - landscapeOuterGap
        }
        val x = baseX + landscapeOverallOffsetXPx + landscapeDrawWasteOffsetXPx + landscapeStockOffsetXPx
        return RectF(x, y, x + cardW, y + cardH)
    }

      private fun getStockRect(): RectF {
          return getStockRectPhase2OrNull() ?: getStockRectLegacy()
      }

    private fun getWasteRectLegacy(): RectF {
          if (!isLandscape) {
             val yWithOffset = applyPortraitPileYOffset(
                 rawBaseY = getPortraitTopPileBaseY(),
                 groupOffsetY = portraitDrawWasteOffsetYPx,
                 specificOffsetY = portraitWasteOffsetYPx
             )
             val x = portraitWasteBaseX() +
                 portraitOverallOffsetXPx + portraitDrawWasteOffsetXPx + portraitWasteOffsetXPx
             return RectF(x, yWithOffset, x + cardW, yWithOffset + cardH)
         }

        val y = applyLandscapePileYOffset(
            rawBaseY = getLandscapeWasteBaseY(),
            groupOffsetY = landscapeDrawWasteOffsetYPx,
            specificOffsetY = landscapeWasteOffsetYPx
        )
        val landscapeOuterGap = landscapeOuterGap()
        val baseX = if (isMirrored) {
            // Mirrored: waste to the RIGHT of tableau (same X column as mirrored stock)
            (columnX.lastOrNull()?.let { it + cardW } ?: (width - cardPadding)) + landscapeOuterGap
        } else {
            // Classic: waste to the LEFT of tableau (same X column as classic stock)
            (columnX.firstOrNull() ?: cardPadding) - cardW - landscapeOuterGap
        }
        val x = baseX + landscapeOverallOffsetXPx + landscapeDrawWasteOffsetXPx + landscapeWasteOffsetXPx
        return RectF(x, y, x + cardW, y + cardH)
    }

       private fun getWasteRect(): RectF {
           return getWasteRectPhase2OrNull() ?: getWasteRectLegacy()
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

    private fun getFoundationRectLegacy(index: Int): RectF {
          if (!isLandscape) {
             val foundationCount = getFoundationCountForLayout()

             val row = if (foundationCount > 4) index / 4 else 0
             val col = if (foundationCount > 4) index % 4 else index
             val x = portraitFoundationColumnBaseX(col) +
                 portraitOverallOffsetXPx + portraitFoundationOffsetXPx
             val y = applyPortraitPileYOffset(
                 rawBaseY = getPortraitFoundationBaseY(row),
                 specificOffsetY = portraitFoundationOffsetYPx
             )
             return RectF(x, y, x + cardW, y + cardH)
         }

        // Landscape: foundations are outside the tableau columns
        val landscapeOuterGap = landscapeOuterGap()
        val foundationSideBaseX = if (isMirrored) {
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
        val foundationColumnBaseX = if (isMirrored) {
            foundationSideBaseX - (col * columnStep)
        } else {
            foundationSideBaseX + (col * columnStep)
        }
        val x = foundationColumnBaseX + landscapeOverallOffsetXPx + landscapeFoundationOffsetXPx

        val y = applyLandscapePileYOffset(
            rawBaseY = getLandscapeFoundationBaseY(index),
            specificOffsetY = landscapeFoundationOffsetYPx
        )

        return RectF(x, y, x + cardW, y + cardH)
    }

     private fun getFoundationRect(index: Int): RectF {
         return getFoundationRectPhase2OrNull(index) ?: getFoundationRectLegacy(index)
    }

    private fun logPhase2TopRowParityIfNeeded() {
        val phase2Stock = getStockRectPhase2OrNull() ?: return
        val phase2Waste = getWasteRectPhase2OrNull() ?: return
        val phase2Foundation = getFoundationRectPhase2OrNull(0) ?: return

        val legacyStock = getStockRectLegacy()
        val legacyWaste = getWasteRectLegacy()
        val legacyFoundation = getFoundationRectLegacy(0)

        val fingerprint = buildString {
            append(if (isLandscape) 'L' else 'P')
            append('|').append(if (isMirrored) 'M' else 'C')
            append('|').append(cardW.toInt())
            append('|').append(cardH.toInt())
            append('|').append(legacyStock.left.toInt()).append(',').append(phase2Stock.left.toInt())
            append('|').append(legacyWaste.left.toInt()).append(',').append(phase2Waste.left.toInt())
            append('|').append(legacyFoundation.left.toInt()).append(',').append(phase2Foundation.left.toInt())
            append('|').append(legacyStock.top.toInt()).append(',').append(phase2Stock.top.toInt())
            append('|').append(legacyWaste.top.toInt()).append(',').append(phase2Waste.top.toInt())
            append('|').append(legacyFoundation.top.toInt()).append(',').append(phase2Foundation.top.toInt())
        }

        if (fingerprint == lastPhase2TopRowParityFingerprint) return
        lastPhase2TopRowParityFingerprint = fingerprint

        fun delta(a: RectF, b: RectF): String {
            val dx = String.format(Locale.US, "%.1f", b.left - a.left)
            val dy = String.format(Locale.US, "%.1f", b.top - a.top)
            val dw = String.format(Locale.US, "%.1f", b.width() - a.width())
            val dh = String.format(Locale.US, "%.1f", b.height() - a.height())
            return "dx=$dx dy=$dy dw=$dw dh=$dh"
        }

        Log.d(
            "GameBoardViewLayout",
            "phase2Parity orient=${if (isLandscape) "landscape" else "portrait"} mirrored=$isMirrored stock(${delta(legacyStock, phase2Stock)}) waste(${delta(legacyWaste, phase2Waste)}) foundation0(${delta(legacyFoundation, phase2Foundation)})"
        )
    }

    private fun logPhase2DrawWasteSpacingIfNeeded() {
        val stock = getStockRectPhase2OrNull() ?: return
        val waste = getWasteRectPhase2OrNull() ?: return
        val drawWasteGroup = getPhase2GroupRect(GroupId.DRAW_WASTE) ?: return
        val gap = stock.top - waste.bottom
        val stockOverflow = stock.bottom - drawWasteGroup.bottom

        val fingerprint = buildString {
            append(if (isLandscape) 'L' else 'P')
            append('|').append(if (isMirrored) 'M' else 'C')
            append('|').append(currentAspectCategory.name)
            append('|').append(gap.toInt())
            append('|').append(stockOverflow.toInt())
            append('|').append(cardW.toInt()).append('x').append(cardH.toInt())
        }
        if (fingerprint == lastPhase2DrawWasteSpacingFingerprint) return
        lastPhase2DrawWasteSpacingFingerprint = fingerprint

        if (gap < 1f) {
            Log.w(
                "GameBoardViewLayout",
                "phase2DrawWasteSpacing gapTooSmall gap=${gap.toInt()} overflow=${stockOverflow.toInt()} orient=${if (isLandscape) "landscape" else "portrait"} mirrored=$isMirrored aspect=$currentAspectCategory"
            )
        } else {
            Log.d(
                "GameBoardViewLayout",
                "phase2DrawWasteSpacing gap=${gap.toInt()} overflow=${stockOverflow.toInt()} orient=${if (isLandscape) "landscape" else "portrait"} mirrored=$isMirrored aspect=$currentAspectCategory"
            )
        }
    }

    private fun getLandscapeFoundationTop(index: Int): Float {
        return getLandscapeFoundationBaseY(index)
    }

    private fun currentAspectCategoryYTrimPx(): Float {
        return if (isLandscape) {
            when (currentAspectCategory) {
                // SLIM_COMPACT uses the same trim as SLIM (all values currently 0; kept for future tuning)
                DeviceAspectCategory.SLIM_COMPACT,
                DeviceAspectCategory.SLIM    -> aspectCategoryLandscapeTrimSlimPx
                DeviceAspectCategory.CLASSIC -> aspectCategoryLandscapeTrimClassicPx
                DeviceAspectCategory.BROAD   -> aspectCategoryLandscapeTrimBroadPx
                DeviceAspectCategory.SQUARE  -> aspectCategoryLandscapeTrimSquarePx
            }
        } else {
            when (currentAspectCategory) {
                DeviceAspectCategory.SLIM_COMPACT,
                DeviceAspectCategory.SLIM    -> aspectCategoryPortraitTrimSlimPx
                DeviceAspectCategory.CLASSIC -> aspectCategoryPortraitTrimClassicPx
                DeviceAspectCategory.BROAD   -> aspectCategoryPortraitTrimBroadPx
                DeviceAspectCategory.SQUARE  -> aspectCategoryPortraitTrimSquarePx
            }
        }
    }

    private fun currentAspectCategoryXTrimPx(): Float {
        return if (isLandscape) {
            when (currentAspectCategory) {
                DeviceAspectCategory.SLIM_COMPACT,
                DeviceAspectCategory.SLIM    -> aspectCategoryLandscapeTrimXSlimPx
                DeviceAspectCategory.CLASSIC -> aspectCategoryLandscapeTrimXClassicPx
                DeviceAspectCategory.BROAD   -> aspectCategoryLandscapeTrimXBroadPx
                DeviceAspectCategory.SQUARE  -> aspectCategoryLandscapeTrimXSquarePx
            }
        } else {
            when (currentAspectCategory) {
                DeviceAspectCategory.SLIM_COMPACT,
                DeviceAspectCategory.SLIM    -> aspectCategoryPortraitTrimXSlimPx
                DeviceAspectCategory.CLASSIC -> aspectCategoryPortraitTrimXClassicPx
                DeviceAspectCategory.BROAD   -> aspectCategoryPortraitTrimXBroadPx
                DeviceAspectCategory.SQUARE  -> aspectCategoryPortraitTrimXSquarePx
            }
        }
    }

    private fun computeBoardStartY(viewHeight: Int) {
        val estimatedBoardHeight = estimateBoardHeight(cardW)
        val desiredBoardStartY = getTopRowInsetTargetPx() - cardPadding + currentAspectCategoryYTrimPx()
        val maxBoardStartY = (viewHeight - estimatedBoardHeight).coerceAtLeast(0f)
        boardStartY = desiredBoardStartY.coerceIn(0f, maxBoardStartY)
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
                val pile = viewModel.game.value.tableau.getOrNull(move.destStackIndex) ?: return null
                getTableauCardRectVisual(move.destStackIndex, pile, -1)
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
                val pile = viewModel.game.value.tableau.getOrNull(dest.destStackIndex) ?: return null
                getTableauCardRectVisual(dest.destStackIndex, pile, -1)
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
        val startY = getTableauStartYVisual()
        viewModel.game.value.tableau.forEachIndexed { colIdx, pile ->
            val x = getTableauColumnXVisual(colIdx) ?: return@forEachIndexed
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
                    GameViewModel.DrawResult.RECYCLE_SHUFFLE -> {
                        val commitRecycle = {
                            viewModel.completeRecycleAfterShuffleSound()
                            Unit
                        }
                        val shuffleHandler = onShuffleSoundRequested
                        if (shuffleHandler != null) {
                            shuffleHandler.invoke(commitRecycle)
                        } else {
                            // Fallback for safety when no shuffle callback is wired.
                            commitRecycle()
                        }
                    }
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
        return when (type) {

            StackType.STOCK -> getStockRect()

            StackType.WASTE -> getWasteTopCardRect()

            StackType.FOUNDATION -> getFoundationRect(stackIndex)

            StackType.TABLEAU -> {
                val pile = viewModel.game.value.tableau.getOrNull(stackIndex)
                // Prefer Phase2/visual coordinates so drag-start and render match exactly.
                getTableauCardRectVisual(stackIndex, pile ?: return RectF(0f, 0f, cardW, cardH), cardIndex)
                    ?: run {
                        // Fallback to legacy if Phase2 not yet ready.
                        val x = getTableauColumnX(stackIndex) ?: return RectF(0f, 0f, cardW, cardH)
                        var y = getTableauStartY()
                        val cards = pile.asList()
                        for (i in 0 until cardIndex) {
                            y += if (cards[i].isFaceUp) cardW * 0.4f else cardW * 0.1f
                        }
                        RectF(x, y, x + cardW, y + cardH)
                    }
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

                    // Use Phase2/visual card rect so drag offsets stay aligned with what's drawn.
                    val cardRect = getTableauCardRectVisual(stackIndex, pile, dragCardIndex)
                        ?: run {
                            // Fallback to legacy if Phase2 not ready yet
                            val startY = getTableauStartY()
                            val cardTopY = startY + tableauYOffsetForIndex(pile, dragCardIndex)
                            val colX = getTableauColumnX(stackIndex) ?: return false
                            dragOffsetX = event.x - colX
                            dragOffsetY = event.y - cardTopY
                            return true
                        }
                    dragOffsetX = event.x - cardRect.left
                    dragOffsetY = event.y - cardRect.top

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

                val hadDragGesture = isDragging
                var moveSucceeded = false

                if (isDragging) {
                    // Drag-and-drop already provides motion feedback via the ghost card,
                    // so post-drop card-flight animation is skipped (animate = false).

                    // 1️⃣ FOUNDATION DROP (highest priority)
                    for (i in viewModel.game.value.foundations.indices) {
                        val rect = getFoundationRect(i)

                        if (containsTouch(rect, event.x, event.y)) {
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
                        val dropTableauIndex = findTableauDropTargetStrict(event.x, event.y)
                        if (dropTableauIndex != null) {
                            when (dragStackType) {

                                StackType.WASTE -> {
                                    moveSucceeded = viewModel.tryMoveWasteToTableau(dropTableauIndex, animate = false)
                                }

                                StackType.TABLEAU -> {
                                    moveSucceeded = viewModel.tryMoveTableauToTableau(
                                        dragStackIndex,
                                        dragCardIndex,
                                        dropTableauIndex,
                                        animate = false
                                    )
                                }

                                StackType.FOUNDATION -> {
                                    moveSucceeded = viewModel.tryMoveFoundationToTableau(
                                        dragStackIndex,
                                        dropTableauIndex
                                    )
                                }

                                else -> {
                                    moveSucceeded = false
                                }
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

                if (hadDragGesture) {
                    onDragDropResult?.invoke(moveSucceeded)
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
            if (containsTouch(getStockRect(), x, y)) {
                return Triple(StackType.STOCK, 0, -1)
            }
            // getWasteHitRect() already uses Phase2-first via getWasteRect(), and also
            // covers the waste fan spread (union of base rect + top card rect).
            if (containsTouch(getWasteHitRect(), x, y)) {
                return Triple(StackType.WASTE, 0, -1)
            }
        }

        // Check foundations (in left column in landscape, top row in portrait)
        for (i in viewModel.game.value.foundations.indices) {
            if (containsTouch(getFoundationRect(i), x, y)) {
                return Triple(StackType.FOUNDATION, i, -1)
            }
        }

        // In portrait mode, check top-row pile hit targets using their actual rects.
        if (!isLandscape) {
            if (containsTouch(getWasteHitRect(), x, y)) return Triple(StackType.WASTE, 0, -1)
            if (containsTouch(getStockRect(), x, y)) return Triple(StackType.STOCK, 0, -1)
        }

        // Strict tableau hit-testing: only actual card/placeholder rectangles are tappable.
        findTableauHitStrict(x, y)?.let { (stackIndex, cardIndex) ->
            return Triple(StackType.TABLEAU, stackIndex, cardIndex)
        }

        return Triple(null, -1, -1)
    }

    private fun findTableauHitStrict(x: Float, y: Float): Pair<Int, Int>? {
        val visibleColumns = min(columns, columnX.size)
        for (stackIndex in 0 until visibleColumns) {
            val pile = viewModel.game.value.tableau.getOrNull(stackIndex) ?: continue

            if (pile.isEmpty()) {
                val placeholderRect = getTableauCardRectVisual(stackIndex, pile, 0) ?: continue
                if (containsTouch(placeholderRect, x, y)) return stackIndex to 0
                continue
            }

            val cards = pile.asList()
            val topRect = getTableauCardRectVisual(stackIndex, pile, cards.lastIndex) ?: continue
            if (x < topRect.left || x >= topRect.right) continue

            for (index in cards.lastIndex downTo 0) {
                val rect = getTableauCardRectVisual(stackIndex, pile, index) ?: continue
                val top = rect.top
                val bottom = if (index == cards.lastIndex) {
                    rect.bottom
                } else {
                    getTableauCardRectVisual(stackIndex, pile, index + 1)?.top ?: rect.bottom
                }
                if (y >= top && y < bottom) return stackIndex to index
            }
        }
        return null
    }

    private fun findTableauDropTargetStrict(x: Float, y: Float): Int? {
        return findTableauHitStrict(x, y)?.first
    }


    private fun getStack(type: StackType, index: Int) = when (type) {
        StackType.STOCK -> viewModel.game.value.stock
        StackType.WASTE -> viewModel.game.value.waste
        StackType.TABLEAU -> viewModel.game.value.tableau.getOrNull(index)
        StackType.FOUNDATION -> viewModel.game.value.foundations.getOrNull(index)
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelPendingSingleTap()
        cancelNewGameDealAnimation()
        tabletopBitmap?.recycle()
        tabletopBitmap = null
    }
}
