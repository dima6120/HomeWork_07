package otus.homework.customview.piechart

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PieChartCategory(
    val name: String,
    val amount: Int,
    val color: Int
): Parcelable
