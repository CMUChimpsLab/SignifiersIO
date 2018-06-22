package org.cmuchimps.signifiersio;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int ICONS_PER_ROW = 3;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DeviceDetector dd = new DeviceDetector();
        dd.refreshDevices();

        TableLayout table = findViewById(R.id.icon_table);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int tableIndex = 0;
        TableRow row = null;
        for (DataType d : dd.getDataTypes()) {
            if(tableIndex % ICONS_PER_ROW == 0){
                row = new TableRow(this);
                table.addView(row);
            }

            ConstraintLayout icon = (ConstraintLayout) inflater.inflate(R.layout.data_type_icon, null);
            ((ImageView)icon.findViewById(R.id.icon_img)).setImageResource(Device.DataTypeToIcon(d));
            icon.setMinWidth(100);
            icon.setMinHeight(100);

            row.addView(icon);

            tableIndex++;
        }

        //LinearLayout test = findViewById(R.id.testLayout);
        Log.d("number of devices",""+dd.devices.size());


//            //ViewGroup.LayoutParams ps = icon.getLayoutParams();
    }
}
