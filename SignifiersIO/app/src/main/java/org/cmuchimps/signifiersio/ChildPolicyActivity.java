package org.cmuchimps.signifiersio;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChildPolicyActivity extends PolicyActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize global variables
        exceptions = new ArrayList<>();


        // Create a new view from each exception and add it to the list
        for(int i = 0; i < exceptions.size(); i++){
            // TODO: make the order stable
            addException(exceptions.get(i));
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
            exception.put("company",((EditText)findViewById(R.id.company_edit)).getText());
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
