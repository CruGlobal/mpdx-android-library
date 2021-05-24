package org.mpdx.androids.library.base.databinding

import androidx.databinding.BindingAdapter
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.mpdx.R
import org.mpdx.androids.library.base.model.CombineChartLabelValueData

@BindingAdapter("barChartData")
internal fun CombinedChart.setUpChart(combineChartLabelValueData: ArrayList<CombineChartLabelValueData>?) {
    if (combineChartLabelValueData == null) return
    val barEntries = arrayListOf<BarEntry>()
    val labelNames = arrayListOf<String>()
    val committedEntries = arrayListOf<Entry>()
    combineChartLabelValueData.forEachIndexed { i, data ->
        barEntries.add(BarEntry(i.toFloat(), data.value))
        labelNames.add(data.label)
        committedEntries.add(Entry(i.toFloat(), data.committed))
    }

    val committedLineDataSet = LineDataSet(committedEntries, context.getString(R.string.commitments))
    committedLineDataSet.color = Color.YELLOW
    committedLineDataSet.setDrawCircleHole(false)
    committedLineDataSet.setDrawCircles(false)
    val barDataSet = BarDataSet(barEntries, context.getString(R.string.donations_for_month))
    description.isEnabled = false
    val newCombinedData = CombinedData()
    newCombinedData.setData(LineData(committedLineDataSet).apply { setValueTextColor(android.R.color.transparent) })
    newCombinedData.setData(BarData(barDataSet))
    data = newCombinedData
    xAxis.valueFormatter = IndexAxisValueFormatter(labelNames)
    xAxis.position = XAxis.XAxisPosition.BOTTOM
    xAxis.setDrawGridLines(false)
    xAxis.setDrawAxisLine(false)
    xAxis.granularity = 1f
    xAxis.labelCount = labelNames.size
    xAxis.labelRotationAngle = 270f
    animateY(2000)

    legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
    invalidate()
}
