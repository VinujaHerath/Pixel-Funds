package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MonthlyTrendPoint
import com.example.ui.theme.LocalFinancialColors
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Clean 6-month spending trend card for the Dashboard powered by Vico (Compose Material 3).
 * Displays a smooth cubic line chart with soft tonal area fill, compact currency Y-axis labels,
 * month abbreviations on the X-axis, and a subtle vs-average comparison pill.
 */
@Composable
fun DashboardSpendingTrendCard(
    trendPoints: List<MonthlyTrendPoint>,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currency = LocalAppCurrency.current
    val financialColors = LocalFinancialColors.current
    val performHaptic = rememberPixelHaptics()

    val hasAnySpend = remember(trendPoints) { trendPoints.any { it.totalSpent > 0.0 } }
    val nonZeroMonths = remember(trendPoints) { trendPoints.filter { it.totalSpent > 0.0 } }
    val averageMonthlySpend = remember(nonZeroMonths) {
        if (nonZeroMonths.isEmpty()) 0.0 else nonZeroMonths.sumOf { it.totalSpent } / nonZeroMonths.size
    }

    val currentMonthSpend = trendPoints.lastOrNull()?.totalSpent ?: 0.0
    val priorMonths = remember(trendPoints) { trendPoints.dropLast(1).filter { it.totalSpent > 0.0 } }
    val priorAverage = remember(priorMonths) {
        if (priorMonths.isEmpty()) 0.0 else priorMonths.sumOf { it.totalSpent } / priorMonths.size
    }
    val deltaPercent: Int? = remember(currentMonthSpend, priorAverage) {
        if (priorAverage <= 0.0) null
        else (((currentMonthSpend - priorAverage) / priorAverage) * 100.0).roundToInt()
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(trendPoints) {
        if (trendPoints.isNotEmpty() && trendPoints.any { it.totalSpent > 0.0 }) {
            modelProducer.runTransaction {
                lineSeries {
                    series(trendPoints.map { it.totalSpent })
                }
            }
        }
    }

    val monthLabels = remember(trendPoints) { trendPoints.map { it.shortLabel } }
    val bottomAxisFormatter = remember(monthLabels) {
        CartesianValueFormatter { _, x, _ ->
            monthLabels.getOrNull(x.roundToInt()) ?: ""
        }
    }
    val startAxisFormatter = remember(currency) {
        CartesianValueFormatter { _, y, _ ->
            formatCurrencyValue(amount = y, currency = currency, showCents = false, compact = true)
        }
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val areaFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_six_month_trend_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_six_month_trend_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = {
                    performHaptic(HapticFeedbackType.TextHandleMove)
                    onOpenInsights()
                },
                modifier = Modifier.testTag("dashboard_trend_insights_button"),
            ) {
                Text(stringResource(R.string.dashboard_six_month_insights_cta))
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!hasAnySpend) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_six_month_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    // Calm summary header: Monthly average + vs-average delta pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    R.string.dashboard_six_month_avg,
                                    formatCurrency(averageMonthlySpend, showCents = false),
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (deltaPercent != null) {
                            val isHigher = deltaPercent > 4
                            val isLower = deltaPercent < -4
                            val badgeBg = when {
                                isHigher -> financialColors.warningContainer
                                isLower -> financialColors.healthyContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                            val badgeFg = when {
                                isHigher -> financialColors.onWarningContainer
                                isLower -> financialColors.onHealthyContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val badgeIcon = when {
                                isHigher -> Icons.AutoMirrored.Rounded.TrendingUp
                                isLower -> Icons.AutoMirrored.Rounded.TrendingDown
                                else -> Icons.AutoMirrored.Rounded.TrendingFlat
                            }
                            val badgeLabel = when {
                                isHigher -> stringResource(R.string.dashboard_six_month_vs_avg_up, abs(deltaPercent))
                                isLower -> stringResource(R.string.dashboard_six_month_vs_avg_down, abs(deltaPercent))
                                else -> stringResource(R.string.dashboard_six_month_vs_avg_even)
                            }
                            Surface(
                                shape = CircleShape,
                                color = badgeBg,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = badgeIcon,
                                        contentDescription = null,
                                        tint = badgeFg,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = badgeFg,
                                    )
                                }
                            }
                        }
                    }

                    // Vico Compose Material 3 Smooth 6-Month Line Chart
                    ProvideVicoTheme(rememberM3VicoTheme()) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(
                                    lineProvider = LineCartesianLayer.LineProvider.series(
                                        LineCartesianLayer.rememberLine(
                                            fill = LineCartesianLayer.LineFill.single(fill(lineColor)),
                                            areaFill = LineCartesianLayer.AreaFill.single(fill(areaFillColor)),
                                            pointConnector = LineCartesianLayer.PointConnector.cubic(curvature = 0.38f),
                                        )
                                    ),
                                ),
                                startAxis = VerticalAxis.rememberStart(
                                    valueFormatter = startAxisFormatter,
                                    itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 4 }) },
                                ),
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    valueFormatter = bottomAxisFormatter,
                                    guideline = null,
                                ),
                            ),
                            modelProducer = modelProducer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(172.dp)
                                .testTag("dashboard_six_month_vico_chart"),
                        )
                    }
                }
            }
        }
    }
}
