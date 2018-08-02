package org.cmuchimps.signifiersio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

public class RootPolicyActivity extends PolicyActivity {
    private boolean rootAllow;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try{
            // Load stored privacy policy from Shared Preferences
            String privacyPolicyJSON = sharedPreferences.getString(getString(R.string.privacy_policy),
                    getString(R.string.default_policy));
            JSONObject privacyPolicy = new JSONObject(privacyPolicyJSON);

            // Parse the privacy policy to set the global variables
            rootAllow = privacyPolicy.getString("rule_type").equalsIgnoreCase("allow");
            if (privacyPolicy.has("except")) {
                JSONArray es = privacyPolicy.getJSONArray("except");

                // Create a new view from each exception and add it to the list
                for (int i = 0; i < es.length(); i++) {
                    // TODO: make the order stable
                    addException(es.getJSONObject(i));
                }
            }

        } catch(JSONException e){
            Log.e("onCreate PolicyFragment",e.toString());
            return; // TODO: maybe a toast or something more elegant, but this shouldn't happen
        }

        // Make the radio group visible and select the correct one
        final RadioGroup allowGroup = findViewById(R.id.root_allow_group);
        allowGroup.check(rootAllow ? R.id.root_allow : R.id.root_disallow);
        allowGroup.setVisibility(View.VISIBLE);

        // Hide the unnecessary components
        int[] ids = {R.id.datatype_layout,R.id.device_name_layout,R.id.company_layout,R.id.purpose_layout,R.id.status_layout};
        for(int id : ids){
            findViewById(id).setVisibility(View.GONE);
        }

        // Create listener for radio group
        allowGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                rootAllow = (id == R.id.root_allow); // Update root allow
                updatePP();           // Save privacy policy
            }
        });
    }

    // Called when the new exception activity finishes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Let super handle most things
        super.onActivityResult(requestCode, resultCode, data);

        // If the NewException activity returned a new exception, save the policy
        if (resultCode == RESULT_OK){
            updatePP();
        }
    }

    // Rebuild and update stored privacy policy
    private void updatePP(){
        try {
            // Create the privacy policy JSON
            JSONObject newPrivacyPolicy = new JSONObject();
            newPrivacyPolicy.put("rule_type", rootAllow ? "allow" : "disallow");

            // Build a JSONArray of exceptions from the LinearLayout of exceptions
            JSONArray exceptions = new JSONArray();
            for(int i = 0; i < exceptionList.getChildCount(); i++){
                exceptions.put(((PolicyException)exceptionList.getChildAt(i)).getExceptionJSON());
            }
            newPrivacyPolicy.put("except",exceptions);

            // Save it to SharedPreferences
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putString(getString(R.string.privacy_policy), newPrivacyPolicy.toString());
            sharedPreferencesEditor.apply();

            // Propagate the new privacy policy:
            // PrivacyParser needs the new policy
            PrivacyParser.loadPP(this);
            // All the devices need to be reevaluated by PrivacyParser
            Set<Device> devices = DeviceDetector.getDevices();
            for(Device d : devices){
                d.updateViolation();
            }
            // MainActivity needs to rebuild the view to show correct alerts
            DeviceDetector.listener.onDeviceUpdate(devices, devices);
            // (okay that's a slightly hacky way to do this, but it does have the virtue of
            // not holding an extra reference to our MainActivity)

        } catch(JSONException e){
            Log.e("updatePP",e.toString());
            // TODO: make a toast to let user know it didn't save, even though this should never happen
        }
    }

}
