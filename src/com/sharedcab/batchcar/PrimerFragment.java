package com.sharedcab.batchcar;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PrimerFragment extends Fragment{
	
	Context mainContext;
    MainActivity mainActivity;
    View rootView;
	
	@Override
    public void onAttach(Activity a){
    	super.onAttach(a);
    	mainContext = a;
    	mainActivity = (MainActivity) a;    
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    }
    
   
    @Override
    public void onResume() {
        super.onResume();
        Log.d("OnResume View", "Sucess");
    }
  
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
        rootView = inflater.inflate(R.layout.primer_layout, container, false);
        return rootView;
    }
    
    @Override
  	public void onActivityCreated(Bundle savedInstanceState) {
  		super.onActivityCreated(savedInstanceState);
        mainContext = getActivity();
    }
}
