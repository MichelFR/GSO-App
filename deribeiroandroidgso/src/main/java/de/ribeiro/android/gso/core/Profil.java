package de.ribeiro.android.gso.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;

import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.dataclasses.Const;
import de.ribeiro.android.gso.dataclasses.Types;

public class Profil {
    public String myElement = "";
    public int myTypeIndex = 0;
    public String myTypeKey = "";
    public String myTypeName = "";
    public Boolean onlyWlan = false;
    public Boolean hideEmptyHours = false;
    public Boolean autoSync = false;
    public Boolean notificate = true;
    public Boolean vibrate = true;
    public Boolean sound = true;
    public Boolean fastLoad = true;
    public Boolean muteEvents = false;
    public Types types = new Types();
    public Boolean isDirty = false;
    public long myResync = 60;
    public long mylastResync = 0;
    public long startCounter = 0;
    public Stupid stupid;
    private MyContext ctxt;
    private Logger _logger = new Logger(Const.APPFOLDER, "Profil");

    public Profil(MyContext ctxt) {
        this.ctxt = ctxt;
        this.stupid = new Stupid();
    }

    public Profil(String myElement, int myTypeIndex, String myTypeKey, String myTypeName, Boolean onlyWlan, Boolean hideEmptyHours, Boolean autoSync,
                  Boolean notificate, Boolean vibrate, Boolean sound, long myResync, long mylastResync, boolean fastLoad, boolean muteEvents, MyContext ctxt) {
        this.myElement = myElement;
        this.myTypeIndex = myTypeIndex;
        this.myTypeKey = myTypeKey;
        this.myTypeName = myTypeName;
        this.onlyWlan = onlyWlan;
        this.hideEmptyHours = hideEmptyHours;
        this.autoSync = autoSync;
        this.notificate = notificate;
        this.vibrate = vibrate;
        this.sound = sound;
        this.myResync = myResync;
        this.mylastResync = mylastResync;
        this.fastLoad = fastLoad;
        this.muteEvents = muteEvents;
        this.ctxt = ctxt;
        this.stupid = new Stupid();
    }

    public Profil clone() {
        Profil result = new Profil(this.myElement, this.myTypeIndex, this.myTypeKey, this.myTypeName, this.onlyWlan, this.hideEmptyHours, this.autoSync,
                this.notificate, this.vibrate, this.sound, this.myResync, this.mylastResync, this.fastLoad, this.muteEvents, this.ctxt);
        result.types = this.types.clone();
        return result;
    }

    /**
     * Liefert die Types Datei
     *
     * @param ctxt
     * @return
     */
    public File getTypesFile(Context ctxt) {
        File file = new File(ctxt.getFilesDir(), Const.FILETYPE);
        return file;
    }

    public Type currType() {
        return types.list.get(myTypeIndex);
    }


    /**
     * @return
     */
    public String getMyElement() {
        return this.myElement;
    }

    /**
     * @param value
     */
    public void setMyElement(String value) {
        this.myElement = value;
    }


    /**
     * @param context Context der Applikation
     * @author Tobias Janssen
     * ldt die einstellungen der applikation
     */
    public void loadPrefs() {
        SharedPreferences prefs;
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
        } catch (Exception e) {
            _logger.Error("Error loading Preferences", e);
            return;
        }

        //prfen, ob noch alte Settingsversionen vorhanden sind, wenn ja diese konvertieren und lschen
        try {
            myElement = prefs.getString("listElement", "");
            if (myElement.equalsIgnoreCase("Verbindungsfehler"))
                myElement = "";
        } catch (Exception e) {
            _logger.Error("Error loading Preferences", e);
        }
        try {
            myTypeName = prefs.getString("listType", "Klassen");
            if (myTypeName.equalsIgnoreCase("Verbindungsfehler"))
                myTypeName = "Klassen";
            myTypeIndex = prefs.getInt("myTypeIndex", 0);
            myTypeKey = prefs.getString("myTypeKey", "c");
        } catch (Exception e) {
            _logger.Error("Error loading Preferences", e);
        }
        try {
            String value = prefs.getString("listResync", "60");
            myResync = Long.parseLong(value);
        } catch (Exception e) {
            _logger.Error("Error loading Preferences", e);
        }
        try {
            String value = prefs.getString("mylastResync", "0");
            this.mylastResync = Long.parseLong(value);
        } catch (Exception e) {
            _logger.Error("Error loading Preferences", e);
        }

        hideEmptyHours = prefs.getBoolean("boxHide", false);
        fastLoad = prefs.getBoolean("boxFastLoad", true);
        muteEvents = prefs.getBoolean("boxMuteEvents", false);
        onlyWlan = prefs.getBoolean("boxWlan", false);
        autoSync = prefs.getBoolean("boxAutoSync", false);
        notificate = prefs.getBoolean("boxNotify", true);
        vibrate = prefs.getBoolean("boxVibrate", true);
        sound = prefs.getBoolean("boxSound", true);

    }

    /**
     * @param context Context der Applikation
     * @author Tobias Janssen
     * setzt die bestehenden einstellungen der applikation auf dieses Profil
     */
    public void setPrefs() {
        SharedPreferences prefs;
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
        } catch (Exception e) {
            _logger.Error("Error setting Preferences", e);
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();


        try {
            editor.putString("listElement", myElement);
            editor.putString("listType", myTypeName);
            editor.putInt("myTypeIndex", myTypeIndex);
            editor.putString("myTypeKey", myTypeKey);
            editor.putString("listResync", String.valueOf(myResync));
            editor.putString("mylastResync", String.valueOf(mylastResync));
            editor.putBoolean("boxHide", hideEmptyHours);
            editor.putBoolean("boxWlan", onlyWlan);
            editor.putBoolean("boxFastLoad", fastLoad);
            editor.putBoolean("boxMuteEvents", muteEvents);
            editor.putBoolean("boxAutoSync", autoSync);
            editor.putBoolean("boxNotify", notificate);
            editor.putBoolean("boxVibrate", vibrate);
            editor.putBoolean("boxSound", sound);
            editor.apply();

        } catch (Exception e) {
            _logger.Error("Error setting Preferences", e);
        }
    }

    public int getMyTypeIndex() {
        if (myTypeName == null)
            return 0;
        for (int i = 0; i < types.list.size(); i++) {
            if (types.list.get(i).typeName.equalsIgnoreCase(myTypeName)) {
                return i;
            }
        }
        return 0;
    }
}
