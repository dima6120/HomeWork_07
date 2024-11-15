package otus.homework.customview

import otus.homework.customview.piechart.PieChartData

data class PayloadData(
    val id: Int,
    val name: String,
    val amount: Int,
    val category: String,
    val time: Long
)

fun PayloadData.toPieChartData(): PieChartData = PieChartData(amount, category)
