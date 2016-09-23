/*
 * PlanActivity.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */
package de.janssen.android.gsoplan.activities;

import java.io.File;
import java.util.Calendar;

import de.janssen.android.gsoplan.dataclasses.ProfilManager;

import java.util.GregorianCalendar;

import com.google.analytics.tracking.android.EasyTracker;

import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.R;
import de.janssen.android.gsoplan.asyncTasks.PlanActivityLuncher;
import de.janssen.android.gsoplan.core.FileOPs;
import de.janssen.android.gsoplan.core.MyContext;
import de.janssen.android.gsoplan.core.UntisProvider;
import de.janssen.android.gsoplan.core.Tools;
import de.janssen.android.gsoplan.dataclasses.Const;

import android.os.Bundle;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.DatePicker;
import android.widget.Toast;

public class PlanActivity extends Activity {

    public MyContext ctxt;
    private int orientation;
    private Logger _logger;

    /**
     * @author janssen
     * ffnet ein Datumsplugin und prft, ob dieses TimeTable verfgbar ist, wenn ja, springt er dorthin
     */
    private void gotoDate() {
        ctxt.handler.post(new Runnable() {

            @Override
            public void run() {
                _logger.Info("Goto Date called");
                if (ctxt.mIsRunning && ctxt.pager != null && ctxt.pager.size() > 1) {
                    Calendar cal = ctxt.pager.getDateOfCurrentPage();
                    if (cal != null) {
                        DatePickerDialog picker = new DatePickerDialog(PlanActivity.this, new DatePickerDialog.OnDateSetListener() {

                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                _logger.Info("Set Date " + dayOfMonth + "." + monthOfYear + "." + year);
                                // das Ausgewhlte Datum einstellen
                                Calendar newDate = new GregorianCalendar();
                                newDate.set(year, monthOfYear, dayOfMonth);
                                // prfen, ob es sich dabei um wochenend tage
                                // handelt:
                                switch (newDate.get(Calendar.DAY_OF_WEEK)) {
                                    case Calendar.SATURDAY:
                                        newDate.setTimeInMillis(newDate.getTimeInMillis() + (1000 * 60 * 60 * 24 * 2));
                                        break;
                                    case Calendar.SUNDAY:
                                        newDate.setTimeInMillis(newDate.getTimeInMillis() + (1000 * 60 * 60 * 24 * 1));
                                        break;
                                }
                                int page = ctxt.pager.getPage(newDate, -1);
                                if (page != -1) {
                                    // gefunden
                                    ctxt.pager.setPage(page);
                                } else {
                                    _logger.Info("Selected date is unavailable");
                                    Toast.makeText(PlanActivity.this, "Dieses Datum ist nicht verfügbar!", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                        picker.show();
                    } else
                        _logger.Info("Cal Picker canceled, because Pager is empty");
                }

            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:

                ctxt.handler.post(new Runnable() {

                    @Override
                    public void run() {
                        _logger.Info("onActivityResult");
                        ProfilManager pm = new ProfilManager(PlanActivity.this.ctxt);
                        pm.profiles.get(pm.currentProfilIndex).loadPrefs();
                        pm.applyProfilIndex();
                        pm.saveAllProfiles();
                        Intent intent = new Intent(ctxt.activity, PlanActivity.class);
                        ctxt.activity.startActivity(intent);
                        finish();
                    }
                });
                break;
            case 1:
                ctxt.handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Intent intent = new Intent(ctxt.activity, PlanActivity.class);
                        ctxt.activity.startActivity(intent);
                        finish();
                    }
                });
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ctxt.mIsRunning = true;
        EasyTracker.getInstance(this).activityStart(this);

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int message = intent.getIntExtra("message", Activity.RESULT_CANCELED);
            _logger.Info("PlanActivity received refresh message");
            if (message == Activity.RESULT_OK) {
                Message msg = new Message();
                msg.arg1 = message;
                ctxt.msgHandler.handleMessage(msg);
            }
        }
    };

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Android Version prfen, wenn neuer als API11,
        Boolean actionBarAvailable = false;
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // ActionBar anfragen
            actionBarAvailable = getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        }
        _logger = new Logger(this, "PlanActivity");
        setContentView(R.layout.activity_plan);
        this.ctxt = new MyContext(this, this);

        Configuration c = getResources().getConfiguration();
        this.orientation = c.orientation;
        _logger.Info("Creating PlanActivity with orientation int: " + orientation);
        try {
            File f = new File(this.getCacheDir(), "date.bin");
            if (f.exists() && f.canRead()) {
                ctxt.mProfil.stupid.currentDate = (Calendar) FileOPs.loadObject(f);
                f.delete();
            }
        } catch (Exception e) {
            _logger.Error("An Error occurred while loading date.bin file", e);
        }

        ctxt.mProfil = new ProfilManager(ctxt).getCurrentProfil();
        ctxt.mProfil.setPrefs();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mMessageReceiver, new IntentFilter(Const.BROADCASTREFRESH));
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int noticationId = extras.getInt("notificationId");

            if (noticationId != 0) {
                extras.remove("notificationId");
                //notication aus taskbar entfernen
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.cancel(noticationId);
                Calendar date = new GregorianCalendar();
                date.setTimeInMillis(extras.getLong("date"));

                extras.remove("weekIndex");
                int profilIndex = extras.getInt("profilIndex");
                extras.remove("profilIndex");

                //alle Profile laden
                ProfilManager pm = new ProfilManager(ctxt);
                if (pm.profiles.size() >= pm.currentProfilIndex) {
                    pm.profiles.get(pm.currentProfilIndex).setPrefs();
                    if (profilIndex > pm.profiles.size() - 1)
                        profilIndex = 0;
                    else
                        pm.currentProfilIndex = profilIndex;
                    pm.applyProfilIndex();
                    ctxt.mProfil.loadPrefs();
                }


                ctxt.mProfil.stupid.currentDate = date;

            }

            ctxt.newVersionReqSetup = extras.getBoolean("newVersionInfo", false);
        }
        // Wenn ActionBar verfgbar ist,
        if (actionBarAvailable) {
            // ActionBar hinzufgen
            ActionBar actionBar = getActionBar();
            if (ctxt.mIsRunning)
                actionBar.show();
        }


