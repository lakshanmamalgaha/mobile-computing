package net.progresstransformer.android.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.progresstransformer.android.R;
import net.progresstransformer.android.util.Settings;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Show basic application information
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(new Settings(this).getTheme());
        setContentView(R.layout.activity_about);

        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }
}
