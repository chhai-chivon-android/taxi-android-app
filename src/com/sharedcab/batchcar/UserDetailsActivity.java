package com.sharedcab.batchcar;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class UserDetailsActivity extends Activity {

	SharedPreferences prefs;
	SharedPreferences.Editor editor;
	Context mainContext;
    MainActivity mainActivity;
	ConnectionDetector cd;
    AlertDialogManager alert = new AlertDialogManager();
	static View rootView;
	ProgressBar pb;
	private Dialog pd;
	
	private String tag = "Verifier";

	String host = "http://ektaxi-staging.herokuapp.com";
	String localhost = "http://192.168.1.8:9292";
	
	int cid=-1;
	String name="";
	String email="";
	String mobile="";
	String message="";
	String reg_id="";
	String new_name="";
	String new_email="";
	String new_mobile="";
	boolean verified=false;
	boolean makingcall = false;
	EndCallListener callListener;
	TelephonyManager mTM;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.user_details_activity);	
	    
	    callListener = new EndCallListener();
     	mTM = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        
//	    cd = new ConnectionDetector(this);
//        if (!cd.isConnectingToInternet()) {
//        	 AlertDialog.Builder builder = new AlertDialog.Builder(this);
//             builder.setMessage("Internet Problem")
//                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            finish();
//                        }
//                    })
//                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                        	finish();
//                        }
//                    });
//            builder.show();
//            return;
//        }
	    
        LinearLayout verifyBtn = (LinearLayout) findViewById(R.id.verify_btn);
        verifyBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				EditText et = (EditText) findViewById(R.id.et_name);
				et = (EditText) findViewById(R.id.et_phone);
				mobile = et.getText().toString();
				if("".equals(mobile)){
					Toast.makeText(UserDetailsActivity.this, "Please enter Mobile", Toast.LENGTH_SHORT).show();
					return;
				}
				new VerifyTask().execute(mobile);
			}
		});
        
        if(getIntent().getBooleanExtra("show_dialog", false)){
	    	new verTask().execute();
	    }
                
	}
	
	
	private class VerifyTask extends AsyncTask<String, Void, Void>{
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
  	    	pd.dismiss();
  	    	Log.i(tag,"Making call... in post execute..");
		}
		
		
		@Override         
        protected void onPreExecute(){
         	pd = ProgressDialog.show(UserDetailsActivity.this,"Please wait..", "Initiating verification call..");
         	
         	mTM.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);
  	    }
		
		@Override
		protected Void doInBackground(String... params) {
			
			if(createTempCustomer()){
				//start service
				Intent callIntent = new Intent(Intent.ACTION_CALL);
				callIntent.setData(Uri.parse("tel:+912230770284"));
				startActivity(callIntent);
			}
			
			return null;
		}
		
		private boolean createTempCustomer() {
			
			String url = "";
			Log.i(tag,"Creating customer");
			url = host+"/customers"; 
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpClient httpclient2 = new DefaultHttpClient();

			HttpPost auth_post = new HttpPost(host + "/staffs/login?email=admin@ektaxi.com&password=4dmi9");
			HttpPost customer_request = new HttpPost(url);

			HttpResponse auth_r;
			try {
				auth_r = httpclient.execute(auth_post);
				((AbstractHttpClient) httpclient2).setCookieStore(((AbstractHttpClient) httpclient).getCookieStore());
				auth_r.getEntity().consumeContent();
			} catch (Exception e1) {
				Log.i("Batchcar","Issues in logging in!");
				return false;
			}
			
			customer_request.setHeader("Accept", "application/json");
	        customer_request.setHeader("Content-type", "application/json");
//	        Log.i(tag,"Cookie Header is :" + ServerUtilities.cookie_header.getName() 
//	        		+ " : " + ServerUtilities.cookie_header.getValue());
//	        customer_request.setHeader(ServerUtilities.cookie_header);
	        reg_id = ServerUtilities.getRegistrationId(UserDetailsActivity.this);
        	try {
    			JSONObject o = new JSONObject();
    			JSONObject c = new JSONObject();
    			c.put("mobile", mobile);
    	        c.put("gcm_reg_id", reg_id);
    	        c.put("verified",false);
    	        o.put("customer", c);
    	        StringEntity se;
        		se = new StringEntity(o.toString());
	        	customer_request.setEntity(se);
				Log.i(tag,"New customer is: " + o.toString());
        	} catch (Exception e1) {
				Log.i(tag,"Some error in JSON parsing or entity encoding");
			}
	        
	        String response_string;
	        boolean status;
			try {
				HttpResponse response = httpclient.execute(customer_request);
				status = (response.getStatusLine().getStatusCode() == 200);
				ResponseHandler<String> handler = new BasicResponseHandler();
				response_string = handler.handleResponse(response);
		        Log.i("New Customer Response",response_string);
			} catch (Exception e) {
				status = false;
				Log.i(tag,"Error in customer request");
			}
			return status;
		}
	}


	private class EndCallListener extends PhoneStateListener {
		boolean makingcall = false;
		@Override
	    public void onCallStateChanged(int state, String incomingNumber) {
	        if(TelephonyManager.CALL_STATE_RINGING == state) {
	            Log.i(tag, "RINGING, number: " + incomingNumber);
	        }
	        if(TelephonyManager.CALL_STATE_OFFHOOK == state) {
	            Log.i(tag, "OFFHOOK");
	            makingcall = true;
	        }
	        if(TelephonyManager.CALL_STATE_IDLE == state) {
	        	if(makingcall){
		            Log.i(tag, "IDLE");
	            	Intent intent=new Intent(UserDetailsActivity.this,UserDetailsActivity.class);
	            	intent.putExtra("show_dialog", true);
	            	finish();
	            	startActivity(intent);
	            	makingcall = false;
	        	}
	        }
	    }
	}

	private class verTask extends AsyncTask<Void, Void, Void> {
    	int limit = 5;
    	int count = 0;
    	boolean found=false;
    	
    	@Override
    	protected void onPreExecute() {
    		// TODO Auto-generated method stub
    		super.onPreExecute();
    		pd = ProgressDialog.show(UserDetailsActivity.this,"Please wait..", "Checking Verification..");
    		count = 0;
    	}
    	
    	@Override
        protected Void doInBackground(Void... params) {
            try {
            	while(!ServerUtilities.getVerifiedStatus(UserDetailsActivity.this) && count < limit){
            		Thread.sleep(1000);
            		count++;
            	}
            	if(count == limit){
            		found = false;
            	}
            	else{
            		found = true;
            	}
            } 
            catch (InterruptedException e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        	pd.dismiss();
        	if(!found){
	        	AlertDialog.Builder builder = new AlertDialog.Builder(UserDetailsActivity.this);
	            builder.setTitle("Verification failed!")
	            .setMessage("")
	               .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                   }
	               })
	               .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   UserDetailsActivity.this.finish();
	                   }
	               }).show();
        	}
        	else{
            	Intent intent=new Intent(UserDetailsActivity.this,HowToUse.class);
            	finish();
            	startActivity(intent);
        	}
        }
    }


}
