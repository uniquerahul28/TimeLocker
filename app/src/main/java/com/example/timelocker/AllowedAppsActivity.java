package com.example.timelocker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllowedAppsActivity extends Activity {

    private PackageManager pm;
    private SharedPreferences prefs;
    private Set<String> selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pm = getPackageManager();
        prefs = createDeviceProtectedStorageContext().getSharedPreferences("zen_prefs", Context.MODE_PRIVATE);
        selected = new HashSet<>(prefs.getStringSet("allowed_apps", new HashSet<>()));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Select up to 6 apps to allow while locked");
        title.setTextSize(18f);
        title.setPadding(24,24,24,24);
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(16,16,16,16);

        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> launchable = new ArrayList<>();
        for (ApplicationInfo ai : apps) {
            if (pm.getLaunchIntentForPackage(ai.packageName) != null) {
                launchable.add(ai);
            }
        }
        Collections.sort(launchable, (a,b) -> pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));

        for (ApplicationInfo ai : launchable) {
            String pkg = ai.packageName;
            String label = pm.getApplicationLabel(ai).toString();
            CheckBox cb = new CheckBox(this);
            cb.setText(label + " (" + pkg + ")");
            cb.setChecked(selected.contains(pkg));
            cb.setTag(pkg);
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (selected.size() >= 6) {
                        Toast.makeText(this, "You can only select up to 6 apps", Toast.LENGTH_SHORT).show();
                        cb.setChecked(false);
                        return;
                    }
                    selected.add(pkg);
                } else {
                    selected.remove(pkg);
                }
            });
            list.addView(cb);
        }

        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);

        Button btnSave = new Button(this);
        btnSave.setText("Save");
        btnSave.setOnClickListener(v -> {
            prefs.edit().putStringSet("allowed_apps", new HashSet<>(selected)).apply();
            Toast.makeText(this, "Allowed apps saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setOnClickListener(v -> finish());

        buttons.addView(btnCancel);
        buttons.addView(btnSave);
        root.addView(buttons);

        setContentView(root);
    }
}
