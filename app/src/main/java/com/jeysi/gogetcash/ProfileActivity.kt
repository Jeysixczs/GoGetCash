package com.jeysi.gogetcash

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageButton // Changed from MaterialButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    // --- UI Views ---
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvGcashBalance: TextView
    private lateinit var tvOnHandBalance: TextView
    private lateinit var tvTotalLoans: TextView
    private lateinit var tvActiveLoans: TextView
    private lateinit var btnAddGcash: ImageButton // Changed from MaterialButton
    private lateinit var btnAddOnHand: ImageButton // Changed from MaterialButton
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar

    // --- Firebase ---
    private lateinit var userRef: DatabaseReference
    private lateinit var historyRef: DatabaseReference
    private var userName: String? = null

    // --- Constants ---
    companion object {
        private const val MAX_DEPOSIT_AMOUNT = 1000000.0
        private const val BALANCE_GCASH = "gcashBalance"
        private const val BALANCE_ON_HAND = "onHandBalance"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_design_activity)

        userName = intent.getStringExtra("USER_NAME")
        if (userName.isNullOrEmpty()) {
            showToast("Error: User not identified.")
            finish()
            return
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!)
        historyRef = userRef.child("history")

        initializeViews()
        setupToolbar()
        setupFirebaseListeners()
        setupClickListeners()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.menu.findItem(R.id.nav_profile).isChecked = true
    }

    override fun onPause() {
        super.onPause()
    }

    private fun initializeViews() {
        tvProfileName = findViewById(R.id.tvProfileCardName)
        tvProfileEmail = findViewById(R.id.tvProfileCardEmail)
        tvGcashBalance = findViewById(R.id.tvGcashBalance)
        tvOnHandBalance = findViewById(R.id.tvOnHandBalance)
        tvTotalLoans = findViewById(R.id.tvTotalLoans)
        tvActiveLoans = findViewById(R.id.tvActiveLoans)
        btnAddGcash = findViewById(R.id.btnAddGcash)
        btnAddOnHand = findViewById(R.id.btnAddOnHand)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupBottomNavigation() {
        // We set the initial state here, but onResume will correct it on return.
        bottomNavigationView.menu.findItem(R.id.nav_profile).isChecked = true

        bottomNavigationView.setOnItemSelectedListener { item ->
            // Prevent reloading the same screen
            if (item.itemId == bottomNavigationView.selectedItemId) {
                return@setOnItemSelectedListener false
            }

            val intent = when (item.itemId) {
                R.id.nav_dashboard -> Intent(this, DashboardActivity::class.java)
                R.id.nav_add_loan -> Intent(this, AddLoanActivity::class.java)
                R.id.nav_history -> Intent(this, HistoryActivity::class.java)
                else -> null
            }

            intent?.let {
                it.putExtra("USER_NAME", userName)
                startActivity(it)
            }

            // Return true for any handled navigation item
            return@setOnItemSelectedListener (item.itemId != R.id.nav_profile)
        }
    }

    private fun setupFirebaseListeners() {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val gcashBalance = snapshot.child("gcashBalance").getValue(Double::class.java) ?: 0.0
                val onHandBalance = snapshot.child("onHandBalance").getValue(Double::class.java) ?: 0.0
                tvProfileName.text = name ?: userName
                tvProfileEmail.text = email ?: "No email provided"
                tvGcashBalance.text = formatCurrency(gcashBalance)
                tvOnHandBalance.text = formatCurrency(onHandBalance)
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load profile data: ${error.message}")
            }
        })

        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalLoans = snapshot.childrenCount.toInt()
                var activeLoansCount = 0
                for (loanSnapshot in snapshot.children) {
                    val paid = loanSnapshot.child("paid").getValue(Boolean::class.java) ?: false
                    if (!paid) {
                        activeLoansCount++
                    }
                }
                tvTotalLoans.text = totalLoans.toString()
                tvActiveLoans.text = activeLoansCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load loan statistics: ${error.message}")
            }
        })
    }

    private fun setupClickListeners() {
        btnAddGcash.setOnClickListener { showUpdateBalanceDialog("GCash", BALANCE_GCASH) }
        btnAddOnHand.setOnClickListener { showUpdateBalanceDialog("On-Hand", BALANCE_ON_HAND) }
        findViewById<android.widget.RelativeLayout>(R.id.actionSettings).setOnClickListener { showToast("Settings feature coming soon!") }
        findViewById<android.widget.RelativeLayout>(R.id.actionHelp).setOnClickListener { showHelpDialog() }
        findViewById<android.widget.RelativeLayout>(R.id.actionLogout).setOnClickListener { showLogoutConfirmationDialog() }
    }

    private fun showUpdateBalanceDialog(balanceType: String, balanceKey: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add to $balanceType Balance")
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "0.00"
        }
        builder.setView(input)
        builder.setPositiveButton("Add") { _, _ ->
            val amountStr = input.text.toString().trim()
            val amount = sanitizeAmountInput(amountStr)
            if (amount == null || amount <= 0 || amount > MAX_DEPOSIT_AMOUNT) {
                showToast("Please enter a valid positive amount.")
                return@setPositiveButton
            }
            updateBalance(amount, balanceKey, balanceType)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun sanitizeAmountInput(input: String): Double? {
        return try {
            input.toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun formatCurrency(amount: Double): String {
        return String.format("₱%,.2f", amount)
    }

    private fun updateBalance(amount: Double, balanceKey: String, balanceType: String) {
        // This function seems to have a bug in the original code, using .get() is not ideal.
        // A transaction is safer for concurrent updates.
        userRef.child(balanceKey).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): com.google.firebase.database.Transaction.Result {
                val currentBalance = currentData.getValue(Double::class.java) ?: 0.0
                currentData.value = currentBalance + amount
                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                finalData: DataSnapshot?
            ) {
                if (committed) {
                    showToast("${formatCurrency(amount)} added to $balanceType balance.")
                    recordTransaction(amount, balanceType)
                } else {
                    showToast("Failed to update $balanceType balance.")
                }
            }
        })
    }

    private fun recordTransaction(amount: Double, type: String) {
        val transactionRef = userRef.child("transactions").push()
        val transaction = mapOf(
            "amount" to amount,
            "type" to type,
            "transactionType" to "deposit",
            "timestamp" to System.currentTimeMillis()
        )
        transactionRef.setValue(transaction)
    }

    private fun showEditProfileDialog() {
        showToast("Profile update feature coming soon!")
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage("For assistance, email support@gogetcash.com.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
