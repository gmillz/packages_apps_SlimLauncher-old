package com.android.launcher3.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.android.launcher3.R;
import com.android.launcher3.util.ShortcutPickHelper;

public class GestureFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    Context mContext;

    ShortcutPickHelper mPicker;

    ListPreference mGestureUp;
    ListPreference mGestureDown;
    ListPreference mGesturePinch;
    ListPreference mGestureSpread;
    ListPreference mGestureDoubleTap;

    String mGesture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();

        addPreferencesFromResource(R.xml.gesture_fragment);

        mGestureUp = (ListPreference) findPreference(SettingsProvider.UP_GESTURE_ACTION);
        mGestureDown = (ListPreference) findPreference(SettingsProvider.DOWN_GESTURE_ACTION);
        mGesturePinch = (ListPreference) findPreference(SettingsProvider.PINCH_GESTURE_ACTION);
        mGestureSpread = (ListPreference) findPreference(SettingsProvider.SPREAD_GESTURE_ACTION);
        mGestureDoubleTap = (ListPreference)
                findPreference(SettingsProvider.DOUBLE_TAP_GESTURE_ACTION);

        mGestureUp.setOnPreferenceChangeListener(this);
        mGestureDown.setOnPreferenceChangeListener(this);
        mGesturePinch.setOnPreferenceChangeListener(this);
        mGestureSpread.setOnPreferenceChangeListener(this);
        mGestureDoubleTap.setOnPreferenceChangeListener(this);

        mPicker = new ShortcutPickHelper(getActivity(), new ShortcutPickHelper.OnPickListener() {
            @Override
            public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
                if (uri == null) {
                    return;
                }
                if (mGesture != null) {
                    SettingsProvider.putString(mContext, mGesture + "_custom", uri);
                    mGesture = null;
                }
            }
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mGestureUp || preference == mGestureDown ||
                preference == mGesturePinch || preference == mGestureSpread ||
                preference == mGestureDoubleTap) {
            if (newValue.equals("custom")) {
                String[] names = new String[] { "" };
                Intent.ShortcutIconResource[] icons = new Intent.ShortcutIconResource[] {
                        Intent.ShortcutIconResource.fromContext(
                                mContext, android.R.drawable.ic_delete)
                };
                mGesture = preference.getKey();
                mPicker.pickShortcut(names, icons, getId());
            }
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != Activity.RESULT_CANCELED
                && resultCode != Activity.RESULT_CANCELED) {
            mPicker.onActivityResult(requestCode, resultCode, data);
        }
    }
}
