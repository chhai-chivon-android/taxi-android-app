package com.sharedcab.batchcar;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class TouchableWrapper extends FrameLayout {
	MainActivity mainActivity;

	public TouchableWrapper(MainActivity a) {
	   super(a);
	   mainActivity = a;
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
	 switch (ev.getAction()) {
	   case MotionEvent.ACTION_DOWN:
		   ((RouteFragment)mainActivity.mainFragment).mapClicked();
	       break;
	   case MotionEvent.ACTION_UP:
           break;
	   }
	
	   return super.dispatchTouchEvent(ev);
	 }
	
}