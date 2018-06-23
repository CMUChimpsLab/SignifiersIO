package org.cmuchimps.signifiersio;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeviceDetector {
    private Set<Device> devices; // Current set of devices
    private EnumMap<DataType, Set<Device>> deviceHierarchy; // Organized as the user sees it
    private int time = 0; // TODO: remove when testing is done

    // Refresh the devices and update devices and deviceHierarchy
    public void refresh(){
        refreshDevices();

        // TODO: Only do this if devices hasn't changed
        rebuildHierarchy();
    }
    // Reload the set of devices in the room
    // TODO: Don't use dummy data
    private void refreshDevices(){
        devices = new HashSet<Device>();

        try{
            if(time % 3 != 0)
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

        time++;
    }

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
