package org.cmuchimps.signifiersio;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

interface DeviceUpdateListener {
    void onDeviceUpdate();
}

public class MainActivity extends AppCompatActivity implements DeviceUpdateListener {
    private static final int ICONS_PER_ROW = 3;   // Number of icons in each row of the UI

    private DeviceDetector deviceDetector = null;

    // DNS service discovery
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager mNsdManager;
    private static final String SERVICE_TYPE = "_http._tcp."; // TODO: pick the right service type
    private static final String SERVICE_NAME = "iot_hub";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load the privacy policy
        PrivacyParser.loadPP();

        // Set up the NSD manager and listeners
        mNsdManager = (NsdManager)getApplicationContext().getSystemService(Context.NSD_SERVICE);
        initializeResolveListener();
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Resume the deviceDetector's timer
        if(deviceDetector != null){
            deviceDetector.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pause the deviceDetector's timer
        if(deviceDetector != null){
            deviceDetector.pause();
        }
    }

    // When devices update, rebuild all the icons
    public void onDeviceUpdate(){
        // Get the new list of devices
        Map<DataType, Set<Device>> hierarchy = deviceDetector.getDeviceHierarchy();

        // Remove the old icons
        TableLayout table = findViewById(R.id.icon_table);
        table.removeAllViews();

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Add the new icons
        int tableIndex = 0;
        TableRow row = null;
        for (DataType dt : hierarchy.keySet()) {

            // Create a new table row for each ICONS_PER_ROW icons
            if(tableIndex % ICONS_PER_ROW == 0){
                row = new TableRow(this);
                table.addView(row);
            }

            // Set image for each icon
            ConstraintLayout icon = (ConstraintLayout) inflater.inflate(R.layout.data_type_icon, null);
            ((ImageView)icon.findViewById(R.id.icon_img)).setImageResource(Device.DataTypeToIcon(dt));

            // Turn on the alert icon if there's a violation
            for(Device dev : hierarchy.get(dt)){
                if(dev.violation){
                    icon.findViewById(R.id.alert).setVisibility(View.VISIBLE);
                    break;
                }
            }

            // Create the click listener that will pop up a device list
            icon.setOnClickListener(new DeviceList(findViewById(R.id.root_view),dt,hierarchy.get(dt)));

            row.addView(icon);
            tableIndex++;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.edit:
                Intent intent = new Intent(this, RootPolicyActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Set up NSD discovery to find the IoT hub
    public void initializeDiscoveryListener() {
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
    private void initializeResolveListener() {
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
                String address = host.getHostAddress();
                Log.d(TAG, "Resolved address = " + address);

                // Create a DeviceDetector with the address of the IoT hub
                deviceDetector = new DeviceDetector(MainActivity.this, address);
                deviceDetector.setOnDeviceUpdateListener(MainActivity.this);
                deviceDetector.resume();

                // TODO: add a listener for when we lose the connection
            }
        };
    }
}
