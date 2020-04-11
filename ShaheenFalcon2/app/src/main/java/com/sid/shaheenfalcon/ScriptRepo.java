package com.sid.shaheenfalcon;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ScriptRepo extends AppCompatActivity {

    AlertDialog alerDialog = null;
    String initVector = "ShaheenFalconAPP";
    private static int FILE_CHOOSER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_repo);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton btnRepoAdd  = (ImageButton) findViewById(R.id.script_repo_btn_add),
                btnAddLocalScript = (ImageButton) findViewById(R.id.script_repo_btn_add_local_script);

        ListView lv = (ListView) findViewById(R.id.script_repo_script_list);

        ScriptItemAdapter scriptItemAdapter = new ScriptItemAdapter(this, (new SFDBController(this)).getAllScripts());

        lv.setAdapter(scriptItemAdapter);

        LayoutInflater li = LayoutInflater.from(this);
        View ll = li.inflate(R.layout.dialog_add_script, null);
        final EditText scriptSource = ll.findViewById(R.id.script_source);
        final EditText scriptPassword = ll.findViewById(R.id.script_password);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(ll);
        dialogBuilder.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            OkHttpClient client = new OkHttpClient.Builder().protocols(Arrays.asList(Protocol.HTTP_1_1)).build();
                            Request request = new Request.Builder().get().url(scriptSource.getText().toString()).build();
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
                                                installScript(scriptSourceUrl, scriptObject2, scriptPassword.getText().toString());
                                            }
                                        }
                                    }
                                }else if(scriptObject.getInt("type_code") == 0){
                                    //Script
                                    installScript(scriptSource.getText().toString(), scriptObject, scriptPassword.getText().toString());
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
                Toast.makeText(getApplicationContext(), "STARTED", Toast.LENGTH_LONG).show();
            }
        });
        dialogBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        alerDialog = dialogBuilder.create();

        btnRepoAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alerDialog.show();
            }
        });

        btnAddLocalScript.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browseIntent = new Intent(Intent.ACTION_GET_CONTENT);
                browseIntent.setType("*/*");
                startActivityForResult(Intent.createChooser(browseIntent, "Choose Script file"), FILE_CHOOSER);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && resultCode == RESULT_OK && data != null) {
            Uri scriptFile = data.getData();
            AlertDialog.Builder dlgLocalScriptBuilder = new AlertDialog.Builder(ScriptRepo.this);
            LayoutInflater inflater = LayoutInflater.from(ScriptRepo.this);
            View dlgLocalScript = inflater.inflate(R.layout.dialog_add_local_script, null);
            final EditText etScriptName = dlgLocalScript.findViewById(R.id.dlg_local_script_name),
                    etFirstRun = dlgLocalScript.findViewById(R.id.dlg_local_script_first_run),
                    etMatchHost = dlgLocalScript.findViewById(R.id.dlg_local_script_match_host),
                    etScriptFile = dlgLocalScript.findViewById(R.id.dlg_local_script_file_addr);
            etScriptFile.setText(scriptFile.toString());
            dlgLocalScriptBuilder.setView(dlgLocalScript);
            dlgLocalScriptBuilder.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    StringBuilder sb = new StringBuilder();

                    if(!scriptFile.getPath().toString().equals("")){
                        try {
                            BufferedReader br = new BufferedReader(new InputStreamReader(getApplicationContext().getContentResolver().openInputStream(scriptFile)));
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
                    }
                    JSONObject scriptObj = new JSONObject();
                    try {
                        scriptObj.put("content", sb.toString());
                        scriptObj.put("script_name", etScriptName.getText().toString());
                        scriptObj.put("first_run", etFirstRun.getText().toString());
                        scriptObj.put("match_host", etMatchHost.getText().toString());
                        scriptObj.put("match_url", "^" + etMatchHost.getText().toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    installScript(scriptFile.toString(), scriptObj, null);
                }
            });

            dlgLocalScriptBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });

            dlgLocalScriptBuilder.create().show();
        }
    }

    private void installScript(String scriptSource, JSONObject script, String key){
        try {
            String scriptName = script.getString("script_name");
            String matchUrl = script.getString("match_url");
            String host = script.getString("match_host");
            String firstRunScript = script.getString("first_run");
            String scriptCode = key == null ? script.getString("content") : decryptScriptContent(script.getString("content"), key);
            File scriptStorage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ShaheenFalcon/sfScripts/");
            File plainScriptStorage  = new File(getFilesDir().getAbsolutePath() + "/plainScriptStorage/");
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

                SFDBController sfdbController = new SFDBController(this);
                sfdbController.insertScript(scriptName, scriptSource, host, matchUrl, encScript.getAbsolutePath(), plainScript.getAbsolutePath(), key == null ? script.getString("content") : decryptScriptContent(firstRunScript, key));
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

    private void updateScript(String scriptSource, JSONObject script, String key){

    }

    private void refreshScripts(){

    }

    private String encryptScriptContent(String content, String key){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes(Charset.forName("UTF-8")));
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(Charset.forName("UTF-8")));
            SecretKeySpec skeySpec = new SecretKeySpec(md.digest(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(content.getBytes());
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
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
