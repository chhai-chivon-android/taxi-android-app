package com.sharedcab.batchcar;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateListenerService extends Service {

    private String LOG_TAG ="Service";
    boolean makingCall = false;
    
	@Override
	public IBinder onBind(Intent intent) {

		EndCallListener callListener = new EndCallListener();
		TelephonyManager mTM = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		mTM.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		return null;
	}
		
	private class EndCallListener extends PhoneStateListener {

		@Override
	    public void onCallStateChanged(int state, String incomingNumber) {
	        if(TelephonyManager.CALL_STATE_RINGING == state) {
	            Log.i(LOG_TAG, "RINGING, number: " + incomingNumber);
	        }
	        if(TelephonyManager.CALL_STATE_OFFHOOK == state) {
	            Log.i(LOG_TAG, "OFFHOOK");
	        }
	        if(TelephonyManager.CALL_STATE_IDLE == state) {
	            Log.i(LOG_TAG, "IDLE");
	            if(makingCall){
//	           	  The PendingIntent to launch our activity if the user selects this notification
	              PendingIntent contentIntent = PendingIntent.getActivity(PhoneStateListenerService.this, 0,
	                      new Intent(PhoneStateListenerService.this, MainActivity.class), 0);
	              try {
	            	  contentIntent.send();
	              } catch (CanceledException e) {
	            	  e.printStackTrace();
	              }

	            }
	        }
	    }
	}
	
}

