/*
 * Tools.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */

package de.ribeiro.android.gso.core;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.ribeiro.android.gso.Convert;
import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.activities.AppPreferences;
import de.ribeiro.android.gso.dataclasses.Const;
import de.ribeiro.android.gso.xml.Xml;
import de.ribeiro.android.gso.xml.XmlSearch;

public class Tools {
    private static Logger _logger = new Logger(Const.APPFOLDER, "Tools");

    /**
     * @param ctxt
     * @return
     * @throws Exception
     * @author Tobias Janssen Pr�ft anhand einer Datei, welche Version zuvor
     * installiert war. Liefert false, wenn Versionen �bereinstimmen und
     * true wenn abweichung
     */
    public static Boolean isNewVersion(MyContext ctxt) throws Exception {
        // App Version abfragen
        Context cont = ctxt.context.getApplicationContext();
        PackageInfo pInfo = cont.getPackageManager().getPackageInfo(cont.getPackageName(), 0);
        String currentVersion = pInfo.versionName;

        // zuerst pr�fen, ob versionsdatei vorhanden
        String filename = Const.FILEVERSION;
        File vFile = new File(ctxt.context.getFilesDir(), filename);
        if (!vFile.exists()) {
            // Datei existiert nicht!
            // Neu anlegen

            String fileContent = "<version>" + currentVersion + "</version>";
            fileContent += "<code>" + pInfo.versionCode + "</code>";
            FileOPs.saveToFile(fileContent, vFile);
            return true;
        } else {
            Xml xml = new Xml("root", FileOPs.readFromFile(vFile));
            xml.parseXml();
            XmlSearch xmlSearch = new XmlSearch();
            Xml versionTag = xmlSearch.tagCrawlerFindFirstEntryOf(xml, "version");
            if (versionTag != null && versionTag.getDataContent() != null) {
                if (currentVersion.equalsIgnoreCase(versionTag.getDataContent()))
                    return false;
                else {
                    String fileContent = "<version>" + currentVersion + "</version>";
                    fileContent += "<code>" + pInfo.versionCode + "</code>";
                    FileOPs.saveToFile(fileContent, vFile);
                    return true;
                }

            } else {
                String fileContent = "<version>" + currentVersion + "</version>";
                fileContent += "<code>" + pInfo.versionCode + "</code>";
                FileOPs.saveToFile(fileContent, vFile);
                return true;
            }
        }

    }

    /**
     * @param ctxt
     * @return
     * @throws Exception
     * @author Tobias Janssen Pr�ft anhand einer Datei, welche Version zuvor
     * installiert war. Liefert false, wenn Versionen �bereinstimmen und
     * true wenn abweichung
     */
    public static int getDataVersion(MyContext ctxt) throws Exception {
        // zuerst pr�fen, ob versionsdatei vorhanden
        String filename = Const.FILEVERSION;
        File vFile = new File(ctxt.context.getFilesDir(), filename);
        if (!vFile.exists()) {
            return 0;
        }

        Xml xml = new Xml("root", FileOPs.readFromFile(vFile));
        xml.parseXml();
        XmlSearch xmlSearch = new XmlSearch();
        Xml versionTag = xmlSearch.tagCrawlerFindFirstEntryOf(xml, "code");
        if (versionTag != null && versionTag.getDataContent() != null) {

            String code = versionTag.getDataContent();
            try {
                return Integer.valueOf(code);
            } catch (Exception e) {
                return 0;
            }
        } else {
            return 0;
        }

    }

