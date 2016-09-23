/*
 * StupidOps.java
 * 
 * Tobias Janssen, 2013
 * GNU GENERAL PUBLIC LICENSE Version 2
 */

package de.ribeiro.android.gso.core;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.os.Messenger;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.SyncFailedException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.ribeiro.android.gso.ArrayOperations;
import de.ribeiro.android.gso.ICalEvent;
import de.ribeiro.android.gso.Logger;
import de.ribeiro.android.gso.dataclasses.Const;
import de.ribeiro.android.gso.dataclasses.HtmlResponse;
import de.ribeiro.android.gso.dataclasses.Lesson;
import de.ribeiro.android.gso.dataclasses.SelectOptions;
import de.ribeiro.android.gso.service.HttpSession;
import de.ribeiro.android.gso.service.MyService;
import de.ribeiro.android.gso.xml.Ical;
import de.ribeiro.android.gso.xml.Xml;
import de.ribeiro.android.gso.xml.XmlOPs;
import de.ribeiro.android.gso.xml.XmlSearch;

public class UntisProvider {
    private static Logger _logger = new Logger(Const.APPFOLDER, "UntisProvider");

    private static JSONArray GetJSON(String htmlresponse) throws Exception {
        JSONArray result = null;
        JSONObject jsonObj = new JSONObject(htmlresponse);
        return jsonObj.getJSONArray("elements");
    }

    private static boolean compareEvents(ICalEvent existing, ICalEvent newEvent) {
        if ((existing == null && newEvent != null) || (existing != null && newEvent == null))
            return true;
        if (!existing.DESCRIPTION.equalsIgnoreCase(newEvent.DESCRIPTION))
            return true;
        if (!existing.LOCATION.equalsIgnoreCase(newEvent.LOCATION))
            return true;
        return !existing.SUMMARY.equalsIgnoreCase(newEvent.SUMMARY);

    }

    private static List<SelectOptions> ConvertJSonArray(JSONArray value) throws Exception {
        List<SelectOptions> tempTypeList = new ArrayList<SelectOptions>();
        try {
            for (int i = 0; i < value.length(); i++) {
                JSONObject jsonObject = value.getJSONObject(i);
                String id = jsonObject.getString("id");
                String type = jsonObject.getString("name");

                tempTypeList.add(new SelectOptions(id, type));
                // _logger.Info("Parsing JSONOnject Found: " + id + "," + type);
            }
        } catch (Exception e) {
            throw e;
        }
        return tempTypeList;
    }

    private static void AddTypeToProfil(Profil mProfil, Type type) {

        // prfen, ob es dieses type schon gibt
        Boolean found = false;
        for (int z = 0; z < mProfil.types.list.size() && !found; z++) {
            if (mProfil.types.list.get(z).type.equalsIgnoreCase(type.type)) {
                // ja, bereinstimmung gefunden
                mProfil.types.list.set(z, type);
                found = true;
            }

        }

        if (!found) {
            mProfil.types.list.add(type);
        }

    }

    /**
     * Ldt die Selectoren(wochen/elemente/typen) von der GSO Seite und parsed
     * diese in den StupidCore
     *
     * @param logger
     * @param htmlResponse
     * @param getProfil    ()
     * @throws Exception
     * @author Tobias Janssen
     */
    public static void syncTypeList(HtmlResponse htmlResponse, Profil mProfil) throws Exception {

        JSONArray clsarray = null;
        JSONArray teacherarray = null;
        JSONArray roomarray = null;
        GregorianCalendar date = new GregorianCalendar();
        // session erzeugen und cookies abholen
        //TODO: Remove hardcoded URL
        String url = "https://webuntis.stadt-koeln.de/WebUntis/Timetable.do?request.preventCache=" + date.getTimeInMillis() + "&school=K175055";
        HttpSession session = new HttpSession(url + 1);
        //5 Wochen in die Zukunft schauen
        for (int i = 0; i < 5; i++) {
            try {
                if (clsarray == null && (clsarray = RequestUrl(session, url, "1")) == null) {
                    date.setTimeInMillis(date.getTimeInMillis() + (1000 * 60 * 60 * 24 * 7));
                    session.SetStupidServerDate(date, i + 1);
                    continue;
                }

                if (teacherarray == null && (teacherarray = RequestUrl(session, url, "2")) == null) {
                    date.setTimeInMillis(date.getTimeInMillis() + (1000 * 60 * 60 * 24 * 7));
                    session.SetStupidServerDate(date, i + 1);
                    continue;
                }

                if (roomarray == null && (roomarray = RequestUrl(session, url, "4")) == null) {
                    date.setTimeInMillis(date.getTimeInMillis() + (1000 * 60 * 60 * 24 * 7));
                    session.SetStupidServerDate(date, i + 1);
                    continue;
                }
                session.Close();
                _logger.Info("new Elements downloaded!");
                mProfil.types.htmlModDate = htmlResponse.lastModified;
                break;
            } catch (Exception e) {
                throw new SyncFailedException(e.getMessage());
            }
        }
        Type type = new Type();
        type.elementList = ConvertJSonArray(clsarray);
        type.typeName = "Klassen";
        type.type = "1";
        AddTypeToProfil(mProfil, type);

        type = new Type();
        type.elementList = ConvertJSonArray(teacherarray);
        type.typeName = "Lehrer";
        type.type = "2";
        AddTypeToProfil(mProfil, type);

        type = new Type();
        type.elementList = ConvertJSonArray(roomarray);
        type.typeName = "Räume";
        type.type = "4";
        AddTypeToProfil(mProfil, type);

        mProfil.isDirty = true;
    }

