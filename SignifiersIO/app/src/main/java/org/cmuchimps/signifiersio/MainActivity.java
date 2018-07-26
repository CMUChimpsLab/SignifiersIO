package org.cmuchimps.signifiersio;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

interface DeviceUpdateListener {
    void onDeviceUpdate(Set<Device> newDevices, Set<Device> oldDevices);
}

public class MainActivity extends AppCompatActivity implements DeviceUpdateListener {
    private static final int ICONS_PER_ROW = 3;   // Number of icons in each row of the UI
    private static final String CHANNEL_ID = "org.cmuchimps.signifier.io.privacy_channel"; // id for notifications channel

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        // Load the privacy policy
        PrivacyParser.loadPP();

        DeviceDetector.startDiscovery(this);
        DeviceDetector.setOnDeviceUpdateListener(this);
    }

    // Specific to Android 8
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Privacy Violations";
            String description = "Alerts of new devices that violate your privacy.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Called when DeviceDetector gets a change in devices
    public void onDeviceUpdate(Set<Device> newDevices, Set<Device> oldDevices){
        // Update the UI
        updateIcons();

        // Send notifications of new devices
        if(shouldShowNotification()) {
            sendNotification(newDevices, oldDevices);
        }
    }

    private void updateIcons(){
        // Get the new list of devices
        Map<DataType, Set<Device>> hierarchy = DeviceDetector.getDeviceHierarchy();

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

    // Send a notification to the user if a new device violates their privacy
    private void sendNotification(Set<Device> newDevices, Set<Device> oldDevices){
        Set<Device> violations = new HashSet<>();

        for(Device d : newDevices){
            if(d.violation && !oldDevices.contains(d)){
                violations.add(d);
            }
        }

        if(violations.size() > 0) {
            // Build a string description of the new violations
            Iterator<Device> iterator = violations.iterator();
            StringBuilder sb = new StringBuilder(iterator.next().toNotificationString());

            while(iterator.hasNext()){
                sb.append("; ").append(iterator.next().toNotificationString());
            }

            // Open MainActivity when notification is tapped
            Intent intent = new Intent(this, MainActivity.class);
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            NotificationCompat.Builder violationNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.alert)
                    .setContentTitle("Privacy Violation")
                    .setContentText(sb.toString())
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify((int)((new Date()).getTime() % Integer.MAX_VALUE), violationNotification.build());
        }
    }

    // Returns false if app is foreground
    private boolean shouldShowNotification() {
        ActivityManager.RunningAppProcessInfo myProcess = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(myProcess);
        if(myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            return true;
        }

        KeyguardManager km = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        // app is in foreground, but if screen is locked show notification anyway
        return km.inKeyguardRestrictedInputMode();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent;
        switch(item.getItemId()){
            case R.id.edit:
                intent = new Intent(this, RootPolicyActivity.class);
                startActivity(intent);
                return true;
            case R.id.indicators:
                intent = new Intent(this, IndicatorFilter.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
