package org.mpdx.android.features.donations.add;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.ccci.gto.android.common.util.NumberUtils;
import org.mpdx.android.R;
import org.mpdx.android.R2;
import org.mpdx.android.core.realm.AccountListQueriesKt;
import org.mpdx.android.features.appeals.realm.AppealQueriesKt;
import org.mpdx.android.features.donations.realm.DesignationAccountQueriesKt;
import org.mpdx.android.features.donations.realm.DonationQueriesKt;
import org.mpdx.android.utils.DateFormatUtilsKt;
import org.mpdx.android.utils.DateUtils;
import org.mpdx.android.utils.DateUtilsKt;
import org.mpdx.android.utils.ViewUtilsKt;
import org.mpdx.android.core.data.api.models.CRUCurrency;
import org.mpdx.android.core.modal.ModalFragment;
import org.mpdx.android.features.AppPrefs;
import org.mpdx.android.features.analytics.model.AnalyticsScreenEvent;
import org.mpdx.android.features.appeals.model.Appeal;
import org.mpdx.android.features.base.fragments.BaseFragment;
import org.mpdx.android.features.contacts.model.Contact;
import org.mpdx.android.features.contacts.model.DonorAccount;
import org.mpdx.android.features.donations.list.DonationsFragment;
import org.mpdx.android.features.donations.model.DesignationAccount;
import org.mpdx.android.features.donations.model.Donation;
import org.mpdx.android.features.donations.sync.DonationsSyncService;
import org.mpdx.android.features.selector.BaseSelectorFragment;
import org.threeten.bp.format.FormatStyle;

import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import butterknife.OnClick;
import dagger.hilt.android.AndroidEntryPoint;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import kotlin.text.StringsKt;
import timber.log.Timber;

import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_DONATIONS_VIEW_EDIT;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_DONATIONS_VIEW_EDIT_APPEALS;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_DONATIONS_VIEW_EDIT_CURRENCY;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_DONATIONS_VIEW_EDIT_DESIGNATION_ACCOUNT;
import static org.mpdx.android.features.analytics.model.AnalyticsScreenEventKt.SCREEN_DONATIONS_VIEW_EDIT_PARTNER_ACCOUNTS;

