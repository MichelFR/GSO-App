/*
 * MainActivity.java
 * 
 * Tobias Janssen, 2013
 * Einstiegtpunkt fr die App, ldt die Settings und startet anschlieend die entsprechende Activity
 * 
 * GNU GENERAL PUBLIC LICENSE Version 2
 */

package de.janssen.android.gsoplan.activities;

import java.io.File;

import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.R;
import de.janssen.android.gsoplan.core.MyContext;
import de.janssen.android.gsoplan.core.UntisProvider;
import de.janssen.android.gsoplan.core.Tools;
import de.janssen.android.gsoplan.runnables.ErrorMessage;
import de.janssen.android.gsoplan.service.AlarmStarter;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

import de.janssen.android.gsoplan.dataclasses.Const;

public class MainActivity extends Activity {

    public MyContext ctxt;
    private Bundle extras;
    private long startCounter = 0;
    private Logger _logger;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 0) {
            Intent intent = new Intent(MainActivity.this, de.janssen.android.gsoplan.activities.PlanActivity.class);

            if (extras != null) {

                intent.putExtras(extras);
            } else {
                Intent service = new Intent(MainActivity.this, AlarmStarter.class);
                MainActivity.this.startService(service);
            }
            MainActivity.this.startActivity(intent);

            MainActivity.this.finish();

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Const.APPFOLDER = this.getFilesDir();
        _logger = new Logger(this, "MainActivity");
        ctxt = new MyContext(this);
        _logger.Info("Starting Application");
        ctxt.mIsRunning = true;
        SharedPreferences prefs;
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
            startCounter = prefs.getLong("startCounter", 0);
        } catch (Exception e) {
            startCounter = 0;
        }
        try {
            startCounter++;
            prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("startCounter", startCounter);
            editor.apply();
        } catch (Exception e) {
        }

        // Extra Daten abholen
        extras = getIntent().getExtras();
        setContentView(R.layout.activity_main);
        loadData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }

    public void clearApplicationData() {
        File cache = getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                if (!s.equals("lib")) {
                    deleteDir(new File(appDir, s));
                }
            }
        }
    }

    public void loadData() {
        try {
            int versioncode = Tools.getDataVersion(ctxt);
            if (versioncode < 15) {
                clearApplicationData();
            }

            if (!Tools.isNewVersion(ctxt)) {
                // version bereits bekannt
                // default activity starten
                continueAppStart();
            } else {
                Context cont = ctxt.context.getApplicationContext();
                PackageInfo pInfo = cont.getPackageManager().getPackageInfo(cont.getPackageName(), 0);
                String currentVersion = pInfo.versionName;
                _logger.Info("===============================================================");
                _logger.Info("| GSOPlan was updated to " + currentVersion + "            |");
                _logger.Info("===============================================================");
                UntisProvider.contactStupidServiceQuiet(ctxt.context, null);
                if(this.getString(R.string.app_suppressUpdateScreen).equalsIgnoreCase("true"))
                    continueAppStart();
            }
        } catch (Exception e) {
            // fehler beim lesen der Versionsdatei
            // egal, laden fortsetzen
            continueAppStart();

        }

    }

    @Override
    protected void onStop() {
        _logger.Info("Exiting Application");
        _logger.Info("-----------------------------------------------------");
        ctxt.mIsRunning = false;
        super.onStop();
    }

    public void continueAppStart(View view) {
//	boolean resync = false;
//	try
//	{
//	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
//	    resync = prefs.getBoolean("boxAutoSync", false);
//	}
//	catch (Exception e)
//	{
//	}

//	if (!resync)
//	{
//	    new ErrorMessage(ctxt, this.getString(R.string.msg_newFunktionAvailable) + ctxt.context.getString(R.string.msg_AutoSync),
//		    new OnClickListener()
//		    {
//
//			@Override
//			public void onClick(DialogInterface dialog, int which)
//			{
//			    // ja, bitte aktivieren
//			    SharedPreferences prefs;
//			    try
//			    {
//				ProfilManager pm = new ProfilManager(ctxt);
//				prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
//				SharedPreferences.Editor editor = prefs.edit();
//				editor.putBoolean("boxAutoSync", true);
//				editor.putString("listResync", "60");
//				editor.apply();
//				ctxt.mProfil.autoSync = true;
//				ctxt.mProfil.myResync = 60;
//				pm.profiles.get(pm.currentProfilIndex).loadPrefs();
//				pm.saveAllProfiles();
//			    }
//			    catch (Exception e)
//			    {
//				_logger.Error("Error loading Preferences", e);
//			    }
//			    Intent service = new Intent(MainActivity.this, AlarmStarter.class);
//			    MainActivity.this.startService(service);
//
//			    Intent intent = new Intent(MainActivity.this, de.janssen.android.gsoplan.activities.PlanActivity.class);
//			    MainActivity.this.startActivity(intent);
//			    MainActivity.this.finish();
//
//			}
//
//		    }, "Ja, bitte!", new OnClickListener()
//		    {
//
//			@Override
//			public void onClick(DialogInterface dialog, int which)
//			{
//			    Intent intent = new Intent(MainActivity.this, de.janssen.android.gsoplan.activities.PlanActivity.class);
//			    MainActivity.this.startActivity(intent);
//			    MainActivity.this.finish();
//			}
//		    }, "Nein, jetzt noch nicht!").run();
//	}
//	else
//	{
        Intent intent = new Intent(MainActivity.this, de.janssen.android.gsoplan.activities.PlanActivity.class);
        MainActivity.this.startActivity(intent);
        MainActivity.this.finish();
//	}

    }


    public void continueAppStart() {
        long startCounter = 0;
        SharedPreferences prefs;
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
            startCounter = prefs.getLong("startCounter", 0);
        } catch (Exception e) {
        }
        if (startCounter == 10) {
            new ErrorMessage(ctxt, this.getString(R.string.msg_StartCounterReached), new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // ja, zum PlayStore
                    Intent intent2 = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=de.janssen.android.gsoplan"));
                    startActivityForResult(intent2, 0);
                }

            }, "Zum Play-Store", new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(MainActivity.this, de.janssen.android.gsoplan.activities.PlanActivity.class);

                    if (extras != null) {
                        intent.putExtras(extras);
                    } else {
                        Intent service = new Intent(MainActivity.this, AlarmStarter.class);
                        MainActivity.this.startService(service);
                    }
                    MainActivity.this.startActivity(intent);

                    MainActivity.this.finish();
                }
            }, "Nein").run();


        } else {
            Intent intent = new Intent(this, de.janssen.android.gsoplan.activities.PlanActivity.class);

            if (extras != null) {
                intent.putExtras(extras);
            } else {
                Intent service = new Intent(MainActivity.this, AlarmStarter.class);
                MainActivity.this.startService(service);
            }
            MainActivity.this.startActivity(intent);

            MainActivity.this.finish();
        }
    }

}
