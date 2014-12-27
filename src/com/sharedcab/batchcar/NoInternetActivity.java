package com.sharedcab.batchcar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class NoInternetActivity extends Activity {

	ConnectionDetector cd;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.no_internet_layout);

		cd = new ConnectionDetector(this);
	    ImageView iv = (ImageView) findViewById(R.id.check_internet_btn);
	    iv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				  if (cd.isConnectingToInternet()){
					  ServerUtilities.initGCM(NoInternetActivity.this);
					  Intent intent = new Intent();
					  intent.setClass(NoInternetActivity.this,MainActivity.class);
					  startActivity(intent);
					  NoInternetActivity.this.finish();
			      }
				  else{
					  Toast.makeText(NoInternetActivity.this, "Still not connected...", Toast.LENGTH_SHORT).show();
				  }
			}
		});
	}

}
