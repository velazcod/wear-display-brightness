package com.danvelazco.wear.displaybrightness;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import com.danvelazco.wear.displaybrightness.service.ScreenMonitorService;
import com.danvelazco.wear.displaybrightness.shared.BrightnessLevel;
import com.danvelazco.wear.displaybrightness.util.ActivityRecognitionHelper;

/**
 * @author Daniel Velazco <velazcod@gmail.com>
 * @since 9/14/14
 */
public class BrightnessLevelsPreferenceActivity extends Activity {

    // Preference keys
    public final static String KEY_PREF_FILENAME = "pref_brightness_levels";
    public final static String KEY_LEVEL_DRIVING = "level_driving";
    public final static String KEY_LEVEL_ON_BICYCLE = "level_on_bicycle";
    public final static String KEY_LEVEL_WALKING = "level_walking";
    public final static String KEY_LEVEL_RUNNING = "level_running";
    public final static String KEY_LEVEL_STILL = "level_still";
    public final static String KEY_LEVEL_ON_FOOT = "level_on_foot";
    public final static String KEY_LEVEL_UNKNOWN = "level_unknown";
    public final static String KEY_LINK_BRIGHTNESS = "link_brightness";

    // Default values
    public final static int DEFAULT_LEVEL_DRIVING = BrightnessLevel.HIGHEST;
    public final static int DEFAULT_LEVEL_BICYCLE = BrightnessLevel.HIGHEST;
    public final static int DEFAULT_LEVEL_WALKING = BrightnessLevel.HIGHEST;
    public final static int DEFAULT_LEVEL_RUNNING = BrightnessLevel.HIGHEST;
    public final static int DEFAULT_LEVEL_STILL = BrightnessLevel.MEDIUM;
    public final static int DEFAULT_LEVEL_ON_FOOT = BrightnessLevel.HIGHEST;
    public final static int DEFAULT_LEVEL_UNKNOWN = BrightnessLevel.MEDIUM;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_container);

        if (actionBar != null) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                // Based on the design guidelines, app icon should be shown on ActionBar
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE
                        | ActionBar.DISPLAY_SHOW_HOME);
            } else {
                // Except for Android L, where icon shouldn't be shown
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
            }

            actionBar.show();
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, BrightnessLevelsPreferenceFragment.newInstance())
                    .commit();
        }

        // Schedule the activity detection updates
        ActivityRecognitionHelper activityRecognitionHelper = new ActivityRecognitionHelper(this);
        activityRecognitionHelper.scheduleActivityUpdates();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (BuildConfig.DEBUG) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_preference_activity, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_debug:
                startActivity(new Intent(BrightnessLevelsPreferenceActivity.this, DebugActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@link PreferenceFragment} to show the list preferences to allow the user to map the brightness level for each
     * activity
     */
    public static class BrightnessLevelsPreferenceFragment extends PreferenceFragment implements
            Preference.OnPreferenceChangeListener {

        // Preference items
        private CheckBoxPreference mPreferenceLink;
        private ListPreference mPreferenceDriving;
        private ListPreference mPreferenceBicyle;
        private ListPreference mPreferenceWalking;
        private ListPreference mPreferenceRunning;
        private ListPreference mPreferenceStill;
        private ListPreference mPreferenceOnFoot;
        private ListPreference mPreferenceUnknown;

        /**
         * Fragments constructors should be empty, use newInstance to pass arguments and then set these arguments as a
         * bundle to the fragment
         *
         * @return {@link BrightnessLevelsPreferenceFragment}
         */
        public static BrightnessLevelsPreferenceFragment newInstance() {
            return new BrightnessLevelsPreferenceFragment();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(KEY_PREF_FILENAME);
            addPreferencesFromResource(R.xml.pref_brightness_levels);

            mPreferenceDriving = (ListPreference) findPreference(KEY_LEVEL_DRIVING);
            mPreferenceDriving.setOnPreferenceChangeListener(this);

            mPreferenceBicyle = (ListPreference) findPreference(KEY_LEVEL_ON_BICYCLE);
            mPreferenceBicyle.setOnPreferenceChangeListener(this);

            mPreferenceWalking = (ListPreference) findPreference(KEY_LEVEL_WALKING);
            mPreferenceWalking.setOnPreferenceChangeListener(this);

            mPreferenceRunning = (ListPreference) findPreference(KEY_LEVEL_RUNNING);
            mPreferenceRunning.setOnPreferenceChangeListener(this);

            mPreferenceStill = (ListPreference) findPreference(KEY_LEVEL_STILL);
            mPreferenceStill.setOnPreferenceChangeListener(this);

            mPreferenceOnFoot = (ListPreference) findPreference(KEY_LEVEL_ON_FOOT);
            mPreferenceOnFoot.setOnPreferenceChangeListener(this);

            mPreferenceUnknown = (ListPreference) findPreference(KEY_LEVEL_UNKNOWN);
            mPreferenceUnknown.setOnPreferenceChangeListener(this);

            mPreferenceLink = (CheckBoxPreference) findPreference(KEY_LINK_BRIGHTNESS);
            mPreferenceLink.setOnPreferenceChangeListener(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onResume() {
            super.onResume();
            resetSummaries();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case KEY_LINK_BRIGHTNESS:
                    Intent screenMonitorService = new Intent(getActivity(), ScreenMonitorService.class);
                    if ((boolean)newValue){
                        getActivity().startService(screenMonitorService);
                    }else{
                        getActivity().stopService(screenMonitorService);
                    }
                    return true;
                case KEY_LEVEL_DRIVING:
                    setBrightnessLevelPreferenceSummary(mPreferenceDriving, getPreferenceEntryLabel(mPreferenceDriving,
                            (CharSequence) newValue));
                    return true;
                case KEY_LEVEL_ON_BICYCLE:
                    setBrightnessLevelPreferenceSummary(mPreferenceBicyle, getPreferenceEntryLabel(mPreferenceBicyle,
                            (CharSequence) newValue));
                    return true;
                case KEY_LEVEL_WALKING:
                    setBrightnessLevelPreferenceSummary(mPreferenceWalking, getPreferenceEntryLabel(mPreferenceWalking,
                            (CharSequence) newValue));
                    return true;
                case KEY_LEVEL_RUNNING:
                    setBrightnessLevelPreferenceSummary(mPreferenceRunning, getPreferenceEntryLabel(mPreferenceRunning,
                            (CharSequence) newValue));
                    return true;
                case KEY_LEVEL_STILL:
                    setBrightnessLevelPreferenceSummary(mPreferenceStill, getPreferenceEntryLabel(mPreferenceStill,
                            (CharSequence) newValue));
                    return true;
                case KEY_LEVEL_ON_FOOT:
                    setBrightnessLevelPreferenceSummary(mPreferenceOnFoot, getPreferenceEntryLabel(mPreferenceOnFoot,
                            (CharSequence) newValue));
                    return true;
                case KEY_LEVEL_UNKNOWN:
                    setBrightnessLevelPreferenceSummary(mPreferenceUnknown, getPreferenceEntryLabel(mPreferenceUnknown,
                            (CharSequence) newValue));
                    return true;
            }
            return false;
        }

        /**
         * Method to reset the summary in all the {@link ListPreference} items to match the current selected value
         */
        private void resetSummaries() {
            setBrightnessLevelPreferenceSummary(mPreferenceDriving, mPreferenceDriving.getEntry());
            setBrightnessLevelPreferenceSummary(mPreferenceBicyle, mPreferenceBicyle.getEntry());
            setBrightnessLevelPreferenceSummary(mPreferenceWalking, mPreferenceWalking.getEntry());
            setBrightnessLevelPreferenceSummary(mPreferenceRunning, mPreferenceRunning.getEntry());
            setBrightnessLevelPreferenceSummary(mPreferenceStill, mPreferenceStill.getEntry());
            setBrightnessLevelPreferenceSummary(mPreferenceOnFoot, mPreferenceOnFoot.getEntry());
            setBrightnessLevelPreferenceSummary(mPreferenceUnknown, mPreferenceUnknown.getEntry());
        }

        /**
         * Helper method to find the entry label for a new entry value by finding the index of the new value, and
         * selecting the label by using the same index from the entries array. (Size of array must match, but this is
         * already a requirement for the {@linkplain ListFragment ListPreference's} when using {@link
         * ListPreference#setEntries(CharSequence[])} and {@link ListPreference#setEntryValues(CharSequence[])}
         * together.
         *
         * @param preference
         *         {@link ListPreference}
         * @param entryValueSelected
         *         {@link CharSequence}
         * @return {@linkplain CharSequence}
         */
        private CharSequence getPreferenceEntryLabel(ListPreference preference, CharSequence entryValueSelected) {
            CharSequence[] values = preference.getEntryValues();
            CharSequence[] entries = preference.getEntries();
            for (int i = 0; i < values.length; i++) {
                if (entryValueSelected == values[i]) {
                    return entries[i];
                }
            }
            return null;
        }

        /**
         * Set the summary for the preference by formatting the new selected value label
         *
         * @param preference
         *         {@link ListPreference}
         * @param valueLabel
         *         {@link CharSequence}
         */
        private void setBrightnessLevelPreferenceSummary(ListPreference preference, CharSequence valueLabel) {
            if (preference != null) {
                preference.setSummary(String.format(getString(R.string.lbl_activity_preference_summary), valueLabel));
            }
        }

    }

}
