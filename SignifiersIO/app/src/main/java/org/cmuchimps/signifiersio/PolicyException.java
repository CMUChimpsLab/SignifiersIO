package org.cmuchimps.signifiersio;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class PolicyException extends FrameLayout {
    public int index;

    /*public PolicyException(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public PolicyException(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }*/

    public PolicyException(Context context) {
        super(context);

        this.index = index;
        initView();
    }

    public void setIndex(int index){
        this.index = index;
    }
    public int getIndex(){
        return this.index;
    }

    public void setText(String text){
        ((TextView)findViewById(R.id.summary)).setText(text);
    }

    private void initView() {
        View view = inflate(getContext(), R.layout.policy_exception, null);
        addView(view);
    }
}
