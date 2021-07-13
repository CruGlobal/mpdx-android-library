package org.mpdx.android.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


@SuppressWarnings("HardCodedStringLiteral")
public class NetUtils {

    public enum Languages {
        FRENCH("fr_FR"),
        GERMAN("de"),
        KOREAN("ko"),
        THAI("th"),
        SPANISH("es-419"),
        INDONESIAN("id"),
        ITALIAN("it"),
        ARABIC("ar"),
        TURKISH("tr"),
        RUSSIAN("ru"),
        ENGLISH("en");

        private final  String language;

        Languages(String language) {
            this.language = language;
        }

        public String toString() {
            return language;
        }
    }

    public static String getPreferredLanaguage() {
        String localLanguage = Locale.getDefault().getLanguage();
        if (localLanguage != null && localLanguage.length() > 2) {
            localLanguage = localLanguage.substring(0, 2);
        }
        for (Languages language : Languages.values()) {
            String code = language.toString();
            if (code.startsWith(localLanguage)) {
                return code;
            }
        }

        return Languages.ENGLISH.toString();
    }

    public static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    String addressStr = addr.getHostAddress();
                    boolean isIPv4 = addressStr.indexOf(':') < 0;
                    if (!addr.isLoopbackAddress() && isIPv4) {
                        return addressStr;
                    }
                }
            }
        } catch (Exception ignored) { }
        return "Unavailable";
    }

}
