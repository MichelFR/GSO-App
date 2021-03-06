package de.ribeiro.android.gso.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.core.MyContext;
import de.ribeiro.android.gso.dataclasses.Const;
import de.ribeiro.android.gso.dataclasses.ProfilManager;

public class AlarmStarter extends Service {
    private Logger _logger;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _logger = new Logger(Const.APPFOLDER, "AlarmStarter");
        Intent worker = new Intent(this, MyService.class);

        PendingIntent pintent = PendingIntent.getService(this, 0, worker, 0);
        Calendar cal = new GregorianCalendar();
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long resync = 60; // default Alle 60 Minuten
        Boolean sync = false;
        MyContext ctxt = new MyContext(this);

        // das Profil heraussuchen, das den k�rzestsen resync hat
        // und pr�fen, ob �berhaupt gesynct werden soll
        ProfilManager pm = new ProfilManager(ctxt);
        for (int i = 0; i < pm.profiles.size(); i++) {
            if (pm.profiles.get(i).myResync < resync)
                resync = pm.profiles.get(i).myResync;
            if (pm.profiles.get(i).autoSync)
                sync = true;
        }

//	if (ctxt.mProfil.muteEvents)
//	{
//	    _logger.Info("Automute is activated");
//	    Intent automuteService = new Intent(this, AutomuteService.class);
//	    this.startService(automuteService);
//	}
        // den Resync um 2 Minuten verl�ngern
        resync += 2;
        _logger.Info("Setting next resync in " + resync + " Minutes");
        if (sync)
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), resync * 60 * 1000, pintent);
        else
            alarm.cancel(pintent);

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
