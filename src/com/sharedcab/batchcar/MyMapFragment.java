package com.sharedcab.batchcar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.MapFragment;

public class MyMapFragment extends MapFragment{

	public View mOriginalContentView;
	public TouchableWrapper mTouchView;
	RouteFragment main;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);
		mTouchView = new TouchableWrapper((MainActivity)getActivity());
		mTouchView.addView(mOriginalContentView);
		return mTouchView;
	}

	@Override
	public View getView() {
	    return mOriginalContentView;
	}
	
}