    private static JSONArray RequestUrl(HttpSession session, String url, String type) throws Exception {

        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", " application/x-www-form-urlencoded");
        post.setHeader("Accept", "application/json");
        post.setHeader("X-Requested-With", "XMLHttpRequest");
        post.setHeader("Referer", "https://webuntis.stadt-koeln.de/WebUntis/?school=K175055#Timetable?type=1&filter=-2");
        post.setHeader("Accept-Language", "de-DE,en-US;q=0.5");
        post.setHeader("Accept-Encoding", "gzip, deflate");
        post.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36");
        post.setHeader("Accept-Encoding", "gzip, deflate");
        post.setHeader("Host", "webuntis.stadt-koeln.de");
        post.setHeader("DNT", "1");
        post.setHeader("Connection", "Keep-Alive");
        post.setHeader("Cache-Control", "no-cache");


        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
        nameValuePairs.add(new BasicNameValuePair("ajaxCommand", "getPageConfig"));
        nameValuePairs.add(new BasicNameValuePair("type", type));
        nameValuePairs.add(new BasicNameValuePair("filter", "-2"));
        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        String response = session.PostRequest(url, post);
        if (response != null)
            return GetJSON(response);
        return null;
    }

    private static String PadLeft(String value, int length, char filler) {
        String result = value;

        for (int i = result.length(); i < length; i++) {
            result = filler + result;
        }
        return result;
    }

    private static List<Lesson> GetSchulstunden() {
        // local time
        List<Lesson> schulstunden = new ArrayList<Lesson>();
        schulstunden.add(new Lesson(7, 45, 8, 30));
        schulstunden.add(new Lesson(8, 30, 9, 15));
        schulstunden.add(new Lesson(9, 35, 10, 20));
        schulstunden.add(new Lesson(10, 20, 11, 5));
        schulstunden.add(new Lesson(11, 25, 12, 10));
        schulstunden.add(new Lesson(12, 10, 12, 55));
        schulstunden.add(new Lesson(13, 15, 14, 0));
        schulstunden.add(new Lesson(14, 0, 14, 45));
        schulstunden.add(new Lesson(15, 5, 15, 50));
        schulstunden.add(new Lesson(15, 50, 16, 35));
        schulstunden.add(new Lesson(16, 55, 17, 40));
        schulstunden.add(new Lesson(17, 40, 18, 25));
        schulstunden.add(new Lesson(18, 25, 19, 10));
        schulstunden.add(new Lesson(19, 30, 20, 15));
        schulstunden.add(new Lesson(20, 15, 21, 0));
        return schulstunden;
    }

    private static ICalEvent GetSchulstunde(WeekData wd, int day, int hour) {
        for (ICalEvent event : wd.events) {
            if (event.DTSTART.get(Calendar.DAY_OF_WEEK) == day) {
                if (GetSchulstundeOfEvent(event) == hour)
                    return event;
            }
        }
        return null;
    }

