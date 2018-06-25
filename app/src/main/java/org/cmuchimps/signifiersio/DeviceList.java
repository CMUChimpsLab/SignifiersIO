package org.cmuchimps.signifiersio;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.Set;

//import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class DeviceList implements View.OnClickListener{
    private View parentView; // Context is used to inflate the xml file for the device list
    private DataType dataType;
    private Set<Device> deviceSet;
    private PopupWindow popupWindow;

    public DeviceList(View parentView, DataType dataType, Set<Device> deviceSet){
        this.parentView = parentView;
        this.dataType = dataType;
        this.deviceSet = deviceSet;
    }


    public void onClick(View v){
        // Create the view
        Context context = this.parentView.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.device_list_popup, null);

        // Set the icon of the popup
        ImageView icon = popupView.findViewById(R.id.icon_img);
        icon.setImageResource(Device.DataTypeToIcon(this.dataType));

        // Add the devices to the popup
        LinearLayout deviceList = popupView.findViewById(R.id.device_list);
        for(Device d : deviceSet){
            TextView row = new TextView(context);
            row.setText(d.getProperty("company"));
            deviceList.addView(row);
        }

        // Create the popup
        popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        // Make the button close the popup
        popupView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });

        popupWindow.showAtLocation(this.parentView, Gravity.CENTER, 0, 0);
    }


}
