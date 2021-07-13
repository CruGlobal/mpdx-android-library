package org.mpdx.android.features.settings;

import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.mpdx.android.R;
import org.mpdx.android.core.model.AccountList;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.MainActivity;
import org.mpdx.android.features.analytics.model.SettingsChangeAccountAnalyticsEvent;
import org.mpdx.android.utils.StringResolver;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import io.realm.Realm;

public class SetAccountListAdapter extends RecyclerView.Adapter<SetAccountListAdapter.ViewHolder> {

    private List<AccountList> accountLists = new ArrayList<>();
    private Fragment fragment;
    private AppPrefs appPrefs;
    private StringResolver stringResolver;
    private ConnectivityManager connectivityManager;

    public SetAccountListAdapter(Fragment fragment, AppPrefs appPrefs, StringResolver stringResolver,
            ConnectivityManager connectivityManager) {
        this.fragment = fragment;
        this.appPrefs = appPrefs;
        this.stringResolver = stringResolver;
        this.connectivityManager = connectivityManager;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AccountList accountList = accountLists.get(position);
        holder.textView.setText(accountList.getName());
        holder.textView.setOnClickListener(view -> {
            AlertDialog alertDialog = new AlertDialog.Builder(fragment.getContext()).create();
            alertDialog.setMessage(
                    stringResolver.getString(R.string.change_account_list_dialog, accountList.getName()));
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, stringResolver.getString(R.string.cancel),
                    (dialog, which) -> dialog.dismiss());
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, stringResolver.getString(R.string.yes),
                    (dialog, which) -> {
                        changeAccountList(accountList.getId());
                        EventBus.getDefault().post(SettingsChangeAccountAnalyticsEvent.INSTANCE);
                    });

            if (appPrefs.getAccountListId() != null && appPrefs.getAccountListId().equals(accountList.getId())) {
                Toast.makeText(fragment.getContext(),
                        stringResolver.getString(R.string.current_account_list, accountList.getName()),
                        Toast.LENGTH_SHORT).show();
            } else {
                alertDialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return accountLists.size();
    }

    private void changeAccountList(String accountListId) {
        if (isOffline()) {
            Toast.makeText(fragment.getContext(), stringResolver.getString(R.string.offline_error), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        appPrefs.setAccountListId(accountListId);
        // TODO: this should utilize the RealmManager
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(innerRealm -> innerRealm.deleteAll(), () -> {
            fragment.startActivity(appPrefs.getSplashActivityIntent(fragment.requireContext()));
            fragment.getActivity().setResult(MainActivity.RESULT_CLOSE_ACTIVITY);
            fragment.getActivity().finish();
        });
        realm.close();
    }

    public void update(List<AccountList> list) {
        accountLists.clear();
        accountLists.addAll(list);
        notifyDataSetChanged();
    }

    private boolean isOffline() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return !(activeNetwork != null && activeNetwork.isConnected());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}
