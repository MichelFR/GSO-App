/*
 * Const.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */

package de.ribeiro.android.gso.dataclasses;

import java.io.File;

public class Const {
    public static final int THISWEEK = 0;
    public static final int NEXTWEEK = 1;
    public static final int LASTWEEK = -1;
    public static final int SELECTEDWEEK = 2;
    public static final int TEXTSIZEOFHEADLINES = 12;
    public static final Boolean FORCEREFRESH = true;
    public final static String FILEPROFILES = "Profiles.xml";
    public final static String FILETYPE = "Type.xml";
    public final static String FILELOG = "log.txt";
    public final static String FILEDATA = "Data.xml";
    public final static String FILEVERSION = "Version.xml";
    public final static String FIRSTSTART = "FirstStart";
    public final static int CONNECTIONTIMEOUT = 5000;
    public static final String XMLVERSION = "1";
    public final static String CHECKBOXPROFILID = "checkboxUseFav";
    public static final String NAVBARURL = "https://webuntis.stadt-koeln.de/WebUntis/Timetable.do";
    public static final String URLSTUPID = "http://stupid.gso-koeln.de/";
    public static final String BROADCASTREFRESH = "broadcast_refresh";
    public static final String NOTIFICATESYNC = "aktualisiert Stundenpläne";
    public static final String NOTIFICATESYNCHEAD = "GSOPlan";
    public static final String NOTIFICATESYNCSHORT = " wird synchronisiert";
    public static final String[] DEVEMAIL = new String[]{"tobi.ribeiro@gmx.de"};
    public static final String EXPORTLOGINTENT = "Log Datei exportieren nach...";
    public static final String EXPORTLOGBUTTONWAITTEXT = "Komprimiere Log Datei...";
    public static final String EXPORTLOGEMAILSUBJECT = "GSOPlan Log Datei";
    public static final String EXPORTLOGEMAILBODY = "Log Datei ist im Anhang!";
    public final static String ERROR_XMLFAILURE = "Fehler bei der XML Konvertierung!";
    public final static String ERROR_NOSERVER = "Es konnte keine Verbindung zum Server hergestellt werden!";
    public final static String ERROR_NONET = "Fehler beim Verbindungsaufbau!";
    public final static String ERROR_NOSUCHFIELD = "Fehler bei der Datenverarbeitung!\ngso-koeln.de lieferte das falsche Element.\nIst die Stupid-Website korrekt?";
    public final static String ERROR_CONNTIMEOUT = "Verbindungs-Timeout! Server nicht erreichbar!";
    public final static String ERROR_NOTIMETABLE_FOR_REFRESH = "Es existiert noch kein Stundenplan, der Aktualisiert werden kann!\nBitte kontrollieren Sie die Internetverbindung und anschlie�end die Einstellungen!";
    public static File APPFOLDER;
}
