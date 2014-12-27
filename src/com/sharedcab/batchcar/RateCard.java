package com.sharedcab.batchcar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.internal.e;
import com.google.android.gms.internal.ei;

public class RateCard {

	int type;
	String vendor="";
	String vendor_key = "";
	String car_type="";
	double rpkm;
	double wtpm;
	int min_fare;
	String icon_key="";
	double book_charge; //double
	double first_km; //double
	String toll_policy="";
	int da;
	int min_kmpd;
	int fix_rate;		//for 4x40/8x80
	double add_km;
	int add_hr;
	double garage_vicinity;
	int four_forty;
	int eight_eighty;
	/*
	 * LOCAL:
	 * 		"car_type": "Compact",
			"4x40": 700, //int
			"8x80": 1200, //int
			"add_km": 8.5,	//double
			"add_hour": 75,	//int
			"toll_policy": "paid by customer",
			"icon_key": "compact",
			"book_charge": 0.0, //double
			"vicinity": 5.0 //double
		OUTSTATION:
			"car_type": "Tavera",
			"rpkm": 9.5, //double
			"driver_allowance": 250, //int
			"min_kmpd": 250,	//int
			"toll_policy": "paid by customer",
			"icon_key": "tavera",
			"book_charge": 0.0, //double
			"vicinity": 5.0 //double
		CITY_TAXI:
			"vendor": "Meru",
			"rpkm": 21.0, //double
			"wait_charge": 2.0, //double
			"min_fare": 150,	//int
			"icon_key": "sedan",
			"book_charge": 30.0, //double
			"first_km": 25.0, //double
	*/
	public RateCard(int whichType, String json_body){
		type = whichType;
		JSONObject o;
		try {
			o = new JSONObject(json_body);
			icon_key = o.getString("icon_key");
			book_charge = o.getDouble("book_charge");
			if(type == 0){
				//city_taxi
				vendor = o.getString("vendor");
				vendor_key = o.getString("vendor_key");
				rpkm = o.getDouble("rpkm");
				wtpm = o.getDouble("wait_charge");
				min_fare = o.getInt("min_fare");
				first_km = o.getDouble("first_km");
			}
			else if(type == 1){
				//outstation
				car_type = o.getString("car_type");
				rpkm = o.getDouble("rpkm");
				da = o.getInt("driver_allowance");
				min_kmpd = o.getInt("min_kmpd");
				garage_vicinity = o.getDouble("vicinity");
				toll_policy = o.getString("toll_policy");
			}
			else if(type == 2){
				//local
				car_type = o.getString("car_type");
				four_forty = o.getInt("4x40");
				eight_eighty = o.getInt("8x80");
				add_hr = o.getInt("add_hour");
				add_km = o.getDouble("add_km");
				garage_vicinity = o.getDouble("vicinity");
				toll_policy = o.getString("toll_policy");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
