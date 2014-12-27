package com.sharedcab.batchcar;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public final class Utilities {
		
	    static final String TAG = "Batchcar";

	    static AlertDialogManager alert = new AlertDialogManager();
	    
	    static String regid;

	    static Header cookie_header;
	    
	    static HttpClient main;
	    
	    static void loginBackground(final Context ctx){
	    	new AsyncTask<Void, Void, Boolean>(){
	    		 
				protected Boolean doInBackground(Void... params){
	    			HttpClient httpclient = new DefaultHttpClient();
	                HttpPost httppost = new HttpPost("http://ektaxi-staging.herokuapp.com/staffs/login?email=admin@ektaxi.com&password=4dmi9");
	                Log.i(TAG, "Logging in...");
	                try {
	              	  	HttpResponse response = httpclient.execute(httppost);
	              	  	ResponseHandler<String> handler = new BasicResponseHandler();
		              	Header[] headers=response.getAllHeaders();
		              	for(int i=0;i<headers.length;i++){
		              		if(headers[i].getName().equalsIgnoreCase("Set-Cookie")){
		    	              	cookie_header = headers[i];
		              			Log.i(TAG,"Cookie saved!");
		    	              	break;
		                    }
		              	}	              	
	              	  	return true;
	                }
	                catch(Exception e){
	                	Log.i(TAG,"Failed to log in!");
	                	Toast.makeText(ctx, "Sorry could not log you in!", Toast.LENGTH_SHORT).show();
	                	return false;
	                }
	    		}
			
	    	}.execute();
	    }
	    
	    static void updateUserGCM(final Context ctx){
	    	new AsyncTask<Void, Void, Boolean>(){
	    		 
				protected Boolean doInBackground(Void... params){
	    			SharedPreferences prefs = getAppSharedPreferences(ctx);
	    			String m = prefs.getString("com.sharedcab.batchcar.mobile", "");
	    			String r = prefs.getString("com.sharedcab.batchcar.reg_id", "");
	    			boolean v = prefs.getBoolean("com.sharedcab.batchcar.verified", false);
	    			int cid = prefs.getInt("com.sharedcab.batchcar.customer_id",-1);
	    			HttpClient httpclient = new DefaultHttpClient();
	    			if("".equals(m) || !v || cid == -1){
	    				Log.i(TAG,"Need for verification...");
	    				return false;
	    			}
	    			else{ 	//update his GCM id
			            HttpPost update_customer = new HttpPost("http://ektaxi-staging.herokuapp.com/customers/"+cid);
			            update_customer.setHeader("Accept", "application/json");
			            update_customer.setHeader("Content-type", "application/json");
				        update_customer.setHeader(cookie_header);
			        	try {
			    			JSONObject o = new JSONObject();
			    			JSONObject c = new JSONObject();
			    	        c.put("gcm_reg_id", r);
			    	        StringEntity se;
			        		se = new StringEntity(o.toString());
			        		update_customer.setEntity(se);
			        	} catch (Exception e1) {
							Log.i(TAG,"Some error in JSON parsing or entity encoding");
						}
				        
				        String response_string;
				        boolean status;
						try {
							HttpResponse response = httpclient.execute(update_customer);
							status = (response.getStatusLine().getStatusCode() == 200);
							ResponseHandler<String> handler = new BasicResponseHandler();
							response_string = handler.handleResponse(response);
					        Log.i("Update Customer Response",response_string);
						} catch (Exception e) {
							status = false;
							Log.i(TAG,"Error in Update Customer");
						}
						return status;
	    			}
	    		}
	    	}.execute();
	    }
		
	    static void fetchJSONPrefs(final Context ctx){
	    	new AsyncTask<Void, Void, Boolean>(){

				protected Boolean doInBackground(Void... params){
					HttpClient httpclient = new DefaultHttpClient();
					SharedPreferences prefs = getAppSharedPreferences(ctx);
		            HttpGet get_rates = new HttpGet("http://ektaxi-staging.herokuapp.com/staffs/get_rate_prefs");
			        String response_string="";
			        boolean status;
			        try {
						HttpResponse response = httpclient.execute(get_rates);
						status = (response.getStatusLine().getStatusCode() == 200);
						ResponseHandler<String> handler = new BasicResponseHandler();
						response_string = handler.handleResponse(response);
				        Log.i("Update Customer Response",response_string);
					} catch (Exception e) {
						status = false;
						Log.i(TAG,"Error in Update Customer");
					}
					try {
//						Log.i("Batchcar", "String from JSON Parsing is: " + response_string);

						JSONObject rates = new JSONObject(response_string);
						rates = rates.getJSONObject("rates");
						JSONArray city_taxi = rates.getJSONArray("city_taxi");
						JSONArray outstaion = rates.getJSONArray("outstation");
						JSONArray local = rates.getJSONArray("local");
						
						SharedPreferences.Editor editor = prefs.edit();
						editor.putString("com.sharedcab.batchcar.city_taxi", city_taxi.toString());
						editor.putString("com.sharedcab.batchcar.outstation", outstaion.toString());
						editor.putString("com.sharedcab.batchcar.local", local.toString());
						editor.commit();
						Log.i(TAG, "Big JSON parsed");
						status = true;
					} catch (JSONException e) {
						Log.i(TAG, "Big JSON parsing error");
						status = false;
					}
					
					return status;
	    		}
	    	}.execute();
	    }
	    
	    static public String loadJSONFromAsset(Context ctx) {
	        String json = null;
	        try {
	            InputStream is = ctx.getAssets().open("preferences.json");
	            int size = is.available();
	            byte[] buffer = new byte[size];
	            is.read(buffer);
	            is.close();
	            json = new String(buffer, "UTF-8");
	        } catch (IOException ex) {
	            ex.printStackTrace();
	            return null;
	        }
			Log.i("Batchcar", "JSON is: "+ json);
	        return json;
	    }
	    
		/**
		 * @return Application's {@code SharedPreferences}.
		 */
		public static SharedPreferences getAppSharedPreferences(Context context) {
		    return context.getSharedPreferences("com.batchcar.sharedcab", 
		            Context.MODE_PRIVATE);
		}
	    	
}
