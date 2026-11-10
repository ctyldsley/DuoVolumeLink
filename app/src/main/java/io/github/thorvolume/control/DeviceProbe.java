package io.github.thorvolume.control;

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.Settings;

import java.util.Locale;
import java.util.TreeMap;

/**
 * Collects the device facts needed to support a new dual-screen firmware.
 *
 * <p>The app depends on vendor-defined {@link Settings.System} keys. When they are missing on a
 * device, this lists the similarly named keys that firmware does expose, so the right one can be
 * identified from a copied report instead of an adb session.</p>
 */
final class DeviceProbe {
    private static final String[] HINTS = {
            "volume", "screen", "second", "sub_", "dual", "focus", "display"
    };
    private static final int MAX_VALUE_LENGTH = 48;

    private DeviceProbe() {}

    static String report(Context context) {
        StringBuilder out = new StringBuilder();
        out.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(" (").append(Build.DEVICE).append(")\n");
        out.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" / API ").append(Build.VERSION.SDK_INT).append('\n');
        out.append("Edition: ").append(context.getString(R.string.edition_name))
                .append(", targetSdk ")
                .append(context.getApplicationInfo().targetSdkVersion).append('\n');
        out.append(VolumeControl.SECONDARY_SETTING_KEY).append(": ")
                .append(read(context, VolumeControl.SECONDARY_SETTING_KEY)).append('\n');
        out.append(FocusChangeSetting.KEY).append(": ")
                .append(read(context, FocusChangeSetting.KEY)).append('\n');
        out.append("Related Settings.System keys:\n").append(relatedKeys(context));
        return out.toString();
    }

    private static String read(Context context, String key) {
        try {
            String value = Settings.System.getString(context.getContentResolver(), key);
            return value == null ? "missing" : value;
        } catch (Throwable error) {
            return "error " + error.getClass().getSimpleName();
        }
    }

    private static String relatedKeys(Context context) {
        TreeMap<String, String> matches = new TreeMap<String, String>();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Settings.System.CONTENT_URI,
                    new String[] { "name", "value" }, null, null, null);
            if (cursor == null) return "  (settings query returned nothing)";
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (name == null || !isRelated(name)) continue;
                String value = cursor.getString(1);
                if (value != null && value.length() > MAX_VALUE_LENGTH) {
                    value = value.substring(0, MAX_VALUE_LENGTH) + "…";
                }
                matches.put(name, value);
            }
        } catch (Throwable error) {
            return "  (settings query failed: " + error.getClass().getSimpleName() + ")";
        } finally {
            if (cursor != null) cursor.close();
        }
        if (matches.isEmpty()) return "  (none)";
        StringBuilder out = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : matches.entrySet()) {
            if (out.length() > 0) out.append('\n');
            out.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue());
        }
        return out.toString();
    }

    private static boolean isRelated(String name) {
        String lower = name.toLowerCase(Locale.US);
        for (String hint : HINTS) {
            if (lower.contains(hint)) return true;
        }
        return false;
    }
}
