/*
 * AppPreferences.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */
package de.janssen.android.gsoplan.activities;

import java.io.File;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

import de.janssen.android.gsoplan.Convert;
import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.R;
import de.janssen.android.gsoplan.core.FileOPs;
import de.janssen.android.gsoplan.core.MyContext;
import de.janssen.android.gsoplan.core.Type;
import de.janssen.android.gsoplan.dataclasses.ListEntry;
import de.janssen.android.gsoplan.dataclasses.SelectOptions;
import de.janssen.android.gsoplan.listener.OnCheckboxChangeListener;
import de.janssen.android.gsoplan.listener.OnElementChangeListener;
import de.janssen.android.gsoplan.listener.OnTypeChangeListener;
import de.janssen.android.gsoplan.runnables.ErrorMessage;
import de.janssen.android.gsoplan.service.AlarmStarter;
import de.janssen.android.gsoplan.service.AutomuteService;
import de.janssen.android.gsoplan.xml.Xml;

public class AppPreferences extends PreferenceActivity implements Runnable {
    public MyContext ctxt;
    private ListEntry elementPref = new ListEntry(new CharSequence[]{}, new CharSequence[]{}, "listElement");
    private ListEntry typePref = new ListEntry(new CharSequence[]{}, new CharSequence[]{}, "listType");
    private ListEntry resyncPref = new ListEntry(new CharSequence[]{"20min", "30min", "1h", "1,5h", "2h", "3h", "5h", "12h", "24h"},
            new CharSequence[]{"20", "30", "60", "90", "120", "180", "300", "720", "1440"}, "listResync");
    private CheckBoxPreference checkbox = null;
    private Logger _logger;

    @Override
    protected void onResume() {
        super.onResume();
        ctxt.mIsRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        ctxt.mIsRunning = false;
    }

    @Override
    protected void onDestroy() {
        _logger.Info("Destroying AppPreferences");
        ctxt.executor.terminateAllThreads();
        super.onDestroy();
    }

