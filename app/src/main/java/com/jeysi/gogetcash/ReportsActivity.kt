package com.jeysi.gogetcash

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportsActivity : AppCompatActivity() {

    // --- UI Views ---
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvCurrentMonth: TextView
    private lateinit var btnChangeMonth: Button
    private lateinit var tvTotalIncome: TextView

    private lateinit var tvTotalLoanFees: TextView
    private lateinit var tvTotalCashInFees: TextView
    private lateinit var tvTotalCashOutFees: TextView
    private lateinit var barChart: BarChart

    // --- Data & Firebase ---
    private var userName: String? = null
    private val allTransactions = mutableListOf<AnyTransaction>()
    private val selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        userName = intent.getStringExtra("USER_NAME")
        if (userName.isNullOrEmpty()) {
            showToast("User not identified. Cannot generate report.")
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        setupListeners()
        fetchAllTransactionData()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth)
        btnChangeMonth = findViewById(R.id.btnChangeMonth)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalLoanFees = findViewById(R.id.tvTotalLoanFees)
        tvTotalCashInFees = findViewById(R.id.tvTotalCashInFees)
        tvTotalCashOutFees = findViewById(R.id.tvTotalCashOutFees)
        barChart = findViewById(R.id.barChart)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            finish() // Go back to the previous screen
        }
    }

    private fun setupListeners() {
        btnChangeMonth.setOnClickListener {
            showDatePicker()
        }
    }

    private fun fetchAllTransactionData() {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTransactions.clear()

                // Fetch Loans
                snapshot.child("history").children.forEach { data ->
                    val loan = data.getValue(Transaction::class.java)
                    if (loan != null && data.key != null) {
                        allTransactions.add(AnyTransaction(data.key!!, "Loan", loan.loanerName, loan.amount, loan.fee, loan.timestamp))
                    }
                }

                // Fetch Cash In
                snapshot.child("Cashin").children.forEach { data ->
                    val cashIn = data.getValue(CashFlowTransaction::class.java)
                    if (cashIn != null && data.key != null) {
                        allTransactions.add(AnyTransaction(data.key!!, "Cash In", cashIn.name, cashIn.amount, cashIn.fee, cashIn.timestamp))
                    }
                }

                // Fetch Cash Out
                snapshot.child("Cashout").children.forEach { data ->
                    val cashOut = data.getValue(CashFlowTransaction::class.java)
                    if (cashOut != null && data.key != null) {
                        allTransactions.add(AnyTransaction(data.key!!, "Cash Out", cashOut.name, cashOut.amount, cashOut.fee, cashOut.timestamp))
                    }
                }

                // Initial processing for the current date
                updateReportForSelectedDate()
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load report data: ${error.message}")
            }
        })
    }

    private fun updateReportForSelectedDate() {
        // Update the display text to show the full date
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        tvCurrentMonth.text = dateFormat.format(selectedDate.time)

        // Filter transactions for the selected day, month and year
        val filteredTransactions = allTransactions.filter {
            val transactionCalendar = Calendar.getInstance()
            transactionCalendar.timeInMillis = it.timestamp

            transactionCalendar.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH) &&
                    transactionCalendar.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
                    transactionCalendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
        }

        // Calculate totals from filtered data
        val totalLoanFee = filteredTransactions.filter { it.type == "Loan" }.sumOf { it.fee }
        val totalCashInFee = filteredTransactions.filter { it.type == "Cash In" }.sumOf { it.fee }
        val totalCashOutFee = filteredTransactions.filter { it.type == "Cash Out" }.sumOf { it.fee }

        val totalIncome = totalLoanFee + totalCashInFee + totalCashOutFee
        val netProfit = totalIncome // Assuming expenses are 0

        // Update UI
        tvTotalIncome.text = String.format("₱%.2f", totalIncome)

        tvTotalLoanFees.text = String.format("₱%.2f", totalLoanFee)
        tvTotalCashInFees.text = String.format("₱%.2f", totalCashInFee)
        tvTotalCashOutFees.text = String.format("₱%.2f", totalCashOutFee)

        // Update Chart
        setupBarChart(totalLoanFee, totalCashInFee, totalCashOutFee)
    }

    private fun setupBarChart(loanFees: Double, cashInFees: Double, cashOutFees: Double) {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, loanFees.toFloat()))
        entries.add(BarEntry(1f, cashInFees.toFloat()))
        entries.add(BarEntry(2f, cashOutFees.toFloat()))

        val labels = listOf("Loans", "Cash In", "Cash Out")

        val dataSet = BarDataSet(entries, "Income Sources")
        dataSet.colors = listOf(
            Color.rgb(30, 30, 30),     // Deep Black for Total Transactions
            Color.rgb(50, 50, 50),     // Soft Charcoal for Secondary Data
            Color.rgb(70, 70, 70)      // Muted Graphite for Background or Inactive Bars
        )
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        barChart.data = BarData(dataSet)

        // Customize X-axis
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = labels.getOrNull(value.toInt()) ?: ""
        }
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelCount = labels.size

        // General Chart Customization
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate() // Refresh chart
    }

    private fun showDatePicker() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(Calendar.YEAR, selectedYear)
                selectedDate.set(Calendar.MONTH, selectedMonth)
                selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay)
                updateReportForSelectedDate()
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}