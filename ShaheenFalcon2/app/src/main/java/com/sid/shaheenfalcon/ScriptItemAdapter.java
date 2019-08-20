package com.sid.shaheenfalcon;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ScriptItemAdapter extends BaseAdapter {

    Context context = null;
    ArrayList<ScriptInfo> scripts = null;
    LayoutInflater inflater;
    String initVector = "ShaheenFalconAPP";


    public ScriptItemAdapter(Context context, ArrayList<ScriptInfo> scripts) {
        this.context = context;
        this.scripts = scripts;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return scripts.size();
    }

    @Override
    public Object getItem(int i) {
        return scripts.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.list_scripts_item, null);
        TextView tv = (TextView) view.findViewById(R.id.script_item_name);
        tv.setText(this.scripts.get(i).getScriptName());
        EditText etFirstRun = (EditText) view.findViewById(R.id.script_item_code_firstrun);
        EditText etLocation = (EditText) view.findViewById(R.id.script_item_code_location);
        Button btnScriptRun = (Button) view.findViewById(R.id.script_repo_btn_run);
        Button btnScriptUpdate = (Button) view.findViewById(R.id.script_repo_btn_update);
        btnScriptRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scripts.get(i).getScriptLocation();
                Intent intent = new Intent(context, Script.class);
                intent.putExtra("URL", "");
                intent.putExtra("SCRIPT", scripts.get(i).getScriptLocation());
                intent.putExtra("USER_AGENT", "");
                Bundle b = new Bundle();
                b.putSerializable("REQUESTS", (Serializable) new ArrayList<SFRequest>());
                intent.putExtra("BREQUESTS", b);
                context.startActivity(intent);
            }
        });

        final int script_index = i;

        btnScriptUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText scriptPassword = new EditText(context);
                scriptPassword.setHint("Enter password: ");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                scriptPassword.setLayoutParams(lp);
                AlertDialog.Builder passInputBulder = new AlertDialog.Builder(context)
                        .setCancelable(true)
                        .setView(scriptPassword)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Thread t = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            OkHttpClient client = new OkHttpClient();
                                            client.setProtocols(Arrays.asList(Protocol.HTTP_1_1));
                                            Request request = new Request.Builder().get().url(scripts.get(script_index).getScriptSource()).build();
                                            Response response = client.newCall(request).execute();
                                            JSONObject scriptObject = new JSONObject(response.body().string());
                                            if(scriptObject.has("type_code")){
                                                if(scriptObject.getInt("type_code") == 1){
                                                    //Index page
                                                    JSONArray scriptsArr = scriptObject.getJSONArray("scripts");
                                                    for(int i = 0; i < scriptsArr.length(); i++){
                                                        String scriptSourceUrl = scriptsArr.getString(i);
                                                        Request request2 = new Request.Builder().url(scriptSourceUrl).get().build();
                                                        Response response2 = client.newCall(request2).execute();
                                                        JSONObject scriptObject2 = new JSONObject(response2.body().string());
                                                        if(scriptObject2.has("type_code")){
                                                            if(scriptObject2.getInt("type_code") == 0){
                                                                //installScript(scriptSourceUrl, scriptObject2, scriptPassword.getText().toString());
                                                            }
                                                        }
                                                    }
                                                }else if(scriptObject.getInt("type_code") == 0){
                                                    //Script
                                                    installScript(scripts.get(script_index).getScriptSource(), scriptObject, scriptPassword.getText().toString());
                                                }
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                t.start();
                            }
                        });
                passInputBulder.create().show();
            }
        });
        etLocation.setText(scripts.get(i).getScriptLocation());
        etFirstRun.setText(scripts.get(i).getFirstRun());

        return view;
    }

    private void installScript(String scriptSource, JSONObject script, String key){
        try {
            String scriptName = script.getString("script_name");
            String matchUrl = script.getString("match_url");
            String host = script.getString("match_host");
            String firstRunScript = script.getString("first_run");
            String scriptCode = decryptScriptContent(script.getString("content"), key);
            File scriptStorage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ShaheenFalcon/sfScripts/");
            File plainScriptStorage  = new File(context.getFilesDir().getAbsolutePath() + "/plainScriptStorage/");
            if(!scriptStorage.exists()){
                scriptStorage.mkdirs();
            }
            if(!plainScriptStorage.exists()){
                plainScriptStorage.mkdirs();
            }

            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(scriptSource.getBytes());
                File plainScript = new File(plainScriptStorage.getAbsolutePath() + "/" + bytesToHex(md.digest()) + ".js");
                File encScript = new File(scriptStorage.getAbsolutePath() + "/" + scriptName.trim() + ".js");
                FileOutputStream fos = new FileOutputStream(plainScript);
                fos.write(scriptCode.getBytes());
                fos.flush();
                fos.close();

                SFDBController sfdbController = new SFDBController(context);
                sfdbController.insertScript(scriptName, scriptSource, host, matchUrl, encScript.getAbsolutePath(), plainScript.getAbsolutePath(), decryptScriptContent(firstRunScript, key));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String decryptScriptContent(String encryptedContent, String key){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes(Charset.forName("UTF-8")));
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(Charset.forName("UTF-8")));
            SecretKeySpec skeySpec = new SecretKeySpec(md.digest(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] original = cipher.doFinal(Base64.decode(encryptedContent, Base64.NO_WRAP));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "";
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
