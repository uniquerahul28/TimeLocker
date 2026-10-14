package com.example.timelocker;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.UserManager;

public class KillReceiver extends BroadcastReceiver {

    // ADB shell UID is 2000. We only accept kill requests from shell (adb).
    private static final int ADB_UID = 2000;

    @Override
    public void onReceive(Context context, Intent intent) {
        int uid = Binder.getCallingUid();
        if (uid != ADB_UID) {
            // ignore unauthorized callers
            return;
        }

        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);

        // clear restrictions and stored end_time
        try {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT);
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET);
            dpm.setUninstallBlocked(admin, context.getPackageName(), false);
            dpm.setLockTaskPackages(admin, new String[]{context.getPackageName()});
        } catch (Exception ignored) {}

        SharedPreferences prefs = context.createDeviceProtectedStorageContext().getSharedPreferences("zen_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("end_time", 0L).apply();

        // Launch MainActivity with extra to force unlock
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("kill_now", true);
        context.startActivity(i);
    }
}