    /**
     * @param date
     * @return
     * @author Tobias Janssen
     * <p/>
     * Liefert den aktuellen Wochentag. Wochenendtage liefern den
     * n�chsten Montag und setzen das currentDate entsprechend um
     */
    @Deprecated
    public static int getSetCurrentWeekDay(Calendar date) {
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.SATURDAY:
                date.setTimeInMillis(date.getTimeInMillis() + (86400000 * 2));
                return Calendar.MONDAY;

            case Calendar.SUNDAY:
                date.setTimeInMillis(date.getTimeInMillis() + (86400000 * 1));
                return Calendar.MONDAY;

            default:
                return dayOfWeek;

        }
    }

    /**
     * @param calendar
     * @return
     * @author Tobias Janssen
     * <p/>
     * Rechnet das Datum einer PageIndex zu einem Vergleichbaren Wert
     */
    public static int calcIntYearDay(Calendar calendar) {
        return (calendar.get(Calendar.YEAR) * 1000) + calendar.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * @param calendar
     * @return
     * @author Tobias Janssen
     * <p/>
     * Rechnet das Datum einer PageIndex zu einem Vergleichbaren Wert
     */
    public static int calcIntYearWeek(Calendar calendar) {
        return (calendar.get(Calendar.YEAR) * 1000) + calendar.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * @param context
     * @param stupid
     * @throws Exception
     * @author Tobias Janssen L�dt alle verf�gbaren Daten-Datein
     */
    public static void loadAllDataFiles(Context context, Profil mProfil, Stupid stupid) throws Exception {
        try {
            File dir = new File(context.getFilesDir() + "/" + mProfil.myElement);
            File[] files = dir.listFiles();
            int start = 0;
            int end = files.length;
            try {
                // Sortieren, damit die �ltetsten abgeschnitten werden
                files = sortFiles(files);
                int offset = GetStartOffset(files);
                if (mProfil.fastLoad) {
                    // maximal 8 Dateien(Wochen) laden
                    if(end - 8 > offset)
                    {
                        int diff = end - 8 - offset;
                        end = end - diff;
                        start = offset;
                    }
                    else
                    {
                        if(end - 8 > 0)
                            start = end - 8;
                        start = 0;
                    }
                } else {
                    // maximal 20 Dateien(Wochen) laden
                    if(end - 20 > offset)
                    {
                        int diff = end - 20 - offset;
                        end = end - diff;
                        start = offset;
                    }
                    else
                    {
                        if(end - 20 > 0)
                            start = end - 20;
                        start = 0;
                    }
                }

            } catch (Exception e) {
                // Sortieren hat nicht geklappt
                start = 0;
            }

            for (int f = start; f < end; f++) {
                loadNAppendFile(context, stupid, files[f]);
            }
        } catch (Exception e) {
            throw e;
        }

    }

    private static int GetStartOffset(File[] files)
    {
        for (int i = 0; i< files.length;i++)
        {
            String[] file1 = files[i].getName().split("_");
            if (Integer.parseInt(file1[1]) == new GregorianCalendar().get(Calendar.WEEK_OF_YEAR)) {
                return i;
            }
        }
        return 0;
    }

    private static File[] sortFiles(File[] files) {
        List<File> listOut = new ArrayList<File>();
        List<File> listIn = new ArrayList<File>();
        for (int i = 0; i < files.length; i++) {
            listIn.add(files[i]);
        }
        if (listIn.size() == 0)
            return new File[0];

        int location = 0;
        File current = listIn.get(location);
        File next;
        int indexToRemove = 0;
        while (listIn.size() != 0) {

            while (location != listIn.size() - 1) {
                location++;
                next = listIn.get(location);
                if (listIn.size() == 1)
                    listOut.add(next);
                else {
                    String[] file1 = current.getName().split("_");
                    String[] file2 = next.getName().split("_");
                    if (Integer.parseInt(file1[1]) > Integer.parseInt(file2[1])) {
                        current = next;
                        indexToRemove = location;
                    } else if (Integer.parseInt(file1[1]) == Integer.parseInt(file2[1])) {
                        if (Integer.parseInt(file1[0]) > Integer.parseInt(file2[0])) {
                            current = next;
                            indexToRemove = location;
                        }
                    }

                }
            }
            listOut.add(current);
            listIn.remove(indexToRemove);
            location = 0;
            indexToRemove = location;
            if (listIn.size() > 0)
                current = listIn.get(location);

        }
        File[] result = new File[listOut.size()];
        result = listOut.toArray(result);
        return result;
    }

    /**
     * @param context
     * @param stupid
     * @param file
     * @throws Exception
     * @author Tobias Janssen
     * <p/>
     * L�dt den angegebenen File und h�ngt diesen an die Daten im
     * StupidCore an
     */
    public static void loadNAppendFile(Context ctxt, Stupid stupid, File file) throws Exception {

        try {
            WeekData[] weekData = Convert.toWeekDataArray(FileOPs.readFromFile(file));

            if (weekData.length > 0)
                stupid.stupidData.add(weekData[0]);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new Exception("Beim laden der Dateien ist ein Fehler aufgetreten");
        }
    }

    /**
     * @param ctxt
     * @return
     * @author Tobias Janssen
     */
    public static boolean gotoSetup(MyContext ctxt) {

        Intent intent = new Intent(ctxt.activity, AppPreferences.class);
        ctxt.activity.startActivityForResult(intent, 0);
        return true;
    }

    /**
     * @param ctxt
     * @param putExtraName
     * @param value
     * @return
     * @author Tobias Janssen
     */
    public static boolean gotoSetup(MyContext ctxt, String putExtraName, Boolean value) {

        Intent intent = new Intent(ctxt.activity, AppPreferences.class);
        intent.putExtra(putExtraName, value);
        ctxt.activity.startActivityForResult(intent, 0);
        return true;
    }

}
