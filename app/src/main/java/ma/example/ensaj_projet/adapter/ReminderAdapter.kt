package ma.example.ensaj_projet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ma.example.ensaj_projet.R
import ma.example.ensaj_projet.beans.Reminder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAdapter(
    private val reminders: MutableList<Reminder>,
    private val onDeleteClick: (Int) -> Unit // Callback for deletion
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.reminderTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.reminderDescription)
        private val timeTextView: TextView = itemView.findViewById(R.id.reminderTime)
        private val checkbox: CheckBox = itemView.findViewById(R.id.reminderCheckbox)

        fun bind(reminder: Reminder) {
            titleTextView.text = reminder.title
            descriptionTextView.text = reminder.description
            timeTextView.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reminder.timeInMillis))
            checkbox.isChecked = reminder.isChecked

            // Handle checkbox click
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                reminder.isChecked = isChecked // Update the reminder's status
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.reminder_item, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.bind(reminder)
    }

    override fun getItemCount() = reminders.size

    // New method to handle swipe action
    fun onItemSwiped(position: Int) {
        onDeleteClick(position) // Call the deletion callback
    }

    fun removeItem(position: Int) {
        reminders.removeAt(position)
        notifyItemRemoved(position)
    }
}
