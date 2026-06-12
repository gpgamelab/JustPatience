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
import com.google.gson.JsonParser
import com.gpgamelab.justpatience.data.GameStatsManager
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.data.TokenManager
import com.gpgamelab.justpatience.game.GameController
import com.gpgamelab.justpatience.model.Card
import com.gpgamelab.justpatience.model.CardRank
import com.gpgamelab.justpatience.model.CardSuit
import com.gpgamelab.justpatience.model.Game
import com.gpgamelab.justpatience.model.GameStatus
import com.gpgamelab.justpatience.model.GlowDestination
import com.gpgamelab.justpatience.model.HintDisplayState
import com.gpgamelab.justpatience.model.HintMove
import com.gpgamelab.justpatience.model.HintPhase
import com.gpgamelab.justpatience.model.FoundationPile
import com.gpgamelab.justpatience.model.SingleClickGlowState
import com.gpgamelab.justpatience.model.ScoreCalculator
import com.gpgamelab.justpatience.model.ScoreMethod
import com.gpgamelab.justpatience.model.StackType
import com.gpgamelab.justpatience.model.StandardRank
import com.gpgamelab.justpatience.model.Joker
import com.gpgamelab.justpatience.model.Stock
import com.gpgamelab.justpatience.model.TableauPile
import com.gpgamelab.justpatience.model.Waste
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

private data class SavedGameEnvelope(
    val currentGame: Game,
    val initialGameState: Game?
)

