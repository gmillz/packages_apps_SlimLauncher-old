package com.android.launcher3.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.android.gallery3d.util.IntArray;
import com.android.launcher3.R;
import com.android.launcher3.util.CustomSharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by gmillz on 9/7/14.
 */
public class BackupRestoreFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.backup_restore_settings);

        Preference backup = findPreference("backup");
        backup.setOnPreferenceClickListener(this);
        Preference restore = findPreference("restore");
        restore.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("backup")) {
            backup();
        } else if (preference.getKey().equals("restore")) {
            restore();
        }
        return true;
    }

    private void backup() {
        File file = new File("data/data/" + getActivity().getApplication().getPackageName()
                + "/shared_prefs/" + SettingsProvider.SETTINGS_KEY + ".xml");
        File toPath = new File(Environment.getExternalStorageDirectory() + "/Slim/Launcher/");
        File toFile = new File(toPath + "/" + SettingsProvider.SETTINGS_KEY + ".xml");

        if (!toPath.exists()) {
            toPath.mkdirs();
        }

        Log.d("Backup", file.toString());
        Log.d("Backup", toFile.toString());

        try {
            copy(file, toFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restore() {
        File toPath = new File(Environment.getExternalStorageDirectory() + "/Slim/Launcher/");
        File toFile = new File(toPath + "/" + SettingsProvider.SETTINGS_KEY + ".xml");
        Log.d("RESTORE", toFile.toString());
        CustomSharedPreferences csp = new CustomSharedPreferences(toFile, Context.MODE_PRIVATE);
        Log.d("RESTORE", "not frozen yet");
        Map<String, Object> map = csp.getAll();
        Log.d("RESTORE", "still not frozen");
        for (String s : map.keySet()) {
            Log.d("RESTORE", s);
        }
        List<String> current = new ArrayList<String>();
        try {
            for (Field field : BackupRestoreFragment.class
                    .getClassLoader()
                    .loadClass(SettingsKeys.class.getCanonicalName())
                    .getFields()) {
                if (field.getType().equals(String.class)) {
                    Log.d("RESTORE", "from SettingsProvider: " + (String) field.get(null));
                    current.add((String) field.get(null));
                }
            }
        } catch (ClassNotFoundException e) {
        } catch (IllegalAccessException e) {
        }

        for (String s : map.keySet()) {
            if (current.contains(s)) {
                Object o = map.get(s);
                if (o instanceof String) {
                    SettingsProvider.putString(getActivity(), s, (String) o);
                } else if (o instanceof Boolean) {
                    SettingsProvider.putBoolean(getActivity(), s, (Boolean) o);
                } else if (o instanceof Integer) {
                    SettingsProvider.putInt(getActivity(), s, (Integer) o);
                }
            }
        }
    }

    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}