@AndroidEntryPoint
public class AddDonationFragment extends BaseFragment
        implements ModalFragment, OnAppealSelectedListener, OnCurrencySelectedListener, OnDesignationSelectedListener,
        OnDonorSelectedListener {
    public static final String ARG_CURRENCY = "currency";
    public static final String ARG_PARTNER_ACCOUNT_ID = "partner_account";
    public static final String ARG_DESIGNATION_ACCOUNT_ID = "designation_account";
    public static final String ARG_DONATION_ID = "donation_id";

    @BindView(R2.id.fragment_add_donation_toolbar) Toolbar toolbar;
    @BindView(R2.id.donation_add_donation_details_amount) EditText amountText;
    @BindView(R2.id.donation_add_donation_currency) TextView currencyText;
    @BindView(R2.id.donation_add_donation_select_donor) TextView selectDonorText;
    @BindView(R2.id.donation_add_donation_date) TextView dateText;
    @BindView(R2.id.donation_add_donation_appeal) TextView appealText;
    @BindView(R2.id.donation_add_donation_appeal_amount) EditText appealAmountText;
    @BindView(R2.id.donation_add_donation_designation) TextView designationText;
    @BindView(R2.id.donation_add_donation_memo_text) EditText memoText;

    @Inject AppPrefs appPrefs;
    @Inject DonationsSyncService mDonationsSyncService;
    private Donation existingDonation;
    private String selectedCurrency;
    private Date selectedDate;
    private String selectedAppealId;
    private String selectedDonorId;
    private String selectedDesignationId;
    private boolean isPartnerAccount = false;
    private boolean isDesignationAccount = false;

    public static AddDonationFragment newInstance() {
        return new AddDonationFragment();
    }

    public static AddDonationFragment newInstance(Contact contact) {
        Bundle args = new Bundle();
        args.putString(ARG_CURRENCY, contact.getPledgeCurrency());

        RealmList<String> accountIds = contact.getDonorAccountIds();
        if (accountIds != null && accountIds.size() == 1 && accountIds.get(0) != null) {
            args.putString(ARG_PARTNER_ACCOUNT_ID, accountIds.get(0));
        }

        String designationId = contact.getLastDesignationId();
        if (designationId != null) {
            args.putString(ARG_DESIGNATION_ACCOUNT_ID, designationId);
        }

        AddDonationFragment fragment = new AddDonationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AddDonationFragment newInstance(String donationId) {

        Bundle args = new Bundle();
        args.putString(ARG_DONATION_ID, donationId);

        AddDonationFragment fragment = new AddDonationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // region Lifecycle

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setupDataModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isDesignationAccount) {
            mEventBus.post(new AnalyticsScreenEvent(SCREEN_DONATIONS_VIEW_EDIT_DESIGNATION_ACCOUNT));
        } else if (isPartnerAccount) {
            mEventBus.post(new AnalyticsScreenEvent(SCREEN_DONATIONS_VIEW_EDIT_PARTNER_ACCOUNTS));
        } else {
            mEventBus.post(new AnalyticsScreenEvent(SCREEN_DONATIONS_VIEW_EDIT));
        }
    }

    @SuppressLint("CheckResult")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try (Realm realm = Realm.getDefaultInstance()) {
            if (getArguments() != null) {
                if (getArguments().getString(ARG_DONATION_ID) != null) {
                    toolbar.setTitle(R.string.donation_edit_title);

                    Donation donation = DonationQueriesKt.getDonation(realm, getArguments()
                            .getString(ARG_DONATION_ID)).findFirst();
                    if (donation == null) {
                        return;
                    }
                    existingDonation = donation;
                    amountText.setText(donation.getAmount() != null ?
                                               String.format(Locale.getDefault(), "%.2f", donation.getAmount()) : null);
                    selectedCurrency = donation.getCurrency();
                    if (selectedCurrency != null) {
                        currencyText.setText(getString(R.string.donation_add_donation_details_currency, selectedCurrency));
                    }
                    selectedDate = donation.getDonationDate();
                    if (selectedDate != null) {
                        dateText.setText(DateUtilsKt.toLocalDate(selectedDate).format(DateFormatUtilsKt
                                .localizedDateFormatter(FormatStyle.MEDIUM, Locale.getDefault())));
                    }
                    selectedAppealId = donation.getAppealId();
                    Appeal selectedAppeal;
                    selectedAppeal = AppealQueriesKt.getAppeal(realm, selectedAppealId).findFirst();

                    if (selectedAppeal != null) {
                        appealText.setText(selectedAppeal.getName());
                        appealAmountText.setText(donation.getAppealAmount());
                    }

                    DonorAccount selectedDonor = donation.getDonorAccount();
                    selectedDonorId = selectedDonor != null ? selectedDonor.getId() : null;
                    selectDonorText.setText(selectedDonor != null ? selectedDonor.getDisplayName() : null);

                    selectedDesignationId = donation.getDesignationId();
                    DesignationAccount selectedDesignation =
                            DesignationAccountQueriesKt.getDesignationAccount(realm, selectedDesignationId).findFirst();
                    if (selectedDesignation != null) {
                        designationText.setText(selectedDesignation.getDisplayName());
                    }
                    memoText.setText(donation.getMemo());
                } else {
                    if (getArguments().getString(ARG_CURRENCY) != null) {
                        selectedCurrency = getArguments().getString(ARG_CURRENCY);
                    }

                    if (getArguments().getString(ARG_PARTNER_ACCOUNT_ID) != null) {
                        isPartnerAccount = true;
                        selectedDonorId = getArguments().getString(ARG_PARTNER_ACCOUNT_ID);
                        DonorAccount selectedDonor =
                                AccountListQueriesKt.getDonorAccount(realm, selectedDonorId).findFirst();
                        if (selectedDonor != null) {
                            selectDonorText.setText(selectedDonor.getDisplayName());
                        }
                    }

                    if (getArguments().getString(ARG_DESIGNATION_ACCOUNT_ID) != null) {
                        isDesignationAccount = true;
                        selectedDesignationId = getArguments().getString(ARG_DESIGNATION_ACCOUNT_ID);
                        DesignationAccount selectedDesignation =
                                DesignationAccountQueriesKt.getDesignationAccount(realm, selectedDesignationId)
                                        .findFirst();
                        if (selectedDesignation != null) {
                            designationText.setText(selectedDesignation.getDisplayName());
                        }
                    }
                }
            }
            if (selectedDesignationId == null) {
                final RealmResults<DesignationAccount> accounts = mDataModel.getDesignationAccounts().getValue();
                if (accounts != null) {
                    accounts.load();
                    DesignationAccount selectedDesignation = accounts.first(null);

                    if (selectedDesignation != null) {
                        designationText.setText(selectedDesignation.getDisplayName());
                        selectedDesignationId = selectedDesignation.getId();
                    }
                }
            }
            if (selectedCurrency == null) {
                Currency currency = Currency.getInstance(Locale.getDefault());
                selectedCurrency = currency == null ? "USD" : currency.getCurrencyCode();
            }
            currencyText.setText(getString(R.string.donation_add_donation_details_currency, selectedCurrency));
        }
    }

    // endregion Lifecycle

    // region Data Model

    private AddDonationFragmentDataModel mDataModel;

    private void setupDataModel() {
        mDataModel = new ViewModelProvider(this).get(AddDonationFragmentDataModel.class);
    }

    // endregion Data Model

    @OnClick(R2.id.donation_add_donation_date)
    public void donationDateClicked() {
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

    @OnClick(R2.id.donation_add_donation_appeal)
    void appealClicked() {
        showSelectorFragment(new AppealSelectorFragment());
        mEventBus.post(new AnalyticsScreenEvent(SCREEN_DONATIONS_VIEW_EDIT_APPEALS));
    }

    @OnClick(R2.id.donation_add_donation_currency)
    void currencyClicked() {
        showSelectorFragment(new CurrencySelectorFragment());
        mEventBus.post(new AnalyticsScreenEvent(SCREEN_DONATIONS_VIEW_EDIT_CURRENCY));
    }

    @OnClick(R2.id.donation_add_donation_designation)
    void designationClicked() {
        showSelectorFragment(new DesignationSelectorFragment());
    }

    @OnClick(R2.id.donation_add_donation_select_donor)
    void selectDonorClicked() {
        showSelectorFragment(new DonorSelectorFragment());
    }

    private void showSelectorFragment(@NonNull final BaseSelectorFragment fragment) {
        fragment.show(getChildFragmentManager(), R.id.root);
    }

    @Override
    public void onAppealSelected(@Nullable final Appeal appeal) {
        selectedAppealId = appeal != null ? appeal.getId() : null;
        appealText.setText(appeal != null ? appeal.getName() : null);

        if (getActivity() != null) {
            ViewUtilsKt.hideKeyboard(appealText);
        }
    }

    @Override
    public void onCurrencyAccountSelected(@Nullable final CRUCurrency currency) {
        selectedCurrency = currency == null ? "USD" : currency.getCode();
        currencyText.setText(currency != null ? currency.getCodeSymbolString() : null);

        if (getActivity() != null) {
            ViewUtilsKt.hideKeyboard(currencyText);
        }
    }

    @Override
    public void onDesignationAccountSelected(@Nullable final DesignationAccount designation) {
        selectedDesignationId = designation != null ? designation.getId() : null;
        designationText.setText(designation != null ? designation.getDisplayName() : null);

        if (getActivity() != null) {
            ViewUtilsKt.hideKeyboard(designationText);
        }
    }

    @Override
    public void onDonorSelected(@Nullable final DonorAccount partner) {
        selectedDonorId = partner != null ? partner.getId() : null;
        selectDonorText.setText(partner != null ? partner.getDisplayName() : null);

        if (getActivity() != null) {
            ViewUtilsKt.hideKeyboard(selectDonorText);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.new_donation_menu, menu);
        if (existingDonation != null) {
            menu.findItem(R.id.new_donation_add).setTitle(R.string.save);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.new_donation_add) {
            addDonation(item);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void addDonation(MenuItem item) {
        if (hasErrors()) {
            return;
        }
        item.setEnabled(false);

        // build the create or update transaction
        final Realm.Transaction transaction;
        if (existingDonation == null) {
            transaction = realm -> {
                // create donation object
                final Donation donation = new Donation();
                donation.setId(UUID.randomUUID().toString());
                donation.setNew(true);
                donation.setAccountList(
                        AccountListQueriesKt.getAccountList(realm, appPrefs.getAccountListId()).findFirst());
                populateDonationFields(donation, realm);

                // store the donation object in realm
                realm.copyToRealm(donation);
            };
        } else {
            final String id = existingDonation.getId();
            transaction = realm -> {
                final Donation donation = DonationQueriesKt.getDonation(realm, id).findFirst();
                if (donation != null) {
                    donation.setTrackingChanges(true);
                    populateDonationFields(donation, realm);
                    donation.setTrackingChanges(false);
                }
            };
        }

        // execute the transaction
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(
                transaction,
                () -> {
                    // sync any pending dirty donations
                    mDonationsSyncService.syncDirtyDonations().launch();

                    // finish this task
                    Toast.makeText(getContext(), getString(R.string.donation_saved), Toast.LENGTH_SHORT).show();
                    final Activity activity = getActivity();
                    if (activity != null) {
                        activity.setResult(DonationsFragment.RESULT_DONATION_ADDED);
                        activity.finish();
                    }
                },
                error -> {
                    Timber.tag("AddDonationFragment").e(error, "Donation save failed");
                    Toast.makeText(getContext(), getString(R.string.add_donation_error), Toast.LENGTH_SHORT).show();
                    final Activity activity = getActivity();
                    if (activity != null) {
                        activity.setResult(DonationsFragment.RESULT_DONATION_ADDED);
                        activity.finish();
                    }
                });
        realm.close();
    }

    private void populateDonationFields(@NonNull final Donation donation, @NonNull final Realm realm) {
        donation.setAmount(StringsKt.toDoubleOrNull(amountText.getText().toString()));
        donation.setDonationDate(selectedDate);
        donation.setMotivation("");
        donation.setCurrency(selectedCurrency);
        donation.setMemo(memoText.getText().toString());
        donation.setAppealId(selectedAppealId);
        donation.setAppealAmount(appealAmountText.getText().toString());

        donation.setDonorAccount(AccountListQueriesKt.getDonorAccount(realm, selectedDonorId).findFirst());
        donation.setDesignationId(selectedDesignationId);
    }

    protected boolean hasErrors() {
        if (NumberUtils.toDouble(amountText.getText().toString(), 0.0) < 0.001) {
            Toast.makeText(getContext(), getString(R.string.add_donation_no_amount),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        if (selectedCurrency == null) {
            Toast.makeText(getContext(), getString(R.string.add_donation_no_currency),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        if (selectedDate == null) {
            Toast.makeText(getContext(), getString(R.string.add_donation_no_date),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        if (selectedDonorId == null) {
            Toast.makeText(getContext(), getString(R.string.add_donation_no_donor),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        if (selectedDesignationId == null) {
            Toast.makeText(getContext(), getString(R.string.add_donation_no_designation),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    @Override
    protected int layoutRes() {
        return R.layout.fragment_add_donation;
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }
}
