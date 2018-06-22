package org.cmuchimps.signifiersio;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeviceDetector {
    public Set<Device> devices;


    // Reload the set of devices in the room
    // TODO: Don't use dummy data
    public void refreshDevices(){
        // Completely rebuilds devices

        devices = new HashSet<Device>();

        try{
            devices.add(new Device(new JSONObject(
                    "{'company':'Google', 'purpose':'advertising', 'data_type':'audio'}")));
            devices.add(new Device(new JSONObject(
                    "{'company':'Skype', 'purpose':'communication', 'data_type':'video'}")));
            devices.add(new Device(new JSONObject(
                    "{'company':'Apple', 'purpose':'security', 'data_type':'location'}")));
            devices.add(new Device(new JSONObject(
                    "{'company':'Phishing4Less', 'purpose':'advertising', 'data_type':'video'}")));
            devices.add(new Device(new JSONObject(
                    "{'company':'CMU', 'purpose':'research', 'data_type':'activity'}")));
        } catch(JSONException e){
            Log.e("refreshDevices JSON err", e.getMessage());

        }
        // "{'company':'', 'purpose':'', 'data_type':''}"
    }

    public Set<DataType> getDataTypes(){
        Set<DataType> ds = EnumSet.noneOf(DataType.class);

        // Add the datatype of each device to the set
        for(Device d : devices){
            ds.add(d.dataType);
        }

        return ds;
    }
}
