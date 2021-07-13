package org.mpdx.android.features.settings;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;

import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.utils.StringResolver;
import org.mpdx.android.core.modal.ModalFragment;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.base.fragments.BaseFragment;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import dagger.hilt.android.AndroidEntryPoint;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_SETTINGS_ACCOUNT_LIST;

@AndroidEntryPoint
public class SetAccountListFragment extends BaseFragment implements ModalFragment {

    @BindView(R2.id.set_account_list_toolbar) Toolbar toolbar;
    @BindView(R2.id.account_list_recycler) RecyclerView recyclerView;

    @Inject AppPrefs appPrefs;
    @Inject StringResolver stringResolver;
    @Inject ConnectivityManager connectivityManager;

    private SetAccountListAdapter adapter;
    private SetAccountListFragmentViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SetAccountListFragmentViewModel.class);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new SetAccountListAdapter(this, appPrefs, stringResolver, connectivityManager);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(adapter);
        viewModel.getAllAccountList().observe(getViewLifecycleOwner(), accountLists -> adapter
                .update(accountLists != null ? accountLists : new ArrayList<>()));
    }

    @Override
    public void onResume() {
        super.onResume();
        mEventBus.post(new AnalyticsScreenEvent(SCREEN_SETTINGS_ACCOUNT_LIST));
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_set_account_list;
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }
}
