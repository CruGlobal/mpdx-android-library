package org.mpdx.android.features.contacts.contactdetail.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.huannguyen.swipeablerv.SWItemRemovalListener
import io.huannguyen.swipeablerv.SWSnackBarDataProvider
import io.huannguyen.swipeablerv.adapter.SWAdapter
import io.huannguyen.swipeablerv.utils.ResourceUtils
import javax.inject.Inject
import org.mpdx.android.R
import org.mpdx.android.base.realm.UniqueItemRealmDataBindingAdapter
import org.mpdx.android.databinding.ItemTaskBinding
import org.mpdx.android.features.tasks.model.Task
import org.mpdx.android.features.tasks.viewmodel.TaskViewModel
import org.mpdx.android.utils.StringResolver

class ContactTasksAdapter @Inject constructor(
    private val listener: ContactTasksViewListener,
    private val stringResolver: StringResolver
) : UniqueItemRealmDataBindingAdapter<Task, ItemTaskBinding>(), SWAdapter<Task?> {
    private var contactName: String? = null
    private var itemRemovalListener: SWItemRemovalListener<Task?>? = null
    private var snackBarDataProvider: SWSnackBarDataProvider? = null

    // region Lifecycle Events
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false).also {
            it.task = TaskViewModel()
            it.listener = listener
            it.resolver = stringResolver
        }

    override fun onBindViewDataBinding(binding: ItemTaskBinding, position: Int) {
        val task = getItem(position)
        binding.task?.model = task
    }

    // endregion Lifecycle Events
    fun updateName(name: String?) {
        contactName = name
    }

    override fun setSnackBarDataProvider(snackBarDataProvider: SWSnackBarDataProvider) {
        this.snackBarDataProvider = snackBarDataProvider
    }

    override fun onItemCleared(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val adapterPosition = viewHolder.adapterPosition
        val item = getItem(adapterPosition)
        displayTaskDeletedSnackBar(viewHolder, item, adapterPosition, direction)
        if (itemRemovalListener != null) {
            itemRemovalListener!!.onItemTemporarilyRemoved(item, adapterPosition)
        }
    }

    private fun displayTaskDeletedSnackBar(
        viewHolder: RecyclerView.ViewHolder,
        item: Task?,
        adapterPosition: Int,
        direction: Int
    ) {
        if (snackBarDataProvider != null && snackBarDataProvider?.isUndoEnabled == true) {
            val message = getSnackBarMessage(viewHolder, direction) ?: return
            val snackbar = Snackbar.make(snackBarDataProvider!!.view, message, Snackbar.LENGTH_LONG)
                .setAction(
                    getUndoActionText(viewHolder, direction)
                ) { v: View? ->
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
            snackbar.view.setOnClickListener { v: View? -> snackbar.dismiss() }
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

    override fun setItemRemovalListener(itemRemovalListener: SWItemRemovalListener<Task?>) {
        this.itemRemovalListener = itemRemovalListener
    }
}
