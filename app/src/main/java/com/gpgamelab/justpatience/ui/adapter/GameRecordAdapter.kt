package com.gpgamelab.justpatience.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gpgamelab.justpatience.data.GameRecord
import com.gpgamelab.justpatience.databinding.ItemGameRecordBinding
import com.gpgamelab.justpatience.model.ScoreMethod
import com.gpgamelab.justpatience.util.UiScaleUtil

/**
 * RecyclerView adapter for displaying game history records.
 */
class GameRecordAdapter(
    private val records: List<GameRecord>,
    scoreMethod: String,
    private val onFormatTime: (Long) -> String
) : RecyclerView.Adapter<GameRecordAdapter.GameRecordViewHolder>() {

    private val selectedScoreMethod = ScoreMethod.normalize(scoreMethod)

    inner class GameRecordViewHolder(private val binding: ItemGameRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: GameRecord) {
            binding.tvGameDate.text = record.dateString
            binding.tvGamePlayerName.text = record.playerName?.takeIf { it.isNotBlank() } ?: "N/A"
            binding.tvGameScore.text = record.scoreForMethod(selectedScoreMethod).toString()
            binding.tvGameMoves.text = record.moves.toString()
            binding.tvGameTime.text = onFormatTime(record.timeMs)
            binding.tvGameCardsDraw.text = (record.cardsDraw ?: 1).toString()
            binding.tvGameStatus.text = if (record.isWin) "Won" else "Lost"
            binding.tvGameStatus.setTextColor(
                if (record.isWin)
                    android.graphics.Color.parseColor("#4CAF50") // Green for win
                else
                    android.graphics.Color.parseColor("#F44336")  // Red for loss
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameRecordViewHolder {
        val binding = ItemGameRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        UiScaleUtil.applyBaselineScale(binding.root, parent.context)
        return GameRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameRecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size
}
