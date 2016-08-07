package com.zeapo.pwdstore.store;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;

public class StoreManager {
    private Activity activity;
    private SharedPreferences preferences;
    private File activeStore;

    public static final int STORE_TYPE_GIT = 100;
    public static final int STORE_TYPE_NONE = 101;
    public static final int STORE_LOCATION_HIDDEN = 110;
    public static final int STORE_LOCATION_EXTERNAL = 111;

    public StoreManager(Activity activity) {
        this.activity = activity;
        preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
    }

    /**
     * Get the current active store path
     *
     * @return a the path to corresponding to the active Store
     */
    public String getActiveStorePath() {
        return preferences.getString("store_path", activity.getFilesDir().getPath() + "/store");
    }

    /**
     * Get the current active store
     *
     * @return a File corresponding to the active Store
     */
    public File getActiveStore() {
        if (activeStore == null) {
            activeStore = new File(getActiveStorePath());
            // todo remove the next line
            if (!activeStore.exists()) { activeStore.mkdir(); }
        }

        return activeStore;
    }

    /**
     * Get the versioning used for active store
     * The possible values are
     * - STORE_TYPE_GIT for git versioning
     * - STORE_TYPE_NONE for no versioning
     *
     * @return the type of the active store
     */
    public int getActiveStoreVersionning() {
        File activeStore = getActiveStore();
        return getStoreVersionning(activeStore);
    }

    /**
     * Get the versioning used for a store
     * The possible values are
     * - STORE_TYPE_GIT for git versioning
     * - STORE_TYPE_NONE for no versioning
     *
     * @return the type of the given store
     */
    public int getStoreVersionning(File store) {
        if (new File(store, ".git").exists()) {
            return STORE_TYPE_GIT;
        } else {
            return STORE_TYPE_NONE;
        }
    }

    /**
     * Get the location type for active store
     * The possible values are
     * - STORE_LOCATION_HIDDEN for stores in the data folder
     * - STORE_LOCATION_EXTERNAL for stores on the sdcard
     *
     * @return the location type for the active store
     */
    public int getActiveStoreLocation() {
        File activeStore = getActiveStore();
        return getStoreLocation(activeStore);
    }

    /**
     * Get the location type for a given store
     * The possible values are
     * - STORE_LOCATION_HIDDEN for stores in the data folder
     * - STORE_LOCATION_EXTERNAL for stores on the sdcard
     *
     * @return the location type for the given store
     */
    public int getStoreLocation(File store) {
        if (store.getAbsolutePath().startsWith(activity.getFilesDir().getAbsolutePath())) {
            return STORE_LOCATION_HIDDEN;
        } else {
            return STORE_LOCATION_EXTERNAL;
        }
    }
}
