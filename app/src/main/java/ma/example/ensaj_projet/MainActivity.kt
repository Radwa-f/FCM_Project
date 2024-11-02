package ma.example.ensaj_projet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import ma.example.ensaj_projet.adapter.ReminderAdapter
import ma.example.ensaj_projet.beans.Reminder
import okhttp3.*
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var reminderRecyclerView: RecyclerView
    private lateinit var reminderAdapter: ReminderAdapter
    private val reminders = mutableListOf<Reminder>()
    private lateinit var db: FirebaseFirestore
    private var fcmToken = "dQ1kVyBWRyiHkKgVodNOrp:APA91bFQ4V9sQGxVgqRziaDK8dzqoGawHFadEM4ZNl5BZ5WSrj6lUUOKMqZ7ER5L8BilQLs6S3FCPPEsOipQtKUUoqims-qjfi1yHcRok0_kcpGxiNkE8AQ"

    companion object {
        private const val REQUEST_UPDATE_REMINDER = 1
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = FirebaseFirestore.getInstance()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Reminders"
        val toolbarTitle = toolbar.getChildAt(0) as TextView
        toolbarTitle.setTypeface(toolbarTitle.typeface, Typeface.BOLD)
        val overflowIcon = toolbar.overflowIcon
        overflowIcon?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_ATOP)

        reminderRecyclerView = findViewById(R.id.recyclerView)
        reminderRecyclerView.layoutManager = LinearLayoutManager(this)

        reminderAdapter = ReminderAdapter(reminders) { position ->
            showDeleteConfirmationDialog(position)
        }
        reminderRecyclerView.adapter = reminderAdapter
        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(reminderAdapter))
        itemTouchHelper.attachToRecyclerView(reminderRecyclerView)

        loadRemindersFromFirestore()
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        val reminderToDelete = reminders[position]
        AlertDialog.Builder(this)
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Yes") { _, _ ->
                db.collection("reminders").document(reminderToDelete.id.toString())
                    .delete()
                    .addOnSuccessListener {
                        reminderAdapter.removeItem(position)
                        Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to delete reminder: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Firestore", "Error deleting document", e)
                    }
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun addReminder(view: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.reminderTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.reminderDescription)
        val dateTextView = dialogView.findViewById<TextView>(R.id.reminderDate)
        val timeTextView = dialogView.findViewById<TextView>(R.id.reminderTime)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val addButton = dialogView.findViewById<Button>(R.id.addButton)

        val calendar = Calendar.getInstance()

        dateTextView.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                dateTextView.text = "$dayOfMonth/${month + 1}/$year"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        timeTextView.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                timeTextView.text = String.format("%02d:%02d", hourOfDay, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Reminder")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        cancelButton.setOnClickListener { dialog.dismiss() }

        addButton.setOnClickListener {
            val title = titleInput.text.toString()
            val description = descriptionInput.text.toString()
            val timeInMillis = calendar.timeInMillis

            if (timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(this, "Please select a future date and time.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val reminderId = reminders.size
            val reminder = Reminder(reminderId, title, description, timeInMillis)

            reminders.add(reminder)
            reminderAdapter.updateList(reminders)
            reminderRecyclerView.scrollToPosition(reminders.size - 1)
            Toast.makeText(this, "Reminder added: $title", Toast.LENGTH_SHORT).show()

            val reminderData = hashMapOf(
                "id" to reminderId,
                "title" to title,
                "description" to description,
                "time" to timeInMillis,
                "userToken" to fcmToken
            )

            db.collection("reminders")
                .document(reminderId.toString())
                .set(reminderData)
                .addOnSuccessListener {
                    Log.e("Firestore", "Added successfully")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save reminder: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Error adding document", e)
                }

            dialog.dismiss()
        }
        dialog.show()
    }

    private fun loadRemindersFromFirestore() {
        db.collection("reminders")
            .get()
            .addOnSuccessListener { documents ->
                reminders.clear()
                for (document in documents) {
                    Log.d("Firestore", "Document: ${document.id} => ${document.data}")
                    val reminder = document.toObject(Reminder::class.java)
                    reminders.add(reminder)
                }
                reminderAdapter.updateList(reminders)
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting reminders: ", exception)
                Toast.makeText(this, "Failed to load reminders: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val menuItem = menu.findItem(R.id.app_bar_search)
        val searchView = MenuItemCompat.getActionView(menuItem) as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filterList(newText)
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.share) {
            val txt = "Check out my great Reminders app!"
            val mimeType = "text/plain"
            ShareCompat.IntentBuilder
                .from(this)
                .setType(mimeType)
                .setChooserTitle("Share this app via:")
                .setText(txt)
                .startChooser()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    private fun filterList(query: String) {
        val filteredReminderList = if (query.isEmpty()) {
            reminders
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            reminders.filter { reminder ->
                reminder.title.lowercase().contains(lowerCaseQuery) ||
                        reminder.description.lowercase().contains(lowerCaseQuery)
            }
        }

        reminderAdapter.updateList(filteredReminderList)
    }

}
