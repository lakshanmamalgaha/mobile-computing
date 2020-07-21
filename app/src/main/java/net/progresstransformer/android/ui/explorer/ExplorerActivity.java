package net.progresstransformer.android.ui.explorer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import net.progresstransformer.android.R;
import net.progresstransformer.android.ui.ShareActivity;
import net.progresstransformer.android.util.Settings;
import net.progresstransformer.android.viewData.Database.DBHelper;
import net.progresstransformer.android.viewData.VideoLoder.Constant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Explorer for browsing directories
 */
public class ExplorerActivity extends AppCompatActivity  {

    private static final String TAG = "ExplorerActivity";
    private static final int SHARE_REQUEST = 1;

    public int positionOfArray;
    private Uri urlOfVideo;
    private Uri progress_text;
    private DBHelper dbHelper;
    private int lastPosition;
    private String type;
    private String filename;
    ArrayList<HashMap<String, String>> progressAttay1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper=new DBHelper(this);

        ArrayList<Uri> uris= new ArrayList<Uri>();

        Intent intent = getIntent();
        positionOfArray = intent.getIntExtra("uri", 0);
        type=intent.getStringExtra("type");

        Log.d("aa",type);

        if (type.equals("video")){
            urlOfVideo = Uri.fromFile(Constant.allMediaList.get(positionOfArray));
            filename = Constant.allMediaList.get(positionOfArray).getName();

            progressAttay1 = dbHelper.getvideoData(String.valueOf(urlOfVideo));
            if (!progressAttay1.isEmpty()) {
                lastPosition = Integer.parseInt(progressAttay1.get(0).get("progress"));
            } else{
                lastPosition=0;
            }
        }else if (type.equals("audio")){
            urlOfVideo = Uri.fromFile(Constant.allaudioList.get(positionOfArray));
            filename = Constant.allaudioList.get(positionOfArray).getName();
            progressAttay1 = dbHelper.getAudioData(String.valueOf(urlOfVideo));
            if (!progressAttay1.isEmpty()) {
                lastPosition = Integer.parseInt(progressAttay1.get(0).get("progress"));
            } else{
                lastPosition=0;
            }
        }else if (type.equals("pdf")){
            urlOfVideo = Uri.fromFile(Constant.allpdfList.get(positionOfArray));
            filename = Constant.allpdfList.get(positionOfArray).getName();
            progressAttay1 = dbHelper.getPdfData(String.valueOf(urlOfVideo));
            if (!progressAttay1.isEmpty()) {
                lastPosition = Integer.parseInt(progressAttay1.get(0).get("progress"));
            } else{
                lastPosition=0;
            }
        }
        Log.d("aa", String.valueOf(lastPosition));
        Log.d("aa", filename);
        setTheme(new Settings(this).getTheme());
        setContentView(R.layout.activity_explorer);
        dbHelper=new DBHelper(this);

        uris.add(urlOfVideo);

        generateNoteOnSD(this, "sample", String.valueOf(lastPosition)+" "+filename+" "+type);
        //generateNoteOnSD( this, "filename", filename);

        //Toast.makeText(this, readFile(), Toast.LENGTH_SHORT).show();

        progress_text=Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Notes/sample"));

        uris.add(progress_text);

        Intent shareIntent = new Intent(this, ShareActivity.class);
        shareIntent.setAction("android.intent.action.SEND_MULTIPLE");
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        startActivityForResult(shareIntent, SHARE_REQUEST);

    }

    public void generateNoteOnSD(Context context, String sFileName, String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            //Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void generateNoteOnSDFileName(Context context, String sFileName, String sBody) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String readFile() {
        File fileEvents = new File(Environment.getExternalStorageDirectory()+"/Notes/sample");
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileEvents));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) { }
        String result = text.toString();
        return result;
    }
    public void readNote(){
        //Find the directory for the SD Card using the API
//*Don't* hardcode "/sdcard"
        File sdcard = Environment.getExternalStorageDirectory();

//Get the text file
        File file = new File(sdcard,"file.txt");

//Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        Toast.makeText(this, text.toString(), Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SHARE_REQUEST) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }
}
