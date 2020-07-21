package net.progresstransformer.android.viewData.Fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.progresstransformer.android.R;
import net.progresstransformer.android.viewData.AudioLoder.AudioRecycleViewAdaptor;
import net.progresstransformer.android.viewData.AudioPlayer.AudioPlayer;
import net.progresstransformer.android.viewData.MainActivity;
import net.progresstransformer.android.viewData.VideoLoder.Constant;


public class AudioFragment extends Fragment implements AudioRecycleViewAdaptor.OnNoteListner {
    View mView;

    private RecyclerView recyclerView;
    private AudioRecycleViewAdaptor audiorecyclerViewAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_audio_bottomnevigation, container, false);

        recyclerView = mView.findViewById(R.id.recyclerViewAudio);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.getContextOfApplication()));

        //if you face lack in scrolling then add following lines
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setNestedScrollingEnabled(false);

        audiorecyclerViewAdapter = new AudioRecycleViewAdaptor(MainActivity.getContextOfApplication(), this);

        recyclerView.setAdapter(audiorecyclerViewAdapter);

        return mView;
    }

    @Override
    public void onNoteClick(int position) {
        Intent intent = new Intent(getActivity(), AudioPlayer.class);
        Uri uri = Uri.fromFile(Constant.allaudioList.get(position));
        intent.putExtra("uri", position);
        startActivity(intent);
    }
}
