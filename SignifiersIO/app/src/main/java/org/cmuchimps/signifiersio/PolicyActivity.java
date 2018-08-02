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

    protected static final String EXCEPTION_JSON = "org.cmuchimps.signifiersio.exception_json";
    protected static final String DELETED_KEY = "org.cmuchimps.signifiersio.deleted_key";

    // Stores all the exceptions to this rule, which includes displaying them
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

        if(resultCode == RESULT_OK) {
            if (requestCode == NEW_EXCEPTION_REQUEST) {
                // If the ChildPolicyActivity returned a new exception
                try {
                    String exceptionJSON = data.getStringExtra(getString(R.string.exception_json));
                    JSONObject exception = new JSONObject(exceptionJSON);

                    // Create a View to display and allow editing of the exception,
                    // and add the new exception to our list
                    addException(exception);

                } catch (JSONException e) {
                    Log.d("onActivityResult", e.toString());
                }

            } else if(0 <= requestCode - EDIT_EXCEPTION_OFFSET &&
                    requestCode - EDIT_EXCEPTION_OFFSET < exceptionList.getChildCount()){
                // We were editing an exception
                int index = requestCode - EDIT_EXCEPTION_OFFSET;

                if(data.hasExtra(DELETED_KEY) && data.getBooleanExtra(DELETED_KEY, false)){
                    // If we deleted the exception we were editing

                    exceptionList.removeViewAt(index);

                    // Update all the indices so we delete and edit the right ones
                    for(int i = 0; i < exceptionList.getChildCount(); i++){
                        ((PolicyException)exceptionList.getChildAt(i)).setIndex(i);
                    }
                } else {
                    // If the ChildPolicyActivity returned an edited exception
                    try {
                        String exceptionJSON = data.getStringExtra(getString(R.string.exception_json));
                        JSONObject exception = new JSONObject(exceptionJSON);

                        ((PolicyException)exceptionList.getChildAt(index)).setExceptionJSON(exception);

                    } catch (JSONException e) {
                        Log.d("onActivityResult", e.toString());
                    }
                }
            }
        }
    }

    // Create a view for exception (in JSON format) and add it to our LinearLayout of exceptions
    protected void addException(final JSONObject exception){
        PolicyException p = new PolicyException(this, exception, exceptionList.getChildCount());

        p.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start activity to edit an exception
                Intent intent = new Intent(PolicyActivity.this, ChildPolicyActivity.class);
                intent.putExtra(EXCEPTION_JSON, exception.toString());

                startActivityForResult(intent, EDIT_EXCEPTION_OFFSET + ((PolicyException)view).getIndex());
            }
        });

        // Add this view to the LinearLayout
        exceptionList.addView(p);
    }

    // Delete this exception. Called when the delete button is tapped
    public void deleteThis(View view){
        Intent intent = new Intent();
        intent.putExtra(DELETED_KEY, true);
        setResult(RESULT_OK, intent);
        finish();
    }

    // Create a Save button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.exception_menu, menu);
        return true;
    }
}
