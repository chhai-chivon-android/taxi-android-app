package com.sharedcab.batchcar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.NumberPicker.OnValueChangeListener;

public class DaysPicker extends DialogFragment implements OnValueChangeListener{
	Context context;
	private int number;
	
	private NumberPicker dp;

    // Define activity
    private Activity activity;

    // Define Dialog view
    private View mView;

    public DaysPicker(Activity a){
    	this.activity = a;
    	LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.number_picker, null);  
        dp = (NumberPicker) mView.findViewById(R.id.days_picker);
        dp.setMaxValue(15);
        dp.setMinValue(1);
    }
    
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    builder.setView(mView);
	    builder.setMessage("Days?")
        .setPositiveButton("Set", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	TextView x = ((TextView)((RouteFragment)((MainActivity)activity).mainFragment).
                		rootView.findViewById(R.id.tv_time_end));
            			if(number == 0)
            				number = 1;
            			String y = Integer.toString(getDays());
            			if("0".equals(y))
            				y="1";
                		x.setText(y+" DAYS");
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                DaysPicker.this.getDialog().cancel();
            }
        });
	    Dialog x = builder.create();
	    return x;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    dp.setOnValueChangedListener(this);
	}

	@Override
	public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		number = newVal;
	}

	public int getDays(){
		return number;
	}
}