package com.sid.shaheenfalcon;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class SFDBController extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "SFDB";
    public static final String SCRIPTS_TABLE_NAME = "SFDB";
    public static final String SCRIPTS_TABLE_ID = "id";
    public static final String SCRIPTS_TABLE_SCRIPT_NAME = "script_name";
    public static final String SCRIPTS_TABLE_SCRIPT_SOURCE = "script_source";
    public static final String SCRIPTS_TABLE_HOST = "host";
    public static final String SCRIPTS_TABLE_MATCH_URL = "match_url";
    public static final String SCRIPTS_TABLE_ENC_LOCATION = "script_location_enc";
    public static final String SCRIPTS_TABLE_PLN_LOCATION = "script_location_pln";
    public static final String SCRIPTS_TABLE_FIRSTRUN = "script_first_run";

    public SFDBController(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + SCRIPTS_TABLE_NAME + " (" +
                "" + SCRIPTS_TABLE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "" + SCRIPTS_TABLE_SCRIPT_NAME + " VARCHAR(100) NOT NULL," +
                "" + SCRIPTS_TABLE_SCRIPT_SOURCE + " VARCHAR(255) NOT NULL," +
                "" + SCRIPTS_TABLE_HOST + " VARCHAR(100) NOT NULL," +
                "" + SCRIPTS_TABLE_MATCH_URL + " VARCHAR(255) NOT NULL," +
                "" + SCRIPTS_TABLE_ENC_LOCATION + " VARCHAR(255) NOT NULL," +
                "" + SCRIPTS_TABLE_PLN_LOCATION + " VARCHAR(255) NOT NULL UNIQUE," +
                "" + SCRIPTS_TABLE_FIRSTRUN + " TEXT" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void insertScript(String scriptName, String source, String host, String matchUrl, String encLocation, String plnLocation, String scriptFirstRun){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SCRIPTS_TABLE_SCRIPT_NAME, scriptName);
        cv.put(SCRIPTS_TABLE_SCRIPT_SOURCE, source);
        cv.put(SCRIPTS_TABLE_HOST, host);
        cv.put(SCRIPTS_TABLE_MATCH_URL, matchUrl);
        cv.put(SCRIPTS_TABLE_ENC_LOCATION, encLocation);
        cv.put(SCRIPTS_TABLE_PLN_LOCATION, plnLocation);
        cv.put(SCRIPTS_TABLE_FIRSTRUN, scriptFirstRun);
        try {
            //db.insert(SCRIPTS_TABLE_NAME, null, cv);
            db.execSQL("INSERT INTO " + SCRIPTS_TABLE_NAME + "("+SCRIPTS_TABLE_SCRIPT_NAME+", "+ SCRIPTS_TABLE_SCRIPT_SOURCE + ", " + SCRIPTS_TABLE_HOST + ", " + SCRIPTS_TABLE_MATCH_URL + ", " + SCRIPTS_TABLE_ENC_LOCATION + ", " + SCRIPTS_TABLE_PLN_LOCATION + ", " + SCRIPTS_TABLE_FIRSTRUN + ")  VALUES (?, ?, ?, ?, ?, ?, ?)", new String[]{scriptName, source, host, matchUrl, encLocation, plnLocation, scriptFirstRun});
        } catch (Exception e) {
            try{
                db.update(SCRIPTS_TABLE_NAME, cv, " "+ SCRIPTS_TABLE_PLN_LOCATION + " = ?", new String[]{plnLocation});
            }catch (SQLException e1){

            }
            //e.printStackTrace();
        }finally {
            db.close();
        }
    }
    public ArrayList<ScriptInfo> getAllScripts(){
        ArrayList<ScriptInfo> scripts = new ArrayList<ScriptInfo>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + SCRIPTS_TABLE_ID + ", " + SCRIPTS_TABLE_SCRIPT_NAME + ", " + SCRIPTS_TABLE_PLN_LOCATION + ", " + SCRIPTS_TABLE_MATCH_URL + ", " + SCRIPTS_TABLE_FIRSTRUN + ", " + SCRIPTS_TABLE_HOST + ", " + SCRIPTS_TABLE_SCRIPT_SOURCE + " FROM " + SCRIPTS_TABLE_NAME + ";", new String[]{});
        while(c.moveToNext()){
            c.getInt(0);
            ScriptInfo si = new ScriptInfo(c.getString(1), c.getString(5), c.getString(3), c.getString(2), c.getString(4), c.getString(6));
            scripts.add(si);
        }
        db.close();
        return scripts;
    }

    public ArrayList<ScriptInfo> getScriptsForHost(String host){
        ArrayList<ScriptInfo> scripts = new ArrayList<ScriptInfo>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + SCRIPTS_TABLE_ID + ", " + SCRIPTS_TABLE_SCRIPT_NAME + ", " + SCRIPTS_TABLE_PLN_LOCATION + ", " + SCRIPTS_TABLE_MATCH_URL + ", " + SCRIPTS_TABLE_FIRSTRUN + " FROM " + SCRIPTS_TABLE_NAME + " WHERE " + SCRIPTS_TABLE_HOST + " LIKE ?", new String[]{host});
        while(c.moveToNext()){
            c.getInt(0);
            ScriptInfo si = new ScriptInfo(c.getString(1), host, c.getString(3), c.getString(2), c.getString(4));
            scripts.add(si);
        }
        db.close();
        return scripts;
    }


}
