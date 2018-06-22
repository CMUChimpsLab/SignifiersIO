package org.cmuchimps.signifiersio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class PrivacyParser {
    // TODO: handle multiple purposes/data_types

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
            "}" +
            "]" +
            "}" +
            "]" +
            "}";

    // TODO: loads privacy preferences from <unclear>
    public static JSONObject loadPP() throws JSONException {
        return new JSONObject(ex_pp);
    }

    // Return the opposite rule_type
    private static String flip(String rule_type) throws JSONException {
        if (rule_type.equals("allow")) {
            return "disallow";
        } else if (rule_type.equals("disallow")) {
            return "allow";
        } else {
            throw new JSONException("Bad rule_type");
        }
    }

    // Checks whether  a rule (and all its exceptions) is valid
    private static boolean ruleValid(JSONObject rule, String rule_type, boolean pedantic)
            throws JSONException {

        // If present, rule_type must match expectation
        if (rule.has("rule_type") && !rule.getString("rule_type").equals(rule_type)) {
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
    public static boolean isValid(JSONObject pp, boolean pedantic)
            throws JSONException {

        // rule_type is required for top-level
        if (!pp.has("rule_type") || !(pp.getString("rule_type").equals("allow") ||
                pp.getString("rule_type").equals("disallow"))) {
            return false;
        }

        // Check for extraneous keys
        if (pedantic) {
            if (pp.length() > (pp.has("except") ? 2 : 1)) {
                return false;
            }
        }

        return ruleValid(pp, pp.getString("rule_type"), pedantic);
    }

    public static boolean isValid(JSONObject pp)
            throws JSONException {
        return isValid(pp, false);
    }

    // Determines whether device matches rule.
    // Ignores rule_type (i.e. doesn't check whether device is allowed)
    private static boolean ruleMatch(JSONObject rule, Device device)
            throws JSONException {
        Iterator<String> keys = rule.keys();
        while (keys.hasNext()) {
            String k = keys.next();

            if (k.equals("rule_type") || k.equals("except")) {
                continue;
            }

            // If device doesn't have property k or it doesn't match,
            // device doesn't match rule
            if (!device.hasProperty(k) || !device.getProperty(k).equals(rule.getString(k))) {
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

    // Returns true if device is allowed by pp and false if device violates pp
    public static boolean match(JSONObject pp, Device device)
            throws JSONException {

        // Whether device is allowed by pp
        boolean allowed = pp.getString("rule_type").equals("allow");

        // If device matches any exception, negate matches
        JSONArray es = pp.getJSONArray("except");
        for (int i = 0; i < es.length(); i++) {
            JSONObject e = es.getJSONObject(i);
            if (ruleMatch(e, device)) {
                return !allowed;
            }
        }
        
        return allowed;
    }
}
