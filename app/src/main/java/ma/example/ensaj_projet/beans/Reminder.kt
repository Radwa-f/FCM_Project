package ma.example.ensaj_projet.beans

data class Reminder(
    val id: Int = -1,
    val title: String = "",
    val description: String = "",
    val timeInMillis: Long = 0L,
    var isChecked: Boolean = false
)

