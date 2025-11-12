package com.jeysi.gogetcash

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddLoanActivity : AppCompatActivity() {

    // --- UI Views ---
    private lateinit var etName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etAmount: EditText
    private lateinit var etFee: EditText
    private lateinit var etStartDate: EditText
    private lateinit var etDueDate: EditText
    private lateinit var rgFundSource: RadioGroup
    private lateinit var btnSaveLoan: MaterialButton
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var tilPhoneNumber: TextInputLayout


    // --- Firebase ---
    private lateinit var userRef: DatabaseReference
    private lateinit var historyRef: DatabaseReference
    private var userName: String? = null

    // --- Date Pickers ---
    private val startCalendar: Calendar = Calendar.getInstance()
    private val dueCalendar: Calendar = Calendar.getInstance()

    // Contact Picker Launcher
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { contactUri ->
                getContactInfo(contactUri)
            }
        }
    }

    // Permission Request Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openContactPicker()
        } else {
            showToast("Contact permission is required to pick contacts")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addloaner_design_activity)

        userName = intent.getStringExtra("USER_NAME")
        if (userName.isNullOrEmpty()) {
            showToast("Error: User not identified. Cannot add loan.")
            finish()
            return
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(userName!!)
        historyRef = userRef.child("history")

        initializeViews()
        setupClickListeners()
        setupBottomNavigation()

    }

    override fun onResume() {
        super.onResume()
        bottomNavigationView.menu.findItem(R.id.nav_add_loan).isChecked = true
    }

    override fun onPause() {
        super.onPause()

    }

    private fun initializeViews() {
        etName = findViewById(R.id.etName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etAmount = findViewById(R.id.etAmount)
        etFee = findViewById(R.id.etFee)
        etStartDate = findViewById(R.id.etStartDate)
        etDueDate = findViewById(R.id.etDueDate)
        rgFundSource = findViewById(R.id.rgFundSource)
        btnSaveLoan = findViewById(R.id.btnSaveLoan)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        tilPhoneNumber = findViewById(R.id.tilPhoneNumber)

        updateDateInView(isStartDate = true)
        updateDateInView(isStartDate = false)
    }

    private fun setupClickListeners() {
        etStartDate.setOnClickListener { showDatePickerDialog(isStartDate = true) }
        etDueDate.setOnClickListener { showDatePickerDialog(isStartDate = false) }
        btnSaveLoan.setOnClickListener { validateAndSaveLoan() }
        tilPhoneNumber.setEndIconOnClickListener {
            checkContactPermission()
        }

    }

    private fun setupBottomNavigation() {
        bottomNavigationView.menu.findItem(R.id.nav_add_loan).isChecked = true

        bottomNavigationView.setOnItemSelectedListener { item ->
            // Prevent reloading the same screen
            if (item.itemId == bottomNavigationView.selectedItemId) {
                return@setOnItemSelectedListener false
            }

            val intent = when (item.itemId) {
                R.id.nav_dashboard -> Intent(this, DashboardActivity::class.java)
                R.id.nav_history -> Intent(this, HistoryActivity::class.java)
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                else -> null
            }

            intent?.let {
                it.putExtra("USER_NAME", userName)
                startActivity(it)

            }

            return@setOnItemSelectedListener (item.itemId != R.id.nav_add_loan)
        }
    }

    private fun checkContactPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                openContactPicker()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.READ_CONTACTS
            ) -> {
                showToast("Contact permission is needed to select contacts")
                requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }

    private fun getContactInfo(contactUri: android.net.Uri) {
        val contentResolver = contentResolver
        var contactName = ""
        var phoneNumber = ""

        // Query for the contact's name
        contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIndex != -1) contactName = cursor.getString(nameIndex)
            }
        }

        // Query for the contact's phone number
        val idCursor = contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts._ID), null, null, null)
        if (idCursor?.moveToFirst() == true) {
            val contactId = idCursor.getString(idCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )
            if (phoneCursor?.moveToFirst() == true) {
                phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            }
            phoneCursor?.close()
        }
        idCursor?.close()

        // Set the text fields
        etName.setText(contactName)
        etPhoneNumber.setText(phoneNumber.replace("\\s".toRegex(), ""))
        showToast("Selected: $contactName")
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = if (isStartDate) startCalendar else dueCalendar
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView(isStartDate)
        }
        DatePickerDialog(
            this, dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateInView(isStartDate: Boolean) {
        val myFormat = "MMM dd, yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        if (isStartDate) {
            etStartDate.setText(sdf.format(startCalendar.time))
        } else {
            etDueDate.setText(sdf.format(dueCalendar.time))
        }
    }

    private fun validateAndSaveLoan() {
        val name = etName.text.toString().trim()
        val phoneNumber = etPhoneNumber.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val feeStr = etFee.text.toString().trim()

        if (name.isEmpty() || phoneNumber.isEmpty() || amountStr.isEmpty()) {
            showToast("Please fill in Name, Phone, and Amount.")
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            showToast("Please enter a valid positive amount.")
            return
        }
        if (dueCalendar.timeInMillis < startCalendar.timeInMillis) {
            showToast("Due date cannot be before the start date.")
            return
        }
        val selectedFundSourceId = rgFundSource.checkedRadioButtonId
        if (selectedFundSourceId == -1) {
            showToast("Please select a fund source.")
            return
        }

        checkBalanceAndProceed(name, phoneNumber, amount, feeStr)
    }

    private fun checkBalanceAndProceed(name: String, phoneNumber: String, amount: Double, feeStr: String) {
        val fee = feeStr.toDoubleOrNull() ?: 0.0
        val isFromGcash = rgFundSource.checkedRadioButtonId == R.id.rbGcash
        val balanceKey = if (isFromGcash) "gcashBalance" else "onHandBalance"
        val balanceName = if (isFromGcash) "GCash" else "On-Hand"

        userRef.child(balanceKey).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0
                if (currentBalance < amount) {
                    showToast("Insufficient $balanceName balance.")
                    return
                }

                val newBalance = currentBalance - amount
                processLoanTransaction(name, phoneNumber, amount, fee, newBalance, balanceKey, balanceName)
            }
            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to check balance: ${error.message}")
            }
        })
    }

    private fun processLoanTransaction(name: String, phoneNumber: String, amount: Double, fee: Double, newBalance: Double, balanceKey: String, balanceName: String) {
        val loanId = historyRef.push().key
        if (loanId == null) {
            showToast("Could not generate loan ID.")
            return
        }

        val newLoan = Transaction(
            loanerName = name,
            phoneNumber = phoneNumber,
            amount = amount,
            fee = fee,
            startDate = startCalendar.timeInMillis,
            dueDate = dueCalendar.timeInMillis,
            paid = false,
            description = "Loan from $balanceName",
            timestamp = System.currentTimeMillis()
        )

        // Use a map for atomic updates
        val childUpdates = hashMapOf<String, Any>(
            "/history/$loanId" to newLoan,
            "/$balanceKey" to newBalance
        )

        userRef.updateChildren(childUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast("Loan saved successfully!")
                // Navigate to dashboard
                val intent = Intent(this, DashboardActivity::class.java).apply {
                    putExtra("USER_NAME", userName)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            } else {
                showToast("Failed to save loan: ${task.exception?.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
