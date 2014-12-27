package com.sharedcab.batchcar;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DrawerAdapter extends BaseAdapter{
	private Activity activity;
    private ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;
 
    public DrawerAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
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
            vi = inflater.inflate(R.layout.drawer_list_row, null);
 
        Drawable id;

        TextView title = (TextView)vi.findViewById(R.id.title); // title
        ImageView thumb_image=(ImageView)vi.findViewById(R.id.list_image); // thumb image

        HashMap<String, String> menuItem = new HashMap<String, String>();
        menuItem = data.get(position);
        if(menuItem.get("icon") == "ride")
        	id = activity.getResources().getDrawable(R.drawable.book_a_ride);
        else if(menuItem.get("icon") == "bookings")
        	id = activity.getResources().getDrawable(R.drawable.booking_list2);
        else if (menuItem.get("icon") == "details")
        	id = activity.getResources().getDrawable(R.drawable.user_details);
        else
        	id = activity.getResources().getDrawable(R.drawable.help);;
        
        thumb_image.setImageDrawable(id);
        title.setText(menuItem.get("text"));
        return vi;
    }
}
