package com.fingo.finlauncher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fingo.finlauncher.data.AppModel
import com.fingo.finlauncher.ui.theme.AccentCyan
import com.fingo.finlauncher.ui.theme.TextPrimary

@Composable
fun rememberNativeDrawablePainter(drawable: Drawable?): Painter? {
    if (drawable == null) return null
    return remember(drawable) {
        val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
        BitmapPainter(bitmap.asImageBitmap())
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItemRow(
    app: AppModel,
    modifier: Modifier = Modifier,
    onAppClick: () -> Unit,
    onAppLongClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onAppClick,
                onLongClick = onAppLongClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            val painter = rememberNativeDrawablePainter(drawable = app.icon)
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = app.label,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // App Label & Inline Notification Preview
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = app.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Niagara inline notification snippet
            if (!app.latestNotificationText.isNullOrBlank()) {
                Text(
                    text = app.latestNotificationText,
                    fontSize = 13.sp,
                    color = AccentCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Notification badge or indicator
        if (app.notificationCount > 0) {
            Text(
                text = app.notificationCount.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
