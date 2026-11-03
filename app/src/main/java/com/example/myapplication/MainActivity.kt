package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.data.location.AmapPrivacy
import com.example.myapplication.ui.AppNavHost
import com.example.myapplication.ui.components.PrivacyConsentScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MyApplication).container
        setContent {
            MyApplicationTheme {
                var agreed by remember { mutableStateOf(AmapPrivacy.isAgreed(this)) }
                if (agreed) {
                    AppNavHost(container = container)
                } else {
                    PrivacyConsentScreen(
                        onAgree = {
                            AmapPrivacy.agree(this)
                            agreed = true
                        },
                        onExit = { finish() },
                    )
                }
            }
        }
    }
}
