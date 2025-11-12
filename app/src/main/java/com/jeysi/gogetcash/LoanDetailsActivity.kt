package com.jeysi.gogetcash

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class LoanDetailsActivity : AppCompatActivity() {

    // --- UI Views ---
    private lateinit var btnBack: ImageButton
    private lateinit var tvLoanStatus: TextView
    private lateinit var etName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etAmount: EditText
    private lateinit var etFee: EditText
    private lateinit var etStartDate: EditText
    private lateinit var etDueDate: EditText
    private lateinit var btnUpdateLoan: MaterialButton
    private lateinit var btnDeleteLoan: MaterialButton
    private lateinit var btnAddMoreLoan: MaterialButton
    private lateinit var btnMarkAsPaid: MaterialButton
    private lateinit var btnReduceLoan: MaterialButton
    private lateinit var llActionButtons: View

    // --- Firebase ---
    private lateinit var userRef: DatabaseReference
    private lateinit var loanRef: DatabaseReference
    private var userName: String? = null
    private var loanId: String? = null
    private var originalLoan: Transaction? = null

    private val startCalendar = Calendar.getInstance()
    private val dueCalendar = Calendar.getInstance()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loan_details_activity)

        userName = intent.getStringExtra("USER_NAME")
        loanId = intent.getStringExtra("LOAN_ID")

        if (userName.isNullOrEmpty() || loanId.isNullOrEmpty()) {
            showToast("Error: Missing user or loan information.")
            finish()
            return
        }

        initializeViews()
        setupFirebaseRefs()
        loadLoanData()
        setupClickListeners()
    }

    private fun initializeViews() {
        // Correctly initialize all views from the XML
        btnBack = findViewById(R.id.btnBack)
        tvLoanStatus = findViewById(R.id.tvLoanStatus)
        etName = findViewById(R.id.etName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etAmount = findViewById(R.id.etAmount)
        etFee = findViewById(R.id.etFee)
        etStartDate = findViewById(R.id.etStartDate)
        etDueDate = findViewById(R.id.etDueDate)
        btnUpdateLoan = findViewById(R.id.btnUpdateLoan)
        btnDeleteLoan = findViewById(R.id.btnDeleteLoan)
        btnAddMoreLoan = findViewById(R.id.btnAddMoreLoan)
        btnMarkAsPaid = findViewById(R.id.btnMarkAsPaid)
        btnReduceLoan = findViewById(R.id.btnReduceLoan) // Initialize new button
        llActionButtons = findViewById(R.id.llActionButtons) // Initialize the container
    }

    private fun setupFirebaseRefs() {
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!)
        loanRef = userRef.child("history").child(loanId!!)
    }

    private fun setupClickListeners() {
        // The back button is now btnBack
        btnBack.setOnClickListener { finish() }

        // Only DueDate can be changed
        etDueDate.setOnClickListener {
            if (originalLoan?.paid != true) {
                showDatePickerDialog(isStartDate = false)
            }
        }

        btnUpdateLoan.setOnClickListener { performUpdate() }
        btnDeleteLoan.setOnClickListener { confirmAndDelete() }
        btnAddMoreLoan.setOnClickListener { showAddMoreLoanDialog() }
        btnMarkAsPaid.setOnClickListener { confirmMarkAsPaid() }
        btnReduceLoan.setOnClickListener { showReduceLoanDialog() } // Set listener for new button
    }

    private fun loadLoanData() {
        loanRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                originalLoan = snapshot.getValue(Transaction::class.java)
                originalLoan?.let { populateUi(it) } ?: run {
                    if (!isFinishing) {
                        showToast("Loan data no longer exists.")
                        finish()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load loan data: ${error.message}")
            }
        })
    }

    private fun populateUi(loan: Transaction) {
        etName.setText(loan.loanerName)
        etPhoneNumber.setText(loan.phoneNumber)
        etAmount.setText(String.format("%.2f", loan.amount))
        etFee.setText(String.format("%.2f", loan.fee))
        startCalendar.timeInMillis = loan.startDate
        dueCalendar.timeInMillis = loan.dueDate
        updateDateInView(isStartDate = true)
        updateDateInView(isStartDate = false)

        // Updated logic to manage UI state based on paid status
        if (!loan.paid) {
            tvLoanStatus.text = "Unpaid"
            tvLoanStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            llActionButtons.visibility = View.VISIBLE // Show the entire button container
            setFieldsEnabled(true) // Enable editing for allowed fields
        } else {
            tvLoanStatus.text = "Paid"
            tvLoanStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            llActionButtons.visibility = View.GONE // Hide the entire button container
            setFieldsEnabled(false) // Disable editing completely
        }
    }

    // Helper function to toggle field editability
    private fun setFieldsEnabled(isEnabled: Boolean) {
        // Only Due Date and Fee are editable when isEnabled is true
        etDueDate.isEnabled = isEnabled
        etFee.isEnabled = isEnabled

        // These fields are ALWAYS non-editable
        etName.isEnabled = false
        etPhoneNumber.isEnabled = false
        etAmount.isEnabled = false
        etStartDate.isEnabled = false
        etFee.isEnabled = false
        // For date fields, prevent keyboard but allow clicks
        etStartDate.showSoftInputOnFocus = false
        etDueDate.showSoftInputOnFocus = false

        if (isEnabled) {
            // Set enabled fields to black
            etFee.setTextColor(resources.getColor(android.R.color.black, null))
            etDueDate.setTextColor(resources.getColor(android.R.color.black, null))

            // Keep disabled fields gray
            etName.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etPhoneNumber.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etAmount.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etStartDate.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etFee.setTextColor(resources.getColor(android.R.color.darker_gray, null))

        } else {
            // Make all text slightly faded to indicate read-only
            etName.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etPhoneNumber.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etAmount.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etFee.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etStartDate.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etDueDate.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            etFee.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }
    }

    // Helper function for date formatting
    private fun updateDateInView(isStartDate: Boolean) {
        val myFormat = "MM/dd/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        if (isStartDate) {
            etStartDate.setText(sdf.format(startCalendar.time))
        } else {
            etDueDate.setText(sdf.format(dueCalendar.time))
        }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        // Double check that loan is not paid before showing dialog
        if (originalLoan?.paid == true) {
            showToast("Cannot edit paid loans.")
            return
        }

        val calendar = if (isStartDate) startCalendar else dueCalendar
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView(isStartDate)
        }

        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }


    private fun confirmMarkAsPaid() {
        val loanToPay = originalLoan ?: return
        val principalAmount = loanToPay.amount
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_mark_as_paid, null)
        val rgReturnFundSource = dialogView.findViewById<RadioGroup>(R.id.rgReturnFundSource)

        AlertDialog.Builder(this)
            .setTitle("Mark Loan as Paid")
            .setMessage("This will return ₱${"%.2f".format(principalAmount)} to the selected balance and update the loan status to 'Paid'. Are you sure?")
            .setView(dialogView)
            .setPositiveButton("Yes, Mark as Paid") { _, _ ->
                val selectedFundId = rgReturnFundSource.checkedRadioButtonId
                if (selectedFundId == -1) {
                    showToast("Please select a destination for the funds.")
                    return@setPositiveButton
                }
                val returnBalanceKey = if (selectedFundId == R.id.rbReturnGcash) "gcashBalance" else "onHandBalance"
                performMarkAsPaid(returnBalanceKey)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performMarkAsPaid(returnBalanceKey: String) {
        val loanToPay = originalLoan ?: return
        val amountToReturn = loanToPay.amount

        userRef.child(returnBalanceKey).get().addOnSuccessListener { balanceSnapshot ->
            val currentBalance = balanceSnapshot.getValue(Double::class.java) ?: 0.0
            val newBalance = currentBalance + amountToReturn

            val childUpdates = mapOf<String, Any?>(
                "/history/$loanId/paid" to true,
                "/$returnBalanceKey" to newBalance
            )

            userRef.updateChildren(childUpdates).addOnSuccessListener {
                showToast("Loan marked as paid.")
                finish()
            }.addOnFailureListener { e ->
                showToast("Failed to mark as paid: ${e.message}")
            }
        }.addOnFailureListener { e ->
            showToast("Could not read balance: ${e.message}")
        }
    }

    private fun showAddMoreLoanDialog() {
        // Check if loan is already paid
        if (originalLoan?.paid == true) {
            showToast("Cannot add to a paid loan.")
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_loan, null)
        val etNewAmount = dialogView.findViewById<EditText>(R.id.etNewAmount)
        val etNewFee = dialogView.findViewById<EditText>(R.id.etNewFee)
        val rgFundSource = dialogView.findViewById<RadioGroup>(R.id.rgFundSource)

        AlertDialog.Builder(this)
            .setTitle("Add to Loan for ${originalLoan?.loanerName}")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val newAmount = etNewAmount.text.toString().toDoubleOrNull()
                val newFee = etNewFee.text.toString().toDoubleOrNull() ?: 0.0

                if (newAmount == null || newAmount <= 0) {
                    showToast("Please enter a valid amount.")
                    return@setPositiveButton
                }
                val selectedFundSourceId = rgFundSource.checkedRadioButtonId
                if (selectedFundSourceId == -1) {
                    showToast("Please select a fund source.")
                    return@setPositiveButton
                }
                val balanceKey = if (selectedFundSourceId == R.id.rbGcash) "gcashBalance" else "onHandBalance"
                processAddToLoan(newAmount, newFee, balanceKey)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processAddToLoan(newAmount: Double, newFee: Double, balanceKey: String) {
        userRef.child(balanceKey).get().addOnSuccessListener { balanceSnapshot ->
            val currentSourceBalance = balanceSnapshot.getValue(Double::class.java) ?: 0.0
            if (currentSourceBalance < newAmount) {
                val balanceName = if (balanceKey == "gcashBalance") "GCash" else "On-Hand"
                showToast("Insufficient $balanceName balance.")
                return@addOnSuccessListener
            }

            val newSourceBalance = currentSourceBalance - newAmount
            val originalLoanAmount = originalLoan?.amount ?: 0.0
            val originalFee = originalLoan?.fee ?: 0.0

            val newTotalLoanAmount = originalLoanAmount + newAmount
            val newTotalFee = originalFee + newFee

            val childUpdates = mapOf<String, Any?>(
                "/history/$loanId/amount" to newTotalLoanAmount,
                "/history/$loanId/fee" to newTotalFee,
                "/$balanceKey" to newSourceBalance
            )

            userRef.updateChildren(childUpdates).addOnSuccessListener {
                showToast("Successfully added to the loan.")
            }.addOnFailureListener {
                showToast("Failed to add funds: ${it.message}")
            }
        }.addOnFailureListener {
            showToast("Could not read balance: ${it.message}")
        }
    }

    // --- NEW: Reduce Loan Functions ---
    private fun showReduceLoanDialog() {
        if (originalLoan?.paid == true) {
            showToast("Cannot reduce a paid loan.")
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reduce_loan, null)
        val etReduceAmount = dialogView.findViewById<EditText>(R.id.etReduceAmount)
        val etReduceFee = dialogView.findViewById<EditText>(R.id.etReduceFee)
        val rgReturnFundSource = dialogView.findViewById<RadioGroup>(R.id.rgReturnFundSource)

        AlertDialog.Builder(this)
            .setTitle("Reduce Loan for ${originalLoan?.loanerName}")
            .setView(dialogView)
            .setPositiveButton("Reduce") { _, _ ->
                val reduceAmount = etReduceAmount.text.toString().toDoubleOrNull()
                val reduceFee = etReduceFee.text.toString().toDoubleOrNull() ?: 0.0

                if (reduceAmount == null || reduceAmount <= 0) {
                    showToast("Please enter a valid amount to reduce.")
                    return@setPositiveButton
                }
                val selectedFundId = rgReturnFundSource.checkedRadioButtonId
                if (selectedFundId == -1) {
                    showToast("Please select where to return the funds.")
                    return@setPositiveButton
                }
                val returnBalanceKey = if (selectedFundId == R.id.rbReturnGcash) "gcashBalance" else "onHandBalance"
                processReduceLoan(reduceAmount, reduceFee, returnBalanceKey)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processReduceLoan(reduceAmount: Double, reduceFee: Double, returnBalanceKey: String) {
        val originalLoanAmount = originalLoan?.amount ?: 0.0
        val originalFee = originalLoan?.fee ?: 0.0

        if (reduceAmount > originalLoanAmount) {
            showToast("Cannot reduce more than the current loan amount (₱${"%.2f".format(originalLoanAmount)}).")
            return
        }
        if (reduceFee > originalFee) {
            showToast("Cannot reduce more than the current fee (₱${"%.2f".format(originalFee)}).")
            return
        }

        userRef.child(returnBalanceKey).get().addOnSuccessListener { balanceSnapshot ->
            val currentReturnBalance = balanceSnapshot.getValue(Double::class.java) ?: 0.0
            val newReturnBalance = currentReturnBalance + reduceAmount

            val newTotalLoanAmount = originalLoanAmount - reduceAmount
            val newTotalFee = originalFee - reduceFee

            val childUpdates = mapOf<String, Any?>(
                "/history/$loanId/amount" to newTotalLoanAmount,
                "/history/$loanId/fee" to newTotalFee,
                "/$returnBalanceKey" to newReturnBalance
            )

            userRef.updateChildren(childUpdates).addOnSuccessListener {
                showToast("Successfully reduced the loan.")
            }.addOnFailureListener {
                showToast("Failed to reduce loan: ${it.message}")
            }
        }.addOnFailureListener {
            showToast("Could not read balance: ${it.message}")
        }
    }


    private fun performUpdate() {
        // Check if loan is paid before allowing update
        if (originalLoan?.paid == true) {
            showToast("Cannot update a paid loan.")
            return
        }

        // Only Fee and DueDate are updatable.
        val updatedData = mapOf(
            "fee" to (etFee.text.toString().toDoubleOrNull() ?: 0.0),
            "dueDate" to dueCalendar.timeInMillis
        )

        loanRef.updateChildren(updatedData).addOnSuccessListener {
            showToast("Loan details updated successfully!")
            // Do not finish, so user can see the change
        }.addOnFailureListener {
            showToast("Failed to update loan: ${it.message}")
        }
    }

    private fun confirmAndDelete() {
        // Check if loan is paid before allowing deletion
        if (originalLoan?.paid == true) {
            showToast("Cannot delete a paid loan.")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Loan")
            .setMessage("Are you sure you want to permanently delete this loan? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteLoan() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteLoan() {
        loanRef.removeValue().addOnSuccessListener {
            showToast("Loan deleted successfully.")
            finish()
        }.addOnFailureListener {
            showToast("Failed to delete loan: ${it.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
