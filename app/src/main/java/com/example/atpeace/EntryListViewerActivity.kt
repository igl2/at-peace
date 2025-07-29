package com.example.atpeace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class EntryListViewerActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var db: FirebaseDatabase
    private lateinit var journalRef : DatabaseReference
    private lateinit var recyclerViewJournalEntries: RecyclerView
    private lateinit var noJournalEntriesView: TextView
    private lateinit var btnLogout: Button
    private lateinit var journalEntryAdapter: JournalEntryAdapter
    private val journalEntriesList = mutableListOf<JournalEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        user = auth.currentUser
        if(user == null) {
            redirectToLogin()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_entry_list_viewer)

        db = FirebaseDatabase.getInstance()
        journalRef = db.getReference("journal_entries")
        recyclerViewJournalEntries = findViewById(R.id.recyclerViewJournalEntries)
        noJournalEntriesView = findViewById(R.id.noJournalEntriesView)
        btnLogout = findViewById(R.id.btn_logout)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnLogout.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(this@EntryListViewerActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        setupRecyclerView()

        loadJournalEntries()

    }

    private fun redirectToLogin() {
        val intent = Intent(this@EntryListViewerActivity, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerView() {
        journalEntryAdapter = JournalEntryAdapter(journalEntriesList) { clickedEntry ->
            // Logs the title of the clicked entry (insecure implementation for educational purposes)
            Log.d("EntryClick", "Clicked on: ${clickedEntry.title}")
            // Toasts the title of the clicked entry (insecure implementation for educational purposes)
            Toast.makeText(this@EntryListViewerActivity, "Clicked: ${clickedEntry.title}", Toast.LENGTH_SHORT).show()
        }
        recyclerViewJournalEntries.layoutManager = LinearLayoutManager(this)
        recyclerViewJournalEntries.adapter = journalEntryAdapter
        Log.d("RecyclerViewSetup", "LayoutManager and Adapter set.")
    }

    private fun loadJournalEntries() {
        Log.d("Firebase", "Setting up Firebase listener for journal entries")
        journalRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Logs the existence of the snapshot (insecure implementation for educational purposes)
                Log.d("FirebaseData", "onDataChange called. Snapshot exists: ${snapshot.exists()}")
                // Log the raw data (insecure implementation for educational purposes)
                Log.d("FirebaseData", "Raw data: ${snapshot.value}")

                journalEntriesList.clear()
                for (entrySnapshot in snapshot.children) {
                    try {
                        val id = entrySnapshot.key ?: ""
                        val title = entrySnapshot.child("title").getValue(String::class.java) ?: ""
                        // Logs the entry snapshot key and parsed title (insecure implementation for educational purposes)
                        Log.d("FirebaseParse", "Parsed title for key ${entrySnapshot.key}: '$title'")
                        val content = entrySnapshot.child("content").getValue(String::class.java) ?: ""
                        val timestamp = entrySnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                        if (id.isNotEmpty()) {
                            journalEntriesList.add(JournalEntry(id, title ?: "", content ?: "", timestamp ?: 0L))
                        } else {
                            Log.w("FirebaseParse", "Skipping entry with null key")
                        }
                    } catch (e: Exception) {
                        // Logs the entry snapshot key (insecure implementation for educational purposes)
                        Log.e("FirebaseParse", "Error parsing journal entry: ${entrySnapshot.key}", e)
                    }
                }

                Log.d("FirebaseData", "Parsed ${journalEntriesList.size} entries.")

                journalEntriesList.sortByDescending { it.timestamp }

                if(::journalEntryAdapter.isInitialized) {
                    journalEntryAdapter.notifyDataSetChanged()
                    // Logs the item count (insecure implementation for educational purposes)
                    Log.d("AdapterUpdate", "Adapter notified. Item count: ${journalEntryAdapter.itemCount}")
                } else {
                    Log.e("AdapterUpdate", "Adapter not initialized when trying to notify.")
                }

                if(journalEntriesList.isEmpty()) {
                    noJournalEntriesView.visibility = View.VISIBLE
                    recyclerViewJournalEntries.visibility = View.GONE
                    Log.d("UIVisibility", "No entries. Showing noJournalEntriesView.")
                } else {
                    noJournalEntriesView.visibility = View.GONE
                    recyclerViewJournalEntries.visibility = View.VISIBLE
                    Log.d("UIVisibility", "Entries found. Showing recyclerViewJournalEntries.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Load failed: ${error.message}", error.toException())
                Toast.makeText(this@EntryListViewerActivity, "Failed to load journal entries: ${error.message}", Toast.LENGTH_SHORT).show()
                recyclerViewJournalEntries.visibility = View.GONE
            }
        })
    }
}