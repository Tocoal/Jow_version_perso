package com.perso.jow.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.perso.jow.app.JowApplication
import com.perso.jow.app.di.AppContainer

@Composable
fun localAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as JowApplication).container
