package org.mpdx.android.core.data.typeadapter;

import java.util.Map;

public class StringMapWrapper {

    Map<String, String> map;

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public String get(String key) {
        if (map == null) {
            return null;
        }
        return map.get(key);
    }
}
