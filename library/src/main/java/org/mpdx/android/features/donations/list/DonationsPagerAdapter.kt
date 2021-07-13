package org.mpdx.android.features.donations.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.realm.RealmResults
import org.ccci.gto.android.common.recyclerview.util.lifecycleOwner
import org.ccci.gto.android.common.support.v4.util.IdUtils
import org.ccci.gto.android.common.viewpager.adapter.BaseDataBindingPagerAdapter
import org.ccci.gto.android.common.viewpager.adapter.DataBindingViewHolder
import org.mpdx.android.R
import org.mpdx.android.databinding.PageDonationsBinding
import org.mpdx.android.features.donations.list.DonationsPagerAdapter.DonationsViewHolder
import org.mpdx.android.features.donations.model.Donation
import org.mpdx.android.features.donations.model.DonationFields
import org.mpdx.android.utils.get
import org.mpdx.android.utils.localizedYearMonthFormatter
import org.mpdx.android.utils.size
import org.threeten.bp.YearMonth

internal class DonationsPagerAdapter(
    lifecycleOwner: LifecycleOwner,
    private val dataModel: DonationsFragmentViewModel
) : BaseDataBindingPagerAdapter<PageDonationsBinding, DonationsViewHolder>(lifecycleOwner) {
    var months: ClosedRange<YearMonth>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getCount() = months?.size ?: 0
    private inline fun monthAt(position: Int) = months!![position]
    override fun getItemId(position: Int) = IdUtils.convertId(monthAt(position))
    override fun getPageTitle(position: Int) = localizedYearMonthFormatter().format(monthAt(position))

    override fun onCreateViewHolder(parent: ViewGroup) =
        DonationsViewHolder(PageDonationsBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewDataBinding(holder: DonationsViewHolder, binding: PageDonationsBinding, position: Int) {
        super.onBindViewDataBinding(holder, binding, position)
        holder.donations = dataModel.getDonationsFor(monthAt(position))
    }

    override fun onViewDataBindingRecycled(holder: DonationsViewHolder, binding: PageDonationsBinding) {
        super.onViewDataBindingRecycled(holder, binding)
        holder.donations = null
    }

    inner class DonationsViewHolder(binding: PageDonationsBinding) :
        DataBindingViewHolder<PageDonationsBinding>(binding) {
        init {
            binding.donationRecycler.lifecycleOwner = lifecycleOwner
        }

        private val adapter = DonationsAdapter()
            .also { binding.donationRecycler.adapter = it }

        private val donationsTotalObserver = Observer<RealmResults<Donation>> { donations: RealmResults<Donation>? ->
            // TODO: this logic should be handled in data binding
            val salaryTotal = donations?.sum(DonationFields.CONVERTED_AMOUNT) ?: 0f
            val currency = donations?.firstOrNull { !it.convertedCurrency.isNullOrEmpty() }?.convertedCurrency ?: ""
            binding.donationCountText.apply {
                text = context.getString(R.string.donation_count_text, salaryTotal.toFloat(), currency)
            }
        }

        var donations: LiveData<RealmResults<Donation>>? = null
            set(value) {
                if (field == value) return
                field?.removeObserver(adapter)
                field?.removeObserver(donationsTotalObserver)
                field = value
                lifecycleOwner?.let {
                    field?.observe(it, adapter)
                    field?.observe(it, donationsTotalObserver)
                }
            }
    }
}