    private static int GetSchulstundeOfEvent(ICalEvent event) {
        List<Lesson> schulstunden = GetSchulstunden();
        for (int std = 0; std < schulstunden.size(); std++) {
            // prfen, ob anfang und ende innerhalb von zwei schulstunden liegt

            if (event.DTSTART.get(Calendar.HOUR_OF_DAY) >= schulstunden.get(std).Start.hour
                    && event.DTSTART.get(Calendar.HOUR_OF_DAY) <= schulstunden.get(std).End.hour) {
                // wenn alles innerhalb einer stunde liegt muss noch die minute
                // verglichen werden
                if (event.DTSTART.get(Calendar.HOUR_OF_DAY) == schulstunden.get(std).End.hour) {
                    if (event.DTSTART.get(Calendar.MINUTE) >= schulstunden.get(std).Start.minute
                            && event.DTSTART.get(Calendar.MINUTE) <= schulstunden.get(std).End.minute) {
                        return std;
                    }
                } else {
                    return std;
                }
            }
        }
        return -1;
    }

    private static String[] ConvertToStupidDateArray(GregorianCalendar gc) {
        ArrayList<String> al = new ArrayList<String>();
        al.add(String.valueOf(gc.get(Calendar.YEAR)));
        al.add(PadLeft(String.valueOf(gc.get(Calendar.MONTH) + 1), 2, '0'));
        al.add(PadLeft(String.valueOf(gc.get(Calendar.DAY_OF_MONTH)), 2, '0'));

        String[] result = new String[al.size()];
        int i = 0;
        for (String el : al) {
            result[i] = el;
            i++;
        }

        return result;
    }

    private static String ConvertToStupidDateString(GregorianCalendar gc) {
        String[] date = ConvertToStupidDateArray(gc);
        return String.valueOf(date[0] + "-" + date[1] + "-" + date[2]);
    }

    /**
     * Synchronisiert alle verfgbaren WeekDatas im Stupid
     *
     * @param logger
     * @param selectedStringDate
     * @param selectedElement
     * @param myType
     * @param htmlResponse
     * @param stupid
     * @return
     * @throws Exception
     */
    public static List<ICalEvent> syncWeekData(GregorianCalendar gc, String selectedElement, Type myType, HtmlResponse htmlResponse, Stupid stupid)
            throws Exception {


        List<ICalEvent> result = new ArrayList<ICalEvent>();

        int currentDayOfWeek = gc.get(Calendar.DAY_OF_WEEK);
        // den currentDay auf den folge Montag setzen
        if (currentDayOfWeek < 2) {
            // 1000*60*60*24 = 1 Tag!
            gc.setTimeInMillis(gc.getTimeInMillis() + (1000 * 60 * 60 * 24 * (2 - currentDayOfWeek)));
        }
        if (currentDayOfWeek > 6) {
            // 1000*60*60*24 = 1 Tag!
            gc.setTimeInMillis(gc.getTimeInMillis() + (1000 * 60 * 60 * 24 * 2));
        }

        String date = ConvertToStupidDateString(gc);

        String selectedType = myType.type;
        String selectedClassIndex = getIndexOfSelectorValue(myType.elementList, selectedElement);
        if (selectedClassIndex == "-1" || selectedType.equalsIgnoreCase("")) {
            throw new Exception(selectedElement + " kann nicht synchronisiert werden! " + selectedElement
                    + " wurde nicht in der Liste der verfügbaren Elemente gefunden!");
        }
        while (selectedClassIndex.length() < 3) {
            selectedClassIndex = "0" + selectedClassIndex;
        }

        WeekData weekData = new WeekData(stupid);
        try {

            // URL setzten
            URL url = new URL("https://webuntis.stadt-koeln.de/WebUntis/Ical.do?school=K175055&ajaxCommand=renderTimetable&rpt_sd=" + date + "&type="
                    + 1 + "&elemId=" + selectedClassIndex + "&elemType=" + selectedType);

            htmlResponse.dataReceived = false;

            htmlResponse.xmlContent = XmlOPs.readFromURL(url, Const.CONNECTIONTIMEOUT);

            if (htmlResponse.xmlContent.length() < 50) {
                // ende
                return null;
            }

            _logger.Info("Week downloaded!");
            List<ICalEvent> events = Ical.Parse(htmlResponse.xmlContent);

            weekData.syncTime = new GregorianCalendar().getTimeInMillis();
            weekData.addParameter("syncTime", String.valueOf(weekData.syncTime));
            weekData.date = (Calendar) gc.clone();
            weekData.elementId = selectedElement;
            weekData.addParameter("classId", selectedElement);
            weekData.events = events;
            weekData.weekId = String.valueOf(gc.get(Calendar.WEEK_OF_YEAR));
            weekData.addParameter("weekId", weekData.weekId);
            weekData.typeId = myType.type;
            weekData.addParameter("typeId", weekData.typeId);
            weekData.weekDataVersion = "2";
            weekData.addParameter("weekDataVersion", weekData.weekDataVersion);
            // stupid.stupidData.add(wd);

        } catch (Exception e) {
            throw e;
        }

        // prfen, ob bereits die Woche fr die Klasse und den typ vorliegt:

        WeekData existWeekData = null;
        // alle bestehden Wochen abrufen:
        for (int y = 0; y < stupid.stupidData.size(); y++) {
            existWeekData = stupid.stupidData.get(y);
            // prfen, ob das bestehende Element, dem neu hinzuzufgenden
            // entspricht(klasse,KW,Typ)
            if (existWeekData.elementId.equalsIgnoreCase(weekData.elementId) && existWeekData.weekId.equalsIgnoreCase(weekData.weekId)
                    && existWeekData.typeId.equalsIgnoreCase(weekData.typeId)) {
                // ja,es ist eine gleiche Woche bereits vorhanden
                // jedes event der neuen woche prfen, ob dieses schon existiert
                for (int ev = 0; ev < weekData.events.size(); ev++) {
                    ICalEvent newevent = weekData.events.get(ev);
                    // die schulstunde vom neuen event heraussuchen
                    int schulstunde = GetSchulstundeOfEvent(newevent);
                    // damit die schulstunde vom bestenden stundeplan abrufen
                    ICalEvent existingevent = GetSchulstunde(existWeekData, newevent.DTSTART.get(Calendar.DAY_OF_WEEK), schulstunde);
                    if(existingevent != null)
                        newevent.UID = existingevent.UID;
                    // beide schulstunden vergleichen
                    if (compareEvents(existingevent, newevent)) {
                        // unterschiede gefunden
                        newevent.UID = "diff";
                        if (existingevent == null)
                            _logger.Info("Es wurden Unterschiede in den Events gefunden: Neues Event " + newevent.DESCRIPTION + " , "
                                    + newevent.SUMMARY + " , kein altes Event vorhanden!");
                        else {
                            _logger.Info("Es wurden Unterschiede in den Events gefunden: Neues Event " + newevent.DESCRIPTION + " , "
                                    + newevent.SUMMARY + " , Altes Event " + existingevent.DESCRIPTION + " , " + existingevent.SUMMARY);
                        }
                        // �ber unterschiede benachrichtigen
                        result.add(newevent);
                    }
                    // das existierende event aus dem datensatz l�schen
                    existWeekData.events.remove(existingevent);
                }
                for (ICalEvent event : existWeekData.events) {
                    event.UID = "deleted";
                    weekData.events.add(event);
                }
                // alle verbleibenden events aus dem existierenden datensatz
                // sind ausgefallene stunden

                // existiert schon...ersetzen
                weekData.isDirty = true;
                stupid.stupidData.set(y, weekData);
                return SummarizeChanges(result);
            }
        }

        stupid.stupidData.add(weekData); // f�gt die geparste Woche den
        // // Hauptdaten
        // // hinzu
        stupid.sort();
        return SummarizeChanges(result);
    }

