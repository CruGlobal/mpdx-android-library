package org.mpdx.android.features.contacts.contactdetail.info;

import org.greenrobot.eventbus.EventBus;
import org.mpdx.android.R;
import org.mpdx.android.features.analytics.model.IconCallAnalyticsEvent;
import org.mpdx.android.features.analytics.model.IconTextAnalyticsEvent;
import org.mpdx.android.features.base.ViewModel;
import org.mpdx.android.features.contacts.model.Contact;
import org.mpdx.android.features.contacts.model.DonorAccount;
import org.mpdx.android.features.contacts.model.DonorAccountFields;
import org.mpdx.android.features.contacts.model.Person;
import org.mpdx.android.features.contacts.model.PhoneNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class ContactInfoViewModel extends BaseObservable implements ViewModel<Contact> {
    private Contact contact;
    @Nullable
    private RealmResults<Person> people;

    private ContactInfoViewListener listener;

    public ContactInfoViewModel(ContactInfoViewListener listener) {
        this.listener = listener;
    }

    @Override
    public void update(Contact data) {
        this.contact = RealmObject.isValid(data) ? data : null;
        notifyChange();
    }

    public void setPeople(final RealmResults<Person> people) {
        this.people = people;
        notifyChange();
    }

    public String getDonorAccountNumbers() {
        if (contact == null || contact.getDonorAccountIds() == null || !contact.getDonorAccountIds().isValid()) {
            return "";
        }

        String acctNumber = "";
        try (Realm realm = Realm.getDefaultInstance()) {
            for (String donorAccountId : contact.getDonorAccountIds()) {
                if (donorAccountId != null) {
                    DonorAccount accounts = realm.where(DonorAccount.class)
                            .equalTo(DonorAccountFields.ID, donorAccountId).findFirst();
                    if (accounts != null) {
                        acctNumber = String.format("%s %s", acctNumber, accounts.getAccountNumber());
                    }
                }
            }
        }
        return acctNumber;
    }

    public void onPhoneCallClicked() {
        List<PhoneNumber> numbers = getUniquePhoneNumbers();
        if (numbers.isEmpty()) {
            listener.showMessageDialog(R.string.contact_action_no_phone);
        } else {
            listener.openPhoneCallApp(numbers);
            EventBus.getDefault().post(IconCallAnalyticsEvent.INSTANCE);
        }
    }

    public void onTextMessageClicked() {
        List<PhoneNumber> numbers = getUniquePhoneNumbers();
        if (numbers.isEmpty()) {
            listener.showMessageDialog(R.string.contact_action_no_phone);
        } else {
            listener.openTextMessageApp(numbers);
            EventBus.getDefault().post(IconTextAnalyticsEvent.INSTANCE);
        }
    }

    private List<PhoneNumber> getUniquePhoneNumbers() {
        List<PhoneNumber> numbers = new ArrayList<>();

        if (people != null) {
            for (Person person : people) {
                final RealmQuery<PhoneNumber> query = person.getPhoneNumbers(false);
                final List<PhoneNumber> phoneNumbers = query != null ? query.findAll() : Collections.emptyList();
                if (!phoneNumbers.isEmpty()) {
                    boolean exists = false;
                    PhoneNumber entry = phoneNumbers.get(0);
                    if (entry != null && entry.getNumber() != null) {
                        for (PhoneNumber next : numbers) {
                            if (next.getId().equals(entry.getId())) {
                                exists = true;
                            }
                        }
                        if (!exists) {
                            numbers.add(entry);
                        }
                    }
                }
            }
        }
        return numbers;
    }
}
