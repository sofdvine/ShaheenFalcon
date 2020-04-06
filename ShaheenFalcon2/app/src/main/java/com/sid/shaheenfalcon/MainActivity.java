package com.sid.shaheenfalcon;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.exoplayer2.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.os.Environment;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import static com.sid.shaheenfalcon.Intruder.REQ_BODY;
import static com.sid.shaheenfalcon.Intruder.REQ_HEADERS;
import static com.sid.shaheenfalcon.Intruder.RES_BODY;
import static com.sid.shaheenfalcon.Intruder.RES_HEADERS;
import static com.sid.shaheenfalcon.SFScriptExecutorService.SCRIPT_INPUT;
import static com.sid.shaheenfalcon.SFScriptExecutorService.SCRIPT_OPTIONS;
import static com.sid.shaheenfalcon.SFScriptExecutorService.SCRIPT_THREAD_INDEX;
import static com.sid.shaheenfalcon.SFScriptExecutorService.TYPE_EVENT;

public class MainActivity extends AppCompatActivity {

    EditText url_box = null;
    WebView wv = null;
    Button scriptRun = null;
    ProgressBar progressBar = null;
    ArrayList<SFRequest> requests = null;
    ArrayList<ScriptInfo> scripts = null;
    private static int DOWNLOAD_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        getSupportActionBar().setDisplayShowTitleEnabled(false);

        url_box = (EditText) findViewById(R.id.url);
        wv = (WebView) findViewById(R.id.sf_webview);
        scriptRun = (Button) findViewById(R.id.script_run);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //Initiating Arraylists
        requests = new ArrayList<SFRequest>();

        BroadcastReceiver receiver = new ScriptUIReceiver(new Handler());
        registerReceiver(receiver, new IntentFilter("com.sid.ShaheenFalcon.ScriptUIReceiver"));

        //Starting required services
        if(!isMyServiceRunning(DownloaderService.class)){
            Intent intent = new Intent(MainActivity.this, DownloaderService.class);
            startService(intent);
        }

