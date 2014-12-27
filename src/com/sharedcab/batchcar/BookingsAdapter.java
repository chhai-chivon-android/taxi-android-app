package com.sharedcab.batchcar;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BookingsAdapter extends BaseAdapter{
	private Activity activity;
    private ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;
 
    public BookingsAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
        activity = a;
        data=d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
 
    public int getCount() {
        return data.size();
    }
 
    public Object getItem(int position) {
        return position;
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.booking_list_item, null);
 
        Drawable id;

        TextView pickup_add = (TextView)vi.findViewById(R.id.booking_address);
        TextView pickup_time = (TextView)vi.findViewById(R.id.booking_time);
        ImageView thumb_image=(ImageView)vi.findViewById(R.id.list_image); // thumb image

        HashMap<String, String> booking = new HashMap<String, String>();
        booking = data.get(position);
        String t = booking.get("type");
        if(t.equalsIgnoreCase("local"))
        	id = activity.getResources().getDrawable(R.drawable.localtrans);
        else if(t.equalsIgnoreCase("outstation"))
        	id = activity.getResources().getDrawable(R.drawable.outstationtrans);
        else
        	id = activity.getResources().getDrawable(R.drawable.citytaxitrans);
        
        thumb_image.setImageDrawable(id);
        pickup_time.setText(booking.get("datetime"));
        pickup_add.setText(booking.get("pickup"));
        return vi;
    }
    
}
