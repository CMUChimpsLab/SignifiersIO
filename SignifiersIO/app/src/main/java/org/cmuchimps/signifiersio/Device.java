package org.cmuchimps.signifiersio;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

enum DataType {
    VIDEO,
    AUDIO,
    LOCATION,
    ACTIVITY,
    EMI,

    UNKNOWN
}

public class Device {
    public DataType dataType;
    public Map<String, String> properties;
    public final boolean violation; // Whether this device violates the user's privacy policy

    public Device(JSONObject o) throws JSONException{
        properties = new HashMap<>();

        // Add all pairs to properties and assign dataType
        Iterator<String> keys = o.keys();
        while (keys.hasNext()) {
            String k = keys.next();

            // Assign dataType
            if(k.equalsIgnoreCase("data_type")){
                this.dataType = stringToDataType(o.getString(k));
            }

            // dataType is also in properties
            properties.put(k, o.getString(k));
        }

        if(this.dataType == null){
            throw new JSONException("Device JSON has no data_type property");
        }

        this.violation = !PrivacyParser.allows(this);
    }

    public boolean hasProperty(String k){
        return this.properties.containsKey(k);
    }
    public String getProperty(String k){
        return this.properties.get(k);
    }

    // Creates short, one-line description of device
    public String toString(){
        StringBuilder res = new StringBuilder();
        boolean first = true;
        String[] props = {"device_name","data_type","purpose"};

        for(String property : props){
            if(this.hasProperty(property)) {
                res.append(first ? "" : ", ").append(this.getProperty(property));
            }
            first = false;
        }

        return res.toString();
    }

    // Comparisons should only compare device properties
    @Override
    public boolean equals(Object obj) {
        return (obj.getClass() == Device.class) && this.properties.equals(((Device)obj).properties);
    }
    @Override
    public int hashCode(){
        return this.properties.hashCode();
    }

    // Creates multiline string describing all properties of this device
    // TODO: sort, make keys friendly (ie not "data_type")
    public String propsToString(){
        StringBuilder res = new StringBuilder();
        boolean first = true;

        for(String key : this.properties.keySet()){
            res.append(first ? "" : "\n")
                    .append(key).append(": ")
                    .append(this.getProperty(key));

            first = false;
        }

        return res.toString();
    }

    public static DataType stringToDataType(String s){
        switch(s.toLowerCase()){
            case "video": return DataType.VIDEO;
            case "audio": return DataType.AUDIO;
            case "location": return DataType.LOCATION;
            case "activity": return DataType.ACTIVITY;
            case "emi": return DataType.EMI;

            default: return DataType.UNKNOWN;
        }
    }
    public static int DataTypeToIcon(DataType d){
        switch(d){
            case VIDEO:
                return R.drawable.camera;
            case AUDIO:
                return R.drawable.mic;
            case LOCATION:
                return R.drawable.map;
            case ACTIVITY:
                return R.drawable.wave;
            case EMI:
                return R.drawable.magnet;
            default:
                return R.drawable.ic_launcher_background;
        }
    }
}
