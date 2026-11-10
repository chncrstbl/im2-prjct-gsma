package com.example.priangradingsystem.data;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One school year, e.g. "2026-2027". Pure Java: no Android classes. */
public final class SchoolYear {
    private static final Pattern FULL = Pattern.compile("^(\\d{4})-(\\d{4})$");
    private static final Pattern SHORT = Pattern.compile("^(\\d{4})-(\\d{2})$");

    public final long id;
    public final String label;
    public final boolean current;

    public SchoolYear(long id, String label, boolean current) {
        this.id = id;
        this.label = label;
        this.current = current;
    }

    @Override public String toString() { return current ? label + " (current)" : label; }

    /**
     * Cleans typed input into "YYYY-YYYY": trims, accepts en/em dashes or "to", and expands "2026-27".
     * Returns the cleaned text even when it is not valid; call {@link #isValidLabel} afterwards.
     */
    public static String normalizeLabel(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replace('–', '-').replace('—', '-').replace('/', '-')
                .replaceAll("(?i)\\s*to\\s*", "-").replaceAll("\\s+", "");
        Matcher m = SHORT.matcher(s);
        if (m.matches()) {
            int start = Integer.parseInt(m.group(1));
            String century = String.valueOf(start / 100);
            return m.group(1) + "-" + century + m.group(2);
        }
        return s;
    }

    /** A valid label is two consecutive four-digit years: "2026-2027". */
    public static boolean isValidLabel(String label) {
        if (label == null) return false;
        Matcher m = FULL.matcher(label);
        if (!m.matches()) return false;
        int a = Integer.parseInt(m.group(1)), b = Integer.parseInt(m.group(2));
        return b == a + 1 && a >= 2000 && a <= 2199;
    }

    public static int startYear(String label) {
        Matcher m = FULL.matcher(label == null ? "" : label);
        return m.matches() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** "2026-2027" -> "2027-2028". Returns "" for an invalid label. */
    public static String next(String label) {
        int a = startYear(label);
        return a < 0 ? "" : (a + 1) + "-" + (a + 2);
    }

    /**
     * The school year a date belongs to. School years start in June (Philippine school calendar):
     * June 2026 to May 2027 is "2026-2027".
     * @param month 1-12
     */
    public static String labelFor(int year, int month) {
        int start = month >= 6 ? year : year - 1;
        return start + "-" + (start + 1);
    }

    public static String labelFor(Calendar c) {
        return labelFor(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }
}
