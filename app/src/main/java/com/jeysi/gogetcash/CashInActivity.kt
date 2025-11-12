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

class CashInActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etAmount: EditText
    private lateinit var etFee: EditText
    private lateinit var btnSaveTransaction: Button
    private lateinit var btnSelectContact: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var loadingProgressBar: ProgressBar // New ProgressBar

    private lateinit var userRef: DatabaseReference
    private var userName: String? = null

    // --- ActivityResultLaunchers remain the same ---
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
        setContentView(R.layout.activity_cash_in)


        initializeViews()

        userName = intent.getStringExtra("USER_NAME")
        if (userName.isNullOrEmpty()) {
            showToast("User not identified. Returning to dashboard.")
            finish()
            return
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java).apply {
                putExtra("USER_NAME", userName)

            }
            startActivity(intent)
            finish()
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!)

        btnSelectContact.setOnClickListener {
            checkPermissionAndLaunchContactPicker()
        }

        btnSaveTransaction.setOnClickListener {
            saveTransaction()
        }
    }



    private fun initializeViews() {
        etName = findViewById(R.id.etName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etAmount = findViewById(R.id.etAmount)
        etFee = findViewById(R.id.etFee)
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction)
        btnSelectContact = findViewById(R.id.btnSelectContact)
        btnBack = findViewById(R.id.btnBack)
        loadingProgressBar = findViewById(R.id.loadingProgressBar) // Initialize ProgressBar
    }

    private fun checkPermissionAndLaunchContactPicker() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
                launchContactPicker()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                // Explain to the user why the permission is needed (optional)
                showToast("We need permission to access your contacts to make selection easier.")
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
            else -> {
                // Directly request the permission
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }

    private fun populateFieldsFromContact(contactUri: Uri) {
        // Define the columns you want to retrieve from the initial query
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        )

        contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // Get Contact Name
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val contactName = cursor.getString(nameIndex)
                etName.setText(contactName)

                // Check if the contact has a phone number
                val hasPhoneNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val hasPhoneNumber = cursor.getInt(hasPhoneNumberIndex) > 0

                if (hasPhoneNumber) {
                    // Get the Contact ID
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val contactId = cursor.getString(idIndex)

                    // Query for the phone number using the Contact ID
                    contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, // We can query all columns here
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(contactId),
                        null
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) {
                            // Get the phone number
                            val phoneIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val phoneNumber = phoneCursor.getString(phoneIndex)
                            etPhoneNumber.setText(phoneNumber)
                        } else {
                            // This case is unlikely if HAS_PHONE_NUMBER is true, but good for safety
                            etPhoneNumber.setText("")
                            showToast("Could not find a phone number for this contact.")
                        }
                    }
                } else {
                    // The contact does not have a phone number saved
                    etPhoneNumber.setText("")
                    showToast("No phone number found for this contact.")
                }
            }
        }
    }

    // --- UPDATED saveTransaction function ---
    private fun saveTransaction() {
        val name = etName.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val feeStr = etFee.text.toString().trim()

        if (name.isEmpty() || phoneNumber.isEmpty() || amountStr.isEmpty() || feeStr.isEmpty()) {
            showToast("Please fill all fields.")
            return
        }

        val amount = amountStr.toDoubleOrNull()
        val fee = feeStr.toDoubleOrNull()

        if (amount == null || fee == null || amount <= 0) {
            showToast("Please enter valid numbers for Amount and Fee.")
            return
        }

        // Show loading indicator and disable button
        setLoadingState(true)

        // Fetch current balances before proceeding
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentGcashBalance = snapshot.child("gcashBalance").getValue(Double::class.java) ?: 0.0
                val currentOnHandBalance = snapshot.child("onHandBalance").getValue(Double::class.java) ?: 0.0

                if (currentGcashBalance < amount) {
                    showToast("Insufficient GCash balance for this transaction.")
                    setLoadingState(false) // Hide loading
                    return
                }

                // Calculate new balances
                val newGcashBalance = currentGcashBalance - amount
                val newOnHandBalance = currentOnHandBalance + amount

                // Create the transaction object
                val timestamp = System.currentTimeMillis()
                val transaction = CashFlowTransaction(name, phoneNumber, amount, fee, timestamp)

                // Get a new unique key for the transaction
                val newTransactionKey = userRef.child("Cashin").push().key
                if (newTransactionKey == null) {
                    showToast("Failed to generate transaction key.")
                    setLoadingState(false) // Hide loading
                    return
                }

                // Create a map to perform an atomic update
                val childUpdates = mutableMapOf<String, Any>(
                    "/Cashin/$newTransactionKey" to transaction,
                    "/gcashBalance" to newGcashBalance,
                    "/onHandBalance" to newOnHandBalance
                )

                // Perform the atomic update
                userRef.updateChildren(childUpdates)
                    .addOnSuccessListener {
                        showToast("Cash In successful!")
                        // Navigate back to the dashboard
                        val intent = Intent(this@CashInActivity, DashboardActivity::class.java).apply {
                            putExtra("USER_NAME", userName)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        setLoadingState(false) // Hide loading on failure
                        showToast("Failed to save transaction: ${e.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                setLoadingState(false) // Hide loading on cancellation
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
            btnSaveTransaction.text = "Save Cash In"
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
