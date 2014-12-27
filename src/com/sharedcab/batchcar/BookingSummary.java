package com.sharedcab.batchcar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class BookingSummary extends Fragment {

	Context mainContext;
    MainActivity mainActivity;
	ConnectionDetector cd;
    AlertDialogManager alert = new AlertDialogManager();
	static View rootView;
	
	//details
	HashMap <String,String> trip = new HashMap<String,String>();
	JSONObject trip_details = new JSONObject();
	String d_name = "";
	String d_no = "";
    String live = "";
    String bid = "";
    String bbid = "";
    String tid = "";
    String status ="";
    String vendor="";
    String type = "";
	String cancellable="";
	String d = "";
	String pt = "";
	String ct = "";
	String pickup="";
	String drop ="";
	String datetime = "";
    ProgressDialog pd;
    
	String host = "http://ektaxi-staging.herokuapp.com";
	String localhost = "http://192.168.1.8:9292";
    
    //for status control
	private int m_interval = 20000;
	private Handler m_handler;
	int count = 0;
	
	public BookingSummary(String tid_from_list) {
		tid = tid_from_list;
	}	
	
	Runnable tripStatusUpdater = new Runnable()
	{
	     @Override 
	     public void run() {
	    	 new TripStatusTask(1).execute();
	         m_handler.postDelayed(tripStatusUpdater, m_interval);
	     }
	};

	void startRepeatingTask()
	{
	    Log.i("test","Here, to start repeating task for status update!");
		tripStatusUpdater.run(); 
	}

	void stopRepeatingTask()
	{
	    Log.i("test","Here, to stop repeating task for status update!");
	    m_handler.removeCallbacks(tripStatusUpdater);
	}

	@Override
    public void onAttach(Activity a){
    	super.onAttach(a);
    	mainContext = a;
        mainActivity = (MainActivity) getActivity();
        cd = new ConnectionDetector(mainContext);
        if (!cd.isConnectingToInternet()) {
            alert.showAlertDialog(mainContext,"Internet Connection Error",
                    "Please connect to working Internet connection and restart the app",false);
            return;
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    }
	
    @Override
    public void onResume() {
        super.onResume();
        Log.d("OnResume View", "Sucess");
    }
    
    @Override
  	public void onActivityCreated(Bundle savedInstanceState) {
  		super.onActivityCreated(savedInstanceState);
        mainContext = getActivity();
	}
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.i("TEST", " in on pause");
//    	stopRepeatingTask();
    }
    
    @Override
    public void onStop(){
    	super.onPause();
    	Log.i("TEST", " in on stop");
    	stopRepeatingTask();
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.booking_summary, container, false);
        m_handler = new Handler();
    	count = 0;
        getSummaryData();	//fill details in trip_details
	   
        return rootView;
    }
	
	private void getSummaryData(){
		Log.i("TEST","I am here!");
		new TripStatusTask(0).execute();		
	}

	private void setSummaryBox() {
		
	    TextView tv = (TextView) rootView.findViewById(R.id.tv_pickup);
	    tv.setText(pickup);
	    tv = (TextView) rootView.findViewById(R.id.tv_drop);
	    tv.setText(drop);
	    tv = (TextView) rootView.findViewById(R.id.tv_details);
	    tv.setText(datetime);
	    tv = (TextView) rootView.findViewById(R.id.car_vendor_text);
	    Log.i("test","Car type is : " + ct);
	    
	    if("indica".equals(ct)){
        	tv.setText("Compact");
	    }
	    else if ("tavera".equals(ct)){
			tv.setText("Tavera");
    	}
	    else if ("innova".equals(ct)){
        	tv.setText("Innova");
	    }
        else{
        	if("city_taxi".equals(type)){
        		tv.setText(vendor.toUpperCase());
        	}
        	else{
        		tv.setText("Sedan");
        	}
	    }
	    tv = (TextView) rootView.findViewById(R.id.tv_pdct);
	    if("city_taxi".equals(type)){
	    	tv.setText("City Taxi");
	    }
	    else if ("outstation".equals(type)){
	    	tv.setText("Outstation");
	    }
	    else{
	    	tv.setText("Local");
	    }

	    LinearLayout summary_box = (LinearLayout) rootView.findViewById(R.id.summary_box);
	    ProgressBar pb = (ProgressBar) rootView.findViewById(R.id.pb_summary);
	    pb.setVisibility(View.INVISIBLE);
	    summary_box.setVisibility(View.VISIBLE);
	    
	    startRepeatingTask();
	    updateCancelAndCallButton();		     	
	}

	public void updateCancelAndCallButton() {
		String text ="";
	    Drawable cd = mainActivity.getResources().getDrawable(R.drawable.call);
	    ImageView iv = (ImageView) rootView.findViewById(R.id.call_img);
	    LinearLayout call_btn = (LinearLayout) rootView.findViewById(R.id.btn_call);
	    
	    Log.i("TEST","STATUS IS "+ status + " LIVE IS " + live);
	    if("cancelled".equals(status) || "to be cancelled".equals(status)){
	    	Log.i("test","in cancelled");
	    	text = "CANCELLED";
	    	rootView.findViewById(R.id.separator9).setVisibility(View.GONE);
	    	rootView.findViewById(R.id.button_box).setVisibility(View.GONE);
        	rootView.findViewById(R.id.pb_status).setVisibility(View.INVISIBLE);
			rootView.findViewById(R.id.call_img).setVisibility(View.VISIBLE);
			stopRepeatingTask();
	    }
	    else{
	    	Log.i("test","in not cancelled");
	    	if("".equals(vendor)){
	    		text = "TRIP NOT VALID";
	    		rootView.findViewById(R.id.separator9).setVisibility(View.GONE);
		    	rootView.findViewById(R.id.button_box).setVisibility(View.GONE);
		    	rootView.findViewById(R.id.pb_status).setVisibility(View.INVISIBLE);
				rootView.findViewById(R.id.call_img).setVisibility(View.VISIBLE);
				stopRepeatingTask();
	    	}
    		else if ("pending".equals(status)){
    			text = "PROCESSING...";
    			rootView.findViewById(R.id.separator9).setVisibility(View.GONE);
    	    	rootView.findViewById(R.id.button_box).setVisibility(View.GONE);
	        	rootView.findViewById(R.id.pb_status).setVisibility(View.VISIBLE);
				rootView.findViewById(R.id.call_img).setVisibility(View.INVISIBLE);
    		}
    		else if("false".equals(live)){
		    	Log.i("test","in not live");
	    		text = "TRIP NOT ACTIVE";
		    	rootView.findViewById(R.id.separator9).setVisibility(View.GONE);
		    	rootView.findViewById(R.id.button_box).setVisibility(View.GONE);
		    	rootView.findViewById(R.id.pb_status).setVisibility(View.INVISIBLE);
				rootView.findViewById(R.id.call_img).setVisibility(View.VISIBLE);
				stopRepeatingTask();
	    	}
	    	else{
		    	Log.i("test","in live");
	    		LinearLayout cancel_btn;
	    	    if(rootView.findViewById(R.id.cancel_btn) != null){
	    	    	cancel_btn = (LinearLayout) rootView.findViewById(R.id.cancel_btn);
	    	    	cancel_btn.setOnClickListener(new OnClickListener() {
	    				@Override
	    				public void onClick(View v) {
	    					Log.i("test","Cancelling trip: " + bid);
	    					new TripCancellerTask().execute(bid);
	    					
	    				}
	    			});
	    	    }
	    	    rootView.findViewById(R.id.pb_status).setVisibility(View.INVISIBLE);
				rootView.findViewById(R.id.call_img).setVisibility(View.VISIBLE);
	    		if(!"".equals(d_no)){
	    			text = "CALL DRIVER";
	    	    	iv.setImageDrawable(cd);
	    			call_btn.setOnClickListener(new OnClickListener() {
	    				@Override
	    				public void onClick(View v) {
	    				 	Intent callIntent = new Intent(Intent.ACTION_DIAL);
	    				    callIntent.setData(Uri.parse("tel:+91"+d_no));
	    				    startActivity(callIntent);
	    				}
	    			});
	    			stopRepeatingTask();
	    		}
	    		else{
	    			text = "DRIVER NOT ASSIGNED YET";
	    		}
	    	}
	    }
	    TextView tv = (TextView)rootView.findViewById(R.id.call_driver);
	    tv.setText(text);
	}

	
	private class TripCancellerTask extends AsyncTask<String, Void, Boolean>{
   	  
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pd = ProgressDialog.show(mainContext, "Cancelling...", "Cancelling trip...", true);
		}
		
        @Override
        protected Boolean doInBackground(String... params) {
        	boolean status = true;
        	for(int i=0;i<params.length;i++){
        		status &= postData(params[0]);
        	}
            return status;
        }   

        public boolean postData(String booking_id) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpClient httpclient2 = new DefaultHttpClient();
            
            HttpPost httppost = new HttpPost(host+"/staffs/login?email=admin@ektaxi.com&password=4dmi9");
            HttpPost httppost_trips = new HttpPost(host + "/bookings/"+booking_id+"/cancel");
            Log.i("test", "in post data");
            try {
	          	  HttpResponse response = httpclient.execute(httppost);
	          	  ((AbstractHttpClient) httpclient2).setCookieStore(((AbstractHttpClient) httpclient).getCookieStore());
            	  response.getEntity().consumeContent();

	          	  response = httpclient.execute(httppost_trips);
	          	  
	          	  if(response.getStatusLine().getStatusCode() == 200)
	          		  return true;
	          	  else
	          		  return false;

            } catch (Exception e) {
            	Log.i("test","Trip could not be cancelled!");
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
	        	super.onPostExecute(result);
        		pd.dismiss();
	        	if(result){
	        		String text;
	        	    Drawable ex = mainActivity.getResources().getDrawable(R.drawable.exclamation);
	        	    ImageView iv = (ImageView) rootView.findViewById(R.id.call_img);
	        		text = "CANCELLED";
	    	    	rootView.findViewById(R.id.separator9).setVisibility(View.GONE);
	    	    	rootView.findViewById(R.id.button_box).setVisibility(View.GONE);
	    	    	iv.setImageDrawable(ex);
	    	    	TextView tv = (TextView)rootView.findViewById(R.id.call_driver);
	    		    tv.setText(text);
	        		Toast.makeText(mainContext, "Your booking has been successfully cancelled!",Toast.LENGTH_SHORT).show();
	        	}
	        	else{
	        		Toast.makeText(mainContext, "Sorry we could not cancel your booking!",Toast.LENGTH_SHORT).show();
	        	}
        }
    }

	private class TripStatusTask extends AsyncTask<Void, Void, Boolean>{

		int whichTask;
		
		public TripStatusTask(int x){
            Log.i("test", "here2 ");
			whichTask = x;
		}
		
        @Override
        protected Boolean doInBackground(Void... params) {
            return getStatusOfBooking();
        }   

        private boolean getStatusOfBooking() {
            HttpClient httpclient = new DefaultHttpClient();
            HttpClient httpclient2 = new DefaultHttpClient();
            
            HttpPost httppost = new HttpPost(host + "/staffs/login?email=admin@ektaxi.com&password=4dmi9");
            HttpGet httpget_bookings = new HttpGet(host + "/trips/"+tid);
            Log.i("test","Trip id is: "+ tid);
            Log.i("test", "in get status");
            Log.i("test", "here3");
            try {
	          	  HttpResponse response = httpclient.execute(httppost);
	          	  ((AbstractHttpClient) httpclient2).setCookieStore(((AbstractHttpClient) httpclient).getCookieStore());
            	  response.getEntity().consumeContent();
	          	  response = httpclient.execute(httpget_bookings);
	          	  JSONObject o = new JSONObject();
	  	          ResponseHandler<String> handler = new BasicResponseHandler();
	              Log.i("test", "here4 ");
	  	          try {
						trip_details = new JSONObject(handler.handleResponse(response));
						Log.i("test","\n\n\nResponse1  is: " + trip_details.toString());
						pickup = trip_details.getJSONObject("from_address").getString("address");
    	            	drop = trip_details.getJSONObject("to_address").getString("address");
    	            	datetime = trip_details.getString("datetime");
    	            	String date = datetime.substring(0, 10);
    	            	String time = datetime.substring(11, 19);
    	            	datetime = getconvertdate(date+" "+time);
    	            	type = trip_details.getString("type");
						if (trip_details.getJSONArray("bookings").length()>0){
							o = (JSONObject) trip_details.getJSONArray("bookings").get(0);
							Log.i("test","\n\n\nResponse2 is: " + o.toString());
							bid = o.getString("id");
							bbid = o.getString("booking_id");
							status = o.getString("status");
							live = o.getString("live");
							vendor = o.getString("vendor");
							JSONObject b_data = new JSONObject();
							b_data = o.getJSONObject("data");

							try{
								d = b_data.getJSONObject("data").getString("days");
							}
							catch (Exception e) {
								d="";
	    	            		Log.i("test","No days found!");
							}
							try{
		    	            	pt = b_data.getJSONObject("data").getString("package_type");
							}
							catch (Exception e) {
								pt = "";
	    	            		Log.i("test","No package found!");
							}
	    	            	try{
	    	            		ct = b_data.getJSONObject("data").getJSONArray("car_type").getString(0);
	    	            	}
	    	            	catch(Exception e){
	    	            		ct="";
	    	            		Log.i("test","No cars found!");
	    	            	}
	    	            	try{
		    	            	d_name = b_data.getJSONObject("assigned_driver").getString("name");
	    	            		d_no = b_data.getJSONObject("assigned_driver").getString("phone_number");
	    	            	}
	    	            	catch(Exception e){
	    	            		Log.i("test","No driver found!");
	    	            		d_name = "";
	    	            		d_no = "";
	    	            	}
    	            	}
						Log.i("test","Booking id: " + bid + " status is: " + status + " live is: " + live);
						return true;
	  	          } catch (JSONException e) {
						Log.i("test", "JSON response parsing failed!");
						e.printStackTrace();
						return false;
	  	          }
            } catch (Exception e) {
            	Log.i("test","Status could not be updated!");
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
        	super.onPostExecute(result);
        	if(result){
        		if(whichTask == 0)
        			setSummaryBox();
        		else
        			updateCancelAndCallButton();
	        	count++;
	        	if (count > 30)
		        	 stopRepeatingTask();
        	}
        	if(!"".equals(bbid)){
        		String x = bbid;
        		if("ytaxi".equals(vendor))
        				x = "BCAR" + x;
        		((TextView)rootView.findViewById(R.id.tv_tab)).setText(x);
        	}
        }
        
        private String getconvertdate(String date){
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM hh:mm aa");
            Date parsed = new Date();
            try
            {
                parsed = inputFormat.parse(date);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            String outputText = outputFormat.format(parsed);
            return outputText;
        }
        
    }
	
}
