package de.janssen.android.gsoplan.service;

import java.io.File;

import de.janssen.android.gsoplan.core.UntisProvider;
import de.janssen.android.gsoplan.dataclasses.ProfilManager;

import java.io.SyncFailedException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.janssen.android.gsoplan.Convert;
import de.janssen.android.gsoplan.ICalEvent;
import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.R;
import de.janssen.android.gsoplan.activities.MainActivity;
import de.janssen.android.gsoplan.core.FileOPs;
import de.janssen.android.gsoplan.core.MyContext;
import de.janssen.android.gsoplan.core.Profil;
import de.janssen.android.gsoplan.core.Stupid;
import de.janssen.android.gsoplan.core.WeekData;
import de.janssen.android.gsoplan.dataclasses.Const;
import de.janssen.android.gsoplan.dataclasses.HtmlResponse;
import de.janssen.android.gsoplan.xml.Xml;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class BackgroundSync extends AsyncTask<Boolean, Integer, Boolean> {
    private Stupid stupid;
    private Logger _logger;
    private Runnable postRun;
    private MyContext ctxt;

    private int notificationId = 2;
    private Profil mProfil;
    private Boolean fromFrontend;
    private Boolean quiet;
    private Calendar now = new GregorianCalendar();
    private int index;
    private ProfilManager pm;

    public BackgroundSync(Context ctxt, Runnable run, Boolean fromFrontend, Boolean quiet) {
        this.ctxt = new MyContext(ctxt);
        _logger = new Logger(ctxt, "BackgroundSync");
        this.pm = new ProfilManager(this.ctxt);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        notificationId = prefs.getInt("notificationId", 2);

        this.postRun = run;
        this.fromFrontend = fromFrontend;
        this.quiet = quiet;

    }

    @Override
    protected Boolean doInBackground(Boolean... bool) {

        for (int i = 0; i < pm.profiles.size(); i++) {
            Boolean proceed = false;
            this.mProfil = pm.profiles.get(i);

            if (fromFrontend) {
                //nur das aktuelle profil aktualisieren
                if (i == pm.currentProfilIndex && fromFrontend)
                    proceed = true;
            } else {
                proceed = mProfil.autoSync;
            }

            if (proceed) {

                // pr�fen, ob die synczeit erreicht ist, oder die Anfrage vom
                // Frontend kommt
                try {
                    if (now.getTimeInMillis() - this.mProfil.mylastResync >= this.mProfil.myResync * 1000 * 60 || fromFrontend) {
                        if (fromFrontend && !quiet)
                            notifcateSync(mProfil.myElement);
                        if ((mProfil.onlyWlan && ctxt.isWifiConnected()) || !mProfil.onlyWlan) {
                            try {
                                _logger.Info("---------------------------------");
                                _logger.Info("starting Sync: ");

                                stupid = new Stupid();
                                // die typedaten laden
                                File typesFile = mProfil.getTypesFile(ctxt.context);

                                if (typesFile.exists()) {
                                    // die ElementDatei Laden
                                    try {
                                        Xml xml = new Xml("root", FileOPs.readFromFile(typesFile));
                                        // Tempor�r Daten umwandeln
                                        Stupid temp = new Stupid();
                                        mProfil.types = Convert.toTypesList(xml);
                                        // Wenn das geklappt hat Elements leeren
                                        stupid.clear();
                                        stupid = temp;
                                    } catch (Exception e) {
                                        // Fehler beim Laden der ElementDatei
                                        _logger.Error("Error loading Element File!", e);
                                    }
                                } else {
                                    _logger.Info(Const.FILETYPE + "doesn't exists!");
                                    // nichts vorhanden neu laden und anlegen
                                    UntisProvider.syncTypeList(new HtmlResponse(), mProfil);
                                    // Typen zu XML konvertieren
                                    String xml = Convert.toXml(mProfil.types);
                                    FileOPs.saveToFile(xml, typesFile);
                                }

                                // Synchronisation durchf�hren
                                HtmlResponse html = new HtmlResponse();
                                try {
                                    syncAllDataInBackground(html);
                                } catch (NoSuchFieldException e) {
                                    _logger.Error("Error while syncing Data:", e);
                                    notifcateError(new SyncFailedException(Const.ERROR_NOSUCHFIELD), true);
                                } catch (Exception e) {
                                    _logger.Error("Error while syncing Data:", e);
                                    notifcateError(new SyncFailedException(Const.ERROR_NONET), true);
                                }
                            } catch (NoSuchFieldException e) {
                                _logger.Error(e.getMessage(), e);
                                notifcateError(new SyncFailedException(Const.ERROR_NOSUCHFIELD));
                            } catch (SyncFailedException e) {
                                _logger.Error("Error fetching Element File", e);
                                notifcateError(new SyncFailedException(Const.ERROR_NONET));
                            } catch (Exception e) {
                                _logger.Error("Error loading Element File: doesn't exist");
                            }
                        } else {
                            // Keine WIFI-Verbindung
                            this.notifcateError(new SyncFailedException(ctxt.context.getString(R.string.msg_noWlan)), true);
                        }
                    }
                } catch (Exception e) {
                    _logger.Error("Sync for profile no: " + i + " has been terminated with errors", e);
                }
                _logger.Info("Removing sync Notification");
                NotificationManager nm = (NotificationManager) ctxt.context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(1);
            }
            if (i == pm.currentProfilIndex && fromFrontend)
                break;
        }
        pm.saveAllProfiles();
        if (this.postRun != null)
            this.postRun.run();
        return null;
    }

    private String getDayNameOfDayofWeek(int value) {
        if (value == 1)
            return "So";
        if (value == 2)
            return "Mo";
        if (value == 3)
            return "Di";
        if (value == 4)
            return "Mi";
        if (value == 5)
            return "Do";
        if (value == 6)
            return "Fr";
        if (value == 7)
            return "Sa";
        return "";
    }

    private String getStringOfInt(int value) {
        if (value == 1)
            return "einer";
        if (value == 2)
            return "zwei";
        if (value == 3)
            return "drei";
        if (value == 4)
            return "vier";
        if (value == 5)
            return "fünf";
        if (value == 6)
            return "sechs";
        if (value == 7)
            return "sieben";
        if (value == 8)
            return "acht";
        if (value == 9)
            return "neun";
        return "";
    }

    private String getTextNotification(ICalEvent event) {
        int actualweek = new GregorianCalendar().get(Calendar.WEEK_OF_YEAR);
        int actualday = new GregorianCalendar().get(Calendar.DAY_OF_WEEK);
        int eventsweek = event.DTSTART.get(Calendar.WEEK_OF_YEAR);
        int eventsday = event.DTSTART.get(Calendar.DAY_OF_WEEK);


        String result = "";
        if (eventsweek == actualweek) {
            result += getDayNameOfDayofWeek(eventsday);
        }
        int diff = eventsweek - actualweek;
        if (diff == 1) {
            result += "Nächste Woche ";
            result += getDayNameOfDayofWeek(eventsday);
        }
        if (diff > 1) {
            result += getDayNameOfDayofWeek(eventsday);
            result += " in " + getStringOfInt(diff) + " Wochen";
        }
        String end = " enthält Änderung";

        result += end;
        return result;

    }

    private void syncAllDataInBackground(HtmlResponse htmlResponse) throws Exception, NoSuchFieldException {

        // Bedingungen pr�fen
        // Element muss gew�hlt sein
        if (!mProfil.myElement.isEmpty()) {

            // die Elemente/Wochen herunterladen
            try {
                _logger.Info("syncing Elements");
                UntisProvider.syncTypeList(htmlResponse, mProfil);

            } catch (SyncFailedException e) {
                _logger.Error("Error fetching Type List from Server", e);
                notifcateError(new SyncFailedException(Const.ERROR_NONET), true);
            } catch (Exception e) {
                _logger.Error("Error fetching Type List from Server", e);
                throw e;
            }

            // Alle online verf�gbaren Wochen aus dem lokalen Datenbestand laden
            try {
                stupid.loadAllFutureDataFiles(mProfil, ctxt.context);
            } catch (Exception e) {
                _logger.Error("Error while reading data files", e);
            }
            _logger.Info("syncing WeekData");
            // jede verf�gbare Woche herunterladen

            // verf�gbare Wochen abrufen
            // dies muss getestet werden.
            // also Wochen hochz�hlen bis kein Stundenplan mehr kommt

            try {
                GregorianCalendar gc = new GregorianCalendar();
                List<ICalEvent> changes = new ArrayList<ICalEvent>();
                int counter = 0;
                while (counter < 4) {
                    htmlResponse = new HtmlResponse();

                    List<ICalEvent> result = UntisProvider.syncWeekData(gc, mProfil.getMyElement(), mProfil.currType(), htmlResponse, stupid);
                    gc.add(Calendar.WEEK_OF_YEAR, 1);
                    if (result == null)
                        counter++;
                    else
                        changes.addAll(result);
                }
                if (!changes.isEmpty()) {
                    if(changes.size() > 1)
                    {
                        if (this.mProfil.notificate) {
                            notifcate(mProfil.myElement + " Es gibt mehrere Änderungen!", index, changes.get(0).DTSTART, MainActivity.class, true);
                        }
                    }
                    else {
                            // notification ausf�hren
                            String text = getTextNotification(changes.get(0));
                            _logger.Info(text);
                            if (this.mProfil.notificate) {
                                notifcate(mProfil.myElement + " " + text, index, changes.get(0).DTSTART, MainActivity.class, true);
                            }
                    }
                }
            } catch (Exception e) {
                _logger.Error("Error in Backgroundsync", e);
                throw e;
            }

            mProfil.mylastResync = now.getTimeInMillis();

            // type speichern
            String xmlContent;

            _logger.Info("Saving Types");
            try {
                xmlContent = Convert.toXml(mProfil.types);
                File typesFile = ctxt.mProfil.getTypesFile(ctxt.context);
                FileOPs.saveToFile(xmlContent, typesFile);
                ctxt.mProfil.isDirty = false;
            } catch (Exception e) {
                throw e;
            }

            // daten speichern
            WeekData wd;
            File dataFile;
            _logger.Info("Saving WeekData");
            for (int i = 0; i < stupid.stupidData.size(); i++) {

                try {
                    wd = stupid.stupidData.get(i);
                    dataFile = stupid.getFileSaveData(ctxt.context, wd);
                    xmlContent = Convert.toXml(wd);
                    FileOPs.saveToFile(xmlContent, dataFile);
                    wd.isDirty = false;
                } catch (Exception e) {
                    _logger.Error("Error while saving data after backgroundsync", e);
                    throw e;
                }

            }
        } else {
            _logger.Info("MyElement is empty!");
            // die Elemente/Wochen herunterladen
            try {

                _logger.Info("syncing all Elements..");
                UntisProvider.syncTypeList(htmlResponse, mProfil);

            } catch (Exception e) {
                throw e;
            }
        }
    }


    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private void notifcate(String message, int profilIndex, Calendar date, Class<?> cls, Boolean vibrate) {
        try {
            CharSequence text = message;
            CharSequence name = ctxt.context.getString(R.string.app_name);
            Vibrator v = (Vibrator) ctxt.context.getSystemService(Context.VIBRATOR_SERVICE);
            if (notificationId + 1 == Integer.MAX_VALUE)
                notificationId = 1;
            else
                notificationId++;

            Notification notification;
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                // Startet die MainActivity
                _logger.Info("Creating deprecated Error Notification for Android SDK: " + android.os.Build.VERSION.SDK_INT);
                notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
                Intent intent = new Intent(ctxt.context, cls);

                intent.putExtra("notificationId", notificationId);
                intent.putExtra("date", date.getTimeInMillis());
                intent.putExtra("profilIndex", profilIndex);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("notificationId", notificationId);
                editor.apply();

                PendingIntent contentIntent = PendingIntent.getActivity(ctxt.context, notificationId, intent, 0);
                notification.setLatestEventInfo(ctxt.context, name, text, contentIntent);
            } else {
                // f�r android api16 & gr��er
                _logger.Info("Creating Error Notification for Android SDK: " + android.os.Build.VERSION.SDK_INT);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctxt.context.getApplicationContext())
                        .setSmallIcon(R.drawable.ic_launcher).setContentTitle(name).setContentText(text).setTicker(text).setAutoCancel(true);
                Intent intent = new Intent(ctxt.context, cls);

                intent.putExtra("notificationId", notificationId);
                intent.putExtra("date", date.getTimeInMillis());
                intent.putExtra("profilIndex", profilIndex);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt.context);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("notificationId", notificationId);
                editor.apply();


                TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctxt.context.getApplicationContext());

                stackBuilder.addParentStack(cls);

                stackBuilder.addNextIntent(intent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);

                notification = mBuilder.getNotification();
            }

            if (vibrate && mProfil.vibrate)
                v.vibrate(new long[]{200, 400}, -1);
            NotificationManager nm = (NotificationManager) ctxt.context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && notification != null)
                nm.notify(notificationId, notification);

            // Klingelton
            if (mProfil.sound) {
                try {
                    Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(ctxt.context, uri);
                    r.play();
                } catch (Exception e) {
                    _logger.Error("Error in Notification! Cannot play Ringtone", e);
                }
            }
        } catch (Exception e) {
            _logger.Error("Cannot create Notification!", e);

        }
        try {
            java.lang.Thread.sleep(5000);
        } catch (InterruptedException e) {
            _logger.Error("Error in Notification! Wait interrupted", e);
        }

        // nm.cancel(0);
    }

    /**
     * Zeigt eine Meldung in der NotificationBar
     *
     * @param message String der angezeigt wird
     * @param cls     Klasse, die aufgerufen werden soll
     * @param vibrate true um Vibration auszul�sen
     */
    @SuppressWarnings("deprecation")
    private void notifcateSync(String element) {
        try {

            NotificationManager nm = (NotificationManager) ctxt.context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                _logger.Info("Creating deprecated sync Notification for Android SDK: " + android.os.Build.VERSION.SDK_INT);
                Notification notification = new Notification(android.R.drawable.stat_notify_sync, Const.NOTIFICATESYNCHEAD
                        + Const.NOTIFICATESYNCSHORT, System.currentTimeMillis());

                PendingIntent contentIntent = PendingIntent.getActivity(ctxt.context.getApplicationContext(), 0, new Intent(), 0);
                notification.setLatestEventInfo(ctxt.context.getApplicationContext(), element, Const.NOTIFICATESYNCSHORT, contentIntent);

                nm.notify(1, notification);
            } else {
                _logger.Info("Creating sync Notification for Android SDK: " + android.os.Build.VERSION.SDK_INT);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctxt.context.getApplicationContext())
                        .setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle(element)
                        .setTicker(element + " " + Const.NOTIFICATESYNCSHORT).setContentText(Const.NOTIFICATESYNCSHORT);

                nm.notify(1, mBuilder.getNotification());
            }

        } catch (Exception e) {
            _logger.Error("Cannot create Notification!", e);
        }

    }

    /**
     * Zeigt eine FehlerMeldung in der NotificationBar
     *
     * @param exception
     */
    private void notifcateError(SyncFailedException exception, Boolean cancel) {
        try {

            NotificationManager nm = (NotificationManager) ctxt.context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification;

            if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                _logger.Info("Creating deprecated Error Notification for Android SDK: " + android.os.Build.VERSION.SDK_INT);
                notification = new Notification(android.R.drawable.stat_notify_error, exception.getMessage(), System.currentTimeMillis());

                PendingIntent contentIntent = PendingIntent.getActivity(ctxt.context.getApplicationContext(), 0, new Intent(), 0);
                notification
                        .setLatestEventInfo(ctxt.context.getApplicationContext(), Const.NOTIFICATESYNCHEAD, exception.getMessage(), contentIntent);

                nm.notify(1, notification);
            } else {
                _logger.Info("Creating Error Notification for Android SDK: " + android.os.Build.VERSION.SDK_INT);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctxt.context.getApplicationContext())
                        .setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle(Const.NOTIFICATESYNCHEAD)
                        .setContentText(exception.getMessage()).setTicker(exception.getMessage()).setAutoCancel(true);

                notification = mBuilder.getNotification();
            }

            if (notification != null)
                nm.notify(999, notification);
            if (cancel)
                nm.cancel(999);

        } catch (Exception e) {
            _logger.Error("Cannot create Notification!", e);
        }

    }

    private void notifcateError(SyncFailedException exception) {
        notifcateError(exception, false);
    }

}
