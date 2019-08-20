package com.sid.shaheenfalcon;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import java.io.Serializable;
import java.util.ArrayList;

public class SFScriptInterface {

    Context context = null;
    ArrayList<SFRequest> requests = null;
    String url = "", userAgentString = "";

    public SFScriptInterface() {

    }

    public SFScriptInterface(Context context) {
        this.context = context;
    }

    public SFScriptInterface(Context context, String url, String userAgentString, ArrayList<SFRequest> requests) {
        this.context = context;
        this.url = url;
        this.requests = requests;
        this.userAgentString = userAgentString;
    }

    @JavascriptInterface
    public void parseData(String scriptToExecute, String data){
        Intent i = new Intent(context, Script.class);
        i.putExtra("URL", this.url);
        i.putExtra("EXTRA_DATA", data);
        i.putExtra("SCRIPT", scriptToExecute);
        i.putExtra("USER_AGENT", this.userAgentString);
        Bundle b = new Bundle();
        b.putSerializable("REQUESTS", (Serializable) requests);
        i.putExtra("BREQUESTS", b);
        context.startActivity(i);
    }

}
