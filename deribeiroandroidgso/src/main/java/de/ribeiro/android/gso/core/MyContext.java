/*
 * MyContext.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */

package de.ribeiro.android.gso.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.viewpagerindicator.TitlePageIndicator;

import java.util.Calendar;

import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.R;
import de.ribeiro.android.gso.WorkerQueue;
import de.ribeiro.android.gso.dataclasses.Const;
import de.ribeiro.android.gso.dataclasses.Pager;

public class MyContext {
    //public List<Profil> profilList = new ArrayList<Profil>();
    public Calendar dateBackup;
    public Handler handler = new Handler();
    public WorkerQueue executor = new WorkerQueue();
    public Pager pager;
    public Context context;
    public Activity activity;
    public Boolean initialLunch = true;
    public LayoutInflater inflater;
    public int[] textSizes;
    public Boolean newVersionReqSetup = false;
    public String newVersionMsg = "";
    public Profil mProfil;
    public Menu appMenu;
    public Boolean mIsRunning = false;
    public Boolean appIsReady = false;
    private ViewPager vp;
    private TitlePageIndicator tpi;
    private Logger _logger;
    public Handler msgHandler = new Handler() {
        public void handleMessage(Message message) {
            _logger.Info("Refresh of Pager requested!");

            if (!appIsReady)
                _logger.Info("App is not ready yet -> waiting!");
            //Prfen, ob die App vollstndig geladen wurde
            for (int time = 0; !appIsReady || time == 10; time++) {

                //wenn nicht 2 Sekunden warten
                try {
                    java.lang.Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    //nichts
                }
            }
            if (!appIsReady)
                _logger.Info("App isn't ready after 10 seconds of waiting!");

            if (pager != null && appIsReady) {
                // Object path = message.obj;
                if (message.arg1 == Activity.RESULT_OK) {
                    // Daten von Stupid leeren
                    mProfil.stupid.clearData();
                    mProfil.stupid.clear();

                    try {
                        // Struktur prfen laden
                        mProfil.stupid.checkStructure(MyContext.this);
                        _logger.Info("Reload of all files");
                        // daten neu aus Datei laden
                        Tools.loadAllDataFiles(context, mProfil, mProfil.stupid);

                    } catch (Exception e) {
                        _logger.Error("Reload of Data failed", e);
                    }
                    if (refreshView())
                        _logger.Info("Pager refreshed successfully");
                } else {
                    Toast.makeText(context, "Download failed.", Toast.LENGTH_LONG).show();
                }
            } else
                _logger.Error("Refresh of Pager failed!");

        }

    };


    public MyContext(Context appctxt, Activity activity) {
        this.context = appctxt;
        this.activity = activity;
        this.mProfil = new Profil(this);
        this.mProfil.loadPrefs();
        _logger = new Logger(this.context, "MyContext");
        this.inflater = LayoutInflater.from(context);

        MyContext.this.vp = (ViewPager) MyContext.this.activity.findViewById(R.id.pager);
        MyContext.this.tpi = (TitlePageIndicator) MyContext.this.activity.findViewById(R.id.indicator);
        MyContext.this.pager = new Pager(context, vp, tpi, inflater, Const.TEXTSIZEOFHEADLINES, mProfil.hideEmptyHours);
    }


    public MyContext(Context appctxt) {
        this.context = appctxt;
        _logger = new Logger(this.context, "MyContext");
        this.mProfil = new Profil(this);
        this.mProfil.loadPrefs();

    }

    public Stupid getCurStupid() {
        return mProfil.stupid;
    }

    /**
     * Liefert den Wert der im Key bergebenen CheckboxPreference zurck
     *
     * @param key String, der den Preference-Key enthlt
     * @return Boolean, default false;
     */
    public Boolean getCheckboxPreference(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, false);
    }

    /**
     * Datum: 11.10.12
     *
     * @author Tobias Janssen Initialisiert den viewPager, der die Tage des
     * Stundenplans darstellt
     */
    public void initViewPager() {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (pager == null) {
                    _logger.Error("initViewPager request, but Pager is null!");
                    return;
                }
                if (mProfil.stupid.stupidData == null) {
                    _logger.Error("initViewPager request, but mProfil.stupid.stupidData is null!");
                    return;
                }

                pager.clear();
                //pager.currentPage = 0;
                for (int i = 0; i < mProfil.stupid.stupidData.size(); i++) {
                    pager.appendTimeTableToPager(mProfil.stupid.stupidData.get(i), MyContext.this);
                }
                pager.init(mProfil.stupid.currentDate);
            }

        });
    }

    /**
     * Datum: 11.10.12
     *
     * @author Tobias Janssen Initialisiert den viewPager, der die Tage des
     * Stundenplans darstellt
     */
    public void initViewPagerWaiting() {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                pager.clear();
                View page = inflater.inflate(R.layout.waiting_page, null);
                pager.addView(0, page, "Warte auf GSO-Daten...");
                pager.init(mProfil.stupid.currentDate);
            }

        });
    }

    /**
     * @param context
     * @return
     * @author Tobias Janssen
     * <p/>
     * Prft, ob eine Wlan verbindung besteht, und liefert das Ergebnis
     */
    public Boolean isWifiConnected() {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiinfo = wifi.getConnectionInfo();

        return wifiinfo.getNetworkId() != -1;
    }

    public Boolean refreshView() {
        //alle pagerDaten leeren
        int page = pager.getCurrentPage();
        pager = new Pager(context, vp, tpi, inflater, Const.TEXTSIZEOFHEADLINES, mProfil.hideEmptyHours);
        for (int i = 0; i < getCurStupid().stupidData.size(); i++) {
            pager.appendTimeTableToPager(getCurStupid().stupidData.get(i), this);
        }
        if (pager.size() < page) {
            _logger.Critical("refreshView request, but Pager.size is smaller than currentPage! Requested:" + page + " But size of Pager is " + pager.size());
            return false;
        }
        pager.setPage(page);
        pager.init(mProfil.stupid.currentDate);
        return true;
    }


}
