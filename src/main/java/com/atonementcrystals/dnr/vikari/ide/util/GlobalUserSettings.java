package com.atonementcrystals.dnr.vikari.ide.util;

import java.util.prefs.Preferences;

public class GlobalUserSettings {
    private static final Preferences preferences = Preferences.userRoot().node(GlobalUserSettings.class.getName());

    public static final String USER_HOME = System.getProperty("user.home");
    public static final String LAST_VIEWED_DIRECTORY_KEY = "lastViewedDirectory";

    public static String getLastViewedDirectory() {
        return preferences.get(LAST_VIEWED_DIRECTORY_KEY, USER_HOME);
    }

    public static void setLastViewedDirectory(String lastViewedDirectory) {
        preferences.put(LAST_VIEWED_DIRECTORY_KEY, lastViewedDirectory);
    }
}
