package de.js.app.agtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import de.js.app.agtracker.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}