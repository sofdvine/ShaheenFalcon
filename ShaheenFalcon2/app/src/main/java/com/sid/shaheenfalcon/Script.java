package com.sid.shaheenfalcon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.util.Function;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Script extends AppCompatActivity {

    TextView tv = null;
    String url = "";
    ArrayList<SFRequest> requests = null;
    String scriptLocation = "", extraData = "", scriptCode = "", userAgentString = "";
    ArrayList<String> options = null;
    AlertDialog optionsDialog = null;

    private MyReceiver receiver;

    private int choosenOption = -1;
    private boolean hasInput = false;
    private String txtInput = "";

    V8 v8 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script);

        tv = (TextView) findViewById(R.id.console);

        receiver = new MyReceiver(new Handler()); // Create the receiver
        registerReceiver(receiver, new IntentFilter("com.sid.ShaheenFalcon.Script")); // Register receiver

        tv.setMovementMethod(new ScrollingMovementMethod());


        //getting bundled requests and url
        Bundle b = getIntent().getBundleExtra("BREQUESTS");
        requests = (ArrayList<SFRequest>) b.getSerializable("REQUESTS");
        url = getIntent().getStringExtra("URL");
        scriptLocation = getIntent().getStringExtra("SCRIPT");

        if(getIntent().hasExtra("USER_AGENT")){
            userAgentString = getIntent().getStringExtra("USER_AGENT");
        }

        extraData = "";

        if(getIntent().hasExtra("EXTRA_DATA")){
            extraData = getIntent().getStringExtra("EXTRA_DATA");
        }

        //Toast.makeText(getApplicationContext(), extraData, Toast.LENGTH_LONG).show();

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


        StrictMode.ThreadPolicy policy =
                new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                v8 = V8.createV8Runtime();

                v8.registerJavaMethod(Script.this, "print", "print", new Class<?>[] {String.class} );
                v8.registerJavaMethod(Script.this, "input", "input", new Class<?>[] {String.class} );
                v8.registerJavaMethod(Script.this, "playVideo", "playVideo", new Class<?>[] {String.class} );
                v8.registerJavaMethod(Script.this, "exoPlayVideo", "exoPlayVideo", new Class<?>[] {String.class} );
                v8.registerJavaMethod(Script.this, "showOpenWithIntent", "showOpenWithIntent", new Class<?>[] {String.class} );
                v8.registerJavaMethod(Script.this, "exoPlayDRMVideo", "exoPlayDRMVideo", new Class<?>[] {String.class, String.class, String.class, V8Array.class, boolean.class} );
                v8.registerJavaMethod(Script.this, "openUrlInBrowser", "openUrlInBrowser", new Class<?>[] {String.class} );
                v8.registerJavaMethod(Script.this, "openUrlInBrowserWithHeaders", "openUrlInBrowserWithHeaders", new Class<?>[] {String.class, V8Object.class} );
                v8.registerJavaMethod(Script.this, "getRequests", "getRequests", new Class<?>[] {} );
                v8.registerJavaMethod(Script.this, "findRequestWithHeader", "findRequestWithHeader", new Class<?>[] {String.class} );
                v8.registerJavaMethod(Script.this, "getExtraData", "getExtraData", new Class<?>[] {} );
                v8.registerJavaMethod(Script.this, "getUrl", "getUrl", new Class<?>[] {} );
                v8.registerJavaMethod(Script.this, "getUserAgentString", "getUserAgentString", new Class<?>[] {} );
                v8.registerJavaMethod(Script.this, "getBrowserCookies", "getBrowserCookies", new Class<?>[] {String.class} );
                v8.registerJavaMethod(Script.this, "chooseOptions", "chooseOptions", new Class<?>[] {V8Array.class} );
                v8.registerJavaMethod(Script.this, "httpRequest", "httpRequest", new Class<?>[] {V8Object.class} );
                //v8.executeVoidScript("var a = 10; a = a + ''; print(JSON.stringify(httpRequest(getRequests()[0])));");

                try {
                    v8.executeVoidScript(scriptCode);
                } catch (Exception e) {
                    e.printStackTrace();
                    Intent i = new Intent("com.sid.ShaheenFalcon.Script");
                    i.putExtra("ERROR", e.getLocalizedMessage());
                    sendBroadcast(i);
                }finally {
                    try{
                        v8.release(false);
                    }catch (Exception ex){

                    }
                }

                //v8.release();

            }
        });
        t.start();

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

    public V8Object httpRequest(V8Object request){
        String url = request.getString("url");
        String method = request.getString("method");
        V8Object v8headers = request.getObject("headers");
        V8Object response = new V8Object(v8);
        String data = "";
        if(request.contains("data")){
            data = request.getString(data);
        }
        Map<String, String> headers = new HashMap<String, String>();
        for(String key : v8headers.getKeys()){
            headers.put(key, v8headers.getString(key));
        }
        OkHttpClient client = new OkHttpClient();
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
            V8Object responseHeaders = V8ObjectUtils.toV8Object(v8, res.headers().toMultimap());
            response.add("headers", responseHeaders);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public int chooseOptions(V8Array options){
        this.options = new ArrayList<String>();
        for(int i = 0; i < options.length(); i++){

            try {
                this.options.add(options.getObject(i).getString("option_title"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        choosenOption = -1;

        Intent intent = new Intent("com.sid.ShaheenFalcon.Script");
        intent.putExtra("OPEN_CONTEXT_MENU", true);
        sendBroadcast(intent);
        //openContextMenu(tv);
        while(choosenOption < 0){
            Log.d("RUNNING LOOP", "" + choosenOption);
        }
        optionsDialog.dismiss();
        Log.d("DONE CHOOSEN OPTION", "" + choosenOption);
        return choosenOption;

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
        Intent i = new Intent("com.sid.ShaheenFalcon.Script");
        i.putExtra("PRINT", s);
        sendBroadcast(i);
    }

    public void playVideo(String url){
        Intent i = new Intent(Script.this, BasicVideoPlayer.class);
        i.putExtra("VIDEO_URL", url);
        Script.this.startActivity(i);
    }

    public void exoPlayVideo(String url){
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, true);
        intent.putExtra(PlayerActivity.ABR_ALGORITHM_EXTRA, PlayerActivity.ABR_ALGORITHM_DEFAULT);
        intent.putExtra("PLAYER_USER_AGENT", userAgentString);
        intent.setData(Uri.parse(url));
        this.startActivity(intent);
    }

    public void showOpenWithIntent(String url){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        Script.this.startActivity(i);
    }

    public void exoPlayDRMVideo(String url, String drmLicenseUrl, String drmScheme, V8Array drmKeyRequestProperties, boolean drmMultiSession){
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, true);
        intent.putExtra(PlayerActivity.ABR_ALGORITHM_EXTRA, PlayerActivity.ABR_ALGORITHM_DEFAULT);
        intent.putExtra(PlayerActivity.DRM_SCHEME_EXTRA, drmScheme);
        intent.putExtra(PlayerActivity.DRM_LICENSE_URL_EXTRA, drmLicenseUrl);
        String[] jDrmKeyRequestProperties = new String[drmKeyRequestProperties.length()];
        V8ObjectUtils.toList(drmKeyRequestProperties).toArray(jDrmKeyRequestProperties);
        intent.putExtra(PlayerActivity.DRM_KEY_REQUEST_PROPERTIES_EXTRA,jDrmKeyRequestProperties);
        intent.putExtra(PlayerActivity.DRM_MULTI_SESSION_EXTRA, drmMultiSession);
        intent.setData(Uri.parse(url));
        this.startActivity(intent);
    }

    public String getBrowserCookies(String url){
        return CookieManager.getInstance().getCookie(url);
    }

    public void openUrlInBrowser(String url){
        Intent intent = new Intent(Script.this, MainActivity.class);
        intent.setData(Uri.parse(url));
        Script.this.startActivity(intent);
    }

    public void openUrlInBrowserWithHeaders(String url, V8Object headers){
        Intent intent = new Intent(Script.this, MainActivity.class);
        intent.setData(Uri.parse(url));
        HashMap<String, String> mHeaders = new HashMap<String, String>();
        for(String key : headers.getKeys()){
            mHeaders.put(key, headers.get(key).toString());
        }
        intent.putExtra("HEADERS", mHeaders);
        Script.this.startActivity(intent);
    }

    public String input(String message){
        txtInput = "";
        hasInput = false;
        Intent i = new Intent("com.sid.ShaheenFalcon.Script");
        i.putExtra("INPUT", message);
        sendBroadcast(i);

        while (!hasInput){
            Log.d("SFSCRIPT", "NO INPUT");
        }
        return txtInput;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(options == null){

        }else{
            for(int i = 0; i < options.size(); i++){
                menu.add(Menu.NONE, i + 1, Menu.NONE, options.get(i));
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        this.choosenOption = item.getItemId() - 1;
        return super.onContextItemSelected(item);
    }

    public class MyReceiver extends BroadcastReceiver {

        private final Handler handler; // Handler used to execute code on the UI thread

        public MyReceiver(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Post the UI updating code to our Handler
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(context, "Toast from broadcast receiver", Toast.LENGTH_SHORT).show();
                    if(intent.hasExtra("OPEN_CONTEXT_MENU")){
                        //btnHidden.performClick();
                        /*registerForContextMenu(tv);

                        openContextMenu(tv);*/
                        LayoutInflater li = LayoutInflater.from(Script.this);
                        View ll = li.inflate(R.layout.list_options, null);
                        final ListView lv = ll.findViewById(R.id.options_list);
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(Script.this, R.layout.script_option_item, Script.this.options);
                        lv.setAdapter(arrayAdapter);
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                choosenOption = i;
                                Log.d("CHOOSEN OPTION", l + "" + i);
                            }
                        });
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(Script.this);
                        dialogBuilder.setView(ll);
                        optionsDialog = dialogBuilder.create();
                        optionsDialog.show();
                    }else if(intent.hasExtra("PRINT")){
                        tv.setText(tv.getText() + "\n" + intent.getStringExtra("PRINT"));
                    }else if(intent.hasExtra("INPUT")){
                        final EditText input = new EditText(Script.this);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        input.setLayoutParams(lp);
                        AlertDialog.Builder inputDialog = new AlertDialog.Builder(Script.this)
                                .setMessage(intent.getStringExtra("INPUT"))
                                .setView(input)
                                .setCancelable(true)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        txtInput = input.getText().toString();
                                        hasInput = true;
                                    }
                                });
                        inputDialog.create().show();
                    }else if(intent.hasExtra("ERROR")){
                        tv.setText(tv.getText() + "\n" + intent.getStringExtra("ERROR"));
                        tv.setTextColor(Color.rgb(255, 0, 0));
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        try {
            unregisterForContextMenu(tv);
        }catch (Exception e) {
            e.printStackTrace();
        }
        try{
            unregisterReceiver(receiver);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        try {
            unregisterForContextMenu(tv);
        }catch (Exception e) {
            e.printStackTrace();
        }
        try{
            unregisterReceiver(receiver);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        registerReceiver(receiver, new IntentFilter("com.sid.ShaheenFalcon.Script"));
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterForContextMenu(tv);
        }catch (Exception e) {
            e.printStackTrace();
        }
        try{
            unregisterReceiver(receiver);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        super.onDestroy();
    }
}