    private static List<ICalEvent> SummarizeChanges(List<ICalEvent> events) {
        List<ICalEvent> result = new ArrayList<ICalEvent>();
        if (events == null || events.isEmpty())
            return result;
        // alle tages �nderungen eines tages zusammenfassen
        for (ICalEvent event : events) {
            if (!AlreadyContainsDay(result, event))
                result.add(event);
        }

        return result;
    }

    private static boolean AlreadyContainsDay(List<ICalEvent> events, ICalEvent event) {
        for (ICalEvent ev : events) {
            if (ev.DTSTART == null || event.DTSTART == null)
                break;
            if (ev.DTSTART.get(Calendar.DAY_OF_YEAR) == event.DTSTART.get(Calendar.DAY_OF_YEAR))
                return true;
        }
        return false;
    }

    /**
     * Sucht den Index aus einem SelectOptionsArray
     *
     * @param array
     * @param value
     * @return
     * @author Tobias Janssen
     */
    public static String getIndexOfSelectorValue(List<SelectOptions> array, String value) {

        for (int x = 0; x < array.size(); x++) {
            if (array.get(x).description.equalsIgnoreCase(value)) {
                return String.valueOf(array.get(x).index);
            }
        }
        return "-1";
    }

    /**
     * Reduziert ein WeekData Objekt auf deren wichtigen Inhalt
     *
     * @param weekData WeekData, das reduzuert werden soll
     * @author Tobias Janssen
     * @deprecated
     */
    private static WeekData collapseWeekDataMultiDim(WeekData weekData) {

        // in das erste Feld gehen
        Xml sourceField;

        // entfernt alle Doppel-Zeilen und Spalten, die durch spans entstanden
        // sind
        weekData = removeDubColsnRows(weekData);

        // nun alle felder durchlaufen und die XML tag zusammen summieren
        for (int y = 0; y < weekData.timetable.length; y++) {
            for (int x = 0; x < weekData.timetable[y].length; x++) {
                sourceField = weekData.timetable[y][x];
                // eine zufalls id f�r dieses feld vergeben, dadurch werden alle
                // schon besuchten tags markiert. jedoch nur f�r diesen suchlauf
                int rndmId = new java.util.Random().nextInt();
                Xml resultField = new Xml("result");
                weekData.timetable[y][x] = SummerizeField(sourceField, rndmId, sourceField, resultField);
            }
        }
        // nun noch alle leeren Zeilen und Spalten entfernen, falls vorhanden
        weekData = removeEmtyColsnRows(weekData);

        return weekData;
    }

