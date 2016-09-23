package de.janssen.android.gsoplan.xml;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import de.janssen.android.gsoplan.ICalEvent;
import de.janssen.android.gsoplan.Logger;
import de.janssen.android.gsoplan.dataclasses.Const;

public class Ical {
    private static Logger _logger;

    public static List<ICalEvent> Parse(String value) throws ParseException {
        // erstmal nach zeilen trennen:
        _logger = new Logger(Const.APPFOLDER, "Ical");
        String[] rows = value.split("\r\n");

        List<ICalEvent> result = new ArrayList<ICalEvent>();
        ICalEvent currentEvent = null;
        // dann jede zeile parsen
        for (int i = 0; i < rows.length; i++) {
            String[] row = rows[i].split(":");
            if (row.length == 2) {
                if (row[0].equalsIgnoreCase(ICalElement.BEGIN.name()) && row[1].equalsIgnoreCase("VEVENT")) {
                    currentEvent = new ICalEvent();
                }
                if (row[0].equalsIgnoreCase(ICalElement.DTSTAMP.name()) && currentEvent != null) {
                    currentEvent.DTSTAMP = ConvertDate(row[1]);
                }
                if (row[0].equalsIgnoreCase(ICalElement.DTSTART.name()) && currentEvent != null) {
                    currentEvent.DTSTART = ConvertDate(row[1]);
                }
                if (row[0].equalsIgnoreCase(ICalElement.DTEND.name()) && currentEvent != null) {
                    currentEvent.DTEND = ConvertDate(row[1]);
                }
                if (row[0].equalsIgnoreCase(ICalElement.UID.name()) && currentEvent != null) {
                    currentEvent.UID = row[1];
                }
                if (row[0].equalsIgnoreCase(ICalElement.LOCATION.name()) && currentEvent != null) {
                    if (row[1] != null)
                        currentEvent.LOCATION = row[1];
                    else
                        currentEvent.LOCATION = "";
                }
                if (row[0].equalsIgnoreCase(ICalElement.SUMMARY.name()) && currentEvent != null) {
                    if (row[1] != null)
                        currentEvent.SUMMARY = row[1];
                    else
                        currentEvent.SUMMARY = "";
                }
                if (row[0].equalsIgnoreCase(ICalElement.DESCRIPTION.name()) && currentEvent != null) {
                    if (row[1] != null)
                        currentEvent.DESCRIPTION = row[1];
                    else
                        currentEvent.DESCRIPTION = "";
                }
                if (row[0].equalsIgnoreCase(ICalElement.END.name()) && currentEvent != null && row[1].equalsIgnoreCase("VEVENT")) {
                    result.add(currentEvent);
                }

            } else if (row.length == 1
                    && (row[0].equalsIgnoreCase(ICalElement.DESCRIPTION.name()) || row[0].equalsIgnoreCase(ICalElement.SUMMARY.name()))) {
                if (row[0].equalsIgnoreCase(ICalElement.DESCRIPTION.name()))
                    currentEvent.DESCRIPTION = "";
                if (row[0].equalsIgnoreCase(ICalElement.SUMMARY.name()))
                    currentEvent.SUMMARY = "";

            } else {
                if (row == null || !(row[0].equals("\n") || row[0].equals("\t") || row[0].equals("\r")))
                    _logger.Error("iCal file is in wrong format: " + rows[i]);
            }
        }
        return result;
    }

    public static String Export(List<ICalEvent> value) {
        _logger = new Logger(Const.APPFOLDER, "Ical");
        // dann jede zeile parsen
        String r = "BEGIN:VCALENDAR\r\n" + "PRODID:-//Ben Fortuna//iCal4j 1.0//EN\r\n" + "VERSION:2.0\r\n" + "CALSCALE:GREGORIAN\r\n";
        for (int i = 0; i < value.size(); i++) {
            r += ICalElement.BEGIN.name() + ":VEVENT\r\n";
            r += ICalElement.DTSTAMP.name() + ":" + ConvertDate(value.get(i).DTSTAMP) + "\r\n";
            r += ICalElement.DTSTART.name() + ":" + ConvertDate(value.get(i).DTSTART) + "\r\n";
            r += ICalElement.DTEND.name() + ":" + ConvertDate(value.get(i).DTEND) + "\r\n";
            r += ICalElement.UID.name() + ":" + value.get(i).UID + "\r\n";
            r += ICalElement.DESCRIPTION.name() + ":" + value.get(i).DESCRIPTION + "\r\n";
            r += ICalElement.LOCATION.name() + ":" + value.get(i).LOCATION + "\r\n";
            r += ICalElement.SUMMARY.name() + ":" + value.get(i).SUMMARY + "\r\n";
            r += ICalElement.END.name() + ":VEVENT" + "\r\n";
        }
        r += "END:VCALENDAR\r\n";
        return r;
    }

    /**
     * Transform ISO 8601 string to Calendar.
     */
    public static Calendar ConvertDate(final String iso8601string) throws ParseException {
        Calendar calendar = GregorianCalendar.getInstance();
        String s = iso8601string.replace("Z", "+00:00");
        Date date = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ").parse(s);
        calendar.setTime(date);
        return calendar;
    }

    /**
     * Transform Calendar to ISO 8601 string.
     */
    public static String ConvertDate(final Calendar calendar) {
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String formatted = sdf.format(date);
        return formatted.substring(0, formatted.indexOf("+")) + "Z";
    }

    private enum ICalElement {
        BEGIN, PRODID, VERSION, CALSCALE, DTSTAMP, DTSTART, DTEND, UID, DESCRIPTION, SUMMARY, LOCATION, END
    }

}
