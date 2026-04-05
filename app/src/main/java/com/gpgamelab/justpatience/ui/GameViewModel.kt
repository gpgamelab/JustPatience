package com.gpgamelab.justpatience.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.gpgamelab.justpatience.data.GameStatsManager
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.data.TokenManager
import com.gpgamelab.justpatience.game.GameController
import com.gpgamelab.justpatience.model.Card
import com.gpgamelab.justpatience.model.CardRank
import com.gpgamelab.justpatience.model.Game
import com.gpgamelab.justpatience.model.GameStatus
import com.gpgamelab.justpatience.model.GlowDestination
import com.gpgamelab.justpatience.model.HintDisplayState
import com.gpgamelab.justpatience.model.HintMove
import com.gpgamelab.justpatience.model.HintPhase
import com.gpgamelab.justpatience.model.SingleClickGlowState
import com.gpgamelab.justpatience.model.StackType
import com.gpgamelab.justpatience.model.StandardRank
import com.gpgamelab.justpatience.model.Joker
import com.gpgamelab.justpatience.repository.GameRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Type
import java.util.Locale

private const val CARD_MOVE_ANIMATION_MS = 250L

/**
 * ViewModel that holds the current Game state and provides actions for UI.
 * Calls the GameController (rules engine) for all moves.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application.applicationContext)
    private val tokenManager = TokenManager(application.applicationContext)
    private val repository = GameRepository(settingsManager, tokenManager)
    private val statsManager = GameStatsManager(application.applicationContext)
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(CardRank::class.java, CardRankAdapter())
        .create()
    private val controller = GameController()
    
    // Reference for scheduling card move animations
    var gameBoardView: GameBoardView? = null
    val hasSavedGame = repository.getCurrentGameState()
        .map { !it.isNullOrEmpty() }
    val gameTime = MutableStateFlow(0L)

    // Current draw size setting (supports any positive integer, <=0 defaults to 1)
    private var currentDrawSize = 1
    private var currentRecycleLimit = 3
    private var currentInfiniteRecycles = false

    private val _drawCountForDisplay = MutableStateFlow(1)
    val drawCountForDisplay: StateFlow<Int> = _drawCountForDisplay

    private val _recycleLimitForDisplay = MutableStateFlow(3)
    val recycleLimitForDisplay: StateFlow<Int> = _recycleLimitForDisplay

    private val _isInfiniteRecycles = MutableStateFlow(false)
    val isInfiniteRecycles: StateFlow<Boolean> = _isInfiniteRecycles

    private val _showGameTimer = MutableStateFlow(true)
    val showGameTimer: StateFlow<Boolean> = _showGameTimer

    private val _showScore = MutableStateFlow(true)
    val showScore: StateFlow<Boolean> = _showScore

    private val _showMoves = MutableStateFlow(true)
    val showMoves: StateFlow<Boolean> = _showMoves

    private val _showCardAnimations = MutableStateFlow(true)
    val showCardAnimations: StateFlow<Boolean> = _showCardAnimations

    private val _showWinAnimation = MutableStateFlow(true)
    val showWinAnimation: StateFlow<Boolean> = _showWinAnimation

    private val _isMirroredLayout = MutableStateFlow(false)
    val isMirroredLayout: StateFlow<Boolean> = _isMirroredLayout

    private val _allowFoundationToTableauDrag = MutableStateFlow(false)
    val allowFoundationToTableauDrag: StateFlow<Boolean> = _allowFoundationToTableauDrag

    private val _autoCompleteEnabled = MutableStateFlow(true)
    val autoCompleteEnabled: StateFlow<Boolean> = _autoCompleteEnabled

    private val _hapticsEnabled = MutableStateFlow(false)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled

    private val _tapToMoveEnabled = MutableStateFlow(true)
    val tapToMoveEnabled: StateFlow<Boolean> = _tapToMoveEnabled

    private val _fullScreenEnabled = MutableStateFlow(false)
    val fullScreenEnabled: StateFlow<Boolean> = _fullScreenEnabled

    // Hint system (manual only via HINT button)
    private val _hintDisplayState = MutableStateFlow<HintDisplayState?>(null)
    val hintDisplayState: StateFlow<HintDisplayState?> = _hintDisplayState

    private var hintCycleJob: Job? = null

    // Single-click destination glows (independent of hint system)
    private val _singleClickGlowState = MutableStateFlow<SingleClickGlowState?>(null)
    val singleClickGlowState: StateFlow<SingleClickGlowState?> = _singleClickGlowState

    private var timerJob: Job? = null
    private var isTimerRunning = false
    private var hasRegisteredFirstMove = false

    private val undoStack = ArrayDeque<Game>()
    private val redoStack = ArrayDeque<Game>()
    private var initialGameState: Game? = null  // Store the game state at the start of the hand
    private var currentHandRecorded = false
    private var launchInitialized = false

    // Exposed state
    private val _game = MutableStateFlow(Game.newGame())

    val game: StateFlow<Game> = _game

    init {
        // Launch behavior is selected by GameActivity via initializeForLaunch(...).
        observeDrawSizeSetting()
    }

    fun initializeForLaunch(forceNewGame: Boolean) {
        if (launchInitialized) return
        launchInitialized = true

        viewModelScope.launch {
            if (forceNewGame) {
                startNewGameInternal()
                updateUndoRedoState()
                return@launch
            }

            val restored = loadSavedGame()
            if (restored) {
                updateUndoRedoState()
            } else {
                startNewGameInternal()
                updateUndoRedoState()
            }
        }
    }

    /**
     * Core new-game setup used when init already handled the abandoned-game check.
     * Skips finalizeCurrentGameIfNeeded() to avoid double-recording.
     */
    private suspend fun startNewGameInternal() {
        undoStack.clear()
        redoStack.clear()
        currentHandRecorded = false
        val newGame = controller.newGameWithClearHistory()
        initialGameState = newGame
        _game.value = newGame
        resetTimerForNewHand()
        saveGame()
    }

    private fun observeDrawSizeSetting() {
        viewModelScope.launch {
            settingsManager.gamePlaySettingsFlow.collect { settings ->
                currentDrawSize = settings.drawSize
                currentRecycleLimit = settings.recycleCount.coerceAtLeast(0)
                currentInfiniteRecycles = settings.infiniteRecycles

                _drawCountForDisplay.value = normalizeDrawCount(currentDrawSize)
                _recycleLimitForDisplay.value = currentRecycleLimit
                _isInfiniteRecycles.value = currentInfiniteRecycles
                _showCardAnimations.value = settings.showCardAnimations
                _showGameTimer.value = settings.showGameTimer
                _showScore.value = settings.showScore
                _showMoves.value = settings.showMoves
                _showWinAnimation.value = settings.showWinAnimation
                _isMirroredLayout.value = settings.boardLayout == "left_hand"
                _allowFoundationToTableauDrag.value = settings.allowFoundationToTableauDrag
                _autoCompleteEnabled.value = settings.autoComplete
                _hapticsEnabled.value = settings.haptics
                _tapToMoveEnabled.value = settings.tapToMove
                _fullScreenEnabled.value = settings.fullScreen

            }
        }
    }

    private fun normalizeDrawCount(rawDrawCount: Int): Int {
        return if (rawDrawCount <= 0) 1 else rawDrawCount
    }

    fun getDrawCountLabelValue(): Int = _drawCountForDisplay.value

    fun getRemainingRecycleCount(): Int? {
        if (currentInfiniteRecycles) return null
        return (currentRecycleLimit - _game.value.recycleCountUsed).coerceAtLeast(0)
    }

    private fun canRecycleWaste(game: Game): Boolean {
        if (currentInfiniteRecycles) return true
        return game.recycleCountUsed < currentRecycleLimit
    }

    private fun startTimerIfNeeded() {
        if (isTimerRunning) return
        isTimerRunning = true
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                gameTime.value += 1
            }
        }
    }

    private fun startTimerOnFirstSuccessfulMoveIfNeeded() {
        if (!hasRegisteredFirstMove) {
            hasRegisteredFirstMove = true
        }
        startTimerIfNeeded()
    }

    private fun resetTimerForNewHand() {
        stopTimer()
        gameTime.value = 0
        hasRegisteredFirstMove = false
    }

    fun stopTimer() {
        isTimerRunning = false
        timerJob?.cancel()
    }

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undoLastMove(): Boolean {
        undo()
        return true
    }
    fun redoLastMove(): Boolean {
        redo()
        return true
    }

    fun startNewGame() {
        finalizeCurrentGameIfNeeded()
        pauseHintTimerForNonPlayerActivity()
        undoStack.clear()
        redoStack.clear()
        resetTimerForNewHand()
        currentHandRecorded = false
        viewModelScope.launch {
            val newGame = controller.newGameWithClearHistory()
            initialGameState = newGame
            _game.value = newGame
            saveGame()
        }
    }

    fun restartGame() {
        finalizeCurrentGameIfNeeded()
        pauseHintTimerForNonPlayerActivity()
        val initial = initialGameState
        if (initial == null) {
            undoStack.clear()
            redoStack.clear()
            currentHandRecorded = false
            viewModelScope.launch {
                val newGame = controller.newGameWithClearHistory()
                initialGameState = newGame
                _game.value = newGame
                resetTimerForNewHand()
                updateUndoRedoState()
                saveGame()
            }
            return
        }
        undoStack.clear()
        redoStack.clear()
        _game.value = initial
        resetTimerForNewHand()
        currentHandRecorded = false
        updateUndoRedoState()
        saveGame()
    }

    fun drawFromStock() {
        val game = _game.value
        var moved = false
        var newGame = game

        // Determine how many cards to draw (<=0 defaults to 1)
        val drawCount = normalizeDrawCount(currentDrawSize)

        // 1️⃣ Normal draw - draw multiple cards if configured
        if (!game.stock.isEmpty()) {
            var currentStock = game.stock
            var currentWaste = game.waste
            var cardsDrawn = 0

            repeat(drawCount) {
                if (currentStock.isEmpty()) return@repeat
                val (newStock, card) = currentStock.withCardPopped()
                currentStock = newStock
                if (card != null) {
                    currentWaste = currentWaste.withCardAdded(card.copy(isFaceUp = true))
                    cardsDrawn++
                }
            }

            if (cardsDrawn > 0) {
                newGame = game.copy(stock = currentStock, waste = currentWaste)
                moved = true
            }
        } else {
            // Recycle waste -> stock only if recycle limit allows it.
            if (!game.waste.isEmpty() && canRecycleWaste(game)) {
                val recycled = game.recycleWasteToStock()
                if (recycled != null) {
                    newGame = recycled.copy(recycleCountUsed = game.recycleCountUsed + 1)
                    moved = true
                }
            }
        }

        if (moved) {
            clearSingleClickGlow()
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(newGame)  // scoreDelta = 0 for simple scoring
            _game.value = updatedWithScore
            updateUndoRedoState()
        }
    }

    fun attemptMove(
        sourceType: StackType,
        sourceIndex: Int,
        cardIndexInSource: Int,
        targetType: StackType,
        targetIndex: Int
    ) {
        viewModelScope.launch {
            try {
                val game = _game.value
                val (theUpdatedGame, success) = controller.attemptMove(
                    game,
                    sourceType,
                    sourceIndex,
                    cardIndexInSource,
                    targetType,
                    targetIndex
                )
                if (success) {
                    undoStack.addLast(game)
                    redoStack.clear()
                    // Score is already applied by controller, just update move count
                    val updatedWithMoves = theUpdatedGame.copy(moves = theUpdatedGame.moves + 1)
                    startTimerOnFirstSuccessfulMoveIfNeeded()
                    _game.value = updatedWithMoves
                    saveGame()
                    updateUndoRedoState()
                }
            } catch (t: Throwable) {
                Log.w("GameViewModel", "attemptMove failed: ${t.message}")
            }
        }
    }

    fun tryMoveWasteToTableau(index: Int) {
        val game = _game.value
        val updated = game.moveWasteToTableau(index)

        if (updated != null) {
            clearSingleClickGlow()
            resetHintTimer()
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated, scoreDelta = 5)
            _game.value = updatedWithScore
            saveGame()
            updateUndoRedoState()
        }
    }

    fun tryMoveTableauToTableau(
        fromIndex: Int,
        cardIndex: Int,
        toIndex: Int
    ): Boolean {
        val game = _game.value
        val updated = game.moveTableauToTableau(fromIndex, cardIndex, toIndex)

        if (updated != null) {
            clearSingleClickGlow()
            resetHintTimer()
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated)
            _game.value = updatedWithScore
            saveGame()
            updateUndoRedoState()
            return true
        }

        return false
    }

    fun tryMoveFoundationToTableau(
        foundationIndex: Int,
        tableauIndex: Int
    ): Boolean {
        if (!_allowFoundationToTableauDrag.value) return false

        val game = _game.value
        val updated = game.moveFoundationToTableau(foundationIndex, tableauIndex)

        if (updated != null) {
            clearSingleClickGlow()
            resetHintTimer()
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated)
            _game.value = updatedWithScore
            saveGame()
            updateUndoRedoState()
            return true
        }

        return false
    }

    fun tryAutoMoveWasteToFoundation(respectThreshold: Boolean = false): Boolean {
        val game = _game.value
        val wasteCard = game.waste.peek() ?: return false

        if (respectThreshold && !shouldPreferFoundationOnSingleTap(game, wasteCard)) {
            return false
        }

        for (i in game.foundations.indices) {
            val updated = game.moveWasteToFoundation(i)
            if (updated != null) {
                animateCardMove(
                    card = wasteCard,
                    sourceStackType = StackType.WASTE,
                    sourceIndex = 0,
                    sourceCardIndex = -1,
                    destStackType = StackType.FOUNDATION,
                    destIndex = i
                )
                clearSingleClickGlow()
                resetHintTimer()
                undoStack.addLast(game)
                redoStack.clear()
                val updatedWithScore = updateAfterMove(updated, scoreDelta = 10)
                _game.value = updatedWithScore
                saveGame()
                updateUndoRedoState()
                return true
            }
        }
        return false
    }

    fun tryAutoMoveTableauTopToFoundation(tableauIndex: Int, respectThreshold: Boolean = false): Boolean {
        val game = _game.value
        val pile = game.tableau.getOrNull(tableauIndex) ?: return false

        if (pile.isEmpty()) return false

        val topIndex = pile.size() - 1
        val topCard = pile.peek()

        if (topCard?.isFaceUp != true) return false
        if (respectThreshold && !shouldPreferFoundationOnSingleTap(game, topCard)) return false

        for (i in game.foundations.indices) {
            val updated = game.moveTableauToFoundation(tableauIndex, topIndex, i)
            if (updated != null) {
                animateCardMove(
                    card = topCard,
                    sourceStackType = StackType.TABLEAU,
                    sourceIndex = tableauIndex,
                    sourceCardIndex = topIndex,
                    destStackType = StackType.FOUNDATION,
                    destIndex = i
                )
                clearSingleClickGlow()
                resetHintTimer()
                undoStack.addLast(game)
                redoStack.clear()
                val updatedWithScore = updateAfterMove(updated, scoreDelta = 10)
                _game.value = updatedWithScore
                saveGame()
                updateUndoRedoState()
                return true
            }
        }

        return false
    }

    /**
     * Schedule a card animation for an auto move.
     * GameBoardView must be set for this to work.
     */
    private fun animateCardMove(
        card: Card?,
        sourceStackType: StackType,
        sourceIndex: Int,
        sourceCardIndex: Int,
        destStackType: StackType,
        destIndex: Int
    ) {
        if (!_showCardAnimations.value || card == null || gameBoardView == null) return

        val view = gameBoardView ?: return
        
        // Get source and destination rects
        val sourceRect = view.getCardRectForAnimation(sourceStackType, sourceIndex, sourceCardIndex)
            ?: return
        val destRect = view.getCardRectForAnimation(destStackType, destIndex, -1)
            ?: return

        // Schedule the animation
        view.scheduleCardAnimation(
            card = card,
            startRect = sourceRect,
            endRect = destRect,
            destStackType = destStackType,
            destStackIndex = destIndex
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Hint system
    // ─────────────────────────────────────────────────────────────

    companion object {
        private const val HINT_PHASE_DURATION_MS = 1000L
    }

    /**
     * Called on player activity to dismiss any currently running manual hint cycle.
     */
    fun resetHintTimer() {
        hintCycleJob?.cancel()
        _hintDisplayState.value = null
    }

    /**
     * Pause/dismiss manual hint display during non-player activity (ad, animation, win dialog…).
     */
    fun pauseHintTimerForNonPlayerActivity() {
        hintCycleJob?.cancel()
        _hintDisplayState.value = null
    }

    /** Manual hints do not auto-resume after non-player activity. */
    fun resumeHintTimerAfterNonPlayerActivity() {
        // Intentionally no-op for manual-only hints.
    }

    fun showManualHints(): Boolean {
        if (_game.value.status != GameStatus.IN_PROGRESS) return false

        val moves = computeLegalMoves()
        if (moves.isEmpty()) {
            hintCycleJob?.cancel()
            _hintDisplayState.value = null
            return false
        }

        clearSingleClickGlow()
        startHintCycle(moves)
        return true
    }

    private fun startHintCycle(moves: List<HintMove>) {
        hintCycleJob?.cancel()
        hintCycleJob = viewModelScope.launch {

            for (move in moves) {
                // Phase 1 – source card glows alone (1 s)
                _hintDisplayState.value = HintDisplayState(move, HintPhase.SOURCE_ONLY)
                delay(HINT_PHASE_DURATION_MS)

                // Phase 2 – source + destination glow together (1 s)
                _hintDisplayState.value = HintDisplayState(move, HintPhase.SOURCE_AND_DEST)
                delay(HINT_PHASE_DURATION_MS)

                // Phase 3 – only destination glows (1 s)
                _hintDisplayState.value = HintDisplayState(move, HintPhase.DEST_ONLY)
                delay(HINT_PHASE_DURATION_MS)
            }

            _hintDisplayState.value = null
        }
    }

    /**
     * Computes every legal move available in the current game state.
     * Order: waste→foundation, waste→tableau, tableau→foundation, tableau→tableau.
     */
    private fun computeLegalMoves(): List<HintMove> {
        val game = _game.value
        val moves = mutableListOf<HintMove>()

        // ── Waste → Foundation ───────────────────────────────────
        if (game.waste.peek() != null) {
            for (fi in game.foundations.indices) {
                if (game.moveWasteToFoundation(fi) != null) {
                    moves.add(HintMove(StackType.WASTE, 0, -1, StackType.FOUNDATION, fi))
                    break // Only one foundation accepts a given card
                }
            }

            // ── Waste → Tableau ──────────────────────────────────
            for (ti in game.tableau.indices) {
                if (game.moveWasteToTableau(ti) != null) {
                    moves.add(HintMove(StackType.WASTE, 0, -1, StackType.TABLEAU, ti))
                }
            }
        }

        // ── Tableau → Foundation / Tableau ──────────────────────
        for (fromIdx in game.tableau.indices) {
            val cards = game.tableau[fromIdx].asList()
            for (cardIdx in cards.indices) {
                if (!cards[cardIdx].isFaceUp) continue

                // Top card → Foundation
                if (cardIdx == cards.lastIndex) {
                    for (fi in game.foundations.indices) {
                        if (game.moveTableauToFoundation(fromIdx, cardIdx, fi) != null) {
                            moves.add(HintMove(StackType.TABLEAU, fromIdx, cardIdx, StackType.FOUNDATION, fi))
                            break
                        }
                    }
                }

                // Any face-up card (+ sequence below) → another Tableau pile
                for (toIdx in game.tableau.indices) {
                    if (toIdx == fromIdx) continue
                    if (game.moveTableauToTableau(fromIdx, cardIdx, toIdx) != null) {
                        moves.add(HintMove(StackType.TABLEAU, fromIdx, cardIdx, StackType.TABLEAU, toIdx))
                    }
                }
            }
        }

        return moves
    }

    private fun lowestFoundationValue(game: Game): Int {
        return game.foundations.minOf { foundation ->
            foundation.peek()?.rank?.sortOrder ?: 0
        }
    }

    private fun shouldPreferFoundationOnSingleTap(game: Game, card: Card): Boolean {
        val lowestFoundation = lowestFoundationValue(game)
        return (card.rank.sortOrder - lowestFoundation) < 3
    }

    // ─────────────────────────────────────────────────────────────
    // Single-click glow system (independent from hints)
    // ─────────────────────────────────────────────────────────────

    /**
     * Single-click on waste card:
     * - If can move to foundation only → move there immediately.
     * - If can move to tableau only → move immediately to first valid tableau.
     * - If can move to both → apply the same threshold rule used for top tableau cards:
     *     prefer foundation only when the card is within 2 ranks of the lowest foundation card;
     *     otherwise move to the first valid tableau.
     * - If can move to multiple tableaus → move immediately to first valid tableau.
     */
    fun handleSingleClickOnWaste() {
        val game = _game.value
        val wasteCard = game.waste.peek() ?: return

        // Collect valid destinations
        val canFoundation = (0..3).any { game.moveWasteToFoundation(it) != null }
        val tableauDests = (0..6).filter { game.moveWasteToTableau(it) != null }

        val shouldPreferFoundation = canFoundation && shouldPreferFoundationOnSingleTap(game, wasteCard)

        when {
            // Foundation only and threshold prefers foundation → move there.
            canFoundation && tableauDests.isEmpty() && shouldPreferFoundation -> {
                for (i in game.foundations.indices) {
                    if (tryMoveWasteToFoundation(i)) return
                }
            }
            // Tableau only → move to first valid tableau
            tableauDests.isNotEmpty() && !canFoundation -> {
                tryMoveWasteToTableau(tableauDests[0])
            }
            // Both available → use the same threshold rule as top-tableau cards
            canFoundation && tableauDests.isNotEmpty() -> {
                if (shouldPreferFoundation) {
                    for (i in game.foundations.indices) {
                        if (tryMoveWasteToFoundation(i)) return
                    }
                } else {
                    tryMoveWasteToTableau(tableauDests[0])
                }
            }
            // Foundation-only but threshold does not prefer foundation → require explicit confirm tap.
            canFoundation -> {
                _singleClickGlowState.value = SingleClickGlowState(
                    sourceStackType = StackType.WASTE,
                    sourceStackIndex = 0,
                    sourceCardIndex = -1,
                    destinations = listOf(GlowDestination(StackType.FOUNDATION, game.foundations.indices.first { game.moveWasteToFoundation(it) != null }))
                )
            }
        }
    }

    /**
     * Single-click on tableau card at [tappedCardIndex]:
     * - Uses tapped face-up card when valid, otherwise falls back to top face-up card.
     * - Any tableau-only move (no foundation destination) moves immediately
     *   to the first valid tableau destination.
     * - Top cards prefer foundation when the card is within the lowest-foundation
     *   threshold rule; otherwise they keep the richer tableau/glow behavior.
     */
    fun handleSingleClickOnTableau(tableauIndex: Int, tappedCardIndex: Int) {
        val game = _game.value
        val pile = game.tableau.getOrNull(tableauIndex) ?: return
        val cards = pile.asList()

        // Find the top face-up card
        var topFaceUpIndex = -1
        for (i in cards.indices.reversed()) {
            if (cards[i].isFaceUp) {
                topFaceUpIndex = i
                break
            }
        }
        if (topFaceUpIndex < 0) return

        // Use the tapped card when it is a valid face-up card in this pile.
        val sourceIndex = if (
            tappedCardIndex in cards.indices && cards[tappedCardIndex].isFaceUp
        ) {
            tappedCardIndex
        } else {
            topFaceUpIndex
        }

        // Check tableau destinations (any face-up card can go)
        val tableauDests = (0..6).filter { destIdx ->
            destIdx != tableauIndex && game.moveTableauToTableau(tableauIndex, sourceIndex, destIdx) != null
        }

        // Foundation move is only possible from the top exposed card.
        val isTopCard = sourceIndex == cards.lastIndex
        val canFoundation = isTopCard && (0..3).any { foundIdx ->
            game.moveTableauToFoundation(tableauIndex, sourceIndex, foundIdx) != null
        }
        val shouldPreferFoundation = isTopCard && canFoundation && shouldPreferFoundationOnSingleTap(game, cards[sourceIndex])

        when {
            // Non-top runs should move directly to the first valid tableau destination.
            !isTopCard && tableauDests.isNotEmpty() -> {
                tryMoveTableauToTableau(tableauIndex, sourceIndex, tableauDests.first())
            }
            // Top cards should move to foundation immediately when they satisfy the threshold rule.
            shouldPreferFoundation -> {
                for (i in game.foundations.indices) {
                    if (tryMoveTableauToFoundation(tableauIndex, sourceIndex, i)) return
                }
            }
            // Tableau-only destinations should move immediately to the first valid tableau.
            tableauDests.isNotEmpty() && !canFoundation -> {
                tryMoveTableauToTableau(tableauIndex, sourceIndex, tableauDests.first())
            }
            // Can move to foundation only AND threshold says to prefer foundation → move immediately.
            // If threshold says no, fall through to the glow branch so the user must confirm.
            canFoundation && tableauDests.isEmpty() && shouldPreferFoundation -> {
                for (i in game.foundations.indices) {
                    if (tryMoveTableauToFoundation(tableauIndex, sourceIndex, i)) return
                }
            }
            // Multiple destinations (tableau or foundation) → show all destination glows
            tableauDests.isNotEmpty() || canFoundation -> {
                val dests = mutableListOf<GlowDestination>()
                // Add all valid tableau destinations
                dests.addAll(tableauDests.map { GlowDestination(StackType.TABLEAU, it) })
                // Add foundation destination(s) if available
                if (canFoundation) {
                    for (foundIdx in game.foundations.indices) {
                        if (game.moveTableauToFoundation(tableauIndex, sourceIndex, foundIdx) != null) {
                            dests.add(GlowDestination(StackType.FOUNDATION, foundIdx))
                            break  // Only one foundation can accept a given card
                        }
                    }
                }
                _singleClickGlowState.value = SingleClickGlowState(
                    sourceStackType = StackType.TABLEAU,
                    sourceStackIndex = tableauIndex,
                    sourceCardIndex = sourceIndex,
                    destinations = dests
                )
            }
        }
    }

    /** Clear single-click glow display (called on any player action or when dismissing). */
    fun clearSingleClickGlow() {
        _singleClickGlowState.value = null
    }

    /**
     * Auto-move all possible cards to foundations.
     * Priority: tableau to foundation, then waste to foundation.
     * Returns the number of moves made.
     */
    suspend fun performAutoMove(): Int {
        pauseHintTimerForNonPlayerActivity()
        var moveCount = 0
        var madeMoveThisPass = true

        while (madeMoveThisPass) {
            madeMoveThisPass = false

            for (tableauIndex in _game.value.tableau.indices) {
                if (tryAutoMoveTableauTopToFoundation(tableauIndex, respectThreshold = true)) {
                    moveCount++
                    madeMoveThisPass = true
                    if (_showCardAnimations.value) {
                        delay(CARD_MOVE_ANIMATION_MS)
                    }
                    break
                }
            }

            if (!madeMoveThisPass && tryAutoMoveWasteToFoundation(respectThreshold = true)) {
                moveCount++
                madeMoveThisPass = true
                if (_showCardAnimations.value) {
                    delay(CARD_MOVE_ANIMATION_MS)
                }
            }
        }

        resumeHintTimerAfterNonPlayerActivity()
        return moveCount
    }

    fun tryMoveWasteToFoundation(index: Int): Boolean {
        val game = _game.value
        val updated = game.moveWasteToFoundation(index)

        if (updated != null) {
            clearSingleClickGlow()
            resetHintTimer()
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated, scoreDelta = 10)
            _game.value = updatedWithScore
            saveGame()
            return true
        }

        return false
    }

    fun tryMoveTableauToFoundation(
        tableauIndex: Int,
        cardIndex: Int,
        foundationIndex: Int
    ): Boolean {
        val game = _game.value
        val updated = game.moveTableauToFoundation(tableauIndex, cardIndex, foundationIndex)

        if (updated != null) {
            clearSingleClickGlow()
            resetHintTimer()
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated, scoreDelta = 10)
            _game.value = updatedWithScore
            saveGame()
            updateUndoRedoState()
            return true
        }

        return false
    }

    private fun updateAfterMove(game: Game, scoreDelta: Int = 0): Game {
        startTimerOnFirstSuccessfulMoveIfNeeded()
        return if (game.isWinCondition()) {
            stopTimer()
            val updatedGame = game.copy(
                status = GameStatus.WON,
                score = game.score + scoreDelta,
                moves = game.moves + 1
            )
            // Record the win in stats
            recordGameCompletion(updatedGame, isWin = true)
            updatedGame
        } else {
            game.copy(score = game.score + scoreDelta, moves = game.moves + 1)
        }
    }

    private suspend fun recordGameCompletionInternal(game: Game, isWin: Boolean) {
        // Only record if game hasn't been recorded yet and player made at least 5 moves
        if (currentHandRecorded || game.moves < 5) return

        currentHandRecorded = true

        try {
            val settings = settingsManager.gamePlaySettingsFlow.firstOrNull()
            val playerName = settings?.playerDisplayName
            val cardsDraw = settings?.drawSize
            statsManager.recordGame(
                score = game.score,
                moves = game.moves,
                timeMs = gameTime.value * 1000,  // Convert seconds to milliseconds
                isWin = isWin,
                playerName = playerName,
                cardsDraw = cardsDraw
            )
        } catch (e: Exception) {
            Log.e("GameViewModel", "Failed to record game completion", e)
            currentHandRecorded = false
        }
    }

    /**
     * Record a completed game in the database.
     * Only records if moves >= 5 and game hasn't been recorded yet.
     * Called when a game is won, or when the game is abandoned with enough moves.
     */
    private fun recordGameCompletion(game: Game, isWin: Boolean) {
        viewModelScope.launch {
            recordGameCompletionInternal(game, isWin)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            resetHintTimer()
            val current = _game.value
            redoStack.addLast(current)
            _game.value = undoStack.removeLast()
            saveGame()
            updateUndoRedoState()
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        resetHintTimer()
        val current = _game.value
        undoStack.addLast(current)
        _game.value = redoStack.removeLast()
        saveGame()
        updateUndoRedoState()
    }

    private fun saveGameIfInProgress() {
        if (_game.value.status == GameStatus.IN_PROGRESS) saveGame()
    }

    // Record an in-progress game as a loss before replacing it with a new hand.
    private fun finalizeCurrentGameIfNeeded() {
        val current = _game.value
        if (current.status == GameStatus.IN_PROGRESS) {
            recordGameCompletion(current, isWin = false)
        }
    }

    fun saveGame() {
        viewModelScope.launch {
            try {
                val gameToSave = _game.value.copy(savedGameTime = gameTime.value)
                val json = gson.toJson(gameToSave)
                repository.saveCurrentGameState(json)
                // Mirror session state: true while an in-progress game exists in storage,
                // false once the game has reached a terminal state.
                settingsManager.setGameSessionActive(gameToSave.status == GameStatus.IN_PROGRESS)
            } catch (t: Throwable) {
                Log.w("GameViewModel", "saveGame failed: ${t.message}")
            }
        }
    }

    suspend fun loadSavedGame(): Boolean {
        return try {
            val saved = repository.getCurrentGameState().firstOrNull()
            if (!saved.isNullOrEmpty()) {
                val savedGame = gson.fromJson(saved, Game::class.java)
                if (savedGame.status == GameStatus.IN_PROGRESS) {
                    // Keep a restart anchor even when resuming from persisted state.
                    initialGameState = savedGame
                    _game.value = savedGame
                    gameTime.value = savedGame.savedGameTime
                    hasRegisteredFirstMove = savedGame.savedGameTime > 0
                    currentHandRecorded = false
                    // Keep the session marker aligned with the resumed in-progress game.
                    settingsManager.setGameSessionActive(true)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (t: Throwable) {
            Log.w("GameViewModel", "loadSavedGame failed: ${t.message}", t)
            false
        }
    }

    fun stopGame() {
        val current = _game.value

        // runBlocking ensures save + session-flag write complete before teardown.
        // Leaving the game screen should preserve in-progress sessions for resume,
        // not auto-record them as abandoned losses.
        runBlocking {
            try {
                val gameToSave = current.copy(savedGameTime = gameTime.value)
                val json = gson.toJson(gameToSave)
                repository.saveCurrentGameState(json)
            } catch (t: Throwable) {
                Log.w("GameViewModel", "stopGame save failed: ${t.message}")
            }
            settingsManager.setGameSessionActive(current.status == GameStatus.IN_PROGRESS)
        }
    }

    /**
     * Convenience: locate the stack that contains a specific card instance.
     * Used by CardStackUIManager if needed.
     */
    fun findStackContainingCard(card: com.gpgamelab.justpatience.model.Card)
            : Pair<StackType, Int>? {
        val g = _game.value
        if (g.waste.peek() == card) return Pair(StackType.WASTE, 0)
        if (g.stock.peek() == card) return Pair(StackType.STOCK, 0)
        g.foundations.forEachIndexed { idx, pile ->
            if (pile.peek() == card) return Pair(StackType.FOUNDATION, idx)
            if (pile.asList().contains(card)) return Pair(StackType.FOUNDATION, idx)
        }
        g.tableau.forEachIndexed { idx, pile ->
            if (pile.asList().contains(card)) return Pair(StackType.TABLEAU, idx)
        }
        return null
    }

    fun tryRecycleWasteToStock(): Boolean {
        val game = _game.value

        if (!canRecycleWaste(game)) return false

        val updated = game.recycleWasteToStock()

        if (updated != null) {
            clearSingleClickGlow()
            resetHintTimer()
            undoStack.addLast(game)
            redoStack.clear()
            startTimerOnFirstSuccessfulMoveIfNeeded()
            _game.value = updated.copy(recycleCountUsed = game.recycleCountUsed + 1)
            saveGame()
            updateUndoRedoState()
            return true
        }

        return false
    }
}

private class CardRankAdapter : JsonSerializer<CardRank>, JsonDeserializer<CardRank> {
    override fun serialize(src: CardRank?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        if (src == null) return JsonPrimitive("JOKER")
        return when (src) {
            Joker -> JsonPrimitive("JOKER")
            is StandardRank -> JsonPrimitive(src.name)
        }
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): CardRank {
        if (json == null || json.isJsonNull) return Joker

        if (json.isJsonPrimitive) {
            val token = json.asString
            return parseRankToken(token)
        }

        if (json.isJsonObject) {
            val obj = json.asJsonObject

            // Newer compact format fallback: rank token fields
            if (obj.has("name")) {
                return parseRankToken(obj.get("name").asString)
            }
            if (obj.has("abbreviation")) {
                return parseRankToken(obj.get("abbreviation").asString)
            }

            // Legacy object-shaped fallback from earlier Gson writes
            if (obj.has("sortOrder")) {
                val sortOrder = obj.get("sortOrder").asInt
                if (sortOrder == 0) return Joker
                return StandardRank.entries.firstOrNull { it.sortOrder == sortOrder }
                    ?: throw JsonParseException("Unknown rank sortOrder: $sortOrder")
            }
            if (obj.has("displayName")) {
                return parseRankToken(obj.get("displayName").asString)
            }
        }

        throw JsonParseException("Unsupported CardRank json: $json")
    }

    private fun parseRankToken(raw: String): CardRank {
        val token = raw.trim().uppercase(Locale.US)
        if (token == "JOKER" || token == "JK" || token == "0") return Joker

        StandardRank.entries.firstOrNull { it.name == token }?.let { return it }
        StandardRank.entries.firstOrNull { it.abbreviation.uppercase(Locale.US) == token }?.let { return it }

        val numeric = token.toIntOrNull()
        if (numeric != null) {
            StandardRank.entries.firstOrNull { it.sortOrder == numeric }?.let { return it }
        }

        throw JsonParseException("Unknown CardRank token: $raw")
    }
}

