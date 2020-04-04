package com.sid.shaheenfalcon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class RequestListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<SFRequest> requests;

    RequestListAdapter(Context context, ArrayList<SFRequest> requests) {
        this.context = context;
        this.requests = requests;
    }

    @Override
    public int getCount() {
        return this.requests.size();
    }

    @Override
    public Object getItem(int i) {
        return this.requests.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        String url = this.requests.get(i).getUrl();
        view = LayoutInflater.from(context).inflate(R.layout.request_list_item, viewGroup, false);

        ((TextView) view.findViewById(R.id.request_list_item_method)).setText(this.requests.get(i).getMethod());
        ((TextView) view.findViewById(R.id.request_list_item_path)).setText(url.substring(url.lastIndexOf('/')).split("\\?")[0]);
        ((TextView) view.findViewById(R.id.request_list_item_url)).setText(url);

        return view;
    }
}
