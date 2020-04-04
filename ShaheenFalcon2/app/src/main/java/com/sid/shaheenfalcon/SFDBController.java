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

    public static final String AUDIO_TABLE_NAME = "tbl_sf_audio";
    public static final String AUDIO_ID = "sf_audio_id";
    public static final String AUDIO_URL = "audio_url";
    public static final String AUDIO_TITLE = "audio_title";
    public static final String AUDIO_SUBTITLE = "audio_subtitle";
    public static final String AUDIO_THUMB = "audio_thumb";
    public static final String AUDIO_CALLBACK = "audio_exec";

    public static class AudioMetadata {
        private String audioTitle, audioUrl, audioSubtitle, audioExec, audioThumb;

        AudioMetadata(String audioUrl, String audioTitle, String audioSubtitle, String audioExec, String audioThumb){
            this.audioSubtitle = audioSubtitle;
            this.audioUrl = audioUrl;
            this.audioTitle = audioTitle;
            this.audioExec = audioExec;
            this.audioThumb = audioThumb;
        }

        public String getAudioTitle() {
            return audioTitle;
        }

        public void setAudioTitle(String audioTitle) {
            this.audioTitle = audioTitle;
        }

        public String getAudioUrl() {
            return audioUrl;
        }

        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }

        public String getAudioSubtitle() {
            return audioSubtitle;
        }

        public void setAudioSubtitle(String audioSubtitle) {
            this.audioSubtitle = audioSubtitle;
        }

        public String getAudioExec() {
            return audioExec;
        }

        public void setAudioExec(String audioExec) {
            this.audioExec = audioExec;
        }

        public String getAudioThumb() {
            return audioThumb;
        }

        public void setAudioThumb(String audioThumb) {
            this.audioThumb = audioThumb;
        }
    }


    public SFDBController(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + SCRIPTS_TABLE_NAME + " (" +
                "" + SCRIPTS_TABLE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "" + SCRIPTS_TABLE_SCRIPT_NAME + " VARCHAR(100) NOT NULL," +
                "" + SCRIPTS_TABLE_SCRIPT_SOURCE + " VARCHAR(2255) NOT NULL," +
                "" + SCRIPTS_TABLE_HOST + " VARCHAR(100) NOT NULL," +
                "" + SCRIPTS_TABLE_MATCH_URL + " VARCHAR(255) NOT NULL," +
                "" + SCRIPTS_TABLE_ENC_LOCATION + " VARCHAR(255) NOT NULL," +
                "" + SCRIPTS_TABLE_PLN_LOCATION + " VARCHAR(255) NOT NULL UNIQUE," +
                "" + SCRIPTS_TABLE_FIRSTRUN + " TEXT" +
                ")");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + AUDIO_TABLE_NAME + " (" +
                "" + AUDIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "" + AUDIO_TITLE + " VARCHAR(100) NOT NULL," +
                "" + AUDIO_SUBTITLE + " VARCHAR(255) NOT NULL," +
                "" + AUDIO_URL + " VARCHAR(2255) NOT NULL UNIQUE," +
                "" + AUDIO_THUMB + " VARCHAR(2255) NOT NULL," +
                "" + AUDIO_CALLBACK + " TEXT" +
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

    public void insertAudio(ArrayList<AudioMetadata> audios){
        String audioTitle, audioUrl, audioSubtitle, audioExec, audioThumb;
        SQLiteDatabase db = getWritableDatabase();

        for(AudioMetadata meta : audios){
            audioUrl = meta.getAudioUrl();
            audioTitle = meta.getAudioTitle();
            audioSubtitle = meta.getAudioSubtitle();
            audioThumb = meta.getAudioThumb();
            audioExec = meta.getAudioExec();
            try {
                //db.insert(SCRIPTS_TABLE_NAME, null, cv);
                db.execSQL("INSERT INTO " + AUDIO_TABLE_NAME + "("+AUDIO_TITLE+", "+ AUDIO_URL + ", " + AUDIO_SUBTITLE + ", " + AUDIO_THUMB + ", " + AUDIO_CALLBACK + ")  VALUES (?, ?, ?, ?, ?)", new String[]{audioTitle, audioUrl, audioSubtitle, audioThumb, audioExec});
            } catch (Exception e) {
                try{
                    ContentValues cv = new ContentValues();
                    cv.put(AUDIO_TITLE, audioTitle);
                    cv.put(AUDIO_SUBTITLE, audioSubtitle);
                    cv.put(AUDIO_URL, audioUrl);
                    cv.put(AUDIO_THUMB, audioThumb);
                    cv.put(AUDIO_CALLBACK, audioExec);
                    db.update(AUDIO_TABLE_NAME, cv, " "+ AUDIO_URL + " = ?", new String[]{audioUrl});
                }catch (SQLException e1){
                    e.printStackTrace();
                }
                e.printStackTrace();
            }
        }
        try{
            db.close();
        }catch (SQLException e){

        }
    }

    public ArrayList<AudioMetadata> getAudio(int id){
        ArrayList<AudioMetadata> audios = new ArrayList<AudioMetadata>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + AUDIO_ID + ", " + AUDIO_URL + ", " + AUDIO_TITLE + ", " + AUDIO_SUBTITLE + ", " + AUDIO_THUMB + ", " + AUDIO_CALLBACK + " FROM " + SCRIPTS_TABLE_NAME + ";", new String[]{id + ""});
        while(c.moveToNext()){
            c.getInt(0);
//            AudioMetadata a = new AudioMetadata();
//            audios.add(a);
        }
        db.close();
        return audios;
    }

}
