package org.mpdx.android.features.contacts;

import org.mpdx.android.features.contacts.model.Contact;
import org.mpdx.android.features.contacts.repository.ContactsRepository;

import javax.inject.Inject;

public class ContactsPresenter {
    private final ContactsRepository mContactsRepository;

    @Inject
    public ContactsPresenter(final ContactsRepository contactsRepository) {
        mContactsRepository = contactsRepository;
    }

    public void onContactStarClicked(Contact contact) {
        if (contact != null) {
            mContactsRepository.toggleStarred(contact.getId());
        }
    }
}
