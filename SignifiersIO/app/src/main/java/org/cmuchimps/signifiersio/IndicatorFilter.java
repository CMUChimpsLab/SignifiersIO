package org.cmuchimps.signifiersio;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class IndicatorFilter extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indicator_filter);
    }

    // Light up the indicators for devices that match the filters
    public void lightFiltered(View view){
        JSONObject filter = getFilter(); // In the form of a privacy policy
        Set<Device> filtered = new HashSet<>();

        // Whether we're only lighting devices that violate Our privacy preferences
        boolean onlyViolations = ((Switch)findViewById(R.id.only_violations_switch)).isChecked();

        for(Device d : DeviceDetector.getDevices()){
            if(!PrivacyParser.allows(d, filter) && (!onlyViolations || d.violation)){
                filtered.add(d);
            }
        }

        DeviceDetector.light(filtered);

        String text = filtered.size() + " devices selected. Look for the lights";

        Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER,0,0);

        toast.show();
    }

    // Parse the fields to create a policy that treats the devices we want to see as violations
    private JSONObject getFilter(){
        JSONObject filter = new JSONObject();
        JSONObject ex = new JSONObject();

        try{
            String data_type = ((EditText)findViewById(R.id.datatype_filter)).getText().toString();
            if(!data_type.equals("")) { ex.put("data_type", data_type); }

            String device_name = ((EditText)findViewById(R.id.devicename_filter)).getText().toString();
            if(!device_name.equals("")) { ex.put("device_name", device_name); }

            String company = ((EditText)findViewById(R.id.company_filter)).getText().toString();
            if(!company.equals("")) { ex.put("company", company); }

            String purpose = ((EditText)findViewById(R.id.purpose_filter)).getText().toString();
            if(!purpose.equals("")) { ex.put("purpose", purpose); }

            String status = ((EditText)findViewById(R.id.status_filter)).getText().toString();
            if(!status.equals("")) { ex.put("status", status); }

            // Put everything into a real policy
            filter.put("rule_type", "allow");
            JSONArray exs = new JSONArray();
            exs.put(ex);
            filter.put("except", exs);

        } catch(JSONException e){
            Log.e("getFilter", e.toString());
        }

        return filter;
    }
}
