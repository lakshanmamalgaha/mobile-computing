package net.progresstransformer.android.viewData.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.progresstransformer.android.R;
import net.progresstransformer.android.viewData.MainActivity;
import net.progresstransformer.android.viewData.PlayVideo.PlayerVideo;
import net.progresstransformer.android.viewData.VideoLoder.Constant;
import net.progresstransformer.android.viewData.VideoLoder.Method;
import net.progresstransformer.android.viewData.VideoLoder.RecyclerViewAdapter;
import net.progresstransformer.android.viewData.VideoLoder.StorageUtil;

import java.io.File;

public class VideoFragment extends Fragment implements RecyclerViewAdapter.OnNoteListner {
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private Uri fileUri;
    private static net.progresstransformer.android.viewData.MainActivity mainActivity;


    //
    private boolean permission;
    private File storage;
    private String[] storagePaths;
    private View mView;

    public VideoFragment() {
        // Required empty public constructor
    }

    public static void setMainActivity(MainActivity activity) {
        mainActivity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_video_bottomnevigation, container, false);

        MainActivity mainActivity = new MainActivity();

        recyclerView = mView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.getContextOfApplication()));

        //if you face lack in scrolling then add following lines
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setNestedScrollingEnabled(false);

        recyclerViewAdapter = new RecyclerViewAdapter(MainActivity.getContextOfApplication(), this);

        recyclerView.setAdapter(recyclerViewAdapter);

        return mView;


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //load data here
                //for first time data will be loaded here
                //then it will be loaded in splash screen
                //because if we could not have permission then we could not load data in splash screen window
                storagePaths = StorageUtil.getStorageDirectories(getContext());

                for (String path : storagePaths) {
                    storage = new File(path);
                    //File file = new File(Environment.getExternalStorageDirectory(), path);
                    Method.load_Directory_Files(storage);
                }

                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    fileUri = data.getData();
                    MainActivity.getContextOfApplication().getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
        }
    }


    @Override
    public void onNoteClick(int position) {
        Intent intent = new Intent(getActivity(), PlayerVideo.class);
        Uri uri = Uri.fromFile(Constant.allMediaList.get(position));
        intent.putExtra("uri", position);
        startActivity(intent);
    }

}
