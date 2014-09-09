package com.android.launcher3.settings;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;

import com.android.launcher3.NotificationListener;
import com.android.launcher3.R;

import java.lang.reflect.Method;
import java.util.HashSet;

/**
 * Created by gmillz on 9/8/14.
 */
public class BadgeFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private HashSet<ComponentName> mEnabledListeners = new HashSet<ComponentName>();

    private CheckBoxPreference mEnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.badge_fragment);

        mEnable = (CheckBoxPreference) findPreference(SettingsProvider.KEY_ENABLE_BADGES);
        mEnable.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnable) {
            if (!isEnabled()) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
            return true;
        }
        return false;
    }

    private boolean isEnabled() {
        final String flat = Settings.Secure.getString(getActivity().getContentResolver(),
                "enabled_notification_listeners");
        boolean enabled = false;
        if (flat != null && !"".equals(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                if (name.contains(getActivity().getPackageName())) {
                    enabled = true;
                }
            }
        }
        return enabled;
    }
}
