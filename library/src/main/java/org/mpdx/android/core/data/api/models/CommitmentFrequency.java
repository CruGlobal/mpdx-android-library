package org.mpdx.android.core.data.api.models;

import androidx.annotation.NonNull;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class CommitmentFrequency extends RealmObject implements Comparable<CommitmentFrequency> {
    @PrimaryKey
    public String id;

    public String name;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(@NonNull CommitmentFrequency o) {
        return name.compareTo(o.name);
    }
}
