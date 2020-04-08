package com.sid.shaheenfalcon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.exoplayer2.util.Log;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Intruder extends AppCompatActivity {

    public static String REQ_METHOD = "REQ_METHOD",
    REQ_URL = "REQ_URL",
    REQ_HEADERS = "REQ_HEADERS",
    REQ_BODY = "REQ_BODY",
    RES_CODE = "RES_CODE",
    RES_HEADERS = "RES_HEADERS",
    RES_BODY = "RES_BODY", TYPE_REQ = "TYPE_REQ", TYPE_RES = "TYPE_RES";

    EditText etMethod, etUrl, etHeaders, etBody, progressBar;
    Button btnSend;
    ProgressDialog sendingDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intruder);
        etMethod = findViewById(R.id.intruder_request_method);
        etUrl = findViewById(R.id.intruder_request_url);
        etHeaders = findViewById(R.id.intruder_request_headers);
        etBody = findViewById(R.id.intruder_request_body);
        btnSend = findViewById(R.id.btn_intruder);
        if (getIntent().hasExtra(TYPE_REQ)) {
            registerForContextMenu(btnSend);
        } else if (getIntent().hasExtra(TYPE_RES)) {
            btnSend.setText("View In Browser");
        }
        btnSend.setOnClickListener(new View.OnClickListener() {
            String method, url, body,
                    contentType = "application/x-www-form-urlencoded";
            HashMap<String, String> headers;
            @Override
            public void onClick(View view) {
                method = etMethod.getText().toString().trim();
                url = etUrl.getText().toString();
                headers = new HashMap<String, String>();
                String[] headerLines = etHeaders.getText().toString().trim().split("\\\n");
                body = etBody.getText().toString();

                if (Intruder.this.getIntent().hasExtra(TYPE_REQ)) {
                    sendingDialog = ProgressDialog.show(Intruder.this, "",
                            "Sending...", true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (String headerLine : headerLines) {
                                String[] pair = headerLine.split(":", 2);
                                if (pair.length == 2) {
                                    if (pair[0].trim().equalsIgnoreCase("Content-Type")) {
                                        contentType = pair[1].trim();
                                    }
                                    headers.put(pair[0], pair[1].trim());
                                }
                            }
                            String cookiesStr = CookieManager.getInstance().getCookie(url);
                            if (cookiesStr != null) {
                                headers.put("Cookies", cookiesStr);
                            }
                            OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).build();
                            Request req = null;
                            if(method.equalsIgnoreCase("GET")) {
                                req = new Request.Builder().url(url).headers(Headers.of(headers)).get().build();
                            }else{
                                RequestBody okReqBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), body);
                                req = new Request.Builder().url(url).headers(Headers.of(headers)).method(method, okReqBody).build();
                            }
                            try {
                                Intent intent = new Intent(Intruder.this, Intruder.class);
                                Response res = client.newCall(req).execute();
                                intent.putExtra(RES_CODE, res.code() + res.message());
                                if (res.body().contentType().toString().startsWith("text") || res.body().contentType().toString().toLowerCase().matches("^(application/json|application/xml)")) {
                                    intent.putExtra(RES_BODY, res.body().string());
                                } else {
                                    intent.putExtra(RES_BODY, Base64.encodeToString(res.body().bytes(), Base64.NO_PADDING));
                                }
                                Map<String, List<String>> resHeaders = res.headers().toMultimap();
                                String resHeadersStr = "";
                                for (String headerName : resHeaders.keySet()) {
                                    // need improvement here
                                    resHeadersStr += headerName + ": " + resHeaders.get(headerName).get(0) + "\n";
                                }
                                intent.putExtra(RES_HEADERS, resHeadersStr);
                                intent.putExtra(REQ_URL, url);
                                intent.putExtra(TYPE_RES, true);
                                Intruder.this.startActivity(intent);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else if (Intruder.this.getIntent().hasExtra(TYPE_RES)) {
                    Intent intent = new Intent(Intruder.this, MainActivity.class);
                    intent.setData(Uri.parse(etUrl.getText().toString()));
                    intent.putExtra(RES_HEADERS, headersStrToHashMap(etHeaders.getText().toString(), false));
                    intent.putExtra(RES_BODY, etBody.getText().toString());
                    Intruder.this.startActivity(intent);
                }
            }
        });

        if (getIntent().hasExtra(REQ_METHOD)) {
            etMethod.setText(getIntent().getStringExtra(REQ_METHOD));
        }
        if (getIntent().hasExtra(REQ_URL)) {
            etUrl.setText(getIntent().getStringExtra(REQ_URL));
        }
        if (getIntent().hasExtra(REQ_HEADERS)) {
            etHeaders.setText(getIntent().getStringExtra(REQ_HEADERS));
        }
        if (getIntent().hasExtra(REQ_BODY)) {
            etBody.setText(getIntent().getStringExtra(REQ_BODY));
        }

        if (getIntent().hasExtra(RES_CODE)) {
            etMethod.setHint("Response Code");
            etMethod.setText(getIntent().getStringExtra(RES_CODE));
        }
        if (getIntent().hasExtra(RES_HEADERS)) {
            etHeaders.setHint("Response Headers");
            etHeaders.setText(getIntent().getStringExtra(RES_HEADERS));
        }
        if (getIntent().hasExtra(RES_BODY)) {
            etBody.setHint("Response Body");
            etBody.setText(getIntent().getStringExtra(RES_BODY));
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.btn_intruder) {
            menu.add(Menu.NONE, 1, Menu.NONE, "Default Video Player");
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            HashMap<String, String> headers = headersStrToHashMap(etHeaders.getText().toString().trim());
            String userAgentStr = "";
            if (headers.containsKey("User-Agent")){
                userAgentStr = headers.get("User-Agent");
            }
            Intent intent = new Intent(Intruder.this, PlayerActivity.class);
//                intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA, true);
//                intent.putExtra(PlayerActivity.ABR_ALGORITHM_EXTRA, PlayerActivity.ABR_ALGORITHM_DEFAULT);
            intent.putExtra("PLAYER_USER_AGENT", userAgentStr);
            intent.putExtra("EXTRA_HEADERS", headers);
            intent.setData(Uri.parse(etUrl.getText().toString().trim()));
            startActivity(intent);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sendingDialog != null) {
            sendingDialog.dismiss();
        }
    }

    public static HashMap<String, String> headersStrToHashMap(String headersStr, boolean isCaseSensitive) {
        HashMap<String, String> headersMap = new HashMap<String, String>();
        String[] headerLines = headersStr.split("\\\n");
        for (String headerLine : headerLines) {
            String[] pair = headerLine.split(":", 2);
            if (pair.length == 2) {
                headersMap.put(isCaseSensitive ? pair[0].trim() : pair[0].trim().toLowerCase(), pair[1].trim());
            }
        }
        return headersMap;
    }

    public static HashMap<String, String> headersStrToHashMap(String headersStr) {
        return headersStrToHashMap(headersStr, true);
    }

    public static String headersHashMaptoStr(HashMap<String, String> headersHashMap) {
        String headersStr = "";
        for (String headerName : headersHashMap.keySet()) {
            headersStr += headerName + ": " + headersHashMap.get(headerName) + "\n";
        }
        return headersStr;
    }
}
