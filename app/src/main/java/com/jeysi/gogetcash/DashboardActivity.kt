package com.jeysi.gogetcash

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class Transaction(
    var loanerName: String = "",
    var phoneNumber: String = "",
    var amount: Double = 0.0,
    var fee: Double = 0.0,
    var startDate: Long = 0,
    var dueDate: Long = 0,
    var paid: Boolean = false,
    var description: String = "",
    var timestamp: Long = 0
) {

    constructor() : this("", "", 0.0, 0.0, 0, 0, false, "", 0)
}


class TransactionAdapter(
    private var transactions: MutableList<Pair<String, Transaction>>,
    private val onItemClicked: (String) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLoanerName: TextView = view.findViewById(R.id.tvLoanerName)
        val tvTransactionDate: TextView = view.findViewById(R.id.tvTransactionDate)
        val tvLoanAmount: TextView = view.findViewById(R.id.tvLoanAmount)
        val tvFee: TextView = view.findViewById(R.id.tvFee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val (loanId, transaction) = transactions[position]
        holder.tvLoanerName.text = transaction.loanerName

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvTransactionDate.text = sdf.format(Date(transaction.timestamp))
        holder.tvFee.text = String.format("Fee: ₱%.2f", transaction.fee)


        // Updated to use lowercase 'paid'
        if (transaction.paid) { // Changed from transaction.Paid to transaction.paid
            // For paid loans, show a positive amount and set the color to GRAY.
            holder.tvLoanAmount.text = String.format("₱%.2f", transaction.amount)
            holder.tvLoanAmount.setTextColor(Color.GRAY)
        } else {
            // For unpaid loans, show a negative amount and set the color to RED.
            holder.tvLoanAmount.text = String.format("-₱%.2f", transaction.amount)
            holder.tvLoanAmount.setTextColor(Color.RED)
        }

        holder.itemView.setOnClickListener {
            if (loanId.isNotEmpty()) {
                onItemClicked(loanId)
            }
        }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<Pair<String, Transaction>>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
    }
}



class DashboardActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private var previousMenuItem: MenuItem? = null


    // --- UI Views ---
    private lateinit var tvWelcomeName: TextView
    private lateinit var tvGcashBalance: TextView
    private lateinit var tvOnHandBalance: TextView
    private lateinit var rvRecentHistory: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var tvViewAll: TextView
    private lateinit var tvViewPaid: TextView
    private lateinit var actionCashin: LinearLayout
    private lateinit var actionCashout: LinearLayout
    private lateinit var actionReports: LinearLayout

    private lateinit var tvTotalLoanBalance: TextView

    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Pair<String, Transaction>>()

    private lateinit var userRef: DatabaseReference
    private lateinit var historyRef: DatabaseReference
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_design_activity)

        userName = intent.getStringExtra("USER_NAME")

        if (userName.isNullOrEmpty()) {

            finish()

            return
        }

        initializeViews()
        tvWelcomeName.text = "Welcome, $userName!"

        setupRecyclerView()
        setupClickListeners()
        setupFirebaseListeners()
        setupBottomNavigation()
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }


    private fun setupClickListeners() {
        tvViewAll.setOnClickListener {
            val intent = Intent(this, UnpaidActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
        }
        tvViewPaid.setOnClickListener {
            val intent = Intent(this, PaidActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
        }

        actionCashin.setOnClickListener {
            val intent = Intent(this, CashInActivity::class.java).apply {
                putExtra("USER_NAME", userName)
            }
            startActivity(intent)
        }

        actionCashout.setOnClickListener {
            val intent = Intent(this, CashOutActivity::class.java).apply {
                putExtra("USER_NAME", userName)
            }
            startActivity(intent)
        }

        actionReports.setOnClickListener {
            val intent = Intent(this, ReportsActivity::class.java).apply {
                putExtra("USER_NAME", userName)
            }
            startActivity(intent)
        }
    }



    private fun initializeViews() {
        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvGcashBalance = findViewById(R.id.tvGcashBalance)
        tvOnHandBalance = findViewById(R.id.tvOnHandBalance)
        rvRecentHistory = findViewById(R.id.rvRecentHistory)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        tvViewAll = findViewById(R.id.tvViewAll)
        tvViewPaid = findViewById(R.id.tvViewPaid)
        tvTotalLoanBalance = findViewById(R.id.tvTotalLoanBalance)
        actionCashin = findViewById(R.id.actionCashin)
        actionCashout = findViewById(R.id.actionCashout)
        actionReports = findViewById(R.id.actionReports)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(transactionList) { loanId ->
            if (loanId.isNotEmpty()) {
                val intent = Intent(this, LoanDetailsActivity::class.java).apply {
                    putExtra("USER_NAME", userName)
                    putExtra("LOAN_ID", loanId)
                }
                startActivity(intent)
            }
        }
        rvRecentHistory.layoutManager = LinearLayoutManager(this)
        rvRecentHistory.adapter = transactionAdapter
    }

    private fun setupFirebaseListeners() {
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!)
        historyRef = userRef.child("history")

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gcashBalance = snapshot.child("gcashBalance").getValue(Double::class.java) ?: 0.0
                val onHandBalance = snapshot.child("onHandBalance").getValue(Double::class.java) ?: 0.0
                tvGcashBalance.text = String.format("₱%.2f", gcashBalance)
                tvOnHandBalance.text = String.format("₱%.2f", onHandBalance)
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to read balance: ${error.message}")
            }
        })

        historyRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()
                var totalLoanAmount = 0.0

                for (transactionSnapshot in snapshot.children) {
                    val loanId = transactionSnapshot.key
                    val transaction = transactionSnapshot.getValue(Transaction::class.java)
                    if (loanId != null && transaction != null) {
                        transactionList.add(Pair(loanId, transaction))

                        if (!transaction.paid) {
                            totalLoanAmount += transaction.amount
                        }
                    }
                }
                transactionList.reverse()

                val recentTransactions = transactionList.take(4)
                transactionAdapter.updateData(recentTransactions)

                tvTotalLoanBalance.text = String.format("₱%.2f", totalLoanAmount)
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load history: ${error.message}")
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.menu.findItem(R.id.nav_dashboard).isChecked = true

        bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNavigationView.selectedItemId) {
                return@setOnItemSelectedListener false
            }

            val intent = when (item.itemId) {
                R.id.nav_add_loan -> Intent(this, AddLoanActivity::class.java)
                R.id.nav_history -> Intent(this, HistoryActivity::class.java)
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                else -> null
            }

            intent?.let {
                it.putExtra("USER_NAME", userName)
                startActivity(it)

            }
            return@setOnItemSelectedListener (item.itemId != R.id.nav_dashboard)
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.menu.findItem(R.id.nav_dashboard).isChecked = true
    }

    override fun onPause() {
        super.onPause()

    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
