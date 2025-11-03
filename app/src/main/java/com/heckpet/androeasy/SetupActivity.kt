// SetupActivity.kt
package com.heckpet.androeasy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.heckpet.androeasy.ui.theme.AndroEasyTheme

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если настройка пройдена — сразу в Main
        if (Prefs.isSetupComplete(this)) {
            startMainActivity()
            return
        }

        setContent {
            AndroEasyTheme {
                SetupWizard { selectedMode ->
                    Prefs.setSetupComplete(this, true)
                    Prefs.setMode(this, selectedMode)
                    startMainActivity()
                }
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}