package org.mpdx.android.features.selector

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.fragment.app.commit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView.NO_ID
import io.realm.OrderedRealmCollection
import io.realm.RealmModel
import org.ccci.gto.android.common.realm.adapter.RealmDataBindingAdapter
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.ccci.gto.android.common.support.v4.util.IdUtils
import org.ccci.gto.android.common.util.findListener
import org.mpdx.android.R
import org.mpdx.android.base.fragment.DataBindingFragment
import org.mpdx.android.base.model.UniqueItem
import org.mpdx.android.databinding.SelectorFragmentBinding
import org.mpdx.android.databinding.SelectorItemBinding
import splitties.fragmentargs.argOrDefault

abstract class BaseSelectorFragment<T : RealmModel>(
    @StringRes private val title: Int,
    private val itemLabel: T.() -> String?,
    private val enableSearch: Boolean
) : DataBindingFragment<SelectorFragmentBinding>(), OnItemSelectedListener<T> {
    protected abstract val dataModel: SelectorFragmentDataModel<T>

    // region Lifecycle
    override fun onAttach(context: Context) {
        super.onAttach(context)
        setupBackPressedCallback()
    }

    override fun onCreateDataBinding(inflater: LayoutInflater, container: ViewGroup?) =
        SelectorFragmentBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
    }

    final override fun onItemSelected(item: T?) {
        dismiss()
        dispatchItemSelectedCallback(item)
    }

    override fun onDetach() {
        removeBackPressedCallback()
        super.onDetach()
    }
    // endregion Lifecycle

    protected open fun dispatchItemSelectedCallback(item: T?) {
        findListener<OnItemSelectedListener<T>>()?.onItemSelected(item)
    }

    // region Toolbar
    private fun setupToolbar() {
        binding.toolbar.setTitle(title)
        if (enableSearch) {
            binding.toolbar.inflateMenu(R.menu.selector_fragment_menu)
            setupSearchAction()
        }
    }

    // region Search Action
    private val searchMenuItem: MenuItem? get() = binding.toolbar.menu.findItem(R.id.action_search)

    private fun setupSearchAction() {
        (searchMenuItem?.actionView as? SearchView)?.apply {
            queryHint = getString(R.string.search_hint)
            setQuery(dataModel.query.value, false)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(query: String): Boolean {
                    dataModel.query.value = query
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    dataModel.query.value = query
                    return false
                }
            })
        }
    }
    // endregion Search Action
    // endregion Toolbar

    // region RecyclerView
    private val adapter by lazy {
        ItemAdapter(this, this, itemLabel)
            .also { dataModel.items.observe(this, it) }
    }

    private fun setupRecyclerView() {
        binding.searchRecycler.also {
            it.lifecycleOwner = viewLifecycleOwner
            it.adapter = adapter
        }
    }
    // endregion RecyclerView

    // region Show/Dismiss logic
    private var backStackId by argOrDefault(-1)
    private val backPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = dismiss()
        }
    }

    fun show(fm: FragmentManager, @IdRes container: Int = R.id.frame) {
        with(fm.beginTransaction()) {
            setReorderingAllowed(true)
            addToBackStack(null)
            setCustomAnimations(R.anim.slide_in_up, 0, 0, R.anim.slide_out_down)
            replace(container, this@BaseSelectorFragment)
            setPrimaryNavigationFragment(this@BaseSelectorFragment)
            backStackId = commit()
        }
    }

    private fun dismiss() = when {
        backStackId != -1 -> parentFragmentManager.popBackStack(backStackId, POP_BACK_STACK_INCLUSIVE)
        else -> parentFragmentManager.commit(true) {
            setReorderingAllowed(true)
            remove(this@BaseSelectorFragment)
        }
    }

    private fun setupBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(this, backPressedCallback)
    }

    private fun removeBackPressedCallback() {
        backPressedCallback.remove()
    }
    // endregion Show/Dismiss logic
}

interface SelectorFragmentDataModel<T> {
    val query: MutableLiveData<String>
    val items: LiveData<out OrderedRealmCollection<T>?>
}

interface OnItemSelectedListener<T> {
    fun onItemSelected(item: T?)
}

private class ItemAdapter<T : RealmModel>(
    lifecycleOwner: LifecycleOwner?,
    private val listener: OnItemSelectedListener<T>,
    private val label: T.() -> String?
) : RealmDataBindingAdapter<T, SelectorItemBinding>(lifecycleOwner) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) =
        (getItem(position) as? UniqueItem)?.id?.let { IdUtils.convertId(it) } ?: NO_ID

    // region Lifecycle
    override fun onCreateViewDataBinding(parent: ViewGroup, viewType: Int) =
        SelectorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            .also { it.listener = listener }

    override fun onBindViewDataBinding(binding: SelectorItemBinding, position: Int) {
        val item = getItem(position)
        binding.label = item?.label()
        binding.item = item
    }
    // endregion Lifecycle
}
