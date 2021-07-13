package org.mpdx.android.features.appeals;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ccci.gto.android.common.compat.util.LocaleCompat;
import org.ccci.gto.android.common.util.NumberUtilsKt;
import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.core.realm.AccountListQueriesKt;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.appeals.realm.AppealQueriesKt;
import org.mpdx.android.features.contacts.realm.ContactQueriesKt;
import org.mpdx.android.utils.DateFormatUtilsKt;
import org.mpdx.android.utils.DateUtils;
import org.mpdx.android.utils.DateUtilsKt;
import org.mpdx.android.utils.ViewUtilsKt;
import org.mpdx.android.core.modal.ModalFragment;

import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.appeals.model.AskedContact;
import org.mpdx.android.features.appeals.model.Pledge;
import org.mpdx.android.features.appeals.model.PledgeFields;
import org.mpdx.android.features.appeals.sync.AskedContactsSyncService;
import org.mpdx.android.features.appeals.sync.PledgesSyncService;
import org.mpdx.android.features.base.fragments.BaseFragment;
import org.mpdx.android.features.contacts.model.Contact;
import org.mpdx.android.features.contacts.model.ContactFields;
import org.mpdx.android.features.selector.OnItemSelectedListener;
import org.threeten.bp.format.FormatStyle;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.OnClick;
import dagger.hilt.android.AndroidEntryPoint;
import io.realm.Realm;
import kotlin.text.StringsKt;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_CONTACTS_EDIT_COMMITMENT;

@AndroidEntryPoint
public class AddCommitmentFragment extends BaseFragment implements ModalFragment, OnItemSelectedListener<AskedContact> {
    private static final int RESULT_COMMITMENT_SAVED = 2;
    private static final int RESULT_COMMITMENT_DELETED = 3;
    private static final String ARG_APPEAL_ID = "appeal_id";
    private static final String ARG_CONTACT_ID = "contact_id";
    private static final String ARG_PLEDGE_ID = "pledge_id";

    @Inject AskedContactsSyncService mAskedContactsSyncService;
    @Inject PledgesSyncService mPledgeSyncService;
    @Inject AppPrefs mAppPrefs;

    @BindView(R2.id.fragment_add_commitment_toolbar) Toolbar toolbar;
    @BindView(R2.id.fragment_add_commitment_contact) TextView contactNameText;
    @BindView(R2.id.fragment_add_commitment_date) TextView dateText;
    @BindView(R2.id.fragment_add_commitment_received_row) LinearLayout receivedRow;
    @BindView(R2.id.fragment_add_commitment_received_switch) SwitchCompat receivedSwitch;
    @BindView(R2.id.fragment_add_commitment_amount) TextView amountText;
    @BindView(R2.id.fragment_edit_commitment_delete_button) Button deleteButton;

    private boolean isEditing;
    private Pledge pledge;
    private Date selectedDate;
    private String selectedContactId = "";
    private String appealsId;
    private String pledgeId;

