package org.mpdx.android.features.tasks.tasklist

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.huannguyen.swipeablerv.SWItemRemovalListener
import io.huannguyen.swipeablerv.SWSnackBarDataProvider
import io.huannguyen.swipeablerv.adapter.SWAdapter
import io.huannguyen.swipeablerv.utils.ResourceUtils
import org.mpdx.android.R
import org.mpdx.android.core.domain.UniqueItemAdapter
import org.mpdx.android.databinding.ItemTaskBinding
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.viewmodel.TaskViewModel
import org.mpdx.android.utils.StringResolver

class TasksViewAllAdapter(private val stringResolver: StringResolver, private val listener: TasksViewAllListener) :
    UniqueItemAdapter<Task?, TasksViewAllAdapter.ViewHolder?>(), SWAdapter<Task> {
    private var itemRemovalListener: SWItemRemovalListener<Task>? = null
    private var snackBarDataProvider: SWSnackBarDataProvider? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val currentTaskItemBinding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(currentTaskItemBinding, stringResolver, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = items[position]!!
        if (task.isValid) {
            holder.update(task)
        }
    }

    fun completeTask(task: Task) {
        for (i in items.indices) {
            if (items[i]!!.id == task.id) {
                items.removeAt(i)
                notifyItemRemoved(i)
                break
            }
        }
    }

    override fun update(list: List<Task?>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun setSnackBarDataProvider(snackBarDataProvider: SWSnackBarDataProvider) {
        this.snackBarDataProvider = snackBarDataProvider
    }

    override fun onItemCleared(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val adapterPosition = viewHolder.adapterPosition
        val item = items[adapterPosition]!!
        displayTaskDeletedSnackBar(viewHolder, item, adapterPosition, direction)
        if (itemRemovalListener != null) {
            itemRemovalListener!!.onItemTemporarilyRemoved(item, adapterPosition)
        }
        items.removeAt(adapterPosition)
        notifyItemRemoved(adapterPosition)
    }

    private fun displayTaskDeletedSnackBar(
        viewHolder: RecyclerView.ViewHolder,
        item: Task,
        adapterPosition: Int,
        direction: Int
    ) {
        if (snackBarDataProvider != null && snackBarDataProvider!!.isUndoEnabled) {
            val snackMessenger = getSnackBarMessage(viewHolder, direction) ?: return
            val snackbar = Snackbar
                .make(snackBarDataProvider!!.view, snackMessenger, Snackbar.LENGTH_LONG)
                .setAction(
                    getUndoActionText(viewHolder, direction)
                ) {
                    items.add(adapterPosition, item)
                    notifyItemInserted(adapterPosition)
                    if (itemRemovalListener != null) {
                        itemRemovalListener!!.onItemAddedBack(item, adapterPosition)
                    }
                }
            snackbar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(snackbar: Snackbar, event: Int) {
                    if (event != DISMISS_EVENT_ACTION && itemRemovalListener != null) {
                        itemRemovalListener!!.onItemPermanentlyRemoved(item)
                    }
                }
            })

            // Set colors
            val snackBarView = snackbar.view
            val snackBarBackgroundColor = snackBarDataProvider!!.getSnackBarBackgroundColor(direction)
            if (snackBarBackgroundColor != ResourceUtils.NO_COLOR) {
                snackBarView.setBackgroundColor(snackBarBackgroundColor)
            }
            val undoActionTextColor = snackBarDataProvider!!.getUndoActionTextColor(direction)
            if (undoActionTextColor != ResourceUtils.NO_COLOR) {
                snackbar.setActionTextColor(undoActionTextColor)
            }
            val infoMessageColor = snackBarDataProvider!!.getSnackBarMessageColor(direction)
            if (infoMessageColor != ResourceUtils.NO_COLOR) {
                val textView = snackBarView.findViewById<TextView>(R.id.snackbar_text)
                textView.setTextColor(infoMessageColor)
            }
            snackbar.show()
            snackbar.view.setOnClickListener { snackbar.dismiss() }
        }
    }

    override fun getSnackBarMessage(viewHolder: RecyclerView.ViewHolder, direction: Int): String? {
        return if (snackBarDataProvider != null) {
            snackBarDataProvider!!.getSnackBarMessage(direction)
        } else null
    }

    override fun getUndoActionText(viewHolder: RecyclerView.ViewHolder, direction: Int): String? {
        return if (snackBarDataProvider != null) {
            snackBarDataProvider!!.getUndoActionText(direction)
        } else null
    }

    override fun getSwipeDirs(viewHolder: RecyclerView.ViewHolder): Int {
        return -1
    }

    override fun getItemRemovalListener(): SWItemRemovalListener<*>? {
        return itemRemovalListener
    }

    override fun setItemRemovalListener(itemRemovalListener: SWItemRemovalListener<Task>) {
        this.itemRemovalListener = itemRemovalListener
    }

    class ViewHolder(
        private val binding: ItemTaskBinding,
        stringResolver: StringResolver?,
        listener: TasksViewAllListener?
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun update(task: Task?) {
            binding.task?.model = task
            binding.executePendingBindings()
        }

        init {
            binding.resolver = stringResolver
            binding.listener = listener
            binding.task = TaskViewModel()
        }
    }
}
