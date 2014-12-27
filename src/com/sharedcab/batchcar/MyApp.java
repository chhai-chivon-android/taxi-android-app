package com.sharedcab.batchcar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class MyApp extends Application{
	private static MyApp instance;

    public static MyApp getInstance() {
        return instance;
    }

    public static Context getContext(){
        return instance;
        // or return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
    }
}

//private void makeYtaxiBooking() {
//	SharedPreferences prefs;
//    prefs = mainActivity.getSharedPreferences("com.sharedcab.batchcar", 
//            Context.MODE_PRIVATE);
//    reg_id = prefs.getString("com.sharedcab.batchcar.reg_id", null);
//    mobile = prefs.getString("com.sharedcab.batchcar.mobile", null);
//    Log.i("test", "Reg Id is " + reg_id + " and mobile is " + mobile);
//    
//   
//    try {
//    	JSONObject from_map = new JSONObject();
//		from_map.put("name",from_ca.getAddressString());
//		from_map.put("latitude",from_ca.getLatitude());
//    	from_map.put("longitude",from_ca.getLongitude());
//    	JSONObject to_map = new JSONObject();
//    	if(trip_type == "local"){
//    		to_map.put("name",from_ca.getAddressString());
//        	to_map.put("latitude",from_ca.getLatitude());
//        	to_map.put("longitude",from_ca.getLongitude());
//    	}
//    	else{
//    		to_map.put("name",to_ca.getAddressString());
//        	to_map.put("latitude",to_ca.getLatitude());
//        	to_map.put("longitude",to_ca.getLongitude());	
//    	}
//    	JSONObject data_map = new JSONObject();
//    	data_map.put("car_type", car_type);
//    	data_map.put("package_type", package_type);
//    	data_map.put("number_of_days", numDays);
//    	trip_params.put("trip_id", "anbooking");
//    	trip_params.put("user_id", mobile);
//    	trip_params.put("trip_at", datetime);
//    	trip_params.put("trip_type", trip_type);
//    	trip_params.put("callback_url","www.google.com");
//    	trip_params.put("live", true);
//    	trip_params.put("from",from_map);
//    	trip_params.put("to", to_map);
//    	trip_params.put("data", data_map);
//    	if (reg_id == null)
//    		reg_id = mainActivity.regid;
//    	trip.put("reg_id", reg_id);
//    	trip.put("trip", trip_params);
//    	
//	} catch (JSONException e) {
//		e.printStackTrace();
//	}
//	new TripbookerTask(1).execute(trip);
//}