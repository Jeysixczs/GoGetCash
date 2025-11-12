package com.jeysi.gogetcash

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.*

class PaidActivity : AppCompatActivity() {

    // --- UI Views ---
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvPaidHistory: RecyclerView
    private lateinit var tvEmptyState: View
    private lateinit var etSearch: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var ivSearch: ImageView

    // --- Adapter and Data ---
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Pair<String, Transaction>>()
    private val filteredTransactionList = mutableListOf<Pair<String, Transaction>>()

    // --- Firebase ---
    private lateinit var historyRef: DatabaseReference
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paid)

        userName = intent.getStringExtra("USER_NAME")
        if (userName.isNullOrEmpty()) {
            showToast("Error: User not identified.")
            finish()
            return
        }

        historyRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!).child("history")

        initializeViews()
        setupRecyclerView()
        setupSearchFunctionality()
        setupFirebaseListener()
        setupBackPressedHandler()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        rvPaidHistory = findViewById(R.id.rvPaidHistory)
        etSearch = findViewById(R.id.etSearch)
        ivClearSearch = findViewById(R.id.ivClearSearch)
        ivSearch = findViewById(R.id.ivSearch)

        tvEmptyState = findViewById(R.id.emptyState)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(filteredTransactionList) { loanId ->
            val intent = Intent(this, LoanDetailsActivity::class.java).apply {
                putExtra("USER_NAME", userName)
                putExtra("LOAN_ID", loanId)
            }
            startActivity(intent)
        }
        rvPaidHistory.layoutManager = LinearLayoutManager(this)
        rvPaidHistory.adapter = transactionAdapter
    }

    private fun setupSearchFunctionality() {
        // Clear search button
        ivClearSearch.setOnClickListener {
            etSearch.setText("")
        }

        // Search text watcher
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTransactions(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // Show/hide clear button based on whether there's text
                ivClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        })
    }

    private fun filterTransactions(query: String) {
        filteredTransactionList.clear()

        if (query.isEmpty()) {
            // If query is empty, show all transactions
            filteredTransactionList.addAll(transactionList)
        } else {
            // Filter transactions based on query
            val lowerCaseQuery = query.lowercase()
            transactionList.forEach { (loanId, transaction) ->
                // Search in name, amount, or loan ID
                val matchesName = transaction.loanerName?.lowercase()?.contains(lowerCaseQuery) == true
                val matchesAmount = String.format("%.2f", transaction.amount).contains(lowerCaseQuery)
                val matchesLoanId = loanId.lowercase().contains(lowerCaseQuery)

                if (matchesName || matchesAmount || matchesLoanId) {
                    filteredTransactionList.add(Pair(loanId, transaction))
                }
            }
        }

        transactionAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (etSearch.text.isNotEmpty()) {
                    etSearch.setText("")
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setupFirebaseListener() {
        historyRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!).child("history")

        // This query fetches only PAID loans (paid = true)
        val query = historyRef.orderByChild("paid").equalTo(true)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()

                for (transactionSnapshot in snapshot.children) {
                    val loanId = transactionSnapshot.key
                    val transaction = transactionSnapshot.getValue(Transaction::class.java)

                    if (loanId != null && transaction != null) {
                        transactionList.add(Pair(loanId, transaction))
                    }
                }
                transactionList.sortByDescending { it.second.timestamp }

                // Apply current search filter to the new data
                filterTransactions(etSearch.text.toString())

                // Update empty state
                updateEmptyState()
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load paid loans: ${error.message}")
            }
        })
    }

    private fun updateEmptyState() {
        if (filteredTransactionList.isEmpty()) {
            if (etSearch.text.isNotEmpty()) {
                // Show search-specific empty state
                findViewById<TextView>(R.id.tvEmptyTitle).text = "No Results Found"
                findViewById<TextView>(R.id.tvEmptySubtitle).text = "No paid loans match your search criteria."
            } else {
                // Show default empty state
                findViewById<TextView>(R.id.tvEmptyTitle).text = "No Paid Loans"
                findViewById<TextView>(R.id.tvEmptySubtitle).text = "You haven't completed any loans yet."
            }
            tvEmptyState.visibility = View.VISIBLE
        } else {
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}