    /**
     * @param xml             Xml, mit den zusammenzuf�hrenden Childs
     * @param rndmId          int der die Feld ID angibt
     * @param origin
     * @param summerizedField Xml mit allen zusammen gef�hrten childs
     * @return
     * @author Tobias Janssen L�st Xml alle Child-Vererbungen auf und vereint
     * diese in ein Xml objekt
     * @deprecated
     */

    private static Xml SummerizeField(Xml xml, int rndmId, Xml origin, Xml summerizedField) {

        if (origin.getChildTags().length == 0) {
            summerizedField.setDataContent(origin.getDataContent());
            return summerizedField;
        }
        XmlSearch xmlSearch = new XmlSearch();
        Xml currentTag = xmlSearch.tagCrawlerFindDeepestUnSumerizedChild(origin, rndmId);

        if (currentTag.getDataContent() != null) {
            if (summerizedField.getDataContent() == null) {
                summerizedField.setDataContent(currentTag.getDataContent());
            } else {
                summerizedField.setDataContent(summerizedField.getDataContent() + " | " + currentTag.getDataContent());
            }
        }
        // Parameter auslesen
        Boolean redundanz = false;
        for (int p = 0; p < currentTag.getParameters().length; p++) {
            redundanz = false;
            if (currentTag.getParameterAtIndex(p).getName().equals("color")) {
                for (int i = 0; i < summerizedField.getParameters().length; i++) {
                    if (summerizedField.getParameterAtIndex(i).getName().equalsIgnoreCase(currentTag.getParameterAtIndex(p).getName())) {
                        redundanz = true;
                    }
                }
                if (!redundanz)
                    summerizedField.addParameter(currentTag.getParameterAtIndex(p).getName(), currentTag.getParameterAtIndex(p).getValue());
            }

        }
        currentTag.setRandomId(rndmId);
        // pr�fen, ob es noch ein parent tag gibt, und ob dieses nicht dem
        // ursprungs tag entspricht
        if (currentTag.getParentTag() != null && currentTag != origin)
            return SummerizeField(currentTag.getParentTag(), rndmId, origin, summerizedField);

        return summerizedField;
    }

