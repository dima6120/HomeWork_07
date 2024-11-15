package otus.homework.customview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import otus.homework.customview.piechart.PieChartData
import otus.homework.customview.piechart.PieChartView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val payloadData = loadPayloadData()
        val pieChartData = payloadData.map { it.toPieChartData() }
        val pieChartView = findViewById<PieChartView>(R.id.pieChart)
        val categoryTextView = findViewById<TextView>(R.id.category)
        val colorView = findViewById<View>(R.id.color)
        val amountTextView = findViewById<TextView>(R.id.amount)

        pieChartView.setData(pieChartData)
        pieChartView.setOnCategoryClick {
            colorView.setBackgroundColor(it.color)
            categoryTextView.text = it.name
            amountTextView.text = it.amount.toString()
        }
    }

    private fun loadPayloadData(): List<PayloadData> =
        resources.openRawResource(R.raw.payload).use {
            val jsonString = it.readBytes().decodeToString()

            Gson().fromJson(jsonString, PAYLOAD_DATA_LIST_TYPE)
        }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private val PAYLOAD_DATA_LIST_TYPE = object : TypeToken<List<PayloadData>>() {}.type
    }
}