package com.alwinsden.eightparams.health

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import kotlinx.datetime.TimeZone
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val XLabelsKey = ExtraStore.Key<List<String>>()
private val PointLabelsKey = ExtraStore.Key<List<String>>()

private val DiastolicColor = Color(0xFFFFA298)
private val MarkerBackground = Color(0xFFECECF1)

/** Number of buckets in the current window, per range. */
private fun bucketCount(range: ChartRange, data: ChartData): Int = when (range) {
    ChartRange.Day -> 24
    ChartRange.Week -> 7
    ChartRange.Month -> (((data.windowEndMillis - data.windowStartMillis) / 86_400_000L) + 1).toInt()
    ChartRange.SixMonths -> 27
    ChartRange.Year -> data.xLabels.size
}

/**
 * Exact index of a timestamp within the window's bucket grid. All ranges use
 * fixed-span buckets (hours / days / weeks) except Year, whose monthly
 * buckets fall back to a proportional estimate — labels for Year are mapped
 * positionally instead.
 */
private fun bucketIndexOf(range: ChartRange, millis: Long, data: ChartData): Int = when (range) {
    ChartRange.Day -> ((millis - data.windowStartMillis) / 3_600_000L).toInt()
    ChartRange.Week, ChartRange.Month -> ((millis - data.windowStartMillis) / 86_400_000L).toInt()
    ChartRange.SixMonths -> ((millis - data.windowStartMillis) / 604_800_000L).toInt()
    ChartRange.Year -> ((millis - data.windowStartMillis).toDouble() / 2_592_000_000.0).roundToInt()
}

/**
 * Labels only the given bucket indices — Vico forbids blank label strings, so
 * sparse Apple-style x labels (e.g. every 6th hour, month starts) need a
 * custom [HorizontalAxis.ItemPlacer]. Guidelines follow the labels.
 */
