package org.cmuchimps.signifiersio;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeviceDetector {
    private Set<Device> devices; // Current set of devices
    private EnumMap<DataType, Set<Device>> deviceHierarchy; // Organized as the user sees it
    private final Context context; // Required for volley
    private int time = 0; // TODO: remove when testing is done
    private final static String DD_URL = "http://lemonshark.ics.cs.cmu.edu:24390/devices.json";

    public DeviceDetector(Context context){
        this.context = context;

        // Initialize with no devices
        devices = new HashSet<>();
        rebuildHierarchy();
    }

    // Refresh the devices and update devices and deviceHierarchy
    public void refresh(){
        // Fetch data from IoT hub
        StringRequest request = new StringRequest(DD_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                try {
                    Log.d("DD","got string");
                    JSONArray jsonDevices = new JSONArray(string);
                    // TODO: check for validity

                    // Turn the JSON into a Set of Devices
                    updateDevices(jsonDevices);

                    // TODO: Only do this if devices hasn't changed
                    rebuildHierarchy();
                } catch (JSONException e){
                    Log.e("DD invalid JSON", e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e("Device Detector error",volleyError.toString()); //volleyError.getMessage()
            }
        });
        RequestQueue rQueue = Volley.newRequestQueue(this.context);
        rQueue.add(request);
    }

    // Set some stock devices
    private void setDevicesDummy(){
        devices = new HashSet<Device>();

        try{
            if(time % 3 != 0)
            devices.add(new Device(new JSONObject(
                    "{'company':'Google', 'purpose':'advertising', 'data_type':'audio', 'device_name':'Google Home'}")));
            devices.add(new Device(new JSONObject(
                    "{'company':'Skype', 'purpose':'communication', 'data_type':'video'}")));
            devices.add(new Device(new JSONObject(
                    "{'company':'Apple', 'purpose':'security', 'data_type':'location'}")));
            devices.add(new Device(new JSONObject(
                    "{'company':'Phishing4Less', 'purpose':'advertising', 'data_type':'video'}")));
            devices.add(new Device(new JSONObject(
                    "{'company':'CMU', 'purpose':'research', 'data_type':'activity'}")));
        } catch(JSONException e){
            Log.e("refreshDevices JSON err", e.toString());
        }

        time++;
    }

    // Turn a JSONArray into a Set of Devices
    private void updateDevices(JSONArray jsonDevices) throws JSONException {
        // Initialize new set with initial capacity = # devices
        Set<Device> newDevices = new HashSet<>(jsonDevices.length());

        // Add each device to newDevices
        for(int i = 0; i < jsonDevices.length(); i++){
            newDevices.add(new Device(jsonDevices.getJSONObject(i)));
        }

        this.devices = newDevices;
    }

    // Make deviceHierarchy use the data in devices
    private void rebuildHierarchy() {
        deviceHierarchy = new EnumMap<DataType, Set<Device>>(DataType.class);

        // Add each device to the proper set in the hierarchy, based on its datatype
        for(Device d : devices){
            if(deviceHierarchy.containsKey(d.dataType)){
                deviceHierarchy.get(d.dataType).add(d);
            } else {
                Set<Device> newSet = new HashSet<>();
                newSet.add(d);
                deviceHierarchy.put(d.dataType, newSet);
            }
        }
    }

    public Map<DataType, Set<Device>> getDeviceHierarchy(){
        return deviceHierarchy;
    }
}
