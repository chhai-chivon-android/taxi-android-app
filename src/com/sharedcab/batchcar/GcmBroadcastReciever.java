package com.sharedcab.batchcar;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmBroadcastReciever extends BroadcastReceiver {
    
	static final String TAG = "Batchcar";
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    Context ctx;
	SharedPreferences.Editor editor;
	SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.i("Batchcar","Recieved GCM Notification");
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        ctx = context;
        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            sendNotification("Send error: " + intent.getExtras().toString());
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            sendNotification("Deleted messages on server: " + 
                    intent.getExtras().toString());
        } else {
        	String tag = intent.getExtras().getString("tag");
        	if("verification_status".equals(tag)){
        		JSONObject c;
				try {
					c = new JSONObject(intent.getExtras().getString("customer"));
	        		Log.i(TAG,"Customer is: " + c.toString());
	        		int cid = c.getInt("id");
	        		String name = c.getString("first_name");
	        		String email = c.getString("email");
	        		String mobile = c.getString("mobile");
	        		prefs = ServerUtilities.getAppSharedPreferences(context);
	                editor = prefs.edit();
	                Log.i(TAG,"Details are: " + name + '\n' + email + '\n' + mobile);
	                editor.putString("com.sharedcab.batchcar.name", name);
	                editor.putString("com.sharedcab.batchcar.email", email);
	                editor.putString("com.sharedcab.batchcar.mobile", mobile);
	                editor.putInt("com.sharedcab.batchcar.customer_id",cid );
	                editor.putBoolean("com.sharedcab.batchcar.verified", true);
	                editor.commit();
	        		Log.i(TAG,"Verified Updated Shared prefs");

				} catch (JSONException e) {
	        		Log.i(TAG,"JSON parsing failed");
				}
        	}
        	else if ("booking_status".equals(tag)){
        		Log.i(TAG,"Booking status GCM Message");
        		sendNotification("Received: " + intent.getExtras().getString("message"));        		
        	}
        	else{
        		Log.i(TAG,"Random GCM Message");
        	}
        }
        setResultCode(Activity.RESULT_OK);
    }

    // Put the GCM message into a notification and post it.
    private void sendNotification(String msg) {
		  mNotificationManager = (NotificationManager)
		          ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		  
		  PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
		      new Intent(ctx, MainActivity.class), 0);
		  
		  NotificationCompat.Builder mBuilder =
		      new NotificationCompat.Builder(ctx)
		      .setSmallIcon(R.drawable.logo_minus_text)
		      .setContentTitle("Bathcar")
		      .setStyle(new NotificationCompat.BigTextStyle()
		                 .bigText(msg))
		      .setContentText(msg);
		  
		 mBuilder.setContentIntent(contentIntent);
		 mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}