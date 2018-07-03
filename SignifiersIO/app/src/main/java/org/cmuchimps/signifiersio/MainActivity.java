package org.cmuchimps.signifiersio;

import android.content.Context;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.json.JSONObject;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final int ICONS_PER_ROW = 3;   // Number of icons in each row of the UI
    private static final int REFRESH_TIME = 10000; // ms between updating the device list

    private final DeviceDetector deviceDetector = new DeviceDetector(this);
    private Timer refreshTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load the privacy policy
        PrivacyParser.loadPP();
    }

    @Override
    protected void onResume(){
        super.onResume();
        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask(){
            public void run(){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        drawIcons();
                    }
                });
            }
        }, 0, REFRESH_TIME);
    }

    @Override
    public void onPause() {
        // Stop refreshing when you pause the app
        refreshTimer.cancel();
        super.onPause();
    }

    // Draw the icons and alerts
    public void drawIcons(){
        // Update the list of devices
        deviceDetector.refresh();
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
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
