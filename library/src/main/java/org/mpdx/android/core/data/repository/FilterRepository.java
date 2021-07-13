package org.mpdx.android.core.data.repository;

import android.text.TextUtils;

import com.google.firebase.perf.metrics.AddTrace;

import org.mpdx.android.features.analytics.BaseAnalyticsServiceKt;
import org.mpdx.android.features.contacts.model.Address;
import org.mpdx.android.features.contacts.model.Contact;
import org.mpdx.android.features.contacts.realm.ContactQueriesKt;
import org.mpdx.android.features.filter.FilterConstantsKt;
import org.mpdx.android.features.filter.model.Filter;
import org.mpdx.android.features.filter.model.Filter.Type;
import org.mpdx.android.features.filter.model.FilterFields;
import org.mpdx.android.features.tasks.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.Nullable;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;

@Singleton
public class FilterRepository {
    @Inject
    public FilterRepository() { }

    private List<Contact> filterContacts(List<Contact> contacts, List<Filter> enabledFilters) {
        List<Contact> results = new ArrayList<>();
        for (Contact contact : contacts) {
            final RealmQuery<Address> addressesQuery = contact.getAddresses();
            final RealmResults<Address> addresses = addressesQuery != null ? addressesQuery.findAll() : null;

            boolean includeContact = !FilterConstantsKt.HIDDEN_STATUSES.contains(contact.getStatus());

            Filter.Type lastFilterType = Filter.Type.ACTION_TYPE;
            for (Filter filter : enabledFilters) {
                Filter.Type filterType = filter.getType();
                if (filterType == Filter.Type.ACTION_TYPE) {
                    continue;
                }
                if (filterType == lastFilterType && includeContact) {
                    continue;
                }
                if (filterType != lastFilterType && !includeContact) {
                    break;
                }
                lastFilterType = filterType;

                String key = filter.getKey();
                if (filterType == Type.CONTACT_STATUS) {
                    String status = contact.getStatus();
                    if (FilterConstantsKt.FILTER_CONTACT_STATUS_NONE.equals(key)) {
                        includeContact = TextUtils.isEmpty(status);
                        break;
                    }
                    includeContact = !TextUtils.isEmpty(status) && status.equals(key);
                    break;
                } else if (filterType == Type.CONTACT_REFERRER) {
                    final List<Contact> referrers = contact.getReferredBy();
                    boolean hasReferrer = false;
                    if (referrers != null) {
                        for (final Contact referrer : referrers) {
                            if (filter.getKey().equals(referrer.getId())) {
                                hasReferrer = true;
                                break;
                            }
                        }
                    }
                    includeContact = hasReferrer;
                    break;
                } else if (filterType == Type.CONTACT_CHURCH) {
                    String church = contact.getChurchName();
                    includeContact = !TextUtils.isEmpty(church) && church.equals(key);
                    break;
                } else if (filterType == Type.CONTACT_LIKELY_TO_GIVE) {
                    String likelyToGive = contact.getLikelyToGive();
                    includeContact = !TextUtils.isEmpty(likelyToGive) && likelyToGive.equals(key);
                    break;
                } else if (filterType == Type.CONTACT_TIMEZONE) {
                    String timezone = contact.getTimezone();
                    includeContact = !TextUtils.isEmpty(timezone) && timezone.equals(key);
                    break;
                } else if (filterType == Type.CONTACT_CITY) {
                    boolean hasCity = false;
                    if (addresses != null) {
                        for (Address address : addresses) {
                            if (!TextUtils.isEmpty(address.getCity()) && address.getCity().equals(key)) {
                                hasCity = true;
                                break;
                            }
                        }
                    }
                    includeContact = hasCity;
                    break;
                } else if (filterType == Type.CONTACT_STATE) {
                    boolean hasState = false;
                    if (addresses != null) {
                        for (Address address : addresses) {
                            if (!TextUtils.isEmpty(address.getState()) && address.getState().equals(key)) {
                                hasState = true;
                                break;
                            }
                        }
                    }
                    includeContact = hasState;
                    break;
                } else if (filterType == Type.CONTACT_TAGS) {
                    boolean hasTag = false;
                    RealmList<String> tags = contact.getTags();
                    for (String tag : tags) {
                        if (tag.equals(key)) {
                            hasTag = true;
                            break;
                        }
                    }
                    includeContact = hasTag;
                    break;
                }
            }
            if (includeContact) {
                results.add(contact);
            }
        }
        return results;
    }

