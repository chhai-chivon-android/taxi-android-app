package com.sharedcab.batchcar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import at.bookworm.widget.TabControlButton;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;


/* NOTE:
 * 
 * This class is the heart of the app. It handles local, outstation and 
 * city taxi bookings along with car types and vendors. All of this is managed in the same
 * fragment. This needs a lot of refactoring. In the idle scenario, the three
 * types of trips should have respective fragments and layouts. This will also help to
 * remove all the code to control views which is called when the radiogroup check is changed etc.
 * All that is now being taken care by hiding/unhiding views.
 * 
 * For setting location, we could have a different fragment and pass on the location
 * on click of set location. This will at least make the implementation more modular.
 * 
 * There are quite a few Async Tasks for autocomplete, place details, making trip, 
 * and map geocoding. They could be shifted to their respective classes with proper abstraction.
 * 
 * The handling for pickup and drop could be more DRY (for instance right now there are 
 * two camera change listeners and in setup autocomplete almost the same code is written twice
 * for text changed and itemclick listener. That definitely should be more DRY.
 * 
 * There are some redundant things which are left as a bad after taste of so many iterations.
 * For instance the existence of Custom Address. It was used for passing data when in the 
 * first iteration of the app it was designed as activities. We shifted from Activities to 
 * Fragments because we wanted to implement the Navigation Drawer which has come out beautiful,
 * thanks to Prakash's awesome sense of design.
 * 
 * One big flaw is the repeated logins for every request to the back-end. That is the worst thing
 * I am doing in the app I think. It was a temporary arrangement but can't be dealt with at this time.
 * On reading up more I realized to maintain the session across the app, I should login initially 
 * and maintain session using a Singleton Http Client. I am not trained in OOP and had no idea
 * of a Singleton Pattern but I have nonetheless added an HttpClient class but that is a work-in-progress.
 * So ideally every HttpRequest should be made with this singleton.
 * 
 * Also there are two static classes, pretty much like singletons, called ServerUtilites and Utilities
 * which should be used for all server and other tasks (like JSON parsing) respectively, so that
 * the fragment classes can be kept small and will only deal with UI implementation.
 * 
 * On an ending note I insist you read the docs of every function carefully and 
 * see every class in the app before making changes to it. There are some things like 
 * the TouchWrapper for the map which calls the toggleMapIn because it works as a listener 
 * to touch action_down on map. This call can be deceptive.
 * 
 * Copyright (c) Sahil Shah 
 * 	27th June, 2013
 * 
*/
public class RouteFragment extends Fragment implements LocationListener, OnCheckedChangeListener{
	
    Context mainContext;
    MainActivity mainActivity;

	Address map_pickup = new Address(new Locale("en","INDIA"));
	Address map_drop = new Address(new Locale("en","INDIA"));
	
	boolean first = true;
	boolean g_map_in = true;
	boolean p_latlng_set = false;
	boolean d_latlng_set = false;
	int whichETClicked = 0;
    
	GooglePlaces ac;
	GoogleMap googleMap;
    MarkerOptions toMarker;
    MarkerOptions fromMarker;

    CustomAddress from_ca;
    CustomAddress to_ca;
    CustomDateTimeDialog startDT;
    DaysPicker dp;
    
    Calendar dateTime;
    String approx_cost;
    ProgressDialog pd;

    JSONObject trip_params;
    JSONObject trip;
    
    String datetime = "";
    String date ="";
    String time ="";
	String mobile = "";
	String trip_type = "city_taxi";
	String vendor="";
	String package_type = ""; 
	String reg_id = null;	//should be null, a check is used later
	String numDays = "";
	String vendors = "";
	String car_type = "";
	String email = "";
	
	ConnectionDetector cd;
    AlertDialogManager alert = new AlertDialogManager();
	
	static View rootView;
    RadioGroup rg_tt;
    RadioGroup rg_ct;

    LocationManager locationManager;
    
    AutoCompleteTextView inPickup;
    AutoCompleteTextView inDrop;
    
    PlacesDownload placesDownloadTask;
    DetailsDownloadTask placeDetailsDownloadTask;
    ParserTask placesParserTask;
    ParserTask placeDetailsParserTask;
    GeocoderTask p_geocode;
    GeocoderTask d_geocode;
    
    MapFragment mapFragment;

    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    
    final int PLACES = 0;
    final int PLACES_DETAILS = 1;
    final int PICKUP = 0;
    final int DROP = 1;
    final public String SEDAN = "indigo,etios,manza,dezire";
    final public String COMPACT = "indica,swift";
    final public String INNOVA = "innova";
    final public String TAVERA = "tavera";
	
    Marker old_pickup;
    Marker old_drop;
    
	String host = "http://ektaxi-staging.herokuapp.com";
	String localhost = "http://192.168.1.8:9292";
	
	RateCard ct1,ct2,ct3,ct4,l1,l2,l3,l4,o1,o2,o3,o4;
    
