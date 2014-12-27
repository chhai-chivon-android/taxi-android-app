package com.sharedcab.batchcar;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class GooglePlaces {

    private static final String LOG_TAG = "Batchcar";

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";

    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";

    private static final String TYPE_DETAILS = "/details";

    private static final String OUT_JSON = "/json";

    private String apiKey;

    public GooglePlaces(String apiKey) {
        this.apiKey = apiKey;
    }

    public ArrayList<HashMap<String,String>> autocomplete(String input) {
        ArrayList<HashMap<String,String>> resultList = null;

        StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
        sb.append("?sensor=true&key=" + apiKey);
            sb.append("&components=country:in");
            sb.append("&location=19.1167,72.8333&radius=50000");
            sb.append("&components=country:in");

        try {
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        }

        try {
            JSONObject jsonObj = new JSONObject(downloadStuff(sb.toString()));
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            resultList = new ArrayList<HashMap<String,String>>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                JSONObject item = predsJsonArray.getJSONObject(i);
                HashMap<String, String> newhm = new HashMap<String, String>();
                newhm.put("description", item.getString("description"));
                newhm.put("reference", item.getString("reference"));
                resultList.add(newhm);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }
        return resultList;
    }

    public CustomAddress location(String reference) {
        JSONObject json = details(reference);
        try {
            JSONObject result = json.getJSONObject("result");
            JSONObject location = result.getJSONObject("geometry").getJSONObject("location");
            String name = result.getString("formatted_address");
            String lat = Double.toString(location.getDouble("lat"));
            String lng = Double.toString(location.getDouble("lng"));
            return new CustomAddress(name, lat, lng);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }
        return null;
    }

    public JSONObject details(String reference) {
        StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_DETAILS + OUT_JSON);
        sb.append("?sensor=true&key=" + apiKey);
        sb.append("&reference=" + reference);

        try {
            return new JSONObject(downloadStuff(sb.toString()));
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }
        return null;
    }

    private static String downloadStuff(String uri) {
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            URL url = new URL(uri);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return jsonResults.toString();
    }
	
}
