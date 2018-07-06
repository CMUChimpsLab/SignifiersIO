package org.cmuchimps.signifiersio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class PolicyActivity extends AppCompatActivity {
    protected static final int NEW_EXCEPTION_REQUEST = 1;  // Request code for a new exception
    protected static final int EDIT_EXCEPTION_OFFSET = 10; // Offset from index for editing an exception

    protected ArrayList<JSONObject> exceptions;
    LinearLayout exceptionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);

        // Initialize global variables
        exceptionList = findViewById(R.id.exception_list);

        // Add action to "Add Exception" button
        Button button = findViewById(R.id.add_exception);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start an activity that handles making a new exception
                Intent intent = new Intent(PolicyActivity.this, ChildPolicyActivity.class);
                startActivityForResult(intent, NEW_EXCEPTION_REQUEST);
            }
        });
    }

    // Called when the new exception activity finishes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityRequest","requestCode = "+requestCode);

        // If the ChildPolicyActivity returned a new exception
        if (requestCode == NEW_EXCEPTION_REQUEST && resultCode == RESULT_OK) {
            try {
                String exceptionJSON = data.getStringExtra(getString(R.string.exception_json));
                JSONObject exception = new JSONObject(exceptionJSON);

                // Add the new exception to our list
                exceptions.add(exception);

                // Create a Preference to display and allow editing of the exception
                addException(exception);

            } catch(JSONException e) {
                Log.d("onActivityResult",e.toString());
            }
        }
    }

    protected void addException(JSONObject exception){
        // TODO: use a custom view with an arrow or something
        TextView p = new TextView(this);
        p.setTextAppearance(R.style.TextAppearance_AppCompat_Large);
        p.setText(summarizeException(exception));

        p.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // Add this Preference to the view
        exceptionList.addView(p);
    }

    // Create a short and meaningful description of the exception
    protected String summarizeException(JSONObject exception) {
        try {
            StringBuilder res = new StringBuilder();
            Iterator<String> keys = exception.keys();
            boolean first = true;

            // Concatenate all properties of the exception
            while(keys.hasNext()){
                String property = keys.next();
                // TODO: skip any special keys you add

                res.append(first ? "" : ", ");
                if(property.equals("except")){
                    // The except property is a list, so just ellipsize it.
                    res.append("except...");
                } else {
                    res.append(exception.getString(property));
                }

                first = false;
            }

            return res.toString();

        } catch(JSONException e){
            Log.e("summarizeException",e.toString());
            return "JSON Error";
        }
    }

    // Create a Save button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.exception_menu, menu);
        return true;
    }
}
