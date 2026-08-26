package com.perso.jow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.perso.jow.app.ui.navigation.JowNavGraph
import com.perso.jow.app.ui.theme.JowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JowNavGraph()
                }
            }
        }
    }
}