    public RouteFragment() {
		
	}	

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
	    
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    }
	
    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        Log.d("OnResume View", "Sucess");
    }
    
    /*
     * Overrides activity's lifecycle callback. Takes care of cancelling all the background async tasks
    */
    @Override
    public void onPause(){
    	super.onPause();
    	if(p_geocode!=null) p_geocode.cancel(true);
    	if(d_geocode!=null) d_geocode.cancel(true);
    	if(placeDetailsDownloadTask!=null)placeDetailsDownloadTask.cancel(true);
    	if(placeDetailsDownloadTask!=null)placesDownloadTask.cancel(true);
    	if(placesParserTask!=null)placesParserTask.cancel(true);
    	if(placeDetailsParserTask!=null)placeDetailsParserTask.cancel(true);
    }
    
    @Override
  	public void onActivityCreated(Bundle savedInstanceState) {
  		super.onActivityCreated(savedInstanceState);
        mainContext = getActivity();
        locationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
        reg_id = mainActivity.regid;
	}

    /*
     * Will take care of setting up the UI (view). It sets up rate cards, autocomplete etc
     * as well.
     */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
		if (rootView != null) {
		        ViewGroup parent = (ViewGroup) rootView.getParent();
		        if (parent != null)
		            parent.removeView(rootView);
	    }
	    try {
	        rootView = inflater.inflate(R.layout.route_fragment, container, false);
	    } catch (InflateException e) {
	    	//map is already there, just return view as it is
	    }
	    
        rg_tt = (RadioGroup) rootView.findViewById(R.id.buttongroup1);
	    rg_tt.setOnCheckedChangeListener(this);
	    rg_ct = (RadioGroup) rootView.findViewById(R.id.buttongroup2);
	    inPickup = (AutoCompleteTextView) rootView.findViewById(R.id.in_pickup);
	    inDrop = (AutoCompleteTextView) rootView.findViewById(R.id.in_drop);
	    from_ca = new CustomAddress();
	    to_ca = new CustomAddress();
	    	    
	    setUpMapIfNeeded();
	    setUpAutocomplete();
	    setCityTaxiRateCardObjects();
	    setLocalRateCardsObjects();
	    setOutstationRateCardsObjects();
	    setDefaultStuff();
	    
	    //set rate card button listeners
	    rootView.findViewById(R.id.opt_one).setOnTouchListener(rate_card_listener);
    	rootView.findViewById(R.id.opt_two).setOnTouchListener(rate_card_listener);
    	rootView.findViewById(R.id.opt_three).setOnTouchListener(rate_card_listener);
    	rootView.findViewById(R.id.opt_four).setOnTouchListener(rate_card_listener);
	    
    	//set confirm button listener and colour
	    Button confirm_btn = (Button) rootView.findViewById(R.id.btn_confirm);
        confirm_btn.setBackgroundColor(Color.rgb(255,187,51));
	    confirm_btn.setOnClickListener(confirm_btn_listener);
	    
	    View sv = (LinearLayout) rootView.findViewById(R.id.time_start_box);
	    View ev = (LinearLayout) rootView.findViewById(R.id.time_end_box);
	    
	    ev.setOnClickListener(new OnClickListener() {
	        public void onClick(View view) {
	        	if(rg_tt.getCheckedRadioButtonId() == R.id.opt_lo){
	        	}
	        	else if (rg_tt.getCheckedRadioButtonId() == R.id.opt_os){
	        		dp = new DaysPicker(mainActivity);
	        		dp.show(getFragmentManager(),"NumberPicker");
	        	}
	        	else{
	        		startDT = new CustomDateTimeDialog(mainActivity,2);
	        		startDT.show(getFragmentManager(),"DateTimePicker");
	        	}
	        }
	    });
	   
	    sv.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View view) {
	            	if(rg_tt.getCheckedRadioButtonId() != R.id.opt_ct){
		        		startDT = new CustomDateTimeDialog(mainActivity,3);
		        		startDT.show(getFragmentManager(),"DateTimePicker");
		        	}
	            }
	     });
	    
	    ImageView edit_pickup = (ImageView)rootView.findViewById(R.id.pickup_map_toggle);
	    ImageView edit_drop = (ImageView)rootView.findViewById(R.id.drop_map_toggle);
	    Button set_location = (Button)rootView.findViewById(R.id.set_location);

	    edit_pickup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!g_map_in){
					toggleMapView();
				}
				else{
					((EditText)rootView.findViewById(R.id.in_pickup)).setText("");
				}
				rootView.findViewById(R.id.in_pickup).requestFocus();
			}
		});
	
	    edit_drop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!g_map_in){
					toggleMapView();
				}
				else{
					if(rg_tt.getCheckedRadioButtonId() == R.id.opt_os)
						((EditText)rootView.findViewById(R.id.in_drop)).setHint("Destination");
					((EditText)rootView.findViewById(R.id.in_drop)).setText("");
				}
				rootView.findViewById(R.id.in_drop).requestFocus();
			}
		});
	    
	    set_location.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if("".equals(((EditText)rootView.findViewById(R.id.in_pickup)).getText().toString())){
					Toast.makeText(mainContext, "Please enter a valid pickup address...", Toast.LENGTH_SHORT).show();
					return;
				}
				if("".equals(((EditText)rootView.findViewById(R.id.in_drop)).getText().toString()) && 
						rg_tt.getCheckedRadioButtonId() != R.id.opt_lo){
					Toast.makeText(mainContext, "Please enter a valid drop address...", Toast.LENGTH_SHORT).show();
					return;
				}
				toggleMapView();
			}
		});
	    
        return rootView;
    }
	
	
	/*
	 * This function is not called in this class but it is called by the touchable
	 * wrapper on the map. Basically it detects a touch on the map. Since the Gmap APIs
	 * only provide tap/click listener but no touch listener, we have to use a touchable wrapper.
	 */
	public void mapClicked(){
    	if(!g_map_in){
    		toggleMapView();
    		return;
    	}
		if(whichETClicked == 0){
			Log.i("Batchcar","For Pickup...");
			switchToMapInputForLocations(0);
		}
		else{
			Log.i("Batchcar","For Drop...");
			switchToMapInputForLocations(1);
		}	
    }
    
	/*
	 * This sets the default view for the app.
	 */	
    public void setDefaultStuff(){
		//default autocomplete input with focus on pickup
		googleMap.setOnCameraChangeListener(null);
		p_latlng_set = false;
		d_latlng_set = false;
		inPickup.setFocusable(true);
		inPickup.requestFocus();
		//since default city taxi, set UI
	    setCityTaxiRateCardsUI();
	    rg_ct.check(R.id.opt_one);
	    setDrawable(R.id.opt_one, ct1.icon_key, true);
	    //default mode, show map
	  	g_map_in = false;
	    toggleMapView();
	}
	
	/*
	 * This function takes the drop/pickup as which and accordingly switches
	 * to location input from map for the respective edit text box. 
	 * 
	 * If the type is not 0/1 then it simply switches to map view. This is called 
	 * when we are in details view and we click on map.
	 */	
	public void switchToMapInputForLocations(int which){
		ImageView iv = (ImageView) rootView.findViewById(R.id.img_center);
		 if(old_pickup != null)
	 		old_pickup.remove();
	     if(old_drop != null)
	 		old_drop.remove();
		if(which == 0){
            googleMap.setOnCameraChangeListener(onPickupChangeListener);
    		iv.setVisibility(View.VISIBLE);
    		p_geocode = new GeocoderTask(0);
    		p_geocode.execute(googleMap.getCameraPosition().target);
		}
		else if (which == 1){
            googleMap.setOnCameraChangeListener(onDropChangeListener);
    		iv.setVisibility(View.VISIBLE);
    		d_geocode = new GeocoderTask(1);
    		d_geocode.execute(googleMap.getCameraPosition().target);
		}
		else{
			toggleMapView();
		}
	}
	
	/*
	 * This function switched to autocomplete from map input
	*/
	public void switchToAutocompleteInputForLocations(){
		rootView.findViewById(R.id.img_center).setVisibility(View.GONE);
		googleMap.setOnCameraChangeListener(null);
	}
	
	/*
	 * This function toggles between map view and details view based on the current config.
	 * This deals with hiding the linear layouts for details other than drop and pick.
	 * It also shows the set location button and changes the edit image to clear (cancel)
	 * image and vice versa.
	 */	
	public void toggleMapView(){
		ImageView iv = (ImageView) rootView.findViewById(R.id.img_center);
		ImageView iv1 = (ImageView) rootView.findViewById(R.id.pickup_map_toggle);
		ImageView iv2 = (ImageView) rootView.findViewById(R.id.drop_map_toggle);
		
		Drawable cancel = mainContext.getResources().getDrawable(R.drawable.cancel3);
		Drawable edit = mainContext.getResources().getDrawable(R.drawable.edit_clicked);
		if(!g_map_in){
			rootView.findViewById(R.id.to_hide).setVisibility(View.GONE);
			rootView.findViewById(R.id.separator12).setVisibility(View.VISIBLE);
			rootView.findViewById(R.id.set_location).setVisibility(View.VISIBLE);
			rootView.findViewById(R.id.in_tv_drop).setVisibility(View.GONE);
			rootView.findViewById(R.id.in_tv_pickup).setVisibility(View.GONE);
			rootView.findViewById(R.id.in_drop).setVisibility(View.VISIBLE);
			rootView.findViewById(R.id.in_pickup).setVisibility(View.VISIBLE);
			iv1.setImageDrawable(cancel);
			iv2.setImageDrawable(cancel);
			rootView.findViewById(R.id.from_layout).setBackgroundResource(R.color.white);
			rootView.findViewById(R.id.to_layout).setBackgroundResource(R.color.white);			
			g_map_in = true;
		}
		else{
			iv.setVisibility(View.GONE);
			rootView.findViewById(R.id.separator12).setVisibility(View.GONE);
			rootView.findViewById(R.id.set_location).setVisibility(View.GONE);	
			rootView.findViewById(R.id.to_hide).setVisibility(View.VISIBLE);
			rootView.findViewById(R.id.in_drop).setVisibility(View.GONE);
			rootView.findViewById(R.id.in_pickup).setVisibility(View.GONE);
			((TextView)rootView.findViewById(R.id.in_tv_pickup)).setText(from_ca.getAddressString());
			((TextView)rootView.findViewById(R.id.in_tv_drop)).setText(to_ca.getAddressString());
			rootView.findViewById(R.id.in_pickup).setVisibility(View.GONE);
			rootView.findViewById(R.id.in_tv_drop).setVisibility(View.VISIBLE);
			rootView.findViewById(R.id.in_tv_pickup).setVisibility(View.VISIBLE);
			inPickup.clearFocus();
			inDrop.clearFocus();
			inPickup.setFocusable(true);
			inDrop.setFocusable(true);
			iv1.setImageDrawable(edit);
			iv2.setImageDrawable(edit);
			rootView.findViewById(R.id.from_layout).setBackgroundResource(R.color.silver_four);
			rootView.findViewById(R.id.to_layout).setBackgroundResource(R.color.silver_four);
			googleMap.setOnCameraChangeListener(null);
			g_map_in = false;
		}
	}

    private void setUpMapIfNeeded() {
    	if (googleMap == null) {
            googleMap = ((MyMapFragment) getFragmentManager().findFragmentById(R.id.map_route)).getMap();
        	if (googleMap != null) {
                setUpMap();
            }
        }
    }    
    
    private void setUpMap(){
    	if(googleMap==null)
            Toast.makeText(mainContext,"Sorry! Google maps are currently not available!" , Toast.LENGTH_SHORT).show();
		else{
    	    googleMap.getUiSettings().setRotateGesturesEnabled(false);
    	    googleMap.getUiSettings().setTiltGesturesEnabled(false);
    	    googleMap.setMyLocationEnabled(true);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(19.1167, 72.8333),15.0f));
		}
    }
    
    /* This obviously sets up autocomplete. The thing is there is a lot of repetition here.
     * It could do with some refactoring and made more DRY.
    */
    private void setUpAutocomplete(){
    	//set autocomplete
		ac = new GooglePlaces("AIzaSyAwOfbKiL_U1D9wIPZyzgQVBwTmJjK1Q8M");
    	inPickup.setFocusable(false);
    	inDrop.setFocusable(false);
    	p_latlng_set = false;
    	d_latlng_set = false;
    	inPickup.setThreshold(2);
    	inDrop.setThreshold(2);
        
    	inPickup.setAdapter(new PlacesAutoCompleteAdapter(mainContext, android.R.layout.simple_list_item_1));
    	inDrop.setAdapter(new PlacesAutoCompleteAdapter(mainContext, android.R.layout.simple_list_item_1));
    	
        inPickup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				whichETClicked = 0;
				switchToAutocompleteInputForLocations();
				inPickup.setFocusableInTouchMode(true);
				inPickup.requestFocus();
			}
		});
        
        inDrop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				whichETClicked = 1;
				switchToAutocompleteInputForLocations();
				inDrop.setFocusableInTouchMode(true);
				inDrop.requestFocus();
			}
		});
        
        inPickup.setOnFocusChangeListener(new OnFocusChangeListener() {
        	@Override
        	public void onFocusChange(View v, boolean hasFocus) {
        	    if(hasFocus){
    				Log.i("Batchcar","Pickup has Focus..");
        	    }else {
    				Log.i("Batchcar","Pickup lost focus..");
    		    	inPickup.setFocusable(false);
        	    }
        	}
        });
        
        inDrop.setOnFocusChangeListener(new OnFocusChangeListener() {
        	@Override
        	public void onFocusChange(View v, boolean hasFocus) {
        	    if(hasFocus){
    				Log.i("Batchcar","Drop has Focus..");
        	    }else {
    				Log.i("Batchcar","Drop lost focus..");
    		    	inDrop.setFocusable(false);
        	    }
        	}
        });
        
        inPickup.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();

        	@Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
            	if(s.length() < 2 || "Fetching Location...".equals(s))
            		return;
            	if(p_latlng_set){
            		Log.i("Batchcar","Over here, resetting p lat lng set");
            		p_latlng_set = false;
            		return;
            	}
