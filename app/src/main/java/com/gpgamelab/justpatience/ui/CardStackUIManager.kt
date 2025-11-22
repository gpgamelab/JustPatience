package com.gpgamelab.justpatience.ui

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.gpgamelab.justpatience.model.Card
import com.gpgamelab.justpatience.model.Game
import com.gpgamelab.justpatience.model.StackType
import com.gpgamelab.justpatience.viewmodel.GameViewModel

/**
 * STUB: Manages the visual rendering and interaction of card stacks on the screen.
 * In a real application, this class would handle creating, positioning, and updating
 * all custom CardView objects within the GameBoardContainer and managing the complex
 * drag-and-drop logic that maps screen coordinates back to Card and CardStack objects.
 */
class CardStackUIManager(
    private val context: Context,
    private val gameBoardContainer: ViewGroup,
    private val viewModel: GameViewModel
) {
    // Map to quickly find the ViewGroup (UI element) for a given stack index/type
    private val stackViews = mutableMapOf<Pair<StackType, Int>, ViewGroup>()

    /**
     * Finds and maps the UI container views to their corresponding game model stacks.
     */
    fun initViews(
        stockId: Int,
        wasteId: Int,
        foundationIds: List<Int>,
        tableauIds: List<Int>
    ) {
        // Find views and populate stackViews map
        gameBoardContainer.findViewById<ViewGroup>(stockId)?.let {
            stackViews[Pair(StackType.STOCK, -1)] = it
            it.setOnClickListener { viewModel.drawFromStock() } // Only the stock pile has a click action
        }
        gameBoardContainer.findViewById<ViewGroup>(wasteId)?.let {
            stackViews[Pair(StackType.WASTE, -1)] = it
        }

        foundationIds.forEachIndexed { index, id ->
            gameBoardContainer.findViewById<ViewGroup>(id)?.let {
                stackViews[Pair(StackType.FOUNDATION, index)] = it
                it.setOnDragListener { v, event -> handleDrag(v, event, StackType.FOUNDATION, index) }
            }
        }

        tableauIds.forEachIndexed { index, id ->
            gameBoardContainer.findViewById<ViewGroup>(id)?.let {
                stackViews[Pair(StackType.TABLEAU, index)] = it
                it.setOnDragListener { v, event -> handleDrag(v, event, StackType.TABLEAU, index) }
            }
        }

        Log.d("UIManager", "Initialized ${stackViews.size} stack containers.")
    }

    /**
     * Renders the current game state to the screen.
     * This method clears all card views and recreates them based on the Game object.
     */
    fun render(game: Game) {
        // In a real app, this would be a complex loop:
        // 1. Clear all existing card views from stackViews
        // 2. Iterate through game.tableauPiles, game.foundationPiles, game.stock, game.waste
        // 3. For each Card, instantiate a custom CardView, set its faceUp state, and add it to the correct ViewGroup
        // 4. Attach an OnTouchListener to the CardView for drag start logic

        // STUB Implementation: For demonstration, we just log a successful render.
        Log.d("UIManager", "Rendering new game state. Score: ${game.score}")
    }

    /**
     * Handles the receiving end of a drag event (the target stack).
     */
    private fun handleDrag(view: View, event: DragEvent, targetType: StackType, targetIndex: Int): Boolean {
        // STUB: This is where the complex logic of identifying the dragged card and
        // passing it to the ViewModel happens.
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true // Accept drag operation
            DragEvent.ACTION_DROP -> {
                // 1. Retrieve the source card data passed during DragStart
                val sourceCard = event.localState as? Card ?: return false
                val sourceStack = viewModel.findStackContainingCard(sourceCard) ?: return false

                // 2. Call the ViewModel's move handler
                viewModel.handleCardMove(sourceCard, sourceStack, targetType, targetIndex)

                // The UIManager's render() method (called by the Activity observer) will refresh the UI
                true
            }
            else -> false
        }
    }

    /**
     * STUB: Logic to start a drag operation from a card view.
     * This would typically be called from the CardView's OnTouchListener.
     */
    private fun startDrag(card: Card, view: View) {
        val dragShadowBuilder = View.DragShadowBuilder(view)
        // Pass the Card object as local state so the target can identify what is being dragged
        view.startDragAndDrop(null, dragShadowBuilder, card, 0)
    }
}