package org.mpdx.android.features.coaching;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.utils.NetUtilsKt;
import org.mpdx.android.core.modal.ModalFragment;
import org.mpdx.android.core.model.AccountList;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.base.fragments.BaseFragment;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.SortedListAdapterCallback;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import dagger.hilt.android.AndroidEntryPoint;

import static org.mpdx.android.core.model.AccountListKt.COMPARATOR_ACCOUNT_LIST_NAME;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_COACHING_LIST;

@AndroidEntryPoint
public class CoachingFragment extends BaseFragment implements ModalFragment {

    @BindView(R2.id.coaching_toolbar) Toolbar toolbar;
    @BindView(R2.id.coaching_recycler_view) RecyclerView coachingRecycler;
    @BindView(R2.id.coaching_swipe_layout) SwipeRefreshLayout coachingSwipeLayout;

    private CoachingAdapter adapter;
    private CoachingFragmentViewModel viewModel;

    public static Fragment create(String id, long deepLinkTime) {
        CoachingFragment fragment = new CoachingFragment();
        fragment.setDeepLinkId(id, deepLinkTime);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CoachingFragmentViewModel.class);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getSupportActivity().setSupportActionBar(toolbar);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        coachingRecycler.setLayoutManager(linearLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(coachingRecycler.getContext(),
                linearLayoutManager.getOrientation());
        coachingRecycler.addItemDecoration(dividerItemDecoration);

        adapter = new CoachingAdapter();
        coachingRecycler.setAdapter(adapter);

        coachingSwipeLayout.setOnRefreshListener(() -> viewModel.syncData(true));
        viewModel.getSyncTracker().isSyncing().observe(getViewLifecycleOwner(), state -> {
            if (coachingSwipeLayout != null) {
                coachingSwipeLayout.setRefreshing(state);
            }
        });

        viewModel.getAllCoachingAccounts()
                .observe(getViewLifecycleOwner(), accountLists -> adapter.update(accountLists));

        if (!NetUtilsKt.isNetworkAvailable(requireContext())) {
            Toast.makeText(getActivity(), R.string.coaching_connectivity_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mEventBus.post(new AnalyticsScreenEvent(SCREEN_COACHING_LIST));
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_coaching;
    }

    private class CoachingAdapter extends RecyclerView.Adapter<CoachingAdapter.CoachingViewHolder> {

        private SortedList<AccountList> accounts;

        CoachingAdapter() {
            accounts = new SortedList<>(AccountList.class,
                new SortedListAdapterCallback<AccountList>(this) {
                    @Override
                    public int compare(AccountList o1, AccountList o2) {
                        return COMPARATOR_ACCOUNT_LIST_NAME.compare(o1, o2);
                    }

                    @Override
                    public boolean areContentsTheSame(AccountList o1, AccountList o2) {
                        if (o1 == null && o2 == null) {
                            return true;
                        } else if (o1 == null || o2 == null || !o1.getId().equals(o2.getId())) {
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public boolean areItemsTheSame(AccountList o1, AccountList o2) {
                        if (o1 == null && o2 == null) {
                            return true;
                        } else if (o1 == null || o2 == null || !o1.getId().equals(o2.getId())) {
                            return false;
                        }
                        return true;
                    }
                });
        }

        @Override
        public CoachingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            CoachingView view = new CoachingView(getActivity());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);
            return new CoachingViewHolder(view);
        }

        public void update(List<AccountList> accounts) {
            this.accounts.clear();
            this.accounts.addAll(accounts);
            notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(CoachingViewHolder holder, int position) {
            holder.update(accounts.get(position));
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        class CoachingViewHolder extends RecyclerView.ViewHolder {
            private CoachingView coachingView;

            CoachingViewHolder(CoachingView coachingView) {
                super(coachingView);
                this.coachingView = coachingView;
                int padding = getResources().getDimensionPixelOffset(R.dimen.appeals_padding);
                coachingView.setPadding(padding, padding, padding, padding);

                coachingView.setListener(id ->
                        startActivity(CoachingDetailActivity.getIntent(getContext(), id)));
            }

            public void update(AccountList accountList) {
                coachingView.setAccountList(accountList);
            }
        }
    }
}
