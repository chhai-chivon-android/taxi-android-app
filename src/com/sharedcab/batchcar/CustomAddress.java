package com.sharedcab.batchcar;

import android.os.Parcel;
import android.os.Parcelable;

class CustomAddress implements Parcelable{
	
	private String addressString;
	private String latitude;
	private String longitude;
	
	public CustomAddress(String add, String lat, String lng){
         this.addressString = add;
         this.latitude = lat;
         this.longitude = lng;
    }
 
	// Parcelling part
    public CustomAddress(Parcel in){
        String[] data = new String[3];

        in.readStringArray(data);
        this.addressString = data[0];
        this.latitude = data[1];
        this.longitude = data[2];
    }

    public CustomAddress(){
    	this.addressString = "";
    	this.latitude = "";
    	this.longitude = "";
    }
    

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeStringArray(new String[] {this.addressString,this.latitude,this.longitude});
		
	}
	
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public CustomAddress createFromParcel(Parcel in) {
            return new CustomAddress(in); 
        }

        public CustomAddress[] newArray(int size) {
            return new CustomAddress[size];
        }
    };

	public String getAddressString() {
		return addressString;
	}

	public void setAddressString(String address) {
		this.addressString = address;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		
		this.longitude = longitude;
	}
	
}
