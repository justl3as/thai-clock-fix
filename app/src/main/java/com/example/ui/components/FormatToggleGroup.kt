package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.LocaleMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FormatToggleGroup(
    selectedMode: LocaleMode,
    onModeSelect: (LocaleMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val select: (LocaleMode) -> Unit = { mode ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onModeSelect(mode)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        FormatToggleButton(
            label = stringResource(LocaleMode.THAI_BUDDHIST.shortLabelRes),
            checked = selectedMode == LocaleMode.THAI_BUDDHIST,
            onSelect = { select(LocaleMode.THAI_BUDDHIST) },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            modifier = Modifier.weight(1f)
        )

        FormatToggleButton(
            label = stringResource(LocaleMode.ENGLISH.shortLabelRes),
            checked = selectedMode == LocaleMode.ENGLISH,
            onSelect = { select(LocaleMode.ENGLISH) },
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FormatToggleButton(
    label: String,
    checked: Boolean,
    onSelect: () -> Unit,
    shapes: ToggleButtonShapes,
    modifier: Modifier = Modifier
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = { if (it) onSelect() },
        shapes = shapes,
        modifier = modifier
            .height(56.dp)
            .semantics { role = Role.RadioButton }
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
    }
}
