package com.example.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared icon + label content for Material buttons.
 * Button content lambdas are RowScope, so this drops straight in and keeps
 * icon/spacing styling consistent app-wide. Label weight comes from the
 * button's own labelLarge style (bold) — no override needed.
 */
@Composable
internal fun RowScope.ButtonContent(
    icon: ImageVector,
    label: String,
    iconSize: Dp = 18.dp
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(iconSize)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = label)
}
