package com.sharedcab.batchcar;

import java.util.HashMap;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

public class NoFilterArrayAdapter extends ArrayAdapter<HashMap<String, String>> {
    NoFilter noFilter;

    public NoFilterArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }
    
    public Filter getFilter() {
        if (noFilter == null) {
            noFilter = new NoFilter();
        }
        return noFilter;
    }

    private class NoFilter extends Filter {
        protected FilterResults performFiltering(CharSequence prefix) {
            return new FilterResults();
        }

        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            // Do nothing
        }
    }
}
