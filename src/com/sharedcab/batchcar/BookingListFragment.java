package com.sharedcab.batchcar;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ListView;

public class BookingListFragment extends ListFragment{
	
	Context mainContext;
	MainActivity mainActivity;
	String mobile;
    ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	BookingSummary bs;
	String host = "http://ektaxi-staging.herokuapp.com";
	String localhost = "http://192.168.1.8:9292";
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    mainActivity = (MainActivity) getActivity();
	    mainContext = mainActivity;
	    
	    SharedPreferences prefs;
        prefs = ServerUtilities.getAppSharedPreferences(mainContext);
        mobile = prefs.getString("com.sharedcab.batchcar.mobile", null);
	    
        if(list.isEmpty())
        	new TripGetterTask().execute();
	    
	  }

	  private final static String TAG_FRAGMENT = "TAG_FRAGMENT";
	
	  @Override
	  public void onListItemClick(ListView l, View v, int position, long id) {
	      final BookingSummary fragment = new BookingSummary(list.get(position).get("tid"));
	      final FragmentTransaction transaction = getFragmentManager().beginTransaction();
	      transaction.replace(R.id.content_frame, fragment, TAG_FRAGMENT);
	      transaction.addToBackStack(null);
	      transaction.commit();
	  }

	  
	  private class TripGetterTask extends AsyncTask<Void, Void, Boolean>{
     	  
		  @Override
		  protected void onPreExecute() {
			  super.onPreExecute();
//			  list.clear();
		  }
		  
          @Override
          protected Boolean doInBackground(Void... params) {
              return postData();
          }   

          public boolean postData() {
              HttpClient httpclient = new DefaultHttpClient();
              HttpClient httpclient2 = new DefaultHttpClient();
              
              HttpPost httppost = new HttpPost(host + "/staffs/login?email=foo@bar.com&password=foobar");
              HttpGet httpget_trips = new HttpGet(host + "/trips?trip[customer][mobile]="+mobile);
              Log.i("test", "in post data");
              try {
            	  HttpResponse response = httpclient.execute(httppost);
//            	  mainActivity.cabregator_cookies = ((AbstractHttpClient) httpclient).getCookieStore();
            	  
            	  ((AbstractHttpClient) httpclient2).setCookieStore(((AbstractHttpClient) httpclient).getCookieStore());
//            	  ((AbstractHttpClient) httpclient2).setCookieStore(mainActivity.cabregator_cookies);

            	  response.getEntity().consumeContent();

            	  response = httpclient.execute(httpget_trips);
            	  
            	  JSONArray o;
    	          ResponseHandler<String> handler = new BasicResponseHandler();
    	            try {
						o = new JSONArray(handler.handleResponse(response));
						Log.i("test","Trips for mobile no :" + mobile + " are: " + o.toString());
						for(int i=0;i<o.length();i++){
	    	            	HashMap<String,String> triphash = new HashMap<String,String>();
	    	            	JSONObject trip = (JSONObject) o.get(i);
	    	            	String datetime = trip.getString("datetime");
	    	            	String tid = trip.getString("id");
	    	            	String date = datetime.substring(0, 10);
	    	            	String time = datetime.substring(11, 19);
	    	            	String finalDT = getconvertdate(date+" "+time);
	    	            	String type = trip.getString("type");
	    	            	triphash.put("tid", tid);
	    	            	triphash.put("pickup",((JSONObject)trip.get("from_address")).getString("address"));
	    	            	triphash.put("drop",((JSONObject)trip.get("to_address")).getString("address"));
	    	            	triphash.put("datetime", finalDT);
	    	            	triphash.put("type",type);
	    	            	list.add(triphash);
	    	            }
						return true;
					} catch (JSONException e) {
						Log.i("test", "JSON response parsing failed!");
						e.printStackTrace();
						return false;
					}
    	            
              } catch (ClientProtocolException e) {
                  e.printStackTrace();
                  return false;
              } catch (IOException e) {
                  e.printStackTrace();
                  return false;
              }
          }
          
          @Override
          protected void onPostExecute(Boolean result) {
	        	super.onPostExecute(result);
	            Collections.reverse(list);
	        	BookingsAdapter adapter = new BookingsAdapter(mainActivity, list);
	    		setListAdapter(adapter);
	    		adapter.notifyDataSetChanged();
          }
          
          private String getconvertdate(String date)
          {
              SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//              inputFormat.setTimeZone(TimeZone.getTimeZone("IST"));
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