    public AppPreferences() {

    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _logger = new Logger(this, "AppPreferences");
        ctxt = new MyContext(this);
        ctxt.context = this;
        ctxt.activity = this;
        addPreferencesFromResource(R.xml.preferences);
        ctxt.handler = new Handler();
        _logger.Info("-----------------------");
        _logger.Info("| Opening Preferences |");
        _logger.Info("-----------------------");
        loadData();
        if (ctxt.mProfil.types.list.size() == 0) {
            ctxt.handler.post(new ErrorMessage(ctxt, ctxt.context.getString(R.string.setup_message_error_noElements)));
        }

        this.run();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Prft, ob die Selectoren bereits in einer Datei vorliegen, um bei nicht
     * vorhandensein
     * <p/>
     * diese vom Gso Server zu laden
     *
     * @return
     * @author Tobias Janssen
     */
    public void loadData() {
        //Die Type Datei laden
        File typesFile = ctxt.mProfil.getTypesFile(ctxt.context);
        if (typesFile.exists()) {

            try {
                Xml xml = new Xml("root", FileOPs.readFromFile(typesFile));
                ctxt.mProfil.types = Convert.toTypesList(xml);

            } catch (Exception e) {
                _logger.Error("Error loading Types file", e);
            }
        } else
            _logger.Warning("Types file doesn't exist");

    }

    /**
     * @author Tobias Janssen
     */
    @SuppressWarnings("deprecation")
    public void setupType(ListEntry pref) {

        pref.entries = new CharSequence[0];
        pref.vals = new CharSequence[0];
        if (ctxt.mProfil.types.list.size() > 0) {
            pref.entries = new CharSequence[ctxt.mProfil.types.list.size()];
            pref.vals = new CharSequence[ctxt.mProfil.types.list.size()];

            for (int i = 0; i < ctxt.mProfil.types.list.size(); i++) {
                pref.entries[i] = ctxt.mProfil.types.list.get(i).typeName;
                pref.vals[i] = ctxt.mProfil.types.list.get(i).typeName;
            }
            pref.list = (ListPreference) findPreference(pref.prefKey);
            pref.list.setEntryValues(pref.vals);
            pref.list.setEntries(pref.entries);
            pref.list.setOnPreferenceChangeListener(new OnTypeChangeListener(ctxt, pref.list, this, ctxt.mProfil.types));
            if (pref.entries.length >= ctxt.mProfil.myTypeIndex) {
                pref.list.setValueIndex(ctxt.mProfil.myTypeIndex);
            }
        } else {
            _logger.Warning("Types list is empty");
            pref.entries = new CharSequence[1];
            pref.vals = new CharSequence[1];
            Type type = new Type();
            type.elementList.add(new SelectOptions("Verbindungsfehler", "Verbindungsfehler"));

            ctxt.mProfil.types.list.add(type);
            for (int i = 0; i < ctxt.mProfil.currType().elementList.size(); i++) {
                pref.entries[i] = ctxt.mProfil.currType().elementList.get(i).description;
                pref.vals[i] = ctxt.mProfil.currType().elementList.get(i).description;
            }
            pref.list = (ListPreference) findPreference(pref.prefKey);
            pref.entries = new CharSequence[1];
            pref.vals = new CharSequence[1];
            pref.entries[0] = "Verbindungsfehler";
            pref.vals[0] = "Verbindungsfehler";
            pref.list.setEntryValues(pref.vals);
            pref.list.setEntries(pref.entries);
        }

    }

    /**
     * @author Tobias Janssen
     */
    @SuppressWarnings("deprecation")
    public void setupElement(ListEntry pref) {
        _logger.Info("Setting up Elements");
        pref.entries = new CharSequence[0];
        pref.vals = new CharSequence[0];

        if (ctxt.mProfil.types.list.size() > 0 && ctxt.mProfil.currType().elementList.size() > 0) {
            pref.entries = new CharSequence[ctxt.mProfil.currType().elementList.size()];
            pref.vals = new CharSequence[ctxt.mProfil.currType().elementList.size()];

            for (int i = 0; i < ctxt.mProfil.currType().elementList.size(); i++) {
                pref.entries[i] = ctxt.mProfil.currType().elementList.get(i).description;
                pref.vals[i] = ctxt.mProfil.currType().elementList.get(i).description;
            }
            pref.list = (ListPreference) findPreference(pref.prefKey);
            pref.list.setEntryValues(pref.vals);
            pref.list.setEntries(pref.entries);
            pref.list.setOnPreferenceChangeListener(new OnElementChangeListener(pref.list));
            pref.list.setValue(ctxt.mProfil.myElement);
        } else {
            pref.entries = new CharSequence[1];
            pref.vals = new CharSequence[1];
            Type type = new Type();
            type.elementList.add(new SelectOptions("Verbindungsfehler", "Verbindungsfehler"));

            ctxt.mProfil.types.list.add(type);
            for (int i = 0; i < ctxt.mProfil.currType().elementList.size(); i++) {
                pref.entries[i] = ctxt.mProfil.currType().elementList.get(i).description;
                pref.vals[i] = ctxt.mProfil.currType().elementList.get(i).description;
            }
            pref.list = (ListPreference) findPreference(pref.prefKey);
            pref.entries = new CharSequence[1];
            pref.vals = new CharSequence[1];
            pref.entries[0] = "Verbindungsfehler";
            pref.vals[0] = "Verbindungsfehler";
            pref.list.setEntryValues(pref.vals);
            pref.list.setEntries(pref.entries);
        }
    }

    /**
     * @author Tobias Janssen
     */
    @SuppressWarnings("deprecation")
    public void setupResync() {
        _logger.Info("Setting up Resync");
        resyncPref.list = (ListPreference) findPreference(resyncPref.prefKey);
        resyncPref.list.setEntryValues(resyncPref.vals);
        resyncPref.list.setEntries(resyncPref.entries);
        resyncPref.list.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                try {
                    _logger.Info("Setting up new Resync: " + newValue);
                    int index = resyncPref.list.findIndexOfValue(newValue.toString());
                    if (index >= 0 && index < resyncPref.vals.length) {
                        ctxt.mProfil.myResync = Long.parseLong((String) resyncPref.vals[index]);
                        resyncPref.list.setValue(newValue.toString());
                        Intent service = new Intent(AppPreferences.this, AlarmStarter.class);
                        AppPreferences.this.startService(service);
                    } else
                        throw new Exception("Resync is not valid");
                } catch (Exception e) {
                    _logger.Error("An Error occurred while setting up new Resync Value", e);
                }
                return false;
            }

        });
        long myResync = ctxt.mProfil.myResync;
        int index = 1;
        for (int i = 0; i < resyncPref.vals.length; i++) {
            if (myResync == Long.parseLong((String) resyncPref.vals[i])) {
                index = i;
                break;
            }
        }
        resyncPref.list.setValueIndex(index);
    }


    /**
     * @param ref
     * @param obj
     */
    public void setupCheckbox(String ref, Object obj) {
        setupCheckbox(ref, obj, null, null);
    }

    /**
     * @param ref
     * @param obj
     * @param runOnTrue
     */
    @SuppressWarnings("deprecation")
    public void setupCheckbox(String ref, Object obj, Runnable runOnTrue, Runnable runOnFalse) {
        checkbox = (CheckBoxPreference) findPreference(ref);
        checkbox.setOnPreferenceChangeListener(new OnCheckboxChangeListener(obj, checkbox, runOnTrue, runOnFalse));
        checkbox.setChecked((Boolean) obj);
    }


    @Override
    public void finish() {
        super.finish();

        _logger.Info("-----------------------");
        _logger.Info("| Closing Preferences |");
        _logger.Info("-----------------------");
        if (ctxt.mProfil.muteEvents) {
            _logger.Info("Automute is activated");
            Intent automuteService = new Intent(this, AutomuteService.class);
            this.startService(automuteService);
        }
        if (!validatePrefs()) {
            _logger.Error("Prefs havn't been set automatically. Solving...");
            ctxt.mProfil.myElement = this.elementPref.list.getValue();
            ctxt.mProfil.myTypeName = this.typePref.list.getValue();
            ctxt.mProfil.myTypeIndex = this.typePref.list.findIndexOfValue(ctxt.mProfil.myTypeName);
            ctxt.mProfil.setPrefs();
            if (!validatePrefs())
                _logger.Critical("Prefs havn't been set!");
        }

        //fen, ob preferences stimmen
        try {
            ctxt.executor.awaitTermination(30 * 1000);
        } catch (Exception e) {
            _logger.Error("An Error occurred while terminating threads", e);
        }

        Intent returnData = new Intent();
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, returnData);
        } else {
            getParent().setResult(Activity.RESULT_OK, returnData);
        }
    }

    private boolean validatePrefs() {
        ctxt.mProfil.loadPrefs();
        SharedPreferences prefs;
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);

            if (!elementPref.list.getValue().toLowerCase().contains(prefs.getString("listElement", "default").toLowerCase())) {
                _logger.Error("An Error occurred while validating Preferences! myElement is unequal");
                return false;
            }

            if (!this.typePref.list.getValue().toLowerCase().contains(prefs.getString("listType", "Klassen").toLowerCase())) {
                _logger.Error("An Error occurred while validating Preferences! myTypeName is unequal");
                return false;
            }

            if (!this.resyncPref.list.getValue().toLowerCase().contains(prefs.getString("listResync", "60").toLowerCase())) {
                _logger.Error("An Error occurred while validating Preferences! myResync is unequal");
                return false;
            }

            return true;
        } catch (Exception e) {
            _logger.Error("An Error occurred while validating Preferences", e);
            return false;
        }

    }

    public void run() {
        ctxt.handler.post(new Runnable() {

            public void run() {
                setupType(typePref);
                setupElement(elementPref);

                setupResync();
                setupCheckbox("boxHide", (Object) ctxt.mProfil.hideEmptyHours);
                setupCheckbox("boxAutoSync", (Object) ctxt.mProfil.autoSync,

                        //wenn Angewhlt wird:
                        new ErrorMessage(ctxt, ctxt.context.getString(R.string.msg_AutoSync), new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ctxt.mProfil.autoSync = true;
                                if (ctxt.mProfil.onlyWlan) {
                                    new ErrorMessage(ctxt, ctxt.context.getString(R.string.msg_WIFI)).run();
                                }
                                Intent service = new Intent(AppPreferences.this, AlarmStarter.class);
                                AppPreferences.this.startService(service);
                            }

                        }),
                        //wenn Abgewhlt wird:
                        new Runnable() {

                            @Override
                            public void run() {
                                ctxt.mProfil.autoSync = false;
                                Intent service = new Intent(AppPreferences.this, AlarmStarter.class);
                                AppPreferences.this.startService(service);
                            }

                        });
                setupCheckbox("boxWlan", (Object) ctxt.mProfil.onlyWlan, new Runnable() {

                    @Override
                    public void run() {
                        ctxt.mProfil.onlyWlan = true;
                        if (ctxt.mProfil.autoSync) {
                            new ErrorMessage(ctxt, ctxt.context.getString(R.string.msg_WIFI)).run();
                        }
                    }

                }, new Runnable() {
                    @Override
                    public void run() {
                        ctxt.mProfil.onlyWlan = false;
                    }
                });

                setupCheckbox("boxNotify", (Object) ctxt.mProfil.notificate);
                setupCheckbox("boxVibrate", (Object) ctxt.mProfil.vibrate);
                setupCheckbox("boxSound", (Object) ctxt.mProfil.sound);
                setupCheckbox("boxFastLoad", (Object) ctxt.mProfil.fastLoad);
//		setupCheckbox("boxMuteEvents", (Object)ctxt.mProfil.muteEvents);
            }

        });

    }

}
