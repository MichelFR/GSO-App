package de.ribeiro.android.gso.listener;

import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;

import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.core.MyContext;
import de.ribeiro.android.gso.dataclasses.Types;

public class OnTypeChangeListener implements OnPreferenceChangeListener {
    private MyContext ctxt;
    private ListPreference list;
    private Runnable run;
    private Types typesList;
    private Logger _logger;

    /**
     * Geh�rt zu AppPreferences
     *
     * @param ctxt MyContext
     * @param list ListPreference
     * @param run  Runnable die nach fetchonlineSelectors durchgef�hrt wird(i.d.R. AppPreferences.this)
     */
    public OnTypeChangeListener(MyContext ctxt, ListPreference list, Runnable run, Types typesList) {
        this.ctxt = ctxt;
        this.list = list;
        this.run = run;
        this.typesList = typesList;
        _logger = new Logger(ctxt.context, "OnTypeChangeListener");
        _logger.Trace("Registering OnTypeChangeListener for " + list.getTitle());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            _logger.Info("OnTypeChangeListener called");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
            SharedPreferences.Editor editor = prefs.edit();
            int type = findIndexOfValueInTypeList(newValue.toString(), list);
            editor.putInt("myTypeIndex", type);
            ctxt.mProfil.myElement = "";
            editor.putString("listElement", "");
            if (typesList != null && typesList.list != null && typesList.list.size() >= type) {
                String typeKey = typesList.list.get(type).type;
                if (!typeKey.equalsIgnoreCase("null")) {
                    editor.putString("myTypeKey", typeKey);
                    ctxt.mProfil.myTypeKey = typeKey;
                } else
                    return false;
                String typeName = typesList.list.get(type).typeName;
                if (!typeName.equalsIgnoreCase("null")) {
                    editor.putString("listType", typeName);
                    ctxt.mProfil.myTypeName = typeName;
                } else
                    return false;
            } else {
                _logger.Warning("Types List is empty!");
            }
            ctxt.mProfil.myTypeIndex = type;
            editor.apply();
        } catch (Exception e) {
            _logger.Error("An Error occurred while changing type", e);
        }

        run.run();
        return false;

    }

    private int findIndexOfValueInTypeList(String value, ListPreference list) {
        int selected = list.findIndexOfValue(value);
        list.setValueIndex(selected);
        return selected;
    }


}
