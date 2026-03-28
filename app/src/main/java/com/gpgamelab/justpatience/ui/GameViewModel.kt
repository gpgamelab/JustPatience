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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Type
import java.util.Locale

private const val CARD_MOVE_ANIMATION_MS = 250L
private const val GAME_PERSIST_TAG = "GamePersist"

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

    private val _showCardAnimations = MutableStateFlow(true)
    val showCardAnimations: StateFlow<Boolean> = _showCardAnimations

    private val _showWinAnimation = MutableStateFlow(true)
    val showWinAnimation: StateFlow<Boolean> = _showWinAnimation

    private val _isMirroredLayout = MutableStateFlow(false)
    val isMirroredLayout: StateFlow<Boolean> = _isMirroredLayout

    private val _allowFoundationToTableauDrag = MutableStateFlow(false)
    val allowFoundationToTableauDrag: StateFlow<Boolean> = _allowFoundationToTableauDrag

    // Hint system
    private val _showHints = MutableStateFlow(true)
    private val _hintDelaySeconds = MutableStateFlow(5)
    private val _hintDisplayState = MutableStateFlow<HintDisplayState?>(null)
    val hintDisplayState: StateFlow<HintDisplayState?> = _hintDisplayState

    private var hintInactivityJob: Job? = null
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
                Log.d(GAME_PERSIST_TAG, "initializeForLaunch: force new game")
                startNewGameInternal()
                updateUndoRedoState()
                resetHintTimer()
                return@launch
            }

            Log.d(GAME_PERSIST_TAG, "initializeForLaunch: checking for saved game to resume")
            val restored = loadSavedGame()
            if (restored) {
                Log.d(GAME_PERSIST_TAG, "initializeForLaunch: resumed saved in-progress game ${summarizeGame(_game.value)}")
                updateUndoRedoState()
                resetHintTimer()
            } else {
                Log.d(GAME_PERSIST_TAG, "initializeForLaunch: no resumable saved game found, starting fresh hand")
                startNewGameInternal()
                updateUndoRedoState()
                resetHintTimer()
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
        Log.d(GAME_PERSIST_TAG, "startNewGameInternal: created fresh hand ${summarizeGame(newGame)}")
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
                _showWinAnimation.value = settings.showWinAnimation
                _isMirroredLayout.value = settings.boardLayout == "left_hand"
                _allowFoundationToTableauDrag.value = settings.allowFoundationToTableauDrag

                val prevShowHints = _showHints.value
                _showHints.value = settings.showHints
                _hintDelaySeconds.value = settings.hintDelaySeconds
                if (!settings.showHints && prevShowHints) {
                    pauseHintTimerForNonPlayerActivity()
                }
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
        Log.d(GAME_PERSIST_TAG, "startNewGame: requested while current=${summarizeGame(_game.value)}")
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
            Log.d(GAME_PERSIST_TAG, "startNewGame: switched to fresh hand ${summarizeGame(newGame)}")
            saveGame()
        }
    }

    fun restartGame() {
        Log.d(GAME_PERSIST_TAG, "restartGame: requested while current=${summarizeGame(_game.value)}")
        finalizeCurrentGameIfNeeded()
        pauseHintTimerForNonPlayerActivity()
        val initial = initialGameState ?: return
        undoStack.clear()
        redoStack.clear()
        _game.value = initial
        resetTimerForNewHand()
        currentHandRecorded = false
        updateUndoRedoState()
        Log.d(GAME_PERSIST_TAG, "restartGame: restored initial hand ${summarizeGame(initial)}")
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

    fun tryAutoMoveWasteToFoundation(): Boolean {
        val game = _game.value

        for (i in game.foundations.indices) {
            val updated = game.moveWasteToFoundation(i)
            if (updated != null) {
                animateCardMove(
                    card = game.waste.peek(),
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

    fun tryAutoMoveTableauTopToFoundation(tableauIndex: Int): Boolean {
        val game = _game.value
        val pile = game.tableau.getOrNull(tableauIndex) ?: return false

        if (pile.isEmpty()) return false

        val topIndex = pile.size() - 1
        val topCard = pile.peek()

        if (topCard?.isFaceUp != true) return false

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
     * Called on any detectable player activity (touch, undo, redo, draw, move…).
     * Cancels any running hint display, then re-arms the inactivity countdown.
     */
    fun resetHintTimer() {
        hintInactivityJob?.cancel()
        hintCycleJob?.cancel()
        _hintDisplayState.value = null

        if (!_showHints.value) return
        if (_game.value.status != GameStatus.IN_PROGRESS) return

        hintInactivityJob = viewModelScope.launch {
            delay(_hintDelaySeconds.value * 1000L)
            startHintCycle()
        }
    }

    /**
     * Pause hint timers during non-player activity (ad, animation, win dialog…).
     * Call [resumeHintTimerAfterNonPlayerActivity] when control returns to the player.
     */
    fun pauseHintTimerForNonPlayerActivity() {
        hintInactivityJob?.cancel()
        hintCycleJob?.cancel()
        _hintDisplayState.value = null
    }

    /** Re-arms the inactivity countdown from scratch after non-player activity ends. */
    fun resumeHintTimerAfterNonPlayerActivity() {
        resetHintTimer()
    }

    private fun startHintCycle() {
        hintCycleJob?.cancel()
        hintCycleJob = viewModelScope.launch {
            val moves = computeLegalMoves()
            if (moves.isEmpty()) return@launch

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

    // ─────────────────────────────────────────────────────────────
    // Single-click glow system (independent from hints)
    // ─────────────────────────────────────────────────────────────

    /**
     * Single-click on waste card:
     * - If can move to foundation only → move immediately
     * - If can move to tableau only → move immediately
     * - If can move to both → move to tableau immediately
     * - If can move to multiple tableaus → show glow destinations, don't move
     */
    fun handleSingleClickOnWaste() {
        val game = _game.value
        val wasteCard = game.waste.peek() ?: return

        // Collect valid destinations
        val canFoundation = (0..3).any { game.moveWasteToFoundation(it) != null }
        val tableauDests = (0..6).filter { game.moveWasteToTableau(it) != null }

        when {
            // Can move to foundation only → move there
            canFoundation && tableauDests.isEmpty() -> {
                for (i in game.foundations.indices) {
                    if (tryMoveWasteToFoundation(i)) return
                }
            }
            // Can move to exactly one tableau and no foundation → move there
            tableauDests.size == 1 && !canFoundation -> {
                tryMoveWasteToTableau(tableauDests[0])
            }
            // Can move to one tableau + foundation exists → move to tableau
            tableauDests.size == 1 && canFoundation -> {
                tryMoveWasteToTableau(tableauDests[0])
            }
            // Can move to multiple tableaus → show glows
            tableauDests.size > 1 -> {
                val dests = tableauDests.map { GlowDestination(StackType.TABLEAU, it) }
                _singleClickGlowState.value = SingleClickGlowState(
                    sourceStackType = StackType.WASTE,
                    sourceStackIndex = 0,
                    sourceCardIndex = -1,
                    destinations = dests
                )
            }
        }
    }

    /**
     * Single-click on tableau card at [cardIndex]:
     * - If can move to exactly one tableau and no foundation → move immediately
     * - If can move to multiple tableaus OR to foundation → show all tableau destination glows
     */
    fun handleSingleClickOnTableau(tableauIndex: Int) {
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

        // Check tableau destinations (any face-up card can go)
        val tableauDests = (0..6).filter { destIdx ->
            destIdx != tableauIndex && game.moveTableauToTableau(tableauIndex, topFaceUpIndex, destIdx) != null
        }

        // Check foundation destination (only top face-up card)
        val canFoundation = (0..3).any { foundIdx ->
            game.moveTableauToFoundation(tableauIndex, topFaceUpIndex, foundIdx) != null
        }

        when {
            // Can move to exactly one tableau and no foundation → move immediately
            tableauDests.size == 1 && !canFoundation -> {
                tryMoveTableauToTableau(tableauIndex, topFaceUpIndex, tableauDests[0])
            }
            // Can move to foundation only → move immediately
            canFoundation && tableauDests.isEmpty() -> {
                for (i in game.foundations.indices) {
                    if (tryMoveTableauToFoundation(tableauIndex, topFaceUpIndex, i)) return
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
                        if (game.moveTableauToFoundation(tableauIndex, topFaceUpIndex, foundIdx) != null) {
                            dests.add(GlowDestination(StackType.FOUNDATION, foundIdx))
                            break  // Only one foundation can accept a given card
                        }
                    }
                }
                _singleClickGlowState.value = SingleClickGlowState(
                    sourceStackType = StackType.TABLEAU,
                    sourceStackIndex = tableauIndex,
                    sourceCardIndex = topFaceUpIndex,
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
                if (tryAutoMoveTableauTopToFoundation(tableauIndex)) {
                    moveCount++
                    madeMoveThisPass = true
                    if (_showCardAnimations.value) {
                        delay(CARD_MOVE_ANIMATION_MS)
                    }
                    break
                }
            }

            if (!madeMoveThisPass && tryAutoMoveWasteToFoundation()) {
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
                Log.d(
                    GAME_PERSIST_TAG,
                    "saveGame: persisted ${summarizeGame(gameToSave)} jsonLength=${json.length} sessionActive=${gameToSave.status == GameStatus.IN_PROGRESS}"
                )
            } catch (t: Throwable) {
                Log.w("GameViewModel", "saveGame failed: ${t.message}")
                Log.w(GAME_PERSIST_TAG, "saveGame failed for ${summarizeGame(_game.value)}: ${t.message}")
            }
        }
    }

    suspend fun loadSavedGame(): Boolean {
        return try {
            val saved = repository.getCurrentGameState().firstOrNull()
            Log.d(
                GAME_PERSIST_TAG,
                "loadSavedGame: rawSavedPresent=${!saved.isNullOrEmpty()} jsonLength=${saved?.length ?: 0}"
            )
            if (!saved.isNullOrEmpty()) {
                val savedGame = gson.fromJson(saved, Game::class.java)
                Log.d(GAME_PERSIST_TAG, "loadSavedGame: parsed ${summarizeGame(savedGame)}")
                if (savedGame.status == GameStatus.IN_PROGRESS) {
                    _game.value = savedGame
                    gameTime.value = savedGame.savedGameTime
                    hasRegisteredFirstMove = savedGame.savedGameTime > 0
                    currentHandRecorded = false
                    // Keep the session marker aligned with the resumed in-progress game.
                    settingsManager.setGameSessionActive(true)
                    Log.d(GAME_PERSIST_TAG, "loadSavedGame: resume accepted")
                    true
                } else {
                    Log.d(GAME_PERSIST_TAG, "loadSavedGame: saved game not resumable because status=${savedGame.status}")
                    false
                }
            } else {
                Log.d(GAME_PERSIST_TAG, "loadSavedGame: no saved JSON found")
                false
            }
        } catch (t: Throwable) {
            Log.w(GAME_PERSIST_TAG, "loadSavedGame failed: ${t.message}", t)
            false
        }
    }

    fun stopGame() {
        val current = _game.value
        Log.d(GAME_PERSIST_TAG, "stopGame: activity finishing with current=${summarizeGame(current)}")

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
            Log.d(
                GAME_PERSIST_TAG,
                "stopGame: sessionActive=${current.status == GameStatus.IN_PROGRESS} savedCurrent=${summarizeGame(current)}"
            )
        }
    }

    private fun summarizeGame(game: Game): String {
        return "status=${game.status}, moves=${game.moves}, score=${game.score}, stock=${game.stock.size()}, waste=${game.waste.size()}, recycle=${game.recycleCountUsed}"
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