private class SparseLabelPlacer(
    private val labelIndices: List<Int>,
) : HorizontalAxis.ItemPlacer {
    private val values = labelIndices.map { it.toDouble() }

    override fun getLabelValues(
        context: com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = values

    override fun getWidthMeasurementLabelValues(
        context: com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext,
        layerDimensions: com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
    ): List<Double> = values

    override fun getHeightMeasurementLabelValues(
        context: com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext,
        layerDimensions: com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = values

    override fun getShiftExtremeLabels(
        context: com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext,
    ): Boolean = true

    override fun getStartLayerMargin(
        context: com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext,
        layerDimensions: com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = 0f

    override fun getEndLayerMargin(
        context: com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext,
        layerDimensions: com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = 0f
}

/**
 * Apple Health–style metric chart rendered with Vico (smooth data-change
 * animations): right-hand y labels, dashed gridlines, hollow-circle points,
 * and exactly ONE value bubble — at [selectedPoint], defaulting to the latest
 * point (M/6M/Y bubble from the reference screenshot).
 *
 * Interactions:
 *  - tapping a tick moves the bubble to it (tapping the same tick or empty
 *    space deselects, returning the bubble to the latest point);
 *  - the horizontal drag gesture pans the window into the past (whole
 *    periods, clamped by [ChartData.maxBackSteps]), so the user can scroll
 *    back to their first entries.
 */
@Composable
fun MetricLineChart(
    data: ChartData,
    type: MetricType,
    range: ChartRange,
    selectedPoint: ChartPoint?,
    onPointSelected: (ChartPoint?) -> Unit,
    onPanSteps: (Int) -> Unit,
    panStepMillis: Long,
    modifier: Modifier = Modifier,
) {
    val tz = remember { TimeZone.currentSystemDefault() }
    val color = metricColor(type)
    val producer = remember { CartesianChartModelProducer() }

    // x-label text per bucket index ("" where a bucket carries no label).
    val xLabelList: List<String> = remember(data, range) {
        val count = bucketCount(range, data)
        val list = MutableList(count) { "" }
        if (range == ChartRange.Year) {
            // Year labels map positionally (variable month lengths).
            data.xLabels.forEachIndexed { i, label -> if (i < count) list[i] = label.text }
        } else {
            data.xLabels.forEach { label ->
                val idx = bucketIndexOf(range, label.atMillis, data)
                if (idx in list.indices) list[idx] = label.text
            }
        }
        list
    }

    // ChartPoint per bucket index — drives the series x values and the
    // marker bubble's date-range label.
    val pointByIndex: Map<Int, ChartPoint> = remember(data, range) {
        buildMap {
            data.points.forEach { point ->
                val idx = bucketIndexOf(range, point.bucketStartMillis, data)
                if (idx >= 0) put(idx, point)
            }
            if (range == ChartRange.Year) {
                // Fall back to positional indices for Year (estimate may drift).
                data.points.forEachIndexed { i, point -> put(i, point) }
            }
        }
    }

    val pointLabelList: List<String> = remember(data, range, pointByIndex) {
        val count = bucketCount(range, data)
        val list = MutableList(count) { "" }
        pointByIndex.forEach { (idx, point) ->
            if (idx in list.indices) list[idx] = ChartAggregation.pointRangeLabel(range, point, tz)
        }
        list
    }

    LaunchedEffect(data, type) {
        val xs = pointByIndex.keys.sorted().map { it.toDouble() }
        producer.runTransaction {
            lineModel {
                if (type == MetricType.BloodPressure) {
                    series(xs, data.points.map { it.value })
                    series(xs, data.points.map { it.value2 ?: it.value })
                } else {
                    series(xs, data.points.map { it.value })
                }
            }
            extras { extraStore ->
                extraStore[XLabelsKey] = xLabelList
                extraStore[PointLabelsKey] = pointLabelList
            }
        }
    }

    // ---- styling -----------------------------------------------------------

    val gridLine = rememberLineComponent(
        fill = Fill(HealthColors.Grid),
        thickness = 1.dp,
        shape = DashedShape(),
    )
    val axisLabel = rememberTextComponent(
        style = TextStyle(color = HealthColors.TextTertiary, fontSize = 11.sp),
    )
    val markerLabel = rememberTextComponent(
        style = TextStyle(
            color = HealthColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        lineCount = 2,
        background = rememberShapeComponent(Fill(MarkerBackground), RoundedCornerShape(12.dp)),
        padding = Insets(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 6.dp),
    )

    val currentType by rememberUpdatedState(type)
    val markerValueFormatter = remember {
        object : DefaultCartesianMarker.ValueFormatter {
            override fun format(
                context: com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext,
                targets: List<CartesianMarker.Target>,
            ): CharSequence {
                val target = targets.firstOrNull() as? LineCartesianLayerMarkerTarget ?: return ""
                val t = currentType
                val ys = target.points.map { it.entry.y }
                val valueText = when {
                    t == MetricType.BloodPressure && ys.size >= 2 -> "${ys[0].toInt()}/${ys[1].toInt()}"
                    else -> formatValue(ys.first().toDouble())
                }
                val unit = if (t == MetricType.BloodPressure) "mmHg" else t.unit
                val idx = target.x.roundToInt()
                val rangeLabel = context.model.extraStore.getOrNull(PointLabelsKey)
                    ?.getOrElse(idx) { "" } ?: ""
                return buildString {
                    append(valueText).append(' ').append(unit)
                    if (rangeLabel.isNotEmpty()) append('\n').append(rangeLabel)
                }
            }
        }
    }

    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = markerValueFormatter,
        guideline = rememberLineComponent(fill = Fill(HealthColors.Cursor), thickness = 2.dp),
        indicator = null,
    )

    // ---- line layer --------------------------------------------------------

    val mainPoint = rememberShapeComponent(
        fill = Fill(Color.White),
        shape = CircleShape,
        strokeFill = Fill(color),
        strokeThickness = 2.dp,
    )
    val mainLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(color)),
        stroke = LineCartesianLayer.LineStroke.Continuous(2.4.dp),
        pointProvider = LineCartesianLayer.PointProvider.single(
            LineCartesianLayer.Point(mainPoint, size = 9.dp),
        ),
    )
    val lineProvider = if (type == MetricType.BloodPressure) {
        val diaPoint = rememberShapeComponent(
            fill = Fill(Color.White),
            shape = CircleShape,
            strokeFill = Fill(DiastolicColor),
            strokeThickness = 2.dp,
        )
        val diaLine = LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(DiastolicColor)),
            stroke = LineCartesianLayer.LineStroke.Continuous(2.dp),
            pointProvider = LineCartesianLayer.PointProvider.single(
                LineCartesianLayer.Point(diaPoint, size = 8.dp),
            ),
        )
        LineCartesianLayer.LineProvider.series(diaLine, mainLine)
    } else {
        LineCartesianLayer.LineProvider.series(mainLine)
    }

    // ---- marker visibility → header sync ----------------------------------

    // Selection is owned by the screen; the bubble is rendered via
    // persistentMarkers at the selected point (or the latest point).

    // ---- gestures ----------------------------------------------------------

    val currentSelectedPoint by rememberUpdatedState(selectedPoint)
    val currentDataForGestures by rememberUpdatedState(data)
    val currentRangeForGestures by rememberUpdatedState(range)
    val onPointSelectedState by rememberUpdatedState(onPointSelected)
    val gestureModifier = Modifier.pointerInput(Unit) {
        val hitRadius = 28.dp.toPx()
        detectTapGestures { position ->
            val d = currentDataForGestures
            if (!d.hasData) {
                onPointSelectedState(null)
                return@detectTapGestures
            }
            val count = bucketCount(currentRangeForGestures, d)
            if (count < 2) {
                onPointSelectedState(d.points.firstOrNull())
                return@detectTapGestures
            }
            // Approximate Vico's plot geometry for hit testing: the end axis
            // reserves ~40dp on the right, the bottom axis ~24dp below.
            val density = this
            val plotLeft = with(density) { 8.dp.toPx() }
            val plotRight = size.width - with(density) { 40.dp.toPx() }
            val plotTop = with(density) { 6.dp.toPx() }
            val plotBottom = size.height - with(density) { 24.dp.toPx() }
            fun xOf(index: Int) = plotLeft + index.toDouble() / (count - 1) * (plotRight - plotLeft)
            fun yOf(value: Double): Float {
                val span = (d.yMax - d.yMin).coerceAtLeast(1e-9)
                return (plotBottom - ((value - d.yMin) / span).coerceIn(0.0, 1.0) * (plotBottom - plotTop)).toFloat()
            }
            var best: ChartPoint? = null
            var bestDist = Float.MAX_VALUE
            for (point in d.points) {
                val px = xOf(bucketIndexOf(currentRangeForGestures, point.bucketStartMillis, d)).toFloat()
                val py = yOf(point.value)
                val dx = position.x - px
                val dy = position.y - py
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= hitRadius && dist < bestDist) {
                    best = point
                    bestDist = dist
                }
            }
            when {
                best == null -> onPointSelectedState(null)
                best == currentSelectedPoint -> onPointSelectedState(null) // toggle off
                else -> onPointSelectedState(best)
            }
        }
    }
    val currentPanStepMillis by rememberUpdatedState(panStepMillis)
    val onPanStepsState by rememberUpdatedState(onPanSteps)
    val panModifier = Modifier.pointerInput(Unit) {
        var accX = 0f
        var emitted = 0
        detectHorizontalDragGestures(
            onDragStart = { accX = 0f; emitted = 0 },
            onDragEnd = { accX = 0f; emitted = 0 },
            onDragCancel = { accX = 0f; emitted = 0 },
        ) { change, dragAmount ->
            change.consume()
            val d = currentDataForGestures
            if (!d.hasData) return@detectHorizontalDragGestures
            accX += dragAmount
            val plotWidth = (size.width - with(this@pointerInput) { 46.dp.toPx() }).coerceAtLeast(1f)
            val windowSpan = (d.windowEndMillis - d.windowStartMillis).coerceAtLeast(1L).toFloat()
            val pxPerStep = plotWidth * currentPanStepMillis / windowSpan
            if (pxPerStep > 0f) {
                // Dragging right (accX > 0) slides the window back in time.
                val target = (accX / pxPerStep).roundToInt().coerceIn(0, d.maxBackSteps)
                if (target != emitted) {
                    onPanStepsState(target - emitted)
                    emitted = target
                }
            }
        }
    }

    // ---- host --------------------------------------------------------------

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = lineProvider,
                rangeProvider = CartesianLayerRangeProvider.fixed(
                    minX = 0.0,
                    maxX = (bucketCount(range, data) - 1).toDouble(),
                    minY = data.yMin,
                    maxY = data.yMax,
                ),
            ),
            endAxis = VerticalAxis.rememberEnd(
                line = null,
                tick = null,
                guideline = gridLine,
                label = axisLabel,
                itemPlacer = VerticalAxis.ItemPlacer.count(count = { data.yTicks.size }),
                valueFormatter = CartesianValueFormatter { _, value, _ -> formatValue(value) },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                line = null,
                tick = null,
                guideline = gridLine,
                label = axisLabel,
                itemPlacer = SparseLabelPlacer(xLabelList.indices.filter { xLabelList[it].isNotEmpty() }),
                valueFormatter = CartesianValueFormatter { context, x, _ ->
                    // Never return blank — Vico throws on blank axis labels.
                    // A "·" can only flash during range-switch re-measures,
                    // before the new transaction's extras land.
                    context.model.extraStore.getOrNull(XLabelsKey)
                        ?.getOrElse(x.roundToInt()) { "" }
                        ?.ifBlank { "·" } ?: "·"
                },
            ),
            marker = null,
            getXStep = { _, _, _ -> 1.0 },
            persistentMarkers = { _ ->
                // Exactly one bubble: the selected tick, else the latest one.
                val count = bucketCount(range, data)
                val selected = selectedPoint
                    ?.takeIf { bucketIndexOf(range, it.bucketStartMillis, data) in 0 until count }
                val target = selected ?: data.points.lastOrNull()
                target?.let {
                    marker at bucketIndexOf(range, it.bucketStartMillis, data).toDouble()
                }
            },
        ),
        modelProducer = producer,
        modifier = modifier.then(gestureModifier).then(panModifier),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(
            zoomEnabled = false,
            // Fit the whole window to the viewport; the default would stay at
            // 1x and let long windows (D = 24 buckets) overflow off-screen.
            initialZoom = Zoom.Content,
        ),
    )
}
