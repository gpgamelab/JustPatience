package com.gpgamelab.justpatience.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.gpgamelab.justpatience.databinding.DialogStatsBinding
import com.gpgamelab.justpatience.ui.adapter.GameRecordAdapter
import com.gpgamelab.justpatience.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * DialogFragment for displaying game statistics and history.
 */
class StatsDialogFragment : DialogFragment() {

    private lateinit var binding: DialogStatsBinding
    private val statsViewModel: StatsViewModel by viewModels()
    private var adapter: GameRecordAdapter? = null

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

        setupRecyclerView()
        setupListeners()
        collectStats()
    }

    private fun setupRecyclerView() {
        binding.recyclerviewGameHistory.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnResetStats.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun collectStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    statsViewModel.totalGamesPlayed.collect { games ->
                        binding.tvGamesPlayed.text = games.toString()
                    }
                }

                launch {
                    statsViewModel.totalGamesWon.collect { wins ->
                        binding.tvGamesWon.text = wins.toString()
                    }
                }

                launch {
                    statsViewModel.winRate.collect { rate ->
                        binding.tvWinRate.text = String.format(Locale.getDefault(), "%.1f%%", rate)
                    }
                }

                launch {
                    statsViewModel.highestScore.collect { score ->
                        binding.tvHighestScore.text = score?.toString() ?: "0"
                    }
                }

                launch {
                    statsViewModel.averageScore.collect { avg ->
                        binding.tvAverageScore.text = avg?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "0"
                    }
                }

                launch {
                    statsViewModel.averageTimeMs.collect { timeMs ->
                        binding.tvAverageTime.text = if (timeMs != null) {
                            statsViewModel.formatTime(timeMs)
                        } else {
                            "00:00"
                        }
                    }
                }

                launch {
                    statsViewModel.allGameRecords.collect { records ->
                        adapter = GameRecordAdapter(records) { timeMs ->
                            statsViewModel.formatTime(timeMs)
                        }
                        binding.recyclerviewGameHistory.adapter = adapter
                    }
                }
            }
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Statistics")
            .setMessage("Are you sure you want to delete all game history and statistics? This cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                statsViewModel.resetAllStats()
            }
            .setNegativeButton("No", null)
            .show()
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



