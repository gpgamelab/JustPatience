package com.gpgamelab.justpatience

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.model.*
import com.gpgamelab.justpatience.presenter.GamePresenter

/**
 * Custom View responsible for drawing the Solitaire game board and handling user interaction.
 * This class contains the core Solitaire logic and display components.
 */
class GameBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- Core Game Components ---
    private val gamePresenter = GamePresenter(Game())
    private var cardBitmapMap: Map<Int, CardImage> = emptyMap()

    // --- Drawing & Layout Dimensions (will be calculated in onSizeChanged) ---
    private var cardWidth: Float = 0f
    private var cardHeight: Float = 0f
    private var cardCornerRadius: Float = 0f
    private var stackMargin: Float = 0f
    private var tableauMargin: Float = 0f

    // Layout positions (RectF's will be used in the real implementation, using simple floats for now)
    private var foundationsX = FloatArray(4) // X positions for 4 foundations
    private var tableauX = FloatArray(7)     // X positions for 7 tableau piles
    private var topRowY: Float = 0f

    // --- Touch/Interaction State ---
    private var draggedCards: List<Card> = emptyList() // The cards currently being dragged
    private var dragStartX: Float = 0f
    private var dragStartY: Float = 0f
    private var isDragging = false
    private var sourceStack: CardStack? = null // The stack the drag originated from

    // --- Paint Objects ---
    private val backgroundPaint = Paint().apply { color = ContextCompat.getColor(context, R.color.card_background) }
    private val placeholderPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.card_placeholder)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.text_color)
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    // --- Initialization ---

    init {
        // Load card images (mocked for now, replaced with actual assets later)
        loadCardAssets()
        // Set up initial touch listener
        setOnTouchListener { _, event -> handleTouch(event) }
    }

    private fun loadCardAssets() {
        // In a real app, this would load 52 card bitmaps and a back image.
        // For now, we'll just map a default image resource ID to a CardImage stub.
        cardBitmapMap = Card.fullDeck().associate { card ->
            card.id to CardImage(
                cardId = card.id,
                isFaceUp = card.isFaceUp,
                isRed = card.suit == Suit.DIAMONDS || card.suit == Suit.HEARTS,
                // Use a placeholder resource ID for drawing
                resourceId = R.drawable.ic_card_placeholder
            )
        }
    }

    // --- Layout and Dimension Calculation ---

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions(w, h)
        // Ensure the activity binds to this view's functions after dimensions are set
        (context as? GameActivity)?.bindViewToPresenter(gamePresenter)
    }

    /**
     * Calculates the necessary sizes and positions for drawing the board elements.
     * This is crucial for a responsive layout.
     */
    private fun calculateDimensions(width: Int, height: Int) {
        // 7 tableau piles, 4 foundation piles, 1 stock, 1 waste = 13 main slots
        // We will lay them out in two main rows: Top (Foundations, Stock, Waste) and Bottom (Tableau)

        // Use 7 columns for width calculation (to ensure tableau fits)
        val numColumns = 7
        val totalHorizontalMargin = width * 0.04f // 4% total padding
        stackMargin = (totalHorizontalMargin / (numColumns + 1)) // Margin between stacks

        cardWidth = (width - totalHorizontalMargin) / numColumns
        cardHeight = cardWidth * 1.4f // Standard card aspect ratio
        cardCornerRadius = cardWidth * 0.08f

        tableauMargin = cardWidth * 0.15f // Vertical margin for cascade

        topRowY = stackMargin

        // Calculate X positions for Tableau piles (distributed evenly)
        var currentX = stackMargin
        for (i in 0 until 7) {
            tableauX[i] = currentX
            currentX += cardWidth + stackMargin
        }

        // Calculate X positions for Foundations (right side)
        val foundationStart = tableauX[3] + cardWidth + stackMargin
        currentX = foundationStart
        for (i in 0 until 4) {
            foundationsX[i] = currentX
            currentX += cardWidth + stackMargin
        }

        // Placeholder for Stock and Waste are tableauX[0] and tableauX[1]
    }

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the background color (already set via XML, but good practice to draw explicitly if needed)

        // 1. Draw Foundations, Stock, and Waste Piles (Top Row)
        drawTopRow(canvas)

        // 2. Draw Tableau Piles (Main Area)
        drawTableau(canvas)

        // 3. Draw Dragged Cards (On top of everything else)
        if (isDragging && draggedCards.isNotEmpty()) {
            drawDraggedCards(canvas)
        }
    }

    private fun drawTopRow(canvas: Canvas) {
        // Mock positions: Stock is 0, Waste is 1, Foundations start at 3
        val stockRect = RectF(tableauX[0], topRowY, tableauX[0] + cardWidth, topRowY + cardHeight)
        val wasteRect = RectF(tableauX[1], topRowY, tableauX[1] + cardWidth, topRowY + cardHeight)

        // Draw Stock
        drawStackPlaceholder(canvas, stockRect, "S")

        // Draw Waste (Top card if available)
        drawStackPlaceholder(canvas, wasteRect, "W")

        // Draw Foundations
        for (i in 0 until 4) {
            val rect = RectF(foundationsX[i], topRowY, foundationsX[i] + cardWidth, topRowY + cardHeight)
            drawStackPlaceholder(canvas, rect, "F${i+1}")
        }
    }

    private fun drawTableau(canvas: Canvas) {
        // This is where we would iterate through gamePresenter.game.tableauPiles
        for (i in 0 until 7) {
            val rect = RectF(tableauX[i], topRowY + cardHeight + stackMargin, tableauX[i] + cardWidth, topRowY + cardHeight + stackMargin + cardHeight)
            drawStackPlaceholder(canvas, rect, "T${i+1}")
        }
    }

    private fun drawStackPlaceholder(canvas: Canvas, rect: RectF, text: String) {
        canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, placeholderPaint)
        canvas.drawText(text, rect.centerX(), rect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint)
    }

    private fun drawCard(canvas: Canvas, card: Card, x: Float, y: Float) {
        // In the final version, this would draw the actual bitmap from cardBitmapMap
        // For now, we draw a solid rectangle as a placeholder
        val rect = RectF(x, y, x + cardWidth, y + cardHeight)

        backgroundPaint.color = if (card.isFaceUp) {
            if (card.isRed) Color.RED else Color.BLACK
        } else {
            ContextCompat.getColor(context, R.color.card_back_color)
        }

        canvas.drawRoundRect(rect, cardCornerRadius, cardCornerRadius, backgroundPaint)

        // Draw rank/suit text (placeholder)
        if (card.isFaceUp) {
            textPaint.color = Color.WHITE
            canvas.drawText("${card.rank.symbol} ${card.suit.symbol}", rect.centerX(), rect.centerY(), textPaint)
        } else {
            textPaint.color = Color.LTGRAY
            canvas.drawText("BACK", rect.centerX(), rect.centerY(), textPaint)
        }
    }

    private fun drawDraggedCards(canvas: Canvas) {
        var currentY = dragStartY
        for (card in draggedCards) {
            drawCard(canvas, card, dragStartX, currentY)
            currentY += tableauMargin // Offset for cascaded drag
        }
    }

    // --- Interaction Logic ---

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.x
                dragStartY = event.y
                isDragging = false

                // 1. Check for Taps (Stock/Waste/Foundations)
                // (Implementation for tap on stock to draw/reset, etc., would go here)

                // 2. Check for Drag Start (Tableau or Waste)
                sourceStack = findSourceStack(dragStartX, dragStartY)

                if (sourceStack != null) {
                    draggedCards = getCardsToDrag(sourceStack!!, dragStartY)
                    if (draggedCards.isNotEmpty()) {
                        // Start drag but wait for movement threshold
                        isDragging = true
                        return true // Consume event
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    dragStartX = event.x - (draggedCards.first().width / 2) // Center the card under finger (simplified)
                    dragStartY = event.y - (draggedCards.first().height / 2)
                    invalidate() // Redraw the dragged cards
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    val dropX = event.x
                    val dropY = event.y

                    // 1. Handle Drop Logic
                    handleDrop(dropX, dropY)

                    // 2. Reset State
                    isDragging = false
                    draggedCards = emptyList()
                    sourceStack = null
                    invalidate() // Redraw the board without dragged cards
                    return true
                } else {
                    // If no drag occurred, treat it as a tap
                    handleTap(dragStartX, dragStartY)
                }
            }
        }
        return false // Only return true if we consumed the event
    }

    private fun handleTap(x: Float, y: Float) {
        // Find the tapped card/stack
        val tappedStack = findSourceStack(x, y)
        if (tappedStack == null) return

        when (tappedStack.type) {
            StackType.STOCK -> {
                if (gamePresenter.drawCard()) {
                    // Successful draw/reset
                    (context as? GameActivity)?.updateScoreAndMoves()
                    invalidate()
                } else {
                    Toast.makeText(context, "Stock is empty or game is locked.", Toast.LENGTH_SHORT).show()
                }
            }
            StackType.WASTE -> {
                // Attempt to auto-move the waste card to a foundation or tableau
                if (gamePresenter.autoMoveWaste()) {
                    (context as? GameActivity)?.updateScoreAndMoves()
                    invalidate()
                }
            }
            StackType.TABLEAU -> {
                // Attempt to auto-move the top tableau card
                if (gamePresenter.autoMoveTableauCard(tappedStack)) {
                    (context as? GameActivity)?.updateScoreAndMoves()
                    invalidate()
                } else {
                    // Try to flip an unexposed card
                    if (gamePresenter.flipTableauCard(tappedStack)) {
                        (context as? GameActivity)?.updateScoreAndMoves()
                        invalidate()
                    }
                }
            }
            StackType.FOUNDATION -> {
                // Tapping foundations does nothing in standard Solitaire
            }
        }
    }

    private fun handleDrop(x: Float, y: Float) {
        if (sourceStack == null || draggedCards.isEmpty()) return

        val targetStack = findTargetStack(x, y)
        if (targetStack != null && targetStack != sourceStack) {
            val success = gamePresenter.moveCards(draggedCards, sourceStack!!, targetStack)
            if (success) {
                (context as? GameActivity)?.updateScoreAndMoves()
                // Check win condition after every successful move
                if (gamePresenter.checkWinCondition()) {
                    (context as? GameActivity)?.showWinDialog()
                }
            } else {
                Toast.makeText(context, "Invalid move.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Helper Functions (Simplified stubs for card location) ---

    private fun findSourceStack(x: Float, y: Float): CardStack? {
        // Simplified stub: In reality, you'd check RectF bounds for each card pile

        // 1. Check Foundations (Top right 4)
        for (i in 0 until 4) {
            val rect = RectF(foundationsX[i], topRowY, foundationsX[i] + cardWidth, topRowY + cardHeight)
            if (rect.contains(x, y)) {
                // Return the actual foundation pile from the game state
                return gamePresenter.game.foundationPiles.getOrNull(i)
            }
        }

        // 2. Check Stock/Waste (Top left)
        val stockRect = RectF(tableauX[0], topRowY, tableauX[0] + cardWidth, topRowY + cardHeight)
        if (stockRect.contains(x, y)) return gamePresenter.game.stock

        val wasteRect = RectF(tableauX[1], topRowY, tableauX[1] + cardWidth, topRowY + cardHeight)
        if (wasteRect.contains(x, y)) return gamePresenter.game.waste

        // 3. Check Tableau Piles
        for (i in 0 until 7) {
            val tableauStartRect = RectF(tableauX[i], topRowY + cardHeight + stackMargin, tableauX[i] + cardWidth, height.toFloat())
            if (tableauStartRect.contains(x, y)) {
                // If it's a Tableau pile, we also need to determine WHICH card was clicked
                // and if it's eligible to be moved.
                return gamePresenter.game.tableauPiles.getOrNull(i)
            }
        }

        return null
    }

    private fun findTargetStack(x: Float, y: Float): CardStack? {
        // Simplified version: just checks if the drop point is over a stack's base area
        return findSourceStack(x, y) // For simplicity, we use the same location logic for drop targets
    }

    private fun getCardsToDrag(source: CardStack, y: Float): List<Card> {
        // Simplified stub: In a real app, you would calculate the card index based on y position
        // and check if all cards below it are face up and form a valid sequence.

        // For simplicity, we will only allow dragging the top face-up card from tableau
        if (source.type == StackType.TABLEAU) {
            val pile = source as TableauPile
            val topFaceUpIndex = pile.cards.indexOfLast { it.isFaceUp }

            if (topFaceUpIndex != -1) {
                // Select all cards from the top face-up card to the end of the pile
                return pile.cards.subList(topFaceUpIndex, pile.cards.size)
            }
        } else if (source.type == StackType.WASTE) {
            // Only the top card of the waste pile can be moved
            return source.cards.takeLast(1)
        }

        return emptyList()
    }

    // --- Public Game Control Methods (Called by GameActivity) ---

    fun startNewGame(loadSavedGame: Boolean) {
        if (loadSavedGame) {
            // Logic to load saved game state from GameActivity intent/data
            Toast.makeText(context, "Loading saved game...", Toast.LENGTH_SHORT).show()
        } else {
            gamePresenter.startNewGame()
            Toast.makeText(context, "New game started!", Toast.LENGTH_SHORT).show()
        }
        invalidate()
    }

    fun undoLastMove(): Boolean {
        val success = gamePresenter.undoLastMove()
        if (success) {
            (context as? GameActivity)?.updateScoreAndMoves()
            invalidate()
        } else {
            Toast.makeText(context, "No moves to undo.", Toast.LENGTH_SHORT).show()
        }
        return success
    }

    fun getScore(): Int = gamePresenter.getScore()
    fun getMoves(): Int = gamePresenter.getMoves()
    fun getGamePresenter(): GamePresenter = gamePresenter


    // --- Inner Helper Classes for Data Structure Mocking ---

    /**
     * Data structure to hold information needed for drawing a card image.
     * In a real implementation, this would hold the Bitmap.
     */
    private data class CardImage(
        val cardId: Int,
        val isFaceUp: Boolean,
        val isRed: Boolean,
        val resourceId: Int // Placeholder for the actual drawable resource ID
    )

    // Simple mock RectF class since we can't import android.graphics.RectF here without context
    private data class RectF(
        var left: Float,
        var top: Float,
        var right: Float,
        var bottom: Float
    ) {
        fun contains(x: Float, y: Float): Boolean {
            return x >= left && x < right && y >= top && y < bottom
        }
        fun centerX(): Float = (left + right) / 2
        fun centerY(): Float = (top + bottom) / 2
    }
}