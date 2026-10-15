package com.thaiclockfix.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thaiclockfix.R
import com.thaiclockfix.model.LocaleMode
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Material 3 Expressive Hero Clock:
 * - Flexible container (takes remaining space above fold via weight(1f))
 * - Emphasized clock numerals (displayLarge, tabular-feel) with the selected-format sample
 * - Live date preview matching the active mode (Thai Buddhist vs English US)
 * - Ticking live clock preview
 * - Clear "ตัวอย่างในแอป" honest labeling
 */
@Composable
fun HeroClock(
    mode: LocaleMode,
    modifier: Modifier = Modifier,
    timeFlow: Flow<LocalDateTime> = remember { previewTimes() },
) {
    val sampleTime by timeFlow.collectAsStateWithLifecycle(initialValue = remember { LocalDateTime.now() })
    val timeFormatted = mode.formatTime24h(sampleTime)
    val dateFormatted = mode.formatDatePreview(sampleTime)
    val clockSemantics = stringResource(R.string.hero_clock_semantics, timeFormatted, dateFormatted)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 220.dp)
            .testTag("hero_clock_container")
            .semantics {
                contentDescription = clockSemantics
            },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            // Selected-format time and date preview
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Big Clock Numerals (Emphasized display scale)
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("hero_clock_time"),
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Date Preview (Cross-fading with choice)
                AnimatedContent(
                    targetState = dateFormatted,
                    transitionSpec = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh))
                    },
                    label = "hero_date_anim",
                ) { targetDate ->
                    Text(
                        text = targetDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.testTag("hero_clock_date"),
                    )
                }
            }

            // Honest-labeling pill: this clock is an in-app preview, not the status bar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.hero_preview_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
