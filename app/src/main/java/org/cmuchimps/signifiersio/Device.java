package org.cmuchimps.signifiersio;

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

    public Device(JSONObject o) throws JSONException{
        properties = new HashMap<>();

        // Add all pairs to properties and assign dataType
        Iterator<String> keys = o.keys();
        while (keys.hasNext()) {
            String k = keys.next();

            // Assign dataType
            if(k.equals("data_type")){
                this.dataType = stringToDataType(o.getString(k));
            }

            // dataType is also in properties
            properties.put(k, o.getString(k));
        }
    }

    public boolean hasProperty(String k){
        return this.properties.containsKey(k);
    }
    public String getProperty(String k){
        return this.properties.get(k);
    }

    public static DataType stringToDataType(String s){
        switch(s){
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
            default:
                return R.drawable.ic_launcher_background;
        }
    }
}
