package org.cmuchimps.signifiersio;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final int NEW_EXCEPTION_REQUEST = 1;

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || PolicyPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

        }

    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PolicyPreferenceFragment extends PreferenceFragment {
        private boolean rootAllow;
        private ArrayList<JSONObject> exceptions;

        SharedPreferences sharedPreferences;
        PreferenceCategory exceptionCategory;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_policy);

            // Initialize global variables
            exceptions = new ArrayList<>();
            sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
            exceptionCategory = (PreferenceCategory)findPreference(getString(R.string.exception_category));

            try{
                // Load stored privacy policy from Shared Preferences
                String privacyPolicyJSON = sharedPreferences.getString(getString(R.string.privacy_policy),
                                                                       getString(R.string.default_policy));
                JSONObject privacyPolicy = new JSONObject(privacyPolicyJSON);

                // Parse the privacy policy to set the global variables
                rootAllow = privacyPolicy.getString("rule_type").equals("allow");
                if(privacyPolicy.has("except")){
                    JSONArray es = privacyPolicy.getJSONArray("except");

                    for(int i = 0; i < es.length(); i++){
                        exceptions.add(es.getJSONObject(i));
                    }
                }

            } catch(JSONException e){
                Log.e("onCreate PolicyFragment",e.toString());
                return; // TODO: maybe a toast or something more elegant, but this shouldn't happen
            }

            // Select the correct option in the list
            updateAllowList();

            // Create a Preference from each exception and add it to the view
            for(int i = 0; i < exceptions.size(); i++){
                // TODO: make the order stable by controlling order parameter
                addExceptionPreference(exceptions.get(i),i);
            }


            // Disable saving the root allow type as a preference
            final ListPreference listPreference = (ListPreference)findPreference(getString(R.string.root_allow_preference));
            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    int index = listPreference.findIndexOfValue(value.toString());

                    rootAllow = (index == 0); // Update root allow
                    updateAllowList();        // Update visuals
                    updatePP();               // Save privacy policy

                    // Do not save as preference
                    return false;
                }
            });

            // Add action to "Add Exception" button
            Preference button = findPreference(getString(R.string.exception_button));
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Start an activity that handles making a new exception
                    Intent intent = new Intent(getActivity(), NewException.class);
                    startActivityForResult(intent, NEW_EXCEPTION_REQUEST);
                    return true;
                }
            });
        }

        // Called when the new exception activity finishes
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            // If the NewException activity returned a new exception
            if (requestCode == NEW_EXCEPTION_REQUEST && resultCode == RESULT_OK) {
                try {
                    String exceptionJSON = data.getStringExtra(getString(R.string.exception_json));
                    JSONObject exception = new JSONObject(exceptionJSON);

                    // Add the new exception to our list
                    int index = exceptions.size();
                    exceptions.add(exception);

                    // Create a Preference to display and allow editing of the exception
                    addExceptionPreference(exception, index);

                    // Save the new policy
                    updatePP();
                } catch(JSONException e) {
                    Log.d("onActivityResult",e.toString());
                }
            }
        }

        // Set the root rule type based on rootAllow
        private void updateAllowList(){
            ListPreference listPreference = (ListPreference)findPreference(getString(R.string.root_allow_preference));
            listPreference.setValueIndex(rootAllow ? 0 : 1);
        }

        private void addExceptionPreference(JSONObject exception, int order){
            Preference p = new Preference(getContext());
            p.setTitle(summarizeException(exception));
            p.setOrder(order);
            // TODO: set onclick to handle edits

            // Add this Preference to the view
            exceptionCategory.addPreference(p);
        }

        // Create a short and meaningful description of the exception
        private String summarizeException(JSONObject exception) {
            // TODO: do this
            return exception.toString();
        }

        // Rebuild and update stored privacy policy
        private void updatePP(){
            try {
                // Create the privacy policy JSON
                JSONObject newPrivacyPolicy = new JSONObject();
                newPrivacyPolicy.put("rule_type", rootAllow ? "allow" : "disallow");
                newPrivacyPolicy.put("except",new JSONArray(exceptions));

                // Save it to SharedPreferences
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putString(getString(R.string.privacy_policy), newPrivacyPolicy.toString());
                sharedPreferencesEditor.apply();
                // TODO: propagate the new privacy policy
            } catch(JSONException e){
                Log.e("updatePP",e.toString());
                // TODO: make a toast to let user know it didn't save, even though this should never happen
            }
        }


    }

}
