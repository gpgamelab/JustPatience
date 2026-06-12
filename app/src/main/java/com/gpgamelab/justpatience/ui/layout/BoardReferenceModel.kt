package com.gpgamelab.justpatience.ui.layout

import android.graphics.RectF
import com.gpgamelab.justpatience.model.CardSuit

/**
 * Phase 0 – Reference Layout Model Infrastructure
 * ─────────────────────────────────────────────────────────────────────────────
 * All game elements are positioned inside a fixed-size "reference canvas":
 *
 *   Portrait  → 1152 × 1536  (width × height, in reference pixels)
 *   Landscape → 1536 × 1152
 *
 * Upper-left is (0, 0); lower-right is (modelWidth-1, modelHeight-1).
 * Both classic and mirrored layouts share the same origin convention.
 * Mirroring is a pure coordinate transform applied at draw/hit-test time.
 *
 * Reference → screen transform
 * ─────────────────────────────
 *   screenX = referenceX × (screenWidthPx  / modelWidth)
 *   screenY = referenceY × (screenHeightPx / modelHeight)
 *
 * Mirroring a GroupBox in reference space (then scale normally):
 *   mirroredLeft = modelWidth - left - width
 *
 * Touch hit testing
 * ─────────────────
 * Every touch test must be a strict containment check inside a card rectangle
 * (or a deliberately smaller sub-rectangle for multi-card tableau drag handles).
 * No fuzzy expansion beyond the card boundary is permitted.
 *
 * Ad safety
 * ─────────
 * A 3 dp buffer zone separates the ADS GroupBox from every other GroupBox.
 * [RefToScreenTransform.adSafetyPx] carries this distance in screen pixels.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

/** Width of the portrait reference canvas in reference pixels. */
const val PORTRAIT_MODEL_W  = 1152
/** Height of the portrait reference canvas in reference pixels. */
const val PORTRAIT_MODEL_H  = 1536

/** Width of the landscape reference canvas in reference pixels. */
const val LANDSCAPE_MODEL_W = 1536
/** Height of the landscape reference canvas in reference pixels. */
const val LANDSCAPE_MODEL_H = 1152

/**
 * Reserved foundation suit assignment order (index 0-based).
 *
 * 1-deck slots → indices 0..3  : Spades, Hearts, Diamonds, Clubs
 * 2-deck slots → indices 4..7  : Spades, Hearts, Diamonds, Clubs (second set)
 *
 * When auto-routing a card to a foundation, always target the first slot whose
 * reserved suit matches the card's suit and whose top card can accept it
 * (i.e. walk the list in order and pick the first eligible slot).
 */
val FOUNDATION_SUIT_ORDER: List<CardSuit> = listOf(
    CardSuit.SPADES,   CardSuit.HEARTS,   CardSuit.DIAMONDS, CardSuit.CLUBS,
    CardSuit.SPADES,   CardSuit.HEARTS,   CardSuit.DIAMONDS, CardSuit.CLUBS
)

/** Returns the reserved suit for a foundation pile at [index] (0-based). */
fun suitForFoundationIndex(index: Int): CardSuit =
    FOUNDATION_SUIT_ORDER[index.coerceIn(FOUNDATION_SUIT_ORDER.indices)]

// ─────────────────────────────────────────────────────────────────────────────
// Board orientation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Orientation of the game board, tied to the device orientation.
 */
enum class BoardOrientation {
    PORTRAIT,
    LANDSCAPE;

    /** Width of the reference canvas for this orientation. */
    val modelWidth:  Int get() = if (this == PORTRAIT) PORTRAIT_MODEL_W  else LANDSCAPE_MODEL_W

    /** Height of the reference canvas for this orientation. */
    val modelHeight: Int get() = if (this == PORTRAIT) PORTRAIT_MODEL_H  else LANDSCAPE_MODEL_H
}

// ─────────────────────────────────────────────────────────────────────────────
// Group identifiers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The six logical zones that partition the game board.
 * Each zone owns a [GroupBox] in the reference coordinate space.
 *
 * Content within each zone is laid out relative to its box bounds;
 * zones do not know about each other's content.
 */
enum class GroupId {
    /** The fanned-out card columns (8 cols / 1-deck; 10 cols / 2-deck). */
    TABLEAU,

    /**
     * The suit-building piles (4 / 1-deck; 8 / 2-deck).
     *
     * Portrait  : a single 2-row block; 1-deck fills the bottom two slots of each row.
     * Landscape : a double-column block adjacent to TABLEAU; 1-deck uses the column
     *             closest to the tableau (left column in classic, right in mirrored).
     */
    FOUNDATION,

