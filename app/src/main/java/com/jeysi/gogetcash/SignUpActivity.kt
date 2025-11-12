package com.jeysi.gogetcash

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etReEnterPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var tvLogin: TextView

    data class User(
        val name: String? = null, // Store the name as a field, not the key
        val email: String? = null, // Assuming the "username" field is actually for the username
        val password: String? = null,
        val gcashBalance: Double = 0.0,
        val onHandBalance: Double = 0.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_design_activity)

        etName = findViewById(R.id.etName)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etReEnterPassword = findViewById(R.id.etReEnterPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvLogin = findViewById(R.id.tvLogin)

        btnSignUp.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser() {
        val name = etName.text.toString().trim()
        val username = etUsername.text.toString().trim() // This will be the unique key
        val password = etPassword.text.toString().trim()
        val reEnteredPassword = etReEnterPassword.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showToast("Please fill in all required fields")
            return
        }

        if (password != reEnteredPassword) {
            showToast("Passwords do not match")
            return
        }

        val database = FirebaseDatabase.getInstance().getReference("users")

        // [THE FIX] The User object now holds the name and we use username as the key
        val user = User(name = name, password = password, email = username) // Assuming etUsername is for username

        // Use the 'username' as the unique key
        database.child(username).setValue(user)
            .addOnSuccessListener {
                showToast("Registration successful!")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                showToast("Registration failed: ${it.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
