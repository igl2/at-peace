package com.example.atpeace

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.atpeace.databinding.ActivityLoginBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnCreateAnAccount: Button
    private lateinit var binding: ActivityLoginBinding
    private val MAX_ATTEMPTS = 3
    private val LOCKOUT_DURATION = 15 * 60 * 1000L // 15 minutes in milliseconds
    private var loginAttempts = 0

        public override fun onStart() {
        super.onStart()
//        val currentUser = auth.currentUser
//        if (currentUser != null) {
//            val intent = Intent(this@LoginActivity, EntryListViewerActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        auth = Firebase.auth
        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)
        btnLogin = findViewById(R.id.btn_login)
        progressBar = findViewById(R.id.progressBar)
        btnCreateAnAccount = findViewById(R.id.btn_create_an_account)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnCreateAnAccount.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val email = editTextEmail.text.toString()
            val password = editTextPassword.text.toString()

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this@LoginActivity, "Please enter an email address", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this@LoginActivity, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            val trimmedPassword = password?.trim()

            if (trimmedPassword.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPasswordStrong(trimmedPassword)) {
                Toast.makeText(
                    this,
                    "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (isAccountLocked()) {
                Toast.makeText(this, "Account locked! Try again later.", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        // reset failed attempts
                        resetLockout()
                        // Log the username (insecure implementation for educational purposes)
                        Log.d("Login", "Username: ${auth.currentUser?.email}")
                        // Log the password (insecure implementation for educational purposes)
                        Log.d("Auth", "User authentication successful")
                        Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT)
                            .show()
                        val intent = Intent(this@LoginActivity, JournalEntryActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        loginAttempts++
                        val errorMessage = when (val exception = task.exception) {
                            is FirebaseAuthInvalidUserException -> {
                                Log.e("LoginError", "Invalid user: ${exception.message}")
                                "No account found with this email."
                            }

                            is FirebaseAuthInvalidCredentialsException -> {
                                Log.e("LoginError", "Invalid credentials: ${exception.message}")
                                "Incorrect email or password."
                            }

                            is FirebaseAuthUserCollisionException -> {
                                Log.e("LoginError", "User already exists: ${exception.message}")
                                "This account already exists."
                            }

                            is FirebaseAuthWeakPasswordException -> {
                                Log.e("LoginError", "Weak password: ${exception.message}")
                                "Password is too weak."
                            }

                            else -> {
                                Log.e("LoginError", "Unknown error: ${exception?.message}")
                                "Authentication failed. Please try again."
                            }
                        }

                        if (loginAttempts >= MAX_ATTEMPTS) {
                            lockAccount()
                            Toast.makeText(
                                this,
                                "Too many failed attempts. Account locked for 15 minutes.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "$errorMessage Attempts left: ${MAX_ATTEMPTS - loginAttempts}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val passwordPattern =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{8,}$")
        return passwordPattern.matches(password)
    }

    private fun isAccountLocked(): Boolean {
        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val lockoutEndTime = prefs.getLong("lockoutEndTime", 0L)
        return System.currentTimeMillis() < lockoutEndTime
    }

    private fun lockAccount() {
        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        val lockoutEndTime = System.currentTimeMillis() + LOCKOUT_DURATION
        editor.putLong("lockoutEndTime", lockoutEndTime)
        editor.apply()
    }

    private fun resetLockout() {
        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        prefs.edit().remove("lockoutEndTime").apply()
        loginAttempts = 0
    }
}