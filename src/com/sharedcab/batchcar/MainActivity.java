/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sharedcab.batchcar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.datatype.Duration;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class MainActivity extends Activity {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mFrags;
    
    public CustomAddress pickup;
    public CustomAddress drop;
    
    String GCM_SENDER_ID = "183763060532";

    AlertDialogManager alert = new AlertDialogManager();

    // Tag for log messages.
    static final String TAG = "Batchcar";

    GoogleCloudMessaging gcm;
    SharedPreferences prefs;
    String regid;

    Fragment bookinglistFragment,favListfragment;
    Fragment mainFragment = new RouteFragment();
    Fragment df = new DropFragment();
    Fragment pf = new PickupFragment();
    Fragment prf = new PrimerFragment();
    ListFragment bf = new BookingListFragment();
    Fragment uf = new UserDetailsFragment();
    
    CookieStore cabregator_cookies;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);
        
        mTitle = mDrawerTitle = getTitle();
        mFrags = getResources().getStringArray(R.array.items_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        setNewListAdapter();
        
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }
        Utilities.fetchJSONPrefs(this);
    }
    
    private void setNewListAdapter() {
    	ListView list;
    	DrawerAdapter adapter;
    	ArrayList<HashMap<String, String>> menuList = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("text", "Book a Ride");
        map.put("icon","ride");
        menuList.add(map);
        map = new HashMap<String, String>();
        map.put("text", "Booking History");
        map.put("icon","bookings");
        menuList.add(map);
        map = new HashMap<String, String>();
        map.put("text", "Account Details");
        map.put("icon","details");
        menuList.add(map);
        map = new HashMap<String, String>();
        map.put("text", "How to Use?");
        map.put("icon","help");
        menuList.add(map);
        
		list=(ListView)findViewById(R.id.left_drawer);
		adapter = new DrawerAdapter(this, menuList);
		list.setAdapter(adapter);
	}
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
            return super.onOptionsItemSelected(item);
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
    	//dont add them to back stack as these are all main pages
    	
    	FragmentManager fragmentManager = getFragmentManager();
    	switch(position){
    	case 1:
    		fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    		fragmentManager.beginTransaction().replace(R.id.content_frame, bf).commit();
    		break;
    	case 2:
    		fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    		fragmentManager.beginTransaction().replace(R.id.content_frame, uf).commit();
    		break;
    	case 3:
    		fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        	fragmentManager.beginTransaction().replace(R.id.content_frame, prf).commit();
        	break;
        default:
    		fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    		fragmentManager.beginTransaction().replace(R.id.content_frame, mainFragment).commit();
    		break;
    	}

        mDrawerList.setItemChecked(position, true);
        setTitle(mFrags[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    	
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Log.i(TAG,"Here");
        // Pass any configuration change to the drawer toggle
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    public void onBackPressed() {
    	
    	if(getFragmentManager().getBackStackEntryCount()==0){
    		Log.i("TEST FINAL","Back stack count zero");
    		new AlertDialog.Builder(this)
	            .setIcon(android.R.drawable.ic_dialog_alert)
	            .setTitle("Closing Activity")
	            .setMessage("Are you sure you want to exit?")
	            .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
		            @Override
		            public void onClick(DialogInterface dialog, int which) {
		                finish();    
		            }
	            })
	        .setNegativeButton("No", null)
	        .show();
    	}
    	else{
    		getFragmentManager().popBackStack();
    		Log.i("Batchcar","Length of back stack is: " + getFragmentManager().getBackStackEntryCount());
    	}
    }
    
}