    /**
     * @param htmlTableTag XmlTag, aus dem das Array erstellt werden soll
     * @return WeekData ergebnis, des XmlTag
     * @author Tobias Janssen Konvertiert ein XmlTag zu einem mehrdimensionalen
     * Array
     */
    private static WeekData convertXmlTableToWeekData(Stupid stupid, Xml htmlTableTag) {

        // Gr��e des ben�tigten Arrays muss kalkuliert werden
        WeekData weekData = new WeekData(stupid);
        weekData.setSyncDate();
        weekData.timetable = new Xml[0][0];

        // das Xml Tag heraussuchen, in dem "Montag" steht
        XmlSearch xmlSearch = new XmlSearch();
        Xml position = xmlSearch.tagCrawlerFindFirstOf(htmlTableTag, new Xml(Xml.UNSET, "Montag"));
        Xml tuesday;
        Xml table;
        do {
            xmlSearch = new XmlSearch();
            table = xmlSearch.tagCrawlerFindFirstOf(position, new Xml(Xml.TABLE), true);

            xmlSearch = new XmlSearch();
            tuesday = xmlSearch.tagCrawlerFindFirstOf(table, new Xml(Xml.UNSET, "Dienstag"));
            if (tuesday == null || tuesday.getType() == null)
                position = table.getParentTag();
        }
        while ((tuesday == null || tuesday.getType() == null) && (position != null && position.getParentTag() != null));

        xmlSearch = new XmlSearch();
        Xml tr = xmlSearch.tagCrawlerFindFirstEntryOf(table, Xml.TR);
        int rows = tr.getParentTag().getChildTags().length;
        int cols = 0;
        Boolean colSpanFound = false;
        // jedes td pr�fen:
        for (int i = 0; i < tr.getChildTags().length; i++) {
            // auf parameter pr�fen
            if (tr.getChildTagAtIndex(i).getParameters().length > 0) {
                colSpanFound = false;
                // jeden parameter �berpr�fen:
                for (int parIndex = 0; parIndex < tr.getChildTagAtIndex(i).getParameters().length; parIndex++) {
                    if (tr.getChildTagAtIndex(i).getParameterAtIndex(parIndex).getName().equalsIgnoreCase("colspan")) {
                        String value = tr.getChildTagAtIndex(i).getParameterAtIndex(parIndex).getValue();
                        int num = java.lang.Integer.parseInt(value);
                        cols += num;
                        colSpanFound = true;
                    }
                }
                if (!colSpanFound) {
                    cols++;
                }
            } else {
                // kein colspan vorhanden, daher nur eine col dazuz�hlen:
                cols++;
            }

        }
        weekData.timetable = new Xml[rows][cols];

        // die tabelle erstellen
        for (int y = 0; y < tr.getParentTag().getChildTags().length; y++) {
            Xml[] td = tr.getParentTag().getChildTagAtIndex(y).getChildTags();

            Point insertPoint;
            // jedes td pr�fen:
            for (int i = 0; i < td.length; i++) {
                insertPoint = getLastFreePosition(weekData);
                // auf parameter pr�fen
                if (td[i].getParameters().length > 0) {
                    int colspan = 0;
                    int rowspan = 0;
                    // jeden parameter �berpr�fen:
                    for (int parIndex = 0; parIndex < td[i].getParameters().length; parIndex++) {
                        if (td[i].getParameterAtIndex(parIndex).getName().equalsIgnoreCase("colspan")) {
                            String value = td[i].getParameterAtIndex(parIndex).getValue();
                            colspan = java.lang.Integer.parseInt(value);
                        }
                        if (td[i].getParameterAtIndex(parIndex).getName().equalsIgnoreCase("rowspan")) {
                            String value = td[i].getParameterAtIndex(parIndex).getValue();
                            rowspan = Integer.parseInt(value);
                        }
                    }

                    if (rowspan > 0 || colspan > 0) {
                        int col = 0;
                        do {
                            insertPoint = getLastFreePosition(weekData);
                            if (rowspan > 0) {
                                for (int row = 0; row < rowspan; row++) {
                                    weekData.timetable[insertPoint.y + row][insertPoint.x] = td[i];
                                }
                            } else {
                                weekData.timetable[insertPoint.y][insertPoint.x] = td[i];
                            }
                            col++;
                        } while (col < colspan);
                    } else {
                        weekData.timetable[insertPoint.y][insertPoint.x] = td[i];
                    }
                } else {
                    weekData.timetable[insertPoint.y][insertPoint.x] = td[i];
                }

            }
        }

        return weekData;
    }

    /**
     * @param weekData WeekData in dem gesucht werden soll
     * @return Liefert den Point, der frei ist
     * @author Tobias Janssen Sucht in dem WeekData den letzten freien Platz
     */
    private static Point getLastFreePosition(WeekData weekData) {

        Point freeIndexPoint = new Point();
        Boolean success = false;
        for (int y = 0; y < weekData.timetable.length && !success; y++) {
            for (int x = 0; x < weekData.timetable[y].length && !success; x++) {
                if (weekData.timetable[y][x] == null) {
                    freeIndexPoint.y = y;
                    freeIndexPoint.x = x;
                    success = true;
                }
            }
        }
        // TODO: exception wenn kein leerer eintrag gefunden wurde
        return freeIndexPoint;
    }

    /**
     * @param selectTag
     * @param varName
     * @return
     */
    public static List<SelectOptions> getOptionsFromJavaScriptArray(Xml selectTag, String varName) {

        int startIndex = selectTag.getDataContent().indexOf("var " + varName) + ("var " + varName).length();
        startIndex = selectTag.getDataContent().indexOf("[", startIndex) + 1;
        int stopIndex = selectTag.getDataContent().indexOf("]", startIndex);

        String vars = selectTag.getDataContent().substring(startIndex, stopIndex);
        vars = vars.replaceAll("\"", "");
        String[] strgresult = vars.split(",");
        List<SelectOptions> result = new ArrayList<SelectOptions>();
        for (int i = 0; i < strgresult.length; i++) {
            result.add(new SelectOptions(Integer.toString(i + 1), strgresult[i]));
        }

        return result;
    }

