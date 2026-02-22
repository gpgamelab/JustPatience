package com.gpgamelab.justpatience.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.gpgamelab.justpatience.assets.AssetResolver
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.model.Card
import com.gpgamelab.justpatience.model.CardSuit
import com.gpgamelab.justpatience.model.StackType
import com.gpgamelab.justpatience.model.TableauPile
import com.gpgamelab.justpatience.model.Verso
import kotlin.math.abs
import kotlin.math.max

private const val DEFAULT_STOCK_BACK_IMAGE_PATH = "drawable:b_0001"


class GameBoardView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    lateinit var viewModel: GameViewModel
    lateinit var assetResolver: AssetResolver

    private val currentSetId = "default"

    private var lastTapTime = 0L
    private val doubleTapTimeout = 300L

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

    // Board and Card dimensions.
    private val BOARD_WIDTH_FRACTION = 0.70f
    private val BOARD_SHIFT_LEFT_PX = 300f

    private val cardWidthRatio = 1.0f
    private val cardHeightRatio = 2.0f
    private val cardRadius = 20f
    private val cardPadding = 16f
    private val tableauOffset = 40f

    private var cardW = 0f
    private var cardH = 0f
    private var columns = 7
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0) {
            columnX.clear()
            val usableWidth = w * BOARD_WIDTH_FRACTION
            val totalPad = cardPadding * (columns + 1)
            cardW = (usableWidth - totalPad) / (columns + 1) * cardWidthRatio
            cardH = cardW * cardHeightRatio

            computeColumnX()
        }
    }

    private fun computeColumnX() {
        columnX.clear()

        val boardWidth =
            (cardW * columns) + (cardPadding * (columns - 1))

        val startX = (width - boardWidth) / 2f - BOARD_SHIFT_LEFT_PX

        var x = startX
        repeat(columns) {
            columnX.add(x)
            x += cardW + cardPadding
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::viewModel.isInitialized) {
            drawLoading(canvas); return
        }
        drawTopRow(canvas)
        drawTableau(canvas)
        drawDragGhost(canvas)
    }

