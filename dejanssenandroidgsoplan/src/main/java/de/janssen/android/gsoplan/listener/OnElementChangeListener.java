package de.janssen.android.gsoplan.listener;

import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.dataclasses.Const;

import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

public class OnElementChangeListener implements OnPreferenceChangeListener {
    private ListPreference list;
    private Logger _logger;

    /**
     * Geh�rt zu AppPreferences
     *
     * @param list ListPreference
     */
    public OnElementChangeListener(ListPreference list) {
        this.list = list;
        _logger = new Logger(Const.APPFOLDER, "OnElementChangeListener");
        _logger.Trace("Registering OnElementChangeListener for " + list.getTitle());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            _logger.Info("Setting up new Element: " + newValue);
            list.setValue(newValue.toString());

        } catch (Exception e) {
            _logger.Error("An Error occurred while changing Element to Element " + newValue, e);
        }
        return false;

    }

}
