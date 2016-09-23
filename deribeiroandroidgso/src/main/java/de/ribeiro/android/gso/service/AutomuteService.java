package de.ribeiro.android.gso.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import de.ribeiro.android.gso.ICalEvent;
import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.core.MyContext;
import de.ribeiro.android.gso.core.Tools;
import de.ribeiro.android.gso.core.WeekData;
import de.ribeiro.android.gso.dataclasses.Const;

public class AutomuteService extends IntentService {
    private Logger _logger;

    public AutomuteService() {
        super("ServiceConnector");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _logger = new Logger(Const.APPFOLDER, "AutoMuteService");
        _logger.Info("Starting AutoMuteService");
        List<ICalEvent> todayEvents = GetEventsOfDay();
        List<ICalEvent> realevents = GetRealEventsOfDay(todayEvents);
        if (realevents.isEmpty())
            _logger.Info("No mute events today!");

        for (ICalEvent ev : realevents) {
            setMuteEvent(ev.DTSTART.getTime(), String.valueOf(ev.DTSTART.getTimeInMillis()));
            setUnMuteEvent(ev.DTEND.getTime(), String.valueOf(ev.DTEND.getTimeInMillis()));
        }
        Calendar tomorrow = new GregorianCalendar();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 1);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);

//	tomorrow.add(Calendar.MINUTE, 5);
//	tomorrow.set(Calendar.MILLISECOND, 0);
        setAutoMuteEvent(tomorrow.getTime(), String.valueOf(tomorrow.get(Calendar.DAY_OF_YEAR)));
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private List<ICalEvent> GetRealEventsOfDay(List<ICalEvent> todayEvents) {
        List<ICalEvent> realevents = new ArrayList<ICalEvent>();
        while (!todayEvents.isEmpty()) {
            ICalEvent event = new ICalEvent();
            // den fr�hesten start des tages heraussuchen
            for (ICalEvent ev : todayEvents) {
                if (event.DTSTART == null) {
                    event.DTSTART = ev.DTSTART;
                    event.DTEND = ev.DTEND;
                }
                if (event.DTSTART.after(ev.DTSTART))
                    event.DTSTART = ev.DTSTART;
            }
            // solange events entfernen, bis die differenz zwischen ende und
            // start gr��er 5 minuten ist
            event = Test(event, todayEvents);
            realevents.add(event);
        }
        return realevents;
    }

    private ICalEvent Test(ICalEvent event, List<ICalEvent> todayEvents) {
        for (ICalEvent ev : todayEvents) {
            if (event.DTEND.compareTo(ev.DTSTART) == 0) {
                event.DTEND = ev.DTEND;
                todayEvents.remove(ev);
                return Test(event, todayEvents);
            } else {
                if (event.DTSTART.compareTo(ev.DTSTART) == 0) {
                    todayEvents.remove(ev);
                    return Test(event, todayEvents);
                } else {
                    if (todayEvents.size() == 1)
                        todayEvents.clear();
                    return event;
                }
            }
        }
        return event;
    }

    private List<ICalEvent> GetEventsOfDay() {
        MyContext ctxt = new MyContext(this);
        try {
            ctxt.mProfil.stupid.clearData();
            Tools.loadAllDataFiles(ctxt.context, ctxt.mProfil, ctxt.mProfil.stupid);
        } catch (Exception e) {
            _logger.Error("Error while setting nextMuteEvent", e);
        }
        List<ICalEvent> events = new ArrayList<ICalEvent>();
        for (WeekData wd : ctxt.mProfil.stupid.stupidData) {
            for (ICalEvent ev : wd.events) {
                long evtime = ev.DTSTART.getTimeInMillis();
                Calendar cal = new GregorianCalendar();
                Calendar now = new GregorianCalendar();
                cal.setTimeInMillis(evtime);
                if (now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) && now.getTimeInMillis() < cal.getTimeInMillis()) {
                    events.add(ev);
                }
            }
        }
        return events;
    }

    private void setMuteEvent(Date date, String id) {
        Intent worker = new Intent(id, null, this, MuteService.class);
        PendingIntent startintent = PendingIntent.getService(this, 0, worker, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        MyContext ctxt = new MyContext(this);
        _logger.Info("Setting up Mute Event for " + ctxt.mProfil.myElement + " to " + date);

        alarm.set(AlarmManager.RTC_WAKEUP, date.getTime(), startintent);
    }

    private void setUnMuteEvent(Date date, String id) {
        Intent worker = new Intent(id, null, this, UnMuteService.class);
        PendingIntent intent = PendingIntent.getService(this, 0, worker, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        MyContext ctxt = new MyContext(this);
        _logger.Info("Setting up Un-Mute Event for " + ctxt.mProfil.myElement + " to " + date);

        alarm.set(AlarmManager.RTC_WAKEUP, date.getTime() - 30000, intent);
    }

    private void setAutoMuteEvent(Date date, String id) {
        Intent worker = new Intent(id, null, this, AutomuteService.class);
        PendingIntent intent = PendingIntent.getService(this, 0, worker, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        _logger.Info("Setting up AutoMute-Service Event to " + date);
        alarm.set(AlarmManager.RTC_WAKEUP, date.getTime(), intent);
    }
}
