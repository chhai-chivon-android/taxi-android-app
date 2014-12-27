package com.sharedcab.batchcar;

import org.apache.http.impl.client.DefaultHttpClient;

public class HttpClient {

	public static DefaultHttpClient instanCE;
	private static final String TAG = "&&----HTTPClient-----**";
	
	private HttpClient() {
	    //private constructor
	}
	
	public static DefaultHttpClient getInstance(){
	    if(instanCE == null){
	        instanCE = new DefaultHttpClient();
	    }
	    return instanCE;
	}
}
