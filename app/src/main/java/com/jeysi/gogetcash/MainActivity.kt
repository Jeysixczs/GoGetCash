package com.jeysi.gogetcash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvSignUp: TextView
    private lateinit var ivPasswordToggle: ImageButton

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_design_activity)

        // Initialize views
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle)

        // Load saved credentials if "Remember Me" was checked
        loadCredentials()

        // Setup click listeners
        btnLogin.setOnClickListener {
            loginUserWithFirebaseDatabase()
        }

        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        ivPasswordToggle.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            // Show password
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_visibilitys)
        } else {
            // Hide password
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            ivPasswordToggle.setImageResource(R.drawable.ic_eye_visibility_off)
        }
        // Move cursor to the end of the text
        etPassword.setSelection(etPassword.length())
    }

    private fun loginUserWithFirebaseDatabase() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            showToast("Please enter both username and password")
            return
        }

        val database = FirebaseDatabase.getInstance().getReference("users")
        database.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val storedPassword = snapshot.child("password").getValue(String::class.java)

                    if (storedPassword == password) {
                        showToast("Login Successful!")

                        // Handle "Remember Me"
                        if (cbRememberMe.isChecked) {
                            saveCredentials(username, password)
                        } else {
                            clearCredentials()
                        }

                        val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                        intent.putExtra("USER_NAME", snapshot.key)
                        startActivity(intent)
                        finish()
                    } else {
                        showToast("Login Failed: Incorrect password")
                    }
                } else {
                    showToast("Login Failed: User does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Database Error: ${error.message}")
            }
        })
    }

    private fun saveCredentials(username: String, password: String) {
        val sharedPref = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putBoolean("rememberMe", true)
            putString("username", username)
            putString("password", password)
            apply()
        }
    }

    private fun loadCredentials() {
        val sharedPref = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
        val rememberMe = sharedPref.getBoolean("rememberMe", false)
        if (rememberMe) {
            etUsername.setText(sharedPref.getString("username", ""))
            etPassword.setText(sharedPref.getString("password", ""))
            cbRememberMe.isChecked = true
        }
    }

    private fun clearCredentials() {
        val sharedPref = getSharedPreferences("loginPrefs", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
