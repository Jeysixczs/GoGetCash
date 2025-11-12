package com.jeysi.gogetcash

import android.content.Intent
import androidx.compose.ui.layout.layout
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


data class CashFlowTransaction(
    var name: String = "",
    var phoneNumber: String = "",
    var amount: Double = 0.0,
    var fee: Double = 0.0,
    var timestamp: Long = 0
) {
    // Required empty constructor for Firebase
    constructor() : this("", "", 0.0, 0.0, 0)
}


class CashFlowActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etAmount: EditText
    private lateinit var etFee: EditText
    private lateinit var rgTransactionType: RadioGroup
    private lateinit var btnSaveTransaction: Button

    private lateinit var userRef: DatabaseReference
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
      //  setContentView(R.layout.cashflow_activity)

        userName = intent.getStringExtra("USER_NAME")
        if (userName.isNullOrEmpty()) {
            Toast.makeText(this, "User not identified. Returning to dashboard.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!)

        initializeViews()

        btnSaveTransaction.setOnClickListener {
            saveTransaction()
        }
    }

    private fun initializeViews() {
        etName = findViewById(R.id.etName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etAmount = findViewById(R.id.etAmount)
        etFee = findViewById(R.id.etFee)
       // rgTransactionType = findViewById(R.id.rgTransactionType)
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction)
    }

    private fun saveTransaction() {
        val name = etName.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val feeStr = etFee.text.toString().trim()

        if (name.isEmpty() || amountStr.isEmpty() || feeStr.isEmpty()) {
            showToast("Please fill in Name, Amount, and Fee.")
            return
        }

        val amount = amountStr.toDoubleOrNull()
        val fee = feeStr.toDoubleOrNull()

        if (amount == null || fee == null || amount <= 0) {
            showToast("Please enter valid numbers for Amount and Fee.")
            return
        }

        val timestamp = System.currentTimeMillis()
        val transaction = CashFlowTransaction(name, phoneNumber, amount, fee, timestamp)

        val selectedTypeId = rgTransactionType.checkedRadioButtonId
    //    val transactionTypePath = if (selectedTypeId == R.id.rbCashIn) "Cashin" else "Cashout"

//        val transactionRef = userRef.child(transactionTypePath).push()
//        transactionRef.setValue(transaction)
//            .addOnSuccessListener {
//                showToast("$transactionTypePath transaction saved successfully!")
//                // Navigate back to the dashboard after saving
//                val intent = Intent(this, DashboardActivity::class.java).apply {
//                    putExtra("USER_NAME", userName)
//                    // Clear the activity stack and start a new task for the dashboard
//                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//                }
//                startActivity(intent)
//                finish()
//            }
//            .addOnFailureListener { e ->
//                showToast("Failed to save transaction: ${e.message}")
//            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
