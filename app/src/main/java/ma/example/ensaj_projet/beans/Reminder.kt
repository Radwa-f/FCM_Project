package ma.example.ensaj_projet.beans

data class Reminder(
    val id: Int,
    val title: String,
    val description: String,
    val timeInMillis: Long,
    var isChecked: Boolean = false
)