    /**
     * Stock (draw pile) + waste fan, together in one box.
     * The waste fan extends right in classic, left in mirrored.
     */
    DRAW_WASTE,

    /** Score, move counter, game timer, gem count, coupon count. */
    STATS,

    /** Action buttons: new game, undo, redo, hint, auto-complete, etc. */
    CONTROLS,

    /**
     * Banner ad zone.
     * Ad content is placed inside this box via a standard AdMob View.
     * A [RefToScreenTransform.adSafetyPx] gap must be maintained between this box
     * and every other GroupBox to avoid accidental-click detection by the ad library.
     */
    ADS
}

// ─────────────────────────────────────────────────────────────────────────────
// GroupBox
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A rectangular zone in the reference coordinate space.
 *
 * Coordinates are in reference-model pixels (integers, origin upper-left).
 * [width] and [height] must both be > 0.
 *
 * @param id      Which logical zone this box represents.
 * @param left    X coordinate of the left edge (classic layout origin = left screen edge).
 * @param top     Y coordinate of the top edge  (origin = top screen edge).
 * @param width   Width in reference pixels (must be > 0).
 * @param height  Height in reference pixels (must be > 0).
 */
data class GroupBox(
    val id:     GroupId,
    val left:   Int,
    val top:    Int,
    val width:  Int,
    val height: Int
) {
    val right:  Int get() = left + width
    val bottom: Int get() = top  + height

    init {
        require(width  > 0) { "GroupBox.width must be > 0 (id=$id)" }
        require(height > 0) { "GroupBox.height must be > 0 (id=$id)" }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout configuration
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The inputs that select which set of reference-space group box positions to use.
 *
 * @param orientation  Current device orientation.
 * @param deckCount    1 or 2 (any other value is normalised to 1).
 * @param isMirrored   true = left-hand (mirrored) layout.
 */
data class LayoutConfig(
    val orientation: BoardOrientation,
    val deckCount:   Int,
    val isMirrored:  Boolean
) {
    /** Guaranteed to be 1 or 2. */
    val normalizedDeckCount: Int get() = if (deckCount == 2) 2 else 1
}

// ─────────────────────────────────────────────────────────────────────────────
// Reference-to-screen transform
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pre-computed scale factors for mapping reference coordinates → screen pixels.
 *
 * Created once per [View.onSizeChanged] / orientation change; reused for all
 * hit-testing and drawing during that layout cycle.
 *
 * @param scaleX      screenWidthPx  / modelWidth  (float; ≥ 0.0)
 * @param scaleY      screenHeightPx / modelHeight (float; ≥ 0.0)
 * @param screenW     Actual View/screen width in pixels.
 * @param screenH     Actual View/screen height in pixels.
 * @param adSafetyPx  3 dp converted to px (minimum gap between ADS box and other boxes).
 */
data class RefToScreenTransform(
    val scaleX:     Float,
    val scaleY:     Float,
    val screenW:    Int,
    val screenH:    Int,
    val adSafetyPx: Int
)

/**
 * Build a [RefToScreenTransform] from the current View dimensions, the chosen
 * orientation, and the screen density.
 *
 * @param screenW     View width in pixels.
 * @param screenH     View height in pixels.
 * @param orientation Which reference canvas to use.
 * @param density     `context.resources.displayMetrics.density` (dp per screen pixel).
 */
fun buildRefToScreenTransform(
    screenW:     Int,
    screenH:     Int,
    orientation: BoardOrientation,
    density:     Float
): RefToScreenTransform {
    val sx = screenW.toFloat() / orientation.modelWidth.toFloat()
    val sy = screenH.toFloat() / orientation.modelHeight.toFloat()
    // 3 dp, rounded up to the nearest pixel, minimum 1 px.
    val adSafety = (3f * density + 0.5f).toInt().coerceAtLeast(1)
    return RefToScreenTransform(sx, sy, screenW, screenH, adSafety)
}

// ─────────────────────────────────────────────────────────────────────────────
// Coordinate transform helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Translate a reference [GroupBox] to a screen-space [RectF].
 *
 * When [isMirrored] is true the box is flipped horizontally around the model
 * centre before scaling:
 *
 *   mirroredLeft = modelWidth - left - width
 *
 * Then both classic and mirrored reference coordinates are scaled the same way.
 *
 * @param transform  Pre-computed scale factors for the current layout.
 * @param modelWidth Width of the reference canvas ([BoardOrientation.modelWidth]).
 * @param isMirrored Whether to apply the horizontal mirror.
 */
fun GroupBox.toScreenRectF(
    transform:  RefToScreenTransform,
    modelWidth: Int,
    isMirrored: Boolean
): RectF {
    val effectiveLeft = if (isMirrored) modelWidth - left - width else left
    return RectF(
        effectiveLeft            * transform.scaleX,
        top                      * transform.scaleY,
        (effectiveLeft + width)  * transform.scaleX,
        (top + height)           * transform.scaleY
    )
}

/**
 * Scale a single reference-space X value to screen pixels.
 * Use this for widths, horizontal offsets, etc.
 */
fun Int.refXToScreenPx(transform: RefToScreenTransform): Float = this * transform.scaleX

/**
 * Scale a single reference-space Y value to screen pixels.
 * Use this for heights, vertical offsets, etc.
 */
fun Int.refYToScreenPx(transform: RefToScreenTransform): Float = this * transform.scaleY

/**
 * Mirror a reference-space X position (left edge of a rect with the given [width])
 * around the reference model of [modelWidth].
 *
 *   mirroredLeft = modelWidth - left - width
 *
 * This keeps the right edge of the mirrored rect at the same distance from the
 * right edge of the model as the original left edge was from the left edge.
 */
fun mirrorRefX(left: Int, width: Int, modelWidth: Int): Int =
    modelWidth - left - width

/**
 * Strict touch containment check.
 *
 * Returns true only when (touchX, touchY) falls strictly inside [rect].
 * No fuzzy expansion beyond the rectangle boundary.
 *
 * All card-level hit testing must use this function (or a deliberately narrower
 * sub-rectangle) to prevent the "tap-at-screen-bottom selects topmost tableau
 * card" bug from the old layout system.
 */
fun containsTouch(rect: RectF, touchX: Float, touchY: Float): Boolean =
    touchX >= rect.left && touchX < rect.right &&
    touchY >= rect.top  && touchY < rect.bottom

// ─────────────────────────────────────────────────────────────────────────────
// Card specification
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Card rendered dimensions in reference-model pixels.
 *
 * The actual card aspect ratio is width : height ≈ 5 : 7 (≈ 1 : 1.40).
 * For accessibility / large-print decks the ratio may differ.
 *
 * Fan step sizes are NOT stored here because they are computed from available
 * space in the layout engine (Phase 1).  They depend on:
 *   - tableau box height
 *   - maximum pile depth (7 face-down + 13 face-up in the worst case)
 *
 * @param widthRef   Card width in reference pixels.
 * @param heightRef  Card height in reference pixels.
 * @param cornerRef  Corner radius in reference pixels (for rounded-rect drawing).
 */
data class CardSpec(
    val widthRef:  Int,
    val heightRef: Int,
    val cornerRef: Int
) {
    /**
     * Width of the waste fan when up to [MAX_VISIBLE_WASTE_CARDS] = 3 cards are shown.
     *
     * Exactly 2 extra cards are shown at 1/3 card-width each:
     *   fanWidth = widthRef + 2 × (widthRef / 3)  = (5/3) × widthRef  ≈ 1.667 × widthRef
     */
    val wasteFanWidthRef: Int get() = widthRef + 2 * (widthRef / 3)

    companion object {
        /** Maximum number of waste cards visible in the fan. */
        const val MAX_VISIBLE_WASTE_CARDS = 3
    }
}

/**
 * Default card dimensions for standard 1-deck play (portrait reference).
 *
 * With 8 tableau columns and reference width 1152:
 *   slot width = 1152 / 8 = 144 ref-px
 *   card width = 120 ref-px  (12-px padding each side)
 *   card height = 168 ref-px  (120 × 1.40)
 */
val DEFAULT_CARD_SPEC_1_DECK = CardSpec(widthRef = 120, heightRef = 168, cornerRef = 8)

/**
 * Default card dimensions for standard 2-deck play (portrait reference).
 *
 * With 10 tableau columns and reference width 1152:
 *   slot width = 1152 / 10 ≈ 115 ref-px
 *   card width = 96  ref-px  (~10-px padding each side)
 *   card height = 134 ref-px  (96 × 1.396 ≈ 96 × 1.40)
 */
val DEFAULT_CARD_SPEC_2_DECK = CardSpec(widthRef = 96,  heightRef = 134, cornerRef = 7)

/** Returns the default [CardSpec] for the given [deckCount]. */
fun defaultCardSpec(deckCount: Int): CardSpec =
    if (deckCount == 2) DEFAULT_CARD_SPEC_2_DECK else DEFAULT_CARD_SPEC_1_DECK

