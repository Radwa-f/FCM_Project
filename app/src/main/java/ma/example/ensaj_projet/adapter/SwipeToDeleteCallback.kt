package ma.example.ensaj_projet

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ma.example.ensaj_projet.adapter.ReminderAdapter

class SwipeToDeleteCallback(private val adapter: ReminderAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false // We don't need to handle move events
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Get the position of the item that was swiped
        val position = viewHolder.adapterPosition
        adapter.onItemSwiped(position)
    }
}
