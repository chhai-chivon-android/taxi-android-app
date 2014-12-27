package com.sharedcab.batchcar;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.internal.am;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UserDetailsFragment extends Fragment {
	
	Context mainContext;
    MainActivity mainActivity;
	ConnectionDetector cd;
    AlertDialogManager alert = new AlertDialogManager();
	static View rootView;
	ProgressBar pb;
	private Dialog pd;
	
	private String tag = "Update Other Details";

	String host = "http://ektaxi-staging.herokuapp.com";
	String localhost = "http://192.168.1.8:9292";
	
	SharedPreferences prefs;
	SharedPreferences.Editor editor;
	int cid=-1;
	String name="";
	String email="";
	String reg_id="";
	String mobile="";
    	
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
        reg_id = ServerUtilities.getRegistrationId(mainContext);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    }
	
    @Override
    public void onResume() {
        super.onResume();
        Log.d("OnResume View", "Sucess");
//        prefs = ServerUtilities.getAppSharedPreferences(mainContext);
//        editor = prefs.edit();
//        name = prefs.getString("com.sharedcab.batchcar.name", "");
//        email = prefs.getString("com.sharedcab.batchcar.email", "");
//        cid = prefs.getInt("com.sharedcab.batchcar.customer_id", -1);
//        mobile = prefs.getString("com.sharedcab.batchcar.mobile", "");
//        TextView tv;
//        EditText et;
//        tv = (TextView) rootView.findViewById(R.id.tv_name);
//        et = (EditText) rootView.findViewById(R.id.et_name);
//        et.setVisibility(View.GONE);
//        tv.setText(name);
//        tv.setVisibility(View.VISIBLE);
//        tv = (TextView) rootView.findViewById(R.id.tv_email);
//        et = (EditText) rootView.findViewById(R.id.et_email);
//        et.setVisibility(View.GONE);
//        tv.setText(email);
//        tv.setVisibility(View.VISIBLE);
       
    }
    
    @Override
  	public void onActivityCreated(Bundle savedInstanceState) {
  		super.onActivityCreated(savedInstanceState);
        mainContext = getActivity();
	}
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
        rootView = inflater.inflate(R.layout.user_details_fragment, container, false);
        prefs = ServerUtilities.getAppSharedPreferences(mainContext);
        editor = prefs.edit();

        name = prefs.getString("com.sharedcab.batchcar.name", "");
        email = prefs.getString("com.sharedcab.batchcar.email", "");
        cid = prefs.getInt("com.sharedcab.batchcar.customer_id", -1);
        mobile = prefs.getString("com.sharedcab.batchcar.mobile", "");
        
        Log.i(tag,"Details are: " + name + "   " + email + "  " + cid + "  " + mobile);

        ((TextView) rootView.findViewById(R.id.tv_name)).setText(name);
        ((TextView) rootView.findViewById(R.id.tv_email)).setText(email);
        ((EditText) rootView.findViewById(R.id.et_name)).setText(name);
        ((EditText) rootView.findViewById(R.id.et_email)).setText(email);
        ((TextView) rootView.findViewById(R.id.tv_mobile)).setText(mobile);
        
        
        rootView.findViewById(R.id.edit_name).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        ((TextView) rootView.findViewById(R.id.tv_name)).setVisibility(View.GONE);
		        rootView.findViewById(R.id.et_name).setVisibility(View.VISIBLE);
			}
		});
        
		rootView.findViewById(R.id.edit_email).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        ((TextView) rootView.findViewById(R.id.tv_email)).setVisibility(View.GONE);
		        rootView.findViewById(R.id.et_email).setVisibility(View.VISIBLE);
			}
		});

        LinearLayout updateBtn = (LinearLayout) rootView.findViewById(R.id.update_btn);

        updateBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				EditText et = (EditText) rootView.findViewById(R.id.et_name);
				name = et.getText().toString();
				if("".equals(name)){
					Toast.makeText(mainContext, "Please enter Name", Toast.LENGTH_SHORT).show();
					return;
				}
				et = (EditText) rootView.findViewById(R.id.et_email);
				email = et.getText().toString();
				if("".equals(email)){
					Toast.makeText(mainContext, "Please enter Email", Toast.LENGTH_SHORT).show();
					return;
				}
				et = (EditText) rootView.findViewById(R.id.et_phone);
			
				new UpdateCustomerTask().execute();
			}
		});
        
        return rootView;
    }
	
		
	private class UpdateCustomerTask extends AsyncTask<String, Void, Boolean>{
	
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
  	    	pd.dismiss();
  	    	if(result){
  	    		Toast.makeText(mainContext, "Your details have been updated", Toast.LENGTH_SHORT).show();
  	    		editor.putString("com.sharedcab.batchcar.email", email);
  	    		editor.putString("com.sharedcab.batchcar.name", name);
  	    		editor.commit();
  	    	}
  	    	else{
  	    		Toast.makeText(mainContext, "Sorry, could not update your details", Toast.LENGTH_SHORT).show();
  	    	}
		}
		
		@Override         
        protected void onPreExecute(){
         	pd = ProgressDialog.show(mainContext,"Please wait..", "Updating details..");
  	    }
		
		@Override
		protected Boolean doInBackground(String... params) {
			
			return updateCustomerDetails(cid);
		}
		
		private boolean updateCustomerDetails(int cid) {
			
			String url = "";
			Log.i(tag,"Updating customer");
			url = host+"/customers/"+cid; 
			
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
        	try {
    			JSONObject o = new JSONObject();
    			JSONObject c = new JSONObject();
    			c.put("first_name", name);
    	        c.put("email", email);
    	        c.put("gcm_reg_id", reg_id);
    	        o.put("customer", c);
    	        StringEntity se;
        		se = new StringEntity(o.toString());
	        	customer_request.setEntity(se);
				Log.i(tag,"New customer is: " + o.toString());
        	} catch (Exception e1) {
				Log.i(tag,"Some error in JSON parsing or entity encoding");
				return false;
			}
	        
	        String response_string;
	        boolean status;
			try {
				HttpResponse response = httpclient.execute(customer_request);
				status = (response.getStatusLine().getStatusCode() == 200);
				ResponseHandler<String> handler = new BasicResponseHandler();
				response_string = handler.handleResponse(response);
		        Log.i("Update Customer Response",response_string);
			} catch (Exception e) {
				status = false;
				Log.i(tag,"Error in customer request");
			}
			return status;
		}

	}
}
