package org.cmuchimps.signifiersio;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChildPolicyActivity extends PolicyActivity {
    private boolean deletable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If this activity is editing an exception, there will be an extra that contains
        // a JSON description of the exception.
        if(getIntent().hasExtra(EXCEPTION_JSON)){
            // This is an edit, so enable the delete button
            findViewById(R.id.delete_button).setVisibility(View.VISIBLE);

            try {
                JSONObject exceptionJSON = new JSONObject(getIntent().getStringExtra(EXCEPTION_JSON));

                // Populate the fields with the values in the extra, as given
                if(exceptionJSON.has("data_type")){
                    ((EditText)findViewById(R.id.datatype_edit)).setText(exceptionJSON.getString("data_type"));
                }
                if(exceptionJSON.has("device_name")){
                    ((EditText)findViewById(R.id.device_name_edit)).setText(exceptionJSON.getString("device_name"));
                }
                if(exceptionJSON.has("company")){
                    ((EditText)findViewById(R.id.company_edit)).setText(exceptionJSON.getString("company"));
                }
                if(exceptionJSON.has("purpose")){
                    ((EditText)findViewById(R.id.purpose_edit)).setText(exceptionJSON.getString("purpose"));
                }
                if(exceptionJSON.has("status")){
                    ((EditText)findViewById(R.id.status_edit)).setText(exceptionJSON.getString("status"));
                }

                // Create a new view from each exception in the Extra and add it to the list
                if(exceptionJSON.has("except")){
                    JSONArray exs = exceptionJSON.getJSONArray("except");

                    for(int i = 0; i < exs.length(); i++){
                        // TODO: make the order stable
                        addException(exs.getJSONObject(i));
                    }
                }

            } catch(JSONException e){
                Log.e("CPA onCreate", e.toString());
            }
        }
    }

    // Create the JSON of the current exception and return it as a result
    private void returnException(){
        Intent intent = new Intent();
        intent.putExtra(getResources().getString(R.string.exception_json), createException().toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    // Represent the current fields as a JSONObject
    private JSONObject createException(){
        JSONObject exception = new JSONObject();

        try{
            String data_type = ((EditText)findViewById(R.id.datatype_edit)).getText().toString();
            if(!data_type.equals("")) { exception.put("data_type", data_type); }
            
            String device_name = ((EditText)findViewById(R.id.device_name_edit)).getText().toString();
            if(!device_name.equals("")) { exception.put("device_name", device_name); }

            String company = ((EditText)findViewById(R.id.company_edit)).getText().toString();
            if(!company.equals("")) { exception.put("company", company); }

            String purpose = ((EditText)findViewById(R.id.purpose_edit)).getText().toString();
            if(!purpose.equals("")) { exception.put("purpose", purpose); }

            String status = ((EditText)findViewById(R.id.status_edit)).getText().toString();
            if(!status.equals("")) { exception.put("status", status); }

            // TODO: add exceptions
        } catch(JSONException e){
            Log.e("NewException", e.toString());
        }

        return exception;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.done:
                returnException();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