private fun drawDragGhost(canvas: Canvas) {
    if (!isDragging) return

    val x = dragX - dragOffsetX
    val y = dragY - dragOffsetY

    when (dragStackType) {

        StackType.TABLEAU -> {
            val pile = viewModel.game.value.tableau[dragStackIndex]
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

        else -> Unit
    }
}


    private fun drawLoading(c: Canvas) {
        textPaint.color = Color.GRAY
        textPaint.textSize = 60f
        c.drawText("Loading Game...", width / 2f, height / 2f, textPaint)
        textPaint.textSize = cardW * 0.3f
    }

    private fun drawTopRow(canvas: Canvas) {
        val topY = cardPadding
        // Stock
        val stockRect = RectF(columnX[0], topY, columnX[0] + cardW, topY + cardH)
        val stock = viewModel.game.value.stock
        if (!stock.isEmpty()) drawStockBack(canvas, stockRect) else canvas.drawRoundRect(stockRect, cardRadius, cardRadius, placeholderPaint)

        // Waste
        val wasteRect = RectF(columnX[1], topY, columnX[1] + cardW, topY + cardH)
        val waste = viewModel.game.value.waste
        if (!waste.isEmpty() && !(isDragging && dragStackType == StackType.WASTE)) {
            waste.peek()?.let { drawCard(canvas, it, wasteRect) }
        } else
            canvas.drawRoundRect(wasteRect, cardRadius, cardRadius, placeholderPaint)

        // Foundations at columns 3..6 (index 0..3)
        for (i in 0..3) {
            val x = columnX[3 + i]
            val rect = RectF(x, topY, x + cardW, topY + cardH)
            canvas.drawRoundRect(rect, cardRadius, cardRadius, placeholderPaint)
            viewModel.game.value.foundations.getOrNull(i)?.peek()?.let { drawCard(canvas, it, rect) }
        }
    }

    private fun drawTableau(canvas: Canvas) {
        val startY = cardPadding + cardH + cardPadding + tableauOffset
        viewModel.game.value.tableau.forEachIndexed { colIdx, pile ->
            val x = columnX[colIdx]
            var y = startY
            val cards = pile.asList()
            if (cards.isEmpty()) {
                val rect = RectF(x, y, x + cardW, y + cardH)
                canvas.drawRoundRect(rect, cardRadius, cardRadius, placeholderPaint)
            } else {
                cards.forEachIndexed { index, card ->

                    // Skip cards that are currently being dragged
                    if (isDragging &&
                        dragStackType == StackType.TABLEAU &&
                        colIdx == dragStackIndex &&
                        index >= dragCardIndex
                    ) {
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

    private fun drawCard(canvas: Canvas, card: Card, rect: RectF) {
        val bitmap = assetResolver.resolve(currentSetId, card.recto.imagePath, rect.width().toInt(), rect.height().toInt())

        canvas.drawBitmap(
            bitmap,
            null,          // draw entire bitmap
            rect,          // scale to card rect
            null
        )

        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderPaint)
    }

    private fun drawCardBack(canvas: Canvas, rect: RectF, verso: Verso) {
        val bitmap = assetResolver.resolve(currentSetId, verso.imagePath, rect.width().toInt(), rect.height().toInt())

        canvas.drawBitmap(
            bitmap,
            null,
            rect,
            null
        )

        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderPaint)
    }
    private fun drawStockBack(canvas: Canvas, rect: RectF) {
        val bitmap = assetResolver.resolve(
            currentSetId,
            DEFAULT_STOCK_BACK_IMAGE_PATH,
            cardW.toInt(),
            cardH.toInt()
        )
        canvas.drawBitmap(bitmap, null, rect, null)
        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderPaint)
    }

     private fun handleTap(x: Float, y: Float) {
        val (type, stackIndex, _) = findStackAt(x, y)

        when (type) {
            StackType.STOCK -> {
                viewModel.drawFromStock()
            }

            StackType.WASTE -> {
                viewModel.tryAutoMoveWasteToFoundation()
            }

            StackType.TABLEAU -> {
                viewModel.tryAutoMoveTableauTopToFoundation(stackIndex)
            }

            else -> {}
        }
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

        val topY = cardPadding
        val startTableauY = topY + cardH + cardPadding + tableauOffset

        return when (type) {

            StackType.STOCK -> {
                val x = columnX[0]
                RectF(x, topY, x + cardW, topY + cardH)
            }

            StackType.WASTE -> {
                val x = columnX[1]
                RectF(x, topY, x + cardW, topY + cardH)
            }

            StackType.FOUNDATION -> {
                val x = columnX[3 + stackIndex]
                RectF(x, topY, x + cardW, topY + cardH)
            }

            StackType.TABLEAU -> {
                val x = columnX[stackIndex]
                var y = startTableauY

                val pile = viewModel.game.value.tableau[stackIndex]
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

        for (i in 0 until index) {
            val card = pile.cards[i]
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
        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isDragging = false

                val (stackType, stackIndex, cardIndex) = findStackAt(event.x, event.y)

                if (stackType == StackType.TABLEAU) {
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

                    val startY =
                        cardPadding + cardH + cardPadding + tableauOffset

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
                        if (dragStackType == StackType.TABLEAU || dragStackType == StackType.WASTE) {                            isDragging = true
                            postInvalidateOnAnimation()
                        } else
                        {                            clearDragState()
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

                    // 1️⃣ FOUNDATION DROP (highest priority)
                    val topY = cardPadding
                    val foundationStartX = columnX[3]

                    for (i in viewModel.game.value.foundations.indices) {
                        val x = foundationStartX + i * (cardW + cardPadding)
                        val rect = RectF(x, topY, x + cardW, topY + cardH)

                        if (rect.contains(event.x, event.y)) {
                            when (dragStackType) {

                                StackType.WASTE -> {
                                    moveSucceeded = viewModel.tryMoveWasteToFoundation(i)
                                }

                                StackType.TABLEAU -> {
                                    moveSucceeded = viewModel.tryMoveTableauToFoundation(
                                        dragStackIndex,
                                        dragCardIndex,
                                        i
                                    )
                                }

                                else -> {}
                            }
                            break
                        }
                    }

                    // 2️⃣ TABLEAU DROP (only if foundation failed)
                    if (!moveSucceeded) {
                        val startY = cardPadding + cardH + cardPadding + tableauOffset

                        columnX.forEachIndexed { index, colX ->
                            val rect = RectF(colX, startY, colX + cardW, height.toFloat())

                            if (rect.contains(event.x, event.y)) {

                                when (dragStackType) {

                                    StackType.WASTE -> {
                                        viewModel.tryMoveWasteToTableau(index)
                                        moveSucceeded = true
                                    }

                                    StackType.TABLEAU -> {
                                        moveSucceeded = viewModel.tryMoveTableauToTableau(
                                            dragStackIndex,
                                            dragCardIndex,
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
                        handleTap(event.x, event.y)
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
        val topY = cardPadding
        val topBottom = topY + cardH
        if (y in topY..topBottom) {
            for (i in 0 until columns) {
                val cx = columnX[i]
                if (x in cx..(cx + cardW)) {
                    return when (i) {
                        0 -> Triple(StackType.STOCK, 0, -1)
                        1 -> Triple(StackType.WASTE, 0, -1)
                        in 3..6 -> Triple(StackType.FOUNDATION, i - 3, -1)
                        else -> Triple(null, -1, -1)
                    }
                }
            }
        }
        val startY = topBottom + cardPadding + tableauOffset
        if (y >= startY) {
            for (i in 0 until columns) {
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
        val startY = cardPadding + cardH + cardPadding + tableauOffset

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
}
