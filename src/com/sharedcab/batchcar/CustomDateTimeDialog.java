package com.sharedcab.batchcar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

public class CustomDateTimeDialog extends DialogFragment 
				implements OnDateChangedListener, OnTimeChangedListener {
    // Define constants for date-time picker.
    public final static int DATE_PICKER = 1;
    public final static int TIME_PICKER = 2;
    public final static int DATE_TIME_PICKER = 3;

    // DatePicker reference
    private DatePicker datePicker;

    // TimePicker reference
    private TimePicker timePicker;

    // Calendar reference
    private Calendar mCalendar;

    // Define activity
    private Activity activity;

    // Define Dialog type
    private int DialogType;

    // Define Dialog view
    private View mView;

    // Constructor start
    public CustomDateTimeDialog(Activity activity) {
        this(activity, DATE_TIME_PICKER);
    }

    public CustomDateTimeDialog(Activity activity, int DialogType) {
        this.activity = activity;
        this.DialogType = DialogType;

        // Inflate layout for the view
        // Pass null as the parent view because its going in the dialog layout
        LayoutInflater inflater = activity.getLayoutInflater();
        mView = inflater.inflate(R.layout.custom_datetime_dialog, null);  

        // Grab a Calendar instance
        mCalendar = Calendar.getInstance();

        // Init date picker
        datePicker = (DatePicker) mView.findViewById(R.id.datePicker1);
        datePicker.init(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH), this);

        // Init time picker
        timePicker = (TimePicker) mView.findViewById(R.id.timePicker1);

        // Set default Calendar and Time Style
        setIs24HourView(false);
        setCalendarViewShown(false);

        switch (DialogType) {
        case DATE_PICKER:
            timePicker.setVisibility(View.GONE);
            break;
        case TIME_PICKER:
            datePicker.setVisibility(View.GONE);
            break;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        Builder builder = new AlertDialog.Builder(activity);

        // Set the layout for the dialog
        builder.setView(mView);

        // Set title of dialog
        builder.setMessage("When?")
                // Set Ok button
                .setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User ok the dialog
                            	if(DialogType == DATE_TIME_PICKER){
                            		TextView x = ((TextView)((RouteFragment)((MainActivity)activity).mainFragment).
                                    		rootView.findViewById(R.id.tv_time_start));
                            		x.setVisibility(View.GONE);
                            		x = ((TextView)((RouteFragment)((MainActivity)activity).mainFragment).
                            		rootView.findViewById(R.id.tv_time_start_up));
                            		x.setText(getFormattedDate());
                            		x.setVisibility(View.VISIBLE);
                            		x = ((TextView)((RouteFragment)((MainActivity)activity).mainFragment).
                            		rootView.findViewById(R.id.tv_time_start_down));
                            		x.setText(getTime());
                            		x.setVisibility(View.VISIBLE);
                            	}
                            	else if (DialogType == TIME_PICKER){
                            		TextView x = ((TextView)((RouteFragment)((MainActivity)activity).mainFragment).
                                    		rootView.findViewById(R.id.tv_time_end));
                                    x.setText(getTime());                            		
                            	}
                            }
                        })
                // Set Cancel button
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                                CustomDateTimeDialog.this.getDialog().cancel();
                            }
                        }); 

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        timePicker.setOnTimeChangedListener(this);
    }

    // Convenience wrapper for internal Calendar instance
    public int get(final int field) {
        return mCalendar.get(field);
    }

    // Convenience wrapper for internal Calendar instance
    public long getDateTimeMillis() {
        return mCalendar.getTimeInMillis();
    }

    // Convenience wrapper for internal TimePicker instance
    public void setIs24HourView(boolean is24HourView) {
        timePicker.setIs24HourView(is24HourView);
    }

    // Convenience wrapper for internal TimePicker instance
    public boolean is24HourView() {
        return timePicker.is24HourView();
    }

    // Convenience wrapper for internal DatePicker instance
    public void setCalendarViewShown(boolean calendarView) {
        datePicker.setCalendarViewShown(calendarView);
    }

    // Convenience wrapper for internal DatePicker instance
    public boolean CalendarViewShown() {
        return datePicker.getCalendarViewShown();
    }

    // Convenience wrapper for internal DatePicker instance
    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        datePicker.updateDate(year, monthOfYear, dayOfMonth);
    }

    // Convenience wrapper for internal TimePicker instance
    public void updateTime(int currentHour, int currentMinute) {
        timePicker.setCurrentHour(currentHour);
        timePicker.setCurrentMinute(currentMinute);
    }

    public String getDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        return sdf.format(mCalendar.getTime());
    }
    

    public String getDateTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM	", Locale.US);
        String d = sdf.format(mCalendar.getTime());
        sdf = new SimpleDateFormat("hh:mm aa", Locale.US);
        String t = sdf.format(mCalendar.getTime());
        return  d + "\n" + t;
    }
    
    public String getFormattedDate(){
    	SimpleDateFormat sdf = new SimpleDateFormat("dd MMM	", Locale.US);
        return sdf.format(mCalendar.getTime());
    }
    
    public String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa", Locale.US);
        return sdf.format(mCalendar.getTime());
    }
    
    public String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM", Locale.US);
        return sdf.format(mCalendar.getTime());
    }

    // Called every time the user changes DatePicker values
    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // Update the internal Calendar instance
        mCalendar.set(year, monthOfYear, dayOfMonth, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE));
    }

    // Called every time the user changes TimePicker values
    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        // Update the internal Calendar instance
        mCalendar.set(mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);
    }
}