package org.cmuchimps.signifiersio;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class DeviceDetector {
    // Device organization
    private static Set<Device> devices; // Current set of devices
    private static EnumMap<DataType, Set<Device>> deviceHierarchy; // Organized as the user sees it

    // Networking
    private static final String PREFIX = "http://";
    private static final String URI = "/devices.json";
    private static boolean connected = false;
    private static String hubAddress;
    private static RequestQueue requestQueue;

    // DNS service discovery
    private static NsdManager.DiscoveryListener mDiscoveryListener;
    private static NsdManager.ResolveListener mResolveListener;
    private static NsdManager mNsdManager;
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String SERVICE_NAME = "iot_hub";

    // Timing
    private static Timer refreshTimer; // Timer to periodically refresh devices
    private static final int REFRESH_TIME = 10000;

    // Other
    private static DeviceUpdateListener listener; // We call listener's onDeviceUpdate when the devices change

    public static void startDiscovery(Context context){
        requestQueue = Volley.newRequestQueue(context);

        // Set up the NSD manager and listeners
        mNsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);
        initializeResolveListener();
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

        // Initialize with no devices
        devices = new HashSet<>();
        rebuildHierarchy();
    }

    public static void setOnDeviceUpdateListener(DeviceUpdateListener listener){
        DeviceDetector.listener = listener;
    }

    // Start the periodic timer to check for changes to devices
    private static void startTimer(){
        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask(){
            public void run(){
                refresh();
            }
        }, 0, REFRESH_TIME);
    }

    private static void stopTimer(){
        refreshTimer.cancel();
    }

    // Refresh the devices and update devices and deviceHierarchy
    private static void refresh(){
        if(!connected){
            // Haven't found anything to get data from
            return;
        }

        // Fetch data from IoT hub
        StringRequest request = new StringRequest(DeviceDetector.PREFIX + hubAddress + DeviceDetector.URI, new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                try {
                    Log.d("DD","got string");
                    JSONArray jsonDevices = new JSONArray(string);
                    // TODO: check for validity

                    // Turn the JSON into a Set of Devices
                    Set<Device> newDevices = buildSet(jsonDevices);

                    if(!newDevices.equals(devices)) {
                        Log.d("DD", "devices changed");

                        Set<Device> oldDevices = devices;
                        devices = newDevices;

                        // Build the device hierarchy
                        rebuildHierarchy();

                        // Alert the listener that the devices have changed
                        if(listener != null) {
                            listener.onDeviceUpdate(newDevices, oldDevices);
                        }
                    }
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
        requestQueue.add(request);
    }


    // Set some stock devices
    private static void setDevicesDummy(){
        devices = new HashSet<Device>();

        try{
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
    }

    // Turn a JSONArray into a Set of Devices
    private static Set<Device> buildSet(JSONArray jsonDevices) throws JSONException {
        // Initialize new set with initial capacity = # devices
        Set<Device> newDevices = new HashSet<>(jsonDevices.length());

        // Add each device to newDevices
        for(int i = 0; i < jsonDevices.length(); i++){
            newDevices.add(new Device(jsonDevices.getJSONObject(i)));
        }

        return newDevices;
    }

    // Make deviceHierarchy use the data in devices
    private static void rebuildHierarchy() {
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

    // Set up NSD discovery to find the IoT hub
    private static void initializeDiscoveryListener() {
        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            private static final String TAG = "NSD_discover";

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started: "+regType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains(SERVICE_NAME)){ //TODO: .equals?
                    Log.d(TAG,"resolving " + service);
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Start Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Stop Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    // Set up te resolver to get the IP of the IoT hub and create a DeviceDetector
    private static void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            private static final String TAG = "NSD_resolve";

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                InetAddress host = serviceInfo.getHost();
                hubAddress = host.getHostAddress();
                connected = true;

                startTimer();

                Log.d(TAG, "Resolved address = " + hubAddress);

                // TODO: add a listener for when we lose the connection
            }
        };
    }

    // Light up the devices in lightDevices
    public static void light(final Set<Device> lightDevices){
        if(lightDevices.size() == 0){
            Log.d("light", "No devices!");
            return;
        }

        StringRequest request = new StringRequest(Request.Method.POST, "http://" + hubAddress, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("light Response", response);

                // TODO: use response to show color
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("light Error", error.toString());
            }
        }){
            // Add devices to POST request
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                for(Device d : lightDevices) {
                    // Make devices that violate policy show red
                    params.put(d.getProperty("device_id"), d.violation ? "1" : "0");
                }

                return params;
            }

            // TODO: try removing this
            @Override
            public Map<String, String> getHeaders() {
                Log.d("light","getHeaders");
                Map<String,String> params = new HashMap<>();
                params.put("Content-Type","text/plain");
                return params;
            }
        };

        requestQueue.add(request);
    }

    public static Map<DataType, Set<Device>> getDeviceHierarchy(){
        return deviceHierarchy;
    }
    public static Set<Device> getDevices(){
        return devices;
    }
}