        //Setting up webview


        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setAllowContentAccess(true);
        wv.getSettings().setAllowUniversalAccessFromFileURLs(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setAllowFileAccessFromFileURLs(true);
        wv.getSettings().setAllowFileAccess(true);

        wv.getSettings().setAppCacheEnabled(false);
        wv.getSettings().setCacheMode(wv.getSettings().LOAD_NO_CACHE);
        wv.clearFormData();
        wv.clearHistory();
        wv.clearCache(true);
        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();

        wv.setWebViewClient(new WebViewClient(){

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                requests.clear();
                progressBar.setVisibility(View.VISIBLE);
                url_box.setText(url);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if(request.getUrl().toString().startsWith("magnet:")){
                    Intent i = new Intent(MainActivity.this, TorrentStreamActivity.class);
                    i.setData(request.getUrl());
                    startActivity(i);
                }else {
                    requests.add(new SFRequest(request.getMethod(), request.getUrl().toString(), request.getRequestHeaders()));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }
        });

        wv.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String browserUserAgent, String contentDisposition, String mimeType, long l) {
                String fileName = url.split("\\?")[0];
                String[] listPath = fileName.split("/");
                fileName = listPath[listPath.length - 1];
                if(contentDisposition != null){
                    if(!contentDisposition.trim().equals("")){
                        fileName = contentDisposition;
                    }
                }
                if(mimeType.startsWith("video/") || fileName.endsWith(".mkv")) {
                    String finalFileName1 = fileName;
                    AlertDialog.Builder downladDialogBuilder = new AlertDialog.Builder(MainActivity.this)
                            .setCancelable(true)
                            .setItems(new CharSequence[]{"Play Video", "Download"}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (i == 0) {
                                        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                                        intent.setData(Uri.parse(url));
                                        intent.putExtra("PLAYER_USER_AGENT", browserUserAgent);
                                        startActivity(intent);
                                    } else if(isStoragePermissionGranted()){
                                        if(!isMyServiceRunning(DownloaderService.class)){
                                            Intent intent = new Intent(MainActivity.this, DownloaderService.class);
                                            startService(intent);
                                        }
                                        HashMap<String, String> headers = new HashMap<String, String>();
                                        headers.put("User-Agent", browserUserAgent);
                                        if(CookieManager.getInstance().getCookie(url) != null) {
                                            headers.put("Cookies", CookieManager.getInstance().getCookie(url));
                                        }
                                        Intent intent = new Intent("com.sid.ShaheenFalcon.DownloaderService");
                                        intent.putExtra("ADD_DOWNLOAD", true);
                                        intent.putExtra("DOWNLOAD_URL", url);
                                        intent.putExtra("DOWNLOAD_FILE", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + finalFileName1);
                                        intent.putExtra("DOWNLOAD_HEADERS", headers);
                                        sendBroadcast(intent);
                                    }else if(!isStoragePermissionGranted()){
                                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, DOWNLOAD_REQUEST_CODE);
                                    }
                                }
                            });
                    downladDialogBuilder.create().show();
                }else{
                    if (isStoragePermissionGranted()) {
                        if(!isMyServiceRunning(DownloaderService.class)){
                            Intent intent = new Intent(MainActivity.this, DownloaderService.class);
                            startService(intent);
                        }
                        HashMap<String, String> headers = new HashMap<String, String>();
                        headers.put("User-Agent", browserUserAgent);
                        if(CookieManager.getInstance().getCookie(url) != null) {
                            headers.put("Cookies", CookieManager.getInstance().getCookie(url));
                        }
                        Intent intent = new Intent("com.sid.ShaheenFalcon.DownloaderService");
                        intent.putExtra("ADD_DOWNLOAD", true);
                        intent.putExtra("DOWNLOAD_URL", url);
                        intent.putExtra("DOWNLOAD_FILE", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + fileName);
                        intent.putExtra("DOWNLOAD_HEADERS", headers);
                        sendBroadcast(intent);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, DOWNLOAD_REQUEST_CODE);
                    }
                }
            }
        });

        wv.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }
        });


        //wv.addJavascriptInterface(new SFScriptInterface(this), "SFINTERFACE");
        wv.addJavascriptInterface(new SFScriptInterface(MainActivity.this, wv.getUrl(), wv.getSettings().getUserAgentString(), requests), "SFINTERFACE");

