package net.progresstransformer.android.viewData.SendVideo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.progresstransformer.android.R;


public class SendVideo extends AppCompatActivity {
    public int positionOfArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_video);

        Intent intent = getIntent();
        positionOfArray = intent.getIntExtra("uri", 0);
        String msg = String.valueOf(positionOfArray);
        TextView textView = findViewById(R.id.progresssend);
        textView.setText(msg);

    }


}
