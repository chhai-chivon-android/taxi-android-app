package com.sharedcab.batchcar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class HowToUse extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.how_to_use);
	    
	    final ImageView got_it = (ImageView)findViewById(R.id.got_it);
	    got_it.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				got_it.setVisibility(View.GONE);
				findViewById(R.id.spin_1).setVisibility(View.VISIBLE);
				Toast.makeText(HowToUse.this, "Taking you to main app...", Toast.LENGTH_SHORT).show();
				Intent i = new Intent(HowToUse.this,MainActivity.class);
				finish();
				startActivity(i);
			}
		});
	}

}