        ctxt.executor.post(new PlanActivityLuncher(PlanActivity.this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ctxt.appMenu = menu;
        getMenuInflater().inflate(R.menu.activity_plan, menu);
        return true;
    }


    @Override
    protected void onDestroy() {
        Configuration c = getResources().getConfiguration();
        if (c.orientation != this.orientation) {
            _logger.Info("Closing Application...!");
            try {
                File file = new File(this.getCacheDir(), "date.bin");
                if (ctxt.pager.getDateOfCurrentPage() != null) {
                    FileOPs.saveObject(ctxt.pager.getDateOfCurrentPage(), file);
                } else {
                    _logger.Info("date.bin not saved, because pager is empty!");
                }
            } catch (Exception e) {
                _logger.Error("An Error occurred while saving to date.bin", e);
            }
        }


        // Alle Hintergrundprozesse beenden
        ctxt.executor.terminateAllThreads();
        super.onDestroy();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_setup) {
            Tools.gotoSetup(ctxt);
            return true;
        } else if (itemId == R.id.menu_gotoDate) {
            gotoDate();
            return true;
        } else if (itemId == R.id.menu_refresh) {
            UntisProvider.contactStupidService(this, ctxt.msgHandler);
            return true;
        } else if (itemId == R.id.menu_today) {
            ctxt.getCurStupid().currentDate = new GregorianCalendar();
            ctxt.pager.setPage(ctxt.getCurStupid().currentDate);
            return true;
        } else if (itemId == R.id.menu_profiles) {
            //ProfilActivity starten
            Intent intent = new Intent(this, ProfilActivity.class);
            startActivityForResult(intent, 1);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ctxt.appIsReady) {
            ctxt.mIsRunning = true;
            ctxt.initViewPagerWaiting();
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
            lbm.registerReceiver(mMessageReceiver, new IntentFilter(Const.BROADCASTREFRESH));
            Message msg = new Message();
            msg.arg1 = Activity.RESULT_OK;
            ctxt.msgHandler.handleMessage(msg);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        ctxt.mIsRunning = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }


}
