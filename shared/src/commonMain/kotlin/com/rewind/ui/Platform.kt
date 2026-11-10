package com.rewind.ui

import androidx.compose.runtime.Composable

/** Barras del sistema / modo inmersivo según la plataforma. */
@Composable
expect fun SystemBarsEffect(dark: Boolean)
