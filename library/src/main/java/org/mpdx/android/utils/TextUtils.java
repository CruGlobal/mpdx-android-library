package org.mpdx.android.utils;

public class TextUtils {
    public static int compare(String left, String right) {
        if (left == null && right == null) {
            return 0;
        } else if (left == null) {
            return -1;
        } else if (right == null) {
            return 1;
        } else {
            return left.compareTo(right);
        }
    }
}
