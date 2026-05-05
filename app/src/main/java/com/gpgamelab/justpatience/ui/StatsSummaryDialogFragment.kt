package com.gpgamelab.justpatience.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.data.GameRecord
import com.gpgamelab.justpatience.databinding.DialogStatsSummaryBinding
import com.gpgamelab.justpatience.model.ScoreMethod
import com.gpgamelab.justpatience.viewmodel.StatsViewModel
import com.gpgamelab.justpatience.util.UiScaleUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class StatsSummaryDialogFragment : DialogFragment() {

    private lateinit var binding: DialogStatsSummaryBinding
    private val statsViewModel: StatsViewModel by viewModels()

    private lateinit var settingsManager: SettingsManager

    private var selectedDrawCount: Int = 1
    private var selectedDeckCount: Int = 1
    private var selectedScoreMethod: String = ScoreMethod.WINDOWS
    private var latestRecords: List<GameRecord> = emptyList()
    private var hasAppliedInitialDrawSetting: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogStatsSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UiScaleUtil.applyBaselineScale(view, requireContext())
        settingsManager = SettingsManager(requireContext().applicationContext)
        setupToggle()
        binding.btnCloseSummary.setOnClickListener { dismiss() }
        applyCurrentDrawSetting()
        collectRecords()
    }

    private fun setupToggle() {
        binding.toggleDrawCount.check(binding.btnDrawOne.id)
        binding.toggleDeckCount.check(binding.btnDeckOne.id)
        binding.toggleDrawCount.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedDrawCount = if (checkedId == binding.btnDrawThree.id) 3 else 1
            renderSummary()
        }
        binding.toggleDeckCount.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedDeckCount = if (checkedId == binding.btnDeckTwo.id) 2 else 1
            renderSummary()
        }
    }

    private fun applyCurrentDrawSetting() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            val drawSize = currentSettings.drawSize
            val deckCount = currentSettings.deckCount
            selectedScoreMethod = ScoreMethod.normalize(currentSettings.scoreMethod)
            if (hasAppliedInitialDrawSetting) return@launch
            selectedDrawCount = normalizeDrawCount(drawSize)
            selectedDeckCount = normalizeDeckCount(deckCount)
            binding.toggleDrawCount.check(
                if (selectedDrawCount == 3) binding.btnDrawThree.id else binding.btnDrawOne.id
            )
            binding.toggleDeckCount.check(
                if (selectedDeckCount == 2) binding.btnDeckTwo.id else binding.btnDeckOne.id
            )
            hasAppliedInitialDrawSetting = true
            renderSummary()
        }
    }

    private fun collectRecords() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                statsViewModel.allGameRecords.collect { records ->
                    latestRecords = records
                    renderSummary()
                }
            }
        }
    }

    private fun renderSummary() {
        val filtered = latestRecords.filter {
            normalizeDrawCount(it.cardsDraw) == selectedDrawCount &&
                normalizeDeckCount(it.deckCount) == selectedDeckCount
        }
        val gamesPlayed = filtered.size
        val gamesWon = filtered.count { it.isWin }
        val winRate = if (gamesPlayed == 0) 0.0 else (gamesWon.toDouble() / gamesPlayed) * 100.0
        val highestScore = filtered.maxOfOrNull { it.scoreForMethod(selectedScoreMethod) } ?: 0
        val averageTimeMs = filtered.map { it.timeMs }.average().let { avg ->
            if (avg.isNaN()) null else avg.toLong()
        }

        binding.tvSummaryGamesPlayed.text = gamesPlayed.toString()
        binding.tvSummaryGamesWon.text = gamesWon.toString()
        binding.tvSummaryWinRate.text = String.format(Locale.getDefault(), "%.1f%%", winRate)
        binding.tvSummaryHighestScore.text = highestScore.toString()
        binding.tvSummaryAverageTime.text = averageTimeMs?.let { statsViewModel.formatTime(it) } ?: "00:00"
    }

    private fun normalizeDrawCount(cardsDraw: Int?): Int {
        val normalized = cardsDraw ?: 1
        return if (normalized >= 3) 3 else 1
    }

    private fun normalizeDeckCount(deckCount: Int?): Int {
        return if (deckCount == 2) 2 else 1
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        fun newInstance(): StatsSummaryDialogFragment = StatsSummaryDialogFragment()
    }
}
