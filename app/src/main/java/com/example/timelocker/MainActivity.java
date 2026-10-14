package com.example.timelocker;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.UserManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private SharedPreferences prefs;
    private CountDownTimer timer;
    private TextView tvTimer;
    private EditText etMinutes;
    private Button btnStart;
    private Button btnAllowedApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);

        Context storageContext = createDeviceProtectedStorageContext();
        prefs = storageContext.getSharedPreferences("zen_prefs", Context.MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.BLACK);
        int pad = (int) (64 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        tvTimer = new TextView(this);
        tvTimer.setText("00:00:00");
        tvTimer.setTextSize(48f);
        tvTimer.setTextColor(Color.WHITE);
        tvTimer.setGravity(Gravity.CENTER);
        layout.addView(tvTimer);

        etMinutes = new EditText(this);
        etMinutes.setHint("वेळ टाका (मिनिटांमध्ये)");
        etMinutes.setHintTextColor(Color.GRAY);
        etMinutes.setTextColor(Color.WHITE);
        etMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        etMinutes.setGravity(Gravity.CENTER);
        layout.addView(etMinutes);

        btnStart = new Button(this);
        btnStart.setText("START STRICT ZEN LOCK");
        layout.addView(btnStart);

        btnAllowedApps = new Button(this);
        btnAllowedApps.setText("Configure Allowed Apps");
        layout.addView(btnAllowedApps);

        setContentView(layout);

        long endTime = prefs.getLong("end_time", 0L);
        long now = System.currentTimeMillis();
        if (endTime > now) {
            enforceStrictLock(endTime - now);
        }

        btnStart.setOnClickListener(v -> {
            String input = etMinutes.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "कृपया मिनिटे टाका", Toast.LENGTH_SHORT).show();
                return;
            }
            long minutes = Long.parseLong(input);
            if (minutes <= 0) return;

            long durationMillis = minutes * 60 * 1000L;
            long targetTime = System.currentTimeMillis() + durationMillis;
            prefs.edit().putLong("end_time", targetTime).apply();
            enforceStrictLock(durationMillis);
        });

        btnAllowedApps.setOnClickListener(v -> {
            Intent i = new Intent(this, AllowedAppsActivity.class);
            startActivity(i);
        });

        // Handle kill switch intent if app launched via KillReceiver
        if (getIntent() != null && getIntent().getBooleanExtra("kill_now", false)) {
            unlockDevice();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getBooleanExtra("kill_now", false)) {
            unlockDevice();
        }
    }

    private void enforceStrictLock(long remainingMillis) {
        etMinutes.setVisibility(View.GONE);
        btnStart.setVisibility(View.GONE);
        btnAllowedApps.setVisibility(View.GONE);

        // Build allowed package list: include this app plus up to 6 user-selected apps
        Set<String> allowed = prefs.getStringSet("allowed_apps", new HashSet<>());
        Set<String> packages = new HashSet<>();
        packages.add(getPackageName());
        int count = 0;
        for (String p : allowed) {
            if (p == null) continue;
            if (packages.contains(p)) continue;
            packages.add(p);
            count++;
            if (count >= 6) break; // cap at 6 user apps
        }

        String[] pkgsArray = packages.toArray(new String[0]);

        if (dpm.isDeviceOwnerApp(getPackageName())) {
            dpm.setLockTaskPackages(adminComponent, pkgsArray);
            dpm.setLockTaskFeatures(adminComponent, DevicePolicyManager.LOCK_TASK_FEATURE_NONE);
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT);
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET);
            dpm.setUninstallBlocked(adminComponent, getPackageName(), true);
            startLockTask();
        } else {
            Toast.makeText(this, "Warning: ADB Device Owner परवानगी दिलेली नाही!", Toast.LENGTH_LONG).show();
        }

        if (timer != null) timer.cancel();
        timer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000;
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;
                tvTimer.setText(String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));
            }

            @Override
            public void onFinish() {
                unlockDevice();
            }
        }.start();
    }

    private void unlockDevice() {
        prefs.edit().putLong("end_time", 0L).apply();
        if (dpm.isDeviceOwnerApp(getPackageName())) {
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT);
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET);
            dpm.setUninstallBlocked(adminComponent, getPackageName(), false);
            // reset allowed packages to only this package
            dpm.setLockTaskPackages(adminComponent, new String[]{getPackageName()});
            try {
                stopLockTask();
            } catch (Exception ignored) {}
        }
        tvTimer.setText("00:00:00");
        etMinutes.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.VISIBLE);
        btnAllowedApps.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        long endTime = prefs.getLong("end_time", 0L);
        long now = System.currentTimeMillis();
        if (endTime > now) {
            enforceStrictLock(endTime - now);
        }
    }

    @Override
    public void onBackPressed() {
        // लॉक असताना Back बटण पूर्णपणे ब्लॉक
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (prefs.getLong("end_time", 0L) > System.currentTimeMillis()) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
