package net.progresstransformer.android.viewData.PlayVideo;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import net.progresstransformer.android.R;
import net.progresstransformer.android.viewData.Database.DBHelper;
import net.progresstransformer.android.viewData.VideoLoder.Constant;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayerVideo extends AppCompatActivity {
    VideoView videoView;
    MediaController mediaController;
    private DBHelper dbHelper;
    private Uri urlOfVideo;


    public int lastPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DBHelper(this);

        setContentView(R.layout.activity_player_video);
        videoView = (VideoView) findViewById(R.id.videoView);
        mediaController = new MediaController(this);

        Intent intent = getIntent();
        int positionOfArray = intent.getIntExtra("uri", 0);
        urlOfVideo = Uri.fromFile(Constant.allMediaList.get(positionOfArray));

        ArrayList<HashMap<String, String>> progressAttay1 = dbHelper.getvideoData(String.valueOf(urlOfVideo));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Start the video at the begining?");
        builder.setCancelable(false);

        videoView.setVideoURI(urlOfVideo);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        Log.d("aa", String.valueOf(urlOfVideo));

        if (progressAttay1.isEmpty()) {
            lastPosition = videoView.getCurrentPosition();
            dbHelper.insertData(Constant.allMediaList.get(positionOfArray).getName(), String.valueOf(urlOfVideo), String.valueOf(lastPosition));
            videoView.seekTo(lastPosition);
            videoView.start();
            //Constant.allSendToDB.add(String.valueOf(urlOfVideo));
        } else {
            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    lastPosition = videoView.getCurrentPosition();
                    String urlOfVideoString = String.valueOf(urlOfVideo);

                    dbHelper.updateProgress(String.valueOf(lastPosition), urlOfVideoString);
                    videoView.seekTo(lastPosition);
                    videoView.start();

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ArrayList<HashMap<String, String>> progressAttay = dbHelper.getvideoData(String.valueOf(urlOfVideo));
                    lastPosition = Integer.parseInt(progressAttay.get(0).get("progress"));
                    videoView.seekTo(lastPosition);
                    videoView.start();
                    dialog.cancel();
                }
            });
            builder.show();
            Log.d("aa", String.valueOf(lastPosition));
        }

    }

    @Override
    protected void onStop() {
        videoView.pause();
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        lastPosition = videoView.getCurrentPosition();
        String urlOfVideoString = String.valueOf(urlOfVideo);
        dbHelper.updateProgress(String.valueOf(lastPosition), urlOfVideoString);

        super.onPause();
    }
}
