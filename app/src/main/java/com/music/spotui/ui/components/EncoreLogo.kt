package com.music.spotui.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import app.myspoti.engine.R

/**
 * The Encore mark for Compose screens. `painterResource` cannot draw an XML `<bitmap>`/`<inset>`
 * drawable (it decodes whatever is not a vector with BitmapFactory, which returns null for XML and
 * crashes on null), and the launcher foreground is exactly that. This reads the density PNG that
 * `@drawable/encore_logo` resolves to, which is a genuine bitmap at every density.
 */
@Composable
fun encoreLogo(): ImageBitmap = ImageBitmap.imageResource(R.drawable.encore_logo)