/**
 * ViewModel that holds the current Game state and provides actions for UI.
 * Calls the GameController (rules engine) for all moves.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private data class WandCandidate(
        val sourceType: StackType,          // TABLEAU, STOCK, or WASTE
        val sourceTableauIndex: Int,        // meaningful only for TABLEAU
        val sourceCardIndex: Int,           // meaningful only for TABLEAU / STOCK
        val card: Card
    )

    /** Public representation of a magic-wand candidate card, used for picker dialogs. */
    data class MagicWandCandidate(
        val card: Card,
        val sourceType: StackType,
        val sourceTableauIndex: Int,
        val sourceCardIndex: Int
    )

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
    private var currentDeckCount = Game.DEFAULT_DECK_COUNT
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

    private val _scoreMethod = MutableStateFlow(ScoreMethod.WINDOWS)
    val scoreMethod: StateFlow<String> = _scoreMethod

    private val _showCardAnimations = MutableStateFlow(true)
    val showCardAnimations: StateFlow<Boolean> = _showCardAnimations

    private val _showWinAnimation = MutableStateFlow(true)
    val showWinAnimation: StateFlow<Boolean> = _showWinAnimation

    private val _isMirroredLayout = MutableStateFlow(false)
    val isMirroredLayout: StateFlow<Boolean> = _isMirroredLayout

    private val _allowFoundationToTableauDrag = MutableStateFlow(false)
    val allowFoundationToTableauDrag: StateFlow<Boolean> = _allowFoundationToTableauDrag

    private val _enforceFoundationBalanceEnabled = MutableStateFlow(false)
    val enforceFoundationBalanceEnabled: StateFlow<Boolean> = _enforceFoundationBalanceEnabled

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
    private var initialGameState: Game? = null  // Start-of-hand anchor used by Restart.
    private var currentHandRecorded = false
    private var launchInitialized = false
    // Two-phase recycle state: stage waste cards first, commit to stock after shuffle SFX.
    private var pendingRecycleCards: List<Card>? = null
    private var pendingRecycleSourceGame: Game? = null

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
        clearPendingRecycleState()
        undoStack.clear()
        redoStack.clear()
        currentHandRecorded = false
        val newGame = createNewGameWithScoring()
        initialGameState = newGame.deepCopy()
        _game.value = newGame
        resetTimerForNewHand()
        saveGame()
    }

    private fun observeDrawSizeSetting() {
        viewModelScope.launch {
            settingsManager.gamePlaySettingsFlow.collect { settings ->
                currentDrawSize = settings.drawSize
                currentDeckCount = Game.normalizeDeckCount(settings.deckCount)
                currentRecycleLimit = settings.recycleCount.coerceAtLeast(0)
                currentInfiniteRecycles = settings.infiniteRecycles

                _drawCountForDisplay.value = normalizeDrawCount(currentDrawSize)
                _recycleLimitForDisplay.value = currentRecycleLimit
                _isInfiniteRecycles.value = currentInfiniteRecycles
                _showCardAnimations.value = settings.showCardAnimations
                _showGameTimer.value = settings.showGameTimer
                _showScore.value = settings.showScore
                _showMoves.value = settings.showMoves
                _scoreMethod.value = ScoreMethod.normalize(settings.scoreMethod)
                _showWinAnimation.value = settings.showWinAnimation
                _isMirroredLayout.value = settings.boardLayout == "left_hand"
                _allowFoundationToTableauDrag.value = settings.allowFoundationToTableauDrag
                _enforceFoundationBalanceEnabled.value = settings.enforceFoundationBalance
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
        clearPendingRecycleState()
        pauseHintTimerForNonPlayerActivity()
        undoStack.clear()
        redoStack.clear()
        resetTimerForNewHand()
        currentHandRecorded = false
        viewModelScope.launch {
            finalizeCurrentGameIfNeededSync()
            val newGame = createNewGameWithScoring()
            initialGameState = newGame.deepCopy()
            _game.value = newGame
            saveGame()
        }
    }

    fun switchDeckCountAndStartNewGame(deckCount: Int) {
        currentDeckCount = Game.normalizeDeckCount(deckCount)
        startNewGame()
    }

    fun restartGame() {
        clearPendingRecycleState()
        pauseHintTimerForNonPlayerActivity()
        val wasUnlocked = _game.value.extraTableauUnlocked
        viewModelScope.launch {
            finalizeCurrentGameIfNeededSync()

            val initial = initialGameState
            val restartedGame = if (initial == null) {
                createNewGameWithScoring().copy(extraTableauUnlocked = wasUnlocked)
            } else {
                val vegasBase = settingsManager.getVegasCumulativeBankroll()
                withScoringChannels(
                    initial.copy(
                        status = GameStatus.IN_PROGRESS,
                        moves = 0,
                        windowsScore = 0,
                        score = 0,
                        vegasCumulativeBase = vegasBase,
                        extraTableauUnlocked = wasUnlocked
                    ),
                    windowsScore = 0
                )
            }

            undoStack.clear()
            redoStack.clear()
            currentHandRecorded = false
            initialGameState = restartedGame.deepCopy()
            _game.value = restartedGame
            resetTimerForNewHand()
            updateUndoRedoState()
            saveGame()
        }
    }

    enum class DrawResult { NORMAL_DRAW, RECYCLE_SHUFFLE, NO_MOVE }

    fun drawFromStock(): DrawResult {
        val game = _game.value
        if (pendingRecycleCards != null) return DrawResult.NO_MOVE

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
            // Stage recycle first so UI can show emptied waste while shuffle SFX plays.
            if (!game.waste.isEmpty() && canRecycleWaste(game)) {
                val (newWaste, recycled) = game.waste.withAllCardsTaken()
                if (recycled == null) return DrawResult.NO_MOVE

                pendingRecycleCards = recycled
                pendingRecycleSourceGame = game
                _game.value = game.copy(waste = newWaste)
                return DrawResult.RECYCLE_SHUFFLE
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
        return when {
            !moved   -> DrawResult.NO_MOVE
            else     -> DrawResult.NORMAL_DRAW
        }
    }

    /**
     * Commits waste->stock recycle after shuffle sound sequence completes.
     * Returns true only when recycle is successfully applied.
     */
    fun completeRecycleAfterShuffleSound(): Boolean {
        val sourceGame = pendingRecycleSourceGame ?: return false
        val stagedCards = pendingRecycleCards ?: return false
        val game = _game.value

        if (!game.stock.isEmpty() || !game.waste.isEmpty() || !canRecycleWaste(sourceGame)) {
            clearPendingRecycleState()
            return false
        }

        val cardsToStock = stagedCards.reversed().map { it.copy(isFaceUp = false) }
        val newStock = game.stock.withCardsAdded(cardsToStock)
        val updated = game.copy(stock = newStock, recycleCountUsed = sourceGame.recycleCountUsed + 1)
        clearPendingRecycleState()

        clearSingleClickGlow()
        undoStack.addLast(sourceGame)
        redoStack.clear()
        _game.value = updateAfterMove(updated)
        updateUndoRedoState()
        return true
    }

    private fun clearPendingRecycleState() {
        pendingRecycleCards = null
        pendingRecycleSourceGame = null
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
                    val scoreDelta = theUpdatedGame.score - game.score
                    val updatedWithMoves = updateAfterMove(
                        theUpdatedGame.copy(
                            score = game.score,
                            windowsScore = game.windowsScore,
                            moves = game.moves
                        ),
                        scoreDelta = scoreDelta
                    )
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

    fun tryMoveWasteToTableau(index: Int, animate: Boolean = true): Boolean {
        val game = _game.value
        val wasteCard = game.waste.peek() ?: return false
        val updated = game.moveWasteToTableau(index)

        if (updated != null) {
            if (animate) {
                animateCardMove(
                    card = wasteCard,
                    sourceStackType = StackType.WASTE,
                    sourceIndex = 0,
                    sourceCardIndex = -1,
                    destStackType = StackType.TABLEAU,
                    destIndex = index
                )
            }
            clearSingleClickGlow()
            resetHintTimer()
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated, scoreDelta = 5)
            _game.value = updatedWithScore
            saveGame()
            updateUndoRedoState()
            return true
        }
        return false
    }

    fun tryMoveTableauToTableau(
        fromIndex: Int,
        cardIndex: Int,
        toIndex: Int,
        animate: Boolean = true
    ): Boolean {
        val game = _game.value
        val pile = game.tableau.getOrNull(fromIndex) ?: return false
        if (pile.isEmpty()) return false
        val sourceCards = pile.asList()
        val movingCard = sourceCards.getOrNull(cardIndex) ?: return false
        val movedCardCount = sourceCards.size - cardIndex
        val updated = game.moveTableauToTableau(fromIndex, cardIndex, toIndex)

        if (updated != null) {
            if (animate) {
                animateCardMove(
                    card = movingCard,
                    sourceStackType = StackType.TABLEAU,
                    sourceIndex = fromIndex,
                    sourceCardIndex = cardIndex,
                    destStackType = StackType.TABLEAU,
                    destIndex = toIndex,
                    movedCardCount = movedCardCount
                )
            }
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

        if (respectThreshold && !foundationBalanceAllowsMoveToFoundation(game, wasteCard)) {
            return false
        }

        val foundationIndex = firstLegalFoundationIndex(game) { index ->
            game.moveWasteToFoundation(index) != null
        } ?: return false

        val updated = game.moveWasteToFoundation(foundationIndex) ?: return false
        animateCardMove(
            card = wasteCard,
            sourceStackType = StackType.WASTE,
            sourceIndex = 0,
            sourceCardIndex = -1,
            destStackType = StackType.FOUNDATION,
            destIndex = foundationIndex
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

    fun tryAutoMoveTableauTopToFoundation(tableauIndex: Int, respectThreshold: Boolean = false): Boolean {
        val game = _game.value
        val pile = game.tableau.getOrNull(tableauIndex) ?: return false

        if (pile.isEmpty()) return false

        val topIndex = pile.size() - 1
        val topCard = pile.peek()

        if (topCard?.isFaceUp != true) return false
        if (respectThreshold && !foundationBalanceAllowsMoveToFoundation(game, topCard)) return false

        val foundationIndex = firstLegalFoundationIndex(game) { index ->
            game.moveTableauToFoundation(tableauIndex, topIndex, index) != null
        } ?: return false

        val updated = game.moveTableauToFoundation(tableauIndex, topIndex, foundationIndex) ?: return false
        animateCardMove(
            card = topCard,
            sourceStackType = StackType.TABLEAU,
            sourceIndex = tableauIndex,
            sourceCardIndex = topIndex,
            destStackType = StackType.FOUNDATION,
            destIndex = foundationIndex
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
        destIndex: Int,
        movedCardCount: Int = 1
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
            destStackIndex = destIndex,
            movedCardCount = movedCardCount
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

        // -- Waste -> Foundation
        if (game.waste.peek() != null) {
            val wasteFoundationIndex = firstLegalFoundationIndex(game) { foundationIndex ->
                game.moveWasteToFoundation(foundationIndex) != null
            }
            if (wasteFoundationIndex != null) {
                moves.add(HintMove(StackType.WASTE, 0, -1, StackType.FOUNDATION, wasteFoundationIndex))
            }

            // -- Waste -> Tableau
            for (ti in game.tableau.indices) {
                if (game.moveWasteToTableau(ti) != null) {
                    moves.add(HintMove(StackType.WASTE, 0, -1, StackType.TABLEAU, ti))
                }
            }
        }

        // -- Tableau -> Foundation / Tableau
        for (fromIdx in game.tableau.indices) {
            val cards = game.tableau[fromIdx].asList()
            for (cardIdx in cards.indices) {
                if (!cards[cardIdx].isFaceUp) continue

                if (cardIdx == cards.lastIndex) {
                    val tableauFoundationIndex = firstLegalFoundationIndex(game) { foundationIndex ->
                        game.moveTableauToFoundation(fromIdx, cardIdx, foundationIndex) != null
                    }
                    if (tableauFoundationIndex != null) {
                        moves.add(HintMove(StackType.TABLEAU, fromIdx, cardIdx, StackType.FOUNDATION, tableauFoundationIndex))
                    }
                }

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

    private fun foundationBalanceAllowsMoveToFoundation(game: Game, card: Card): Boolean {
        return !_enforceFoundationBalanceEnabled.value || shouldPreferFoundationOnSingleTap(game, card)
    }

    /** Returns the first legal foundation index in stable slot order, or null if none. */
    private inline fun firstLegalFoundationIndex(game: Game, canMoveToIndex: (Int) -> Boolean): Int? {
        for (foundationIndex in game.foundations.indices) {
            if (canMoveToIndex(foundationIndex)) return foundationIndex
        }
        return null
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
    fun handleSingleClickOnWaste(): Boolean {
        val game = _game.value
        val wasteCard = game.waste.peek() ?: return false

        val foundationDestIndex = firstLegalFoundationIndex(game) { foundationIndex ->
            game.moveWasteToFoundation(foundationIndex) != null
        }
        val canFoundation = foundationDestIndex != null
        val tableauDests = game.tableau.indices.filter { game.moveWasteToTableau(it) != null }

        val shouldPreferFoundation = canFoundation && foundationBalanceAllowsMoveToFoundation(game, wasteCard)

        when {
            canFoundation && tableauDests.isEmpty() && shouldPreferFoundation -> {
                return tryMoveWasteToFoundation(foundationDestIndex ?: return false)
            }
            tableauDests.isNotEmpty() && !canFoundation -> {
                return tryMoveWasteToTableau(tableauDests[0])
            }
            canFoundation && tableauDests.isNotEmpty() -> {
                return if (shouldPreferFoundation) {
                    tryMoveWasteToFoundation(foundationDestIndex ?: return false)
                } else {
                    tryMoveWasteToTableau(tableauDests[0])
                }
            }
            canFoundation -> {
                _singleClickGlowState.value = SingleClickGlowState(
                    sourceStackType = StackType.WASTE,
                    sourceStackIndex = 0,
                    sourceCardIndex = -1,
                    destinations = listOf(GlowDestination(StackType.FOUNDATION, foundationDestIndex ?: return false))
                )
                return false
            }
        }
        return false
    }

    /**
     * Single-click on tableau card at [tappedCardIndex]:
     * - Uses tapped face-up card when valid, otherwise falls back to top face-up card.
     * - Any tableau-only move (no foundation destination) moves immediately
     *   to the first valid tableau destination.
     * - Top cards prefer foundation when the card is within the lowest-foundation
     *   threshold rule; otherwise they keep the richer tableau/glow behavior.
     */
    fun handleSingleClickOnTableau(tableauIndex: Int, tappedCardIndex: Int): Boolean {
        val game = _game.value
        val pile = game.tableau.getOrNull(tableauIndex) ?: return false
        val cards = pile.asList()

        var topFaceUpIndex = -1
        for (i in cards.indices.reversed()) {
            if (cards[i].isFaceUp) {
                topFaceUpIndex = i
                break
            }
        }
        if (topFaceUpIndex < 0) return false

        val sourceIndex = if (
            tappedCardIndex in cards.indices && cards[tappedCardIndex].isFaceUp
        ) {
            tappedCardIndex
        } else {
            topFaceUpIndex
        }

        val tableauDests = game.tableau.indices.filter { destIdx ->
            destIdx != tableauIndex && game.moveTableauToTableau(tableauIndex, sourceIndex, destIdx) != null
        }

        val isTopCard = sourceIndex == cards.lastIndex
        val foundationDestIndex = if (isTopCard) {
            firstLegalFoundationIndex(game) { foundationIndex ->
                game.moveTableauToFoundation(tableauIndex, sourceIndex, foundationIndex) != null
            }
        } else {
            null
        }
        val canFoundation = foundationDestIndex != null
        val shouldPreferFoundation = isTopCard && canFoundation && foundationBalanceAllowsMoveToFoundation(game, cards[sourceIndex])

        when {
            // Non-top runs should move directly to the first valid tableau destination.
            !isTopCard && tableauDests.isNotEmpty() -> {
                return tryMoveTableauToTableau(tableauIndex, sourceIndex, tableauDests.first())
            }
            // Top cards should move to foundation immediately when they satisfy the threshold rule.
            shouldPreferFoundation -> {
                return tryMoveTableauToFoundation(tableauIndex, sourceIndex, foundationDestIndex ?: return false)
            }
            // Tableau-only destinations should move immediately to the first valid tableau.
            tableauDests.isNotEmpty() && !canFoundation -> {
                return tryMoveTableauToTableau(tableauIndex, sourceIndex, tableauDests.first())
            }
            // Multiple destinations (tableau or foundation) → show all destination glows
            tableauDests.isNotEmpty() || canFoundation -> {
                val dests = mutableListOf<GlowDestination>()
                // Add all valid tableau destinations
                dests.addAll(tableauDests.map { GlowDestination(StackType.TABLEAU, it) })
                // Add foundation destination(s) if available
                if (canFoundation) {
                    dests.add(GlowDestination(StackType.FOUNDATION, foundationDestIndex ?: return false))
                }
                _singleClickGlowState.value = SingleClickGlowState(
                    sourceStackType = StackType.TABLEAU,
                    sourceStackIndex = tableauIndex,
                    sourceCardIndex = sourceIndex,
                    destinations = dests
                )
                return false
            }
        }
        return false
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
    suspend fun performAutoMove(onCardMoved: (() -> Unit)? = null): Int {
        pauseHintTimerForNonPlayerActivity()
        var moveCount = 0
        var madeMoveThisPass = true

        while (madeMoveThisPass) {
            madeMoveThisPass = false

            for (tableauIndex in _game.value.tableau.indices) {
                if (tryAutoMoveTableauTopToFoundation(tableauIndex, respectThreshold = _enforceFoundationBalanceEnabled.value)) {
                    moveCount++
                    madeMoveThisPass = true
                    onCardMoved?.invoke()
                    if (_showCardAnimations.value) {
                        delay(CARD_MOVE_ANIMATION_MS)
                    }
                    break
                }
            }

            if (!madeMoveThisPass && tryAutoMoveWasteToFoundation(respectThreshold = _enforceFoundationBalanceEnabled.value)) {
                moveCount++
                madeMoveThisPass = true
                onCardMoved?.invoke()
                if (_showCardAnimations.value) {
                    delay(CARD_MOVE_ANIMATION_MS)
                }
            }
        }

        resumeHintTimerAfterNonPlayerActivity()
        return moveCount
    }

    fun tryMoveWasteToFoundation(index: Int, animate: Boolean = true): Boolean {
        val game = _game.value
        val wasteCard = game.waste.peek() ?: return false
        val updated = game.moveWasteToFoundation(index)

        if (updated != null) {
            if (animate) {
                animateCardMove(
                    card = wasteCard,
                    sourceStackType = StackType.WASTE,
                    sourceIndex = 0,
                    sourceCardIndex = -1,
                    destStackType = StackType.FOUNDATION,
                    destIndex = index
                )
            }
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
        foundationIndex: Int,
        animate: Boolean = true
    ): Boolean {
        val game = _game.value
        val pile = game.tableau.getOrNull(tableauIndex) ?: return false
        if (pile.isEmpty()) return false
        val topCard = pile.peek()
        val updated = game.moveTableauToFoundation(tableauIndex, cardIndex, foundationIndex)

        if (updated != null) {
            if (animate) {
                animateCardMove(
                    card = topCard,
                    sourceStackType = StackType.TABLEAU,
                    sourceIndex = tableauIndex,
                    sourceCardIndex = cardIndex,
                    destStackType = StackType.FOUNDATION,
                    destIndex = foundationIndex
                )
            }
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

    fun tryUseMagicWandOnTarget(targetType: StackType, targetIndex: Int, targetCardIndex: Int): Boolean {
        val game = _game.value
        val targetIsValid = when (targetType) {
            StackType.TABLEAU -> {
                val pile = game.tableau.getOrNull(targetIndex) ?: return false
                val targetCard = pile.peekAt(targetCardIndex) ?: return false
                targetCard.isFaceUp
            }
            StackType.FOUNDATION -> {
                val top = game.foundations.getOrNull(targetIndex)?.peek() ?: return false
                top.isFaceUp
            }
            else -> return false
        }
        if (!targetIsValid) return false

        val candidate = findFirstMagicWandCandidate(game, targetType, targetIndex) ?: return false
        val wandAppliedGame = applyWandCandidate(game, candidate, targetType, targetIndex) ?: return false

        clearSingleClickGlow()
        resetHintTimer()
        undoStack.addLast(game)
        redoStack.clear()
        _game.value = updateAfterMove(wandAppliedGame)
        saveGame()
        updateUndoRedoState()
        return true
    }

    /**
     * Shared logic: remove the candidate card from its source, place it face-up on the target.
     * Returns the updated Game on success, or null if the operation cannot be performed.
     */
    private fun applyWandCandidate(
        game: Game,
        candidate: WandCandidate,
        targetType: StackType,
        targetIndex: Int
    ): Game? {
        val candidateFaceUp = candidate.card.copy(isFaceUp = true)

        fun placeOnTarget(baseGame: Game, newTableau: MutableList<TableauPile>? = null): Game? {
            return when (targetType) {
                StackType.TABLEAU -> {
                    val tbl = newTableau ?: baseGame.tableau.toMutableList()
                    val targetPile = baseGame.tableau.getOrNull(targetIndex) ?: return null
                    val updatedTarget = targetPile.withCardsAdded(listOf(candidateFaceUp))
                    if (updatedTarget === targetPile && !targetPile.isEmpty()) return null
                    tbl[targetIndex] = updatedTarget
                    baseGame.copy(tableau = tbl)
                }
                StackType.FOUNDATION -> {
                    val targetFoundation = baseGame.foundations.getOrNull(targetIndex) ?: return null
                    val updatedFoundation = targetFoundation.withCardAdded(candidateFaceUp)
                    if (updatedFoundation === targetFoundation) return null
                    val newFoundations = baseGame.foundations.toMutableList()
                    newFoundations[targetIndex] = updatedFoundation
                    if (newTableau != null) baseGame.copy(tableau = newTableau, foundations = newFoundations)
                    else baseGame.copy(foundations = newFoundations)
                }
                else -> null
            }
        }

        return when (candidate.sourceType) {
            StackType.TABLEAU -> {
                val sourcePile = game.tableau.getOrNull(candidate.sourceTableauIndex) ?: return null
                val sourceCards = sourcePile.asList().toMutableList()
                if (candidate.sourceCardIndex !in sourceCards.indices) return null
                sourceCards.removeAt(candidate.sourceCardIndex)
                var rebuiltSourcePile = TableauPile(sourceCards.map { it.copy() }.toMutableList())
                rebuiltSourcePile = rebuiltSourcePile.withTopCardFlipped()
                val newTableau = game.tableau.toMutableList()
                newTableau[candidate.sourceTableauIndex] = rebuiltSourcePile
                placeOnTarget(game, newTableau)
            }
            StackType.STOCK -> {
                val stockCards = game.stock.asList().toMutableList()
                if (candidate.sourceCardIndex !in stockCards.indices) return null
                stockCards.removeAt(candidate.sourceCardIndex)
                val newStock = Stock(stockCards.map { it.copy() }.toMutableList())
                placeOnTarget(game.copy(stock = newStock))
            }
            StackType.WASTE -> {
                val wasteCardsList = game.waste.asList().toMutableList()
                if (candidate.sourceCardIndex !in wasteCardsList.indices) return null
                wasteCardsList.removeAt(candidate.sourceCardIndex)
                val newWaste = Waste().also { w -> wasteCardsList.forEach { c -> w.push(c) } }
                placeOnTarget(game.copy(waste = newWaste))
            }
            else -> null
        }
    }

    /**
     * Returns all available magic-wand candidates for an empty target slot.
     * Empty TABLEAU → looks for Kings; empty FOUNDATION → looks for Aces.
     * Searches face-down tableau cards, face-down stock, and face-up waste (in that priority order).
     */
    fun getMagicWandCandidatesForEmpty(targetType: StackType, targetIndex: Int): List<MagicWandCandidate> {
        val game = _game.value
        val requiredRank: StandardRank = when (targetType) {
            StackType.TABLEAU -> StandardRank.KING
            StackType.FOUNDATION -> StandardRank.ACE
            else -> return emptyList()
        }
        val results = mutableListOf<MagicWandCandidate>()

        // 1. Face-down tableau cards
        for (sourcePileIndex in game.tableau.indices) {
            if (!game.extraTableauUnlocked && sourcePileIndex == Game.LOCKED_TABLEAU_INDEX) continue
            if (targetType == StackType.TABLEAU && sourcePileIndex == targetIndex) continue
            val sourceCards = game.tableau[sourcePileIndex].asList()
            for (cardIndex in sourceCards.lastIndex downTo 0) {
                val c = sourceCards[cardIndex]
                if (c.isFaceUp) continue
                if (c.rank == requiredRank) {
                    results.add(MagicWandCandidate(c, StackType.TABLEAU, sourcePileIndex, cardIndex))
                }
            }
        }

        // 2. Face-down stock cards
        val stockCards = game.stock.asList()
        for (cardIndex in stockCards.indices) {
            val c = stockCards[cardIndex]
            if (c.isFaceUp) continue
            if (c.rank == requiredRank) {
                results.add(MagicWandCandidate(c, StackType.STOCK, -1, cardIndex))
            }
        }

        // 3. Face-up waste cards
        val wasteCards = game.waste.asList()
        for (cardIndex in wasteCards.lastIndex downTo 0) {
            val c = wasteCards[cardIndex]
            if (!c.isFaceUp) continue
            if (c.rank == requiredRank) {
                results.add(MagicWandCandidate(c, StackType.WASTE, -1, cardIndex))
            }
        }

        return results
    }

    /**
     * Applies a specific magic-wand candidate to an empty target slot.
     * Returns true on success (caller should then consume the wand).
     */
    fun tryUseMagicWandWithCandidate(
        targetType: StackType,
        targetIndex: Int,
        candidate: MagicWandCandidate
    ): Boolean {
        val game = _game.value
        val privateCandidate = WandCandidate(
            sourceType = candidate.sourceType,
            sourceTableauIndex = candidate.sourceTableauIndex,
            sourceCardIndex = candidate.sourceCardIndex,
            card = candidate.card
        )
        val wandAppliedGame = applyWandCandidate(game, privateCandidate, targetType, targetIndex)
            ?: return false

        clearSingleClickGlow()
        resetHintTimer()
        undoStack.addLast(game)
        redoStack.clear()
        _game.value = updateAfterMove(wandAppliedGame)
        saveGame()
        updateUndoRedoState()
        return true
    }

    private fun findFirstMagicWandCandidate(game: Game, targetType: StackType, targetIndex: Int): WandCandidate? {
        // Helper: does this face-up card satisfy the target slot?
        fun canSatisfy(candidateFaceUp: Card, sourcePileIndex: Int): Boolean = when (targetType) {
            StackType.TABLEAU -> {
                if (sourcePileIndex == targetIndex) false
                else game.tableau.getOrNull(targetIndex)?.canPush(candidateFaceUp) == true
            }
            StackType.FOUNDATION -> game.foundations.getOrNull(targetIndex)?.canPush(candidateFaceUp) == true
            else -> false
        }

        // ── 1. Facedown tableau cards (existing behaviour) ──────
        for (sourcePileIndex in game.tableau.indices) {
            if (!game.extraTableauUnlocked && sourcePileIndex == Game.LOCKED_TABLEAU_INDEX) continue
            val sourceCards = game.tableau[sourcePileIndex].asList()
            for (cardIndex in sourceCards.lastIndex downTo 0) {
                val facedown = sourceCards[cardIndex]
                if (facedown.isFaceUp) continue
                val candidateFaceUp = facedown.copy(isFaceUp = true)
                if (canSatisfy(candidateFaceUp, sourcePileIndex)) {
                    return WandCandidate(StackType.TABLEAU, sourcePileIndex, cardIndex, facedown)
                }
            }
        }

        // ── 2. Facedown cards in the stock (draw pile) ──────────
        val stockCards = game.stock.asList()
        for (cardIndex in stockCards.indices) {
            val facedown = stockCards[cardIndex]
            if (facedown.isFaceUp) continue
            val candidateFaceUp = facedown.copy(isFaceUp = true)
            if (canSatisfy(candidateFaceUp, -1)) {
                return WandCandidate(StackType.STOCK, -1, cardIndex, facedown)
            }
        }

        // ── 3. All faceup cards in the waste pile (top first) ───
        val wasteCards = game.waste.asList()
        for (cardIndex in wasteCards.lastIndex downTo 0) {
            val wasteCard = wasteCards[cardIndex]
            if (!wasteCard.isFaceUp) continue
            if (canSatisfy(wasteCard, -1)) {
                return WandCandidate(StackType.WASTE, -1, cardIndex, wasteCard)
            }
        }

        return null
    }

    private fun updateAfterMove(game: Game, scoreDelta: Int = 0): Game {
        startTimerOnFirstSuccessfulMoveIfNeeded()
        val windowsScore = game.windowsScore + scoreDelta
        val scoredGame = withScoringChannels(
            game.copy(moves = game.moves + 1),
            windowsScore = windowsScore
        )

        return if (scoredGame.isWinCondition()) {
            stopTimer()
            val updatedGame = scoredGame.copy(
                status = GameStatus.WON,
                extraTableauUnlocked = false
            )
            // Record the win in stats
            recordGameCompletion(updatedGame, isWin = true)
            updatedGame
        } else {
            scoredGame
        }
    }

    private suspend fun recordGameCompletionInternal(game: Game, isWin: Boolean, elapsedTimeMs: Long) {
        // Only record if game hasn't been recorded yet and player made at least 5 moves
        if (currentHandRecorded || game.moves < 5) return

        currentHandRecorded = true

        try {
            val settings = settingsManager.gamePlaySettingsFlow.firstOrNull()
            val playerName = settings?.playerDisplayName
            val cardsDraw = settings?.drawSize
            val deckCount = game.deckCount
            val isPremium = settings?.premiumAcct ?: false
            val selectedScoreMethod = ScoreMethod.normalize(settings?.scoreMethod)
            statsManager.recordGame(
                score = game.scoreForMethod(selectedScoreMethod),
                windowsScore = game.windowsScore,
                vegasScore = game.vegasScore,
                vegasCumulativeScore = game.vegasCumulativeScore,
                completionPercentage = game.completionPercentage,
                moves = game.moves,
                timeMs = elapsedTimeMs.coerceAtLeast(0L),
                isWin = isWin,
                playerName = playerName,
                cardsDraw = cardsDraw,
                deckCount = deckCount
            )
            settingsManager.setVegasCumulativeBankroll(game.vegasCumulativeScore)
            settingsManager.recordCompletedGamePremiumFlag(isPremium)
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
    private fun recordGameCompletion(game: Game, isWin: Boolean, elapsedTimeMs: Long = gameTime.value * 1000L) {
        viewModelScope.launch {
            recordGameCompletionInternal(game, isWin, elapsedTimeMs)
        }
    }

    fun undo(): Boolean {
        if (undoStack.isNotEmpty()) {
            resetHintTimer()
            val current = _game.value
            redoStack.addLast(current)
            _game.value = undoStack.removeLast()
            saveGame()
            updateUndoRedoState()
            return true
        }
        return false
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        resetHintTimer()
        val current = _game.value
        undoStack.addLast(current)
        _game.value = redoStack.removeLast()
        saveGame()
        updateUndoRedoState()
        return true
    }

    private fun saveGameIfInProgress() {
        if (_game.value.status == GameStatus.IN_PROGRESS) saveGame()
    }

    // Record an in-progress game as a loss before replacing it with a new hand.
    private fun finalizeCurrentGameIfNeeded() {
        viewModelScope.launch {
            finalizeCurrentGameIfNeededSync()
        }
    }

    private suspend fun finalizeCurrentGameIfNeededSync() {
        val current = _game.value
        if (current.status == GameStatus.IN_PROGRESS) {
            val elapsedTimeMs = gameTime.value * 1000L
            recordGameCompletionInternal(current, isWin = false, elapsedTimeMs = elapsedTimeMs)
        }
    }

    private suspend fun createNewGameWithScoring(): Game {
        val freshGame = controller.newGameWithClearHistory(currentDeckCount)
        val vegasBase = settingsManager.getVegasCumulativeBankroll()
        return withScoringChannels(
            freshGame.copy(
                windowsScore = 0,
                score = 0,
                vegasCumulativeBase = vegasBase
            ),
            windowsScore = 0
        )
    }

    private fun withScoringChannels(game: Game, windowsScore: Int = game.windowsScore): Game {
        val vegasScore = ScoreCalculator.computeVegasScore(game)
        val vegasCumulativeScore = game.vegasCumulativeBase + vegasScore
        val completionPercentage = ScoreCalculator.computeCompletionPercentage(game)
        return game.copy(
            score = windowsScore,
            windowsScore = windowsScore,
            vegasScore = vegasScore,
            vegasCumulativeScore = vegasCumulativeScore,
            completionPercentage = completionPercentage
        )
    }

    fun saveGame() {
        viewModelScope.launch {
            try {
                val gameToSave = _game.value.copy(savedGameTime = gameTime.value)
                val initialToSave = initialGameState?.copy(savedGameTime = 0L)
                val json = gson.toJson(
                    SavedGameEnvelope(
                        currentGame = gameToSave,
                        initialGameState = initialToSave
                    )
                )
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
            clearPendingRecycleState()
            val saved = repository.getCurrentGameState().firstOrNull()
            if (!saved.isNullOrEmpty()) {
                val parsed = parseSavedPayload(saved) ?: return false
                val (savedGameRaw, initialFromSaveRaw, usedLegacyPayload) = parsed
                val savedGame = normalizeGameForDeckMode(savedGameRaw)
                val initialFromSave = initialFromSaveRaw?.let { normalizeGameForDeckMode(it) }
                val (migratedCurrentRaw, currentMigrated) = migrateSavedGameImagePaths(savedGame)
                val (migratedInitial, initialMigrated) = initialFromSave
                    ?.let { migrateSavedGameImagePaths(it) }
                    ?: Pair(null, false)
                val migratedCurrent = withScoringChannels(migratedCurrentRaw)
                val restartAnchor = (migratedInitial ?: migratedCurrent.deepCopy()).copy(savedGameTime = 0L)

                if (migratedCurrent.status == GameStatus.IN_PROGRESS) {
                    initialGameState = restartAnchor
                    _game.value = migratedCurrent
                    gameTime.value = migratedCurrent.savedGameTime
                    hasRegisteredFirstMove = migratedCurrent.savedGameTime > 0
                    currentHandRecorded = false

                    // Persist one-time migrations + legacy payload upgrade.
                    if (currentMigrated || initialMigrated || usedLegacyPayload || initialFromSave == null) {
                        val upgradedJson = gson.toJson(
                            SavedGameEnvelope(
                                currentGame = migratedCurrent,
                                initialGameState = restartAnchor
                            )
                        )
                        repository.saveCurrentGameState(upgradedJson)
                    }

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

    private fun parseSavedPayload(saved: String): Triple<Game, Game?, Boolean>? {
        return try {
            val root = JsonParser().parse(saved)
            if (root.isJsonObject && root.asJsonObject.has("currentGame")) {
                val envelope: SavedGameEnvelope = gson.fromJson(saved, SavedGameEnvelope::class.java)
                Triple(envelope.currentGame, envelope.initialGameState, false)
            } else {
                val legacyGame: Game = gson.fromJson(saved, Game::class.java)
                Triple(legacyGame, null, true)
            }
        } catch (t: Throwable) {
            Log.w("GameViewModel", "parseSavedPayload failed: ${t.message}")
            null
        }
    }

    private fun migrateSavedGameImagePaths(game: Game): Pair<Game, Boolean> {
        var changed = false

        val normalizedDeckCount = Game.normalizeDeckCount(game.deckCount)
        if (normalizedDeckCount != game.deckCount) changed = true

        val expectedTableauCount = Game.totalTableauPilesFor(normalizedDeckCount)
        val expectedFoundationCount = Game.foundationCountFor(normalizedDeckCount)

        val normalizedTableau = when {
            game.tableau.size == Game.dealtTableauCountFor(normalizedDeckCount) -> {
                changed = true
                listOf(TableauPile()) + game.tableau
            }
            game.tableau.size < expectedTableauCount -> {
                changed = true
                game.tableau + List(expectedTableauCount - game.tableau.size) { TableauPile() }
            }
            game.tableau.size > expectedTableauCount -> {
                changed = true
                game.tableau.take(expectedTableauCount)
            }
            else -> game.tableau
        }

        val normalizedFoundations = List(expectedFoundationCount) { index ->
            val existing = game.foundations.getOrNull(index)
            val reservedSuit = Game.reservedFoundationSuitForIndex(index)
            when {
                existing == null -> {
                    changed = true
                    FoundationPile(reservedSuit = reservedSuit)
                }
                existing.reservedSuit != reservedSuit -> {
                    changed = true
                    FoundationPile(existing.asList().map { it.copy() }.toMutableList(), reservedSuit)
                }
                else -> existing
            }
        }

        val normalizedGame = if (
            normalizedTableau !== game.tableau ||
            normalizedFoundations !== game.foundations ||
            normalizedDeckCount != game.deckCount
        ) {
            game.copy(
                deckCount = normalizedDeckCount,
                tableau = normalizedTableau,
                foundations = normalizedFoundations
            )
        } else {
            game
        }

        fun normalizeCard(card: Card): Card {
            val targetFacePath = expectedFaceImagePath(card)
            val targetBackPath = "drawable:card_back_crosshatch_001"

            val migrated = card.copy(
                recto = card.recto.copy(imagePath = targetFacePath),
                verso = card.verso.copy(imagePath = targetBackPath)
            )

            if (migrated != card) changed = true
            return migrated
        }

        val migratedStock = Stock(normalizedGame.stock.asList().map(::normalizeCard).toMutableList())

        val migratedWaste = Waste().also { newWaste ->
            normalizedGame.waste.asList().map(::normalizeCard).forEach { card ->
                newWaste.push(card)
            }
        }

        val migratedTableau = normalizedGame.tableau.map { pile ->
            TableauPile(pile.asList().map(::normalizeCard).toMutableList())
        }

        val migratedFoundations = normalizedGame.foundations.mapIndexed { index, pile ->
            val reservedSuit = Game.reservedFoundationSuitForIndex(index)
            if (pile.reservedSuit != reservedSuit) changed = true
            FoundationPile(
                cards = pile.asList().map(::normalizeCard).toMutableList(),
                reservedSuit = reservedSuit
            )
        }

        return Pair(
            normalizedGame.copy(
                stock = migratedStock,
                waste = migratedWaste,
                tableau = migratedTableau,
                foundations = migratedFoundations
            ),
            changed
        )
    }

    private fun expectedFaceImagePath(card: Card): String {
        if (card.rank == Joker) {
            val existing = card.recto.imagePath.lowercase(Locale.US)
            return if (existing.contains("j_li")) "drawable:j_li" else "drawable:j_da"
        }

        val suitCode = when (card.suit) {
            CardSuit.HEARTS -> "hearts"
            CardSuit.DIAMONDS -> "diamonds"
            CardSuit.SPADES -> "spades"
            CardSuit.CLUBS -> "clubs"
            else -> "spades"
        }

        val rankCode = when (card.rank) {
            StandardRank.ACE -> "ace"
            StandardRank.JACK -> "jack"
            StandardRank.QUEEN -> "queen"
            StandardRank.KING -> "king"
            else -> card.rank.sortOrder.toString()
        }

        return "drawable:ic_${suitCode}_${rankCode}"
    }

    private fun normalizeGameForDeckMode(game: Game): Game {
        val normalizedDeckCount = Game.normalizeDeckCount(game.deckCount)
        if (normalizedDeckCount == game.deckCount) return game
        return game.copy(deckCount = normalizedDeckCount)
    }

    fun stopGame() {
        val current = _game.value

        // runBlocking ensures save + session-flag write complete before teardown.
        // Leaving the game screen should preserve in-progress sessions for resume,
        // not auto-record them as abandoned losses.
        runBlocking {
            try {
                val gameToSave = current.copy(savedGameTime = gameTime.value)
                val json = gson.toJson(
                    SavedGameEnvelope(
                        currentGame = gameToSave,
                        initialGameState = initialGameState?.copy(savedGameTime = 0L)
                    )
                )
                repository.saveCurrentGameState(json)
            } catch (t: Throwable) {
                Log.w("GameViewModel", "stopGame save failed: ${t.message}")
            }
            settingsManager.setGameSessionActive(current.status == GameStatus.IN_PROGRESS)
        }
    }

    fun unlockExtraTableauPile() {
        val current = _game.value
        if (current.extraTableauUnlocked) return
        _game.value = current.copy(extraTableauUnlocked = true)
        saveGame()
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




