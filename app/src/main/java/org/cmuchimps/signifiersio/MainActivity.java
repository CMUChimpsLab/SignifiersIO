package org.cmuchimps.signifiersio;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.json.JSONObject;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final int ICONS_PER_ROW = 3;
    private static final int REFRESH_TIME = 1000;
    DeviceDetector deviceDetector = new DeviceDetector();
    JSONObject privacyPolicy = PrivacyParser.loadPP();
    private Timer refreshTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceDetector = new DeviceDetector();
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
                if(!PrivacyParser.allows(privacyPolicy, dev)){
                    icon.findViewById(R.id.alert).setVisibility(View.VISIBLE);
                    break;
                }
            }

            row.addView(icon);
            tableIndex++;
        }

    }

}
