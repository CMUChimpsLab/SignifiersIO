package org.cmuchimps.signifiersio;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class NewException extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_exception);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.exception_menu, menu);
        return true;
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