//        Intent serviceIntent = new Intent(MainActivity.this, SFScriptExecutorService.class);
//        startService(serviceIntent);

        if(getIntent() != null && getIntent().getData() != null){
            if (getIntent().hasExtra(RES_BODY)) {
                String contentType = "text/html", contentEncoding = null;
                if (getIntent().hasExtra(RES_HEADERS)) {
                    HashMap<String, String> resHeaders = (HashMap<String, String>) getIntent().getSerializableExtra(RES_HEADERS);
                    if (resHeaders.containsKey("Content-Type")) {
                        contentType = resHeaders.get("Content-Type");
                    }
                    if (resHeaders.containsKey("Content-Encoding")) {
                        contentType = resHeaders.get("Content-Encoding");
                    }
                }
                wv.loadDataWithBaseURL(getIntent().getData().toString(), getIntent().getStringExtra(RES_BODY), contentType, contentEncoding, null);
            } else if (getIntent().hasExtra("HEADERS")){
                wv.loadUrl(getIntent().getData().toString(), (HashMap<String, String>) getIntent().getSerializableExtra("HEADERS"));
            } else {
                wv.loadUrl(getIntent().getData().toString());
            }
        }

        url_box.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(url_box.getText().toString().trim().startsWith("magnet:")) {
                    Intent intent = new Intent(MainActivity.this, TorrentStreamActivity.class);
                    intent.setData(Uri.parse(url_box.getText().toString()));
                    startActivity(intent);
                }else {
                    try {
                        URI uri = new URL(url_box.getText().toString()).toURI();
                        wv.loadUrl(uri.toURL().toString());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(url_box.getText().toString()));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        try {

                            String mayHost = url_box.getText().toString().trim().split("\\?")[0].split(":")[0];
                            if(!mayHost.contains(" ") && mayHost.split("\\.").length > 1) {
                                URI uri = new URL("http://" + url_box.getText().toString()).toURI();
                                wv.loadUrl(uri.toURL().toString());
                            }else{
                                wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(url_box.getText().toString()));
                            }
                        } catch (URISyntaxException e2) {
                            wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(url_box.getText().toString()));
                        } catch (MalformedURLException e2) {
                            wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(url_box.getText().toString()));
                        }
                    }
                }
                return false;
            }
        });

        registerForContextMenu(wv);

        url_box.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    scriptRun.setText("Go");
                }else{
                    scriptRun.setText("Run");
                }
            }
        });

        scriptRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(url_box.hasFocus()){
                    if(url_box.getText().toString().trim().startsWith("magnet:")) {
                        Intent intent = new Intent(MainActivity.this, TorrentStreamActivity.class);
                        intent.setData(Uri.parse(url_box.getText().toString()));
                        startActivity(intent);
                    }else {
                        try {
                            URI uri = new URL(url_box.getText().toString()).toURI();
                            wv.loadUrl(uri.toURL().toString());
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                            wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(url_box.getText().toString()));
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            try {

                                String mayHost = url_box.getText().toString().trim().split("\\?")[0].split(":")[0];
                                if(!mayHost.contains(" ") && mayHost.split("\\.").length > 1) {
                                    URI uri = new URL("http://" + url_box.getText().toString()).toURI();
                                    wv.loadUrl(uri.toURL().toString());
                                }else{
                                    wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(url_box.getText().toString()));
                                }
                            } catch (URISyntaxException e2) {
                                wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(url_box.getText().toString()));
                            } catch (MalformedURLException e2) {
                                wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(url_box.getText().toString()));
                            }
                        }
                    }
                }else {
                    SFDBController sfdbc = new SFDBController(MainActivity.this);
                    if (scripts != null) {
                        scripts.clear();
                    }
                    scripts = new ArrayList<ScriptInfo>();
                    try {
                        Toast.makeText(getApplicationContext(), (new URL(wv.getUrl())).getHost(), Toast.LENGTH_LONG).show();
                        scripts = sfdbc.getScriptsForHost((new URL(wv.getUrl())).getHost());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    if (scripts.size() == 0) {
                        //Toast.makeText(getApplicationContext(), "DIRECT", Toast.LENGTH_LONG).show();
//                        Intent i = new Intent(MainActivity.this, Script.class);
//                        i.putExtra("URL", wv.getUrl());
//                        i.putExtra("SCRIPT", "");
//                        i.putExtra("USER_AGENT", wv.getSettings().getUserAgentString());
//                        Bundle b = new Bundle();
//                        b.putSerializable("REQUESTS", (Serializable) requests);
//                        i.putExtra("BREQUESTS", b);
//                        startActivity(i);
                        Toast.makeText(getApplicationContext(), "No script found", Toast.LENGTH_LONG).show();
                    } else {
                        registerForContextMenu(scriptRun);
                        openContextMenu(scriptRun);
                    }
                }
            }
        });

        //requestWindowFeature(Window.FEATURE_NO_TITLE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_scripts) {
            Intent i = new Intent(MainActivity.this, ScriptRepo.class);
            startActivity(i);
            return true;
        }else if(id == R.id.action_downloads){
            Intent i = new Intent(MainActivity.this, com.sid.shaheenfalcon.DownloadManager.class);
            startActivity(i);
        }else if(id == R.id.action_torrent){
            Intent i = new Intent(MainActivity.this, TorrentStreamActivity.class);
            startActivity(i);
            return true;
        }else if (id == R.id.action_settings) {
            return true;
        }else if(id == R.id.action_timer_pause){
            item.setChecked(!item.isChecked());
            if (item.isChecked()) {
                wv.pauseTimers();
            } else {
                wv.resumeTimers();
            }
        } else if (id == R.id.action_view_source) {
            wv.loadUrl("view-source:" + wv.getUrl());
        } else if (id == R.id.action_page_contents) {
            Intent intent = new Intent(MainActivity.this, RequestListActivity.class);
            intent.putExtra("USER_AGENT", wv.getSettings().getUserAgentString());
            RequestListActivity.requests = (ArrayList<SFRequest>) requests.clone();
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if(v.getId() == R.id.sf_webview){
            WebView.HitTestResult result = wv.getHitTestResult();
            if(result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE){
                Intent i = new Intent();
                i.putExtra("A_URL", result.getExtra());
                MenuItem item1 = menu.add(Menu.NONE, 1, Menu.NONE, "Open URL");
                //item1.setIntent(i);
                MenuItem item2 = menu.add(Menu.NONE, 2, Menu.NONE, "Copy URL");
                //item2.setIntent(i);
                if(result.getExtra().split("\\?")[0].endsWith(".mp4") || result.getExtra().split("\\?")[0].endsWith(".m3u8")) {
                    MenuItem item3 = menu.add(Menu.NONE, 6, Menu.NONE, "Play Video");
                    //item3.setIntent(i);
                }
            }else if(result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE){
                Intent i = new Intent();
                i.putExtra("A_URL", result.getExtra());
                MenuItem item3 = menu.add(Menu.NONE, 3, Menu.NONE, "Open URL");
                MenuItem item4 = menu.add(Menu.NONE, 4, Menu.NONE, "Copy URL");
                //item3.setIntent(i);
                //item4.setIntent(i);
            }else if(result.getType() == WebView.HitTestResult.IMAGE_TYPE){
                Intent i = new Intent();
                i.putExtra("IMG_URL", result.getExtra());
                MenuItem item5 = menu.add(Menu.NONE, 5, Menu.NONE, "Open Image");
                MenuItem item7 = menu.add(Menu.NONE, 7, Menu.NONE, "Download Image");
                //item5.setIntent(i);
                //item7.setIntent(i);
            }
        }else if(v.getId() == R.id.script_run) {
            if (scripts == null) {

            } else {
                for (int i = 0; i < scripts.size(); i++) {
                    menu.add(Menu.NONE, i + 1, Menu.NONE, scripts.get(i).getScriptName());
                }
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if(item.getIntent() != null){
            if(item.getIntent().hasExtra("A_URL")) {
                if(item.getItemId() == 2 || item.getItemId() == 4) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied URL", item.getIntent().getStringExtra("A_URL"));
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getApplicationContext(), "Copied", Toast.LENGTH_LONG).show();
                }else if(item.getItemId() == 6){
                    Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                    intent.setData(Uri.parse(item.getIntent().getStringExtra("A_URL")));
                    intent.putExtra("PLAYER_USER_AGENT", wv.getSettings().getUserAgentString());
                    startActivity(intent);
                }else if(item.getItemId() == 7){
                    String url = item.getIntent().getStringExtra("IMG_URL");
                    String[] splittedUrl = url.split("\\?")[0].split("/");
                    String fileName = splittedUrl[splittedUrl.length - 1];
                    if (isStoragePermissionGranted()) {
                        if(!isMyServiceRunning(DownloaderService.class)){
                            Intent intent = new Intent(MainActivity.this, DownloaderService.class);
                            startService(intent);
                        }
                        HashMap<String, String> headers = new HashMap<String, String>();
                        headers.put("User-Agent", wv.getSettings().getUserAgentString());
                        if(CookieManager.getInstance().getCookie(url) != null) {
                            headers.put("Cookies", CookieManager.getInstance().getCookie(url));
                        }
                        Intent intent = new Intent("com.sid.ShaheenFalcon.DownloaderService");
                        intent.putExtra("ADD_DOWNLOAD", true);
                        intent.putExtra("DOWNLOAD_URL", url);
                        intent.putExtra("DOWNLOAD_FILE", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + fileName);
                        intent.putExtra("DOWNLOAD_HEADERS", headers);
                        sendBroadcast(intent);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, DOWNLOAD_REQUEST_CODE);
                    }
                }else if(item.getItemId() == 3 || item.getItemId() == 1){
                    wv.loadUrl(item.getIntent().getStringExtra("A_URL"));
                }
            }else {
                if(item.getItemId() == 7){
                    String url = item.getIntent().getStringExtra("IMG_URL");
                    String[] splittedUrl = url.split("\\?")[0].split("/");
                    String fileName = splittedUrl[splittedUrl.length - 1];
                    if (isStoragePermissionGranted()) {
                        if(!isMyServiceRunning(DownloaderService.class)){
                            Intent intent = new Intent(MainActivity.this, DownloaderService.class);
                            startService(intent);
                        }
                        HashMap<String, String> headers = new HashMap<String, String>();
                        headers.put("User-Agent", wv.getSettings().getUserAgentString());
                        if(CookieManager.getInstance().getCookie(url) != null) {
                            headers.put("Cookies", CookieManager.getInstance().getCookie(url));
                        }
                        Intent intent = new Intent("com.sid.ShaheenFalcon.DownloaderService");
                        intent.putExtra("ADD_DOWNLOAD", true);
                        intent.putExtra("DOWNLOAD_URL", url);
                        intent.putExtra("DOWNLOAD_FILE", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + fileName);
                        intent.putExtra("DOWNLOAD_HEADERS", headers);
                        sendBroadcast(intent);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, DOWNLOAD_REQUEST_CODE);
                    }
                }else{

                    wv.loadUrl(item.getIntent().getStringExtra("IMG_URL"));
                }
            }
        } else {
            int i = item.getItemId() - 1;
            if (scripts.get(i).getFirstRun().trim().equals("")) {
//                Intent intent = new Intent(MainActivity.this, Script.class);
//                intent.putExtra("URL", wv.getUrl());
//                intent.putExtra("SCRIPT", scripts.get(i).getScriptLocation());
//                intent.putExtra("USER_AGENT", wv.getSettings().getUserAgentString());
//                Bundle b = new Bundle();
//                b.putSerializable("REQUESTS", (Serializable) requests);
//                intent.putExtra("BREQUESTS", b);
//                startActivity(intent);
                if(!isMyServiceRunning(SFScriptExecutorService.class)){
                    Intent serviceIntent = new Intent(MainActivity.this, SFScriptExecutorService.class);
                    serviceIntent.putExtra("URL", wv.getUrl());
                    serviceIntent.putExtra("SCRIPT", scripts.get(i).getScriptLocation());
                    serviceIntent.putExtra("USER_AGENT", wv.getSettings().getUserAgentString());
                    serviceIntent.putExtra("TYPE", SFScriptExecutorService.TYPE_EXEC);
                    SFScriptExecutorService.scriptRequests.add((ArrayList<SFRequest>) requests.clone());
                    MainActivity.this.startService(serviceIntent);
                    Log.d("MAIN", "SCRIPT EXECUTOR SERVICE STARTED");
                }else{
                    Intent intent = new Intent("com.sid.ShaheenFalcon.SFScriptExecutorService");
                    intent.putExtra("URL", wv.getUrl());
                    intent.putExtra("SCRIPT", scripts.get(i).getScriptLocation());
                    intent.putExtra("USER_AGENT", wv.getSettings().getUserAgentString());
                    intent.putExtra("TYPE", SFScriptExecutorService.TYPE_EXEC);
                    SFScriptExecutorService.scriptRequests.add((ArrayList<SFRequest>) requests.clone());
                    sendBroadcast(intent);
                    Log.d("MAIN", "SCRIPT EXECUTOR SERVICE RUNNING");
                }
                Log.d("MAIN", "broadcast sent");
            } else {
                wv.loadUrl("javascript:" + scripts.get(i).getFirstRun().replace("runSFScript(", "SFINTERFACE.parseData(\"" + scripts.get(i).getScriptLocation() + "\", "));
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length > 0 && requestCode == DOWNLOAD_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "YOU CAN INITIATE DOWNLOAD NOW", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(getApplicationContext(), "Shaheen Falcon needs to access your storage to download files.", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if(wv.canGoBack()){
            wv.goBack();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else {
            return true;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startDownload(String url, String browserUserAgent, String fileName, String mimeType){
        if (isStoragePermissionGranted()) {
            if(!isMyServiceRunning(DownloaderService.class)){
                Intent intent = new Intent(MainActivity.this, DownloaderService.class);
                startService(intent);
            }
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", browserUserAgent);
            if(CookieManager.getInstance().getCookie(url) != null) {
                headers.put("Cookies", CookieManager.getInstance().getCookie(url));
            }
            Intent intent = new Intent("com.sid.ShaheenFalcon.DownloaderService");
            intent.putExtra("ADD_DOWNLOAD", true);
            intent.putExtra("DOWNLOAD_URL", url);
            intent.putExtra("DOWNLOAD_FILE", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + fileName);
            intent.putExtra("DOWNLOAD_HEADERS", headers);
            sendBroadcast(intent);
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, DOWNLOAD_REQUEST_CODE);
        }
    }

    public class ScriptUIReceiver extends BroadcastReceiver {
        private Handler handler;
        private android.app.AlertDialog optionsDialog = null;
        private ArrayList<String> options = null;
        ScriptUIReceiver(Handler handler){
            this.handler = handler;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            final int threadIdx = intent.getIntExtra(SFScriptExecutorService.SCRIPT_THREAD_INDEX, -1);
            // Post the UI updating code to our Handler
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(context, "Toast from broadcast receiver", Toast.LENGTH_SHORT).show();
                    if(intent.hasExtra(SCRIPT_OPTIONS)){
                        //btnHidden.performClick();
                        /*registerForContextMenu(tv);

                        openContextMenu(tv);*/
                        options = intent.getStringArrayListExtra(SCRIPT_OPTIONS);
                        Log.d("SCRIPT_UI_RECEIVER", "GOT " + options.size());
                        LayoutInflater li = LayoutInflater.from(MainActivity.this);
                        View ll = li.inflate(R.layout.list_options, null);
                        final ListView lv = ll.findViewById(R.id.options_list);
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.script_option_item, options);
                        lv.setAdapter(arrayAdapter);
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                                 i;
//                                android.util.Log.d("CHOOSEN OPTION", l + "" + i);
                                Intent scriptExecutorServiceIntent = new Intent("com.sid.ShaheenFalcon.SFScriptExecutorService");
                                scriptExecutorServiceIntent.putExtra("TYPE", TYPE_EVENT);
                                scriptExecutorServiceIntent.putExtra(SCRIPT_THREAD_INDEX, threadIdx);
                                scriptExecutorServiceIntent.putExtra(SCRIPT_OPTIONS, i);
                                sendBroadcast(scriptExecutorServiceIntent);
                                optionsDialog.dismiss();
                            }
                        });
                        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(MainActivity.this);
                        dialogBuilder.setView(ll);
                        optionsDialog = dialogBuilder.create();
                        optionsDialog.show();
                    }else if(intent.hasExtra(SFScriptExecutorService.SCRIPT_STDOUT)){
                        Toast.makeText(MainActivity.this.getApplicationContext(), intent.getStringExtra(SFScriptExecutorService.SCRIPT_STDOUT), Toast.LENGTH_LONG).show();
                    }else if(intent.hasExtra(SFScriptExecutorService.SCRIPT_INPUT)){
                        final EditText input = new EditText(MainActivity.this);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        input.setLayoutParams(lp);
                        android.app.AlertDialog.Builder inputDialog = new android.app.AlertDialog.Builder(MainActivity.this)
                                .setMessage(intent.getStringExtra("INPUT"))
                                .setView(input)
                                .setCancelable(true)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String txtInput = input.getText().toString();
                                        Intent scriptExecutorServiceIntent = new Intent("com.sid.ShaheenFalcon.SFScriptExecutorService");
                                        scriptExecutorServiceIntent.putExtra("TYPE", TYPE_EVENT);
                                        scriptExecutorServiceIntent.putExtra(SCRIPT_THREAD_INDEX, threadIdx);
                                        scriptExecutorServiceIntent.putExtra(SCRIPT_INPUT, txtInput);
                                        sendBroadcast(scriptExecutorServiceIntent);
                                    }
                                });
                        inputDialog.create().show();
                    }
                }
            });
        }
    }
}
