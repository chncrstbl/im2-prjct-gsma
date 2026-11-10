package com.example.priangradingsystem.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Splits a saved full name into first / middle / last / extension. Pure Java. */
public final class NameSplitter {
    private static final Set<String> SUFFIXES = new HashSet<>(Arrays.asList(
            "jr", "jr.", "sr", "sr.", "ii", "iii", "iv", "v"));

    private NameSplitter() {}

    /** @return {first, middle, last, extension}; never null, parts may be "". */
    public static String[] split(String full) {
        String[] out = {"", "", "", ""};
        if (full == null) return out;
        String[] t = full.trim().split("\\s+");
        if (t.length == 1 && t[0].isEmpty()) return out;
        int n = t.length;
        if (n > 1 && SUFFIXES.contains(t[n - 1].toLowerCase(Locale.ROOT))) {
            out[3] = t[n - 1];
            n--;
        }
        if (n == 1) {
            out[0] = t[0];
        } else if (n == 2) {
            out[0] = t[0];
            out[2] = t[1];
        } else {
            out[0] = t[0];
            out[2] = t[n - 1];
            out[1] = String.join(" ", Arrays.copyOfRange(t, 1, n - 1));
        }
        return out;
    }

    /** Joins the parts back into one display name, skipping blanks. */
    public static String join(String first, String middle, String last, String extension) {
        StringBuilder sb = new StringBuilder();
        for (String p : new String[]{first, middle, last, extension}) {
            if (p != null && !p.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }
}