//                timer.cancel();
//                timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                    	if(placesDownloadTask!=null)
//                    		placesDownloadTask.cancel(true);
            	if(p_geocode != null)
            		p_geocode.cancel(true);
            	if(d_geocode != null)
            		d_geocode.cancel(true);
            	placesDownloadTask = new PlacesDownload(PLACES,0);
                placesDownloadTask.execute(s.toString());
//                    }
//                }, 750);
//            	
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
            }
        });
       
        inDrop.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
            }
            private Timer timer = new Timer();
            @Override
            public void afterTextChanged(final Editable s) {
            	if(s.length() <2)
            		return;
            	if(d_latlng_set){
            		Log.i("Batchcar","Over here, resetting d lat lng set");
            		d_latlng_set = false;
            		return;
            	}
//            	timer.cancel();
//                timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
            	if(p_geocode != null)
            		p_geocode.cancel(true);
            	if(d_geocode != null)
            		d_geocode.cancel(true);
            	placesDownloadTask = new PlacesDownload(PLACES,1);
            	placesDownloadTask.execute(s.toString());
//                    }
//
//                }, 750);
            }
        });
        
        inPickup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int index,
            long id) {

            	EditText editText = inPickup;
            	Editable etext = editText.getText();
            	Selection.setSelection(etext, 0);
            	
            	InputMethodManager inputManager = 
            	        (InputMethodManager) mainContext.
            	            getSystemService(Context.INPUT_METHOD_SERVICE); 
            	if(mainActivity.getCurrentFocus() != null)
            		inputManager.hideSoftInputFromWindow(mainActivity.getCurrentFocus().    
            	        getWindowToken(),
            	        InputMethodManager.HIDE_NOT_ALWAYS); 
            	
                SimpleAdapter adapter = (SimpleAdapter) arg0.getAdapter();
                HashMap<String, String> hm = (HashMap<String, String>) adapter.getItem(index);
                placeDetailsDownloadTask = new DetailsDownloadTask(PLACES_DETAILS, 0);
                String url = getPlaceDetailsUrl(hm.get("reference"));
                placeDetailsDownloadTask.execute(url);
                
            }
        });
 
        // Setting an item click listener for the AutoCompleteTextView dropdown list
        inDrop.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int index,
            long id) {

            	EditText editText = inDrop;
            	Editable etext = editText.getText();
            	Selection.setSelection(etext, 1);
            	
            	InputMethodManager inputManager = 
            	        (InputMethodManager) mainContext.
            	            getSystemService(Context.INPUT_METHOD_SERVICE); 
            	if(mainActivity.getCurrentFocus() != null)
            		inputManager.hideSoftInputFromWindow(mainActivity.getCurrentFocus().    
            	        getWindowToken(),
            	        InputMethodManager.HIDE_NOT_ALWAYS); 
            	
                SimpleAdapter adapter = (SimpleAdapter) arg0.getAdapter();
                HashMap<String, String> hm = (HashMap<String, String>) adapter.getItem(index);
                placeDetailsDownloadTask = new DetailsDownloadTask(PLACES_DETAILS, 1);
                String url = getPlaceDetailsUrl(hm.get("reference"));
                placeDetailsDownloadTask.execute(url);
                Log.i("bathccar","reference is : "+ hm.get("reference") + "description is : "+ hm.get("description"));
            }
        });
        
    }
    
    private String getPlaceDetailsUrl(String ref){
        String key = "key=AIzaSyAwOfbKiL_U1D9wIPZyzgQVBwTmJjK1Q8M";
        String reference = "reference="+ref;
        String sensor = "sensor=false";
        String parameters = reference+"&"+sensor+"&"+key;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/place/details/"+output+"?"+parameters;
        return url;
    }
    
    private boolean setCityTaxiRateCardObjects(){
		Log.i("Batchcar", "Setting City Taxi Rate Cards");
    	SharedPreferences prefs = ServerUtilities.getAppSharedPreferences(mainContext);
    	try {
    		String main = prefs.getString("com.sharedcab.batchcar.city_taxi","");
        	JSONArray o= new JSONArray(main);
        	ct1 = new RateCard(0, o.get(0).toString());
        	ct2 = new RateCard(0, o.get(1).toString());
        	ct3 = new RateCard(0, o.get(2).toString());
        	ct4 = new RateCard(0, o.get(3).toString());    	  
        	return true;
		} catch (Exception e) {
			Log.i("Batchcar","City Taxi Rate JSON parsing error");
			return false;
		}
    }

	private boolean setOutstationRateCardsObjects(){
		Log.i("Batchcar", "Setting Outstation Rate Cards");
    	SharedPreferences prefs = ServerUtilities.getAppSharedPreferences(mainContext);
    	try {
    		String main = prefs.getString("com.sharedcab.batchcar.outstation","");
        	JSONArray o= new JSONArray(main);
        	o1 = new RateCard(1, o.get(0).toString());
        	o2 = new RateCard(1, o.get(1).toString());
        	o3 = new RateCard(1, o.get(2).toString());
        	o4 = new RateCard(1, o.get(3).toString());
        	return true;
		} catch (Exception e) {
			Log.i("Batchcar","Outstation Rate JSON parsing error");
			return false;
		}
    }
    
    private boolean setLocalRateCardsObjects(){
		Log.i("Batchcar", "Setting Local Rate Cards");
    	SharedPreferences prefs = ServerUtilities.getAppSharedPreferences(mainContext);
    	try {
    		String main = prefs.getString("com.sharedcab.batchcar.local","");
        	JSONArray o= new JSONArray(main);
        	l1 = new RateCard(2, o.get(0).toString());
        	l2 = new RateCard(2, o.get(1).toString());
        	l3 = new RateCard(2, o.get(2).toString());
        	l4 = new RateCard(2, o.get(3).toString());
        	return true;
		} catch (Exception e) {
			Log.i("Batchcar","Local Rate JSON parsing error");
			return false;
		}
    }

    private void resetDrawables(int type){
    	if(type == 0 ){
    		setDrawable(R.id.opt_one,ct1.icon_key,false);
        	setDrawable(R.id.opt_two,ct2.icon_key,false);
        	setDrawable(R.id.opt_three,ct3.icon_key,false);
        	setDrawable(R.id.opt_four,ct4.icon_key,false);
    	}
    	else if (type == 1){
    		setDrawable(R.id.opt_one,o1.icon_key,false);
        	setDrawable(R.id.opt_two,o2.icon_key,false);
        	setDrawable(R.id.opt_three,o3.icon_key,false);
        	setDrawable(R.id.opt_four,o4.icon_key,false);
    	}
    	else{
    		setDrawable(R.id.opt_one,l1.icon_key,false);
        	setDrawable(R.id.opt_two,l2.icon_key,false);
        	setDrawable(R.id.opt_three,l3.icon_key,false);
        	setDrawable(R.id.opt_four,l4.icon_key,false);
    	}
    }
    
    private void setCityTaxiRateCardsUI(){
    	((RadioButton)rootView.findViewById(R.id.opt_one)).setText(ct1.vendor);
    	((RadioButton)rootView.findViewById(R.id.opt_two)).setText(ct2.vendor);
    	((RadioButton)rootView.findViewById(R.id.opt_three)).setText(ct3.vendor);
    	((RadioButton)rootView.findViewById(R.id.opt_four)).setText(ct4.vendor);
    	resetDrawables(0);
		setDrawable(R.id.opt_one, ct1.icon_key, true);
    }
    
    private void setOutstationRateCardsUI(){
    	((RadioButton)rootView.findViewById(R.id.opt_one)).setText(o1.car_type);
    	((RadioButton)rootView.findViewById(R.id.opt_two)).setText(o2.car_type);
    	((RadioButton)rootView.findViewById(R.id.opt_four)).setText(o3.car_type);
    	((RadioButton)rootView.findViewById(R.id.opt_three)).setText(o4.car_type);
    	resetDrawables(1);
		setDrawable(R.id.opt_one, o1.icon_key, true);
    }
    
    private void setLocalRateCardsUI(){
    	((RadioButton)rootView.findViewById(R.id.opt_one)).setText(l1.car_type);
    	((RadioButton)rootView.findViewById(R.id.opt_two)).setText(l2.car_type);
    	((RadioButton)rootView.findViewById(R.id.opt_four)).setText(l3.car_type);
    	((RadioButton)rootView.findViewById(R.id.opt_three)).setText(l4.car_type);
    	resetDrawables(2);
		setDrawable(R.id.opt_one, l1.icon_key, true);
    }
    
    /*
     * Sets the drawables for the rate card and radio button for cartype/vendor
     * the clicked attribute is true for selected and false for unselected.
     * When transitioning to multiple vendor just set it for more than one.
    */
    private void setDrawable(int button, String icon_key, boolean clicked) {
    	Drawable ex;
    	if("compact".equals(icon_key))
    		ex = mainActivity.getResources().getDrawable(clicked? 
    				R.drawable.compact_onclick_final2_resized : R.drawable.compact_final2_resized);
    	else if("innova".equals(icon_key))
    		ex = mainActivity.getResources().getDrawable(clicked? 
    				R.drawable.innova_onclick_final_resized : R.drawable.innova_final_resized);
    	else if("tavera".equals(icon_key))
    		ex = mainActivity.getResources().getDrawable(clicked? 
    				R.drawable.suv_onclick_final_resized : R.drawable.suv_final_resized);    		
    	else if("any".equals(icon_key))
    		ex = mainActivity.getResources().getDrawable(clicked? 
    				R.drawable.sedan_onclick_resized_with_questionmark :
    					R.drawable.sedan_offclick_resized_with_questionmark);
    	else
    		ex = mainActivity.getResources().getDrawable(clicked? 
    				R.drawable.sedan_onclick_final_resized : R.drawable.sedan_final_resized);    	
    	((TabControlButton)rootView.findViewById(button)).setBackgroundDrawable(ex);    		
    }
 
    /*
     * Called from details downlaod to task to download the details from places API
    */
    private String downloadFromURL(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            
            Log.i("TEST","Code is: " + urlConnection.getResponseCode());

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }
 
            data = sb.toString();
            br.close();
            Log.i("TEST","Data is: \n" + data);            
            
        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    	
    /*
     * Parses the details of the selected location from the autocomplete dropdown.
     * Post the parsing it sets the to_ca/from_ca depending on pickup or drop.
     * Also it sets the marker on the map and moves the camera there.
    */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{
        int parserType = 0;
        int inputType = 0;
        public ParserTask(int type, int input){
            this.parserType = type;
            this.inputType = input;
        }
 
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<HashMap<String, String>> list = null;
            try{
                jObject = new JSONObject(jsonData[0]);
                PlaceDetailsJSONParser placeDetailsJsonParser = new PlaceDetailsJSONParser();
                list = placeDetailsJsonParser.parse(jObject); 
            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return list;
        }
 
        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {

            HashMap<String, String> hm = result.get(0);
            double latitude = Double.parseDouble(hm.get("lat"));
            double longitude = Double.parseDouble(hm.get("lng"));
            LatLng point = new LatLng(latitude, longitude);
            CameraUpdate cameraPosition = CameraUpdateFactory.newLatLng(point);
            googleMap.moveCamera(cameraPosition);
            
            MarkerOptions options = new MarkerOptions();
            options.position(point);
            options.title("Position");
            options.snippet("Latitude:"+latitude+",Longitude:"+longitude);
            if(inputType == 0){
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.orange_gen_marker_resized2);
                Bitmap bhalfsize=Bitmap.createScaledBitmap(b, b.getWidth()/4,b.getHeight()/4, false);
                options.icon(BitmapDescriptorFactory.fromBitmap(bhalfsize));
            	from_ca.setAddressString(hm.get("address"));
            	from_ca.setLatitude(hm.get("lat"));
            	from_ca.setLongitude(hm.get("lng"));
            	if(old_pickup != null)
            		old_pickup.remove();
            	Log.i("Batchcar","Adding pcikup marker...");
                old_pickup = googleMap.addMarker(options);
            }
            else{
            	Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.orange_gen_marker_resized2);
                Bitmap bhalfsize=Bitmap.createScaledBitmap(b, b.getWidth()/4,b.getHeight()/4, false);
                options.icon(BitmapDescriptorFactory.fromBitmap(bhalfsize));
            	to_ca.setAddressString(hm.get("address"));
            	to_ca.setLatitude(hm.get("lat"));
            	to_ca.setLongitude(hm.get("lng"));
            	if(old_drop != null)
            		old_drop.remove();
            	Log.i("Batchcar","Adding pcikup marker...");
            	old_drop = googleMap.addMarker(options);
            }
        }
    }
    
    /*
     * Downloads the places based on the string which is sent as an argument.
     * The string is the text in the edit text box sent from the edit text box's on
     * text changed callback.
     * It populates a list of description and reference. The reference string is later used by
     * the details download task.  
     *  
    */
    private class PlacesDownload extends AsyncTask<String,Void,ArrayList<HashMap<String, String>>>{
        private int inputType=0;
 
        public PlacesDownload(int type, int input){
            this.inputType = input;
        }
        
        protected void onPreExecute(){
        	mainActivity.runOnUiThread(new Runnable() {
        	     public void run() {
        	    	ProgressBar pb;
    	     		int id;
    	     		id = inputType == 0? R.id.pb1 : R.id.pb2;
    	     		pb = (ProgressBar)rootView.findViewById(id);
 	            	pb.setVisibility(View.VISIBLE);
        	    }
        	});
        }

        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(String... url) {
            ArrayList<HashMap<String, String>> l = new ArrayList<HashMap<String,String>>();
            try{
            	l = ac.autocomplete(url[0]);            	
            }catch(Exception e){
                Log.d("Places Background Task",e.toString());
            }
            return l;
        }
 
        @Override
        protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
            super.onPostExecute(result);
            mainActivity.runOnUiThread(new Runnable() {
       	     	public void run() {
       	     		ProgressBar pb;
       	     		int id;
       	     		id = inputType == 0? R.id.pb1 : R.id.pb2;
       	     		pb = (ProgressBar)rootView.findViewById(id);
	            	pb.setVisibility(View.INVISIBLE);
       	     	}
            });
           
			String[] from = new String[] { "description"};
		    int[] to = new int[] { android.R.id.text1 };
		    SimpleAdapter adapter = new SimpleAdapter
		    		(mainContext, result, android.R.layout.simple_list_item_1, from, to);
		    
		    if(inputType == 0){
			    inPickup.setAdapter(adapter);		    		
		    }
		    else{
			    inDrop.setAdapter(adapter);
		    }
		    adapter.notifyDataSetChanged();
        }
    }
    
    /*
     * Downloads the details of the selected location from the autocomplete dropdown.
     * It uses the reference string which we get with the results of the autocomplete places.
     * From the details it parses the json and gets thelat lng and sets the from_ca and to_ca 
    */
	private class DetailsDownloadTask extends AsyncTask<String, Void, String>{
		 
        private int downloadType=0;
        private int inputType=0;
 
        public DetailsDownloadTask(int type, int input){
            this.downloadType = type;
            this.inputType = input;
        }

        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try{
                data = downloadFromURL(url[0]);
            	
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }
 
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            placeDetailsParserTask = new ParserTask(PLACES_DETAILS,inputType);
            placeDetailsParserTask.execute(result);
        }
    }

	private class TripbookerTask extends AsyncTask<JSONObject, Void, String>{
		
		private int type;	//0 for cabregator, 1 for ytaxi
    	private final static String TAG_FRAGMENT = "TAG_FRAGMENT";
		
		public TripbookerTask(int x){
			type = x;
		}
   	  
        @Override
        protected String doInBackground(JSONObject... trip_details) {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpPost httpost;

            if(type == 0){
            	Log.i("test","Making Cabregator booking");
        	    return postData(trip_details[0].toString());
            }
            //not really used for now because we don't make a ytaxi booking directly
            else{
            	Log.i("test","Making Ytaxi booking");
    			try {
                	httpost = new HttpPost("http://batchcar.herokuapp.com/api/1/trips.json");
    				StringEntity se = new StringEntity(trip_details[0].toString());
    	            httpost.setEntity(se);
    	            httpost.setHeader("Accept", "application/json");
    	            httpost.setHeader("Content-type", "application/json");
    	            HttpResponse r = httpclient.execute(httpost); 
    	            ResponseHandler<String> handler = new BasicResponseHandler();
    	            JSONObject o = new JSONObject(handler.handleResponse(r));
    	        	if (r.getStatusLine().getStatusCode() == 202){
    	        		Log.i("Batchcar", o.toString());
    	        		return "true";
    	        	}
    	        	else
    	        		return "";
    			} catch (Exception e) {
    				e.printStackTrace();
    				Toast.makeText(mainContext, "Sorry, some error occurred while making the booking. " +
    						"Please try again later", Toast.LENGTH_SHORT).show();
    				return "";
    			}
            }
        }

        public String postData(String trip) {
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpClient httpclient2 = new DefaultHttpClient();
            
            Log.i("test", "in post data");
            try {
                HttpPost httppost = new HttpPost(host + "/staffs/login?email=admin@ektaxi.com&password=4dmi9");
                HttpResponse response = httpclient.execute(httppost);
                Log.i("test",response.getStatusLine().toString());
	            Log.i("test","Cookies are: " + ((AbstractHttpClient) httpclient).getCookieStore().getCookies().toString());
  
	            ((AbstractHttpClient) httpclient2).setCookieStore(((AbstractHttpClient) httpclient).getCookieStore());
	           
	            StringEntity se;
	        	se = new StringEntity(trip);
	        	httppost = new HttpPost(host + "/trips");
	        	httppost.setEntity(se);
	        	httppost.setHeader("Accept", "application/json");
 	            httppost.setHeader("Content-type", "application/json");
                response = httpclient.execute(httppost);
                Log.i("test",response.getStatusLine().toString());
	            Log.i("test","Cookies are: " + ((AbstractHttpClient) httpclient).getCookieStore().getCookies().toString());

	            JSONObject o;
  	            String id;
	            ResponseHandler<String> handler = new BasicResponseHandler();
  	            try {
					o = new JSONObject(handler.handleResponse(response));
					id = o.getString("id");
				} catch (JSONException e) {
					Log.i("test", "JSON response parsing failed!");
					e.printStackTrace();
					return "";
				}
  	            Log.i("test","ID is : " + id);
  	            
	            if(response.getStatusLine().getStatusCode() == 200)
	            	return id;
	            else
	            	return "";
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return "";
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
        } 
        
        @Override
        protected void onPostExecute(String trip_id) {
 	    	pd.dismiss();
 	    	if (trip_id.length() > 0){
 	    		Toast.makeText(mainContext, "Your booking number is " + trip_id, Toast.LENGTH_SHORT).show();
 	    		final BookingSummary fragment = new BookingSummary(trip_id);
 	    		final FragmentTransaction transaction = getFragmentManager().beginTransaction();
 	    		transaction.replace(R.id.content_frame, fragment);
 	    		transaction.commit();
 	    	}
 	    	else
 	    		Toast.makeText(mainContext, "Booking failed! ", Toast.LENGTH_SHORT).show();
 	   	
        }
        
        protected void onPreExecute(){
        	pd = ProgressDialog.show(mainContext,"Please wait..", "Creating Booking..");
 	    }
    }
	
	/*
	 * Not used now, but can always be helpful if we ever decide to show direction from pickup
	 * to drop. 
	*/
	private class DirectionPlotter extends AsyncTask<LatLng, Void, ArrayList<LatLng>>{
	 
        @Override
        protected ArrayList<LatLng> doInBackground(LatLng... locations) {
        	GMapDirection md = new GMapDirection();
		    Document doc = md.getDocument(locations[0], locations[1], GMapDirection.MODE_DRIVING);
		    return md.getDirection(doc);
        }
 
        @Override
        protected void onPostExecute(ArrayList<LatLng> directionPoints) {
 
            if(directionPoints==null || directionPoints.size()==0){
                Toast.makeText(mainContext, "Sorry! Directions could not be found", Toast.LENGTH_SHORT).show();
            }
            else{
            	PolylineOptions rectLine = new PolylineOptions().width(4).color(Color.BLUE);
    		    for(int j = 0 ; j < directionPoints.size() ; j++) {          
    		    	rectLine.add(directionPoints.get(j));
    		    }
    		    googleMap.addPolyline(rectLine);
                CameraUpdate cameraZoom = CameraUpdateFactory.zoomTo(11);
                googleMap.animateCamera(cameraZoom);
            } 
        }
    }
	
	
	/*
	 * This is to take the map camera to current location , but only at the start. 
	 * So the default value for the bool first is true at the start. But it switched to false
	 * when the location change is listened to for the first time.
	 *  
	 */	
	@Override
	public void onLocationChanged(Location location) {
		if(first){
			LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
		    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
		    googleMap.animateCamera(cameraUpdate);
		    locationManager.removeUpdates(this);
		    first = false;
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
	/*
	 * Callback when the radio group check changes. This is used to shift views.
	 * This is what will be totally removed if we shift to a fragment approach.
	 * The views to be hidden are hidden and to be shown are shown! 
	 * 
	 */	
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		
		int x = group.getId();
		if (x == R.id.buttongroup1){
			LinearLayout rv = (LinearLayout) rootView.findViewById(R.id.to_layout);
			ImageView iv = (ImageView) rootView.findViewById(R.id.img_time_start);
			TextView tv = (TextView) rootView.findViewById(R.id.tv_time_start);
			ToggleButton tb = (ToggleButton) rootView.findViewById(R.id.toggle_btn);
			ImageView iv1 = (ImageView) rootView.findViewById(R.id.img_time_end);
			ImageView iv2 = (ImageView) rootView.findViewById(R.id.img_cal_end);
			ToggleButton tb2 = (ToggleButton) rootView.findViewById(R.id.toggle_btn2);
			TextView tv1 = (TextView) rootView.findViewById(R.id.tv_time_end);
			ImageView iv3 = (ImageView) rootView.findViewById(R.id.dropdown);
			ImageView iv4 = (ImageView) rootView.findViewById(R.id.dropdown1);			
			TextView tv3 = (TextView) rootView.findViewById(R.id.tv_time_start_up);
			TextView tv2 = (TextView) rootView.findViewById(R.id.tv_time_start_down);
			EditText et = (EditText) rootView.findViewById(R.id.in_drop);
			switch(checkedId){
			case R.id.opt_ct:
	        	rv.setVisibility(View.VISIBLE);
	        	iv.setVisibility(View.GONE);
	        	tv.setVisibility(View.GONE);
	        	tb.setVisibility(View.VISIBLE);
	        	iv2.setVisibility(View.GONE);
	        	iv1.setVisibility(View.VISIBLE);
	        	tv1.setVisibility(View.VISIBLE);
	        	tb2.setVisibility(View.GONE);
	        	tv1.setText("TIME");
	        	iv3.setVisibility(View.GONE);
	        	iv4.setVisibility(View.VISIBLE);
	        	tv3.setVisibility(View.GONE);
	        	tv2.setVisibility(View.GONE);
	        	et.setHint("Drop");
	        	trip_type = "city_taxi";
				setCityTaxiRateCardsUI();
	        	break;
			case R.id.opt_lo:
	        	rv.setVisibility(View.GONE);
	        	iv.setVisibility(View.VISIBLE);
	        	tv.setVisibility(View.VISIBLE);
	        	tb.setVisibility(View.GONE);
	        	iv1.setVisibility(View.GONE);
	        	iv2.setVisibility(View.GONE);
	        	tv1.setVisibility(View.GONE);
	        	tb2.setVisibility(View.VISIBLE);
	        	iv3.setVisibility(View.VISIBLE);
	        	iv4.setVisibility(View.GONE);
	        	tv3.setVisibility(View.GONE);
	        	et.setHint("Drop");
	        	tv2.setVisibility(View.GONE);
	        	trip_type = "local";
			    setLocalRateCardsUI();
                break;
			case R.id.opt_os:
	        	rv.setVisibility(View.VISIBLE);
	        	iv.setVisibility(View.VISIBLE);
	        	tv.setVisibility(View.VISIBLE);
	        	tb.setVisibility(View.GONE);
	        	iv1.setVisibility(View.GONE);
	        	iv2.setVisibility(View.VISIBLE);
	        	tb2.setVisibility(View.GONE);
	        	tv1.setVisibility(View.VISIBLE);
	        	iv3.setVisibility(View.VISIBLE);
	        	iv4.setVisibility(View.VISIBLE);
	        	tv3.setVisibility(View.GONE);
	        	tv2.setVisibility(View.GONE);
	        	et.setHint("Destination");
	        	tv1.setText("DAYS");
	        	trip_type = "outstation";
			    setOutstationRateCardsUI();
                break;
			}
		}
	}

	private void setRateCard(RateCard rc) {
    	Log.i("Batchcar","Setting rate card for "+ rc.vendor + rc.car_type);

		switch(rc.type){
		case 0:
			((TextView)rootView.findViewById(R.id.first_1)).setText("Rs."+rc.rpkm);
			((TextView)rootView.findViewById(R.id.first_2)).setText("per km");
			((TextView)rootView.findViewById(R.id.second_1)).setText("Rs."+rc.wtpm);
			((TextView)rootView.findViewById(R.id.second_2)).setText("waiting");
			((TextView)rootView.findViewById(R.id.third_1)).setText("Rs."+rc.min_fare);
			((TextView)rootView.findViewById(R.id.third_2)).setText("min fare");
			break;
		case 1:
			((TextView)rootView.findViewById(R.id.first_1)).setText("Rs."+rc.rpkm);
			((TextView)rootView.findViewById(R.id.first_2)).setText("per km");
			((TextView)rootView.findViewById(R.id.second_1)).setText(rc.min_kmpd+" km");
			((TextView)rootView.findViewById(R.id.second_2)).setText("per day");
			((TextView)rootView.findViewById(R.id.third_1)).setText("Rs."+rc.da);
			((TextView)rootView.findViewById(R.id.third_2)).setText("driver allowance");
			break;
		case 2:
			((TextView)rootView.findViewById(R.id.first_1)).setText("Rs. " + rc.eight_eighty);
			((TextView)rootView.findViewById(R.id.first_2)).setText("8hrs 80kms");
			((TextView)rootView.findViewById(R.id.second_1)).setText("Rs."+rc.add_km);
			((TextView)rootView.findViewById(R.id.second_2)).setText("extra km");
			((TextView)rootView.findViewById(R.id.third_1)).setText("Rs."+rc.add_hr);
			((TextView)rootView.findViewById(R.id.third_2)).setText("extra hour");
			break;
		}
	}
	
	/* To show the rate card on long press of any icons. It does that by hiding pickup drop and
	 * showing a grey rate card instead. handled again by hude/unhide of views. It also sets 
	 * the values of cartype and vendors because any touch is considered as selected.
	*/
	private final OnTouchListener rate_card_listener = new View.OnTouchListener() {

		public boolean onTouch(View v, MotionEvent event) {
			int x = rg_tt.getCheckedRadioButtonId();
        	int y = v.getId();
        	RateCard temp = null;
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
	        	switch(x){
	        	case R.id.opt_ct:
	        		switch(y){
	        		case R.id.opt_one:
	        			temp = ct1;
	        			break;
	        		case R.id.opt_two:
	        			temp = ct2;
	        			break;
	        		case R.id.opt_three:
	        			temp = ct3;
	        			break;
	        		case R.id.opt_four:
	        			temp = ct4;
		            	break;		        			
	        		}
	            	resetDrawables(0);
	        		break;
	        	case R.id.opt_lo:
	        		switch(y){
	        		case R.id.opt_one:
	        			temp = l1;
	        			break;
	        		case R.id.opt_two:
	        			temp = l2;
	        			break;
	        		case R.id.opt_four:
	        			temp = l3;
	        			break;
	        		case R.id.opt_three:
	        			temp = l4;
	        			break;
	        		}
	        		resetDrawables(1);
	        		break;
	        	case R.id.opt_os:
	        		switch(y){
	        		case R.id.opt_one:
	        			temp = o1;
	        			break;
	        		case R.id.opt_two:
	        			temp = o2;
	        			break;
	        		case R.id.opt_four:
	        			temp = o3;
	        			break;
	        		case R.id.opt_three:
	        			temp = o4;
	        			break;		        			
	        		}
	            	resetDrawables(2);
	        		break;
	        	}
	        	if("coolcab".equals(temp.vendor.toLowerCase()))
	        		vendor = "ytaxi";
	        	else if("tabcab".equals(temp.vendor.toLowerCase()))
	        		vendor = "tab_cab";
	        	else
	        		vendor = temp.vendor.toLowerCase();
	        	if("Sedan".equals(temp.car_type))
	        		car_type = SEDAN;
	        	if("Innova".equals(temp.car_type))
	        		car_type = INNOVA;
	        	if("Tavera".equals(temp.car_type))
	        		car_type = TAVERA;
	        	if("Compact".equals(temp.car_type))
	        		car_type = COMPACT;
    			setRateCard(temp);
    			rg_ct.clearCheck();
            	rg_ct.check(y);
            	setDrawable(y,temp.icon_key,true);
				rootView.findViewById(R.id.rate_card).setVisibility(View.VISIBLE);
            	return true;
            } else if(event.getAction() == MotionEvent.ACTION_UP) {
    			rootView.findViewById(R.id.rate_card).setVisibility(View.GONE);
                return true;
            }
            return false;
        }
    };
    
	private final OnClickListener confirm_btn_listener = new View.OnClickListener(){

		public void onClick(View v) {
			//checks whether all fields are set
			checkBooking();
        }
		
		private void checkBooking(){
        	String details = "";
        	
        	if(startDT == null){
	              Toast.makeText(mainContext,"Please set a pickup time!" , Toast.LENGTH_SHORT).show();
	              return;
	        }
        	
        	int x = rg_tt.getCheckedRadioButtonId();
        	if(x == R.id.opt_ct){
	        	details+= "Trip type: " + "City Taxi\n";
        		time = startDT.getDateTime().split(" ")[1];
        		Calendar c = Calendar.getInstance();
        		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	        	if(((ToggleButton)rootView.findViewById(R.id.toggle_btn)).isChecked()){
	        		date = df.format(c.getTime());
	        		Log.i("BA","Date is : " + date);
	        	}
	        	else{
	        		c.add(Calendar.DAY_OF_YEAR, 1);
	        		date = df.format(c.getTime());
	        		Log.i("BA","Date is : " + date);
	        	}
	        	datetime = date + " " + time;
        	}
        	else if (x == R.id.opt_lo){
	        	details+= "Trip type: " + "Local\n";
        		if(((ToggleButton)rootView.findViewById(R.id.toggle_btn2)).isChecked()){
	        		package_type = "4hrs 40kms";
		        	details+= "Package: Half Day\n" ;
	        	}
	        	else{
	        		package_type = "8hrs 80kms";
		        	details+= "Package: Full Day\n" ;
	        	}
        		datetime = startDT.getDateTime();
        	}
        	else{
	        	details+= "Trip type: " + "Outstation\n" ;
        		numDays = Integer.toString(dp.getDays());
        		datetime = startDT.getDateTime();
        		if(dp == null || dp.getDays() < 0){
		              Toast.makeText(mainContext,"Please set number of days!" , Toast.LENGTH_SHORT).show();
		              return;
	        	}
	        	details+= "Number of Days: " + numDays + "\n";
        	}

        	details += "Car Type: " + car_type + "\n";
        	
        	trip_params = new JSONObject();
        	trip = new JSONObject();
        	
        	if(from_ca.getAddressString().length() < 2){
	              Toast.makeText(mainContext,"Please select a pickup destination!" , Toast.LENGTH_SHORT).show();
	              return;
        	}
        	details+= "From: " + from_ca.getAddressString() +"\n" ;
        	if(to_ca.getAddressString().length() < 2 ){
        		if(trip_type.equalsIgnoreCase("local")){
        			Log.i("test","DONE");
        		}else{
        			Toast.makeText(mainContext,"Please select a drop destination!" , Toast.LENGTH_SHORT).show();
		            return;
        		}
        	}
        	if(!trip_type.equalsIgnoreCase("local")){
	        	details+= "To: " + to_ca.getAddressString() +"\n" ;
        	}
        	
        	details+= "Date & Time: " + datetime +"hrs\n" ;
        		        	
        	new AlertDialog.Builder(mainContext)
        	.setTitle("Do you want to confirm your booking?")
        	.setMessage("Your booking details are:\n" + details)
        	.setIcon(android.R.drawable.ic_dialog_alert)
        	.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int whichButton) {
        	        makeCabregatorBooking();
        	    }})
        	 .setNegativeButton(android.R.string.no, null).show();
        	
		}
		
		private void makeCabregatorBooking() {
			SharedPreferences prefs;
	        prefs = ServerUtilities.getAppSharedPreferences(mainContext);
	        reg_id = ServerUtilities.getRegistrationId(mainContext);
	        mobile = prefs.getString("com.sharedcab.batchcar.mobile", null);
	        email = prefs.getString("com.sharedcab.batchcar.email", null);
	        Log.i("test", "Reg Id is " + reg_id + " and mobile is " + mobile);

	        try {
	        	JSONObject cust_map = new JSONObject();
				cust_map.put("email",email);
				cust_map.put("mobile",mobile);
				JSONObject from_map = new JSONObject();
	        	JSONObject from_add_map = new JSONObject();
	        	from_map.put("name",from_ca.getAddressString());
				from_map.put("lat",from_ca.getLatitude());
	        	from_map.put("lng",from_ca.getLongitude());
	        	from_add_map.put("location",from_map);
	        	from_add_map.put("address", from_ca.getAddressString());
	        	JSONObject to_map = new JSONObject();
	        	JSONObject to_add_map = new JSONObject();
	        	if(trip_type == "local"){
	        		to_map.put("name",from_ca.getAddressString());
		        	to_map.put("lat",from_ca.getLatitude());
		        	to_map.put("lng",from_ca.getLongitude());
		        	to_add_map.put("location",from_map);
		        	to_add_map.put("address", from_ca.getAddressString());
	        	}
	        	else{
	        		to_map.put("name",to_ca.getAddressString());
		        	to_map.put("lat",to_ca.getLatitude());
		        	to_map.put("lng",to_ca.getLongitude());
		        	to_add_map.put("location",to_map);
		        	to_add_map.put("address", to_ca.getAddressString());
	        	}
	        	JSONObject data_map = new JSONObject();
	        	data_map.put("car_type", car_type);
	        	data_map.put("package_type", package_type);
	        	data_map.put("number_of_days", numDays);
	        	trip_params.put("customer", cust_map);
	        	trip_params.put("from_address",from_add_map);
	        	trip_params.put("to_address",to_add_map);
	        	trip_params.put("datetime", datetime + ":00");
	        	trip_params.put("type", trip_type);
	        	trip_params.put("data", data_map);

	        	trip.put("trip", trip_params);
	        	JSONArray vendors = new JSONArray();
	        	vendors.put(vendor);
	        	trip.put("vendors",vendors);
			} catch (JSONException e) {
				e.printStackTrace();
			}
//	        trip is the JSON with all the details
        	new TripbookerTask(0).execute(trip);
		}
	};
	
    private class GeocoderTask extends AsyncTask<LatLng, Void, List<Address>>{
    	int type;
    	ProgressBar pb;
    	
    	public GeocoderTask(int whichType){
    		type = whichType;
    		if(type==0)
    			pb = (ProgressBar) rootView.findViewById(R.id.pb1);
    		else
    			pb = (ProgressBar) rootView.findViewById(R.id.pb2);
    	}
    	
        @Override
        protected List<Address> doInBackground(LatLng... cur_center) {
            Geocoder geocoder = new Geocoder(mainContext);
            List<Address> addresses = null;
            try {
            	addresses = geocoder.getFromLocation(cur_center[0].latitude,cur_center[0].longitude, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return addresses;
        }
        
        @Override
        protected void onPreExecute() {
        	super.onPreExecute();
        	int id = type == 0? R.id.in_pickup : R.id.in_drop;
    		((EditText)rootView.findViewById(id)).setHint("Fetching Location...");
    		((EditText)rootView.findViewById(id)).setText("");
    		pb.setVisibility(View.VISIBLE);
        }
        	        
        @Override
        protected void onPostExecute(List<Address> addresses) {
    		pb.setVisibility(View.INVISIBLE);
            if(addresses==null || addresses.size()==0){
                Toast.makeText(mainContext, "No Location found", Toast.LENGTH_SHORT).show();
            }
            else{
            	if(type == 0){
	            	map_pickup = addresses.get(0);
	            	String full_address = "";
	        		for(int i = 0; i < map_pickup.getMaxAddressLineIndex()-1;i++){
	        			full_address += map_pickup.getAddressLine(i);
	        		}
	                p_latlng_set = true;
	        		EditText pickup_address = (EditText) rootView.findViewById(R.id.in_pickup); 
	                CharSequence c = full_address;
	                pickup_address.setText(c);
	                pickup_address.setHint("Pickup");	                
                	from_ca.setAddressString(full_address);
                	from_ca.setLatitude(Double.toString(map_pickup.getLatitude()));
                	from_ca.setLongitude(Double.toString(map_pickup.getLongitude()));
            	}
            	else{
            		map_drop = addresses.get(0);
	            	String full_address = "";
	        		for(int i = 0; i < map_drop.getMaxAddressLineIndex()-1;i++){
	        			full_address += map_drop.getAddressLine(i);
	        		}
	                d_latlng_set = true;
	        		EditText drop_address = (EditText) rootView.findViewById(R.id.in_drop); 
	                CharSequence c = full_address;
	                drop_address.setHint("Drop");
	                drop_address.setText(c);
                	to_ca.setAddressString(full_address);
                	to_ca.setLatitude(Double.toString(map_drop.getLatitude()));
                	to_ca.setLongitude(Double.toString(map_drop.getLongitude()));
            	}
            } 
        }
    }
    
    private final OnCameraChangeListener onPickupChangeListener = 
            new OnCameraChangeListener() {

        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            LatLng x = googleMap.getCameraPosition().target;
            if(p_geocode!= null)
            	p_geocode.cancel(true);
            p_geocode = new GeocoderTask(0);
            p_geocode.execute(x);
        }
    };
    
    private final OnCameraChangeListener onDropChangeListener = 
            new OnCameraChangeListener() {

        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            LatLng x = googleMap.getCameraPosition().target;
            if(d_geocode!= null)
            	d_geocode.cancel(true);
            d_geocode = new GeocoderTask(1);
            d_geocode.execute(x);
        }
    };
		
}    	
