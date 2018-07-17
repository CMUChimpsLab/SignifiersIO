package org.cmuchimps.signifiersio;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.Set;

//import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class DeviceList implements View.OnClickListener{
    private final View parentView; // Context is used to inflate the xml file for the device list
    private final DataType dataType;
    private Set<Device> deviceSet;
    private PopupWindow popupWindow;
    private final MainActivity mainActivity;

    public DeviceList(View parentView, DataType dataType, Set<Device> deviceSet, MainActivity mainActivity){
        this.parentView = parentView;
        this.dataType = dataType;
        this.deviceSet = deviceSet;
        this.mainActivity = mainActivity;
    }


    public void onClick(View v){
        // Create the view
        Context context = this.parentView.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.device_list_popup, null);

        // Set the proper width, which unfortunately must be done programmatically
        final int screen_width = this.parentView.getWidth();
        Guideline page_layout = popupView.findViewById(R.id.double_width);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)page_layout.getLayoutParams();
        params.guideBegin = 2*screen_width;
        page_layout.setLayoutParams(params);

        // Configure the scroll between pages by disabling touch scrolling
        final HorizontalScrollView pageScroll = popupView.findViewById(R.id.scroller);
        pageScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        // Make the back arrow scroll left
        popupView.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pageScroll.smoothScrollTo(0, 0);
            }
        });
        // TODO: make the system back button work

        // Set the icon and text of the popup
        ImageView icon = popupView.findViewById(R.id.icon_img);
        icon.setImageResource(Device.DataTypeToIcon(this.dataType));
        TextView dataType = popupView.findViewById(R.id.data_type_text);
        dataType.setText(this.dataType.toString());

        // Add the devices to the popup
        LinearLayout deviceList = popupView.findViewById(R.id.device_list);
        for(final Device d : deviceSet){
            View row = inflater.inflate(R.layout.device_blurb, null);

            // Show device details
            TextView rowText = row.findViewById(R.id.device_string);
            rowText.setText(d.toString());

            // If the device violates our policy, change the arrow icon to the alert icon
            if(d.violation){
                ((ImageView)row.findViewById(R.id.row_arrow)).setImageResource(R.drawable.alert);
                // TODO: Stabilize the order, make alert devices come first
            }

            // Add functionality to the device string button
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // When device string is clicked, show full device properties
                    ((TextView)pageScroll.findViewById(R.id.device_details)).setText(d.propsToString());
                    // TODO: set device icon

                    // If the device violates our policy, show yet another alert
                    if(d.violation) {
                        pageScroll.findViewById(R.id.alert).setVisibility(View.VISIBLE);
                    } else {
                        // We need this else because the icon is shared among all devices of this datatype
                        pageScroll.findViewById(R.id.alert).setVisibility(View.GONE);
                    }

                    // Scroll to the right
                    pageScroll.smoothScrollTo(2*screen_width, 0);

                    // Send a request to light this device
                    mainActivity.light(d.getProperty("device_id"));
                }
            });
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
