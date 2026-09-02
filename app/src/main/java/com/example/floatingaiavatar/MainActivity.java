package com.example.floatingaiavatar;
import android.app.Activity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button btn = new Button(this);
        btn.setText("Start Floating Avatar");
        btn.setTextSize(18);
        setContentView(btn);
        btn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
                return;
            }
            Intent s = new Intent(this, FloatingAvatarService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(s); else startService(s);
            finish();
        });
    }
}
