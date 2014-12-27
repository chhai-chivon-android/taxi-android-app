package com.sharedcab.batchcar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Splash extends Activity{

	SharedPreferences prefs;
	SharedPreferences.Editor editor;
	ConnectionDetector cd;
	
	public void onCreate(Bundle savedInstanceState) {
	  	  super.onCreate(savedInstanceState);
	  	  setContentView(R.layout.splash);
		  SplashHandler mHandler = new SplashHandler();
		  Message msg = new Message();
	  	  
	  	  prefs = ServerUtilities.getAppSharedPreferences(this);
	  	  editor = prefs.edit();
	  	  
	  	  boolean first_use = prefs.getBoolean("com.sharedcab.batchcar.first_use", true);
	  	  if (first_use) {
	  		  setSimNo();
		  	  editor.putBoolean("com.sharedcab.batchcar.first_use", false);
		  	  editor.commit();
		  }
	  	  boolean simChanged = getSimChangedStatus();
		  boolean verified = ServerUtilities.getVerifiedStatus(this);
		  Log.i("Batchcar","Verified Staus:" + verified);
		  
		  cd = new ConnectionDetector(this);
		  if (!cd.isConnectingToInternet()) {
			  msg.what = 2;
	      }
		  else {
			  ServerUtilities.initGCM(this);
			  if(first_use || !verified || simChanged){
				  Log.i("Batchcar",first_use + "   " + !verified + "  " + simChanged);
				  msg.what = 0;
				  Log.i("Batchcar", "Here1");
			  }
			  else
				  msg.what = 1;
		  }
		  
		  mHandler.sendMessageDelayed(msg, 3000);
	}

	private class SplashHandler extends Handler {

    	public void handleMessage(Message msg){
		  	Intent intent = new Intent();
    		switch (msg.what){
				case 0:
					super.handleMessage(msg);
					intent.setClass(Splash.this,UserDetailsActivity.class);
					intent.putExtra("show_dialog", false);
					startActivity(intent);
					Splash.this.finish();
					break;
				case 1:
					super.handleMessage(msg);
					intent.setClass(Splash.this,MainActivity.class);
				    startActivity(intent);
				    Splash.this.finish();
					break;
				case 2:
					super.handleMessage(msg);
					intent.setClass(Splash.this,NoInternetActivity.class);
					startActivity(intent);
					Splash.this.finish();
					break;
				default:
					super.handleMessage(msg);
					intent.setClass(Splash.this,MainActivity.class);
					startActivity(intent);
					Splash.this.finish();
					break;
			}
    	}
    }
	
	private void setSimNo() {
		TelephonyManager teleManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String sim_no = teleManager.getSimSerialNumber();
		editor.putString("com.sharedcab.batchcar.sim_no", sim_no);
		editor.commit();
	}

	private boolean getSimChangedStatus() {
		TelephonyManager teleManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String c_sim_no = teleManager.getSimSerialNumber();
		String sim_no = prefs.getString("com.sharedcab.batchcar.sim_no", "");
		if(c_sim_no.equals(sim_no))
			return false;
		else
			return true;
	}
}

