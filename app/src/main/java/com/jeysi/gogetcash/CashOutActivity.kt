package com.jeysi.gogetcash

import android.Manifest
import android.R.color.white
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CashOutActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etAmount: EditText
    private lateinit var etFee: EditText
    private lateinit var btnSaveTransaction: Button
    private lateinit var btnSelectContact: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var loadingProgressBar: ProgressBar

    private lateinit var userRef: DatabaseReference
    private var userName: String? = null

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                populateFieldsFromContact(contactUri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchContactPicker()
        } else {
            showToast("Contact permission is required to select a contact.")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cash_out)

        initializeViews()

        userName = intent.getStringExtra("USER_NAME")
        if (userName.isNullOrEmpty()) {
            showToast("User not identified. Returning to dashboard.")
            finish()
            return
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!)

        btnBack.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java).apply {
                putExtra("USER_NAME", userName)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        btnSelectContact.setOnClickListener {
            checkPermissionAndLaunchContactPicker()
        }

        btnSaveTransaction.setOnClickListener {
            saveTransaction()
        }
    }

    private fun initializeViews() {
        etName = findViewById(R.id.etName)
        etAmount = findViewById(R.id.etAmount)
        etFee = findViewById(R.id.etFee)
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction)
        btnSelectContact = findViewById(R.id.btnSelectContact)
        btnBack = findViewById(R.id.btnBack)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
    }

    private fun checkPermissionAndLaunchContactPicker() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchContactPicker()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                showToast("We need permission to access your contacts to make selection easier.")
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }

    private fun populateFieldsFromContact(contactUri: Uri) {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                etName.setText(cursor.getString(nameIndex))


            }
        }
    }

    private fun saveTransaction() {
        val name = etName.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val feeStr = etFee.text.toString().trim()

        if (name.isEmpty() || amountStr.isEmpty()) {
            showToast("Please fill in at least Name and Amount.")
            return
        }

        val amount = amountStr.toDoubleOrNull()
        val fee = feeStr.toDoubleOrNull() ?: 0.0

        if (amount == null || amount <= 0) {
            showToast("Please enter a valid positive number for Amount.")
            return
        }

        setLoadingState(true)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentGcashBalance = snapshot.child("gcashBalance").getValue(Double::class.java) ?: 0.0
                val currentOnHandBalance = snapshot.child("onHandBalance").getValue(Double::class.java) ?: 0.0

                if (currentOnHandBalance < amount) {
                    showToast("Insufficient On-Hand balance for this transaction.")
                    setLoadingState(false)
                    return
                }

                val newGcashBalance = currentGcashBalance + amount
                val newOnHandBalance = currentOnHandBalance - amount
                val timestamp = System.currentTimeMillis()
                val transaction = CashFlowTransaction(name, "",amount, fee, timestamp)

                val newTransactionKey = userRef.child("Cashout").push().key
                if (newTransactionKey == null) {
                    showToast("Failed to generate transaction key.")
                    setLoadingState(false)
                    return
                }

                val childUpdates = mutableMapOf<String, Any>(
                    "/Cashout/$newTransactionKey" to transaction,
                    "/gcashBalance" to newGcashBalance,
                    "/onHandBalance" to newOnHandBalance
                )

                userRef.updateChildren(childUpdates)
                    .addOnSuccessListener {
                        showToast("Cash Out successful!")
                        val intent = Intent(this@CashOutActivity, DashboardActivity::class.java).apply {
                            putExtra("USER_NAME", userName)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        setLoadingState(false)
                        showToast("Failed to save transaction: ${e.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                setLoadingState(false)
                showToast("Failed to read current balance: ${error.message}")
            }
        })
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            loadingProgressBar.visibility = View.VISIBLE
            btnSaveTransaction.isEnabled = false
            btnSaveTransaction.text = "Saving..."
            btnSaveTransaction.setTextColor(ContextCompat.getColor(this, white))
        } else {
            loadingProgressBar.visibility = View.GONE
            btnSaveTransaction.isEnabled = true
            btnSaveTransaction.text = "Save Cash Out"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
