package com.legendsoftware.richmangoogleplaybillinglibrarytest.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.legendsoftware.richmangoogleplaybillinglibrarytest.databinding.ActivityLogBinding
import com.legendsoftware.richmangoogleplaybillinglibrarytest.logging.AppLog

class LogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.btnClearLogs.setOnClickListener {
            AppLog.clear()
            renderLogs()
        }
        renderLogs()
    }

    override fun onResume() {
        super.onResume()
        renderLogs()
    }

    private fun renderLogs() {
        val entries = AppLog.entries()
        binding.logContent.text = if (entries.isEmpty()) {
            "No activity recorded yet."
        } else {
            entries.joinToString(separator = "\n\n") {
                "${it.timestamp}  ${it.level}\n[${it.category}] ${it.message}"
            }
        }
    }
}
