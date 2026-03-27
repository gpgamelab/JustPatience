package com.gpgamelab.justpatience.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gpgamelab.justpatience.data.GameRecord
import com.gpgamelab.justpatience.data.SettingsManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gpgamelab.justpatience.databinding.DialogStatsBinding
import com.gpgamelab.justpatience.ui.adapter.GameRecordAdapter
import com.gpgamelab.justpatience.viewmodel.StatsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * DialogFragment for displaying game statistics and history.
 */
class StatsDialogFragment : DialogFragment() {

    private lateinit var binding: DialogStatsBinding
    private val statsViewModel: StatsViewModel by viewModels()
    private var adapter: GameRecordAdapter? = null
    private lateinit var settingsManager: SettingsManager
    private var selectedDrawCount: Int = 1
    private var latestRecords: List<GameRecord> = emptyList()
    private var hasAppliedInitialDrawSetting: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsManager = SettingsManager(requireContext().applicationContext)

        setupRecyclerView()
        setupListeners()
        setupToggle()
        applyCurrentDrawSetting()
        collectStats()
    }

    private fun setupRecyclerView() {
        binding.recyclerviewGameHistory.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupToggle() {
        binding.toggleDrawCount.check(binding.btnDrawOne.id)
        binding.toggleDrawCount.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedDrawCount = if (checkedId == binding.btnDrawThree.id) 3 else 1
            renderFilteredStats()
        }
    }

    private fun applyCurrentDrawSetting() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (hasAppliedInitialDrawSetting) return@launch
            val drawSize = settingsManager.gamePlaySettingsFlow.first().drawSize
            selectedDrawCount = normalizeDrawCount(drawSize)
            binding.toggleDrawCount.check(
                if (selectedDrawCount == 3) binding.btnDrawThree.id else binding.btnDrawOne.id
            )
            hasAppliedInitialDrawSetting = true
            renderFilteredStats()
        }
    }

    private fun collectStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    statsViewModel.allGameRecords.collect { records ->
                        latestRecords = records
                        renderFilteredStats()
                    }
                }
            }
        }
    }

    private fun renderFilteredStats() {
        val filtered = latestRecords.filter { normalizeDrawCount(it.cardsDraw) == selectedDrawCount }
        val gamesPlayed = filtered.size
        val gamesWon = filtered.count { it.isWin }
        val winRate = if (gamesPlayed == 0) 0.0 else (gamesWon.toDouble() / gamesPlayed) * 100.0
        val highestScore = filtered.maxOfOrNull { it.score } ?: 0
        val averageScore = filtered.map { it.score }.average().let { if (it.isNaN()) null else it }
        val averageTimeMs = filtered.map { it.timeMs }.average().let { if (it.isNaN()) null else it.toLong() }

        binding.tvGamesPlayed.text = gamesPlayed.toString()
        binding.tvGamesWon.text = gamesWon.toString()
        binding.tvWinRate.text = String.format(Locale.getDefault(), "%.1f%%", winRate)
        binding.tvHighestScore.text = highestScore.toString()
        binding.tvAverageScore.text = averageScore?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "0"
        binding.tvAverageTime.text = averageTimeMs?.let { statsViewModel.formatTime(it) } ?: "00:00"

        adapter = GameRecordAdapter(filtered) { timeMs ->
            statsViewModel.formatTime(timeMs)
        }
        binding.recyclerviewGameHistory.adapter = adapter
    }

    private fun normalizeDrawCount(cardsDraw: Int?): Int {
        val normalized = cardsDraw ?: 1
        return if (normalized >= 3) 3 else 1
    }

    override fun onStart() {
        super.onStart()
        // Make dialog fill most of the screen
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.9).toInt()
        )
    }

    companion object {
        fun newInstance(): StatsDialogFragment {
            return StatsDialogFragment()
        }
    }
}
