package com.sid.shaheenfalcon;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SFScriptExecutorService extends Service {

    public static final int TYPE_EVENT = 1,
    TYPE_EXEC = 2;

    public static final String  SCRIPT_INPUT = "script_input",
                                SCRIPT_OPTIONS = "script_options",
                                SCRIPT_EXEC = "script_exec",
                                SCRIPT_STDOUT = "script_print",
                                SCRIPT_THREAD_INDEX = "script_thread_index";

    protected static ArrayList<ArrayList<SFRequest>> scriptRequests = new ArrayList<ArrayList<SFRequest>>();

    private ArrayList<Thread> sfScriptExecutors;

    private SFScriptExecutorReceiver receiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SF_SCRIPT_EXEC_SERVICE", "STARTing...");
        sfScriptExecutors = new ArrayList<Thread>();
        this.execScript(intent);
        receiver = new SFScriptExecutorService.SFScriptExecutorReceiver(); // Create the receiver
        registerReceiver(receiver, new IntentFilter("com.sid.ShaheenFalcon.SFScriptExecutorService")); // Register receiver
        Log.d("SCRIPT_EXEC_SERVICE", "RECEIVER REGISTERED");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void execScript(Intent intent){
        Log.d("SCRIPT_EXECUTOR_SERVICE", "BROADCAST RECEIVED");
        if(intent.hasExtra("TYPE")) {
            if (intent.getIntExtra("TYPE", 2) == TYPE_EXEC){
                Log.d("SCRIPT_EXECUTOR_SERVICE", "BROADCAST RECEIVED TO EXEC");
//                Bundle b = intent.getBundleExtra("BREQUESTS");
//                ArrayList<SFRequest> requests = (ArrayList<SFRequest>) b.getSerializable("REQUESTS");
                ArrayList<SFRequest> requests = scriptRequests.get(sfScriptExecutors.size());
                String url = intent.getStringExtra("URL"),
                        scriptLocation = intent.getStringExtra("SCRIPT"),
                        userAgentString = "", extraData = "", scriptCode = "";

                if(intent.hasExtra("USER_AGENT")){
                    userAgentString = intent.getStringExtra("USER_AGENT");
                }

                if(intent.hasExtra("EXTRA_DATA")){
                    extraData = intent.getStringExtra("EXTRA_DATA");
                }

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

                Log.d("service", scriptCode.length() + "" + scriptLocation);
                System.out.println(scriptCode);

                StrictMode.ThreadPolicy policy =
                        new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                Thread t = new SFScriptExecutor(SFScriptExecutorService.this, scriptLocation, url, requests, extraData, userAgentString, sfScriptExecutors.size());
                sfScriptExecutors.add(t);
                t.start();
            }
        }
    }


    public class SFScriptExecutorReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if(intent.hasExtra("TYPE")) {
                if (intent.getIntExtra("TYPE", 2) == TYPE_EXEC) {
                    SFScriptExecutorService.this.execScript(intent);
                } else if (intent.hasExtra(SCRIPT_THREAD_INDEX) && intent.getIntExtra("TYPE", 2) == TYPE_EVENT){
                    int threadIdx = intent.getIntExtra(SCRIPT_THREAD_INDEX, -1);
                    SFScriptExecutor sfScriptExecutorThread = (SFScriptExecutor) sfScriptExecutors.get(threadIdx);
                    if (intent.hasExtra(SCRIPT_OPTIONS)){
                        sfScriptExecutorThread.choosenOption = intent.getIntExtra(SCRIPT_OPTIONS, -1);
                    } else if (intent.hasExtra(SCRIPT_INPUT)){
                        sfScriptExecutorThread.txtInput = intent.getStringExtra(SCRIPT_INPUT);
                    }
                }
            }
        }
    }
}
