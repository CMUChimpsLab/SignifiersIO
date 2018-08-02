package org.cmuchimps.signifiersio;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class PolicyException extends FrameLayout {
    private int index;
    private JSONObject exceptionJSON;

    public PolicyException(Context context, JSONObject exceptionJSON, int index) {
        super(context);
        this.exceptionJSON = exceptionJSON;
        this.index = index;
        initView();
        this.setText(summarizeException());
    }
    private void initView() {
        View view = inflate(getContext(), R.layout.policy_exception, null);
        addView(view);
    }

    // Create a short and meaningful description of the exception
    private String summarizeException() {
        try {
            StringBuilder res = new StringBuilder();
            Iterator<String> keys = exceptionJSON.keys();
            boolean first = true;

            // Concatenate all properties of the exception
            while(keys.hasNext()){
                String property = keys.next();
                // TODO: skip any special keys you add for bookkeeping, e.g. an order param

                res.append(first ? "" : ", ");
                if(property.equals("except")){
                    // The except property is a list, so just ellipsize it.
                    res.append("except...");
                } else {
                    res.append(exceptionJSON.getString(property));
                }

                first = false;
            }

            return res.toString();

        } catch(JSONException e){
            Log.e("summarizeException",e.toString());
            return "JSON Error";
        }
    }

    private void setText(String text){
        ((TextView)findViewById(R.id.summary)).setText(text);
    }

    public int getIndex(){ return this.index; }
    public void setIndex(int index){ this.index = index; }
    public JSONObject getExceptionJSON() { return exceptionJSON; }

    public void setExceptionJSON(JSONObject exceptionJSON) {
        this.exceptionJSON = exceptionJSON;
        // Update the summary to reflect the new JSON
        setText(summarizeException());
    }
}