    public static AddCommitmentFragment newInstance(String appealId) {
        Bundle args = new Bundle();
        args.putString(ARG_APPEAL_ID, appealId);

        AddCommitmentFragment fragment = new AddCommitmentFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AddCommitmentFragment newInstance(String appealId, String contactId) {
        Bundle args = new Bundle();
        args.putString(ARG_APPEAL_ID, appealId);
        args.putString(ARG_CONTACT_ID, contactId);

        AddCommitmentFragment fragment = new AddCommitmentFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AddCommitmentFragment newInstance(String appealId, Pledge pledge) {
        Bundle args = new Bundle();
        args.putString(ARG_APPEAL_ID, appealId);
        args.putString(ARG_PLEDGE_ID, pledge.getId());

        AddCommitmentFragment fragment = new AddCommitmentFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            pledgeId = getArguments().getString(ARG_PLEDGE_ID);
            appealsId = getArguments().getString(ARG_APPEAL_ID);
            selectedContactId = getArguments().getString(ARG_CONTACT_ID, "");
            try (Realm realm1 = Realm.getDefaultInstance()) {
                pledge = realm1.where(Pledge.class).equalTo("id", pledgeId).findFirst();
                if (pledge != null) {
                    if (pledge.getAmount() != null) {
                        amountText.setText(NumberFormat.getCurrencyInstance(Locale.getDefault()).format(pledge.getAmount()));
                    } else {
                        amountText.setText("");
                    }
                    selectedDate = pledge.getExpectedDate();
                    if (selectedDate != null) {
                        dateText.setText(DateUtilsKt.toLocalDate(selectedDate).format(DateFormatUtilsKt
                                .localizedDateFormatter(FormatStyle.MEDIUM, Locale.getDefault())));
                    }
                    receivedSwitch.setChecked(pledge.isReceived());

                    if (selectedContactId.isEmpty()) {
                        selectedContactId = pledge.getContactId();
                    }

                    isEditing = true;
                }
                Contact contact = realm1.where(Contact.class).equalTo(ContactFields.ID, selectedContactId).findFirst();
                if (contact != null) {
                    contactNameText.setText(contact.getName());
                }
            }
        }

        toolbar.setTitle(isEditing ? R.string.edit_commitment_title : R.string.add_commitment_title);
        deleteButton.setVisibility(isEditing ? View.VISIBLE : View.GONE);

        receivedRow.setOnClickListener(event -> receivedSwitch.toggle());
    }

    @Override
    public void onResume() {
        super.onResume();
        mEventBus.post(new AnalyticsScreenEvent(SCREEN_CONTACTS_EDIT_COMMITMENT));
    }

    private void saveCommitment() {
        if (hasErrors()) {
            return;
        }

        try (Realm realm = Realm.getDefaultInstance()) {
        realm.executeTransactionAsync(realm1 -> {
            Pledge newPledge = new Pledge();

            if (pledge != null) {
                newPledge = realm1.where(Pledge.class).equalTo(PledgeFields.ID, pledgeId).findFirst();
                newPledge.setTrackingChanges(true);
                newPledge.setNew(false);
            } else {
                newPledge.setId(UUID.randomUUID().toString());
                newPledge.setNew(true);
                newPledge.setAccountList(AccountListQueriesKt.getAccountList(realm1, mAppPrefs.getAccountListId()).findFirst());
            }

            final String rawAmount = amountText.getText().toString();
            Double amount = NumberUtilsKt
                    .localizedToDoubleOrNull(rawAmount, LocaleCompat.getDefault(LocaleCompat.Category.FORMAT));
            if (amount == null) {
                amount = StringsKt.toDoubleOrNull(rawAmount);
            }
            newPledge.setAmount(amount);

            newPledge.setExpectedDate(selectedDate);
            newPledge.setReceived(receivedSwitch.isChecked());

            newPledge.setAppeal(AppealQueriesKt.getAppeal(realm1, appealsId).findFirst());
            newPledge.setContact(ContactQueriesKt.getContact(realm1, selectedContactId).findFirst());

            realm1.copyToRealmOrUpdate(newPledge);
            getActivity().setResult(RESULT_COMMITMENT_SAVED);
            getActivity().finish();
        }, mPledgeSyncService.syncDirtyPledges()::launch);
        }

        Toast.makeText(getContext(), getString(R.string.add_commitment_pledge_saved), Toast.LENGTH_LONG).show();
    }

    private boolean hasErrors() {
        if (selectedDate == null) {
            Toast.makeText(getContext(), getString(R.string.add_commitment_error_no_date),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        if (selectedContactId == null) {
            Toast.makeText(getContext(), getString(R.string.add_commitment_error_no_contact),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        if (TextUtils.isEmpty(amountText.getText().toString())) {
            Toast.makeText(getContext(), getString(R.string.add_commitment_error_no_amount),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    @SuppressLint("CheckResult")
    @OnClick(R2.id.fragment_edit_commitment_delete_button)
    public void deleteCommitment() {
        if (pledge == null) {
            return;
        }
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransactionAsync(realm1 -> {
                Pledge pledge = realm1.where(Pledge.class).equalTo(PledgeFields.ID, pledgeId).findFirst();
                pledge.setDeleted(true);

                getActivity().setResult(RESULT_COMMITMENT_DELETED);
                getActivity().finish();
            }, mPledgeSyncService.syncDirtyPledges()::launch);
        }

        Toast.makeText(getContext(), R.string.add_commitment_pledge_delete_failed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.new_commitment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.new_commitment_save) {
            saveCommitment();
            return true;
        } else if (itemId == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R2.id.fragment_add_commitment_date)
    public void commitmentDateClicked() {
        Calendar newDateCalendar = Calendar.getInstance();
        newDateCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        String currentText = dateText.getText().toString();
        if (!TextUtils.isEmpty(currentText)) {
            Date newDate = DateUtils.parseUTCFriendlyDate(currentText);
            if (newDate != null) {
                newDateCalendar.setTime(newDate);
            }
        }

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(getActivity(), (view, year, month, dayOfMonth) -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
                    calendar.set(year, month, dayOfMonth);
                    selectedDate = calendar.getTime();
                    dateText.setText(DateUtilsKt.toLocalDate(selectedDate).format(DateFormatUtilsKt
                            .localizedDateFormatter(FormatStyle.MEDIUM, Locale.getDefault())));
                }, newDateCalendar.get(Calendar.YEAR), newDateCalendar.get(Calendar.MONTH),
                        newDateCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    @OnClick(R2.id.fragment_add_commitment_contact)
    void selectContactClicked() {
        new AskedContactSelectorFragment(appealsId).show(getChildFragmentManager(), R.id.root);
    }

    @Override
    public void onItemSelected(@Nullable final AskedContact askedContact) {
        final Contact contact = askedContact != null ? askedContact.getContact() : null;
        selectedContactId = contact != null ? contact.getId() : null;
        contactNameText.setText(contact != null ? contact.getName() : null);

        if (getActivity() != null) {
            ViewUtilsKt.hideKeyboard(contactNameText);
        }
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_add_commitment;
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

}
