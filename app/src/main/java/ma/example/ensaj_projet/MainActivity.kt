package ma.example.ensaj_projet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import ma.example.ensaj_projet.adapter.ReminderAdapter
import ma.example.ensaj_projet.beans.Reminder
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var reminderRecyclerView: RecyclerView
    private lateinit var reminderAdapter: ReminderAdapter
    private val reminders = mutableListOf<Reminder>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Reminders"
        val toolbarTitle = toolbar.getChildAt(0) as TextView
        toolbarTitle.setTypeface(toolbarTitle.typeface, Typeface.BOLD)
        val overflowIcon = toolbar.getOverflowIcon()
        overflowIcon?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_ATOP)

        reminderRecyclerView = findViewById(R.id.recyclerView)
        reminderRecyclerView.layoutManager = LinearLayoutManager(this)

        reminderAdapter = ReminderAdapter(reminders) { position ->

            showDeleteConfirmationDialog(position)
        }

        reminderRecyclerView.adapter = reminderAdapter
        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(reminderAdapter))
        itemTouchHelper.attachToRecyclerView(reminderRecyclerView)

    }
    private fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Yes") { _, _ ->
                reminderAdapter.removeItem(position)
                Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                reminderAdapter.notifyItemChanged(position)
            }
            .show()
    }

    fun requestPermission(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {

            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {

            } else {

                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {

            fetchFCMToken()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchFCMToken()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM: ", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val token = task.result
            Log.d("FCM: ", "Fetching FCM registration token: $token")
            Toast.makeText(baseContext, "Token fetched", Toast.LENGTH_SHORT).show()
        })
    }

    fun addReminder(view: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.reminderTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.reminderDescription)
        val dateTextView = dialogView.findViewById<TextView>(R.id.reminderDate)
        val timeTextView = dialogView.findViewById<TextView>(R.id.reminderTime)

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

        AlertDialog.Builder(this)
            .setTitle("Add Reminder")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString()
                val description = descriptionInput.text.toString()
                val timeInMillis = calendar.timeInMillis

                if (timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Please select a future date and time.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val data = Data.Builder()
                    .putString("TITLE", title)
                    .putString("MESSAGE", description)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(timeInMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build()

                WorkManager.getInstance(this).enqueue(workRequest)

                val reminder = Reminder(reminders.size, title, description, timeInMillis)
                reminders.add(reminder)

                reminderAdapter.updateList(reminders)
                reminderRecyclerView.scrollToPosition(reminders.size - 1)

                Toast.makeText(this, "Reminder added: $title", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
