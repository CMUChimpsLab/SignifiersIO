package org.cmuchimps.signifiersio;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class PrivacyParser {
    // TODO: handle multiple purposes/data_types
    private static JSONObject policy;

    private static final String ex_pp = "{" +
            "'rule_type': 'allow'," +
            "'except': [" +
            "{" +
            "'rule_type': 'disallow'," +
            "'company': 'Phishing4Less'" +
            "}," +
            "{" +
            "'rule_type': 'disallow'," +
            "'company': 'Google'," +
            "'purpose': 'advertising'" +
            "}," +
            "{" +
            "'rule_type': 'disallow'," +
            "'data_type': 'video'," +
            "'except': [" +
            "{" +
            "'rule_type': 'allow'," +
            "'purpose': 'communication'" +
            "}," +
            "{" +
            "'rule_type': 'allow'," +
            "'purpose': 'security'" +
            "}" +
            "]" +
            "}" +
            "]" +
            "}";

    // Loads privacy preferences from shared preferences
    public static void loadPP(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        try{
            policy = new JSONObject(sharedPreferences.getString(context.getResources().getString(R.string.privacy_policy), "{'rule_type':'allow'}"));
        } catch(JSONException e){
            Log.e("Error loading PP", e.toString());
        }

        //assert(isValid(policy));
    }

    // Return the opposite rule_type
    private static String flip(String rule_type) throws JSONException {
        if (rule_type.equalsIgnoreCase("allow")) {
            return "disallow";
        } else if (rule_type.equalsIgnoreCase("disallow")) {
            return "allow";
        } else {
            throw new JSONException("Bad rule_type");
        }
    }

    // Checks whether  a rule (and all its exceptions) is valid
    private static boolean ruleValid(JSONObject rule, String rule_type, boolean pedantic)
            throws JSONException {

        // If present, rule_type must match expectation
        if (rule.has("rule_type") && !rule.getString("rule_type").equalsIgnoreCase(rule_type)) {
            return false;
        }

        if (pedantic) {
            // Check for empty except lists
            if (rule.has("except") && rule.getJSONArray("except").length() == 0) {
                return false;
            }

            // Check for empty objects
            if (rule.length() == 0) {
                return false;
            }
        }

        // Check that all subrules are valid
        if (rule.has("except")) {
            JSONArray es = rule.getJSONArray("except");

            for (int i = 0; i < es.length(); i++) {
                if (!ruleValid(es.getJSONObject(i), flip(rule_type), pedantic)) {
                    return false;
                }
            }
        }

        return true;
    }

    // Checks whether the privacy preference is valid
    // If pedantic is true, disallows extraneous properties and
    //   empty rule objects and except lists
    private static boolean isValid(JSONObject pp, boolean pedantic) {
        try {
            // rule_type is required for top-level
            if (!pp.has("rule_type") || !(pp.getString("rule_type").equalsIgnoreCase("allow") ||
                    pp.getString("rule_type").equalsIgnoreCase("disallow"))) {
                return false;
            }

            // Check for extraneous keys
            if (pedantic) {
                if (pp.length() > (pp.has("except") ? 2 : 1)) {
                    return false;
                }
            }

            return ruleValid(pp, pp.getString("rule_type"), pedantic);
        } catch(JSONException e){
            Log.e("isValid invalid JSON", e.toString());
            return false;
        }
    }

    private static boolean isValid(JSONObject pp) {
        return isValid(pp, false);
    }

    // Determines whether device matches rule.
    // Ignores rule_type (i.e. doesn't check whether device is allowed)
    private static boolean ruleMatch(JSONObject rule, Device device)
            throws JSONException {
        Iterator<String> keys = rule.keys();
        while (keys.hasNext()) {
            String k = keys.next();

            if (k.equalsIgnoreCase("rule_type") || k.equalsIgnoreCase("except")) {
                continue;
            }

            // If device doesn't have property k or it doesn't match,
            // device doesn't match rule
            if (!device.hasProperty(k) || !device.getProperty(k).equalsIgnoreCase(rule.getString(k))) {
                return false;
            }
        }

        // If device matches any exceptions, it doesn't match rule
        if (rule.has("except")) {
            JSONArray es = rule.getJSONArray("except");
            for (int i = 0; i < es.length(); i++) {
                if (ruleMatch(es.getJSONObject(i), device)) {
                    return false;
                }
            }
        }

        return true;
    }

    // Returns true if device is allowed by testPolicy and false if device violates testPpolicy
    public static boolean allows(Device device, JSONObject testPolicy) {
        try {
            // Whether device is allowed by policy
            boolean allowed = testPolicy.getString("rule_type").equalsIgnoreCase("allow");

            // If device matches any exception, negate matches
            if (testPolicy.has("except")) {
                JSONArray es = testPolicy.getJSONArray("except");
                for (int i = 0; i < es.length(); i++) {
                    JSONObject e = es.getJSONObject(i);
                    if (ruleMatch(e, device)) {
                        return !allowed;
                    }
                }
            }

            return allowed;

        } catch(JSONException e){
            Log.e("allows JSON error", e.toString());
            return false;
        }

    }

    // Returns true if device is allowed by policy
    public static boolean allows(Device device){
        return allows(device, policy);
    }
}