    public List<Task> filterTasks(@Nullable final List<Task> tasks) {
        return filterTasks(tasks, -1);
    }

    @AddTrace(name = BaseAnalyticsServiceKt.PERF_FILTER_TASKS)
    public List<Task> filterTasks(@Nullable final List<Task> tasks, int count) {
        // short-circuit if the tasks list is empty
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Filter> enabledFilters = getListOfEnabledTaskFilters();
        if (enabledFilters.isEmpty()) {
            if (count == -1 || tasks.size() <= count) {
                return tasks;
            } else {
                return tasks.subList(0, count);
            }
        } else {
            enabledFilters = sortByType(enabledFilters);
            List<Task> results = new ArrayList<>();
            StringBuffer actionBuffer = new StringBuffer();
            StringBuffer tagBuffer = new StringBuffer();

            Collections.sort(enabledFilters, (f1, f2) -> f1.getType().compareTo(f2.getType()));
            int filterCount = enabledFilters.size();
            int actionFilterCount = 0;
            int tagFilterCount = 0;
            for (Filter filter : enabledFilters) {
                if (filter.getType() == Filter.Type.ACTION_TYPE) {
                    actionBuffer.append(filter.getKey()).append(',');
                    actionFilterCount++;
                } else if (filter.getType() == Filter.Type.TASK_TAGS) {
                    tagBuffer.append(filter.getKey()).append(',');
                    tagFilterCount++;
                }
            }
            String actionStr = null;
            if (actionBuffer.length() > 0) {
                actionStr = actionBuffer.toString();
            }
            String tagStr = null;
            if (tagBuffer.length() > 0) {
                tagStr = tagBuffer.toString();
            }

            for (Task task : tasks) {
                boolean passedActionTest = true;
                boolean passedTagTest = true;
                if (actionStr != null) {
                    passedActionTest = false;
                    String activityActionType = task.getActivityType();
                    if (!TextUtils.isEmpty(activityActionType) && actionStr.contains(activityActionType)) {
                        passedActionTest = true;
                    }
                }
                if (tagStr != null) {
                    passedTagTest = false;
                    for (String tag : task.getTags()) {
                        if (tagStr.contains(tag)) {
                            passedTagTest = true;
                        }
                    }
                }

                List<Contact> contacts = null;
                if (filterCount > actionFilterCount + tagFilterCount) {
                    contacts = filterContacts(getContactsForFilter(task), enabledFilters);
                }

                if (passedActionTest && passedTagTest &&
                        (filterCount == actionFilterCount + tagFilterCount || contacts.size() > 0)) {
                    results.add(task);
                    if (count > 0 && results.size() == count) {
                        return results;
                    }
                }
            }
            return results;
        }
    }

    private List<Filter> getListOfEnabledTaskFilters() {
        try (Realm realm = Realm.getDefaultInstance()) {
            return realm.where(Filter.class)
                    .equalTo(FilterFields.CONTAINER, Filter.CONTAINER_TASK)
                    .equalTo(FilterFields.IS_ENABLED, true).findAll();
        }
    }

    private List<Contact> getContactsForFilter(Task task) {
        try (Realm realm = Realm.getDefaultInstance()) {
            return ContactQueriesKt.forTask(realm.where(Contact.class), task.getId()).findAll();
        }
    }

    private List<Filter> sortByType(List<Filter> realmFilters) {
        List<Filter> filters = new ArrayList<>();
        for (Filter filter : realmFilters) {
            filters.add(filter);
        }
        Collections.sort(filters, (f1, f2) -> f1.getType().compareTo(f2.getType()));

        return filters;
    }
}
