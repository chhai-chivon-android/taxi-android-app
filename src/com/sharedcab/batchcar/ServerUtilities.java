package com.sharedcab.batchcar;

import java.io.IOException;
import java.sql.Timestamp;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;


public final class ServerUtilities {
	
    private static final String SENDER_ID = "183763060532";
    static final String TAG = "Batchcar";

    static AlertDialogManager alert = new AlertDialogManager();

    static GoogleCloudMessaging gcm;
    static SharedPreferences prefs;
    static String regid;
    
    public static final String EXTRA_MESSAGE = "com.batchcar.sharedcab.message";
    public static final String PROPERTY_REG_ID = "com.batchcar.sharedcab.reg_id";
    public static final String PROPERTY_APP_VERSION = "com.batchcar.sharedcab.appVersion";
    public static final String PROPERTY_ON_SERVER_EXPIRATION_TIME =
            "com.sharedcab.batchcar.onServerExpirationTimeMs";
    /**
     * Default lifespan (7 days) of a reservation until it is considered expired.
     */
    public static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 7;

    private static int count=0;
    
	public static Header cookie_header;	//cookie from ektaxi-staging server

    static void initGCM(Context context){

    	Log.i(TAG, "Initializing GCM...");
    	regid = getRegistrationId(context);
        if (regid.length() == 0) {
        	Log.i(TAG, "GCM not registered... First registering on GCM... ");
        	registerBackground(context);
        }
        else{
        	Log.i(TAG, "GCM registered... Calling initServer directly... ");
//    		initServerSession(context);
        }
        gcm = GoogleCloudMessaging.getInstance(context);
    }

    static void initServerSession(Context context){

    	Log.i(TAG, "Initializing Server Session...");

    	loginBackground(context);
    	checkUserBackground(context);
    }    
    
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
    
    static void checkUserBackground(final Context ctx){
    	new AsyncTask<Void, Void, Boolean>(){
    		 
			protected Boolean doInBackground(Void... params){
    			SharedPreferences prefs = getAppSharedPreferences(ctx);
    			String m = prefs.getString("com.sharedcab.batchcar.mobile", "");
    			String r = prefs.getString("com.sharedcab.batchcar.reg_id", "");
    			boolean v = prefs.getBoolean("com.sharedcab.batchcar.verified", false);
    			int cid = prefs.getInt("com.sharedcab.batchcar.customer_id",-1);
    			HttpClient httpclient = new DefaultHttpClient();
    			if("".equals(m) || !v || cid == -1){
    				//TODO: take him to reg page and disable navigation drawer
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

	static void registerBackground(final Context ctx) {
        new AsyncTask <Void, Void, String>() {
        	
            protected String doInBackground(Void... params) {
            	 String msg = "";
                 try {
                     if (gcm == null) {
                         gcm = GoogleCloudMessaging.getInstance(ctx);
                     }
                     regid = gcm.register(SENDER_ID);
                     msg = "Device registered, registration id=" + regid;
                     setRegistrationId(ctx, regid);
                     
                 } catch (IOException ex) {
                     msg = "Error :" + ex.getMessage();
                 }
                 return msg;
            }
            
        	
			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
                Log.i(TAG,result);
			}
         
        }.execute(null, null, null);
    }
    
	/**
	 * Stores the registration id, app versionCode, and expiration time in the
	 * application's {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration id
	 */
	private static void setRegistrationId(Context context, String regId) {
	    final SharedPreferences prefs = getAppSharedPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.v(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    long expirationTime = System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS;

	    Log.v(TAG, "Setting registration expiry time to " +
	            new Timestamp(expirationTime));
	    editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
	    editor.commit();
	}
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}

	/**
	 * Checks if the registration has expired.
	 *
	 * <p>To avoid the scenario where the device sends the registration to the
	 * server but the server loses it, the app developer may choose to re-register
	 * after REGISTRATION_EXPIRY_TIME_MS.
	 *
	 * @return true if the registration has expired.
	 */
	private static boolean isRegistrationExpired(Context ctx) {
	    final SharedPreferences prefs = getAppSharedPreferences(ctx);
	    // checks if the information is not stale
	    long expirationTime =
	            prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
	    return System.currentTimeMillis() > expirationTime;
	}
    
	/**
	 * Gets the current registration id for application on GCM service.
	 * <p>
	 * If result is empty, the registration has failed.
	 *
	 * @return registration id, or empty string if the registration is not
	 *         complete.
	 */
	public static String getRegistrationId(Context context) {
	    final SharedPreferences prefs = getAppSharedPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.length() == 0) {
	        Log.v(TAG, "Registration not found.");
	        return "";
	    }
	    // check if app was updated; if so, it must clear registration id to
	    // avoid a race condition if GCM sends a message
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion || isRegistrationExpired(context)) {
	        Log.v(TAG, "App version changed or registration expired.");
	        return "";
	    }
	    return registrationId;
	}

	public static boolean getVerifiedStatus(Context context){
		
		SharedPreferences prefs = getAppSharedPreferences(context);
		String m = prefs.getString("com.sharedcab.batchcar.mobile", "");
		boolean v = prefs.getBoolean("com.sharedcab.batchcar.verified", false);
		int cid = prefs.getInt("com.sharedcab.batchcar.customer_id",-1);
	
		if("".equals(m) || !v || cid == -1 ){
			Log.i(TAG,"Need for verification...");
			return false;
		}
		return true;
	}
	
	
	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	public static SharedPreferences getAppSharedPreferences(Context context) {
	    return context.getSharedPreferences("com.batchcar.sharedcab", 
	            Context.MODE_PRIVATE);
	}
    	
}