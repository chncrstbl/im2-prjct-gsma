package com.example.priangradingsystem.data;

/**
 * Which school year the screens are showing. Every list, grade entry, record and report screen reads
 * this through {@link DatabaseHelper}, so changing it once switches the whole app. It is kept in memory
 * only: every time the app starts it is empty, which means "the current school year".
 */
public final class SchoolYearContext {
    private static volatile long selectedId = -1;

    private SchoolYearContext() {}

    public static long getSelectedId() { return selectedId; }

    public static void setSelectedId(long id) { selectedId = id; }

    public static void clear() { selectedId = -1; }
}
