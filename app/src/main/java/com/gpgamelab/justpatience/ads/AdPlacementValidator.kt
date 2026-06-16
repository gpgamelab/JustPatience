package com.gpgamelab.justpatience.ads

import android.graphics.RectF
import com.gpgamelab.justpatience.ui.layout.GroupId

/**
 * Phase 4 – Ad Placement Validator
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * Runtime validator that confirms a proposed ad container rectangle does NOT
 * overlap any interactive game zone, with an enforced safety gap around each zone.
 *
 * All input coordinates are in screen pixels (as produced by the Phase 2
 * reference-model-to-screen transform).
 *
 * Design contract:
 * - [validate] is the primary entry point.  Pass the proposed ad container rect,
 *   the current Phase 2 group rects, and the safety gap in pixels.
 * - [getInteractiveZones] extracts the zones to check from the Phase 2 rects.
 * - The ADS group itself is excluded from the zone list (it IS the ad zone).
 * - Two rects that merely touch at an edge are NOT considered overlapping.
 */
object AdPlacementValidator {

    /**
     * Group IDs considered "interactive" – i.e., zones that the player touches
     * and that must not be obscured or adjacent to the ad container.
     *
     * [GroupId.ADS] is intentionally excluded (it is the ad zone itself).
     * [GroupId.STATS] is included because tapping the stats area is a common
     * interaction (e.g., score tap to open stats dialog).
     */
    private val INTERACTIVE_GROUP_IDS = setOf(
        GroupId.TABLEAU,
        GroupId.FOUNDATION,
        GroupId.DRAW_WASTE,
        GroupId.CONTROLS,
        GroupId.STATS
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validate that [adRect] is safe to display without overlapping any interactive zone.
     *
     * Each interactive zone is expanded outward by [safetyGapPx] before the overlap
     * check, enforcing the mandatory physical gap between the ad and game controls.
     *
     * @param adRect       Proposed ad container bounds in screen pixels.
     * @param groupRects   Phase 2 screen-space rectangles keyed by [GroupId].
     *                     Must contain at least the groups in [INTERACTIVE_GROUP_IDS] that
     *                     are currently visible; missing groups are silently skipped.
     * @param safetyGapPx  Minimum gap in pixels (derived from [RefToScreenTransform.adSafetyPx]
     *                     or from [AdSafetyGap.dpValue] × display density).
     * @return true  — placement is safe (no overlaps with any interactive zone).
     *         false — at least one overlap detected (caller should try fallback or suppress).
     */
    fun validate(
        adRect:      RectF,
        groupRects:  Map<GroupId, RectF>,
        safetyGapPx: Float
    ): Boolean {
        val zones = getInteractiveZones(groupRects)
        if (zones.isEmpty()) {
            return true
        }
        for (zone in zones) {
            val expanded = expandRect(zone.rect, safetyGapPx)
            if (overlaps(adRect, expanded)) {
                return false
            }
        }
        return true
    }

    /**
     * Extract the interactive zones from Phase 2 group rects.
     *
     * Returns a [InteractiveZone] for each group ID in [INTERACTIVE_GROUP_IDS] that
     * is present in [groupRects].  Groups not yet computed are silently omitted.
     */
    fun getInteractiveZones(groupRects: Map<GroupId, RectF>): List<InteractiveZone> =
        INTERACTIVE_GROUP_IDS.mapNotNull { id ->
            groupRects[id]?.let { rect -> InteractiveZone(label = id.name, rect = RectF(rect)) }
        }

    /**
     * Expand [rect] outward by [gapPx] on all four sides.
     *
     * Returns a new [RectF]; the original is not modified.
     * If [gapPx] is 0, a copy of the original is returned.
     */
    fun expandRect(rect: RectF, gapPx: Float): RectF =
        RectF(
            rect.left   - gapPx,
            rect.top    - gapPx,
            rect.right  + gapPx,
            rect.bottom + gapPx
        )

    /**
     * Returns true if rectangles [a] and [b] share any overlapping area.
     *
     * Two rectangles that merely touch at an edge (a.right == b.left etc.)
     * are NOT considered overlapping – this matches AdMob's "not touching"
     * requirement for accidental-click prevention.
     */
    fun overlaps(a: RectF, b: RectF): Boolean =
        a.left < b.right  && a.right  > b.left &&
        a.top  < b.bottom && a.bottom > b.top
}

// ──────────────────────────────────────────────────────────────────────────────
// Supporting types
// ──────────────────────────────────────────────────────────────────────────────

/**
 * A named interactive zone on the game board.
 *
 * @param label  Human-readable zone name for logging (matches [GroupId.name]).
 * @param rect   Screen-space rectangle (pixels).  An immutable copy should be stored.
 */
data class InteractiveZone(
    val label: String,
    val rect:  RectF
)


