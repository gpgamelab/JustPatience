package com.gpgamelab.justpatience

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.databinding.ActivitySettingsBinding
import com.gpgamelab.justpatience.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private lateinit var binding: ActivitySettingsBinding

    // Track the current working copy so we can save on each change
    private var currentSettings = SettingsManager.GamePlaySettings()
    private var recycleCount = 3
    private var settingsLoaded = false
    private var isBindingUi = false

    // Orientation spinner values (display → internal key)
    private val orientationLabels by lazy {
        listOf(
            getString(R.string.settings_orientation_device),
            getString(R.string.settings_orientation_portrait),
            getString(R.string.settings_orientation_landscape)
        )
    }
    private val orientationKeys = listOf("device", "portrait", "landscape")

    // Board layout spinner values
    private val boardLayoutLabels by lazy {
        listOf(
            getString(R.string.settings_board_layout_right),
            getString(R.string.settings_board_layout_left)
        )
    }
    private val boardLayoutKeys = listOf("right_hand", "left_hand")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_button_description)

        setupSpinners()
        setupListeners()
        observeSettings()
    }

    private fun setupSpinners() {
        val orientationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, orientationLabels)
        orientationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOrientation.adapter = orientationAdapter

        val boardAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, boardLayoutLabels)
        boardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBoardLayout.adapter = boardAdapter
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            settingsViewModel.gamePlaySettings.collectLatest { settings ->
                // Always apply latest persisted settings.
                // The first emission from stateIn can be a default value; a later emission has stored values.
                applySettingsToUi(settings)
                settingsLoaded = true
            }
        }
    }

    private fun applySettingsToUi(settings: SettingsManager.GamePlaySettings) {
        isBindingUi = true
        currentSettings = settings
        recycleCount = settings.recycleCount

        // Draw size
        if (settings.drawSize == 3) {
            binding.radioGroupDrawSize.check(R.id.radio_draw_3)
        } else {
            binding.radioGroupDrawSize.check(R.id.radio_draw_1)
        }

        // Recycle count + infinite
        updateRecycleCountDisplay()
        binding.switchInfiniteRecycles.isChecked = settings.infiniteRecycles
        updateRecycleButtonsEnabled(!settings.infiniteRecycles)

        // Appearance
        binding.switchTimer.isChecked = settings.showGameTimer
        binding.switchCardAnimations.isChecked = settings.showCardAnimations
        binding.switchWinAnimation.isChecked = settings.showWinAnimation

        // Sound
        binding.switchMuteMusic.isChecked = settings.muteMusic
        binding.switchMuteCardSound.isChecked = settings.muteCardSound
        binding.switchMuteWinSound.isChecked = settings.muteWinSound

        // Orientation
        val orientationIndex = orientationKeys.indexOf(settings.orientationLock).coerceAtLeast(0)
        binding.spinnerOrientation.setSelection(orientationIndex, false)

        // Board layout
        val boardIndex = boardLayoutKeys.indexOf(settings.boardLayout).coerceAtLeast(0)
        binding.spinnerBoardLayout.setSelection(boardIndex, false)

        // Player name
        binding.editPlayerName.setText(settings.playerDisplayName)
        isBindingUi = false
    }

    private fun setupListeners() {
        // Draw size
        binding.radioGroupDrawSize.setOnCheckedChangeListener { _, checkedId ->
            if (isBindingUi || !settingsLoaded) return@setOnCheckedChangeListener
            val size = if (checkedId == R.id.radio_draw_3) 3 else 1
            saveSettings(currentSettings.copy(drawSize = size))
        }

        // Recycle count +/-
        binding.btnRecycleMinus.setOnClickListener {
            if (isBindingUi || !settingsLoaded) return@setOnClickListener
            if (recycleCount > 0) {
                recycleCount--
                updateRecycleCountDisplay()
                saveSettings(currentSettings.copy(recycleCount = recycleCount))
            }
        }
        binding.btnRecyclePlus.setOnClickListener {
            if (isBindingUi || !settingsLoaded) return@setOnClickListener
            recycleCount++
            updateRecycleCountDisplay()
            saveSettings(currentSettings.copy(recycleCount = recycleCount))
        }

        // Infinite recycles toggle
        binding.switchInfiniteRecycles.setOnCheckedChangeListener { _, checked ->
            if (isBindingUi || !settingsLoaded) return@setOnCheckedChangeListener
            updateRecycleButtonsEnabled(!checked)
            saveSettings(currentSettings.copy(infiniteRecycles = checked))
        }

        // Appearance
        binding.switchTimer.setOnCheckedChangeListener { _, checked ->
            if (isBindingUi || !settingsLoaded) return@setOnCheckedChangeListener
            saveSettings(currentSettings.copy(showGameTimer = checked))
        }
        binding.switchCardAnimations.setOnCheckedChangeListener { _, checked ->
            if (isBindingUi || !settingsLoaded) return@setOnCheckedChangeListener
            saveSettings(currentSettings.copy(showCardAnimations = checked))
        }
        binding.switchWinAnimation.setOnCheckedChangeListener { _, checked ->
            if (isBindingUi || !settingsLoaded) return@setOnCheckedChangeListener
            saveSettings(currentSettings.copy(showWinAnimation = checked))
        }

        // Sound
        binding.switchMuteMusic.setOnCheckedChangeListener { _, checked ->
            if (isBindingUi || !settingsLoaded) return@setOnCheckedChangeListener
            saveSettings(currentSettings.copy(muteMusic = checked))
        }
        binding.switchMuteCardSound.setOnCheckedChangeListener { _, checked ->
            if (isBindingUi || !settingsLoaded) return@setOnCheckedChangeListener
            saveSettings(currentSettings.copy(muteCardSound = checked))
        }
        binding.switchMuteWinSound.setOnCheckedChangeListener { _, checked ->
            if (isBindingUi || !settingsLoaded) return@setOnCheckedChangeListener
            saveSettings(currentSettings.copy(muteWinSound = checked))
        }

        // Orientation spinner
        binding.spinnerOrientation.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    if (isBindingUi || !settingsLoaded) return
                    val key = orientationKeys[position]
                    saveSettings(currentSettings.copy(orientationLock = key))
                    applyOrientationLock(key)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        // Board layout spinner
        binding.spinnerBoardLayout.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    if (isBindingUi || !settingsLoaded) return
                    val key = boardLayoutKeys[position]
                    saveSettings(currentSettings.copy(boardLayout = key))
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        // Player name – save on Done key or focus loss
        binding.editPlayerName.setOnEditorActionListener { _, actionId, _ ->
            if (isBindingUi || !settingsLoaded) return@setOnEditorActionListener false
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                savePlayerName()
                true
            } else false
        }
        binding.editPlayerName.setOnFocusChangeListener { _, hasFocus ->
            if (isBindingUi || !settingsLoaded) return@setOnFocusChangeListener
            if (!hasFocus) savePlayerName()
        }
    }

    private fun savePlayerName() {
        val name = binding.editPlayerName.text?.toString()?.trim() ?: ""
        saveSettings(currentSettings.copy(playerDisplayName = name))
    }

    private fun updateRecycleCountDisplay() {
        binding.textRecycleCount.text = recycleCount.toString()
    }

    private fun updateRecycleButtonsEnabled(enabled: Boolean) {
        binding.btnRecycleMinus.isEnabled = enabled
        binding.btnRecyclePlus.isEnabled = enabled
    }

    private fun saveSettings(settings: SettingsManager.GamePlaySettings) {
        currentSettings = settings
        settingsViewModel.saveGamePlaySettings(settings)
    }

    private fun applyOrientationLock(key: String) {
        requestedOrientation = when (key) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Persist in-progress text even if EditText still has focus.
                savePlayerName()
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        // Final safety net for keyboard dismiss/home gesture/navigation away.
        savePlayerName()
    }
}