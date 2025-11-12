package com.jeysi.gogetcash

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryActivity : AppCompatActivity() {

    // --- UI Views ---
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvHistory: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    // --- Adapter and Data ---
    private lateinit var historyAdapter: HistoryAdapter
    private val fullTransactionList = mutableListOf<AnyTransaction>()
    private val filteredList = mutableListOf<AnyTransaction>()

    // --- Firebase and User ---
    private lateinit var userRef: DatabaseReference
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_design_activity)

        userName = intent.getStringExtra("USER_NAME")
        if (userName.isNullOrEmpty()) {
            Toast.makeText(this, "Error: User not identified. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupBottomNavigation()
        loadAllTransactions()
    }

    override fun onResume() {
        super.onResume()
        // Ensure the correct menu item is selected when the user returns
        bottomNavigationView.menu.findItem(R.id.nav_history).isChecked = true
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        rvHistory = findViewById(R.id.rvHistory)
        etSearch = findViewById(R.id.etSearch)
        ivClearSearch = findViewById(R.id.ivClearSearch)
        emptyState = findViewById(R.id.emptyState)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(filteredList) { transaction ->
            if (transaction.type == "Loan") {
                navigateToLoanDetails(transaction.id)
            } else {
                Toast.makeText(this, "${transaction.type} transaction clicked", Toast.LENGTH_SHORT).show()
            }
        }
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = historyAdapter
    }

    private fun setupSearch() {
        ivClearSearch.setOnClickListener {
            etSearch.text.clear()
            hideKeyboard()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {
                ivClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.menu.findItem(R.id.nav_history).isChecked = true

        bottomNavigationView.setOnItemSelectedListener { item ->
            // Prevent reloading the same screen
            if (item.itemId == bottomNavigationView.selectedItemId) {
                return@setOnItemSelectedListener false
            }

            val intent = when (item.itemId) {
                R.id.nav_dashboard -> Intent(this, DashboardActivity::class.java)
                R.id.nav_add_loan -> Intent(this, AddLoanActivity::class.java)
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                else -> null
            }

            intent?.let {
                it.putExtra("USER_NAME", userName)
                startActivity(it)
            }

            return@setOnItemSelectedListener (item.itemId != R.id.nav_history)
        }
    }

    private fun loadAllTransactions() {
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fullTransactionList.clear()

                // 1. Fetch Loans from "history"
                snapshot.child("history").children.forEach { data ->
                    val loan = data.getValue(Transaction::class.java)
                    if (loan != null && data.key != null) {
                        fullTransactionList.add(AnyTransaction(
                            id = data.key!!, type = "Loan", name = loan.loanerName,
                            amount = loan.amount, fee = loan.fee, timestamp = loan.timestamp, isPaid = loan.paid
                        ))
                    }
                }

                // 2. Fetch from "Cashin"
                snapshot.child("Cashin").children.forEach { data ->
                    val cashIn = data.getValue(CashFlowTransaction::class.java)
                    if (cashIn != null && data.key != null) {
                        fullTransactionList.add(AnyTransaction(
                            id = data.key!!, type = "Cash In", name = cashIn.name,
                            amount = cashIn.amount, fee = cashIn.fee, timestamp = cashIn.timestamp
                        ))
                    }
                }

                // 3. Fetch from "Cashout"
                snapshot.child("Cashout").children.forEach { data ->
                    val cashOut = data.getValue(CashFlowTransaction::class.java)
                    if (cashOut != null && data.key != null) {
                        fullTransactionList.add(AnyTransaction(
                            id = data.key!!, type = "Cash Out", name = cashOut.name,
                            amount = cashOut.amount, fee = cashOut.fee, timestamp = cashOut.timestamp
                        ))
                    }
                }

                fullTransactionList.sortByDescending { it.timestamp }
                filter(etSearch.text.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HistoryActivity, "Failed to load history: ${error.message}", Toast.LENGTH_SHORT).show()
                updateEmptyState(isSearch = false)
            }
        })
    }

    private fun filter(query: String) {
        filteredList.clear()
        val searchQuery = query.lowercase().trim()

        if (searchQuery.isEmpty()) {
            filteredList.addAll(fullTransactionList)
        } else {
            fullTransactionList.forEach { transaction ->
                if (transaction.name.lowercase().contains(searchQuery) ||
                    transaction.amount.toString().contains(searchQuery) ||
                    transaction.type.lowercase().contains(searchQuery)) {
                    filteredList.add(transaction)
                }
            }
        }
        historyAdapter.notifyDataSetChanged()
        updateEmptyState(isSearch = searchQuery.isNotEmpty())
    }

    private fun updateEmptyState(isSearch: Boolean) {
        if (filteredList.isEmpty()) {
            rvHistory.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            if (isSearch) {
                tvEmptyTitle.text = getString(R.string.no_results_found)
                tvEmptySubtitle.text = getString(R.string.no_transactions_match_search)
            } else {
                tvEmptyTitle.text = getString(R.string.no_transactions_yet)
                tvEmptySubtitle.text = getString(R.string.transactions_will_appear_here)
            }
        } else {
            rvHistory.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun navigateToLoanDetails(loanId: String) {
        val intent = Intent(this, LoanDetailsActivity::class.java).apply {
            putExtra("USER_NAME", userName)
            putExtra("LOAN_ID", loanId)
        }
        startActivity(intent)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    override fun onPause() {
        super.onPause()
    }
}
