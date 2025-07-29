package com.example.atpeace

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalEntryAdapter (

    private val journalEntries : List<JournalEntry>,
    private val onItemClick : (JournalEntry) -> Unit

) : RecyclerView.Adapter<JournalEntryAdapter.EntryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = journalEntries[position]
        holder.bind(entry)
    }

    override fun getItemCount(): Int = journalEntries.size

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val journalEntryTitleView: TextView = itemView.findViewById(R.id.journalEntryTitleView)
        private val journalEntryDateView: TextView = itemView.findViewById(R.id.journalEntryViewDate)
        private val journalEntryPreview: TextView = itemView.findViewById(R.id.journalEntryPreview)

        fun bind(journalEntry: JournalEntry) {
            journalEntryTitleView.text = journalEntry.title.ifEmpty {
                "Untitled Journal Entry"
            }
            journalEntryDateView.text = SimpleDateFormat("MM dd, yyyy - hh:mm a", Locale.getDefault())
                .format(Date(journalEntry.timestamp))
            journalEntryPreview.text = journalEntry.content

            itemView.setOnClickListener{
                onItemClick(journalEntry)
            }
        }
    }
}