package com.sid.shaheenfalcon;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import com.google.android.exoplayer2.util.Log;

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
//        Intent i = new Intent(context, Script.class);

        if(!isMyServiceRunning(SFScriptExecutorService.class)){
            Intent serviceIntent = new Intent(this.context, SFScriptExecutorService.class);
            serviceIntent.putExtra("URL", this.url);
            serviceIntent.putExtra("EXTRA_DATA", data);
            serviceIntent.putExtra("SCRIPT", scriptToExecute);
            serviceIntent.putExtra("USER_AGENT", this.userAgentString);
//            Bundle b = new Bundle();
//            b.putSerializable("REQUESTS", (Serializable) requests);
//            serviceIntent.putExtra("BREQUESTS", b);
//        context.startActivity(i);
            serviceIntent.putExtra("TYPE", SFScriptExecutorService.TYPE_EXEC);
            SFScriptExecutorService.scriptRequests.add((ArrayList<SFRequest>) requests.clone());
            this.context.startService(serviceIntent);
        }else{
            Intent i = new Intent("com.sid.ShaheenFalcon.SFScriptExecutorService");
            i.putExtra("URL", this.url);
            i.putExtra("EXTRA_DATA", data);
            i.putExtra("SCRIPT", scriptToExecute);
            i.putExtra("USER_AGENT", this.userAgentString);
//            Bundle b = new Bundle();
//            b.putSerializable("REQUESTS", (Serializable) requests);
//            i.putExtra("BREQUESTS", b);
//        context.startActivity(i);
            i.putExtra("TYPE", SFScriptExecutorService.TYPE_EXEC);
            Log.d("INTERFACE", "service running");
            SFScriptExecutorService.scriptRequests.add((ArrayList<SFRequest>) requests.clone());
            this.context.sendBroadcast(i);
        }
        Log.d("INTERFACE", "broadcast sent");

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) this.context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
