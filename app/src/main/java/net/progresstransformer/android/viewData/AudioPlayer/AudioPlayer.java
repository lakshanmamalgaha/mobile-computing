package net.progresstransformer.android.viewData.AudioPlayer;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import net.progresstransformer.android.R;
import net.progresstransformer.android.viewData.Database.DBHelper;
import net.progresstransformer.android.viewData.VideoLoder.Constant;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashMap;


public class AudioPlayer extends AppCompatActivity {

    static MediaPlayer mp;//assigning memory loc once or else multiple songs will play at once
    int position;
    SeekBar sb;
    //ArrayList<File> mySongs;
    Thread updateSeekBar;
    Button pause, next, previous;
    TextView songNameText;
    private DBHelper dbHelper;
    private Uri urlOfAudio;
    int currentPosition;
    private int lastPosition;
    private boolean isPause;

    String sname;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        Intent intent = getIntent();
        position = intent.getIntExtra("uri", 0);
        urlOfAudio = Uri.fromFile(Constant.allaudioList.get(position));
        dbHelper = new DBHelper(this);

        songNameText = (TextView) findViewById(R.id.txtSongLabel);

        ArrayList<HashMap<String, String>> progressAttay1 = dbHelper.getAudioData(String.valueOf(urlOfAudio));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Start the audio at the begining?");
        builder.setCancelable(false);


        if (progressAttay1.isEmpty()) {
            lastPosition = 0;
            dbHelper.insertAudioData(Constant.allMediaList.get(position).getName(), String.valueOf(urlOfAudio), String.valueOf(lastPosition));

        } else {

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    lastPosition = 0;
                    String urlOfVideoString = String.valueOf(urlOfAudio);
                    dbHelper.updateAudioProgress(String.valueOf(lastPosition), urlOfVideoString);
                    playAudio();

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ArrayList<HashMap<String, String>> progressAttay = dbHelper.getAudioData(String.valueOf(urlOfAudio));
                    lastPosition = Integer.parseInt(progressAttay.get(0).get("progress"));
                    playAudio();
                    dialog.cancel();
                }
            });
            builder.show();
        }
        playAudio();


    }

    private void playAudio() {

        pause = (Button) findViewById(R.id.pause);

        sb = (SeekBar) findViewById(R.id.seekBar);


        updateSeekBar = new Thread() {
            @Override
            public void run() {
                int totalDuration = mp.getDuration();
                while (lastPosition < totalDuration && !isPause) {
                    try {
                        sleep(500);
                        lastPosition = mp.getCurrentPosition();
                        sb.setProgress(lastPosition);

                    } catch (InterruptedException e) {

                    }
                }
            }
        };


        if (mp != null) {
            mp.stop();
            mp.release();
        }

        sname = Constant.allaudioList.get(position).getName();

        songNameText.setSelected(true);

        Uri u = urlOfAudio;

        mp = MediaPlayer.create(getApplicationContext(), u);
        mp.seekTo(lastPosition);

        sb.setMax(mp.getDuration());
        updateSeekBar.start();
        sb.getProgressDrawable().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        sb.getThumb().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);


        sb.setOnSeekBarChangeListener(new
                                              SeekBar.OnSeekBarChangeListener() {
                                                  @Override
                                                  public void onProgressChanged(SeekBar seekBar, int i,
                                                                                boolean b) {
                                                  }

                                                  @Override
                                                  public void onStartTrackingTouch(SeekBar seekBar) {
                                                  }

                                                  @Override
                                                  public void onStopTrackingTouch(SeekBar seekBar) {
                                                      mp.seekTo(seekBar.getProgress());

                                                  }
                                              });
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sb.setMax(mp.getDuration());
                if (mp.isPlaying()) {
                    pause.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                    mp.pause();
                    isPause = true;

                } else {
                    pause.setBackgroundResource(R.drawable.pause);
                    mp.start();
                    isPause = false;
                }
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            isPause = true;
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        mp.pause();
        EventBus.getDefault().unregister(this);
        isPause = true;
        super.onStop();
    }

    @Override
    protected void onPause() {
        lastPosition = mp.getCurrentPosition();
        String urlOfVideoString = String.valueOf(urlOfAudio);
        dbHelper.updateAudioProgress(String.valueOf(lastPosition), urlOfVideoString);
        isPause = false;

        super.onPause();
    }
}