package org.mpdx.android.core.domain;

import java.util.List;

public interface UpdatableAdapter<T> {
    void update(List<T> list);
}
