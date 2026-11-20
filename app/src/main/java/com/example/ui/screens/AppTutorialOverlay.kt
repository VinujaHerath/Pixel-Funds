package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CadenceTab
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion

data class TutorialStepSpec(
    val targetTab: CadenceTab,
    val targetBudgetsSection: BudgetsSection = BudgetsSection.OVERVIEW,
    val targetActivitySection: ActivitySection = ActivitySection.TRANSACTIONS,
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val tipRes: Int,
    val anchorBottom: Boolean = true,
)

val AppTutorialSteps: List<TutorialStepSpec> = listOf(
    TutorialStepSpec(
        targetTab = CadenceTab.Home,
        icon = Icons.Rounded.Home,
        titleRes = R.string.tutorial_step1_title,
        bodyRes = R.string.tutorial_step1_body,
        tipRes = R.string.tutorial_step1_tip,
        anchorBottom = true,
    ),
    TutorialStepSpec(
        targetTab = CadenceTab.Home,
        icon = Icons.Rounded.AddCircleOutline,
        titleRes = R.string.tutorial_step2_title,
        bodyRes = R.string.tutorial_step2_body,
        tipRes = R.string.tutorial_step2_tip,
        anchorBottom = false,
    ),
    TutorialStepSpec(
        targetTab = CadenceTab.Budgets,
        targetBudgetsSection = BudgetsSection.OVERVIEW,
        icon = Icons.Rounded.PieChart,
        titleRes = R.string.tutorial_step3_title,
        bodyRes = R.string.tutorial_step3_body,
        tipRes = R.string.tutorial_step3_tip,
        anchorBottom = true,
    ),
    TutorialStepSpec(
        targetTab = CadenceTab.Budgets,
        targetBudgetsSection = BudgetsSection.CATEGORIES,
        icon = Icons.Rounded.Tune,
        titleRes = R.string.tutorial_step4_title,
        bodyRes = R.string.tutorial_step4_body,
        tipRes = R.string.tutorial_step4_tip,
        anchorBottom = true,
    ),
    TutorialStepSpec(
        targetTab = CadenceTab.Activity,
        targetActivitySection = ActivitySection.TRANSACTIONS,
        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
        titleRes = R.string.tutorial_step5_title,
        bodyRes = R.string.tutorial_step5_body,
        tipRes = R.string.tutorial_step5_tip,
        anchorBottom = true,
    ),
    TutorialStepSpec(
        targetTab = CadenceTab.Insights,
        icon = Icons.Rounded.Insights,
        titleRes = R.string.tutorial_step6_title,
        bodyRes = R.string.tutorial_step6_body,
        tipRes = R.string.tutorial_step6_tip,
        anchorBottom = true,
    ),
)

@Composable
fun AppTutorialOverlay(
    stepIndex: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onFinish: () -> Unit = onDismiss,
) {
    val safeIndex = stepIndex.coerceIn(0, AppTutorialSteps.lastIndex)
    val step = AppTutorialSteps[safeIndex]
    val totalSteps = AppTutorialSteps.size
    val isFirst = safeIndex == 0
    val isLast = safeIndex == totalSteps - 1
    val performHaptic = rememberPixelHaptics()

    val animatedProgress by animateFloatAsState(
        targetValue = (safeIndex + 1).toFloat() / totalSteps.toFloat(),
        animationSpec = ExpressiveMotion.defaultSpatial(),
        label = "TutorialProgress",
    )

    BackHandler {
        if (isFirst) onDismiss() else onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .testTag("tutorial_overlay"),
        contentAlignment = if (step.anchorBottom) Alignment.BottomCenter else Alignment.TopCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(bottom = if (step.anchorBottom) 12.dp else 0.dp, top = if (!step.anchorBottom) 12.dp else 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header: Step badge + Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.tutorial_badge, safeIndex + 1, totalSteps),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("tutorial_step_counter"),
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("tutorial_skip_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.tutorial_skip),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                // Smooth progress bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )

                // Animated step copy
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(ExpressiveMotion.fastEffects()) togetherWith
                            fadeOut(ExpressiveMotion.fastEffects())
                    },
                    label = "TutorialStepCopy",
                ) { current ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(current.titleRes),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(current.bodyRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(18.dp),
                                )
                                Text(
                                    text = stringResource(current.tipRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }

                // Navigation controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isFirst) {
                        FilledTonalButton(
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                onBack()
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("tutorial_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.tutorial_back))
                        }
                    }

                    Button(
                        onClick = {
                            performHaptic(if (isLast) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                            if (isLast) onFinish() else onNext()
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag(if (isLast) "tutorial_done_button" else "tutorial_next_button"),
                    ) {
                        Text(
                            text = if (isLast) stringResource(R.string.tutorial_done)
                            else stringResource(R.string.tutorial_next),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLast) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
