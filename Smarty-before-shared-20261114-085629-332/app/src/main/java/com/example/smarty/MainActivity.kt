package com.example.smarty

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.smarty.ui.theme.SmartyTheme
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appCheck = FirebaseAppCheck.getInstance()

        appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // Triggers the request so the SDK prints its debug token.
        appCheck.getAppCheckToken(false)
            .addOnSuccessListener {
                Log.d("SmartyAppCheck", "App Check connected.")
            }
            .addOnFailureListener {
                Log.w(
                    "SmartyAppCheck",
                    "App Check not ready. Register the debug token.",
                    it
                )
            }

        enableEdgeToEdge()

        setContent {
            SmartyTheme {
                SmartyApp()
            }
        }
    }
}