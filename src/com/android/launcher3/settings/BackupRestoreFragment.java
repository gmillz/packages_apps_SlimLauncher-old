package com.android.launcher3.settings;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.android.gallery3d.util.IntArray;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.R;
import com.android.launcher3.util.CustomSharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by gmillz on 9/7/14.
 */
public class BackupRestoreFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener {

    private ProgressDialog mProgress;

    private File mBackupFolder;
    private File mTmpBackupFolder;
    private File mTmpRestoreFolder;
    private File mDatabaseFolder;
    private File mFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgress = new ProgressDialog(getActivity());
        mProgress.setMessage("Please wait...");

        mBackupFolder = new File(Environment.getExternalStorageDirectory() + "/Slim/Launcher/");
        mTmpBackupFolder = new File(getActivity().getFilesDir() + "/tmp/backup");
        mTmpRestoreFolder = new File(getActivity().getFilesDir() + "/temp/restore");
        mDatabaseFolder = new File("/data/data/" + getActivity().getPackageName() + "/databases");


        addPreferencesFromResource(R.xml.backup_restore_settings);

        Preference backup = findPreference("backup");
        backup.setOnPreferenceClickListener(this);
        Preference restore = findPreference("restore");
        restore.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("backup")) {
            mProgress.setTitle("Backing up");
            backup();
        } else if (preference.getKey().equals("restore")) {
            mProgress.setTitle("Restoring");
            restore();
        }
        return true;
    }

    private void backup() {
        mProgress.show();
        if (!mBackupFolder.exists()) {
            mBackupFolder.mkdirs();
        }
        File prefXml = new File("/data/data/" + getActivity().getPackageName()
                + "/shared_prefs/" + SettingsProvider.SETTINGS_KEY + ".xml");

        File dbBackupDir = new File(mTmpBackupFolder + "/dbs/");
        if (!dbBackupDir.exists()) {
            dbBackupDir.mkdirs();
        }
        try {
            copy(prefXml, new File(mTmpBackupFolder + "/" + SettingsProvider.SETTINGS_KEY + ".xml"));
            for (String s : getActivity().databaseList()) {
                copy(getActivity().getDatabasePath(s), new File(mTmpBackupFolder + "/dbs/" + s));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> files = listFiles(mTmpBackupFolder.toString());
        zipFiles(files);
        mProgress.dismiss();
    }

    private List<String> listFiles(String dir) {
        List<String> val = new ArrayList<String>();
        for (String s : new File(dir).list()) {
            if (new File(dir + "/" + s).isDirectory()) {
                Log.d("listFiles", dir + " : is directory");
                val.addAll(listFiles(dir + "/" + s));
            } else {
                val.add(dir + "/" + s);
            }
        }
        return val;
    }

    private void zipFiles(List<String> list) {
        byte[] buf = new byte[1024];
        DateFormat df = new SimpleDateFormat("yyyyMMdd-HH:mm");
        Date date = new Date();
        File zipFile = new File(mBackupFolder +  "/backup-" + df.format(date) + ".zip");
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
            for (String s : list) {
                String file = s.substring(s.lastIndexOf("/"), s.length());
                FileInputStream in = new FileInputStream(s);
                out.putNextEntry(new ZipEntry(file));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
                new File(s).delete();
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restore() {
        final String[] files = mBackupFolder.list();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Backups");
        builder.setItems(files, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mFile = new File(mBackupFolder + "/" + files[which]);
                new restore().execute();
            }
        });
        builder.create().show();
    }

    public void copy(File src, File dst) throws IOException {
        if (dst.exists())
            dst.delete();

        FileChannel srcChan = new FileInputStream(src).getChannel();
        FileChannel dstChan = new FileOutputStream(dst).getChannel();
        dstChan.transferFrom(srcChan, 0, srcChan.size());
        srcChan.close();
        dstChan.close();
    }

    public class restore extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        protected Void doInBackground(Void... params) {
            List<String> files;
            if (mFile == null) return null;
            String input_zip = mFile.toString();
            mFile = null;
            String output = mTmpRestoreFolder.toString();
            byte[] buffer = new byte[1024];
            try {
                File folder = new File(output);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                ZipInputStream zis = new ZipInputStream(new FileInputStream(input_zip));
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    String filename = ze.getName();
                    Log.d("UNZIP", filename);
                    File newFile = new File(output + File.separator + filename);
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    ze = zis.getNextEntry();
                }
                zis.closeEntry();
                zis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File xml = null;
            for (String s : listFiles(mTmpRestoreFolder.toString())) {
                if (s.endsWith("db")) {
                    Log.d("RESTORE", s);
                    Log.d("RESTORE", new File(s).getName());
                    try {
                        File dst = new File(mDatabaseFolder + "/" + new File(s).getName());
                        Log.d("RESTORE", "destination, " + dst.toString());
                        //copy(new File(s), dst);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (s.contains("xml")) {
                    xml = new File(s);
                }
            }
            Log.d("RESTORE", "XML file=" +  xml.toString());
            Log.d("RESTORE", "not frozen yet");
            CustomSharedPreferences csp = new CustomSharedPreferences(xml, Context.MODE_PRIVATE);
            Log.d("RESTORE", "still not frozen");
            Map<String, Object> map = csp.getAll();
            Log.d("RESTORE", "not frozen 1");
            List<String> current = new ArrayList<String>();
            try {
                for (Field field : BackupRestoreFragment.class
                        .getClassLoader()
                        .loadClass(SettingsKeys.class.getCanonicalName())
                        .getFields()) {
                    if (field.getType().equals(String.class)) {
                        current.add((String) field.get(null));
                    }
                }
            } catch (ClassNotFoundException e) {
            } catch (IllegalAccessException e) {
            }

            Log.d("RESTORE", map.toString());
            for (String s : map.keySet()) {
                Log.d("RESTORE", s);
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
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            mProgress.dismiss();
        }
    }
}