package org.cmuchimps.signifiersio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
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
    private Map<String, String> properties;
    public boolean violation; // Whether this device violates the user's privacy policy
    public Bitmap deviceImage;

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

        this.updateViolation();

        // Download the image, if it is specified
        if(this.hasProperty("device_image")){
            new DownloadImage().execute(this.getProperty("device_image"));
        } else {
            this.deviceImage = null;
        }
    }

    // AsyncTask to download an image in the background
    private class DownloadImage extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... URL) {

            String imageURL = URL[0];

            Bitmap bitmap = null;
            try {
                // Download Image from URL
                InputStream input = new java.net.URL(imageURL).openStream();
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // Set the host Device's image with the bitmap we download
            deviceImage = result;
        }
    }

    public boolean hasProperty(String k){ return this.properties.containsKey(k); }
    public String getProperty(String k){ return this.properties.get(k); }

    // Set violation to reflect whether this device violates the privacy policy
    public void updateViolation(){ this.violation = !PrivacyParser.allows(this); }

    // Creates short, one-line description of device
    public String toString(){
        StringBuilder res = new StringBuilder();
        boolean first = true;
        String[] props = {"device_name","purpose"};

        for(String property : props){
            if(this.hasProperty(property)) {
                res.append(first ? "" : ", ").append(this.getProperty(property));
                first = false;
            }
        }

        return res.toString();
    }

    public String toNotificationString(){
        StringBuilder res = new StringBuilder();

        if(this.hasProperty("device_name")){
            res.append(this.getProperty("device_name"));
        } else if(this.hasProperty("company")){
            res.append(this.getProperty("company"));
        } else {
            res.append("A device");
        }
        res.append(" is recording ");

        if(this.hasProperty("data_type")){
            res.append(this.getProperty("data_type"));
        } else {
            res.append("something");
        }

        if(this.hasProperty("purpose")) {
            res.append(" for ").append(this.getProperty("purpose"));
        }

        return res.toString();
    }

    // Comparisons should only compare device properties
    @Override
    public boolean equals(Object obj) { return (obj.getClass() == Device.class) && this.properties.equals(((Device)obj).properties); }
    @Override
    public int hashCode(){ return this.properties.hashCode(); }

    // Creates multiline string describing all properties of this device
    // TODO: sort
    public String propsToString(){
        StringBuilder res = new StringBuilder();
        boolean first = true;

        for(String key : this.properties.keySet()){
            // Don't render the device image url because it's long and not helpful
            if(key.equalsIgnoreCase("device_image")){ continue; }
            res.append(first ? "" : "\n")
                    .append(keyClean(key)).append(": ")
                    .append(this.getProperty(key));

            first = false;
        }

        return res.toString();
    }

    private static String keyClean(String key){
        // Turn '_' into ' '
        String res = TextUtils.join(" ", key.split("_"));

        // Make the first letter capitalized
        String first = res.substring(0,1);
        return res.replaceFirst(first, first.toUpperCase());
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
