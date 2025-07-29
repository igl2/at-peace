package com.example.atpeace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class JournalEntryActivity : AppCompatActivity() {

    private lateinit var db: FirebaseDatabase
    private lateinit var journalRef : DatabaseReference
    private lateinit var editTextJournalTitle: TextInputEditText
    private lateinit var editTextJournalEntry: TextInputEditText
    private lateinit var btnSaveJournalEntry: Button
    private lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        auth = Firebase.auth
        user = auth.currentUser

        if(user == null) {
            redirectToLogin()
            return
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_journal_entry)
        editTextJournalTitle = findViewById(R.id.journalEntryTitleText)
        editTextJournalEntry = findViewById(R.id.journalEntryText)
        btnSaveJournalEntry = findViewById(R.id.btn_save_journal_entry)

        btnLogout = findViewById(R.id.btn_logout)

        db = FirebaseDatabase.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnLogout.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(this@JournalEntryActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        btnSaveJournalEntry.setOnClickListener {
            val journalEntryTitle = editTextJournalTitle.text.toString().trim()
            val journalEntryContent = editTextJournalEntry.text.toString().trim()
            val currentTimestamp = System.currentTimeMillis()
            val currentUserId = auth.currentUser?.uid

            if (currentUserId == null) {
                Toast.makeText(this@JournalEntryActivity, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return@setOnClickListener
            }

            if (journalEntryContent.isEmpty()) {
                Toast.makeText(this@JournalEntryActivity, "Journal Entry cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            journalRef = db.getReference("journal_entries")
            val entryID = journalRef.push().key

            if (entryID == null) {
                Toast.makeText(this@JournalEntryActivity, "Failed to create an entry ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val journalEntryData = HashMap<String, Any>()
            journalEntryData["id"] = entryID
            journalEntryData["title"] = journalEntryTitle
            journalEntryData["content"] = journalEntryContent
            journalEntryData["timestamp"] = currentTimestamp
            journalEntryData["userId"] = currentUserId

            journalRef.child(entryID).setValue(journalEntryData)
                .addOnSuccessListener {
                    Toast.makeText(this@JournalEntryActivity, "Journal Entry Saved", Toast.LENGTH_SHORT).show()
                    // Logs the user and entry IDs (insecure implementation for educational purposes)
                    Log.d("Firebase", "Entry Saved successfully under user $currentUserId, entry $entryID")
                    // Logs the title (insecure implementation for educational purposes)
                    Log.d("Firebase", "Title: $journalEntryTitle")
                    // Logs the content (insecure implementation for educational purposes)
                    Log.d("Firebase", "Content: $journalEntryContent")
                    // Logs the timestamp (insecure implementation for educational purposes)
                    Log.d("Firebase", "Timestamp: $currentTimestamp")
                    val intent = Intent(this@JournalEntryActivity, EntryListViewerActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@JournalEntryActivity, "Failed to save journal entry: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firebase", "Failed to save journal entry: ", e)
                }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this@JournalEntryActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}