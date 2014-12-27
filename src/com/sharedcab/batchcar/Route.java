package com.sharedcab.batchcar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class Route extends FragmentActivity {
	
	GoogleMap googleMap;
    LatLng latLng;
    Context context;
    Address drop = new Address(new Locale("en","INDIA"));
    LatLng center;
    MarkerOptions toMarker;
    MarkerOptions fromMarker;
    String approx_cost;
    ProgressDialog pd;
    JSONObject trip_params;
    JSONObject trip;
    CustomAddress from;
    CustomAddress to;
    String datetime = "2013-05-21 00:00";
	String mobile = "8424815838";
	String trip_type = "city_taxi";
	String package_type = ""; 
	private String[] mPlanetTitles;
    private ListView mDrawerList;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.route_fragment);
        context = Route.this;
	
        Intent i = getIntent();
        from = i.getExtras().getParcelable("pickup_address");
        to = i.getExtras().getParcelable("drop_address");
        
	    LatLng fromPosition = new LatLng(Double.parseDouble(from.getLatitude()), Double.parseDouble(from.getLongitude()));
	    LatLng toPosition = new LatLng(Double.parseDouble(to.getLatitude()), Double.parseDouble(to.getLongitude()));
//	    SupportMapFragment supportMapFragment = (SupportMapFragment)
//	    		getSupportFragmentManager().findFragmentById(R.id.map_route);
	        
//	    googleMap = supportMapFragment.getMap();
        googleMap.moveCamera( CameraUpdateFactory.newLatLngZoom
        		(new LatLng((fromPosition.latitude+toPosition.latitude)/2, (fromPosition.longitude+toPosition.longitude)/2),13.0f) );
        toMarker = new MarkerOptions();
        fromMarker = new MarkerOptions();
        toMarker.position(toPosition);
        fromMarker.position(fromPosition);
        googleMap.addMarker(toMarker);        
        googleMap.addMarker(fromMarker);
        new DirectionPlotter().execute(fromPosition,toPosition);
        
        Button confirm_btn = (Button) findViewById(R.id.btn_confirm);

	    confirm_btn.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	trip_params = new JSONObject();
	        	trip = new JSONObject();
	        	JSONObject from_map = new JSONObject();
	        	try {
					from_map.put("name",from.getAddressString());
					from_map.put("latitude",from.getLatitude());
		        	from_map.put("longitude",from.getLongitude());
		        	JSONObject to_map = new JSONObject();
		        	to_map.put("name",to.getAddressString());
		        	to_map.put("latitude",to.getLatitude());
		        	to_map.put("longitude",to.getLongitude());
		        	JSONObject data_map = new JSONObject();
		        	data_map.put("car_type", "indica,etios,logan");
		        	data_map.put("package_type", "");
		        	trip_params.put("trip_id", "anbooking");
		        	trip_params.put("user_id", mobile);
		        	trip_params.put("trip_at", datetime);
		        	trip_params.put("trip_type", trip_type);
		        	trip_params.put("callback_url","www.google.com");
		        	trip_params.put("live", true);
		        	trip_params.put("from",from_map);
		        	trip_params.put("to", to_map);
		        	trip_params.put("data", data_map);
		        	trip.put("trip", trip_params);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	new TripbookerTask().execute(trip);
	        }
	    });	

	}
	
	private class DirectionPlotter extends AsyncTask<LatLng, Void, ArrayList<LatLng>>{
		 
        @Override
        protected ArrayList<LatLng> doInBackground(LatLng... locations) {
        	GMapDirection md = new GMapDirection();
		    Document doc = md.getDocument(locations[0], locations[1], GMapDirection.MODE_DRIVING);
		    return md.getDirection(doc);
        }
 
        @Override
        protected void onPostExecute(ArrayList<LatLng> directionPoints) {
 
            if(directionPoints==null || directionPoints.size()==0){
                Toast.makeText(getBaseContext(), "Sorry! Directions could not be found", Toast.LENGTH_SHORT).show();
            }
            else{
            	PolylineOptions rectLine = new PolylineOptions().width(4).color(Color.BLUE);
    		    for(int j = 0 ; j < directionPoints.size() ; j++) {          
    		    	rectLine.add(directionPoints.get(j));
    		    }
    		    googleMap.addPolyline(rectLine);
            } 
        }
    }
	
    private class TripbookerTask extends AsyncTask<JSONObject, Void, Boolean>{
    	 
        @Override
        protected Boolean doInBackground(JSONObject... trip_details) {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPost httpost = new HttpPost("http://192.168.1.9:5000/api/1/trips.json");
			try {
				StringEntity se = new StringEntity(trip_details[0].toString());
	            httpost.setEntity(se);
	            httpost.setHeader("Accept", "application/json");
	            httpost.setHeader("Content-type", "application/json");
	            HttpResponse r = httpclient.execute(httpost); 
	        	if (r.getStatusLine().getStatusCode() == 202)
	        		return true;
	        	else
	        		return false;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Toast.makeText(getBaseContext(), "Sorry, some error occurred while making the booking. " +
						"Please try again later", Toast.LENGTH_SHORT).show();
				return false;
			}
        }
 
        @Override
        protected void onPostExecute(Boolean status) {
 	    	pd.dismiss();
 	    	//TODO add status
 	    	if (status)
 	    		Toast.makeText(getBaseContext(), "Your booking has number is ", Toast.LENGTH_SHORT).show();
 	    	else
 	    		Toast.makeText(getBaseContext(), "Booking failed! ", Toast.LENGTH_SHORT).show();
        }
        
        protected void onPreExecute(){
        	pd = ProgressDialog.show(Route.this,"Please wait..", "Logging in..");
 	    }
        
        private JSONObject getJsonObjectFromMap(Map params) throws JSONException {

            Iterator iter = params.entrySet().iterator();
            JSONObject holder = new JSONObject();

            //While there is another entry
            while (iter.hasNext()) 
            {
                Map.Entry pairs = (Map.Entry)iter.next();
                String key = (String)pairs.getKey();
                Map m = (Map)pairs.getValue();   
                JSONObject data = new JSONObject();

                Iterator iter2 = m.entrySet().iterator();
                while (iter2.hasNext()) 
                {
                    Map.Entry pairs2 = (Map.Entry)iter2.next();
                    data.put((String)pairs2.getKey(), (String)pairs2.getValue());
                }
                holder.put(key, data);
            }
            return holder;
        }
    }
    
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        // Create a new fragment and specify the planet to show based on position
//        Fragment fragment = new PlanetFragment();
//        Bundle args = new Bundle();
//        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
//        fragment.setArguments(args);
//
//        // Insert the fragment by replacing any existing fragment
//        FragmentManager fragmentManager = getFragmentManager();
//        fragmentManager.beginTransaction()
//                       .replace(R.id.content_frame, fragment)
//                       .commit();
//
//        // Highlight the selected item, update the title, and close the drawer
//        mDrawer.setItemChecked(position, true);
//        setTitle(mPlanetTitles[position]);
//        mDrawerLayout.closeDrawer(mDrawer);
    }

    @Override
    public void setTitle(CharSequence title) {
//        mTitle = title;
//        getActionBar().setTitle(mTitle);
    }
    
}
