package com.gpgamelab.justpatience.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.util.UiScaleUtil

class HowToPlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how_to_play)
        UiScaleUtil.applyBaselineScale(findViewById(android.R.id.content), this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_how_to_play)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.how_to_play_title)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