    /**
     * @param selectTag
     * @return
     * @throws Exception
     */
    public static List<SelectOptions> getOptionsFromSelectTag(Xml selectTag) throws Exception {

        List<SelectOptions> result = new ArrayList<SelectOptions>();
        if (selectTag.getChildTags().length > 0) {
            for (int i = 0; i < selectTag.getChildTags().length; i++) {
                if (selectTag.getChildTagAtIndex(i).getType().equalsIgnoreCase(Xml.OPTION)) {
                    if (selectTag.getChildTagAtIndex(i).getParameters().length > 0) {
                        SelectOptions so = new SelectOptions();
                        so.description = selectTag.getChildTagAtIndex(i).getDataContent();
                        so.index = selectTag.getChildTagAtIndex(i).getParameterAtIndex(0).getValue();
                        result.add(so);
                    }
                }
            }
        } else {
            throw new Exception("Keine Elemente im Html gefunden!");
        }
        return result;
    }

    /**
     * @param weekData WeekData, das bereinigt werden soll
     * @return WeekData, das bereinigt wurde
     * @author Tobias Janssen Entfernt doppelte Reihen und Spalten
     */
    private static WeekData removeDubColsnRows(WeekData weekData) {

        Xml[][] tempTimeTable = new Xml[0][0];

        Boolean dub = false;

        // zuerst alle Zeilen pr�fen, ob diese gleich der n�chsten ist
        for (int y = 0; y + 1 < weekData.timetable.length; y++) {
            dub = true;
            for (int x = 0; x < weekData.timetable.length && dub; x++) {
                if (!weekData.timetable[y + 1][x].equals(weekData.timetable[y][x]) && dub) {
                    dub = false;
                }
            }
            if (!dub) {
                // alle nicht Dublicate werden dem neuen array hinzugef�gt
                tempTimeTable = (Xml[][]) ArrayOperations.AppendToArray(tempTimeTable, weekData.timetable[y]);
            }
        }
        tempTimeTable = (Xml[][]) ArrayOperations.AppendToArray(tempTimeTable, weekData.timetable[weekData.timetable.length - 1]);

        // fingerprints(strings aus 0 und 1) f�r jede zeile erstellen. 1 zeigt,
        // dass dieses feld mit dem vorg�nger gleich ist
        String[] print = new String[weekData.timetable.length];
        for (int y = 0; y < weekData.timetable.length; y++) {
            print[y] = fingerprintOfDubs(weekData.timetable[y]);
        }

        // nun m�ssen die fingerprints aller Array zeilen zusammengef�gt werden
        int sum = 0;
        String printRes = "";
        for (int y = 0; y < print.length; y++) {
            sum = 0;
            for (int x = 0; x < print[y].length(); x++) {
                sum += Integer.decode(String.valueOf(print[y].charAt(x)));
            }
            if (sum != 0) {
                printRes += "1";
            } else {
                printRes += "0";
            }
        }
        // es ist eine fingerabdruck f�r ein zweidimensinales array entstanden,
        // an hand diesem kann nun ein neues array erstellt werden, dass keine
        // dublicate hat

        int count = -1;
        // z�hlen der 0en f�r die l�nge einer zeile, denn diese sind kein
        // dublicat.
        for (int y = 0; y < printRes.length() && count == -1; y++) {
            if (String.valueOf(printRes.charAt(y)).equalsIgnoreCase("0")) {
                count = print[y].length();
            }

        }
        // das neue array f�r das ergebnis erstellen
        weekData.timetable = new Xml[tempTimeTable.length][count];
        Point point = new Point();
        // das vorherige ergenis nutzen wir nun um mit hilfe des fingerprints
        // das neue array zu f�llen
        for (int y = 0; y < tempTimeTable.length; y++) {
            for (int x = 0; x < tempTimeTable[y].length; x++) {
                // nur 0en, also nicht dublicate hinzuf�gen
                if (String.valueOf(printRes.charAt(x)).equalsIgnoreCase("0")) {
                    // das feld hinzuf�gen
                    point = getLastFreePosition(weekData);
                    System.arraycopy(tempTimeTable[y], x, weekData.timetable[point.y], point.x, 1);
                }
            }

        }

        return weekData;
    }

