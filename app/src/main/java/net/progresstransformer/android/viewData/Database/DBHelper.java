package net.progresstransformer.android.viewData.Database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class DBHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "usersdb";
    private static final String TABLE_Video = "videodetails";
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_URL = "url";
    private static final String KEY_Progress = "progress";

    private static final String TABLE_PDF = "pdfdetails";
    private static final String KEY_PDF_ID = "id";
    private static final String KEY_PDF_NAME = "name";
    private static final String KEY_PDF_URL = "url";
    private static final String KEY_PDF_PAGE_NUMBER = "page_number";

    private static final String TABLE_AUDIO = "audiodetails";
    private static final String KEY_AUDIO_ID = "id";
    private static final String KEY_AUDIO_NAME = "name";
    private static final String KEY_AUDIO_URL = "url";
    private static final String KEY_AUDIO_PAGE_NUMBER = "progress";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_Video + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_NAME + " TEXT,"
                + KEY_URL + " TEXT,"
                + KEY_Progress + " TEXT" + ")";
        String CREATE_PDF_TABLE = "CREATE TABLE " + TABLE_PDF + "("
                + KEY_PDF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_PDF_NAME + " TEXT,"
                + KEY_PDF_URL + " TEXT,"
                + KEY_PDF_PAGE_NUMBER + " TEXT" + ")";
        String CREATE_AUDIO_TABLE = "CREATE TABLE " + TABLE_AUDIO + "("
                + KEY_AUDIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_AUDIO_NAME + " TEXT,"
                + KEY_AUDIO_URL + " TEXT,"
                + KEY_AUDIO_PAGE_NUMBER + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);
        db.execSQL(CREATE_PDF_TABLE);
        db.execSQL(CREATE_AUDIO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_Video);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PDF);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUDIO);
        // Create tables again
        onCreate(db);
    }

    public void insertData(String name, String url, String progress) {
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();
        cValues.put(KEY_NAME, name);
        cValues.put(KEY_URL, url);
        cValues.put(KEY_Progress, progress);
        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(TABLE_Video, null, cValues);
        db.close();
    }

    public void insertPdfData(String name, String url, String pageNumber) {
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();
        cValues.put(KEY_PDF_NAME, name);
        cValues.put(KEY_PDF_URL, url);
        cValues.put(KEY_PDF_PAGE_NUMBER, pageNumber);
        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(TABLE_PDF, null, cValues);
        db.close();
    }

    public void insertAudioData(String name, String url, String pageNumber) {
        //Get the Data Repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();
        //Create a new map of values, where column names are the keys
        ContentValues cValues = new ContentValues();
        cValues.put(KEY_AUDIO_NAME, name);
        cValues.put(KEY_AUDIO_URL, url);
        cValues.put(KEY_AUDIO_PAGE_NUMBER, pageNumber);
        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(TABLE_AUDIO, null, cValues);
        db.close();
    }

    public ArrayList<HashMap<String, String>> getvideoData(String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> userList = new ArrayList<>();
        String query = "SELECT name, location, designation FROM " + TABLE_Video;
        Cursor cursor = db.query(TABLE_Video, new String[]{KEY_NAME, KEY_Progress}, KEY_URL + "=?", new String[]{url}, null, null, null, null);
        if (cursor.moveToNext()) {
            HashMap<String, String> user = new HashMap<>();
            user.put("name", cursor.getString(cursor.getColumnIndex(KEY_NAME)));
            user.put("progress", cursor.getString(cursor.getColumnIndex(KEY_Progress)));
            userList.add(user);
        }
        return userList;
    }

    public ArrayList<HashMap<String, String>> getPdfData(String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> userList = new ArrayList<>();
        String query = "SELECT name, location, designation FROM " + TABLE_Video;
        Cursor cursor = db.query(TABLE_PDF, new String[]{KEY_PDF_NAME, KEY_PDF_PAGE_NUMBER}, KEY_PDF_URL + "=?", new String[]{url}, null, null, null, null);
        if (cursor.moveToNext()) {
            HashMap<String, String> user = new HashMap<>();
            user.put("name", cursor.getString(cursor.getColumnIndex(KEY_PDF_NAME)));
            user.put("progress", cursor.getString(cursor.getColumnIndex(KEY_PDF_PAGE_NUMBER)));
            userList.add(user);
        }
        return userList;
    }

    public ArrayList<HashMap<String, String>> getAudioData(String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> userList = new ArrayList<>();
        String query = "SELECT name, location, designation FROM " + TABLE_AUDIO;
        Cursor cursor = db.query(TABLE_AUDIO, new String[]{KEY_AUDIO_NAME, KEY_AUDIO_PAGE_NUMBER}, KEY_AUDIO_URL + "=?", new String[]{url}, null, null, null, null);
        if (cursor.moveToNext()) {
            HashMap<String, String> user = new HashMap<>();
            user.put("name", cursor.getString(cursor.getColumnIndex(KEY_AUDIO_NAME)));
            user.put("progress", cursor.getString(cursor.getColumnIndex(KEY_AUDIO_PAGE_NUMBER)));
            userList.add(user);
        }
        return userList;
    }

    public int updateProgress(String progress, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cVals = new ContentValues();
        cVals.put(KEY_Progress, progress);
        int count = db.update(TABLE_Video, cVals, KEY_URL + " = ?", new String[]{url});
        return count;
    }

    public int updatePdfPageNumber(String pageNumber, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cVals = new ContentValues();
        cVals.put(KEY_PDF_PAGE_NUMBER, pageNumber);
        int count = db.update(TABLE_PDF, cVals, KEY_PDF_URL + " = ?", new String[]{url});
        return count;
    }

    public int updateAudioProgress(String pageNumber, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cVals = new ContentValues();
        cVals.put(KEY_AUDIO_PAGE_NUMBER, pageNumber);
        int count = db.update(TABLE_AUDIO, cVals, KEY_AUDIO_URL + " = ?", new String[]{url});
        return count;
    }
}
