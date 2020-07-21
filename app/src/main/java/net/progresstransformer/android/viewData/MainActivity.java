package net.progresstransformer.android.viewData;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import net.progresstransformer.android.R;
import net.progresstransformer.android.viewData.Database.DBHelper;
import net.progresstransformer.android.viewData.Fragment.HomeFragement;
import net.progresstransformer.android.viewData.Fragment.VideoFragment;
import net.progresstransformer.android.viewData.VideoLoder.Method;
import net.progresstransformer.android.viewData.VideoLoder.StorageUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static long progress1;
    public static DBHelper dbHelper;

    private DrawerLayout drawer;
    private File directory;
    private String[] allPath;

    private boolean permission;
    private File storage;
    private String[] storagePaths;
    private Uri fileUri;
    private net.progresstransformer.android.viewData.VideoLoder.RecyclerViewAdapter recyclerViewAdapter;
    public static Context contextOfApplication;

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VideoFragment.setMainActivity(this);

        contextOfApplication = getApplicationContext();
        dbHelper = new DBHelper(this);

        checkStorageAccessPermission();
        //load data here
        storagePaths = StorageUtil.getStorageDirectories(this);

        for (String path : storagePaths) {

            storage = new File(path);
            Method.load_Directory_Files(storage);
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragement()).commit();


    }


    public void checkStorageAccessPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Permission Needed")
                        .setMessage("This permission is needed to access media file in your phone")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        1);
                            }
                        })
                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            // Do nothing. Because if permission is already granted then files will be accessed/loaded in splash_screen_activity
        }
    }


}
