package org.mpdx.android.base.realm

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.realm.OrderedRealmCollection
import io.realm.RealmModel
import org.ccci.gto.android.common.realm.adapter.RealmDataBindingAdapter
import org.ccci.gto.android.common.support.v4.util.IdUtils
import org.mpdx.android.base.model.UniqueItem

abstract class UniqueItemRealmDataBindingAdapter<T, B : ViewDataBinding>(
    lifecycleOwner: LifecycleOwner? = null,
    data: OrderedRealmCollection<T>? = null
) : RealmDataBindingAdapter<T, B>(lifecycleOwner, data) where T : RealmModel, T : UniqueItem {
    init {
        @Suppress("LeakingThis")
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position)?.id?.let { IdUtils.convertId(it) } ?: RecyclerView.NO_ID
}
