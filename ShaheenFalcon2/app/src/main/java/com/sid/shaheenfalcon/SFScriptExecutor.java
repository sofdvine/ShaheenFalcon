package com.sid.shaheenfalcon;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.sid.shaheenfalcon.SFScriptExecutorService.SCRIPT_EXEC_DONE;
import static com.sid.shaheenfalcon.SFScriptExecutorService.SCRIPT_INPUT;
import static com.sid.shaheenfalcon.SFScriptExecutorService.SCRIPT_OPTIONS;
import static com.sid.shaheenfalcon.SFScriptExecutorService.SCRIPT_THREAD_INDEX;
import static com.sid.shaheenfalcon.SFScriptExecutorService.TYPE_EVENT;


public class SFScriptExecutor extends Thread {
    private  V8 v8 = null;
    private String userAgentString = "", extraData = "", scriptCode = "", scriptLocation = "", url = "";
    private int threadIdx;
    private boolean isDone = false, keepAlive = false;
    ArrayList<SFRequest> requests = null;
    Context context = null;
    public int choosenOption = -1;
    public String txtInput = null, evtName;

    SFScriptExecutor(Context context, String scriptLocation, String url, ArrayList<SFRequest> requests, String extraData, String userAgentString, int threadIdx){
        this.context = context;
        this.userAgentString = userAgentString;
        this.requests = requests;
        this.extraData = extraData;
        this.scriptLocation = scriptLocation;
        this.url = url;
        this.threadIdx = threadIdx;

        StringBuilder sb = new StringBuilder();

        if(!scriptLocation.equals("")){
            File scriptFile = new File(scriptLocation);
            try {
                FileInputStream fis = new FileInputStream(scriptFile);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                String line = "";
                while((line = br.readLine()) != null){
                    sb.append(line);
                    sb.append('\n');
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            scriptCode = sb.toString();
        }
    }

    @Override
    public void run() {

        v8 = V8.createV8Runtime();

        v8.executeVoidScript("const ShaheenFalcon = {" +
                "_events: {}," +
                "addEventListeners: function(evtName, func){" +
                "   if (!this._events.hasOwnProperty(evtName)) {" +
                "       this._events[evtName] = [];" +
                "   }" +
                "   this._events[evtName].push(func);" +
                "}," +
                "fireEvent: function (evtName, ...evtData) {" +
                "   this._events[evtName].forEach( function(func) { " +
                "       func(...evtData);" +
                "   })" +
                "}" +
                "}");

        v8.registerJavaMethod(SFScriptExecutor.this, "print", "print", new Class<?>[] {String.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "input", "input", new Class<?>[] {String.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "playVideo", "playVideo", new Class<?>[] {String.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "playAudio", "playAudio", new Class<?>[] {String.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "exoPlayVideo", "exoPlayVideo", new Class<?>[] {String.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "exoPlayVideo", "exoPlayVideoWithHeaders", new Class<?>[] {String.class, V8Object.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "showOpenWithIntent", "showOpenWithIntent", new Class<?>[] {String.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "exoPlayDRMVideo", "exoPlayDRMVideo", new Class<?>[] {String.class, String.class, String.class, V8Array.class, boolean.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "exoPlayDRMVideo", "exoPlayDRMVideoWithHeaders", new Class<?>[] {String.class, String.class, String.class, V8Array.class, boolean.class, V8Object.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "openUrlInBrowser", "openUrlInBrowser", new Class<?>[] {String.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "openUrlInBrowserWithHeaders", "openUrlInBrowserWithHeaders", new Class<?>[] {String.class, V8Object.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "getRequests", "getRequests", new Class<?>[] {} );
        v8.registerJavaMethod(SFScriptExecutor.this, "findRequestWithHeader", "findRequestWithHeader", new Class<?>[] {String.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "getExtraData", "getExtraData", new Class<?>[] {} );
        v8.registerJavaMethod(SFScriptExecutor.this, "getUrl", "getUrl", new Class<?>[] {} );
        v8.registerJavaMethod(SFScriptExecutor.this, "getUserAgentString", "getUserAgentString", new Class<?>[] {} );
        v8.registerJavaMethod(SFScriptExecutor.this, "getBrowserCookies", "getBrowserCookies", new Class<?>[] {String.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "chooseOptions", "chooseOptions", new Class<?>[] {V8Array.class} );
        v8.registerJavaMethod(SFScriptExecutor.this, "httpRequest", "httpRequest", new Class<?>[] {V8Object.class} );
        //v8.executeVoidScript("var a = 10; a = a + ''; print(JSON.stringify(httpRequest(getRequests()[0])));");

        try {
            v8.executeVoidScript(scriptCode);
            while (keepAlive && !isDone) {
                this.wait();
                v8.executeVoidScript("ShaheenFalcon && ShaheenFalcon.fireEvent(\"" + evtName + "\");");
            }
        } catch (Exception e) {
            e.printStackTrace();
//            Intent i = new Intent("com.sid.ShaheenFalcon.Script");
//            i.putExtra("ERROR", e.getLocalizedMessage());
//            context.sendBroadcast(i);
        }finally {
            try{
                Intent intent = new Intent("com.sid.ShaheenFalcon.ScriptUIReceiver");
                intent.putExtra("TYPE", TYPE_EVENT);
                intent.putExtra(SCRIPT_THREAD_INDEX, threadIdx);
                intent.putExtra(SCRIPT_EXEC_DONE, true);
                this.context.sendBroadcast(intent);
                v8.terminateExecution();
                v8.release(false);
            }catch (Exception ex){

            }
        }
        //v8.release();
    }
    public String getUrl(){
        return this.url;
    }

    public String getExtraData(){
        return this.extraData;
    }
    public String getUserAgentString(){
        return this.userAgentString;
    }

    public V8Array findRequestWithUrl(String url){
        V8Array arr = new V8Array(v8);
        for(SFRequest request: requests){
            if(request.getUrl().matches(url)){
                V8Object req = new V8Object(v8);
                V8Object headers = new V8Object(v8);
                req.add("url", request.getUrl());
                req.add("method", request.getMethod());
                for(String key : request.getHeaders().keySet()){
                    headers.add(key, request.getHeaders().get(key));
                }
                req.add("headers", headers);
                arr.push(req);
            }
        }
        return arr;
    }

    public V8Array findRequestWithHeader(String headerName){
        V8Array arr = new V8Array(v8);
        for(SFRequest request: requests){
            boolean toAdd = false;
            //if(request.getUrl().matches(url)){
            V8Object req = new V8Object(v8);
            V8Object headers = new V8Object(v8);
            req.add("url", request.getUrl());
            req.add("method", request.getMethod());
            for(String key : request.getHeaders().keySet()){
                if((!headerName.equals("") && key.matches(headerName))) {
                    toAdd = true;
                }
                headers.add(key, request.getHeaders().get(key));
            }
            req.add("headers", headers);
            if(toAdd) {
                arr.push(req);
            }
            //}
        }
        return arr;
    }

    public int chooseOptions(V8Array opts){
        ArrayList<String> options = new ArrayList<String>();
        for(int i = 0; i < opts.length(); i++){

            try {
                options.add(opts.getObject(i).getString("option_title"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        choosenOption = -1;

        Intent intent = new Intent("com.sid.ShaheenFalcon.ScriptUIReceiver");
        intent.putExtra("TYPE", TYPE_EVENT);
        intent.putExtra(SCRIPT_THREAD_INDEX, threadIdx);
        intent.putExtra(SCRIPT_OPTIONS, options);
        this.context.sendBroadcast(intent);

        int temp = 0;
        while (choosenOption < 0) {
            temp++;
            Log.d("SCRIPT_EXEC_THREAD", choosenOption + "");
        }
        temp = choosenOption;
        choosenOption = -1;
        return temp;
    }

    public String input(String message){
        txtInput = null;
        Intent intent = new Intent("com.sid.ShaheenFalcon.ScriptUIReceiver");
        intent.putExtra("TYPE", TYPE_EVENT);
        intent.putExtra(SCRIPT_THREAD_INDEX, threadIdx);
        intent.putExtra(SCRIPT_INPUT, message);
        this.context.sendBroadcast(intent);

        while (txtInput == null){
            Log.d("SFSCRIPT", "NO INPUT");
        }
        String temp = txtInput;
        return temp;
    }

    public V8Object httpRequest(V8Object request){
        String url = request.getString("url");
        String method = request.getString("method");
        V8Object v8headers = request.getObject("headers");
        V8Object response = new V8Object(v8);
        String data = "";
        if(request.contains("data")){
            data = request.getString("data");
        }
        Map<String, String> headers = new HashMap<String, String>();
        for(String key : v8headers.getKeys()){
            headers.put(key, v8headers.getString(key));
        }
        OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).build();
        Request req = null;
        if(method.equalsIgnoreCase("GET")) {
            req = new Request.Builder().url(url).headers(Headers.of(headers)).get().build();
        }else{
            RequestBody body = RequestBody.create(MediaType.parse(headers.get("Content-Type")), data);
            req = new Request.Builder().url(url).headers(Headers.of(headers)).method(method, body).build();
        }
        try {
            Response res = client.newCall(req).execute();
            response.add("code", res.code());
            response.add("data", res.body().string());
            Log.d("SRCIPT_EXEC_THREAD", "RESPONSE RECEIVED");
            V8Object responseHeaders = V8ObjectUtils.toV8Object(v8, res.headers().toMultimap());
            response.add("headers", responseHeaders);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public V8Array getRequests(){
        V8Array arr = new V8Array(v8);
        for(SFRequest request : requests){
            V8Object req = new V8Object(v8);
            V8Object headers = new V8Object(v8);
            for(String key : request.getHeaders().keySet()){
                headers.add(key, request.getHeaders().get(key));
            }
            req.add("url", request.getUrl());
            req.add("method", request.getMethod());
            req.add("headers", headers);
            arr.push(req);
        }
        return arr;
    }

    public void print(String s){
//        Intent i = new Intent("com.sid.ShaheenFalcon.Script");
//        i.putExtra("PRINT", s);
//        this.context.sendBroadcast(i);
    }

    public void playVideo(String url){
        Intent i = new Intent(this.context, BasicVideoPlayer.class);
        i.putExtra("VIDEO_URL", url);
        this.context.startActivity(i);
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

    public void playAudio(String url){
        if (!isMyServiceRunning(SFAudioService.class)) {
            Intent i = new Intent(this.context, SFAudioService.class);
            i.putExtra("SF_AUDIO_URL", url);
            this.context.startService(i);
        }else {
            Intent broadcastIntent = new Intent("com.sid.ShaheenFalcon.SFAudioService");
            broadcastIntent.putExtra("SF_AUDIO_URL", url);
            this.context.sendBroadcast(broadcastIntent);
        }
    }

    public void exoPlayVideo(String url, V8Object headers){
        Log.d("SCRIPT_EXEC_THREAD", url);
        Intent intent = new Intent(this.context, PlayerActivity.class);
        intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, true);
        intent.putExtra(PlayerActivity.ABR_ALGORITHM_EXTRA, PlayerActivity.ABR_ALGORITHM_DEFAULT);
        intent.putExtra("PLAYER_USER_AGENT", this.userAgentString);
        if(headers != null) {
            HashMap<String, String> mHeaders = new HashMap<String, String>();
            for (String key : headers.getKeys()) {
                mHeaders.put(key, headers.get(key).toString());
            }
            intent.putExtra("EXTRA_HEADERS", mHeaders);
        }
        intent.setData(Uri.parse(url));
        this.context.startActivity(intent);
    }

    public void exoPlayVideo(String url){
        exoPlayVideo(url, null);
    }

    public void showOpenWithIntent(String url){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        this.context.startActivity(i);
    }

    public void exoPlayDRMVideo(String url, String drmLicenseUrl, String drmScheme, V8Array drmKeyRequestProperties, boolean drmMultiSession, V8Object headers){
        Intent intent = new Intent(this.context, PlayerActivity.class);
        intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, true);
        intent.putExtra(PlayerActivity.ABR_ALGORITHM_EXTRA, PlayerActivity.ABR_ALGORITHM_DEFAULT);
        intent.putExtra(PlayerActivity.DRM_SCHEME_EXTRA, drmScheme);
        intent.putExtra(PlayerActivity.DRM_LICENSE_URL_EXTRA, drmLicenseUrl);
        String[] jDrmKeyRequestProperties = new String[drmKeyRequestProperties.length()];
        V8ObjectUtils.toList(drmKeyRequestProperties).toArray(jDrmKeyRequestProperties);
        intent.putExtra(PlayerActivity.DRM_KEY_REQUEST_PROPERTIES_EXTRA,jDrmKeyRequestProperties);
        intent.putExtra(PlayerActivity.DRM_MULTI_SESSION_EXTRA, drmMultiSession);
        intent.putExtra("PLAYER_USER_AGENT", this.userAgentString);
        if(headers != null) {
            HashMap<String, String> mHeaders = new HashMap<String, String>();
            for (String key : headers.getKeys()) {
                mHeaders.put(key, headers.get(key).toString());
            }
            intent.putExtra("EXTRA_HEADERS", mHeaders);
        }
        intent.setData(Uri.parse(url));
        this.context.startActivity(intent);
    }

    public void exoPlayDRMVideo(String url, String drmLicenseUrl, String drmScheme, V8Array drmKeyRequestProperties, boolean drmMultiSession){
        exoPlayDRMVideo(url, drmLicenseUrl, drmScheme, drmKeyRequestProperties, drmMultiSession, null);
    }

    public String getBrowserCookies(String url){
        return CookieManager.getInstance().getCookie(url);
    }

    public void openUrlInBrowser(String url){
        Intent intent = new Intent(this.context, MainActivity.class);
        intent.setData(Uri.parse(url));
        this.context.startActivity(intent);
    }

    public void openUrlInBrowserWithHeaders(String url, V8Object headers){
        Intent intent = new Intent(this.context, MainActivity.class);
        intent.setData(Uri.parse(url));
        HashMap<String, String> mHeaders = new HashMap<String, String>();
        for(String key : headers.getKeys()){
            mHeaders.put(key, headers.get(key).toString());
        }
        intent.putExtra("HEADERS", mHeaders);
        this.context.startActivity(intent);
    }
}
