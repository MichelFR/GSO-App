package de.janssen.android.gsoplan.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import de.janssen.android.gsoplan.ICalEvent;
import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.core.FileOPs;
import de.janssen.android.gsoplan.core.MyContext;
import de.janssen.android.gsoplan.core.Tools;
import de.janssen.android.gsoplan.core.WeekData;
import de.janssen.android.gsoplan.dataclasses.Const;

public class UnMuteService extends IntentService {
    private Logger _logger;

    public UnMuteService() {
        super("ServiceConnector");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        _logger = new Logger(Const.APPFOLDER, "UnMuteService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _logger = new Logger(Const.APPFOLDER, "UnMuteService");
        _logger.Info("Starting UnMuteService");
        try {
            String content = loadLockFile();
            if (!content.equalsIgnoreCase("")) {
                String rm = content.substring(0, content.indexOf(";"));
                int ringerMode = Integer.valueOf(rm);
                // auf den alten ringemode zur�cksetzten
                if (ringerMode != -1) {
                    unMute(ringerMode);

                } else {
                    _logger.Info("Lock file is invalid!");
                    unMute(2);
                }
                File lockfile = new File(Const.APPFOLDER, "muteService.lock");
                if (lockfile.exists())
                    lockfile.delete();
            } else {
                _logger.Info("No lock file found");
            }
        } catch (Exception e) {
            _logger.Error("mutService.lock is currupted. Deleting", e);
            unMute(2);
            File lockfile = new File(Const.APPFOLDER, "muteService.lock");
            if (lockfile.exists())
                lockfile.delete();
        }

        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        _logger.Trace("MuteService is getting destroyed!");
        super.onDestroy();
    }

    public void setNextStartEvent() {
        Intent startworker = new Intent(this, MuteService.class);
        PendingIntent startintent = PendingIntent.getService(this, 0, startworker, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        MyContext ctxt = new MyContext(this);

        try {
            ctxt.mProfil.stupid.clearData();
            Tools.loadAllDataFiles(ctxt.context, ctxt.mProfil, ctxt.mProfil.stupid);
        } catch (Exception e) {
            _logger.Error("Error while setting nextMuteEvent", e);
        }
        long startalarmDate = -1;
        for (WeekData wd : ctxt.mProfil.stupid.stupidData) {
            for (ICalEvent ev : wd.events) {
                long evtime = ev.DTSTART.getTimeInMillis();
                if ((startalarmDate == -1 || startalarmDate > evtime) && evtime > System.currentTimeMillis()) {
                    startalarmDate = evtime;
                }
            }
        }
        if (startalarmDate == -1) {
            _logger.Error("No Start Time found. quitting");
            return;
        }
        _logger.Info("Setting next Event for " + ctxt.mProfil.myElement + " to " + new Date(startalarmDate));

        alarm.set(AlarmManager.RTC_WAKEUP, startalarmDate, startintent);
    }

    private String loadLockFile() throws Exception {

        File lockfile = new File(Const.APPFOLDER, "muteService.lock");
        if (lockfile.exists()) {
            return FileOPs.readFromFile(lockfile);
        }
        return "";
    }

    private void unMute(int old) {
        AudioManager amanager = (AudioManager) getSystemService(AUDIO_SERVICE);
        // turn ringer back
        amanager.setRingerMode(old);
        _logger.Info("Setting phone to previous mode");
    }

}
