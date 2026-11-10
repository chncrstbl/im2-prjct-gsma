package com.example.priangradingsystem.data;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Age from a stored birthdate. Pure Java. Understands "MM/dd/yyyy" (what the app saves) and "yyyy-MM-dd". */
public final class AgeCalculator {
    private static final Pattern US = Pattern.compile("^\\s*(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s*$");
    private static final Pattern ISO = Pattern.compile("^\\s*(\\d{4})-(\\d{1,2})-(\\d{1,2})\\s*$");

    private AgeCalculator() {}

    /** Whole years on {@code asOf}, or -1 when the text is not a usable date or lies in the future. */
    public static int ageOn(String birthdate, Calendar asOf) {
        if (birthdate == null) return -1;
        int y, m, d;
        Matcher us = US.matcher(birthdate), iso = ISO.matcher(birthdate);
        if (us.matches()) {
            m = Integer.parseInt(us.group(1)); d = Integer.parseInt(us.group(2)); y = Integer.parseInt(us.group(3));
        } else if (iso.matches()) {
            y = Integer.parseInt(iso.group(1)); m = Integer.parseInt(iso.group(2)); d = Integer.parseInt(iso.group(3));
        } else {
            return -1;
        }
        if (m < 1 || m > 12 || d < 1 || d > 31) return -1;
        int ny = asOf.get(Calendar.YEAR), nm = asOf.get(Calendar.MONTH) + 1, nd = asOf.get(Calendar.DAY_OF_MONTH);
        int age = ny - y;
        if (nm < m || (nm == m && nd < d)) age--;
        return age < 0 ? -1 : age;
    }

    public static int ageToday(String birthdate) {
        return ageOn(birthdate, Calendar.getInstance());
    }

    /** True when the birthdate is a real date that is not after {@code asOf}. */
    public static boolean isPlausible(String birthdate, Calendar asOf) {
        int age = ageOn(birthdate, asOf);
        return age >= 0 && age <= 120;
    }
}