    /**
     * @param array XmlTag-Array von dem der Fingeprint erstellt werden soll
     * @return ein String, der durch 0 und 1 angibt, ob das element an dieser
     * Position den vorherigen gleicht
     * @author Tobias Janssen erstellt einen Fingeprint f�r ein eindimensoinales
     * Array
     */
    private static String fingerprintOfDubs(Xml[] array) {
        String fingerprint = "0";
        for (int x = 1; x < array.length; x++) {

            if (array[x].equals(array[x - 1])) {
                fingerprint += "1";
            } else {
                fingerprint += "0";
            }
        }
        return fingerprint;
    }

    /**
     * @param weekData WeekData, das zu bereinigen ist
     * @return WeekData, das bereinigt wurde
     * @author Tobias Janssen Pr�ft und entfernt vollst�ndig leere Zeilen und
     * Spalten
     */
    private static WeekData removeEmtyColsnRows(WeekData weekData) {

        Xml[][] yResult = new Xml[0][0];

        // erst leere y-zeilen entfernen
        Boolean empty;
        for (int y = 0; y < weekData.timetable.length; y++) {
            empty = true;
            // alle spalten dieser zeile durchgehen und pr�fen, ob alle leer
            // sind
            for (int x = 0; x < weekData.timetable[y].length; x++) {
                if (weekData.timetable[y][x].getDataContent() != null && empty == true)
                    empty = false;
            }

            // wenn davon eines nicht leer ist
            if (!empty) {
                // wird diese zeile dem ergebnis angef�gt
                yResult = (Xml[][]) ArrayOperations.AppendToArray(yResult, weekData.timetable[y]);
            }

        }

        Xml[][] xResult = new Xml[yResult.length][yResult[0].length];
        int lengthX;
        // jetzt alle x im yResult pr�fen
        for (int x = 0; x < yResult[0].length; x++) {
            empty = true;
            // alle zeilen dieser zeile durchgehen und pr�fen, ob alle leer sind
            for (int y = 0; y < yResult.length && empty; y++) {
                if (yResult[y][x].getDataContent() != null && empty == true)
                    empty = false;
            }
            // wenn davon eines nicht leer ist
            if (!empty) {
                // hinzuf�gen
                lengthX = 0;
                Boolean positionFound = false;
                for (int i = 0; i < xResult[0].length && !positionFound; i++) {
                    if (xResult[0][i] == null) {
                        lengthX = i;
                        positionFound = true;
                    }
                }
                for (int y = 0; y < yResult.length; y++) {
                    System.arraycopy(yResult[y], x, xResult[y], lengthX, 1);
                }
            }
        }

        // herausfinden, ob das xResult noch leere Felder hat, wenn ja, wird
        // diese posX zur�ckgeliefert
        lengthX = xResult[0].length;
        Boolean positionFound = false;
        for (int i = 0; i < xResult[0].length && !positionFound; i++) {
            if (xResult[0][i] == null) {
                lengthX = i;
                positionFound = true;
            }
        }
        Xml[][] endResult = new Xml[xResult.length][lengthX];
        for (int y = 0; y < xResult.length; y++) {
            System.arraycopy(xResult[y], 0, endResult[y], 0, lengthX);
        }
        // nun noch alle Felder durchlaufen und die dataContent null mit ""
        // ersetzten:
        for (int y = 0; y < endResult.length; y++) {
            for (int x = 0; x < endResult[y].length; x++) {
                if (endResult[y][x].getDataContent() == null) {
                    endResult[y][x].setDataContent("");
                }
            }

        }

        weekData.timetable = endResult;
        return weekData;
    }

    /**
     * @param ctxt
     * @param handler
     */
    public static void contactStupidService(Context ctxt, Handler handler) {

        Intent intent = new Intent(ctxt, MyService.class);
        if (handler != null) {
            Messenger messenger = new Messenger(handler);
            intent.putExtra("MESSENGER", messenger);

        }
        intent.putExtra("fromFrontend", true);
        ctxt.startService(intent);
    }

    /**
     * @param ctxt
     * @param handler
     */
    public static void contactStupidServiceQuiet(Context ctxt, Handler handler) {

        Intent intent = new Intent(ctxt, MyService.class);
        if (handler != null) {
            Messenger messenger = new Messenger(handler);
            intent.putExtra("MESSENGER", messenger);

        }
        intent.putExtra("fromFrontend", true);
        intent.putExtra("quiet", true);
        ctxt.startService(intent);
    }
}
