package com.gpgamelab.justpatience.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gpgamelab.justpatience.data.GameRecord
import com.gpgamelab.justpatience.databinding.ItemGameRecordBinding

/**
 * RecyclerView adapter for displaying game history records.
 */
class GameRecordAdapter(
    private val records: List<GameRecord>,
    private val onFormatTime: (Long) -> String
) : RecyclerView.Adapter<GameRecordAdapter.GameRecordViewHolder>() {

    inner class GameRecordViewHolder(private val binding: ItemGameRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: GameRecord) {
            binding.tvGameDate.text = record.dateString
            binding.tvGameScore.text = record.score.toString()
            binding.tvGameMoves.text = record.moves.toString()
            binding.tvGameTime.text = onFormatTime(record.timeMs)
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
        return GameRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameRecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size
}

