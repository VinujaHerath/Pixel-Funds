package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LocalDay
import com.example.data.UserPreferences
import com.example.ui.components.rememberPixelHaptics
import com.example.ui.theme.ExpressiveMotion
import kotlinx.coroutines.launch

private data class OnboardingStoryPage(
    val drawableRes: Int,
    val titleRes: Int,
    val bodyRes: Int,
    val badgeText: String,
)

private const val TOTAL_ONBOARDING_PAGES = 4

@Composable
fun OnboardingScreen(
    preferences: UserPreferences,
    hasSampleData: Boolean,
    onSelectCurrency: (String) -> Unit,
    onSelectResetDay: (Int) -> Unit,
    onCompleteOnboarding: (startTutorial: Boolean, keepSampleData: Boolean) -> Unit,
) {
    val performHaptic = rememberPixelHaptics()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { TOTAL_ONBOARDING_PAGES })

    var keepSampleData by remember(hasSampleData) { mutableStateOf(hasSampleData) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showResetDayPicker by remember { mutableStateOf(false) }

    val storyPages = remember {
        listOf(
            OnboardingStoryPage(
                drawableRes = R.drawable.img_onboarding_welcome,
                titleRes = R.string.onboarding_page1_title,
                bodyRes = R.string.onboarding_page1_body,
                badgeText = "Pixel Funds",
            ),
            OnboardingStoryPage(
                drawableRes = R.drawable.img_onboarding_budgets,
                titleRes = R.string.onboarding_page2_title,
                bodyRes = R.string.onboarding_page2_body,
                badgeText = "Monthly Budgets",
            ),
            OnboardingStoryPage(
                drawableRes = R.drawable.img_onboarding_private,
                titleRes = R.string.onboarding_page3_title,
                bodyRes = R.string.onboarding_page3_body,
                badgeText = "100% On-Device",
            ),
        )
    }

    val isLastPage = pagerState.currentPage == TOTAL_ONBOARDING_PAGES - 1

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                // Top Row: Back arrow (when > 0) + Skip button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pagerState.currentPage > 0) {
                        FilledTonalIconButton(
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("onboarding_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.onboarding_back),
                            )
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }

                    TextButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onCompleteOnboarding(false, keepSampleData)
                        },
                        modifier = Modifier.testTag("onboarding_skip_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { page ->
                    if (page < storyPages.size) {
                        OnboardingIllustrationPage(page = storyPages[page])
                    } else {
                        OnboardingQuickSetupPage(
                            preferences = preferences,
                            keepSampleData = keepSampleData,
                            onKeepSampleDataChange = { keepSampleData = it },
                            onOpenCurrencyPicker = { showCurrencyPicker = true },
                            onOpenResetDayPicker = { showResetDayPicker = true },
                        )
                    }
                }

                // Page Indicator Dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(TOTAL_ONBOARDING_PAGES) { index ->
                        val active = index == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (active) 28.dp else 8.dp,
                            animationSpec = ExpressiveMotion.fastSpatial(),
                            label = "OnboardingDotWidth_$index",
                        )
                        val color by animateColorAsState(
                            targetValue = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            animationSpec = ExpressiveMotion.fastEffects(),
                            label = "OnboardingDotColor_$index",
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }

                // Bottom Action Area
                AnimatedContent(
                    targetState = isLastPage,
                    transitionSpec = {
                        fadeIn(ExpressiveMotion.fastEffects()) togetherWith
                            fadeOut(ExpressiveMotion.fastEffects())
                    },
                    label = "OnboardingCtaSwitch",
                ) { finalStep ->
                    if (finalStep) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    performHaptic(HapticFeedbackType.LongPress)
                                    onCompleteOnboarding(true, keepSampleData)
                                },
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("onboarding_start_tour_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.onboarding_start_tour),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            TextButton(
                                onClick = {
                                    performHaptic(HapticFeedbackType.TextHandleMove)
                                    onCompleteOnboarding(false, keepSampleData)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("onboarding_finish_button"),
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_go_to_app),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .height(56.dp)
                                .testTag("onboarding_next_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_next),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCurrencyPicker) {
        CurrencyPickerBottomSheet(
            selectedCode = preferences.currencyCode,
            onDismiss = { showCurrencyPicker = false },
            onSelectCurrency = { code ->
                onSelectCurrency(code)
                showCurrencyPicker = false
            },
        )
    }

    if (showResetDayPicker) {
        ResetDayPickerDialog(
            currentDay = preferences.budgetResetDay,
            onDismiss = { showResetDayPicker = false },
            onSelectDay = { day ->
                onSelectResetDay(day)
                showResetDayPicker = false
            },
        )
    }
}

@Composable
private fun OnboardingIllustrationPage(page: OnboardingStoryPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
        ) {
            Image(
                painter = painterResource(id = page.drawableRes),
                contentDescription = stringResource(page.titleRes),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(28.dp))

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = page.badgeText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun OnboardingQuickSetupPage(
    preferences: UserPreferences,
    keepSampleData: Boolean,
    onKeepSampleDataChange: (Boolean) -> Unit,
    onOpenCurrencyPicker: () -> Unit,
    onOpenResetDayPicker: () -> Unit,
) {
    val performHaptic = rememberPixelHaptics()
    val resetLabel = if (preferences.budgetResetDay == 1) {
        stringResource(R.string.settings_reset_day_default)
    } else {
        stringResource(R.string.settings_reset_day_custom, LocalDay.ordinalDay(preferences.budgetResetDay))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_setup_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_setup_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Currency setup row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_currency_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${preferences.currency.code} (${preferences.currency.symbol}) • ${preferences.currency.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onOpenCurrencyPicker()
                        },
                        modifier = Modifier.testTag("onboarding_currency_button"),
                    ) {
                        Text(stringResource(R.string.onboarding_change))
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                )

                // Reset day setup row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_reset_day_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = resetLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onOpenResetDayPicker()
                        },
                        modifier = Modifier.testTag("onboarding_reset_day_button"),
                    ) {
                        Text(stringResource(R.string.onboarding_change))
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                )

                // Starter sample data toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.onboarding_sample_data_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.onboarding_sample_data_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = keepSampleData,
                        onCheckedChange = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onKeepSampleDataChange(it)
                        },
                        modifier = Modifier.testTag("onboarding_sample_data_switch"),
                    )
                }
            }
        }
    }
}
