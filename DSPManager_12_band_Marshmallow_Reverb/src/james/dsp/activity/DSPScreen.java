package james.dsp.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference;

import james.dsp.R;
import james.dsp.preference.EqualizerPreference;
import james.dsp.preference.SummariedListPreference;
import james.dsp.service.HeadsetService;

/**
 * This class implements a general PreferencesActivity that we can use to
 * adjust DSP settings. It adds a menu to clear the preferences on this page,
 * and a listener that ensures that our {@link HeadsetService} is running if
 * required.
 *
 * @author alankila
 */
public final class DSPScreen extends PreferenceFragment {
    protected static final String TAG = DSPScreen.class.getSimpleName();

    private final OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            /* If the listpref is updated, copy the changed setting to the eq. */
            if ("dsp.tone.eq".equals(key)) {
                String newValue = sharedPreferences.getString(key, null);
                if (!"custom".equals(newValue)) {
                    Editor e = sharedPreferences.edit();
                    e.putString("dsp.tone.eq.custom", newValue);
                    e.commit();

                    /* Now tell the equalizer that it must display something else. */
                    EqualizerPreference eq = (EqualizerPreference)
                            getPreferenceScreen().findPreference("dsp.tone.eq.custom");
                    eq.refreshFromPreference();
                }
            }

            /* If the equalizer surface is updated, select matching pref entry or "custom". */
            if ("dsp.tone.eq.custom".equals(key)) {
                String newValue = sharedPreferences.getString(key, null);

                String desiredValue = "custom";
                SummariedListPreference preset = (SummariedListPreference)
                        getPreferenceScreen().findPreference("dsp.tone.eq");
                for (CharSequence entry : preset.getEntryValues()) {
                    if (entry.equals(newValue)) {
                        desiredValue = newValue;
                        break;
                    }
                }

                /* Tell listpreference that it must display something else. */
                if (!desiredValue.equals(preset.getEntry())) {
                    Editor e = sharedPreferences.edit();
                    e.putString("dsp.tone.eq", desiredValue);
                    e.commit();
                    preset.refreshFromPreference();
                }
            }

            getActivity().sendBroadcast(new Intent(DSPManager.ACTION_UPDATE_PREFERENCES));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String config = getArguments().getString("config");
        boolean stereoWide = getArguments().getBoolean("stereoWide");

        getPreferenceManager().setSharedPreferencesName(
                DSPManager.SHARED_PREFERENCES_BASENAME + "." + config);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);

        try {
            int xmlId = R.xml.class.getField(config + "_preferences").getInt(null);
            addPreferencesFromResource(xmlId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final PreferenceCategory stereoWideCategory =
                (PreferenceCategory) getPreferenceScreen().findPreference("dsp.stereowide");
        if (stereoWideCategory != null && !stereoWide) {
            getPreferenceScreen().removePreference(stereoWideCategory);
        }

        final PreferenceCategory bassBoostCategory =
                (PreferenceCategory) getPreferenceScreen().findPreference("dsp.bass");
        if (bassBoostCategory != null && !stereoWide) {
            Preference centerFreq = bassBoostCategory.findPreference("dsp.bass.freq");
            if (centerFreq != null) {
                bassBoostCategory.removePreference(centerFreq);
            }
        }

        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(listener);
    }
}
